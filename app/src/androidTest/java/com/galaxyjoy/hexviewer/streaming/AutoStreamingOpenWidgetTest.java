package com.galaxyjoy.hexviewer.streaming;

import android.net.Uri;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.constants.AppConstants;
import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.LineEntry;
import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.ui.act.ActMain;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** User-visible contract: a normal Open action transparently selects bounded streaming. */
@RunWith(AndroidJUnit4.class)
public class AutoStreamingOpenWidgetTest {
    @Test
    public void normalOpen_aboveThirtyMiB_loadsBoundedFirstWindowWithoutLegacyError() throws Exception {
        long realSize = AppConstants.MAX_NORMAL_FILE_SIZE + 1;
        File file = PatternFileFactory.createSparsePatternFile(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "widget_auto_streaming.bin",
                realSize,
                0,
                realSize - PatternFileFactory.MARKER_SIZE
        );

        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            scenario.onActivity(activity -> {
                FileData data = new FileData(activity, Uri.fromFile(file), false);
                activity.getLauncherOpen().processFileOpen(data, null, false);
            });

            AtomicBoolean loaded = new AtomicBoolean(false);
            AtomicReference<Throwable> assertionFailure = new AtomicReference<>();
            long deadline = SystemClock.elapsedRealtime() + 15_000L;
            while (!loaded.get() && assertionFailure.get() == null
                    && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> {
                    try {
                        FileData opened = activity.getFileData();
                        if (opened == null || activity.getPayloadHex().getAdapter().getCount() == 0) {
                            return;
                        }
                        assertTrue("Large normal open must transparently select streaming",
                                opened.isStreaming());
                        assertEquals(realSize, opened.getRealSize());
                        assertEquals(0, opened.getStartOffset());
                        assertEquals(AppConstants.STREAMING_WINDOW_SIZE, opened.getEndOffset());
                        assertEquals(AppConstants.STREAMING_WINDOW_SIZE, opened.getSize());

                        int rowCount = activity.getPayloadHex().getAdapter().getCount();
                        // 8 bytes/row is the densest supported setting, hence the largest row count.
                        assertTrue("Resident rows exceeded one configured streaming window",
                                rowCount <= AppConstants.STREAMING_WINDOW_SIZE / 8 + 1);
                        LineEntry first = activity.getPayloadHex().getAdapter().getItem(0);
                        assertNotNull(first);
                        List<Byte> bytes = first.getRaw();
                        assertTrue(bytes.size() >= 2);
                        assertEquals(PatternFileFactory.expectedByte(0), (byte) bytes.get(0));
                        assertEquals(PatternFileFactory.expectedByte(1), (byte) bytes.get(1));
                        loaded.set(true);
                    } catch (Throwable failure) {
                        assertionFailure.set(failure);
                    }
                });
                if (!loaded.get()) {
                    SystemClock.sleep(50L);
                }
            }

