package com.galaxyjoy.hexviewer.streaming.edit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class StreamingEditSessionTest {
    @Test
    public void replace_createsAbsoluteCoalescedDirtyRanges() throws Exception {
        byte[] source = sequence(20);
        StreamingEditSession session = session(source);

        session.replace(3, new byte[]{3, 4}, new byte[]{30, 40});
        session.replace(5, new byte[]{5}, new byte[]{50});
        session.replace(10, new byte[]{10}, new byte[]{100});

        List<DirtyRange> ranges = session.getDirtyRanges();
        assertEquals(2, ranges.size());
        assertEquals(3, ranges.get(0).getOffset());
        assertArrayEquals(new byte[]{3, 4, 5}, ranges.get(0).getOriginal());
        assertArrayEquals(new byte[]{30, 40, 50}, ranges.get(0).getReplacement());
        assertEquals(10, ranges.get(1).getOffset());
    }

    @Test
    public void read_appliesSparseOverlayWithoutMutatingSource() throws Exception {
        byte[] source = sequence(8);
        StreamingEditSession session = session(source);
        session.replace(2, new byte[]{22, 33});

        assertArrayEquals(new byte[]{0, 1, 22, 33, 4, 5, 6, 7}, session.read(0, 8));
        assertArrayEquals(sequence(8), source);
    }

    @Test
    public void replace_sameBytes_isNoOpAndDoesNotClearRedo() throws Exception {
        StreamingEditSession session = session(sequence(8));
        session.replace(1, new byte[]{11});
        session.undo();
        assertTrue(session.canRedo());

        assertFalse(session.replace(2, new byte[]{2}, new byte[]{2}));
        assertTrue(session.canRedo());
        assertFalse(session.isDirty());
    }

    @Test
    public void replace_newEditClearsRedo() throws Exception {
        StreamingEditSession session = session(sequence(8));
        session.replace(1, new byte[]{11});
        session.undo();

        session.replace(2, new byte[]{22});

        assertFalse(session.canRedo());
        assertTrue(session.canUndo());
    }

    @Test
    public void undoRedo_overlappingEditsRestoreEveryState() throws Exception {
        StreamingEditSession session = session(new byte[]{0, 0, 0, 0});
        session.replace(1, new byte[]{1, 1});
        session.replace(2, new byte[]{2, 2});
        assertArrayEquals(new byte[]{0, 1, 2, 2}, session.read(0, 4));

        assertTrue(session.undo());
        assertArrayEquals(new byte[]{0, 1, 1, 0}, session.read(0, 4));
        assertTrue(session.undo());
        assertArrayEquals(new byte[]{0, 0, 0, 0}, session.read(0, 4));
        assertFalse(session.isDirty());

        assertTrue(session.redo());
        assertTrue(session.redo());
        assertArrayEquals(new byte[]{0, 1, 2, 2}, session.read(0, 4));
    }

    @Test
    public void replace_rejectsStaleWindowBytes() {
        StreamingEditSession session = session(sequence(8));
        assertThrows(StreamingEditSession.StaleEditException.class,
                () -> session.replace(2, new byte[]{99}, new byte[]{22}));
    }

    @Test
    public void replace_rejectsResizeEmptyAndOutOfBounds() {
        StreamingEditSession session = session(sequence(8));
        assertThrows(IllegalArgumentException.class,
                () -> session.replace(0, new byte[]{0}, new byte[]{1, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> session.replace(0, new byte[0], new byte[0]));
        assertThrows(IndexOutOfBoundsException.class,
                () -> session.replace(8, new byte[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> session.replace(-1, new byte[]{1}));
    }

    @Test
    public void absoluteOffsets_supportFilesLargerThanTwoGigabytes() throws Exception {
        long offset = 3L * 1024 * 1024 * 1024;
        ByteSource source = (start, length) -> new byte[length];
        StreamingEditSession session = new StreamingEditSession(offset + 10, source);

        session.replace(offset + 4, new byte[]{7});

        assertEquals(offset + 4, session.getDirtyRanges().get(0).getOffset());
    }

    @Test
    public void dirtyRangeAndResultAreDefensiveCopies() throws Exception {
        StreamingEditSession session = session(sequence(8));
        byte[] replacement = new byte[]{44};
        session.replace(4, replacement);
        replacement[0] = 99;
        List<DirtyRange> ranges = session.getDirtyRanges();
        ranges.get(0).getReplacement()[0] = 88;

        assertArrayEquals(new byte[]{44}, session.getDirtyRanges().get(0).getReplacement());
        assertThrows(UnsupportedOperationException.class,
                () -> ranges.add(new DirtyRange(0, new byte[]{0}, new byte[]{1})));
    }

    @Test
    public void markSaved_clearsOverlayAndHistory() throws Exception {
        byte[] source = sequence(8);
        StreamingEditSession session = session(source);
        session.replace(1, new byte[]{11});

        session.markSaved();

        assertFalse(session.isDirty());
        assertFalse(session.canUndo());
        assertFalse(session.canRedo());
        assertTrue(session.getDirtyRanges().isEmpty());
    }

    private static StreamingEditSession session(byte[] source) {
        return new StreamingEditSession(source.length, (offset, length) -> {
            if (offset < 0 || offset + length > source.length) throw new IOException("range");
            byte[] result = new byte[length];
            System.arraycopy(source, (int) offset, result, 0, length);
            return result;
        });
    }

    private static byte[] sequence(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) bytes[i] = (byte) i;
        return bytes;
    }
}
