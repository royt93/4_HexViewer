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
}
