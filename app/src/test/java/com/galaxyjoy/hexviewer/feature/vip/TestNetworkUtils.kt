package com.galaxyjoy.hexviewer.feature.vip

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.robolectric.Shadows.shadowOf

/**
 * SDK 1.6.16 gate `activateVipByKey` sau `NetworkUtils.isDeviceConnected()` (V-03, xem
 * `AdManager.kt`) — Robolectric mặc định active network KHÔNG có `NET_CAPABILITY_INTERNET`/
 * `NET_CAPABILITY_VALIDATED` (chỉ set transport type), nên phải tự set trước khi test path VIP
 * redeem/token. `grantVipDays` KHÔNG bị gate này, không cần hàm này khi test đường reward.
 */
internal object TestNetworkUtils {
    fun simulateConnected(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowCm = shadowOf(cm)
        val activeNetwork = cm.activeNetwork ?: return
        val capabilities = (cm.getNetworkCapabilities(activeNetwork) ?: NetworkCapabilities())
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadowCm.setNetworkCapabilities(activeNetwork, capabilities)
    }
}
