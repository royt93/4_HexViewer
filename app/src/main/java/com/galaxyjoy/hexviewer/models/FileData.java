/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * File data representation (used by MainActivity)
 * </p>
 *
 * @author Keidan
 * <p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.models;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.util.io.FileHelper;

import java.util.Locale;

public class FileData {
    protected static final String SEQUENTIAL_SEP = "^";
    protected static final String STREAMING_PREFIX = "@streaming@";
    private final String mName;
    private final Uri mUri;
    private boolean mOpenFromAppIntent;
    private long mStartOffset;
    private long mEndOffset;
    private long mSize;
    private long mRealSize;
    private boolean mIsNotFound;
    private boolean mIsAccessError;
    private boolean mIsSizeUnknown;
    private int mShiftOffset;
    private boolean mStreaming;
    private byte[] mStreamingWindowOriginal;

    public FileData(final Context ctx,
                    final Uri uri,
                    boolean openFromAppIntent) {
        this(ctx,
                uri,
                openFromAppIntent,
                0L,
                0L);
    }

    public FileData(final Context ctx,
                    final Uri uri,
                    boolean openFromAppIntent,
                    long startOffset,
                    long endOffset) {
        mShiftOffset = 0;
        mName = FileHelper.getFileName(ctx, uri);
        mUri = FileHelper.adjustUri(ctx, uri);
        mStartOffset = startOffset;
        mEndOffset = endOffset;
        mOpenFromAppIntent = openFromAppIntent;
        mRealSize = FileHelper.getFileSize(ctx, ctx.getContentResolver(), mUri);
        mIsSizeUnknown = mRealSize == FileHelper.FILE_SIZE_UNKNOWN;
        mStreaming = startOffset == 0L && endOffset == 0L
                && (mIsSizeUnknown
                || mRealSize > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE);
        if (isSequential())
            mSize = Math.abs(mEndOffset - mStartOffset);
        else
            mSize = mRealSize;

        /* We assume that if the file sizes are not <= 0, the file exists. */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !mIsSizeUnknown
                && (mSize <= 0 || mRealSize <= 0)) {
            DocumentFile sourceFile = DocumentFile.fromSingleUri(ctx, mUri);
            mIsNotFound = (sourceFile == null || !sourceFile.exists());
        }
        if (mIsNotFound) {
            mIsAccessError = false;
            mRealSize = 0;
            mSize = 0;
        } else {
            if (mRealSize == FileHelper.FILE_SIZE_NOT_FOUND) {
                mIsAccessError = false;
                mIsNotFound = true;
                mRealSize = 0;
                mSize = 0;
            } else if (mRealSize == FileHelper.FILE_SIZE_ACCESS_ERROR) {
                mIsAccessError = true;
                mIsNotFound = false;
                mRealSize = 0;
                mSize = 0;
            } else {
                mIsAccessError = false;
                mIsNotFound = false;
                if (mIsSizeUnknown) mSize = 0L;
            }
        }
        MyApplication.addLog(ctx,
                "FileData",
                String.format(Locale.US,
                        "%s size: %d, r_size: %d, s_off: %d, e_off: %d, shift: %d, fromIntent: %b, notFound: %b, accessError: %b",
                        mName,
                        mSize,
                        mRealSize,
                        mStartOffset,
                        mEndOffset,
                        mShiftOffset,
                        mOpenFromAppIntent,
                        mIsNotFound,
                        mIsAccessError));
    }

    /**
     * Sets the offset used to shift the text to the end of the line.
     *
     * @param shiftOffset The new value.
     */
    public void setShiftOffset(int shiftOffset) {
        mShiftOffset = shiftOffset;
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
     * Returns true if a file access error was raised.
     *
     * @return boolean
     */
    public boolean isAccessError() {
        return mIsAccessError;
    }

    /** Returns true when the provider opened successfully but did not expose a size. */
    public boolean isSizeUnknown() {
        return mIsSizeUnknown;
    }

    /** Applies a size discovered by a seekable channel or completed spool operation. */
    public void setResolvedRealSize(long resolvedSize) {
        if (resolvedSize < 0L) throw new IllegalArgumentException("resolvedSize must be >= 0");
        boolean transparentCandidate = mStreaming || !isSequential();
        mRealSize = resolvedSize;
        mIsSizeUnknown = false;
        mIsNotFound = false;
        mIsAccessError = false;
        if (!isSequential()) mSize = resolvedSize;
        mStreaming = transparentCandidate
                && resolvedSize > com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE;
    }

    /** Restores an auto-streaming recent item without restoring its transient resident window. */
    public static FileData restoreStreaming(Context context, Uri uri, boolean openFromAppIntent) {
        FileData result = new FileData(context, uri, openFromAppIntent);
        result.mStreaming = true;
        return result;
    }

    /**
     * Returns true if the file was not found.
     *
     * @return boolean
     */
    public boolean isNotFound() {
        return mIsNotFound;
    }

    /**
     * Tests if the file is opened from the application's intent.
     *
     * @return boolean
     */
    public boolean isOpenFromAppIntent() {
        return mOpenFromAppIntent;
    }

    /**
     * Clears the open from app intent flag.
     */
    public void clearOpenFromAppIntent() {
        mOpenFromAppIntent = false;
    }

    /**
     * Tests if the name is empty.
     *
     * @param fd FileData
     * @return boolean
     */
    public static boolean isEmpty(FileData fd) {
        return fd == null || fd.mName == null || fd.mName.isEmpty();
    }

    /**
     * Returns the file name.
     *
     * @return String
     */
    public String getName() {
        return mName;
    }

    @NonNull
    public String toString() {
        if (mStreaming) return STREAMING_PREFIX + mUri;
        String ret = mStartOffset + SEQUENTIAL_SEP + mEndOffset + SEQUENTIAL_SEP;
        ret += mUri.toString();
        return ret;
    }

    /**
     * Test if it's a sequential file or not.
     *
     * @return boolean
     */
    public boolean isSequential() {
        return mStartOffset != 0L || mEndOffset != 0L;
    }

    /** Returns whether this file was transparently switched to bounded-memory streaming. */
    public boolean isStreaming() {
        return mStreaming;
    }

    /**
     * Selects the resident range for a streaming file. Offsets are end-exclusive.
     */
    public void setStreamingWindow(long startOffset, long endOffset) {
        if (startOffset < 0L || endOffset < startOffset || endOffset > mRealSize) {
            throw new IllegalArgumentException("Invalid streaming window: " + startOffset + ".." + endOffset);
        }
        mStreaming = true;
        setOffsets(startOffset, endOffset, true);
        // The resident window changed; any previously captured snapshot no longer applies.
        mStreamingWindowOriginal = null;
    }

    /**
     * Captures the on-disk bytes of the currently resident streaming window at load time, so a
     * later save can detect external changes made at any point during the edit session rather
     * than only in the instant right before the save copy starts.
     */
    public void setStreamingWindowOriginal(byte[] original) {
        mStreamingWindowOriginal = original == null ? null : original.clone();
    }

    /**
     * Returns the snapshot captured by {@link #setStreamingWindowOriginal(byte[])}, or {@code null}
     * if none was captured for the current window.
     */
    public byte[] getStreamingWindowOriginal() {
        return mStreamingWindowOriginal == null ? null : mStreamingWindowOriginal.clone();
    }

    /**
     * Returns the file uri.
     *
     * @return Uri
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Returns the start offset.
     *
     * @return long
     */
    public long getStartOffset() {
        return mStartOffset;
    }

    /**
     * Returns the end offset.
     *
     * @return long
     */
    public long getEndOffset() {
        return mEndOffset;
    }

    /**
     * Sets the start offset.
     *
     * @param startOffset Start offset
     * @param endOffset   End offset
     * @param refreshSize Refresh the size?
     */
    public void setOffsets(long startOffset, long endOffset, boolean refreshSize) {
        mStartOffset = startOffset;
        mEndOffset = endOffset;
        if (refreshSize)
            mSize = Math.abs(mEndOffset - mStartOffset);
    }

    /**
     * Returns the file size.
     *
     * @return long
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Returns the real file size.
     *
     * @return long
     */
    public long getRealSize() {
        return mRealSize;
    }
}
