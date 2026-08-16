package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WindowRangeTest {
    @Test
    public void centersAndAlignsWindow() {
        WindowRange range = WindowRange.around(10_000, 4_096, 100_000, 16);

        assertEquals(0, range.start() % 16);
        assertEquals(4_096, range.length());
        assertTrue(range.contains(10_000));
    }

    @Test
    public void clampsWindowAtBothEnds() {
        assertEquals(new WindowRange(0, 4_096), WindowRange.around(0, 4_096, 10_000, 16));
        assertEquals(new WindowRange(5_904, 10_000), WindowRange.around(10_000, 4_096, 10_000, 16));
    }

    @Test
    public void supportsAnchorsBeyondTwoGiB() {
        long anchor = 3L * 1024 * 1024 * 1024;
        WindowRange range = WindowRange.around(anchor, 128 * 1024, anchor + 1024 * 1024, 16);

        assertTrue(range.start() > Integer.MAX_VALUE);
        assertTrue(range.contains(anchor));
        assertEquals(0, range.start() % 16);
    }

    @Test
    public void sourceSmallerThanRequestedWindowReturnsWholeSource() {
        assertEquals(new WindowRange(0, 99), WindowRange.around(50, 1_000, 99, 16));
    }

    @Test
    public void endPinnedAlignedWindowIncludesFinalUnalignedByte() {
        WindowRange range = WindowRange.around(10_001, 4_096, 10_001, 16);

        assertEquals(0, range.start() % 16);
        assertEquals(10_001, range.endExclusive());
    }
}
