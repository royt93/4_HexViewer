/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Line entry.
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import java.util.AbstractList;
import java.util.List;
import com.galaxyjoy.hexviewer.util.SysHelper;

public class LineEntry {
    private String mPlain;
    private byte[] mRaw; // Performance: Use primitive byte array instead of List<Byte>
    private int mIndex;
    private boolean mUpdated;
    private int mShiftOffset;
    private byte mMaxByRow; // Store maxByRow to format plain string dynamically

    public LineEntry(final String plain, final List<Byte> raw) {
        mPlain = plain;
        mRaw = toPrimitive(raw);
        mShiftOffset = 0;
        mMaxByRow = 0;
    }

    // Performance: Private constructor, called via static factory to avoid overload ambiguity
    private LineEntry(final String plain, final byte[] raw) {
        mPlain = plain;
        mRaw = raw;
        mShiftOffset = 0;
        mMaxByRow = 0;
    }

    // Lazy constructor called via static factory when reading buffer
    private LineEntry(final byte[] raw, final int maxByRow) {
        mPlain = null;
        mRaw = raw;
        mMaxByRow = (byte) maxByRow;
        mShiftOffset = 0;
    }

    /**
     * Public factory method to create a LineEntry directly from a primitive byte array
     */
    public static LineEntry create(final String plain, final byte[] raw) {
        return new LineEntry(plain, raw);
    }

    /**
     * Public factory method to create a LineEntry with lazy plain text generation
     */
    public static LineEntry create(final byte[] raw, final int maxByRow) {
        return new LineEntry(raw, maxByRow);
    }

    public LineEntry(LineEntry le) {
        mPlain = le.mPlain;
        mRaw = le.mRaw != null ? le.mRaw.clone() : null;
        mIndex = le.mIndex;
        mUpdated = le.mUpdated;
        mShiftOffset = le.mShiftOffset;
        mMaxByRow = le.mMaxByRow;
    }

    /**
     * Helper to convert List<Byte> to primitive byte[]
     */
    private static byte[] toPrimitive(List<Byte> list) {
        if (list == null) return null;
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Byte b = list.get(i);
            bytes[i] = b != null ? b : 0;
        }
        return bytes;
    }

    /**
     * Sets the offset used to shift the text to the end of the line.
     *
     * @param shiftOffset The new value.
     */
    public void setShiftOffset(int shiftOffset) {
        mShiftOffset = shiftOffset;
        if (mMaxByRow != 0) {
            mPlain = null; // Force lazy re-generation with new shift offset
        }
    }

    /**
     * Returns the offset used to shift the text to the end of the line.
     *
     * @return int
     */
    public int getShiftOffset() {
        return mShiftOffset;
    }

    /**
     * Sets the values.
     *
     * @param plain Plain text
     * @param raw   Raw data.
     */
    public void setValues(String plain, List<Byte> raw) {
        mPlain = plain;
        mRaw = toPrimitive(raw);
    }

    /**
     * Sets the values.
     *
     * @param plain Plain text
     * @param raw   Raw data.
     */
    public void setValues(String plain, byte[] raw) {
        mPlain = plain;
        mRaw = raw;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return getPlain();
    }

    /**
     * Returns the plain value.
     *
     * @return String
     */
    public String getPlain() {
        if (mPlain == null && mRaw != null && mMaxByRow != 0) {
            mPlain = SysHelper.formatSingleLine(mRaw, mMaxByRow, mShiftOffset);
        }
        return mPlain;
    }

    /**
     * Returns the raw value as a List<Byte> via a lazy wrapper.
     * This avoids any object allocation during normal viewing.
     *
     * @return List<Byte>
     */
    public List<Byte> getRaw() {
        if (mRaw == null) return null;
        return new AbstractList<Byte>() {
            @Override
            public Byte get(int index) {
                return mRaw[index];
            }

            @Override
            public int size() {
                return mRaw.length;
            }

            @Override
            public Byte set(int index, Byte element) {
                byte old = mRaw[index];
                mRaw[index] = element != null ? element : 0;
                return old;
            }
        };
    }

    /**
     * Tests if the data is updated.
     *
     * @return boolean
     */
    public boolean isUpdated() {
        return mUpdated;
    }

    /**
     * Sets the data updated state.
     *
     * @param updated The new value.
     */
    public void setUpdated(boolean updated) {
        mUpdated = updated;
    }

    /**
     * Gets the origin index.
     *
     * @return int
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Sets the origin index.
     *
     * @param index The new value.
     */
    public void setIndex(int index) {
        mIndex = index;
    }
}
