package com.example.logidesignai;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private VideoView videoViewSplash;
    private View videoCoverView;
    private boolean isNavigating = false;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());

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
        videoCoverView = findViewById(R.id.videoCoverView);

        // Click en cualquier parte para navegar de inmediato
        View container = findViewById(R.id.splashContainer);
        if (container != null) container.setOnClickListener(v -> navigateToMainChat());
        if (videoCoverView != null) videoCoverView.setOnClickListener(v -> navigateToMainChat());
        if (videoViewSplash != null) videoViewSplash.setOnClickListener(v -> navigateToMainChat());

        // Temporizador de seguridad de 3 segundos para garantizar transicion
        splashHandler.postDelayed(this::navigateToMainChat, 3000);

        playSplashVideo();
    }

    private void playSplashVideo() {
        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video);
            videoViewSplash.setVideoURI(videoUri);

            // Desvanecer la cubierta unicamente cuando el primer fotograma del video ya este dibujado
            videoViewSplash.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    desvanecerCubierta();
                    return true;
                }
                return false;
            });

            // Respaldo tras 1200ms por si el evento no fuera emitido por el hardware
            splashHandler.postDelayed(this::desvanecerCubierta, 1200);

            // Al terminar el video saltar al chat principal
            videoViewSplash.setOnCompletionListener(mp -> navigateToMainChat());

            // En caso de error en el video, pasar al chat
            videoViewSplash.setOnErrorListener((mp, what, extra) -> {
                navigateToMainChat();
                return true;
            });

            videoViewSplash.setOnPreparedListener(mp -> {
                try {
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    videoViewSplash.start();
                } catch (Exception ignored) {
                    videoViewSplash.start();
                }
            });

        } catch (Exception e) {
            navigateToMainChat();
        }
    }

    private void desvanecerCubierta() {
        if (videoCoverView != null && videoCoverView.getVisibility() == View.VISIBLE) {
            videoCoverView.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(() -> videoCoverView.setVisibility(View.GONE))
                    .start();
        }
    }

    private synchronized void navigateToMainChat() {
        if (isNavigating) return;
        isNavigating = true;

        splashHandler.removeCallbacksAndMessages(null);

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        // Transicion sin destello negro: fade in de MainActivity sin fundido a negro de salida
        overridePendingTransition(android.R.anim.fade_in, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        splashHandler.removeCallbacksAndMessages(null);
    }
}
