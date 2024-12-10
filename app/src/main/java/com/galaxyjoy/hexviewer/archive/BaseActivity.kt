//package com.galaxyjoy.hexviewer.archive
//
//import android.content.Context
//import android.content.res.Configuration
//import android.os.Build
//import android.view.Display
//import android.view.WindowManager
//import androidx.appcompat.app.AppCompatActivity
//import com.galaxyjoy.hexviewer.MyApplication
//import kotlin.apply
//import kotlin.collections.maxByOrNull
//
//open class BaseActivity : AppCompatActivity() {
//
//    override fun attachBaseContext(context: Context) {
//        val override = Configuration(context.resources.configuration)
//        override.fontScale = 1.0f
//        applyOverrideConfiguration(override)
////        super.attachBaseContext(context)
//        super.attachBaseContext((context.applicationContext as MyApplication).onAttach(context));
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            enableAdaptiveRefreshRate()
//        }
//    }
//
//    private fun enableAdaptiveRefreshRate() {
//        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
//        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            display // Sử dụng API mới
//        } else {
//            @Suppress("DEPRECATION") wm.defaultDisplay // Fallback cho API thấp hơn
//        }
//
//        if (display != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val supportedModes = display.supportedModes
//                val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
//                if (highestRefreshRateMode != null) {
//                    window.attributes = window.attributes.apply {
//                        preferredDisplayModeId = highestRefreshRateMode.modeId
//                    }
//                    println("Adaptive refresh rate applied: ${highestRefreshRateMode.refreshRate} Hz")
//                }
//            }
//        }
//    }
//}
