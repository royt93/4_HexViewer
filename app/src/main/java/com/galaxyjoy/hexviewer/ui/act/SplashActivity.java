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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.BuildConfig;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.sdkadbmob.AdMobManager;
import com.galaxyjoy.hexviewer.sdkadbmob.UIUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private final Runnable finishRunnable = this::finish;

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
        View appName = findViewById(R.id.appName);
        View progressContainer = findViewById(R.id.progressContainer);
        View loadingText = findViewById(R.id.loadingText);
        View adNoticeCard = findViewById(R.id.adNoticeCard);

        // App name animation - zoom in with fade
        appName.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Progress bar container - fade in with scale
        progressContainer.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(900)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Loading text - fade in with pulsing
        loadingText.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(1100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> pulseAnimation(loadingText))
                .start();

        // Ad notice card - slide up from bottom
        adNoticeCard.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(900)
                .setStartDelay(1300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void pulseAnimation(View view) {
        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(800)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> pulseAnimation(view))
                            .start();
                })
                .start();
    }

    private void checkShowAd() {
        AdMobManager.INSTANCE.initSplashScreen(this, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                goToMain();
                return null;
            }
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
        // Clear all pending callbacks and messages
        getWindow().getDecorView().removeCallbacks(finishRunnable);
        // Clear AdMob references before calling super.onDestroy()
        // This ensures App Open Ad releases its WebView and Activity references
        AdMobManager.INSTANCE.clearCurrentActivity();
        super.onDestroy();
    }
}
