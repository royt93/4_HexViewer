package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.galaxyjoy.hexviewer.R
import com.google.android.material.textfield.TextInputLayout
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyLimits
import com.roy.sdkadbmob.AdSdkConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ActVipManagementTest {

    private val vipSecretKey = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0"

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
            vipKeySecret = vipSecretKey,
            applovinSdkKey = "test_sdk_key"
        )
        AdManager.setConfig(config)
        AdManager.clearVipByKey()

        // Clear local preferences
        VipPrefs(context).clearGrantedAtMs()
        context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun getDialogTitle(dialog: androidx.appcompat.app.AlertDialog?): String {
        if (dialog == null) return ""
        val titleView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        return titleView?.text?.toString() ?: ""
    }

    @Test
    fun testActivityLaunchesAndDisplaysFreeUserState() {
        ActivityScenario.launch(ActVipManagement::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStatusTitle = activity.findViewById<TextView>(R.id.tvStatusTitle)
                assertNotNull(tvStatusTitle)
                assertEquals(activity.getString(R.string.vip_free_user), tvStatusTitle.text)

                // VIP Countdown card should be hidden for free users
                val cardVipDetails = activity.findViewById<View>(R.id.cardVipDetails)
                assertEquals(View.GONE, cardVipDetails.visibility)

                // Activate button should be disabled initially (empty key)
                val btnActivate = activity.findViewById<View>(R.id.btnActivate)
                assertFalse(btnActivate.isEnabled)

                // Watch ad button should be enabled for free users
                val btnWatchAd = activity.findViewById<View>(R.id.btnWatchAd)
                assertTrue(btnWatchAd.isEnabled)
            }
        }
    }

    @Test
    fun testActivateButton_enablesWhenKeyNotEmpty() {
        ActivityScenario.launch(ActVipManagement::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val etKey = activity.findViewById<EditText>(R.id.etVipKey)
                val btnActivate = activity.findViewById<View>(R.id.btnActivate)

                assertFalse(btnActivate.isEnabled)

                etKey.setText("key")
                assertTrue(btnActivate.isEnabled)

                etKey.setText("")
                assertFalse(btnActivate.isEnabled)
            }
        }
    }

    @Test
    fun testActivateButton_whenInvalidKey_showsFailureDialog() {
        ActivityScenario.launch(ActVipManagement::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val etKey = activity.findViewById<EditText>(R.id.etVipKey)
                val btnActivate = activity.findViewById<View>(R.id.btnActivate)

                etKey.setText("wrong_key_123")
                btnActivate.performClick()
            }

            // Advance main looper by 1500ms to trigger delayed verification response
            shadowOf(android.os.Looper.getMainLooper()).idleFor(1500, TimeUnit.MILLISECONDS)

            val latestDialog = ShadowDialog.getLatestDialog() as? androidx.appcompat.app.AlertDialog
            assertNotNull("Failure dialog should be displayed", latestDialog)
            scenario.onActivity { activity ->
                val title = getDialogTitle(latestDialog)
                assertEquals(activity.getString(R.string.vip_failed_title), title)
            }
        }
    }

    @Test
    fun testActivateButton_whenValidKey_showsSuccessDialogAndClearsEditText() {
        ActivityScenario.launch(ActVipManagement::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val etKey = activity.findViewById<EditText>(R.id.etVipKey)
                val btnActivate = activity.findViewById<View>(R.id.btnActivate)

                // Use the valid 30-day key
                etKey.setText("9fA0q7eN!27cLx04@21993Y2u0I7#Q0")
                btnActivate.performClick()
            }

            // Advance main looper by 1500ms to trigger delayed verification response
            shadowOf(android.os.Looper.getMainLooper()).idleFor(1500, TimeUnit.MILLISECONDS)

            assertTrue(AdManager.isVipByKeyActive())

            val latestDialog = ShadowDialog.getLatestDialog() as? androidx.appcompat.app.AlertDialog
            assertNotNull("Success dialog should be displayed", latestDialog)
            
            scenario.onActivity { activity ->
                val title = getDialogTitle(latestDialog)
                assertEquals(activity.getString(R.string.vip_success_title), title)
                
                // EditText should be cleared
                val etKey = activity.findViewById<EditText>(R.id.etVipKey)
                assertEquals("", etKey.text.toString())
            }
        }
    }

    @Test
    fun testRevokeVipButton_clearsVipState() {
        // Activate VIP beforehand
        val context = ApplicationProvider.getApplicationContext<Context>()
        AdManager.activateVipByKey(context, vipSecretKey, 30)
        VipPrefs(context).saveGrantedAtMs(System.currentTimeMillis())

        ActivityScenario.launch(ActVipManagement::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tvStatusTitle = activity.findViewById<TextView>(R.id.tvStatusTitle)
                assertEquals(activity.getString(R.string.vip_active), tvStatusTitle.text)

                val btnRevoke = activity.findViewById<View>(R.id.btnRevokeVip)
                assertNotNull(btnRevoke)
                btnRevoke.performClick()
            }

            // Revocation confirmation dialog shown immediately
            val latestDialog = ShadowDialog.getLatestDialog() as? androidx.appcompat.app.AlertDialog
            assertNotNull(latestDialog)
            
            scenario.onActivity { activity ->
                val title = getDialogTitle(latestDialog)
                assertEquals(activity.getString(R.string.vip_revoke_all_confirm_title), title)
            }

            // Click confirm
            latestDialog?.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.performClick()
            shadowOf(android.os.Looper.getMainLooper()).idle()

            // VIP should be cleared
            assertFalse(AdManager.isVipByKeyActive())
            scenario.onActivity { activity ->
                val tvStatusTitle = activity.findViewById<TextView>(R.id.tvStatusTitle)
                assertEquals(activity.getString(R.string.vip_free_user), tvStatusTitle.text)
                assertEquals("VIP Revoked", ShadowToast.getTextOfLatestToast())
            }
        }
    }
}
