package com.galaxyjoy.hexviewer.ui.task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.net.Uri;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskSaveSafetyTest {
    @Test
    public void fixedLengthValidation_rejectsMissingWindowAndResize() {
        IOException missingWindow = assertThrows(IOException.class,
                () -> TaskSave.validateWindowLength(true, false, 128, 128));
        assertTrue(missingWindow.getMessage().contains("no resident window"));

        IOException resize = assertThrows(IOException.class,
                () -> TaskSave.validateWindowLength(true, true, 128, 127));
        assertTrue(resize.getMessage().contains("Fixed-length"));
    }

    @Test
    public void fixedLengthValidation_acceptsExactWindow() throws Exception {
        TaskSave.validateWindowLength(true, true, 128, 128);
        TaskSave.validateWindowLength(false, false, 0, 42);
    }

    @Test
    public void failureMessage_isNeverNullForMessageLessProviderFailure() {
        assertTrue(!TaskSave.failureMessage(new SecurityException()).trim().isEmpty());
        assertTrue(TaskSave.failureMessage(new SecurityException()).contains("SecurityException"));
        assertTrue(TaskSave.failureMessage(null).contains("Unknown"));
    }

    @Test
    public void transactionalCommit_successReplacesCompleteDestination() throws Exception {
        Uri destination = mock(Uri.class);
        Uri staging = mock(Uri.class);
        Uri backup = mock(Uri.class);
        FakeStreams streams = new FakeStreams();
        byte[] oldDestination = pattern(511, 3);
        byte[] completeSaveAs = pattern(1024 * 1024 + 37, 9);
        streams.put(destination, oldDestination);
        streams.put(staging, completeSaveAs);
        streams.put(backup, new byte[0]);

        TaskSave.transactionalCommit(destination, staging, backup, streams, () -> false);

        assertArrayEquals(completeSaveAs, streams.get(destination));
        assertArrayEquals(oldDestination, streams.get(backup));
    }

    @Test
    public void transactionalCommit_writeErrorRollsBackExistingDestination() {
        Uri destination = mock(Uri.class);
        Uri staging = mock(Uri.class);
        Uri backup = mock(Uri.class);
        FakeStreams streams = new FakeStreams();
        byte[] oldDestination = pattern(4096, 4);
        streams.put(destination, oldDestination);
        streams.put(staging, pattern(8192, 8));
        streams.put(backup, new byte[0]);
        streams.failNextDestinationWrite(destination);

        assertThrows(IOException.class,
                () -> TaskSave.transactionalCommit(destination, staging, backup, streams,
                        () -> false));

        assertArrayEquals(oldDestination, streams.get(destination));
    }

    @Test
    public void transactionalCommit_runtimeProviderFailureRollsBackAndRethrowsSameType() {
        Uri destination = mock(Uri.class);
        Uri staging = mock(Uri.class);
        Uri backup = mock(Uri.class);
        FakeStreams streams = new FakeStreams();
        byte[] oldDestination = pattern(4096, 12);
        streams.put(destination, oldDestination);
        streams.put(staging, pattern(8192, 15));
        streams.put(backup, new byte[0]);
        streams.failNextDestinationWriteWithSecurityException(destination);

        assertThrows(SecurityException.class,
                () -> TaskSave.transactionalCommit(destination, staging, backup, streams,
                        () -> false));

        assertArrayEquals(oldDestination, streams.get(destination));
    }

    @Test
    public void transactionalCommit_rollbackFailureIsSuppressedOnPrimaryRuntimeFailure() {
        Uri destination = mock(Uri.class);
        Uri staging = mock(Uri.class);
        Uri backup = mock(Uri.class);
        Map<Uri, byte[]> inputs = new HashMap<>();
        inputs.put(destination, pattern(8, 1));
        inputs.put(staging, pattern(8, 2));
        inputs.put(backup, pattern(8, 1));
        AtomicInteger destinationOpens = new AtomicInteger();
        TaskSave.TransactionStreams broken = new TaskSave.TransactionStreams() {
            @Override
            public InputStream openInput(Uri uri) {
                return new ByteArrayInputStream(inputs.get(uri));
            }

            @Override
            public OutputStream openTruncatedOutput(Uri uri) {
                if (uri == destination) {
                    int attempt = destinationOpens.incrementAndGet();
                    return new OutputStream() {
                        @Override
                        public void write(int value) {
                            throw attempt == 1
                                    ? new SecurityException("commit denied")
                                    : new IllegalStateException("rollback denied");
                        }
                    };
                }
                return new ByteArrayOutputStream();
            }
        };

        SecurityException failure = assertThrows(SecurityException.class,
                () -> TaskSave.transactionalCommit(destination, staging, backup, broken,
                        () -> false));

        assertTrue(failure.getMessage().contains("commit"));
        assertTrue(failure.getSuppressed().length == 1);
        assertTrue(failure.getSuppressed()[0] instanceof IllegalStateException);
    }

    @Test
    public void transactionalCommit_cancelRollsBackExistingDestination() {
        Uri destination = mock(Uri.class);
        Uri staging = mock(Uri.class);
        Uri backup = mock(Uri.class);
        FakeStreams streams = new FakeStreams();
        byte[] oldDestination = pattern(512 * 1024, 1);
        streams.put(destination, oldDestination);
        streams.put(staging, pattern(700 * 1024, 7));
        streams.put(backup, new byte[0]);
        AtomicInteger checks = new AtomicInteger();

        assertThrows(InterruptedIOException.class,
                () -> TaskSave.transactionalCommit(destination, staging, backup, streams,
                        () -> checks.incrementAndGet() > 1));

        assertArrayEquals(oldDestination, streams.get(destination));
    }

    private static byte[] pattern(int size, int seed) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) data[i] = (byte) (seed + i * 31);
        return data;
    }

    private static final class FakeStreams implements TaskSave.TransactionStreams {
        private final Map<Uri, byte[]> mFiles = new HashMap<>();
        private final AtomicBoolean mFailDestinationOnce = new AtomicBoolean();
        private Uri mFailureDestination;
        private boolean mRuntimeFailure;

        void put(Uri uri, byte[] data) {
            mFiles.put(uri, data.clone());
        }

        byte[] get(Uri uri) {
            return mFiles.get(uri).clone();
        }

        void failNextDestinationWrite(Uri uri) {
            mFailureDestination = uri;
            mRuntimeFailure = false;
            mFailDestinationOnce.set(true);
        }

        void failNextDestinationWriteWithSecurityException(Uri uri) {
            mFailureDestination = uri;
            mRuntimeFailure = true;
            mFailDestinationOnce.set(true);
        }

        @Override
        public InputStream openInput(Uri uri) throws IOException {
            byte[] data = mFiles.get(uri);
            if (data == null) throw new IOException("missing input");
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream openTruncatedOutput(Uri uri) {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            return new FilterOutputStream(sink) {
                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    if (uri == mFailureDestination && mFailDestinationOnce.compareAndSet(true, false)) {
                        int partial = Math.min(3, length);
                        out.write(bytes, offset, partial);
                        if (mRuntimeFailure) throw new SecurityException();
                        throw new IOException("injected destination failure");
                    }
                    out.write(bytes, offset, length);
                }

                @Override
                public void close() throws IOException {
                    super.close();
                    mFiles.put(uri, sink.toByteArray());
                }
            };
        }
    }
}
