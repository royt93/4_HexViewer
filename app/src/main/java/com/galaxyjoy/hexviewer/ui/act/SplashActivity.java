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
package com.galaxyjoy.hexviewer.ui.act;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.BuildConfig;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.sdkadbmob.AdMobManager;
import com.galaxyjoy.hexviewer.sdkadbmob.UIUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.activity_splash);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.layoutRoot), true, true);
        checkShowAd();
    }

    private void checkShowAd() {
        AdMobManager.INSTANCE.initSplashScreen(this, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                goToMain();
                return null;
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(SplashActivity.this, ActMain.class);
        startActivity(intent);
//        finish(); // Close SplashActivity so the user can't go back to it
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 300);
    }
}
