package com.galaxyjoy.hexviewer.ui.act;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.R;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // Duration in milliseconds (2 seconds)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay and move to the main activity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, ActMain.class);
            startActivity(intent);
            finish(); // Close SplashActivity so the user can't go back to it
        }, SPLASH_DURATION);
    }
}
