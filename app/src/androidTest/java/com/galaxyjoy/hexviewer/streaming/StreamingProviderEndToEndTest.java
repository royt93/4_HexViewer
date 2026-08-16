package com.galaxyjoy.hexviewer.streaming;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.constants.AppConstants;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.streaming.fixture.StreamingTestContentProvider;
import com.galaxyjoy.hexviewer.ui.act.ActMain;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** End-to-end coverage for providers that cannot expose a directly seekable descriptor. */
@RunWith(AndroidJUnit4.class)
public class StreamingProviderEndToEndTest {
    @Test
    public void knownSizePipeProvider_spoolsAndDisplaysFirstWindowWithoutLeakingCache() {
        openProviderAndAssertFirstWindow("pipe", AppConstants.MAX_NORMAL_FILE_SIZE + 1);
    }

    @Test
    public void unknownSizePipeProvider_discoversSizeAndDisplaysFirstWindow() {
        openProviderAndAssertFirstWindow("unknown", AppConstants.MAX_NORMAL_FILE_SIZE + 17);
    }

    private static void openProviderAndAssertFirstWindow(String mode, long expectedSize) {
        File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        int spoolFilesBefore = countSpoolFiles(cache);
        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity,
                            StreamingTestContentProvider.uri(mode, mode + ".bin", expectedSize),
                            false), null, false));

            AtomicBoolean loaded = new AtomicBoolean(false);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            long deadline = SystemClock.elapsedRealtime() + 45_000L;
            while (!loaded.get() && failure.get() == null
                    && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    try {
                        FileData opened = activity.getFileData();
                        if (opened == null || activity.getPayloadHex().getAdapter().getCount() == 0) return;
                        assertTrue(opened.isStreaming());
                        assertEquals(expectedSize, opened.getRealSize());
                        assertTrue(opened.getSize() <= AppConstants.STREAMING_WINDOW_SIZE);
                        LineEntry first = activity.getPayloadHex().getAdapter().getItem(0);
                        assertNotNull(first);
                        assertTrue(first.getRaw().size() >= 2);
                        assertEquals(PatternFileFactory.expectedByte(0), (byte) first.getRaw().get(0));
                        assertEquals(PatternFileFactory.expectedByte(1), (byte) first.getRaw().get(1));
                        loaded.set(true);
                    } catch (Throwable assertion) {
                        failure.set(assertion);
                    }
                });
                if (!loaded.get()) SystemClock.sleep(50L);
            }
            if (failure.get() != null) throw new AssertionError(mode + " provider failed", failure.get());
            assertTrue("Timed out opening " + mode + " provider", loaded.get());
        }
        assertEquals("Spool cache file leaked after activity/session closed",
                spoolFilesBefore, countSpoolFiles(cache));
    }

    private static int countSpoolFiles(File cache) {
        File[] files = cache.listFiles((dir, name) -> name.startsWith("hexviewer-stream-"));
        return files == null ? 0 : files.length;
    }
}
