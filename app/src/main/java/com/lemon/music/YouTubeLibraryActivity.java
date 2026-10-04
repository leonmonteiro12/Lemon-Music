package com.lemon.music;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class YouTubeLibraryActivity
        extends AppCompatActivity {

    private LinearLayout deviceAudioFolder;
    private LinearLayout youtubeMusicFolder;


    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_youtube_library
        );


        // ==================================================
        // BACK
        // ==================================================

        ImageButton backButton =
                findViewById(
                        R.id.libraryBackButton
                );


        if (backButton != null) {

            backButton.setOnClickListener(
                    v -> finish()
            );
        }


        // ==================================================
        // DEVICE AUDIO
        // ==================================================

        deviceAudioFolder =
                findViewById(
                        R.id.deviceAudioFolder
                );


        if (deviceAudioFolder != null) {

            deviceAudioFolder.setOnClickListener(
                    v -> openDeviceAudio()
            );
        }


        // ==================================================
        // YOUTUBE MUSIC
        // ==================================================

        youtubeMusicFolder =
                findViewById(
                        R.id.youtubeMusicFolder
                );


        if (youtubeMusicFolder != null) {

            youtubeMusicFolder.setOnClickListener(
                    v -> openYouTubeMusicLibrary()
            );
        }
    }


    private void openDeviceAudio() {

        Intent intent =
                new Intent(
                        YouTubeLibraryActivity.this,
                        DeviceAudioActivity.class
                );

        startActivity(intent);
    }


    private void openYouTubeMusicLibrary() {

        Intent intent =
                new Intent(
                        YouTubeLibraryActivity.this,
                        YouTubePlaylistsActivity.class
                );

        startActivity(intent);
    }
}