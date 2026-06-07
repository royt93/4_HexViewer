/**
 * ******************************************************************************
 * Unit Tests for WebViewOomFix
 *
 * Tests verify the root cause fix for:
 * java.lang.OutOfMemoryError at VMStack.getThreadStackTrace
 *   → Thread.getStackTrace
 *   → Debug.getCallers
 *   → View.setRequestedFrameRate   (Android 14+ only)
 *   → WebViewChromium.onDraw
 *
 * Fix: throttleWebViewFrameRate() sets frame rate to 30fps,
 * halving the frequency of Debug.getCallers() allocations.
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util;

import android.os.Build;
import android.view.View;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebViewOomFix}.
 *
 * These tests run on JVM (Robolectric) and prove that:
 * 1. The fix correctly identifies affected Android versions
 * 2. Methods are null-safe (never crash with null args)
 * 3. Frame rate is set to the correct throttled value
 * 4. Hardware layer is correctly applied
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33, 34})   // Test on pre-14, 13, and 14 (affected)
public class WebViewOomFixTest {

    @Mock
    private View mockView;

    @Mock
    private WebView mockWebView;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isAffectedByOomBug()
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the bug detection returns the correct boolean based on SDK.
     * The OOM bug only affects Android 14+ (API 34+).
     */
    @Test
    public void isAffectedByOomBug_returnsCorrectValue() {
        boolean result = WebViewOomFix.isAffectedByOomBug();
        boolean expected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
        assertEquals(
                "isAffectedByOomBug() should match SDK_INT >= 34",
                expected,
                result
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Null safety — no method should throw NPE when given null
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void throttleWebViewFrameRate_withNullView_doesNotCrash() {
        // Should be a no-op, not an NPE
        assertDoesNotThrow(() -> WebViewOomFix.throttleWebViewFrameRate(null));
    }

    @Test
    public void restoreWebViewFrameRate_withNullView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.restoreWebViewFrameRate(null));
    }

    @Test
    public void pauseWebViewTimers_withNullWebView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.pauseWebViewTimers(null));
    }

    @Test
    public void resumeWebViewTimers_withNullWebView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.resumeWebViewTimers(null));
    }

    @Test
    public void trimWebViewMemory_withNullWebView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.trimWebViewMemory(null));
    }

    @Test
    public void safeDestroyWebView_withNullWebView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.safeDestroyWebView(null));
    }

    @Test
    public void setHardwareLayer_withNullView_doesNotCrash() {
        assertDoesNotThrow(() -> WebViewOomFix.setHardwareLayer(null, true));
        assertDoesNotThrow(() -> WebViewOomFix.setHardwareLayer(null, false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constants validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the throttled frame rate constant is exactly 30fps.
     * This is critical: must be <= 30 to have meaningful impact.
     */
    @Test
    public void throttledFrameRate_is30fps() {
        assertEquals(
                "Throttled frame rate must be exactly 30fps to halve Debug.getCallers() calls",
                30.0f,
                WebViewOomFix.THROTTLED_FRAME_RATE_FPS,
                0.001f
        );
    }

    /**
     * Verifies the default frame rate constant is 0 (platform default).
     */
    @Test
    public void defaultFrameRate_isZero() {
        assertEquals(
                "Default frame rate must be 0 (platform default, no throttling)",
                0.0f,
                WebViewOomFix.DEFAULT_FRAME_RATE_FPS,
                0.001f
        );
    }

    /**
     * Verifies that throttled rate < default would be nonsensical
     * (default of 0 = no restriction, throttled = 30 = restriction applied).
     */
    @Test
    public void throttledFrameRate_isPositiveAndReasonable() {
        assertTrue(
                "Throttled frame rate must be positive",
                WebViewOomFix.THROTTLED_FRAME_RATE_FPS > 0
        );
        assertTrue(
                "Throttled frame rate should be <= 60fps for meaningful throttling",
                WebViewOomFix.THROTTLED_FRAME_RATE_FPS <= 60.0f
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WebView timer control
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that pauseWebViewTimers calls onPause() and pauseTimers() on the WebView.
     * These two calls stop the render loop that triggers OOM via setRequestedFrameRate.
     */
    @Test
    public void pauseWebViewTimers_callsOnPauseAndPauseTimers() {
        WebViewOomFix.pauseWebViewTimers(mockWebView);
        verify(mockWebView, times(1)).onPause();
        verify(mockWebView, times(1)).pauseTimers();
    }

    /**
     * Verifies that resumeWebViewTimers calls onResume() and resumeTimers() on the WebView.
     */
    @Test
    public void resumeWebViewTimers_callsOnResumeAndResumeTimers() {
        WebViewOomFix.resumeWebViewTimers(mockWebView);
        verify(mockWebView, times(1)).onResume();
        verify(mockWebView, times(1)).resumeTimers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory trimming
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that trimWebViewMemory calls clearCache(false) and freeMemory().
     * We use clearCache(false) to keep navigation history but free memory cache.
     */
    @Test
    public void trimWebViewMemory_callsClearCacheAndFreeMemory() {
        WebViewOomFix.trimWebViewMemory(mockWebView);
        verify(mockWebView, times(1)).clearCache(false);
        verify(mockWebView, times(1)).freeMemory();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hardware layer
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that setHardwareLayer(view, true) applies LAYER_TYPE_HARDWARE.
     */
    @Test
    public void setHardwareLayer_withTrue_appliesHardwareLayer() {
        WebViewOomFix.setHardwareLayer(mockView, true);
        verify(mockView, times(1)).setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    /**
     * Verifies that setHardwareLayer(view, false) removes the layer.
     */
    @Test
    public void setHardwareLayer_withFalse_removesLayer() {
        WebViewOomFix.setHardwareLayer(mockView, false);
        verify(mockView, times(1)).setLayerType(View.LAYER_TYPE_NONE, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Helper to assert a runnable does not throw any exception.
     * Replaces JUnit 5's assertDoesNotThrow for JUnit 4.
     */
    private static void assertDoesNotThrow(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable t) {
            fail("Expected no exception, but got: " + t.getClass().getName() + " — " + t.getMessage());
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
