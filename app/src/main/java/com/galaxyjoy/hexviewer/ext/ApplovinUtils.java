package com.galaxyjoy.hexviewer.ext;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.galaxyjoy.hexviewer.BuildConfig;
import com.galaxyjoy.hexviewer.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApplovinUtils {
    public static void setupApplovinAd(Context context) {
        // Please check config in gradle

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppLovinSdkInitializationConfiguration.Builder initConfigBuilder =
                    AppLovinSdkInitializationConfiguration.builder(
                            context.getString(R.string.SDK_KEY),
                            context
                    );
            initConfigBuilder.setMediationProvider(AppLovinMediationProvider.MAX);

            // Enable test mode by default for the current device. Cannot be run on the main thread.
            /*
            try {
                String currentGaid = AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
                if (currentGaid != null) {
                    initConfigBuilder.setTestDeviceAdvertisingIds(Collections.singletonList(currentGaid));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            // Initialize the AppLovin SDK
            AppLovinSdk sdk = AppLovinSdk.getInstance(context);
            sdk.initialize(initConfigBuilder.build(), config -> {
                // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached
                android.util.Log.e("Applovin", "setupAd initializeSdk " + config);

                if (BuildConfig.DEBUG) {
                    Toast.makeText(
                            context,
                            "Debug toast initializeSdk isTestModeEnabled: " + config.isTestModeEnabled(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

            executor.shutdown();
        });
    }

    public static void showMediationDebuggerApplovin(Context context) {
        if (BuildConfig.DEBUG) {
            AppLovinSdk.getInstance(context).showMediationDebugger();
        } else {
            Toast.makeText(
                    context,
                    "This feature is only available in debug mode",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    public static MaxAdView createAdBanner(
            Activity activity,
            String logTag,
            int bkgColor,
            ViewGroup viewGroup,
            boolean isAdaptiveBanner
    ) {
        String log = logTag + " - createAdBanner";
        boolean enableAdBanner = "true".equals(activity.getString(R.string.EnableAdBanner));
        String id = "1234567890123456"; // dummy id
        if (enableAdBanner) {
            id = activity.getString(R.string.BANNER);
            // viewGroup.setVisibility(View.VISIBLE);
        } else {
            // viewGroup.setVisibility(View.GONE);
        }
        android.util.Log.i(log, "enableAdBanner " + enableAdBanner + " -> " + id);

        MaxAdView adView = new MaxAdView(id, activity);
        adView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdLoaded");
                // viewGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdDisplayed");
            }

            @Override
            public void onAdHidden(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdHidden");
            }

            @Override
            public void onAdClicked(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdClicked");
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError maxError) {
                android.util.Log.i(log, "onAdLoadFailed");
                if (viewGroup != null) {
                    viewGroup.setVisibility(ViewGroup.GONE);
                }
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError maxError) {
                android.util.Log.i(log, "onAdDisplayFailed");
            }

            @Override
            public void onAdExpanded(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdExpanded");
            }

            @Override
            public void onAdCollapsed(@NonNull MaxAd maxAd) {
                android.util.Log.i(log, "onAdCollapsed");
            }
        });

        adView.setRevenueListener(maxAd -> android.util.Log.i(log, "onAdRevenuePaid"));

        if (isAdaptiveBanner) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).getHeight();
            int heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp);

            adView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
            adView.setExtraParameter("adaptive_banner", "true");
            adView.setLocalExtraParameter("adaptive_banner_width", 400);
        } else {
            boolean isTablet = AppLovinSdkUtils.isTablet(activity);
            int heightPx = AppLovinSdkUtils.dpToPx(activity, isTablet ? 90 : 50);

            adView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx
            ));
        }

        if (enableAdBanner) {
            adView.setBackgroundColor(bkgColor);
        } else {
            adView.setBackgroundColor(Color.TRANSPARENT);
        }

        if (viewGroup != null) {
            viewGroup.addView(adView);
        }
        adView.loadAd();
        return adView;
    }

    public static void destroyAdBanner(ViewGroup viewGroup, MaxAdView adView) {
        if (adView != null) {
            adView.destroy();
        }
        viewGroup.removeAllViews();
    }
}