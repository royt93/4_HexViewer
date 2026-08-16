package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import com.roy.sdkadbmob.InternalAdApi
import com.roy.sdkadbmob.clearAppPreferencesForTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(InternalAdApi::class)
class AdIntegrationTest {

    private lateinit var context: Context

    // Secret chống-tamper prefs — KHÔNG còn liên quan tới verify VIP key/token (audit F2/F22).
    private val vipSecret = "test_vip_key_secret_1234567890"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // API test chính thức của SDK ("chỉ dùng trong instrumented tests để reset state") — xoá cả
        // VIP state lẫn token fingerprint/consent decision, đúng hơn tự xoá SharedPreferences thô.
        AdManager.clearAppPreferencesForTest(context)
        // Reset VIP key before each test
        AdManager.clearVipByKey()
        // activateVipByKey (redeem code/token) yêu cầu có mạng (V-03) — Robolectric mặc định
        // active network không có NET_CAPABILITY_INTERNET/VALIDATED.
        TestNetworkUtils.simulateConnected(context)
    }

    private fun baseConfig(isEnableAdmob: Boolean) = AdSdkConfig(
        isEnableAdmob = isEnableAdmob,
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
        vipKeySecret = vipSecret,
        // "Thẻ cào" — SDK tự resolve qua activateVipByKey(ctx, input, 0) (audit F18).
        vipRedeemCodes = VipKeys.REDEEM_CODES,
        applovinSdkKey = "test_sdk_key"
    )

    @Test
    fun testAdSdkConfig_AdMobMode() {
        val config = baseConfig(isEnableAdmob = true)
        AdManager.setConfig(config)

        assertEquals(config, AdManager.adConfig)
        assertTrue(AdManager.adConfig.isEnableAdmob)
    }

    @Test
    fun testAdSdkConfig_AppLovinMode() {
        val config = baseConfig(isEnableAdmob = false)
        AdManager.setConfig(config)

        assertEquals(config, AdManager.adConfig)
        assertFalse(AdManager.adConfig.isEnableAdmob)
    }

    @Test
    fun testVipActivationWorkflow_viaRedeemCode() {
        AdManager.setConfig(baseConfig(isEnableAdmob = false))

        // VIP should not be active initially
        assertFalse(AdManager.isVipByKeyActive())

        // Activate VIP — SDK tự resolve mã thẻ cào qua AdSdkConfig.vipRedeemCodes, `days` bị bỏ qua
        // (audit F13/F18/F22: KHÔNG còn truyền vipKeySecret làm "key" như cách cũ đã fix).
        val success = AdManager.activateVipByKey(context, VipKeys.VIP_30D_KEY, 0)
        assertTrue(success)
        assertTrue(AdManager.isVipByKeyActive())
        assertTrue(AdManager.getVipByKeyExpiry() > System.currentTimeMillis())

        // Clear VIP
        AdManager.clearVipByKey()
        assertFalse(AdManager.isVipByKeyActive())
    }

    @Test
    fun testVipActivation_with3DayRedeemCode() {
        AdManager.setConfig(baseConfig(isEnableAdmob = false))

        val success = AdManager.activateVipByKey(context, VipKeys.VIP_3D_KEY, 0)
        assertTrue(success)
        assertTrue(AdManager.isVipByKeyActive())

        // Expiry should roughly match 3 days
        val expiry = AdManager.getVipByKeyExpiry()
        val remainingMs = expiry - System.currentTimeMillis()
        val remainingDays = remainingMs / (24 * 3600 * 1000).toDouble()
        assertTrue(remainingDays > 2.9 && remainingDays <= 3.0)
    }

    @Test
    fun testGrantVipDays_rewardPath() {
        AdManager.setConfig(baseConfig(isEnableAdmob = false))

        // grantVipDays = nguồn tin cậy nội bộ (reward đã earned), KHÔNG qua verify key/token
        // (audit F13/F22) — khớp đúng đường code thật `grantVipFromAd()` dùng.
        val success = AdManager.grantVipDays(context, 3)
        assertTrue(success)
        assertTrue(AdManager.isVipByKeyActive())

        val expiry = AdManager.getVipByKeyExpiry()
        val remainingMs = expiry - System.currentTimeMillis()
        val remainingDays = remainingMs / (24 * 3600 * 1000).toDouble()
        assertTrue(remainingDays > 2.9 && remainingDays <= 3.0)
    }

    // Lưu ý: KHÔNG thêm test "activateVipByKey trả false với mã sai" trong class này — mỗi lần fail
    // trip `vipActivationBackoff` (cooldown chống brute-force, sống hết đời process/test JVM, SDK cố ý
    // KHÔNG cho `AdManager.destroy()` reset — xem KDoc `AdManager.kt` dòng ~5251), khiến các test khác
    // dùng mã ĐÚNG chạy sau đó trong cùng JVM bị treo cooldown và fail oan. `resetVipActivationBackoffForTest`
    // là `internal` (module-private), :app không gọi được từ ngoài SDK.
}
