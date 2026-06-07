package com.roy.sdkadbmob

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Helper edge-to-edge (API 29+). */
object UIUtils {
    /** Bật edge-to-edge cho window (gọi trước setContentView). */
    fun setupEdgeToEdge1(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /** Apply system bar insets (statusbar/navbar) làm padding cho root view. */
    fun setupEdgeToEdge2(
        rootView: View,
        paddingTop: Boolean = true,
        paddingBottom: Boolean = true,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                /* left = */ systemBars.left,
                /* top = */ if (paddingTop) systemBars.top else 0,
                /* right = */ systemBars.right,
                /* bottom = */ if (paddingBottom) systemBars.bottom else 0,
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}
package com.roy.sdkadbmob

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Helper edge-to-edge (API 29+). */
object UIUtils {
    /** Bật edge-to-edge cho window (gọi trước setContentView). */
    fun setupEdgeToEdge1(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /** Apply system bar insets (statusbar/navbar) làm padding cho root view. */
    fun setupEdgeToEdge2(
        rootView: View,
        paddingTop: Boolean = true,
        paddingBottom: Boolean = true,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                /* left = */ systemBars.left,
                /* top = */ if (paddingTop) systemBars.top else 0,
                /* right = */ systemBars.right,
                /* bottom = */ if (paddingBottom) systemBars.bottom else 0,
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}
