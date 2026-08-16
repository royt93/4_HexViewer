package com.galaxyjoy.hexviewer.streaming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Seekable source backed by a private cache file copied from a forward-only stream.
 * The cache file is always removed when the source is closed or construction fails.
 */
public final class SpoolingSeekableDataSource implements SeekableDataSource {
    public static final int DEFAULT_COPY_BUFFER_SIZE = 256 * 1024;
    public static final long DEFAULT_RESERVED_FREE_BYTES = 8L * 1024L * 1024L;
    private static final long FREE_SPACE_RECHECK_INTERVAL = 8L * 1024L * 1024L;

    public interface ProgressListener {
        void onProgress(long copiedBytes, long expectedBytes);
    }

    public interface CancellationChecker {
        boolean isCancelled();
    }

    public static final class SpoolingCancelledException extends IOException {
        public SpoolingCancelledException() {
            super("Spooling was cancelled");
        }
    }

    private final File cacheFile;
    private final FileChannelSeekableDataSource delegate;
    private boolean closed;

    public static SpoolingSeekableDataSource spool(
            InputStream input,
            File cacheDirectory,
            long expectedSize,
            ProgressListener progressListener,
            CancellationChecker cancellationChecker) throws IOException {
        return spool(input, cacheDirectory, expectedSize, DEFAULT_RESERVED_FREE_BYTES,
                DEFAULT_COPY_BUFFER_SIZE, progressListener, cancellationChecker);
    }

    public static SpoolingSeekableDataSource spool(
            InputStream input,
            File cacheDirectory,
            long expectedSize,
            long reservedFreeBytes,
            int copyBufferSize,
            ProgressListener progressListener,
            CancellationChecker cancellationChecker) throws IOException {
        Objects.requireNonNull(input, "input");
        validateArguments(cacheDirectory, expectedSize, reservedFreeBytes, copyBufferSize);
        File temporary = null;
        boolean success = false;
        boolean inputClosed = false;
        Throwable primaryFailure = null;
        try {
            ensureCacheDirectory(cacheDirectory);
            precheckFreeSpace(cacheDirectory, expectedSize, reservedFreeBytes);
            temporary = File.createTempFile("hexviewer-stream-", ".bin", cacheDirectory);
            copy(input, temporary, expectedSize, reservedFreeBytes, copyBufferSize,
                    progressListener, cancellationChecker);
            input.close();
            inputClosed = true;
            SpoolingSeekableDataSource source = new SpoolingSeekableDataSource(temporary);
            success = true;
            return source;
        } catch (IOException | RuntimeException | Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            IOException closeFailure = null;
            try {
                if (!inputClosed) input.close();
            } catch (IOException error) {
                closeFailure = error;
            }
            if (!success) deleteTemporaryFile(temporary);
            if (closeFailure != null) {
                if (primaryFailure != null) primaryFailure.addSuppressed(closeFailure);
                else throw closeFailure;
            }
        }
    }

