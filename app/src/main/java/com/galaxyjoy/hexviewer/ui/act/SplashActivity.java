/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer.ui.act;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.R;
import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.UIUtils;

import kotlin.Unit;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private final Runnable finishRunnable = this::finish;
    // Store animated view references to cancel animations in onDestroy
    private View mAppName = null;
    private View mProgressContainer = null;
    private View mLoadingText = null;
    private View mAdNoticeCard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.activity_splash);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.layoutRoot), true, true);
        startAnimations();
        checkShowAd();
    }

    private void startAnimations() {
        // Store references to cancel animations later
        mAppName = findViewById(R.id.appName);
        mProgressContainer = findViewById(R.id.progressContainer);
        mLoadingText = findViewById(R.id.loadingText);
        mAdNoticeCard = findViewById(R.id.adNoticeCard);

        // App name animation - zoom in with fade
        mAppName.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Progress bar container - fade in with scale
        mProgressContainer.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Loading text - fade in with pulsing
        mLoadingText.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(1100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Only start pulse if activity is not finishing
                    if (!isFinishing()) {
                        pulseAnimation(mLoadingText);
                    }
                })
                .start();

        // Ad notice card - slide up from bottom
        mAdNoticeCard.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(900)
                .setStartDelay(1300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void pulseAnimation(View view) {
        // Stop animation if activity is finishing to prevent memory leak
        if (isFinishing() || view == null) {
            return;
        }

        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Check again before starting scale-down animation
                    if (isFinishing() || view == null) {
                        return;
                    }

                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(800)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> {
                                // Check again before restarting pulse animation
                                if (!isFinishing()) {
                                    pulseAnimation(view);
                                }
                            })
                            .start();
                })
                .start();
    }

    private void checkShowAd() {
        AdManager.INSTANCE.initSplashScreen(this, () -> {
            goToMain();
            return null;
        });
    }

    private void goToMain() {
        Intent intent = new Intent(SplashActivity.this, ActMain.class);
        startActivity(intent);
//        finish(); // Close SplashActivity so the user can't go back to it
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().getDecorView().postDelayed(finishRunnable, 300);
    }

    @Override
    protected void onDestroy() {
        // Cancel all animations to prevent memory leaks
        if (mAppName != null) {
            mAppName.animate().cancel();
            mAppName.clearAnimation();
        }
        if (mProgressContainer != null) {
            mProgressContainer.animate().cancel();
            mProgressContainer.clearAnimation();
        }
        if (mLoadingText != null) {
            mLoadingText.animate().cancel();
            mLoadingText.clearAnimation();
        }
        if (mAdNoticeCard != null) {
            mAdNoticeCard.animate().cancel();
            mAdNoticeCard.clearAnimation();
        }

        // Clear all pending callbacks and messages
        getWindow().getDecorView().removeCallbacks(finishRunnable);
        super.onDestroy();
    }
}
