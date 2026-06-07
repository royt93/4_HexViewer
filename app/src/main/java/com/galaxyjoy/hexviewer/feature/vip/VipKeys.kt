package com.galaxyjoy.hexviewer.feature.vip

object VipKeys {
    // Base64 của plain key ở Section 0 — AI tự encode khi compile
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
    private const val VIP_3D_B64  = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    val VIP_30D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_30D_B64, android.util.Base64.NO_WRAP))
    }
    val VIP_3D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_3D_B64, android.util.Base64.NO_WRAP))
    }

    /** Plain key (đã decode) → số ngày. Dùng để validate input từ user. */
    private val KEY_TO_DAYS: Map<String, Int> by lazy {
        mapOf(
            VIP_30D_KEY to 30,
            VIP_3D_KEY  to 3,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. Auto trim + uppercase trước khi lookup. */
    fun lookupDays(rawInput: String): Int? =
        KEY_TO_DAYS[rawInput.trim()]
}
