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
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    private Locale currentLocale;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLocale = getResources().getConfiguration().locale;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Check if locale changed
        if (currentLocale != null && !currentLocale.equals(newConfig.locale)) {
            // Locale changed, recreate activity to apply new language
            recreate();
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        // First apply language from MyApplication
        Context newContext = ((MyApplication) context.getApplicationContext()).onAttach(context);
        super.attachBaseContext(newContext);
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            // Only override fontScale, preserve locale for language to work
            overrideConfiguration.fontScale = 1.0f;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
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
            try {
                display = getDisplay(); // Sử dụng API mới
            } catch (Throwable t) {
                display = wm != null ? wm.getDefaultDisplay() : null;
            }
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
                    // Use proper Android logging instead of System.out
                    if (BuildConfig.DEBUG) {
                        Log.d("BaseActivity", "Adaptive refresh rate applied: " + highestRefreshRateMode.getRefreshRate() + " Hz");
                    }
                }
            }
        }
    }
}
