package com.example.logidesignai;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private VideoView videoViewSplash;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        videoViewSplash = findViewById(R.id.videoViewSplash);
        playSplashVideo();
    }

    private void playSplashVideo() {
        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            videoViewSplash.setVideoURI(videoUri);

            // Al terminar la reproducción del video, saltar al chat principal
            videoViewSplash.setOnCompletionListener(mp -> navigateToMainChat());

            // Si ocurre algún problema con el códec o video, pasar al chat sin trabar la app
            videoViewSplash.setOnErrorListener((mp, what, extra) -> {
                navigateToMainChat();
                return true;
            });

            // Permitir al usuario tocar la pantalla para saltar el video inmediatamente
            findViewById(R.id.splashContainer).setOnClickListener(v -> navigateToMainChat());

            videoViewSplash.setOnPreparedListener(mp -> {
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                videoViewSplash.start();
            });

        } catch (Exception e) {
            navigateToMainChat();
        }
    }

    private synchronized void navigateToMainChat() {
        if (isNavigating) return;
        isNavigating = true;

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}

