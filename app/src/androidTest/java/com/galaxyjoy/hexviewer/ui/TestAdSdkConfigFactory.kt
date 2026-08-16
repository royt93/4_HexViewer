package com.galaxyjoy.hexviewer.ui.act

import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig

/**
 * Cầu nối Kotlin cho test Java (`ActMainPillAnimationInstrumentationTest`) — Java không gọi được
 * named-args của `AdSdkConfig` (30+ field, không có @JvmOverloads). Mirror `AdSetup.kt` phía production.
 */
object TestAdSdkConfigFactory {
    @JvmStatic
    fun create(vipKeySecret: String): AdSdkConfig = AdSdkConfig(
        isEnableAdmob = false,
        isDebug = true,
        admobAppOpenId = "test_open",
        admobInterstitialId = "test_inter",
        admobBannerId = "test_banner",
        admobRewardedId = "test_reward",
        applovinAppOpenId = "test_al_open",
        applovinInterstitialId = "test_al_inter",
        applovinBannerId = "test_al_banner",
        applovinRewardedId = "test_al_reward",
        safety = AdSafetyLimits.TEST,
        vipKeySecret = vipKeySecret,
        applovinSdkKey = "test_sdk_key",
    )
}
