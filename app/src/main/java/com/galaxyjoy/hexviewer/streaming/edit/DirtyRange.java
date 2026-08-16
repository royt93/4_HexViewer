package com.galaxyjoy.hexviewer.streaming.edit;

import java.util.Arrays;

/** A contiguous, fixed-length replacement at an absolute file offset. */
public final class DirtyRange {
    private final long mOffset;
    private final byte[] mOriginal;
    private final byte[] mReplacement;

    public DirtyRange(long offset, byte[] original, byte[] replacement) {
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
        if (original == null || replacement == null) {
            throw new IllegalArgumentException("range bytes must not be null");
        }
        if (original.length == 0 || original.length != replacement.length) {
            throw new IllegalArgumentException("dirty ranges must be non-empty and fixed-length");
        }
        mOffset = offset;
        mOriginal = original.clone();
        mReplacement = replacement.clone();
    }

    public long getOffset() {
        return mOffset;
    }

    public int getLength() {
        return mReplacement.length;
    }

    public long getEndOffsetExclusive() {
        return Math.addExact(mOffset, mReplacement.length);
    }

    public byte[] getOriginal() {
        return mOriginal.clone();
    }

    public byte[] getReplacement() {
        return mReplacement.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DirtyRange)) return false;
        DirtyRange other = (DirtyRange) obj;
        return mOffset == other.mOffset
                && Arrays.equals(mOriginal, other.mOriginal)
                && Arrays.equals(mReplacement, other.mReplacement);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(mOffset);
        result = 31 * result + Arrays.hashCode(mOriginal);
        result = 31 * result + Arrays.hashCode(mReplacement);
        return result;
    }
}
