/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Widget/UI test for transparent large-file streaming in ActMain.
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui;

import android.net.Uri;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.constants.AppConstants;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.streaming.fixture.PatternFileFactory;
import com.galaxyjoy.hexviewer.ui.act.ActMain;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the old user-visible 30 MiB rejection.
 */
@RunWith(AndroidJUnit4.class)
public class ActMainFileValidationWidgetTest {

    private ActivityScenario<ActMain> mScenario;
    private File mFixture;

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
        }
        PatternFileFactory.deleteQuietly(mFixture);
    }

    /**
     * A file selected through the normal Open flow must transparently load one bounded window.
     */
    @Test
    public void should_AutoStreamWithoutLegacyDialog_When_FileExceeds30MB() throws Exception {
        long realSize = 35L * 1024 * 1024;
        mFixture = PatternFileFactory.createSparsePatternFile(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "legacy_validation_35mb.bin",
                realSize,
                0,
                realSize - PatternFileFactory.MARKER_SIZE
        );
        mScenario = ActivityScenario.launch(ActMain.class);
        mScenario.onActivity(activity -> {
            FileData largeFile = new FileData(activity, Uri.fromFile(mFixture), false);
            activity.getLauncherOpen().processFileOpen(largeFile, null, false);
        });

        AtomicBoolean loaded = new AtomicBoolean(false);
        long deadline = SystemClock.elapsedRealtime() + 15_000L;
        while (!loaded.get() && SystemClock.elapsedRealtime() < deadline) {
            mScenario.onActivity(activity -> {
                if (activity.getPayloadHex().getAdapter().getCount() == 0) return;
                FileData opened = activity.getFileData();
                assertTrue(opened.isStreaming());
                assertEquals(realSize, opened.getRealSize());
                assertEquals(AppConstants.STREAMING_WINDOW_SIZE, opened.getSize());
                assertTrue(activity.getPayloadHex().getAdapter().getCount()
                        <= AppConstants.STREAMING_WINDOW_SIZE / 8 + 1);
                loaded.set(true);
            });
            if (!loaded.get()) SystemClock.sleep(50L);
        }
        assertTrue("Timed out waiting for the bounded streaming window", loaded.get());
        onView(ViewMatchers.withText(containsString("File too large to open entirely")))
                .check(doesNotExist());
        onView(ViewMatchers.withText(containsString("Sequential opening")))
                .check(doesNotExist());
    }
}
