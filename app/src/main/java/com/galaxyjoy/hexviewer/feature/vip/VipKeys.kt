package com.galaxyjoy.hexviewer.feature.vip

/**
 * Nguồn "thẻ cào" (mã cố định → số ngày VIP) cho `AdSdkConfig.vipRedeemCodes`. SDK ≥1.2.6 tự resolve
 * mã qua `AdManager.activateVipByKey(ctx, input, 0)` (thử token ECDSA trước, rồi tới map này) — app
 * KHÔNG còn tự validate/lookup nữa (audit F18, trước đây `lookupDays()` + gọi sai
 * `activateVipByKey(ctx, vipKeySecret, days)` khiến verify của SDK thành no-op).
 */
object VipKeys {
    // Base64 chỉ để tránh grep plain-text trong APK — KHÔNG phải bảo mật thật (decode tức thì).
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private const val VIP_3D_B64  = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    val VIP_30D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_30D_B64, android.util.Base64.NO_WRAP))
    }
    val VIP_3D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_3D_B64, android.util.Base64.NO_WRAP))
    }

    /** Truyền thẳng vào `AdSdkConfig.vipRedeemCodes`. */
    val REDEEM_CODES: Map<String, Int> by lazy {
        mapOf(
            VIP_30D_KEY to 30,
            VIP_3D_KEY to 3,
        )
    }
}
