package com.galaxyjoy.hexviewer.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
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

    @Test
    public void zeroRequestedLengthReturnsEmptyAlignedWindow() {
        WindowRange range = WindowRange.around(1_000, 0, 10_000, 16);

        assertEquals(0, range.length());
        assertEquals(0, range.start() % 16);
    }

    @Test
    public void emptySourceReturnsEmptyWindowAtZero() {
        assertEquals(new WindowRange(0, 0), WindowRange.around(0, 0, 0, 16));
        assertEquals(new WindowRange(0, 0), WindowRange.around(0, 4_096, 0, 16));
    }

    @Test
    public void constructorRejectsEndBeforeStartOrNegativeStart() {
        assertThrows(IllegalArgumentException.class, () -> new WindowRange(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new WindowRange(10, 5));
    }

    @Test
    public void aroundRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> WindowRange.around(-1, 10, 100, 16));
        assertThrows(IllegalArgumentException.class, () -> WindowRange.around(0, -1, 100, 16));
        assertThrows(IllegalArgumentException.class, () -> WindowRange.around(0, 10, -1, 16));
        assertThrows(IllegalArgumentException.class, () -> WindowRange.around(0, 10, 100, 0));
    }

    @Test
    public void containsIsHalfOpenAtEnd() {
        WindowRange range = new WindowRange(10, 20);

        assertTrue(range.contains(10));
        assertTrue(range.contains(19));
        assertFalse(range.contains(20));
        assertFalse(range.contains(9));
    }
}
