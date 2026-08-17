package com.galaxyjoy.hexviewer.streaming.edit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StreamingCopyHelperTest {
    @Test
    public void copyAndApply_preservesSizeAndUnchangedBytes() throws Exception {
        byte[] source = sequence(20);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        List<DirtyRange> ranges = Arrays.asList(
                new DirtyRange(2, new byte[]{2, 3}, new byte[]{22, 33}),
                new DirtyRange(17, new byte[]{17, 18, 19}, new byte[]{77, 88, 99}));

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, ranges, 5, () -> false, null);

        byte[] expected = source.clone();
        expected[2] = 22;
        expected[3] = 33;
        expected[17] = 77;
        expected[18] = 88;
        expected[19] = 99;
        assertArrayEquals(expected, destination.toByteArray());
        assertEquals(source.length, destination.size());
    }

    @Test
    public void copyAndApply_appliesRangeAcrossChunkBoundary() throws Exception {
        byte[] source = sequence(12);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        DirtyRange range = new DirtyRange(3, new byte[]{3, 4, 5, 6},
                new byte[]{30, 40, 50, 60});

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, Collections.singletonList(range), 4, () -> false, null);

        assertArrayEquals(new byte[]{0, 1, 2, 30, 40, 50, 60, 7, 8, 9, 10, 11},
                destination.toByteArray());
    }

    @Test
    public void copyAndApply_editAtOffsetZero() throws Exception {
        byte[] source = sequence(10);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        DirtyRange atStart = new DirtyRange(0, new byte[]{0, 1}, new byte[]{(byte) 0xAA, (byte) 0xBB});

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, Collections.singletonList(atStart), 4, () -> false, null);

        byte[] expected = source.clone();
        expected[0] = (byte) 0xAA;
        expected[1] = (byte) 0xBB;
        assertArrayEquals(expected, destination.toByteArray());
    }

    @Test
    public void copyAndApply_editAtFinalByteOfSource() throws Exception {
        byte[] source = sequence(10);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        DirtyRange atEnd = new DirtyRange(9, new byte[]{9}, new byte[]{(byte) 0xEE});

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, Collections.singletonList(atEnd), 4, () -> false, null);

        byte[] expected = source.clone();
        expected[9] = (byte) 0xEE;
        assertArrayEquals(expected, destination.toByteArray());
        assertEquals(source.length, destination.size());
    }

    @Test
    public void copyAndApply_multipleNonContiguousEditsIncludingStartAndEnd() throws Exception {
        byte[] source = sequence(16);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        List<DirtyRange> ranges = Arrays.asList(
                new DirtyRange(0, new byte[]{0}, new byte[]{(byte) 0x10}),
                new DirtyRange(5, new byte[]{5}, new byte[]{(byte) 0x50}),
                new DirtyRange(15, new byte[]{15}, new byte[]{(byte) 0xF0}));

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, ranges, 4, () -> false, null);

        byte[] expected = source.clone();
        expected[0] = (byte) 0x10;
        expected[5] = (byte) 0x50;
        expected[15] = (byte) 0xF0;
        assertArrayEquals(expected, destination.toByteArray());
    }

    @Test
    public void copyAndApply_sortsNonOverlappingInputRanges() throws Exception {
        byte[] source = sequence(8);
        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        List<DirtyRange> unsorted = Arrays.asList(
                new DirtyRange(6, new byte[]{6}, new byte[]{66}),
                new DirtyRange(1, new byte[]{1}, new byte[]{11}));

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                source.length, unsorted);

        assertArrayEquals(new byte[]{0, 11, 2, 3, 4, 5, 66, 7}, destination.toByteArray());
    }

    @Test
    public void copyAndApply_reportsMonotonicProgress() throws Exception {
        byte[] source = sequence(10);
        AtomicLong previous = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();

        StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source),
                new ByteArrayOutputStream(), source.length, Collections.emptyList(), 3,
                () -> false, (copied, total) -> {
                    assertTrue(copied > previous.get());
                    assertEquals(source.length, total);
                    previous.set(copied);
                    calls.incrementAndGet();
                });

        assertEquals(source.length, previous.get());
        assertEquals(4, calls.get());
    }

    @Test
    public void copyAndApply_cancelDoesNotReportSuccessOrFlushRemainder() {
        byte[] source = sequence(20);
        AtomicInteger checks = new AtomicInteger();
        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        assertThrows(InterruptedIOException.class,
                () -> StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(source), destination,
                        source.length, Collections.emptyList(), 4,
                        () -> checks.incrementAndGet() > 2, null));
        assertTrue(destination.size() < source.length);
    }

    @Test
    public void copyAndApply_rejectsShortOrGrowingSource() {
        assertThrows(EOFException.class,
                () -> StreamingCopyHelper.copyAndApply(
                        new ByteArrayInputStream(new byte[3]), new ByteArrayOutputStream(),
                        4, Collections.emptyList()));
        assertThrows(IOException.class,
                () -> StreamingCopyHelper.copyAndApply(
                        new ByteArrayInputStream(new byte[5]), new ByteArrayOutputStream(),
                        4, Collections.emptyList()));
    }

    @Test
    public void copyAndApply_rejectsSourceChangedUnderDirtyRange() {
        byte[] changedSource = new byte[]{0, 99, 2, 3};
        DirtyRange editFromOldWindow = new DirtyRange(1, new byte[]{1}, new byte[]{11});

        StreamingCopyHelper.SourceConflictException exception = assertThrows(
                StreamingCopyHelper.SourceConflictException.class,
                () -> StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(changedSource),
                        new ByteArrayOutputStream(), changedSource.length,
                        Collections.singletonList(editFromOldWindow)));

        assertEquals(1, exception.getOffset());
    }

    @Test
    public void copyAndApply_rejectsOverlapAndOutOfBounds() {
        List<DirtyRange> overlap = Arrays.asList(
                new DirtyRange(1, new byte[]{1, 2}, new byte[]{9, 9}),
                new DirtyRange(2, new byte[]{2}, new byte[]{8}));
        assertThrows(IllegalArgumentException.class,
                () -> StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(new byte[4]),
                        new ByteArrayOutputStream(), 4, overlap));

        DirtyRange outOfBounds = new DirtyRange(4, new byte[]{0}, new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> StreamingCopyHelper.copyAndApply(new ByteArrayInputStream(new byte[4]),
                        new ByteArrayOutputStream(), 4, Collections.singletonList(outOfBounds)));
    }

    private static byte[] sequence(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) bytes[i] = (byte) i;
        return bytes;
    }
}
