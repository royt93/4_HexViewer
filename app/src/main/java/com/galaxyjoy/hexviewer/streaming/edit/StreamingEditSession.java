package com.galaxyjoy.hexviewer.streaming.edit;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Sparse, fixed-length edit model for files that are too large to keep in memory.
 * All positions are absolute byte offsets and therefore remain valid when UI windows change.
 */
public final class StreamingEditSession {
    private final long mFileSize;
    private final ByteSource mSource;
    private final NavigableMap<Long, Byte> mOverlay = new TreeMap<>();
    private final NavigableMap<Long, Byte> mOriginalDirtyBytes = new TreeMap<>();
    private final Deque<EditOperation> mUndo = new ArrayDeque<>();
    private final Deque<EditOperation> mRedo = new ArrayDeque<>();

    public StreamingEditSession(long fileSize, ByteSource source) {
        if (fileSize < 0) throw new IllegalArgumentException("fileSize must be >= 0");
        if (source == null) throw new IllegalArgumentException("source must not be null");
        mFileSize = fileSize;
        mSource = source;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public synchronized boolean isDirty() {
        return !mOverlay.isEmpty();
    }

    public synchronized boolean canUndo() {
        return !mUndo.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !mRedo.isEmpty();
    }

    /** Reads source bytes with all unsaved replacements overlaid. */
    public synchronized byte[] read(long offset, int length) throws IOException {
        validateRange(offset, length, true);
        byte[] result = readSourceExactly(offset, length);
        if (length == 0) return result;
        long end = offset + length;
        for (Map.Entry<Long, Byte> entry : mOverlay.subMap(offset, true, end, false).entrySet()) {
            result[(int) (entry.getKey() - offset)] = entry.getValue();
        }
        return result;
    }

    /** Replaces bytes after reading their current effective value. */
    public synchronized boolean replace(long offset, byte[] replacement) throws IOException {
        if (replacement == null) throw new IllegalArgumentException("replacement must not be null");
        return replace(offset, read(offset, replacement.length), replacement);
    }

    /**
     * Atomically performs a fixed-length replacement. The expected bytes protect the UI from
     * applying an edit to a stale window.
     */
    public synchronized boolean replace(long offset, byte[] expectedCurrent, byte[] replacement)
            throws IOException {
        if (expectedCurrent == null || replacement == null) {
            throw new IllegalArgumentException("edit bytes must not be null");
        }
        if (expectedCurrent.length == 0 || expectedCurrent.length != replacement.length) {
            throw new IllegalArgumentException("streaming edits must be non-empty and fixed-length");
        }
        validateRange(offset, replacement.length, false);
        byte[] current = read(offset, replacement.length);
        if (!java.util.Arrays.equals(current, expectedCurrent)) {
            throw new StaleEditException("bytes at offset " + offset + " no longer match the editor window");
        }
        if (java.util.Arrays.equals(current, replacement)) return false;

        EditOperation operation = new EditOperation(offset, current, replacement.clone());
        apply(operation.mOffset, operation.mAfter);
        mUndo.push(operation);
        mRedo.clear();
        return true;
    }

    public synchronized boolean undo() throws IOException {
        if (mUndo.isEmpty()) return false;
        EditOperation operation = mUndo.pop();
        apply(operation.mOffset, operation.mBefore);
        mRedo.push(operation);
        return true;
    }

    public synchronized boolean redo() throws IOException {
        if (mRedo.isEmpty()) return false;
        EditOperation operation = mRedo.pop();
        apply(operation.mOffset, operation.mAfter);
        mUndo.push(operation);
        return true;
    }

    /** Returns immutable, offset-sorted ranges with adjacent dirty bytes coalesced. */
    public synchronized List<DirtyRange> getDirtyRanges() {
        if (mOverlay.isEmpty()) return Collections.emptyList();
        List<DirtyRange> result = new ArrayList<>();
        long rangeStart = -1;
        long previous = -2;
        java.io.ByteArrayOutputStream original = new java.io.ByteArrayOutputStream();
        java.io.ByteArrayOutputStream replacement = new java.io.ByteArrayOutputStream();

        for (Map.Entry<Long, Byte> entry : mOverlay.entrySet()) {
            long offset = entry.getKey();
            if (rangeStart >= 0 && offset != previous + 1) {
                result.add(new DirtyRange(rangeStart, original.toByteArray(), replacement.toByteArray()));
                original.reset();
                replacement.reset();
                rangeStart = -1;
            }
            if (rangeStart < 0) rangeStart = offset;
            original.write(mOriginalDirtyBytes.get(offset));
            replacement.write(entry.getValue());
            previous = offset;
        }
        result.add(new DirtyRange(rangeStart, original.toByteArray(), replacement.toByteArray()));
        return Collections.unmodifiableList(result);
    }

    /** Clears edit history after the caller has durably saved all dirty ranges. */
    public synchronized void markSaved() {
        mOverlay.clear();
        mOriginalDirtyBytes.clear();
        mUndo.clear();
        mRedo.clear();
    }

    /** Discards all unsaved replacements and history. */
    public synchronized void discard() {
        markSaved();
    }

    private void apply(long offset, byte[] desired) throws IOException {
        byte[] original = readSourceExactly(offset, desired.length);
        for (int i = 0; i < desired.length; i++) {
            long absolute = offset + i;
            if (desired[i] == original[i]) {
                mOverlay.remove(absolute);
                mOriginalDirtyBytes.remove(absolute);
            } else {
                mOverlay.put(absolute, desired[i]);
                mOriginalDirtyBytes.putIfAbsent(absolute, original[i]);
            }
        }
    }

    private byte[] readSourceExactly(long offset, int length) throws IOException {
        byte[] bytes = mSource.read(offset, length);
        if (bytes == null || bytes.length != length) {
            throw new EOFException("source returned " + (bytes == null ? "null" : bytes.length)
                    + " bytes; expected " + length);
        }
        return bytes;
    }

    private void validateRange(long offset, int length, boolean allowEmpty) {
        if (offset < 0 || length < 0 || (!allowEmpty && length == 0)) {
            throw new IllegalArgumentException("invalid edit range");
        }
        long end;
        try {
            end = Math.addExact(offset, (long) length);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("range overflow", overflow);
        }
        if (end > mFileSize) throw new IndexOutOfBoundsException("range exceeds file size");
    }

    private static final class EditOperation {
        private final long mOffset;
        private final byte[] mBefore;
        private final byte[] mAfter;

        private EditOperation(long offset, byte[] before, byte[] after) {
            mOffset = offset;
            mBefore = before.clone();
            mAfter = after.clone();
        }
    }

    public static final class StaleEditException extends IOException {
        public StaleEditException(String message) {
            super(message);
        }
    }
}
