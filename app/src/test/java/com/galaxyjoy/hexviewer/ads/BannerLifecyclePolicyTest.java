package com.galaxyjoy.hexviewer.ads;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.view.View;
import android.view.ViewParent;

import org.junit.Test;

public class BannerLifecyclePolicyTest {
    @Test
    public void nullBanner_isNotDetached() {
        assertFalse(BannerLifecyclePolicy.isDetached(null));
    }

    @Test
    public void attachedBanner_isNotStale() {
        View view = mock(View.class);
        when(view.getParent()).thenReturn(mock(ViewParent.class));
        assertFalse(BannerLifecyclePolicy.isDetached(view));
    }

    @Test
    public void sdkDestroyedBanner_isDetachedAndMustBeReloaded() {
        View view = mock(View.class);
        when(view.getParent()).thenReturn(null);
        assertTrue(BannerLifecyclePolicy.isDetached(view));
    }
}
