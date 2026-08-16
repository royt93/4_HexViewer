package com.galaxyjoy.hexviewer.streaming.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamingSearcherTest {
    @Test
    public void search_findsMatchAcrossChunkBoundary() throws Exception {
        byte[] source = "xxxxxxABCDEFyyyy".getBytes(StandardCharsets.US_ASCII);
        StreamingSearcher searcher = new StreamingSearcher(8);

        OptionalLong hit = searcher.findNext(reader(source), 0, source.length,
                "ABCDEF".getBytes(StandardCharsets.US_ASCII), null);

        assertTrue(hit.isPresent());
        assertEquals(6, hit.getAsLong());
    }

    @Test
    public void search_findsOverlappingMatchesOnce() throws Exception {
        byte[] source = "AAAAA".getBytes(StandardCharsets.US_ASCII);
        StreamingSearcher searcher = new StreamingSearcher(2);
        List<Long> hits = new ArrayList<>();

        StreamingSearcher.SearchSummary summary = searcher.search(reader(source), 0, source.length,
                "AAA".getBytes(StandardCharsets.US_ASCII), offset -> {
                    hits.add(offset);
                    return true;
                }, null);

        assertEquals(Arrays.asList(0L, 1L, 2L), hits);
        assertEquals(3, summary.getMatchCount());
        assertEquals(StreamingSearcher.Status.COMPLETED, summary.getStatus());
    }

    @Test
    public void search_findsPatternEndingExactlyAtEof() throws Exception {
        byte[] source = "prefix-hit".getBytes(StandardCharsets.US_ASCII);
        OptionalLong hit = new StreamingSearcher(3).findNext(reader(source), 0, source.length,
                "hit".getBytes(StandardCharsets.US_ASCII), null);

        assertEquals(source.length - 3, hit.getAsLong());
    }

    @Test
    public void search_earlyEofCompletesWithoutMatch() throws Exception {
        byte[] source = "short".getBytes(StandardCharsets.US_ASCII);
        StreamingSearcher searcher = new StreamingSearcher(4);

        StreamingSearcher.SearchSummary summary = searcher.search(reader(source), 0, 100,
                "missing".getBytes(StandardCharsets.US_ASCII), offset -> true, null);

        assertEquals(StreamingSearcher.Status.COMPLETED, summary.getStatus());
        assertEquals(source.length, summary.getBytesRead());
        assertEquals(0, summary.getMatchCount());
    }

    @Test
    public void search_noMatchReturnsEmpty() throws Exception {
        byte[] source = "abcdefgh".getBytes(StandardCharsets.US_ASCII);
        OptionalLong hit = new StreamingSearcher(3).findNext(reader(source), 0, source.length,
                "xyz".getBytes(StandardCharsets.US_ASCII), null);
        assertFalse(hit.isPresent());
    }

    @Test
    public void search_honorsStartAndEndRange() throws Exception {
        byte[] source = "hit__hit__hit".getBytes(StandardCharsets.US_ASCII);
        List<Long> hits = new ArrayList<>();
        new StreamingSearcher(4).search(reader(source), 4, 10,
                "hit".getBytes(StandardCharsets.US_ASCII), offset -> {
                    hits.add(offset);
                    return true;
                }, null);
        assertEquals(Arrays.asList(5L), hits);
    }

    @Test
    public void search_callbackCanStopIncrementallyAtFirstHit() throws Exception {
        byte[] source = "hit-hit-hit".getBytes(StandardCharsets.US_ASCII);
        AtomicInteger callbacks = new AtomicInteger();

        StreamingSearcher.SearchSummary summary = new StreamingSearcher(4).search(reader(source),
                0, source.length, "hit".getBytes(StandardCharsets.US_ASCII), offset -> {
                    callbacks.incrementAndGet();
                    return false;
                }, null);

        assertEquals(1, callbacks.get());
        assertEquals(1, summary.getMatchCount());
        assertEquals(StreamingSearcher.Status.STOPPED_BY_CALLBACK, summary.getStatus());
    }

    @Test
    public void search_cancelsBeforeAndDuringScanning() throws Exception {
        byte[] source = new byte[100_000];
        StreamingSearcher searcher = new StreamingSearcher(16_384);
        StreamingSearcher.SearchSummary before = searcher.search(reader(source), 0, source.length,
                new byte[]{1}, offset -> true, () -> true);
        assertEquals(StreamingSearcher.Status.CANCELLED, before.getStatus());
        assertEquals(0, before.getBytesRead());

        AtomicInteger checks = new AtomicInteger();
        StreamingSearcher.SearchSummary during = searcher.search(reader(source), 0, source.length,
                new byte[]{1}, offset -> true, () -> checks.incrementAndGet() > 3);
        assertEquals(StreamingSearcher.Status.CANCELLED, during.getStatus());
        assertTrue(during.getBytesRead() > 0);
        assertTrue(during.getBytesRead() < source.length);
    }

    @Test
    public void search_handlesShortReads() throws Exception {
        byte[] source = "0123456789TARGET".getBytes(StandardCharsets.US_ASCII);
        StreamingSearcher.RandomAccessReader shortReader = (offset, destination, destOffset, length) -> {
            if (offset >= source.length) return -1;
            int count = (int) Math.min(2, Math.min(length, source.length - offset));
            System.arraycopy(source, (int) offset, destination, destOffset, count);
            return count;
        };

        OptionalLong hit = new StreamingSearcher(8).findNext(shortReader, 0, source.length,
                "TARGET".getBytes(StandardCharsets.US_ASCII), null);
        assertEquals(10, hit.getAsLong());
    }

    @Test
    public void search_retriesThenRejectsNoProgressReader() {
        AtomicInteger reads = new AtomicInteger();
        StreamingSearcher.RandomAccessReader stuck = (offset, destination, destOffset, length) -> {
            reads.incrementAndGet();
            return 0;
        };

        IOException error = assertThrows(IOException.class,
                () -> new StreamingSearcher(8).search(stuck, 0, 100, new byte[]{1},
                        offset -> true, null));
        assertTrue(error.getMessage().contains("no progress"));
        assertEquals(3, reads.get());
    }

    @Test
    public void search_supportsAbsoluteOffsetsAboveTwoGiBWithoutLargeAllocation() throws Exception {
        long matchOffset = 3L * 1024 * 1024 * 1024 + 17;
        long start = matchOffset - 10;
        long end = matchOffset + 20;
        byte[] pattern = new byte[]{9, 8, 7, 6};
        AtomicInteger maxRequested = new AtomicInteger();
        StreamingSearcher.RandomAccessReader fake = (offset, destination, destOffset, length) -> {
            maxRequested.updateAndGet(previous -> Math.max(previous, length));
            if (offset >= end) return -1;
            int count = (int) Math.min(length, end - offset);
            Arrays.fill(destination, destOffset, destOffset + count, (byte) 0);
            long intersectionStart = Math.max(offset, matchOffset);
            long intersectionEnd = Math.min(offset + count, matchOffset + pattern.length);
            if (intersectionStart < intersectionEnd) {
                System.arraycopy(pattern, (int) (intersectionStart - matchOffset), destination,
                        destOffset + (int) (intersectionStart - offset),
                        (int) (intersectionEnd - intersectionStart));
            }
            return count;
        };

        OptionalLong hit = new StreamingSearcher(7).findNext(fake, start, end, pattern, null);

        assertEquals(matchOffset, hit.getAsLong());
        assertTrue(maxRequested.get() <= 7);
    }

    @Test
    public void search_validatesInputsAndInvalidReaderCounts() {
        StreamingSearcher searcher = new StreamingSearcher(4);
        assertThrows(IllegalArgumentException.class, () -> new StreamingSearcher(0));
        assertThrows(IllegalArgumentException.class,
                () -> searcher.search(reader(new byte[0]), 0, 0, new byte[0], offset -> true, null));
        assertThrows(IllegalArgumentException.class,
                () -> searcher.search(reader(new byte[0]), 2, 1, new byte[]{1}, offset -> true, null));
        assertThrows(IOException.class,
                () -> searcher.search((o, d, x, l) -> l + 1, 0, 10, new byte[]{1},
                        offset -> true, null));
    }

    private static StreamingSearcher.RandomAccessReader reader(byte[] source) {
        return (offset, destination, destinationOffset, length) -> {
            if (offset >= source.length) return -1;
            int count = (int) Math.min(length, source.length - offset);
            System.arraycopy(source, (int) offset, destination, destinationOffset, count);
            return count;
        };
    }
}
