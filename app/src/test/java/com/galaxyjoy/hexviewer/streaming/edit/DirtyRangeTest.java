package com.galaxyjoy.hexviewer.streaming.edit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class DirtyRangeTest {
    @Test
    public void constructor_rejectsEmptyRanges() {
        // Bug guard: TaskSave.saveStreamingCopy() builds a whole-window DirtyRange for every
        // streaming Save As, including a genuinely empty (0-byte) resident window (e.g. an
        // unknown-size content:// pipe that resolves to 0 bytes). This constructor rejects an
        // empty range outright, so that Save As path fails with "dirty ranges must be
        // non-empty and fixed-length" instead of writing an empty destination file.
        assertThrows(IllegalArgumentException.class,
                () -> new DirtyRange(0, new byte[0], new byte[0]));
    }

    @Test
    public void constructor_rejectsMismatchedLengths() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirtyRange(0, new byte[]{1, 2}, new byte[]{9}));
    }

    @Test
    public void constructor_rejectsNegativeOffset() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirtyRange(-1, new byte[]{1}, new byte[]{2}));
    }

    @Test
    public void constructor_rejectsNullBytes() {
        assertThrows(IllegalArgumentException.class,
                () -> new DirtyRange(0, null, new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> new DirtyRange(0, new byte[]{1}, null));
    }

    @Test
    public void getters_returnDefensiveCopies() {
        byte[] original = {1, 2, 3};
        byte[] replacement = {9, 8, 7};
        DirtyRange range = new DirtyRange(10, original, replacement);

        original[0] = 99;
        replacement[0] = 99;
        range.getOriginal()[1] = 55;
        range.getReplacement()[1] = 55;

        assertArrayEquals(new byte[]{1, 2, 3}, range.getOriginal());
        assertArrayEquals(new byte[]{9, 8, 7}, range.getReplacement());
    }

    @Test
    public void getEndOffsetExclusive_isOffsetPlusLength() {
        DirtyRange range = new DirtyRange(100, new byte[]{1, 2, 3}, new byte[]{4, 5, 6});

        assertEquals(103, range.getEndOffsetExclusive());
        assertEquals(3, range.getLength());
    }

    @Test
    public void getEndOffsetExclusive_overflowThrows() {
        DirtyRange range = new DirtyRange(Long.MAX_VALUE - 1, new byte[]{1, 2, 3},
                new byte[]{4, 5, 6});

        assertThrows(ArithmeticException.class, range::getEndOffsetExclusive);
    }

    @Test
    public void equalsAndHashCode_compareOffsetAndBytes() {
        DirtyRange a = new DirtyRange(5, new byte[]{1, 2}, new byte[]{3, 4});
        DirtyRange b = new DirtyRange(5, new byte[]{1, 2}, new byte[]{3, 4});
        DirtyRange differentOffset = new DirtyRange(6, new byte[]{1, 2}, new byte[]{3, 4});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        org.junit.Assert.assertNotEquals(a, differentOffset);
    }
}
