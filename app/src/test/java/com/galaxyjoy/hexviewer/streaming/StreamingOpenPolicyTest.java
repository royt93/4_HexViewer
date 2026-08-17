package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StreamingOpenPolicyTest {
    private final StreamingOpenPolicy policy = new StreamingOpenPolicy();

    @Test
    public void choosesFullAtThirtyMiBBoundary() {
        assertEquals(StreamingOpenPolicy.Mode.FULL,
                policy.choose(StreamingOpenPolicy.DEFAULT_FULL_OPEN_LIMIT));
    }

    @Test
    public void choosesStreamingAboveBoundaryAndForUnknownSize() {
        assertEquals(StreamingOpenPolicy.Mode.STREAMING,
                policy.choose(StreamingOpenPolicy.DEFAULT_FULL_OPEN_LIMIT + 1));
        assertEquals(StreamingOpenPolicy.Mode.STREAMING, policy.choose(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeThreshold() {
        new StreamingOpenPolicy(-1);
    }

    /**
     * Regression test: DEFAULT_FULL_OPEN_LIMIT used to hardcode its own "30L * 1024 * 1024"
     * literal instead of referencing AppConstants.MAX_NORMAL_FILE_SIZE (the threshold the real
     * open path in TaskOpen actually uses), so the two could silently drift apart. Now
     * DEFAULT_FULL_OPEN_LIMIT is defined in terms of AppConstants.MAX_NORMAL_FILE_SIZE.
     */
    @Test
    public void defaultLimitMatchesAppConstantsSharedThreshold() {
        assertEquals(com.galaxyjoy.hexviewer.constants.AppConstants.MAX_NORMAL_FILE_SIZE,
                StreamingOpenPolicy.DEFAULT_FULL_OPEN_LIMIT);
    }
}
