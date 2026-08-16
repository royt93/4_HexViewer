package com.galaxyjoy.hexviewer.streaming.edit;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded-memory source-to-destination copy with sparse replacements applied in flight. */
public final class StreamingCopyHelper {
    public static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private StreamingCopyHelper() {
    }

    @FunctionalInterface
    public interface CancellationSignal {
        boolean isCancelled();
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(long bytesCopied, long totalBytes);
    }

    public static void copyAndApply(InputStream source, OutputStream destination, long sourceSize,
                                    List<DirtyRange> dirtyRanges) throws IOException {
        copyAndApply(source, destination, sourceSize, dirtyRanges, DEFAULT_BUFFER_SIZE,
                () -> false, null);
    }

    public static void copyAndApply(InputStream source, OutputStream destination, long sourceSize,
                                    List<DirtyRange> dirtyRanges, int bufferSize,
                                    CancellationSignal cancellation,
                                    ProgressListener progress) throws IOException {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("streams must not be null");
        }
        if (sourceSize < 0 || bufferSize <= 0) {
            throw new IllegalArgumentException("invalid source size or buffer size");
        }
        CancellationSignal safeCancellation = cancellation == null ? () -> false : cancellation;
        List<DirtyRange> ranges = validateAndSort(dirtyRanges, sourceSize);
        byte[] buffer = new byte[(int) Math.min(bufferSize, Math.max(1L, sourceSize))];
        long position = 0;
        int rangeIndex = 0;

        while (position < sourceSize) {
            if (safeCancellation.isCancelled()) {
                throw new InterruptedIOException("streaming copy cancelled");
            }
            int wanted = (int) Math.min(buffer.length, sourceSize - position);
            int count = readFully(source, buffer, wanted);
            if (count != wanted) throw new EOFException("source ended at byte " + (position + count));
            long chunkEnd = position + count;

            while (rangeIndex < ranges.size()
                    && ranges.get(rangeIndex).getEndOffsetExclusive() <= position) {
                rangeIndex++;
            }
            for (int i = rangeIndex; i < ranges.size(); i++) {
                DirtyRange range = ranges.get(i);
                if (range.getOffset() >= chunkEnd) break;
                long intersectionStart = Math.max(position, range.getOffset());
                long intersectionEnd = Math.min(chunkEnd, range.getEndOffsetExclusive());
                byte[] original = range.getOriginal();
                byte[] replacement = range.getReplacement();
                int rangeOffset = (int) (intersectionStart - range.getOffset());
                int chunkOffset = (int) (intersectionStart - position);
                int intersectionLength = (int) (intersectionEnd - intersectionStart);
                for (int j = 0; j < intersectionLength; j++) {
                    if (buffer[chunkOffset + j] != original[rangeOffset + j]) {
                        throw new SourceConflictException(intersectionStart + j);
                    }
                }
                System.arraycopy(replacement, rangeOffset, buffer, chunkOffset, intersectionLength);
            }

            destination.write(buffer, 0, count);
            position = chunkEnd;
            if (progress != null) progress.onProgress(position, sourceSize);
        }
        if (safeCancellation.isCancelled()) {
            throw new InterruptedIOException("streaming copy cancelled");
        }
        if (source.read() != -1) throw new IOException("source grew during streaming copy");
        destination.flush();
    }

    private static int readFully(InputStream source, byte[] buffer, int wanted) throws IOException {
        int total = 0;
        while (total < wanted) {
            int count = source.read(buffer, total, wanted - total);
            if (count < 0) break;
            if (count == 0) {
                int one = source.read();
                if (one < 0) break;
                buffer[total++] = (byte) one;
            } else {
                total += count;
            }
        }
        return total;
    }

    private static List<DirtyRange> validateAndSort(List<DirtyRange> dirtyRanges, long sourceSize) {
        List<DirtyRange> ranges = dirtyRanges == null
                ? new ArrayList<>() : new ArrayList<>(dirtyRanges);
        ranges.sort(Comparator.comparingLong(DirtyRange::getOffset));
        long previousEnd = 0;
        for (DirtyRange range : ranges) {
            if (range == null) throw new IllegalArgumentException("dirty range must not be null");
            if (range.getOffset() < previousEnd) {
                throw new IllegalArgumentException("dirty ranges overlap");
            }
            if (range.getEndOffsetExclusive() > sourceSize) {
                throw new IllegalArgumentException("dirty range exceeds source size");
            }
            previousEnd = range.getEndOffsetExclusive();
        }
        return ranges;
    }

    /** Raised when the source changed after a dirty range was created. */
    public static final class SourceConflictException extends IOException {
        private final long mOffset;

        public SourceConflictException(long offset) {
            super("source changed at byte " + offset);
            mOffset = offset;
        }

        public long getOffset() {
            return mOffset;
        }
    }
}
