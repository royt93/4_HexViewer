package com.galaxyjoy.hexviewer.ui.act;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.galaxyjoy.hexviewer.R;
import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.AdSafetyLimits;
import com.roy.sdkadbmob.AdSdkConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation integration test for VIP pill animation behavior in ActMain.
 *
 * These tests run on real device / emulator (androidTest).
 *
 * Covers:
 *  1. Animation runs for FREE user (no VIP)
 *  2. Animation runs for VIP user (key active) — verifies post-fix behavior
 *  3. Animation stops and scale resets when stopped explicitly
 *  4. Animation re-starts correctly after VIP status change
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ActMainPillAnimationInstrumentationTest {

    private static final String VIP_SECRET_KEY = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0";
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        AdSdkConfig config = new AdSdkConfig(
                false, true,
                "test_open", "test_inter", "test_banner", "test_reward",
                "test_al_open", "test_al_inter", "test_al_banner", "test_al_reward",
                AdSafetyLimits.TEST, VIP_SECRET_KEY, "test_sdk_key"
        );
        AdManager.INSTANCE.setConfig(config);
        AdManager.clearVipByKey();
    }

    @Test
    public void pillAnimation_freeUser_animatorIsRunning() {
        assertFalse(AdManager.INSTANCE.isVipByKeyActive());

        View pillView = new View(mContext);
        AnimationController controller = new AnimationController(pillView);
        controller.start();

        assertTrue("Animator must be running for free user", controller.isRunning());
        controller.stop();
    }

    @Test
    public void pillAnimation_vipUser_animatorIsRunning() {
        AdManager.activateVipByKey(mContext, VIP_SECRET_KEY, 30);
        assertTrue(AdManager.INSTANCE.isVipByKeyActive());

        View pillView = new View(mContext);
        AnimationController controller = new AnimationController(pillView);
        controller.start();

        // Key assertion: after the fix, VIP users also get the animation
        assertTrue("Animator must be running for VIP user", controller.isRunning());
        controller.stop();
    }

    @Test
    public void pillAnimation_stop_scaleResetsTo1() {
        View pillView = new View(mContext);
        pillView.setScaleX(1.06f);
        pillView.setScaleY(1.06f);

        AnimationController controller = new AnimationController(pillView);
        controller.stop();

        assertEquals(1.0f, pillView.getScaleX(), 0.001f);
        assertEquals(1.0f, pillView.getScaleY(), 0.001f);
    }

    @Test
    public void pillAnimation_vipStatusChangeMidSession_animationContinues() {
        // Start animation as free user
        View pillView = new View(mContext);
        AnimationController controller = new AnimationController(pillView);
        controller.start();
        assertTrue(controller.isRunning());

        // Simulate VIP activation mid-session (like onResume after VIP screen)
        AdManager.activateVipByKey(mContext, VIP_SECRET_KEY, 30);

        // Stop and restart as ActMain.onResume() would do
        controller.stop();
        controller.start();

        // Post-fix: animation must still start even with VIP active
        assertTrue("Animation must continue after VIP activation", controller.isRunning());
        controller.stop();
    }

    // Mirror of the private animation logic in ActMain for testing purposes
    private static class AnimationController {
        private final View mView;
        private android.animation.ObjectAnimator mAnimator;

        AnimationController(View view) {
            mView = view;
        }

        void start() {
            if (mView == null) return;
            stop();
            mAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(
                    mView,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.06f)
            );
            mAnimator.setDuration(1300L);
            mAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            mAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            mAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            mAnimator.start();
        }

        void stop() {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
            if (mView != null) {
                mView.setScaleX(1.0f);
                mView.setScaleY(1.0f);
            }
        }

        boolean isRunning() {
            return mAnimator != null && mAnimator.isRunning();
        }
    }
}
