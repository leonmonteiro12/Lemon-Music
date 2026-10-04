package com.lemon.music;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class YouTubeLoginActivity
        extends AppCompatActivity {

    private YouTubeAuthManager youtubeAuthManager;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_youtube_login
        );

        youtubeAuthManager =
                new YouTubeAuthManager(this);

        Button loginButton =
                findViewById(
                        R.id.youtubeLoginButton
                );

        if (loginButton != null) {

            loginButton.setOnClickListener(
                    v -> {

                        LemonDebug.log(
                                "YouTubeLogin",
                                "Google sign-in button pressed"
                        );

                        youtubeAuthManager.authorize();
                    }
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (youtubeAuthManager != null) {

            youtubeAuthManager.handleActivityResult(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }
}