package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpoolingSeekableDataSourceTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void spoolsWithBoundedBufferReportsProgressAndSupportsRandomRead() throws Exception {
        byte[] input = new byte[10_000];
        for (int i = 0; i < input.length; i++) input[i] = (byte) (i % 251);
        List<Long> progress = new ArrayList<>();
        TrackingInputStream stream = new TrackingInputStream(input);

        SpoolingSeekableDataSource source = SpoolingSeekableDataSource.spool(
                stream, temporaryFolder.getRoot(), input.length, 0, 127,
                (copied, expected) -> {
                    assertEquals(input.length, expected);
                    progress.add(copied);
                }, () -> false);
        FileSnapshot snapshot = new FileSnapshot(source.cacheFileForTesting());

        byte[] actual = new byte[333];
        assertEquals(actual.length, source.readAt(8_900, actual, 0, actual.length));
        assertArrayEquals(Arrays.copyOfRange(input, 8_900, 9_233), actual);
        assertEquals(input.length, source.size());
        assertTrue(stream.closed);
        assertEquals(Long.valueOf(0), progress.get(0));
        assertEquals(Long.valueOf(input.length), progress.get(progress.size() - 1));
        assertTrue(snapshot.exists());

        source.close();
        assertFalse(snapshot.exists());
    }

    @Test
    public void cancellationClosesInputAndDeletesPartialFile() throws Exception {
        byte[] input = new byte[4_096];
        TrackingInputStream stream = new TrackingInputStream(input);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        try {
            SpoolingSeekableDataSource.spool(stream, temporaryFolder.getRoot(), input.length,
                    0, 128, (copied, expected) -> {
                        if (copied >= 256) cancelled.set(true);
                    }, cancelled::get);
            fail("Expected cancellation");
        } catch (SpoolingSeekableDataSource.SpoolingCancelledException expected) {
            assertTrue(stream.closed);
            assertEquals(0, spoolFileCount());
        }
    }

    @Test
    public void copyFailureClosesInputAndDeletesPartialFile() throws Exception {
        InputStream failing = new InputStream() {
            private boolean closed;
            private int calls;
            @Override public int read() throws IOException { throw new IOException("unused"); }
            @Override public int read(byte[] bytes) throws IOException {
                if (calls++ == 0) {
                    Arrays.fill(bytes, 0, 10, (byte) 1);
                    return 10;
                }
                throw new IOException("provider failed");
            }
            @Override public void close() { closed = true; }
        };

        try {
            SpoolingSeekableDataSource.spool(failing, temporaryFolder.getRoot(), -1,
                    0, 64, null, null);
            fail("Expected provider failure");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("provider failed"));
            assertEquals(0, spoolFileCount());
        }
    }

    @Test
    public void freeSpacePrecheckFailsBeforeCreatingTempButStillClosesInput() throws Exception {
        TrackingInputStream stream = new TrackingInputStream(new byte[1]);

        try {
            SpoolingSeekableDataSource.spool(stream, temporaryFolder.getRoot(), Long.MAX_VALUE,
                    1, 64, null, null);
            fail("Expected insufficient-space failure");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Insufficient cache space"));
            assertTrue(stream.closed);
            assertEquals(0, spoolFileCount());
        }
    }

    @Test
    public void inputCloseFailureAfterCopyDeletesCompletedTempFile() throws Exception {
        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2}) {
            @Override public void close() throws IOException { throw new IOException("close failed"); }
        };

        try {
            SpoolingSeekableDataSource.spool(stream, temporaryFolder.getRoot(), 2,
                    0, 2, null, null);
            fail("Expected close failure");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("close failed"));
            assertEquals(0, spoolFileCount());
        }
    }

    @Test
    public void copyFailureRemainsPrimaryWhenInputCloseAlsoFails() throws Exception {
        InputStream stream = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("copy failed"); }
            @Override public int read(byte[] bytes) throws IOException { throw new IOException("copy failed"); }
            @Override public void close() throws IOException { throw new IOException("close failed"); }
        };

        try {
            SpoolingSeekableDataSource.spool(stream, temporaryFolder.getRoot(), -1,
                    0, 2, null, null);
            fail("Expected copy failure");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("copy failed"));
            assertEquals(1, expected.getSuppressed().length);
            assertTrue(expected.getSuppressed()[0].getMessage().contains("close failed"));
            assertEquals(0, spoolFileCount());
        }
    }

    @Test
    public void toleratesAZeroLengthReadWithoutSpinning() throws Exception {
        byte[] bytes = {1, 2, 3};
        InputStream stream = new ByteArrayInputStream(bytes) {
            private boolean first = true;
            @Override public synchronized int read(byte[] destination) {
                if (first) {
                    first = false;
                    return 0;
                }
                return super.read(destination, 0, destination.length);
            }
        };

        try (SpoolingSeekableDataSource source = SpoolingSeekableDataSource.spool(
                stream, temporaryFolder.getRoot(), bytes.length, 0, 2, null, null)) {
            byte[] actual = new byte[3];
            assertEquals(3, source.readAt(0, actual, 0, actual.length));
            assertArrayEquals(bytes, actual);
        }
    }

    private int spoolFileCount() {
        String[] files = temporaryFolder.getRoot().list(
                (directory, name) -> name.startsWith("hexviewer-stream-") && name.endsWith(".bin"));
        return files == null ? 0 : files.length;
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;
        private TrackingInputStream(byte[] bytes) { super(bytes); }
        @Override public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static final class FileSnapshot {
        private final java.io.File file;
        private FileSnapshot(java.io.File file) { this.file = file; }
        private boolean exists() { return file.exists(); }
    }
}