            if (assertionFailure.get() != null) {
                throw new AssertionError("Streaming UI assertion failed", assertionFailure.get());
            }
            assertTrue("Timed out waiting for the first streaming window", loaded.get());
            onView(ViewMatchers.withText(containsString("File too large to open entirely")))
                    .check(doesNotExist());
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    @Test
    public void goTo_middleAndTail_replacesWindowWithoutGrowingResidentRows() throws Exception {
        // Deliberately not row-aligned: the final partial row is a common source of lost bytes.
        long realSize = 35L * 1024L * 1024L + 7L;
        long middle = realSize / 2L;
        long tail = realSize - 1L;
        File file = PatternFileFactory.createSparsePatternFile(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "widget_streaming_goto.bin", realSize, middle, tail);
        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity, Uri.fromFile(file), false), null, false));
            waitForWindowContaining(scenario, 0L, realSize);

            scenario.onActivity(activity -> activity.goToStreamingOffset(middle));
            waitForWindowContaining(scenario, middle, realSize);
            assertByteAtAbsoluteOffset(scenario, middle);

            scenario.onActivity(activity -> activity.goToStreamingOffset(tail));
            waitForWindowContaining(scenario, tail, realSize);
            assertByteAtAbsoluteOffset(scenario, tail);
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    @Test
    public void listScrollToEndAndBack_requestsNextAndPreviousWindows() throws Exception {
        long realSize = AppConstants.MAX_NORMAL_FILE_SIZE + AppConstants.STREAMING_WINDOW_SIZE * 2L;
        File file = PatternFileFactory.createSparsePatternFile(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "widget_streaming_scroll.bin", realSize,
                0, AppConstants.STREAMING_WINDOW_SIZE, realSize - PatternFileFactory.MARKER_SIZE);
        try (ActivityScenario<ActMain> scenario = ActivityScenario.launch(ActMain.class)) {
            scenario.onActivity(activity -> activity.getLauncherOpen().processFileOpen(
                    new FileData(activity, Uri.fromFile(file), false), null, false));
            waitForWindowContaining(scenario, 0L, realSize);

            // One programmatic scroll event exercises the same OnScrollListener used by touch,
            // keyboard and accessibility, but unlike Espresso onData().scrollTo() it cannot keep
            // retrying against each replacement adapter and accidentally page to EOF.
            scenario.onActivity(activity -> activity.getPayloadHex().getListView().setSelection(
                    activity.getPayloadHex().getAdapter().getCount() - 1));
            waitForWindowStartGreaterThan(scenario, 0L);

            final long[] forwardStart = new long[1];
            scenario.onActivity(activity -> forwardStart[0] = activity.getFileData().getStartOffset());
            scenario.onActivity(activity -> activity.getPayloadHex().getListView().setSelection(0));
            waitForWindowStartLessThan(scenario, forwardStart[0]);
        } finally {
            PatternFileFactory.deleteQuietly(file);
        }
    }

    private static void waitForWindowContaining(ActivityScenario<ActMain> scenario,
                                                long offset, long realSize) {
        AtomicBoolean loaded = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        long deadline = SystemClock.elapsedRealtime() + 15_000L;
        while (!loaded.get() && failure.get() == null
                && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                try {
                    FileData opened = activity.getFileData();
                    if (opened == null || activity.getPayloadHex().getAdapter().getCount() == 0
                            || offset < opened.getStartOffset() || offset >= opened.getEndOffset()) return;
                    assertEquals(realSize, opened.getRealSize());
                    assertTrue(opened.isStreaming());
                    assertTrue(opened.getSize() <= AppConstants.STREAMING_WINDOW_SIZE);
                    int maxRows = AppConstants.STREAMING_WINDOW_SIZE / 8 + 1;
                    assertTrue(activity.getPayloadHex().getAdapter().getCount() <= maxRows);
                    loaded.set(true);
                } catch (Throwable assertion) {
                    failure.set(assertion);
                }
            });
            if (!loaded.get()) SystemClock.sleep(50L);
        }
        if (failure.get() != null) throw new AssertionError("Go To assertion failed", failure.get());
        assertTrue("Timed out waiting for streaming window at " + offset, loaded.get());
    }

    private static void assertByteAtAbsoluteOffset(ActivityScenario<ActMain> scenario, long offset) {
        scenario.onActivity(activity -> {
            FileData opened = activity.getFileData();
            int bytesPerLine = ((MyApplication) activity.getApplicationContext()).getNbBytesPerLine();
            long relative = offset - opened.getStartOffset();
            int row = (int) (relative / bytesPerLine);
            int byteInRow = (int) (relative % bytesPerLine);
            LineEntry entry = activity.getPayloadHex().getAdapter().getItem(row);
            assertNotNull("Missing row for absolute offset " + offset, entry);
            assertTrue("Missing byte in final/partial row", byteInRow < entry.getRaw().size());
            assertEquals("Wrong UI byte at absolute offset " + offset,
                    PatternFileFactory.expectedByte(offset), (byte) entry.getRaw().get(byteInRow));
        });
    }

    private static void waitForWindowStartGreaterThan(ActivityScenario<ActMain> scenario,
                                                       long previousStart) {
        waitForWindowStart(scenario, previousStart, true);
    }

    private static void waitForWindowStartLessThan(ActivityScenario<ActMain> scenario,
                                                    long previousStart) {
        waitForWindowStart(scenario, previousStart, false);
    }

    private static void waitForWindowStart(ActivityScenario<ActMain> scenario,
                                           long previousStart, boolean greater) {
        AtomicBoolean changed = new AtomicBoolean(false);
        long deadline = SystemClock.elapsedRealtime() + 15_000L;
        while (!changed.get() && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                long current = activity.getFileData().getStartOffset();
                if ((greater && current > previousStart) || (!greater && current < previousStart)) {
                    changed.set(activity.getPayloadHex().getAdapter().getCount() > 0);
                }
            });
            if (!changed.get()) SystemClock.sleep(50L);
        }
        assertTrue("Scroll did not request the " + (greater ? "next" : "previous")
                + " streaming window", changed.get());
    }
}
