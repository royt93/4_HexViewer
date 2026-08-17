package com.galaxyjoy.hexviewer.streaming;

import com.galaxyjoy.hexviewer.constants.AppConstants;

/** Chooses a bounded-memory open mode from source metadata. */
public final class StreamingOpenPolicy {
    public static final long DEFAULT_FULL_OPEN_LIMIT = AppConstants.MAX_NORMAL_FILE_SIZE;

    public enum Mode {
        FULL,
        STREAMING
    }

    private final long fullOpenLimit;

    public StreamingOpenPolicy() {
        this(DEFAULT_FULL_OPEN_LIMIT);
    }

    public StreamingOpenPolicy(long fullOpenLimit) {
        if (fullOpenLimit < 0) {
            throw new IllegalArgumentException("fullOpenLimit must be >= 0");
        }
        this.fullOpenLimit = fullOpenLimit;
    }

    public Mode choose(long sourceSize) {
        // Unknown-size providers are never safe to load entirely into the heap.
        return sourceSize >= 0 && sourceSize <= fullOpenLimit ? Mode.FULL : Mode.STREAMING;
    }

    public long fullOpenLimit() {
        return fullOpenLimit;
    }
}
