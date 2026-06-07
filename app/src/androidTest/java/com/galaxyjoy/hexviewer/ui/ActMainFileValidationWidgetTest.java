/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Widget/UI Test (Espresso) for file validation logic in ActMain.
 *
 * Verifies that trying to open a file >30MB in full mode correctly triggers
 * the warning dialog alerting the user and refusing to load to prevent OOM.
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.ui.act.ActMain;
import com.galaxyjoy.hexviewer.ui.act.SplashActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Widget/UI Espresso tests checking the File Validation dialog trigger.
 */
@RunWith(AndroidJUnit4.class)
public class ActMainFileValidationWidgetTest {

    private ActivityScenario<ActMain> mScenario;

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
        }
    }

    /**
     * TEST: Attempts to open a file of size 35MB (exceeding MAX_NORMAL_FILE_SIZE = 30MB)
     * in non-sequential (full) mode.
     * Asserts that a dialog is displayed to the user containing the warning text.
     */
    @Test
    public void should_ShowFileTooLargeDialog_When_FileExceeds30MB() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            ActMain actMain = activity;
                
            // Create a mock FileData representing a 35MB file (above 30MB limit)
            FileData mockLargeFile = new FileData(actMain, Uri.parse("content://mock/large_file.bin"), false) {
                @Override
                public long getSize() {
                    return 35L * 1024 * 1024; // 35 MB
                }
                @Override
                public long getRealSize() {
                    return 35L * 1024 * 1024;
                }
                @Override
                public boolean isSequential() {
                    return false; // normal mode
                }
            };

            // Trigger file open process directly on ActMain launcher
            actMain.getLauncherOpen().processFileOpen(mockLargeFile, null, false);
        });

        // Wait for AsyncTask (TaskOpen) to doInBackground and postExecute on UI thread
        Thread.sleep(2000);

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String expectedTitle = targetContext.getString(R.string.error_title);

        // Assert Dialog Title is displayed
        onView(withText(expectedTitle))
                .check(ViewAssertions.matches(isDisplayed()));

        // Assert Dialog Message containing limit instructions is displayed
        onView(ViewMatchers.withText(containsString("File too large to open entirely")))
                .check(ViewAssertions.matches(isDisplayed()));

        onView(ViewMatchers.withText(containsString("Sequential opening")))
                .check(ViewAssertions.matches(isDisplayed()));
    }
}
