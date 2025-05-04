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

import java.util.concurrent.atomic.AtomicBoolean;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        checkShowAd();
    }

    private void checkShowAd() {
        AdMobManager.INSTANCE.loadAppOpenAd(
                SplashActivity.this,
                BuildConfig.ADMOB_APP_OPEN_ID,
                new Function1<Boolean, Unit>() {
                    @Override
                    public Unit invoke(Boolean aBoolean) {
                        Log.d("roy93~", "loadAppOpenAd aBoolean " + aBoolean);
                        goToMain();
                        AdMobManager.INSTANCE.showAppOpenAd(SplashActivity.this);
                        return null;
                    }
                }
        );

//        final Handler handler = new Handler(Looper.getMainLooper());
//        final AtomicBoolean hasCalledGoToMain = new AtomicBoolean(false);
//
//        new Thread(() -> {
//            final Runnable delayedRunnable = () -> {
//                if (hasCalledGoToMain.compareAndSet(false, true)) {
//                    Log.d("roy93~", "goToMain #1");
//                    runOnUiThread(this::goToMain);
//                }
//            };
//
//            handler.postDelayed(delayedRunnable, 3000);
//
//            // Load quảng cáo
//            runOnUiThread(() -> AdMobManager.INSTANCE.loadAppOpenAd(
//                    SplashActivity.this,
//                    BuildConfig.ADMOB_APP_OPEN_ID,
//                    new Function0<Unit>() {
//                        @Override
//                        public Unit invoke() {
//                            if (hasCalledGoToMain.compareAndSet(false, true)) {
//                                handler.removeCallbacks(delayedRunnable);
//                                Log.d("roy93~", "goToMain #2");
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        goToMain();
//                                        AdMobManager.INSTANCE.showAppOpenAd(SplashActivity.this);
//                                    }
//                                });
//                            }
//                            return null;
//                        }
//                    }
//            ));
//        }).start();
    }

    private void goToMain() {
        Intent intent = new Intent(SplashActivity.this, ActMain.class);
        startActivity(intent);
        finish(); // Close SplashActivity so the user can't go back to it
    }
}
