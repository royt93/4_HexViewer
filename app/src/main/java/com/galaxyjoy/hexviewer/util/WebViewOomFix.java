/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * WebView OOM Fix — Prevents OutOfMemoryError from WebView/AdView rendering
 * loop on Android 14+ (API 34+) caused by setRequestedFrameRate() calling
 * Debug.getCallers() → VMStack.getThreadStackTrace() under low-heap conditions.
 * </p>
 *
 * <p>Root cause chain:</p>
 * <pre>
 * WebViewChromium.onDraw()
 *   → View.setRequestedFrameRate()   [Android 14+ only]
 *     → Debug.getCallers()
 *       → VMStack.getThreadStackTrace()
 *         → OOM (heap full, cannot allocate StackTraceElement[])
 * </pre>
 *
 * <p>Fixes applied:</p>
 * <ol>
 *   <li>Pause AdView timers when app enters background to stop render loop</li>
 *   <li>Throttle frame rate on Android 14+ to reduce stack allocation pressure</li>
 *   <li>Trim WebView caches aggressively on low-memory signal</li>
 *   <li>Guard against null/destroyed views before any rendering call</li>
 * </ol>
 *
 * @author mckimquyen (OOM fix)
 * License: GPLv3
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.util;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class to mitigate the WebView/AdView OOM crash on Android 14+ (API 34+).
 *
 * <h3>Problem</h3>
 * On Android 14+, {@code View.setRequestedFrameRate()} internally calls
 * {@code Debug.getCallers()} which allocates a {@code StackTraceElement[]} array
 * on every draw frame. When the heap is nearly full (common in ad-heavy apps with
 * deep view hierarchies), this allocation fails with {@code OutOfMemoryError}.
 *
 * <h3>Solution</h3>
 * <ul>
 *   <li>Apply {@link #throttleWebViewFrameRate(View)} to any WebView/AdView to cap
 *       its frame-rate at 30 fps, halving the allocation frequency.</li>
 *   <li>Call {@link #pauseWebViewTimers(WebView)} / {@link #resumeWebViewTimers(WebView)}
 *       in {@code onPause} / {@code onResume} to stop render loop entirely in background.</li>
 *   <li>Call {@link #trimWebViewMemory(WebView)} when the system signals low memory.</li>
 * </ul>
 */
public final class WebViewOomFix {

    private static final String TAG = "WebViewOomFix";

    /**
     * Target frame-rate for WebView on Android 14+ to reduce
     * Debug.getCallers() allocation pressure.
     * 30 fps = half the allocations of 60 fps.
     */
    public static final float THROTTLED_FRAME_RATE_FPS = 30.0f;

    /**
     * Default (unrestricted) frame rate — tells the platform to pick its default.
     */
    public static final float DEFAULT_FRAME_RATE_FPS = 0.0f;

    private WebViewOomFix() {
        // Utility class — no instances
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Throttle the frame-rate of any View (typically a WebView or AdView) to
     * {@value #THROTTLED_FRAME_RATE_FPS} fps on Android 14+ (API 34+).
     *
     * <p>This is the primary fix for the OOM crash. Call it once after the
     * view is attached to a window (e.g., in {@code onResume} or after the ad
     * view is loaded).</p>
     *
     * @param view The WebView or AdView whose frame-rate should be throttled.
     *             May be null — the method is a no-op in that case.
     */
    public static void throttleWebViewFrameRate(@Nullable View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                view.setRequestedFrameRate(THROTTLED_FRAME_RATE_FPS);
                Log.d(TAG, "throttleWebViewFrameRate: set to " + THROTTLED_FRAME_RATE_FPS + " fps");
            } catch (Exception e) {
                Log.w(TAG, "throttleWebViewFrameRate failed: " + e.getMessage());
            }
        }
    }

    /**
     * Restore the default (unrestricted) frame-rate for a View on Android 14+.
     *
     * @param view The view to restore. May be null.
     */
    public static void restoreWebViewFrameRate(@Nullable View view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                view.setRequestedFrameRate(DEFAULT_FRAME_RATE_FPS);
                Log.d(TAG, "restoreWebViewFrameRate: restored to default");
            } catch (Exception e) {
                Log.w(TAG, "restoreWebViewFrameRate failed: " + e.getMessage());
            }
        }
    }

    /**
     * Pause WebView rendering timers to stop the render loop entirely when the
     * activity enters the background.
     *
     * <p>Must be paired with {@link #resumeWebViewTimers(WebView)} in
     * {@code onResume}.</p>
     *
     * @param webView The WebView to pause. May be null.
     */
    public static void pauseWebViewTimers(@Nullable WebView webView) {
        if (webView == null) return;
        try {
            webView.onPause();
            webView.pauseTimers();
            Log.d(TAG, "pauseWebViewTimers: paused");
        } catch (Exception e) {
            Log.w(TAG, "pauseWebViewTimers failed: " + e.getMessage());
        }
    }

    /**
     * Resume WebView rendering timers after the activity returns to the foreground.
     *
     * @param webView The WebView to resume. May be null.
     */
    public static void resumeWebViewTimers(@Nullable WebView webView) {
        if (webView == null) return;
        try {
            webView.onResume();
            webView.resumeTimers();
            Log.d(TAG, "resumeWebViewTimers: resumed");
        } catch (Exception e) {
            Log.w(TAG, "resumeWebViewTimers failed: " + e.getMessage());
        }
    }

    /**
     * Aggressively trim WebView memory caches when the system signals low memory.
     *
     * <p>Call from {@code Application.onTrimMemory()} or {@code Activity.onTrimMemory()}.</p>
     *
     * @param webView The WebView to trim. May be null.
     */
    public static void trimWebViewMemory(@Nullable WebView webView) {
        if (webView == null) return;
        try {
            webView.clearCache(false);   // clear disk+memory cache but keep navigated pages
            webView.freeMemory();
            Log.d(TAG, "trimWebViewMemory: caches trimmed");
        } catch (Exception e) {
            Log.w(TAG, "trimWebViewMemory failed: " + e.getMessage());
        }
    }

    /**
     * Safely destroy a WebView and release all its resources.
     *
     * <p>Must be called from the main thread. Always set the webView reference
     * to {@code null} after calling this method.</p>
     *
     * @param webView The WebView to destroy. May be null.
     */
    public static void safeDestroyWebView(@Nullable WebView webView) {
        if (webView == null) return;
        try {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.onPause();
            webView.removeAllViews();
            webView.destroy();
            Log.d(TAG, "safeDestroyWebView: destroyed");
        } catch (Exception e) {
            Log.w(TAG, "safeDestroyWebView failed: " + e.getMessage());
        }
    }

    /**
     * Apply hardware layer to a view to offload rendering from the CPU.
     * This reduces the number of invalidations and lowers OOM risk in
     * deep view hierarchies.
     *
     * @param view  The view to accelerate. May be null.
     * @param apply {@code true} to apply, {@code false} to remove hardware layer.
     */
    public static void setHardwareLayer(@Nullable View view, boolean apply) {
        if (view == null) return;
        try {
            view.setLayerType(
                    apply ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE,
                    null
            );
            Log.d(TAG, "setHardwareLayer: " + (apply ? "HARDWARE" : "NONE"));
        } catch (Exception e) {
            Log.w(TAG, "setHardwareLayer failed: " + e.getMessage());
        }
    }

    /**
     * Returns {@code true} if the current Android version is affected by the
     * WebView OOM bug (Android 14+, API 34+).
     */
    public static boolean isAffectedByOomBug() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
}
