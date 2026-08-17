package com.galaxyjoy.hexviewer.streaming;

import android.net.Uri;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.constants.AppConstants;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.streaming.fixture.StreamingTestContentProvider;
import com.galaxyjoy.hexviewer.ui.act.ActMain;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Search must preserve the legacy distinction between hex-view and plain-text queries. */
@RunWith(AndroidJUnit4.class)
public class StreamingSearchWidgetTest {
    @Test
    public void hexViewSearch_interpretsSpacedHexAsBytes() throws Exception {
        long size = AppConstants.MAX_NORMAL_FILE_SIZE + AppConstants.STREAMING_WINDOW_SIZE;
        long matchOffset = size / 2;
        byte[] match = {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef};
        File file = createFileWithBytes("stream_search_hex.bin", size, matchOffset, match);
        try (ActivityScenario<ActMain> scenario = open(file, size)) {
            scenario.onActivity(activity -> activity.doSearch("DE AD BE EF"));
            waitForWindowContaining(scenario, matchOffset, 15_000L);
            assertBytesAt(scenario, matchOffset, match);
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    @Test
    public void plainTextSearch_findsUtf8BytesAcrossWholeFile() throws Exception {
        long size = AppConstants.MAX_NORMAL_FILE_SIZE + AppConstants.STREAMING_WINDOW_SIZE;
        byte[] match = "needle".getBytes(StandardCharsets.UTF_8);
        long matchOffset = size - AppConstants.STREAMING_WINDOW_SIZE / 2L;
        File file = createFileWithBytes("stream_search_plain.bin", size, matchOffset, match);
        try (ActivityScenario<ActMain> scenario = open(file, size)) {
            scenario.onActivity(activity -> {
                activity.onPopupItemClick(R.id.actionPlainTextContainer);
                activity.doSearch("needle");
            });
            waitForWindowContaining(scenario, matchOffset, 15_000L);
            assertBytesAt(scenario, matchOffset, match);
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    @Test
    public void clearingSearch_cancelsPendingWholeFileResult() {
        long size = AppConstants.MAX_NORMAL_FILE_SIZE + 1;
        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity,
                            StreamingTestContentProvider.uri("slowpipe", "slow-search.bin", size),
                            false), null, false));
            waitForWindowContaining(scenario, 0L, 45_000L);
            scenario.onActivity(activity -> {
                activity.doSearch("late-target");
                activity.doSearch("");
            });

            long deadline = SystemClock.elapsedRealtime() + 3_000L;
            while (SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> assertEquals(
                        "A cleared search must not navigate from a stale callback",
                        0L, activity.getFileData().getStartOffset()));
                SystemClock.sleep(50L);
            }
        }
    }

    /**
     * Regression test: a background streaming search thread was only ever cancelled by an empty
     * query, closing the Activity, or starting another search on the SAME file. If the user
     * opened a DIFFERENT file while a search on the previous file was still in flight, the stale
     * search's generation token was never invalidated, so a late hit could still call
     * goToStreamingOffset() and jump the now-unrelated new file to an offset that has nothing to
     * do with it. ActMain#setFileData() now bumps the generation on every file switch.
     */
    @Test
    public void openingDifferentFile_whileSearchInFlight_doesNotNavigateNewFile() {
        long size = AppConstants.MAX_NORMAL_FILE_SIZE + 1;
        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            Uri fileA = StreamingTestContentProvider.uri("slowpipe", "switch-a.bin", size);
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity, fileA, false), null, false));
            waitForWindowContaining(scenario, 0L, 45_000L);

            scenario.onActivity(activity -> activity.doSearch("late-target"));
            // Give the slow background search a moment to actually start before switching files.
            SystemClock.sleep(200L);

            Uri fileB = StreamingTestContentProvider.uri("slowpipe", "switch-b.bin", size);
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity, fileB, false), null, false));
            waitForWindowContaining(scenario, 0L, 45_000L);
            scenario.onActivity(activity -> assertEquals(
                    "File B must be the active file right after opening it", fileB,
                    activity.getFileData().getUri()));

            // File A's slow search (~size/8KB milliseconds) has plenty of time to finish here.
            long deadline = SystemClock.elapsedRealtime() + 15_000L;
            while (SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    assertEquals("A stale search from the previous file must not switch files back",
                            fileB, activity.getFileData().getUri());
                    assertEquals("A stale search from the previous file must not move the window",
                            0L, activity.getFileData().getStartOffset());
                });
                SystemClock.sleep(200L);
            }
        }
    }

    private static ActivityScenario<ActMain> open(File file, long size) {
        ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class);
        scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                new FileData(activity, Uri.fromFile(file), false), null, false));
        waitForWindowContaining(scenario, 0L, 15_000L);
        return scenario;
    }

    private static File createFileWithBytes(String name, long size, long offset, byte[] bytes)
            throws Exception {
        File file = PatternFileFactory.createSparsePatternFile(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), name, size, 0);
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            output.seek(offset);
            output.write(bytes);
        }
        return file;
    }

    private static void waitForWindowContaining(ActivityScenario<ActMain> scenario,
                                                long offset, long timeoutMs) {
        AtomicBoolean loaded = new AtomicBoolean(false);
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (!loaded.get() && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                FileData data = activity.getFileData();
                loaded.set(data != null && activity.getPayloadHex().getAdapter().getCount() > 0
                        && offset >= data.getStartOffset() && offset < data.getEndOffset());
            });
            if (!loaded.get()) SystemClock.sleep(50L);
        }
        assertTrue("Timed out waiting for search/window at " + offset, loaded.get());
    }

    private static void assertBytesAt(ActivityScenario<ActMain> scenario,
                                      long offset, byte[] expected) {
        scenario.onActivity(activity -> {
            FileData data = activity.getFileData();
            int bytesPerLine = ((com.galaxyjoy.hexviewer.MyApplication)
                    activity.getApplicationContext()).getNbBytesPerLine();
            for (int i = 0; i < expected.length; i++) {
                long relative = offset + i - data.getStartOffset();
                LineEntry entry = activity.getPayloadHex().getAdapter()
                        .getItem((int) (relative / bytesPerLine));
                assertNotNull(entry);
                assertEquals(expected[i], (byte) entry.getRaw().get((int) (relative % bytesPerLine)));
            }
        });
    }
}
