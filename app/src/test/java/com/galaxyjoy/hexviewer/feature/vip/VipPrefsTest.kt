package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VipPrefsTest {

    private lateinit var context: Context
    private lateinit var vipPrefs: VipPrefs

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        vipPrefs = VipPrefs(context)
    }

    @Test
    fun testDefaultValue() {
        assertFalse(vipPrefs.userRedeemedAtLeastOnce())
    }

    @Test
    fun testMarkUserRedeemed() {
        vipPrefs.markUserRedeemed()
        assertTrue(vipPrefs.userRedeemedAtLeastOnce())
    }
}
