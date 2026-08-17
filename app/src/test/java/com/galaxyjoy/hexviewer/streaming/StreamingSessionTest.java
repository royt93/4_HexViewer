package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class StreamingSessionTest {
    @Test
    public void readsAcrossPagesAndCachesRepeatedRead() throws Exception {
        byte[] bytes = new byte[40];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        ShortReadSource source = new ShortReadSource(bytes, 3);
        StreamingSession session = new StreamingSession(source, 8, 16);

        assertArrayEquals(Arrays.copyOfRange(bytes, 5, 26),
                session.read(new WindowRange(5, 26)));
        int callsAfterFirstRead = source.readCalls;
        assertArrayEquals(Arrays.copyOfRange(bytes, 16, 24),
                session.read(new WindowRange(16, 24)));

        assertEquals(callsAfterFirstRead, source.readCalls);
        session.close();
        assertTrue(source.closed);
    }

    @Test
    public void truncatesWindowAtEof() throws Exception {
        StreamingSession session = new StreamingSession(
                new ShortReadSource(new byte[]{1, 2, 3}, 2), 2, 4);

        assertArrayEquals(new byte[]{2, 3}, session.read(new WindowRange(1, 20)));
        byte[] destination = new byte[2];
        assertEquals(-1, session.readAt(3, destination, 0, destination.length));
    }

    @Test
    public void readsAtOffsetBeyondTwoGiBWithoutNarrowing() throws Exception {
        long markerPosition = 3L * 1024 * 1024 * 1024 + 7;
        SparseSource source = new SparseSource(markerPosition + 1, markerPosition, (byte) 0x5a);
        StreamingSession session = new StreamingSession(source, 256, 512);
        byte[] result = new byte[1];

        assertEquals(1, session.readAt(markerPosition, result, 0, 1));
        assertEquals((byte) 0x5a, result[0]);
        assertTrue(source.lastReadPosition > Integer.MAX_VALUE);
    }

    @Test
    public void generationInvalidatesStaleUiWork() throws Exception {
        StreamingSession session = new StreamingSession(
                new ShortReadSource(new byte[1], 1), 1, 1);
        long first = session.nextGeneration();
        assertTrue(session.isCurrentGeneration(first));
        long second = session.nextGeneration();
        assertFalse(session.isCurrentGeneration(first));
        assertTrue(session.isCurrentGeneration(second));
    }

    @Test
    public void emptyWindowReadReturnsEmptyArrayWithoutTouchingSource() throws Exception {
        ShortReadSource source = new ShortReadSource(new byte[]{1, 2, 3}, 3);
        StreamingSession session = new StreamingSession(source, 8, 16);

        byte[] result = session.read(new WindowRange(2, 2));

        assertEquals(0, result.length);
        assertEquals(0, source.readCalls);
    }

    @Test
    public void readExactlyAtPageBoundaryDoesNotOverlapNeighboringPages() throws Exception {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        ShortReadSource source = new ShortReadSource(bytes, 32);
        // pageSize = 8: pages are [0,8) [8,16) [16,24) [24,32)
        StreamingSession session = new StreamingSession(source, 8, 32);

        // A window that starts and ends exactly on page boundaries, spanning 3 full pages.
        assertArrayEquals(Arrays.copyOfRange(bytes, 8, 32), session.read(new WindowRange(8, 32)));
        // A single byte read straddling the very last page boundary is still correct.
        byte[] lastByte = new byte[1];
        assertEquals(1, session.readAt(31, lastByte, 0, 1));
        assertEquals(bytes[31], lastByte[0]);
    }

    @Test
    public void zeroProgressSourceFailsInsteadOfLooping() throws Exception {
        SeekableDataSource source = new SeekableDataSource() {
            @Override public long size() { return 2; }
            @Override public int readAt(long position, byte[] destination, int offset, int length) { return 0; }
            @Override public void close() { }
        };
        StreamingSession session = new StreamingSession(source, 2, 2);

        try {
            session.read(new WindowRange(0, 2));
            fail("Expected IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("no progress"));
        }
    }

    private static final class ShortReadSource implements SeekableDataSource {
        private final byte[] bytes;
        private final int maxRead;
        private int readCalls;
        private boolean closed;

        private ShortReadSource(byte[] bytes, int maxRead) {
            this.bytes = bytes;
            this.maxRead = maxRead;
        }

        @Override public long size() { return bytes.length; }

        @Override
        public int readAt(long position, byte[] destination, int offset, int length) {
            readCalls++;
            if (position >= bytes.length) return -1;
            int amount = Math.min(Math.min(length, maxRead), bytes.length - (int) position);
            System.arraycopy(bytes, (int) position, destination, offset, amount);
            return amount;
        }

        @Override public void close() { closed = true; }
    }

    private static final class SparseSource implements SeekableDataSource {
        private final long size;
        private final long markerPosition;
        private final byte marker;
        private long lastReadPosition;

        private SparseSource(long size, long markerPosition, byte marker) {
            this.size = size;
            this.markerPosition = markerPosition;
            this.marker = marker;
        }

        @Override public long size() { return size; }

        @Override
        public int readAt(long position, byte[] destination, int offset, int length) {
            lastReadPosition = position;
            if (position >= size) return -1;
            int amount = (int) Math.min(length, size - position);
            Arrays.fill(destination, offset, offset + amount, (byte) 0);
            if (markerPosition >= position && markerPosition < position + amount) {
                destination[offset + (int) (markerPosition - position)] = marker;
            }
            return amount;
        }

        @Override public void close() { }
    }
}
