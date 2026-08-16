package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context

/**
 * `grantedAtMs` KHÔNG lưu ở đây nữa — dùng thẳng `AdManager.getVipGrantedAtMs()` (single source of
 * truth của SDK). Tự lưu riêng sẽ sai khi VIP cấp qua đường khác (auto-trial, `grantVipDays`) mà
 * `VipPrefs` không biết (audit F14).
 */
class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    fun markUserRedeemed() = sp.edit().putBoolean("user_redeemed_once", true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean("user_redeemed_once", false)
}
