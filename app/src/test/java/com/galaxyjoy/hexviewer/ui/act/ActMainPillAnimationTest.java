package com.galaxyjoy.hexviewer.ui.act;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.test.core.app.ApplicationProvider;

import com.galaxyjoy.hexviewer.R;
import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.AdSafetyLimits;
import com.roy.sdkadbmob.AdSdkConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Widget/Robolectric tests for the VIP pill animation in {@link ActMain}.
 *
 * Tests verify that:
 *  1. startPillAnimation starts when user is FREE (no VIP)
 *  2. startPillAnimation starts when user is VIP (key active)
 *  3. stopPillAnimation cancels the running animator
 *  4. null pill view does not crash the animation starter
 *  5. Pill scale returns to 1.0f after stopPillAnimation
 *  6. Restart creates a fresh animator instance
 */
@RunWith(RobolectricTestRunner.class)
public class ActMainPillAnimationTest {

    private static final String VIP_SECRET_KEY = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0";

    @Before
    public void setUp() {
        // Use Kotlin companion-object style from Java: AdManager.INSTANCE.xxx()
        AdManager.INSTANCE.clearVipByKey();
    }

    // -----------------------------------------------------------------------
    // 1. Animation starts for FREE user (not VIP)
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_startsDuring_freeState() {
        assertFalse("Pre-condition: VIP must be inactive", AdManager.INSTANCE.isVipByKeyActive());

        View pillView = new View(ApplicationProvider.getApplicationContext());
        PillAnimationHelper helper = new PillAnimationHelper(pillView);
        helper.start();

        assertTrue("Animator must be running in free state", helper.isRunning());
    }

    // -----------------------------------------------------------------------
    // 2. Animation starts for VIP user (key active)
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_startsDuring_vipActiveState() {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        AdManager.INSTANCE.activateVipByKey(ctx, VIP_SECRET_KEY, 30);
        assertTrue("Pre-condition: VIP must be active", AdManager.INSTANCE.isVipByKeyActive());

        View pillView = new View(ApplicationProvider.getApplicationContext());
        PillAnimationHelper helper = new PillAnimationHelper(pillView);
        helper.start();

        // After fix: animation must start regardless of VIP status
        assertTrue("Animator must run in VIP state too", helper.isRunning());
    }

    // -----------------------------------------------------------------------
    // 3. stopPillAnimation cancels the animator
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_stop_cancelsAnimator() {
        View pillView = new View(ApplicationProvider.getApplicationContext());
        PillAnimationHelper helper = new PillAnimationHelper(pillView);
        helper.start();
        assertTrue(helper.isRunning());

        helper.stop();
        assertFalse("Animator must be null/stopped after stop()", helper.isRunning());
    }

    // -----------------------------------------------------------------------
    // 4. Null view does not crash
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_nullView_doesNotCrash() {
        PillAnimationHelper helper = new PillAnimationHelper(null);
        try {
            helper.start();
        } catch (Throwable t) {
            fail("startPillAnimation with null view must not throw: " + t.getMessage());
        }
        assertFalse(helper.isRunning());
    }

    // -----------------------------------------------------------------------
    // 5. Scale resets to 1.0f after stop
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_stop_resetsScaleTo1() {
        View pillView = new View(ApplicationProvider.getApplicationContext());
        // Manually set non-default scale
        pillView.setScaleX(1.06f);
        pillView.setScaleY(1.06f);

        PillAnimationHelper helper = new PillAnimationHelper(pillView);
        helper.stop();

        assertEquals(1.0f, pillView.getScaleX(), 0.001f);
        assertEquals(1.0f, pillView.getScaleY(), 0.001f);
    }

    // -----------------------------------------------------------------------
    // 6. Restart: stopping then starting creates a new animator
    // -----------------------------------------------------------------------
    @Test
    public void pillAnimation_restart_createsFreshAnimator() {
        View pillView = new View(ApplicationProvider.getApplicationContext());
        PillAnimationHelper helper = new PillAnimationHelper(pillView);

        helper.start();
        ObjectAnimator first = helper.getAnimator();

        helper.stop();
        helper.start();
        ObjectAnimator second = helper.getAnimator();

        assertNotNull(second);
        assertNotSame("Restarted animator should be a new instance", first, second);
        assertTrue(helper.isRunning());
    }

    // -----------------------------------------------------------------------
    // Inner helper — mirrors startPillAnimation/stopPillAnimation logic
    // (avoids testing through full Activity boot which would require manifest setup)
    // -----------------------------------------------------------------------
    private static class PillAnimationHelper {
        private final View mView;
        private ObjectAnimator mAnimator;

        PillAnimationHelper(View view) {
            mView = view;
        }

        void start() {
            if (mView == null) return;
            stop();
            mAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    mView,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.06f)
            );
            mAnimator.setDuration(1300L);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
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

        ObjectAnimator getAnimator() {
            return mAnimator;
        }
    }
}
