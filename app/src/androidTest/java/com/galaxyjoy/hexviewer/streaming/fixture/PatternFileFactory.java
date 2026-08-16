package com.galaxyjoy.hexviewer.streaming.fixture;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Deterministic real and sparse files used by large-file instrumentation tests. */
public final class PatternFileFactory {
    private static final int BUFFER_SIZE = 64 * 1024;
    public static final int MARKER_SIZE = 64;

    private PatternFileFactory() {
    }

    /** Returns the byte expected at an absolute file offset. */
    public static byte expectedByte(long offset) {
        return (byte) (offset & 0xffL);
    }

    /** Creates a fully populated file whose byte value is derived from its absolute offset. */
    @NonNull
    public static File createPatternFile(
            @NonNull Context context,
            @NonNull String name,
            long size
    ) throws IOException {
        validateSize(size);
        File file = new File(context.getCacheDir(), name);
        byte[] buffer = new byte[BUFFER_SIZE];
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            long position = 0;
            while (position < size) {
                int count = (int) Math.min(buffer.length, size - position);
                fillPattern(buffer, count, position);
                output.write(buffer, 0, count);
                position += count;
            }
        }
        return file;
    }

    /**
     * Creates a sparse file and writes deterministic markers at selected offsets. This makes
     * multi-gigabyte boundary fixtures cheap while still proving that seeking used the right offset.
     */
    @NonNull
    public static File createSparsePatternFile(
            @NonNull Context context,
            @NonNull String name,
            long size,
            long... markerOffsets
    ) throws IOException {
        validateSize(size);
        File file = new File(context.getCacheDir(), name);
        try (RandomAccessFile randomAccess = new RandomAccessFile(file, "rw")) {
            randomAccess.setLength(0);
            randomAccess.setLength(size);
            byte[] marker = new byte[MARKER_SIZE];
            for (long markerOffset : markerOffsets) {
                if (markerOffset < 0 || markerOffset >= size) {
                    throw new IllegalArgumentException("Marker outside file: " + markerOffset);
                }
                int count = (int) Math.min(marker.length, size - markerOffset);
                fillPattern(marker, count, markerOffset);
                randomAccess.seek(markerOffset);
                randomAccess.write(marker, 0, count);
            }
        }
        return file;
    }

    public static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            // Test cleanup is best effort. A failed delete is visible as a stale fixture next run.
            file.delete();
        }
    }

    private static void fillPattern(byte[] buffer, int count, long absoluteOffset) {
        for (int i = 0; i < count; i++) {
            buffer[i] = expectedByte(absoluteOffset + i);
        }
    }

    private static void validateSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative file size: " + size);
        }
    }
}
