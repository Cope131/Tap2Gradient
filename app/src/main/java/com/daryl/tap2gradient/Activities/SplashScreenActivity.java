package com.daryl.tap2gradient.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daryl.tap2gradient.R;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        // Splash Screen -> Main Activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // start splash screen (this) then main
                startActivity(new Intent(SplashScreenActivity.this, PreviewCameraActivity.class));
                // prevent going back to the splash screen
                finish();
            }
        }, 1000); // 2 seconds interval for splash screen
    }
}
