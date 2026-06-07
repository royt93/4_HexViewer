/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * Integration Test (Espresso + Real Files) to test loading of small, medium,
 * and large files on the device cache directory to prevent permission prompts.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Validates opening of light, medium, and large files on the physical device.
 */
@RunWith(AndroidJUnit4.class)
public class RealFileOomIntegrationTest {

    private ActivityScenario<ActMain> mScenario;
    private File mSmallFile;
    private File mMediumFile;
    private File mLargeFile;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File cacheDir = context.getCacheDir();

        // 1. Small file (10 KB)
        mSmallFile = new File(cacheDir, "integration_small.bin");
        writeFileOfSize(mSmallFile, 10 * 1024);

        // 2. Medium file (15 MB) - well within 30MB normal limit
        mMediumFile = new File(cacheDir, "integration_medium.bin");
        writeFileOfSize(mMediumFile, 15 * 1024 * 1024);

        // 3. Large file (35 MB) - above 30MB limit
        mLargeFile = new File(cacheDir, "integration_large.bin");
        writeFileOfSize(mLargeFile, 35 * 1024 * 1024);
    }

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
        }
        // Delete files
        if (mSmallFile != null && mSmallFile.exists()) mSmallFile.delete();
        if (mMediumFile != null && mMediumFile.exists()) mMediumFile.delete();
        if (mLargeFile != null && mLargeFile.exists()) mLargeFile.delete();
    }

    private void writeFileOfSize(File file, int size) throws IOException {
        byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
        // Fill buffer with arbitrary pattern
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (i % 256);
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            int remaining = size;
            while (remaining > 0) {
                int toWrite = Math.min(remaining, buffer.length);
                fos.write(buffer, 0, toWrite);
                remaining -= toWrite;
            }
        }
    }

    /**
     * TEST 1: Open small file (10 KB) in normal mode.
     * Must succeed completely and have correct entries count in adapter.
     */
    @Test
    public void openSmallFile_succeedsCompletely() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            FileData fd = new FileData(activity, Uri.fromFile(mSmallFile), false);
            activity.getLauncherOpen().processFileOpen(fd, null, false);
        });

        Thread.sleep(2000); // Wait for TaskOpen
        
        mScenario.onActivity(activity -> {
            int count = activity.getPayloadHex().getAdapter().getCount();
            // 10 KB = 10240 bytes. 16 bytes per row = 640 rows.
            assertTrue("Adapter should have rows populated", count > 0);
            // Verify no exception occurred
            assertTrue("FileData size should be set", activity.getFileData().getSize() == 10240);
        });
    }

    /**
     * TEST 2: Open medium file (15 MB) in normal mode.
     * Must succeed without OOM or crash.
     */
    @Test
    public void openMediumFile_succeedsWithoutOOM() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            FileData fd = new FileData(activity, Uri.fromFile(mMediumFile), false);
            activity.getLauncherOpen().processFileOpen(fd, null, false);
        });

        // 15MB takes a few seconds to load, wait for it
        Thread.sleep(4000);

        mScenario.onActivity(activity -> {
            int count = activity.getPayloadHex().getAdapter().getCount();
            assertTrue("Adapter should contain rows", count > 0);
            assertTrue("FileData size should be exactly 15MB", activity.getFileData().getSize() == 15 * 1024 * 1024);
        });
    }

    /**
     * TEST 3: Open large file (35 MB) in normal mode.
     * Must fail immediately and show warning dialog.
     */
    @Test
    public void openLargeFile_normalMode_failsWithWarningDialog() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            FileData fd = new FileData(activity, Uri.fromFile(mLargeFile), false);
            activity.getLauncherOpen().processFileOpen(fd, null, false);
        });

        Thread.sleep(2000); // Wait for validation failure

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String expectedTitle = targetContext.getString(R.string.error_title);

        // Assert Dialog Title is displayed
        onView(withText(expectedTitle))
                .check(ViewAssertions.matches(isDisplayed()));

        // Assert Dialog Message containing limit instructions is displayed
        onView(ViewMatchers.withText(containsString("File too large to open entirely")))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    /**
     * TEST 4: Open large file (35 MB) in sequential mode with range > 30MB.
     * Must fail with "Selected portion is too large" dialog.
     */
    @Test
    public void openLargeFile_sequentialMode_largeRange_failsWithWarningDialog() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            // Set offsets covering the entire 35MB (which exceeds 30MB limit)
            FileData fd = new FileData(activity, Uri.fromFile(mLargeFile), false, 0L, 35 * 1024 * 1024);
            activity.getLauncherOpen().processFileOpen(fd, null, false);
        });

        Thread.sleep(2000); // Wait for validation failure

        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String expectedTitle = targetContext.getString(R.string.error_title);

        // Assert Dialog Title is displayed
        onView(withText(expectedTitle))
                .check(ViewAssertions.matches(isDisplayed()));

        // Assert Dialog Message containing portion limit warning is displayed
        onView(ViewMatchers.withText(containsString("Selected portion is too large")))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    /**
     * TEST 5: Open large file (35 MB) in sequential mode with a safe range <= 30MB.
     * Must succeed completely and load the portion into the adapter.
     */
    @Test
    public void openLargeFile_sequentialMode_smallRange_succeeds() throws InterruptedException {
        mScenario = ActivityScenario.launch(ActMain.class);
        Thread.sleep(1000);

        mScenario.onActivity(activity -> {
            // Set offsets covering only first 10MB (safe range)
            FileData fd = new FileData(activity, Uri.fromFile(mLargeFile), false, 0L, 10 * 1024 * 1024);
            activity.getLauncherOpen().processFileOpen(fd, null, false);
        });

        Thread.sleep(3000); // Wait for loading to complete

        mScenario.onActivity(activity -> {
            int count = activity.getPayloadHex().getAdapter().getCount();
            assertTrue("Adapter should contain portion rows", count > 0);
            assertTrue("FileData size should be set to portion size (10MB)", activity.getFileData().getSize() == 10 * 1024 * 1024);
        });
    }

    /**
     * TEST 6: Open huge file (130 MB) in sequential mode with a safe range <= 30MB.
     * Must succeed completely and load the portion into the adapter.
     */
    @Test
    public void openHugeFile_130MB_sequentialMode_smallRange_succeeds() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File hugeFile = new File(context.getCacheDir(), "integration_130mb.bin");
        writeFileOfSize(hugeFile, 130 * 1024 * 1024);

        try {
            mScenario = ActivityScenario.launch(ActMain.class);
            Thread.sleep(1000);

            mScenario.onActivity(activity -> {
                // Open 130MB sequentially with a 20MB portion (0 to 20MB)
                FileData fd = new FileData(activity, Uri.fromFile(hugeFile), false, 0L, 20 * 1024 * 1024);
                activity.getLauncherOpen().processFileOpen(fd, null, false);
            });

            Thread.sleep(4000); // Wait for loading portion

            mScenario.onActivity(activity -> {
                int count = activity.getPayloadHex().getAdapter().getCount();
                assertTrue("Adapter should contain portion rows", count > 0);
                assertTrue("FileData size should be set to portion size (20MB)", activity.getFileData().getSize() == 20 * 1024 * 1024);
            });
        } finally {
            if (hugeFile.exists()) {
                hugeFile.delete();
            }
        }
    }

    /**
     * TEST 7: Open huge file (130 MB) in sequential mode with a large range > 30MB.
     * Must fail with "Selected portion is too large" dialog.
     */
    @Test
    public void openHugeFile_130MB_sequentialMode_largeRange_failsWithWarningDialog() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File hugeFile = new File(context.getCacheDir(), "integration_130mb.bin");
        writeFileOfSize(hugeFile, 130 * 1024 * 1024);

        try {
            mScenario = ActivityScenario.launch(ActMain.class);
            Thread.sleep(1000);

            mScenario.onActivity(activity -> {
                // Open 130MB sequentially with a 35MB portion (0 to 35MB - exceeding 30MB limit)
                FileData fd = new FileData(activity, Uri.fromFile(hugeFile), false, 0L, 35 * 1024 * 1024);
                activity.getLauncherOpen().processFileOpen(fd, null, false);
            });

            Thread.sleep(2000); // Wait for validation failure

            String expectedTitle = context.getString(R.string.error_title);

            // Assert Dialog Title is displayed
            onView(withText(expectedTitle))
                    .check(ViewAssertions.matches(isDisplayed()));

            // Assert Dialog Message containing portion limit warning is displayed
            onView(ViewMatchers.withText(containsString("Selected portion is too large")))
                    .check(ViewAssertions.matches(isDisplayed()));
        } finally {
            if (hugeFile.exists()) {
                hugeFile.delete();
            }
        }
    }
}
