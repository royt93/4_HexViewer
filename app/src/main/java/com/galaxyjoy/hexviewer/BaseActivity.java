/**
 * ******************************************************************************
 * <p><b>Project HexViewer</b><br/>
 * </p>
 *
 * @author Keidan
 * <p>
 * License: GPLv3
 * </p>
 * ******************************************************************************
 */
package com.galaxyjoy.hexviewer;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context context) {
        Configuration override = new Configuration(context.getResources().getConfiguration());
        override.fontScale = 1.0f;
        applyOverrideConfiguration(override);
        super.attachBaseContext(((MyApplication) context.getApplicationContext()).onAttach(context));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableAdaptiveRefreshRate();
        }
    }

    private void enableAdaptiveRefreshRate() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = getDisplay(); // Sử dụng API mới
        } else {
            // Fallback cho API thấp hơn
            display = wm != null ? wm.getDefaultDisplay() : null;
        }

        if (display != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Display.Mode[] supportedModes = display.getSupportedModes();
                Display.Mode highestRefreshRateMode = null;

                for (Display.Mode mode : supportedModes) {
                    if (highestRefreshRateMode == null || mode.getRefreshRate() > highestRefreshRateMode.getRefreshRate()) {
                        highestRefreshRateMode = mode;
                    }
                }

                if (highestRefreshRateMode != null) {
                    getWindow().setAttributes(getWindow().getAttributes());
                    getWindow().getAttributes().preferredDisplayModeId = highestRefreshRateMode.getModeId();
                    System.out.println("Adaptive refresh rate applied: " + highestRefreshRateMode.getRefreshRate() + " Hz");
                }
            }
        }
    }
}
