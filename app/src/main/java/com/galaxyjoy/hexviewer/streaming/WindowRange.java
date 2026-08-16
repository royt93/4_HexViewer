package com.galaxyjoy.hexviewer.streaming;

import java.util.Objects;

/** Immutable, end-exclusive byte range for a materialized streaming window. */
public final class WindowRange {
    private final long start;
    private final long endExclusive;

    public WindowRange(long start, long endExclusive) {
        if (start < 0 || endExclusive < start) {
            throw new IllegalArgumentException("Invalid window [" + start + ", " + endExclusive + ")");
        }
        this.start = start;
        this.endExclusive = endExclusive;
    }

    public static WindowRange around(long anchor, long requestedLength, long sourceSize, int alignment) {
        if (anchor < 0 || requestedLength < 0 || sourceSize < 0 || alignment <= 0) {
            throw new IllegalArgumentException("Invalid range arguments");
        }
        long clampedAnchor = Math.min(anchor, sourceSize);
        long half = requestedLength / 2;
        long candidateStart = clampedAnchor > half ? clampedAnchor - half : 0;
        long alignedStart = candidateStart - candidateStart % alignment;
        long maxStart = sourceSize > requestedLength ? sourceSize - requestedLength : 0;
        boolean pinnedToEnd = alignedStart > maxStart;
        if (pinnedToEnd) {
            alignedStart = maxStart - maxStart % alignment;
        }
        // Alignment can extend an end-pinned window by at most alignment - 1 bytes.
        long end = pinnedToEnd
                ? sourceSize
                : Math.min(sourceSize, saturatingAdd(alignedStart, requestedLength));
        return new WindowRange(alignedStart, end);
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public long start() {
        return start;
    }

    public long endExclusive() {
        return endExclusive;
    }

    public long length() {
        return endExclusive - start;
    }

    public boolean contains(long position) {
        return position >= start && position < endExclusive;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WindowRange)) return false;
        WindowRange that = (WindowRange) other;
        return start == that.start && endExclusive == that.endExclusive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, endExclusive);
    }

    @Override
    public String toString() {
        return "[" + start + ", " + endExclusive + ")";
    }
}
