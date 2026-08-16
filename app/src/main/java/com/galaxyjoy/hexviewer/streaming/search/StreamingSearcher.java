package com.galaxyjoy.hexviewer.streaming.search;

import java.io.IOException;
import java.util.OptionalLong;

/** Bounded-memory byte-pattern search over a random-access source. */
public final class StreamingSearcher {
    public static final int DEFAULT_CHUNK_SIZE = 1024 * 1024;
    private static final int MAX_CONSECUTIVE_ZERO_READS = 3;

    private final int mChunkSize;

    public StreamingSearcher() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public StreamingSearcher(int chunkSize) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        mChunkSize = chunkSize;
    }

    @FunctionalInterface
    public interface RandomAccessReader {
        /** Returns bytes read, -1 for EOF, or 0 for a temporarily non-progressing read. */
        int read(long absoluteOffset, byte[] destination, int destinationOffset, int length)
                throws IOException;
    }

    @FunctionalInterface
    public interface MatchCallback {
        /** Return false to stop after this match. */
        boolean onMatch(long absoluteOffset);
    }

    @FunctionalInterface
    public interface CancellationSignal {
        boolean isCancelled();
    }

    public enum Status {
        COMPLETED,
        CANCELLED,
        STOPPED_BY_CALLBACK
    }

    public static final class SearchSummary {
        private final Status mStatus;
        private final long mMatchCount;
        private final long mBytesRead;

        private SearchSummary(Status status, long matchCount, long bytesRead) {
            mStatus = status;
            mMatchCount = matchCount;
            mBytesRead = bytesRead;
        }

        public Status getStatus() {
            return mStatus;
        }

        public long getMatchCount() {
            return mMatchCount;
        }

        public long getBytesRead() {
            return mBytesRead;
        }
    }

    /** Searches {@code [startOffset, endOffsetExclusive)} and emits matches incrementally. */
    public SearchSummary search(RandomAccessReader reader, long startOffset, long endOffsetExclusive,
                                byte[] pattern, MatchCallback callback,
                                CancellationSignal cancellation) throws IOException {
        validate(reader, startOffset, endOffsetExclusive, pattern, callback);
        CancellationSignal safeCancellation = cancellation == null ? () -> false : cancellation;
        if (startOffset == endOffsetExclusive || pattern.length > endOffsetExclusive - startOffset) {
            return new SearchSummary(safeCancellation.isCancelled() ? Status.CANCELLED : Status.COMPLETED,
                    0, 0);
        }

        int overlapSize = pattern.length - 1;
        int capacity;
        try {
            capacity = Math.addExact(mChunkSize, overlapSize);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("pattern is too large", overflow);
        }
        byte[] buffer = new byte[capacity];
        int[] prefix = buildPrefixTable(pattern);
        long readPosition = startOffset;
        long nextCandidateOffset = startOffset;
        long bytesRead = 0;
        long matches = 0;
        int carry = 0;
        int zeroReads = 0;

        while (readPosition < endOffsetExclusive) {
            if (safeCancellation.isCancelled()) {
                return new SearchSummary(Status.CANCELLED, matches, bytesRead);
            }
            int wanted = (int) Math.min(mChunkSize, endOffsetExclusive - readPosition);
            int count = reader.read(readPosition, buffer, carry, wanted);
            if (count < -1 || count > wanted) {
                throw new IOException("reader returned invalid byte count: " + count);
            }
            if (count == 0) {
                if (++zeroReads >= MAX_CONSECUTIVE_ZERO_READS) {
                    throw new IOException("reader made no progress at offset " + readPosition);
                }
                continue;
            }
            if (count < 0) break;
            zeroReads = 0;
            bytesRead += count;

            int total = carry + count;
            long bufferStartOffset = readPosition - carry;
            int matched = 0;
            for (int i = 0; i < total; i++) {
                if ((i & 0x0fff) == 0 && safeCancellation.isCancelled()) {
                    return new SearchSummary(Status.CANCELLED, matches, bytesRead);
                }
                while (matched > 0 && buffer[i] != pattern[matched]) matched = prefix[matched - 1];
                if (buffer[i] == pattern[matched]) matched++;
                if (matched == pattern.length) {
                    long absolute = bufferStartOffset + i - pattern.length + 1L;
                    if (absolute >= nextCandidateOffset
                            && absolute + pattern.length <= endOffsetExclusive) {
                        matches++;
                        if (!callback.onMatch(absolute)) {
                            return new SearchSummary(Status.STOPPED_BY_CALLBACK, matches, bytesRead);
                        }
                    }
                    matched = prefix[matched - 1];
                }
            }

            int searchableStarts = total - pattern.length + 1;
            if (searchableStarts > 0) {
                nextCandidateOffset = Math.max(nextCandidateOffset,
                        bufferStartOffset + searchableStarts);
            }
            carry = Math.min(overlapSize, total);
            if (carry > 0) System.arraycopy(buffer, total - carry, buffer, 0, carry);
            readPosition += count;
        }
        return new SearchSummary(Status.COMPLETED, matches, bytesRead);
    }

    /** Convenience method that stops at the first hit. */
    public OptionalLong findNext(RandomAccessReader reader, long startOffset, long endOffsetExclusive,
                                 byte[] pattern, CancellationSignal cancellation) throws IOException {
        final long[] hit = {-1};
        SearchSummary summary = search(reader, startOffset, endOffsetExclusive, pattern, offset -> {
            hit[0] = offset;
            return false;
        }, cancellation);
        if (summary.getStatus() == Status.CANCELLED) return OptionalLong.empty();
        return hit[0] < 0 ? OptionalLong.empty() : OptionalLong.of(hit[0]);
    }

    private static int[] buildPrefixTable(byte[] pattern) {
        int[] prefix = new int[pattern.length];
        int matched = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (matched > 0 && pattern[i] != pattern[matched]) matched = prefix[matched - 1];
            if (pattern[i] == pattern[matched]) prefix[i] = ++matched;
        }
        return prefix;
    }

    private static void validate(RandomAccessReader reader, long startOffset, long endOffsetExclusive,
                                 byte[] pattern, MatchCallback callback) {
        if (reader == null || callback == null) {
            throw new IllegalArgumentException("reader and callback must not be null");
        }
        if (pattern == null || pattern.length == 0) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
        if (startOffset < 0 || endOffsetExclusive < startOffset) {
            throw new IllegalArgumentException("invalid search range");
        }
    }
}
