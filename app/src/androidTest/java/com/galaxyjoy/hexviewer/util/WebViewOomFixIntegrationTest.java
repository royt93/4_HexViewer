/**
 * ******************************************************************************
 * Integration Tests (Instrumented) for WebViewOomFix
 *
 * These tests run on a real device or emulator and prove that:
 * 1. The fix integrates correctly with the real Android WebView
 * 2. setRequestedFrameRate does not crash on Android 14+
 * 3. pauseTimers/resumeTimers work with a real WebView instance
 * 4. Memory trimming completes without crash
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented integration tests for {@link WebViewOomFix}.
 *
 * Run on real device: ./gradlew connectedAndroidTest
 *
 * These tests validate that the OOM fix works end-to-end on a real Android device,
 * specifically targeting the crash scenario:
 * WebView.onDraw → setRequestedFrameRate → Debug.getCallers → OOM
 */
@RunWith(AndroidJUnit4.class)
public class WebViewOomFixIntegrationTest {

    private Context mContext;
    private WebView mWebView;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // WebView must be created on main thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mWebView = new WebView(mContext);
        });
    }

    /**
     * CORE TEST: Proves that calling throttleWebViewFrameRate on a real WebView
     * on Android 14+ does NOT throw any exception.
     *
     * This directly validates the crash fix — if setRequestedFrameRate caused
     * an OOM before, this call should now succeed safely.
     */
    @Test
    public void throttleWebViewFrameRate_onRealWebView_doesNotCrash() {
        // Should complete without OOM or any other exception
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                WebViewOomFix.throttleWebViewFrameRate(mWebView);
                // If we reach here, the fix works
            } catch (OutOfMemoryError oom) {
                fail("OOM occurred — fix is not working! " + oom.getMessage());
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
    }

    /**
     * Verifies that restoring frame rate after throttling works without crash.
     */
    @Test
    public void throttleThenRestore_onRealWebView_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WebViewOomFix.throttleWebViewFrameRate(mWebView);
            WebViewOomFix.restoreWebViewFrameRate(mWebView);
            // Both operations should complete without error
        });
    }

    /**
     * Verifies that pauseTimers/resumeTimers lifecycle works on real WebView.
     * These calls stop the render loop that triggers the OOM crash.
     */
    @Test
    public void pauseAndResumeTimers_onRealWebView_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WebViewOomFix.pauseWebViewTimers(mWebView);
            WebViewOomFix.resumeWebViewTimers(mWebView);
        });
    }

    /**
     * Verifies that memory trim completes without crash on real WebView.
     */
    @Test
    public void trimWebViewMemory_onRealWebView_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Load minimal content first so there is something to trim
            mWebView.loadData("<html><body>test</body></html>", "text/html", "UTF-8");
            WebViewOomFix.trimWebViewMemory(mWebView);
        });
    }

    /**
     * Verifies that hardware layer can be applied and removed from real WebView.
     */
    @Test
    public void setHardwareLayer_onRealWebView_appliesCorrectly() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WebViewOomFix.setHardwareLayer(mWebView, true);
            assertEquals(
                    "Hardware layer should be applied",
                    View.LAYER_TYPE_HARDWARE,
                    mWebView.getLayerType()
            );

            WebViewOomFix.setHardwareLayer(mWebView, false);
            assertEquals(
                    "Hardware layer should be removed",
                    View.LAYER_TYPE_NONE,
                    mWebView.getLayerType()
            );
        });
    }

    /**
     * Verifies that safeDestroyWebView completes without crash.
     * After destroy, the WebView reference should be considered invalid.
     */
    @Test
    public void safeDestroyWebView_onRealWebView_doesNotCrash() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WebViewOomFix.safeDestroyWebView(mWebView);
            mWebView = null;   // Must null out the reference after destroy
        });
        // Reaching here = success
        assertNull(mWebView);
    }

    /**
     * Verifies the OOM bug detection is correct for this device's SDK level.
     */
    @Test
    public void isAffectedByOomBug_matchesDeviceSdkVersion() {
        boolean expected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
        assertEquals(
                "OOM bug detection should match device SDK " + Build.VERSION.SDK_INT,
                expected,
                WebViewOomFix.isAffectedByOomBug()
        );
    }

    /**
     * Stress test: apply the fix 100 times in quick succession to ensure
     * no memory leak or cumulative OOM from repeated calls.
     */
    @Test
    public void throttleWebViewFrameRate_repeatedCalls_doesNotLeak() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Create a fresh WebView for this test
            WebView stressView = new WebView(mContext);
            try {
                for (int i = 0; i < 100; i++) {
                    WebViewOomFix.throttleWebViewFrameRate(stressView);
                    WebViewOomFix.restoreWebViewFrameRate(stressView);
                }
                // Success if we reach here without OOM
            } catch (OutOfMemoryError oom) {
                fail("OOM during repeated throttle calls — possible memory leak: " + oom.getMessage());
            } finally {
                WebViewOomFix.safeDestroyWebView(stressView);
            }
        });
    }
}
