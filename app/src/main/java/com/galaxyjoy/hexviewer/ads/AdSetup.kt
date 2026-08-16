package com.galaxyjoy.hexviewer.ads

import android.app.Application
import android.util.Log
import com.galaxyjoy.hexviewer.BuildConfig
import com.galaxyjoy.hexviewer.feature.vip.VipKeys
import com.galaxyjoy.hexviewer.ui.act.SplashActivity
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import com.roy.sdkadbmob.ErrorReporter
import com.roy.sdkadbmob.PaidEventListener

/**
 * Cầu nối Kotlin cho [AdSdkConfig] — `MyApplication` là Java nên không gọi được named-args của
 * Kotlin data class 30+ field (không có @JvmOverloads, Java phải truyền vị trí đủ mọi field).
 * Gọi từ `MyApplication.onCreate()`.
 */
object AdSetup {

    @JvmStatic
    fun init(app: Application) {
        val config = AdSdkConfig(
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB,
            isDebug = BuildConfig.DEBUG,
            admobAppOpenId = BuildConfig.ADMOB_APP_OPEN_ID,
            admobInterstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobBannerId = BuildConfig.ADMOB_BANNER_ID,
            admobRewardedId = BuildConfig.ADMOB_REWARDED_ID,
            applovinAppOpenId = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinBannerId = BuildConfig.APPLOVIN_BANNER_ID,
            applovinRewardedId = BuildConfig.APPLOVIN_REWARDED_ID,
            applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY,
            safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits(),
            // Secret chống-tamper prefs — độc lập với VIP redeem key (audit F2/F13/F22).
            vipKeySecret = BuildConfig.VIP_KEY_SECRET,
            // Public key verify VIP token ECDSA — private key giữ ngoài app (local.properties).
            vipTokenPublicKey = BuildConfig.VIP_TOKEN_PUBLIC_KEY,
            // "Thẻ cào" cấu hình sẵn — SDK tự resolve qua activateVipByKey(ctx, input, 0), không cần
            // app tự lookup nữa (audit F18).
            vipRedeemCodes = VipKeys.REDEEM_CODES,
            applovinHasUserConsent = null, // gms: UMP ở SplashActivity quyết định.
            applovinPrivacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
            appOpenExcludedActivities = listOf(SplashActivity::class.java),
            // QC-only (debug build; SDK không forward field này ở release) — ép UMP báo geography giả
            // để test consent form EEA không cần VPN. Set qua BuildConfig.UMP_DEBUG_GEOGRAPHY.
            umpDebugGeography = BuildConfig.UMP_DEBUG_GEOGRAPHY.takeIf { BuildConfig.DEBUG && it.isNotBlank() },
            umpTestDeviceHashedIds = BuildConfig.UMP_TEST_DEVICE_HASH.takeIf { BuildConfig.DEBUG && it.isNotBlank() }
                ?.let { listOf(it) } ?: emptyList(),
        )

        AdManager.setConfig(config)

        AdManager.errorReporter = ErrorReporter { throwable, context ->
            Log.e("roy93~AdError", context, throwable)
        }

        // BẮT BUỘC set trong Application.onCreate() (trước khi có Activity nào) — set trong Activity
        // sẽ bị SDK tự xoá khi Activity đó destroy (owner-tracking chống leak).
        AdManager.paidEventListener = PaidEventListener { adType, valueMicros, currency, precision, adSource ->
            Log.d(
                "roy93~AdRevenue",
                "$adType value=${valueMicros / 1_000_000.0} $currency precision=$precision source=$adSource",
            )
        }

        AdManager.initialize(app) { success, gaid ->
            Log.d("roy93~", "AdManager init success=$success, gaid=$gaid")

            // Bước 3b (AD_PROMPT_AOS.MD) — bảo vệ tài khoản khỏi invalid traffic khi QA test bằng
            // ad-unit production thật. Điền GAID thật vào BuildConfig.TEST_DEVICE_GAID_* khi có.
            if (BuildConfig.DEBUG) {
                val testDeviceIds = listOf(BuildConfig.TEST_DEVICE_GAID_1, BuildConfig.TEST_DEVICE_GAID_2)
                    .filter { it.isNotBlank() }
                if (testDeviceIds.isNotEmpty()) {
                    AdManager.setTestDeviceIds(*testDeviceIds.toTypedArray())
                }
                // QC-only: dump toàn bộ trạng thái ad để soi qua logcat tag roy93~Diag.
                Log.d("roy93~Diag", AdManager.getDiagnostics())
            }
        }
    }
}
