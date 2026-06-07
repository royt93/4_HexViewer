package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context

class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    fun saveGrantedAtMs(ms: Long) = sp.edit().putLong("granted_at_ms", ms).apply()
    fun getGrantedAtMs(): Long = sp.getLong("granted_at_ms", 0L)
    fun clearGrantedAtMs() = sp.edit().remove("granted_at_ms").apply()

    fun markUserRedeemed() = sp.edit().putBoolean("user_redeemed_once", true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean("user_redeemed_once", false)
}
