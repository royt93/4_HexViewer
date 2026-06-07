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
        vipPrefs = VipPrefs(context)
        vipPrefs.clearGrantedAtMs()
        // Manually clear the redeemed flag by accessing sharedPreferences directly
        context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testDefaultValues() {
        assertEquals(0L, vipPrefs.getGrantedAtMs())
        assertFalse(vipPrefs.userRedeemedAtLeastOnce())
    }

    @Test
    fun testSaveAndGetGrantedAtMs() {
        val testMs = 123456789L
        vipPrefs.saveGrantedAtMs(testMs)
        assertEquals(testMs, vipPrefs.getGrantedAtMs())
    }

    @Test
    fun testClearGrantedAtMs() {
        vipPrefs.saveGrantedAtMs(99999L)
        vipPrefs.clearGrantedAtMs()
        assertEquals(0L, vipPrefs.getGrantedAtMs())
    }

    @Test
    fun testMarkUserRedeemed() {
        vipPrefs.markUserRedeemed()
        assertTrue(vipPrefs.userRedeemedAtLeastOnce())
    }
}
