package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdIntegrationTest {

    private lateinit var context: Context
    private val vipSecretKey = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset VIP key before each test
        AdManager.clearVipByKey()
    }

    @Test
    fun testAdSdkConfig_AdMobMode() {
        val config = AdSdkConfig(
            isEnableAdmob = true,
            isDebug = true,
            admobAppOpenId = "test_admob_open",
            admobInterstitialId = "test_admob_inter",
            admobBannerId = "test_admob_banner",
            admobRewardedId = "test_admob_reward",
            applovinAppOpenId = "test_applovin_open",
            applovinInterstitialId = "test_applovin_inter",
            applovinBannerId = "test_applovin_banner",
            applovinRewardedId = "test_applovin_reward",
            safety = AdSafetyLimits.TEST,
            vipKeySecret = vipSecretKey,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)

        assertEquals(config, AdManager.adConfig)
        assertTrue(AdManager.adConfig.isEnableAdmob)
    }

    @Test
    fun testAdSdkConfig_AppLovinMode() {
        val config = AdSdkConfig(
            isEnableAdmob = false, // AppLovin mode (primary)
            isDebug = true,
            admobAppOpenId = "test_admob_open",
            admobInterstitialId = "test_admob_inter",
            admobBannerId = "test_admob_banner",
            admobRewardedId = "test_admob_reward",
            applovinAppOpenId = "test_applovin_open",
            applovinInterstitialId = "test_applovin_inter",
            applovinBannerId = "test_applovin_banner",
            applovinRewardedId = "test_applovin_reward",
            safety = AdSafetyLimits.TEST,
            vipKeySecret = vipSecretKey,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)

        assertEquals(config, AdManager.adConfig)
        assertFalse(AdManager.adConfig.isEnableAdmob)
    }

    @Test
    fun testVipActivationWorkflow() {
        // Initialize config
        val config = AdSdkConfig(
            isEnableAdmob = false,
            isDebug = true,
            admobAppOpenId = "test_admob_open",
            admobInterstitialId = "test_admob_inter",
            admobBannerId = "test_admob_banner",
            admobRewardedId = "test_admob_reward",
            applovinAppOpenId = "test_applovin_open",
            applovinInterstitialId = "test_applovin_inter",
            applovinBannerId = "test_applovin_banner",
            applovinRewardedId = "test_applovin_reward",
            safety = AdSafetyLimits.TEST,
            vipKeySecret = vipSecretKey,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)

        // VIP should not be active initially
        assertFalse(AdManager.isVipByKeyActive())

        // Activate VIP
        val success = AdManager.activateVipByKey(context, vipSecretKey, 30)
        assertTrue(success)
        assertTrue(AdManager.isVipByKeyActive())
        assertTrue(AdManager.getVipByKeyExpiry() > System.currentTimeMillis())

        // Clear VIP
        AdManager.clearVipByKey()
        assertFalse(AdManager.isVipByKeyActive())
    }

    @Test
    fun testVipBypassWith3DaysKey() {
        // Initialize config
        val config = AdSdkConfig(
            isEnableAdmob = false,
            isDebug = true,
            admobAppOpenId = "test_admob_open",
            admobInterstitialId = "test_admob_inter",
            admobBannerId = "test_admob_banner",
            admobRewardedId = "test_admob_reward",
            applovinAppOpenId = "test_applovin_open",
            applovinInterstitialId = "test_applovin_inter",
            applovinBannerId = "test_applovin_banner",
            applovinRewardedId = "test_applovin_reward",
            safety = AdSafetyLimits.TEST,
            vipKeySecret = vipSecretKey,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)

        // Lookup days from the 3-day key
        val key3Days = VipKeys.VIP_3D_KEY
        val days = VipKeys.lookupDays(key3Days)
        assertNotNull(days)
        assertEquals(3, days)

        // Activate using bypass flow (using the configuration vipKeySecret & days resolved locally)
        val success = AdManager.activateVipByKey(context, AdManager.adConfig.vipKeySecret, days!!)
        assertTrue(success)
        assertTrue(AdManager.isVipByKeyActive())

        // Expiry should roughly match 3 days
        val expiry = AdManager.getVipByKeyExpiry()
        val remainingMs = expiry - System.currentTimeMillis()
        val remainingDays = remainingMs / (24 * 3600 * 1000).toDouble()
        assertTrue(remainingDays > 2.9 && remainingDays <= 3.0)
    }
}
