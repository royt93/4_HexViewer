package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.galaxyjoy.hexviewer.R
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ActVipManagementInstrumentationTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
            // Secret chống-tamper prefs — KHÔNG còn liên quan tới verify VIP key/token (audit F2/F22).
            vipKeySecret = "test_vip_key_secret_1234567890",
            vipRedeemCodes = VipKeys.REDEEM_CODES,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)
        AdManager.clearVipByKey()

        // Reset preferences
        context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testVipScreen_InitialDisplay_FreeUser() {
        ActivityScenario.launch(ActVipManagement::class.java).use {
            // Verify status title is displayed
            onView(withId(R.id.tvStatusTitle)).check(matches(isDisplayed()))

            // Verify countdown card and progress bar are NOT displayed for free user
            onView(withId(R.id.cardVipDetails)).check(matches(not(isDisplayed())))
            onView(withId(R.id.progressVip)).check(matches(not(isDisplayed())))

            // Verify input fields are displayed
            onView(withId(R.id.etVipKey)).check(matches(isDisplayed()))
            onView(withId(R.id.btnActivate)).check(matches(isDisplayed()))
            
            // Verify activate button is disabled initially
            onView(withId(R.id.btnActivate)).check(matches(not(isEnabled())))
        }
    }

    @Test
    fun testVipScreen_TypeKey_EnablesActivateButton() {
        ActivityScenario.launch(ActVipManagement::class.java).use {
            // Initially disabled
            onView(withId(R.id.btnActivate)).check(matches(not(isEnabled())))

            // Type key
            onView(withId(R.id.etVipKey)).perform(typeText("TESTKEY"), closeSoftKeyboard())

            // Verify activate button becomes enabled
            onView(withId(R.id.btnActivate)).check(matches(isEnabled()))

            // Clear text
            onView(withId(R.id.etVipKey)).perform(clearText(), closeSoftKeyboard())

            // Verify it becomes disabled again
            onView(withId(R.id.btnActivate)).check(matches(not(isEnabled())))
        }
    }
}
