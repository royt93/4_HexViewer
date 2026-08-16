package com.galaxyjoy.hexviewer.ads;

import android.view.View;

/** Host-side checks required when the SDK invalidates a banner after a privacy change. */
public final class BannerLifecyclePolicy {
    private BannerLifecyclePolicy() {}

    public static boolean isDetached(View adView) {
        return adView != null && adView.getParent() == null;
    }
}
