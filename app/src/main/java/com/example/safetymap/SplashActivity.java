package com.example.safetymap;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safetymap.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DISPLAY_LENGTH = 3000; // Duration of splash screen in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find the logo ImageView
        ImageView logo = findViewById(R.id.logo);
        ImageView logoBia = findViewById(R.id.logobia);

        // Load the fade-in and scale animations and apply them to the logo
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation scale = AnimationUtils.loadAnimation(this, R.anim.scale);



        ProgressBar progressBar = findViewById(R.id.loading_screen);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(3000); // Set duration same as the splash screen duration
        progressAnimator.setInterpolator(new LinearInterpolator());


        // Start both animations
        logo.startAnimation(fadeIn);
        logo.startAnimation(scale);

        logoBia.startAnimation(fadeIn);
        logoBia.startAnimation(scale);

        progressAnimator.start();

        // Delay the transition to the main activity for a smooth splash experience
        new Handler().postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, MapsActivity.class);
            startActivity(mainIntent);
            finish(); // Finish SplashActivity to prevent going back to it
        }, SPLASH_DISPLAY_LENGTH);
    }
}
