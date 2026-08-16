package com.galaxyjoy.hexviewer.streaming;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.RandomAccessFile;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PatternFileFactoryTest {
    @Test
    public void realPattern_isCorrectAcrossBufferAndRowBoundaries() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = PatternFileFactory.createPatternFile(
                context, "streaming_pattern_real.bin", 64 * 1024L + 17
        );
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            long[] offsets = {0, 15, 16, 64 * 1024L - 1, 64 * 1024L, file.length() - 1};
            for (long offset : offsets) {
                input.seek(offset);
                assertEquals(
                        "Wrong pattern byte at " + offset,
                        PatternFileFactory.expectedByte(offset) & 0xff,
                        input.read()
                );
            }
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    @Test
    public void sparsePattern_hasExactLengthAndMarkersAtLargeOffsets() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        long size = 130L * 1024 * 1024 + 1;
        long[] markers = {0, 30L * 1024 * 1024, size / 2, size - 1};
        File file = PatternFileFactory.createSparsePatternFile(
                context, "streaming_pattern_sparse.bin", size, markers
        );
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            assertEquals(size, input.length());
            for (long offset : markers) {
                input.seek(offset);
                assertEquals(
                        "Wrong sparse marker at " + offset,
                        PatternFileFactory.expectedByte(offset) & 0xff,
                        input.read()
                );
            }
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }
}
