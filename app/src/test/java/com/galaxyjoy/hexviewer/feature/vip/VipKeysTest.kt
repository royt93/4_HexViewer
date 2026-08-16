package com.galaxyjoy.hexviewer.feature.vip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VipKeysTest {

    @Test
    fun testVip30DKey_decodesToExpectedPlaintext() {
        assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", VipKeys.VIP_30D_KEY)
    }

    @Test
    fun testVip3DKey_decodesToExpectedPlaintext() {
        assertEquals("eQ7@93L0f!2Y2707xN04021993u0I#2aK", VipKeys.VIP_3D_KEY)
    }

    @Test
    fun testRedeemCodes_mapsKeysToCorrectDays() {
        assertEquals(30, VipKeys.REDEEM_CODES[VipKeys.VIP_30D_KEY])
        assertEquals(3, VipKeys.REDEEM_CODES[VipKeys.VIP_3D_KEY])
    }

    @Test
    fun testRedeemCodes_unknownKeyReturnsNull() {
        assertNull(VipKeys.REDEEM_CODES["invalid_key"])
        assertNull(VipKeys.REDEEM_CODES[""])
    }
}
