package com.galaxyjoy.hexviewer.feature.vip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VipKeysTest {

    @Test
    fun testLookupDays_withValid30DaysKey() {
        val key30Days = VipKeys.VIP_30D_KEY
        assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", key30Days)
        assertEquals(30, VipKeys.lookupDays(key30Days))
    }

    @Test
    fun testLookupDays_withValid3DaysKey() {
        val key3Days = VipKeys.VIP_3D_KEY
        assertEquals("eQ7@93L0f!2Y2707xN04021993u0I#2aK", key3Days)
        assertEquals(3, VipKeys.lookupDays(key3Days))
    }

    @Test
    fun testLookupDays_withTrimAndCaseSensitivity() {
        val key30Days = VipKeys.VIP_30D_KEY
        assertEquals(30, VipKeys.lookupDays("  $key30Days  "))
    }

    @Test
    fun testLookupDays_withInvalidKeys() {
        assertNull(VipKeys.lookupDays("invalid_key"))
        assertNull(VipKeys.lookupDays(""))
    }
}
