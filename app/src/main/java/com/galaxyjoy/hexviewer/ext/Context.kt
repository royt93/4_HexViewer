package com.galaxyjoy.hexviewer.ext

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

//check xem app hien tai co phai la default launcher hay khong
fun Context.isDefaultLauncher(): Boolean {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            /* intent = */ intent,
            /* flags = */ PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    val currentLauncherName = resolveInfo?.activityInfo?.packageName
    if (currentLauncherName == packageName) {
        return true
    }
    return false
}

//mo app setting default cua device
fun Context.launchSystemSetting(
    packageName: String,
) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.parse("package:$packageName")
    this.startActivity(intent)
}

/*
         * send email support
         */
fun Context?.sendEmail(
) {
    val emailIntent = Intent(Intent.ACTION_SENDTO)
    emailIntent.data = Uri.parse("mailto: roy.mobile.dev@gmail.com")
    this?.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
}

const val URL_POLICY_NOTION = "https://loitp.notion.site/loitp/Privacy-Policy-319b1cd8783942fa8923d2a3c9bce60f/"

//const val URL_LICENSE = "https://raw.githubusercontent.com/gj-loitp/PocketPlan/master/LICENSE"

fun Context.openBrowserPolicy(
) {
    this.openUrlInBrowser(url = URL_POLICY_NOTION)
}

fun Context?.openUrlInBrowser(
    url: String?,
) {
    if (this == null || url.isNullOrEmpty()) {
        return
    }
    try {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        this.startActivity(i)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