    private SpoolingSeekableDataSource(File cacheFile) throws IOException {
        this.cacheFile = cacheFile;
        FileChannelSeekableDataSource opened = null;
        try {
            opened = FileChannelSeekableDataSource.open(cacheFile);
            this.delegate = opened;
        } catch (IOException | RuntimeException error) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (IOException closeError) {
                    error.addSuppressed(closeError);
                }
            }
            deleteTemporaryFile(cacheFile);
            throw error;
        }
    }

    private static void validateArguments(File cacheDirectory, long expectedSize,
                                          long reservedFreeBytes, int copyBufferSize) {
        Objects.requireNonNull(cacheDirectory, "cacheDirectory");
        if (expectedSize < -1) throw new IllegalArgumentException("expectedSize must be >= -1");
        if (reservedFreeBytes < 0) throw new IllegalArgumentException("reservedFreeBytes must be >= 0");
        if (copyBufferSize <= 0) throw new IllegalArgumentException("copyBufferSize must be > 0");
    }

    private static void ensureCacheDirectory(File cacheDirectory) throws IOException {
        if (!cacheDirectory.isDirectory()) {
            throw new IOException("Cache directory is unavailable: " + cacheDirectory);
        }
    }

    private static void precheckFreeSpace(File directory, long expectedSize,
                                          long reservedFreeBytes) throws IOException {
        if (expectedSize < 0) return;
        long required = saturatingAdd(expectedSize, reservedFreeBytes);
        if (directory.getUsableSpace() < required) {
            throw new IOException("Insufficient cache space: need " + required
                    + " bytes, available " + directory.getUsableSpace() + " bytes");
        }
    }

    private static void copy(InputStream input, File destination, long expectedSize,
                             long reservedFreeBytes, int bufferSize,
                             ProgressListener progressListener,
                             CancellationChecker cancellationChecker) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long copied = 0;
        long nextSpaceCheck = 0;
        notifyProgress(progressListener, 0, expectedSize);
        try (FileOutputStream output = new FileOutputStream(destination)) {
            while (true) {
                throwIfCancelled(cancellationChecker);
                if (copied >= nextSpaceCheck) {
                    ensureSpaceDuringCopy(destination.getParentFile(), reservedFreeBytes, bufferSize);
                    nextSpaceCheck = saturatingAdd(copied, FREE_SPACE_RECHECK_INTERVAL);
                }
                int read = input.read(buffer);
                if (read < 0) break;
                if (read == 0) {
                    int singleByte = input.read();
                    if (singleByte < 0) break;
                    output.write(singleByte);
                    copied++;
                } else {
                    output.write(buffer, 0, read);
                    copied = Math.addExact(copied, read);
                }
                notifyProgress(progressListener, copied, expectedSize);
            }
            output.getFD().sync();
        } catch (ArithmeticException error) {
            throw new IOException("Spool size overflow", error);
        }
        throwIfCancelled(cancellationChecker);
        notifyProgress(progressListener, copied, expectedSize);
    }

    private static void ensureSpaceDuringCopy(File directory, long reservedFreeBytes,
                                              int nextWriteBytes) throws IOException {
        long required = saturatingAdd(reservedFreeBytes, nextWriteBytes);
        long available = directory.getUsableSpace();
        if (available < required) {
            throw new IOException("Insufficient cache space while spooling: need " + required
                    + " bytes free, available " + available + " bytes");
        }
    }

    private static void throwIfCancelled(CancellationChecker checker)
            throws SpoolingCancelledException {
        if (checker != null && checker.isCancelled()) throw new SpoolingCancelledException();
    }

    private static void notifyProgress(ProgressListener listener, long copied, long expected) {
        if (listener != null) listener.onProgress(copied, expected);
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static void deleteTemporaryFile(File file) {
        if (file != null && file.exists() && !file.delete()) file.deleteOnExit();
    }

    File cacheFileForTesting() {
        return cacheFile;
    }

    /** Transfers ownership of the completed cache file to a longer-lived cache. */
    File detachCacheFile() throws IOException {
        ensureOpen();
        closed = true;
        try {
            delegate.close();
            return cacheFile;
        } catch (IOException error) {
            deleteTemporaryFile(cacheFile);
            throw error;
        }
    }

    @Override
    public long size() throws IOException {
        ensureOpen();
        return delegate.size();
    }

    @Override
    public int readAt(long position, byte[] destination, int offset, int length) throws IOException {
        ensureOpen();
        return delegate.readAt(position, destination, offset, length);
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Spooling data source is closed");
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        IOException failure = null;
        try {
            delegate.close();
        } catch (IOException error) {
            failure = error;
        } finally {
            if (cacheFile.exists() && !cacheFile.delete()) {
                cacheFile.deleteOnExit();
                IOException deleteError = new IOException("Unable to delete spool file: " + cacheFile);
                if (failure == null) failure = deleteError;
                else failure.addSuppressed(deleteError);
            }
        }
        if (failure != null) throw failure;
    }
}
