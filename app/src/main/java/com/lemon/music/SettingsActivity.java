package com.lemon.music;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String SETTINGS_PREFS =
            "lemon_music_settings";


    private YouTubeAuthManager youtubeAuthManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_settings
        );

        youtubeAuthManager =
                new YouTubeAuthManager(this);

        
        setupYouTubeButtons();
        setupLoggerButton();
        setupAboutButton();
    }

    // =========================================================
    // API KEY
    // =========================================================

    
    // =========================================================
    // YOUTUBE
    // =========================================================

    private void setupYouTubeButtons() {

        Button connectButton =
                findViewById(
                        R.id.connectYouTubeButton
                );

        if (connectButton != null) {

            connectButton.setOnClickListener(
                    v -> youtubeAuthManager.authorize()
            );
        }

        Button signOutButton =
                findViewById(
                        R.id.signOutYouTubeButton
                );

        if (signOutButton != null) {

            signOutButton.setOnClickListener(
                    v -> showSignOutDialog()
            );
        }
    }

    private void showSignOutDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Sign out of YouTube?"
                )
                .setMessage(
                        "This will disconnect your YouTube account from Lemon Music."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Sign Out",
                        (dialog, which) -> {

                            youtubeAuthManager.signOut(
                                    () -> {

                                        Toast.makeText(
                                                SettingsActivity.this,
                                                "YouTube account signed out",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        LemonDebug.log(
                                                "Settings",
                                                "YouTube account signed out"
                                        );
                                    }
                            );
                        }
                )
                .show();
    }

    // =========================================================
    // LOGGER
    // =========================================================

    private void setupLoggerButton() {

        Button logButton =
                findViewById(
                        R.id.settingsLogButton
                );

        if (logButton != null) {

            logButton.setOnClickListener(
                    v -> showLogger()
            );
        }
    }

    private void showLogger() {

        android.widget.TextView logView =
                new android.widget.TextView(this);

        logView.setText(
                LemonDebug.getLogsText()
        );

        logView.setTextColor(
                android.graphics.Color.GREEN
        );

        logView.setTextSize(11);

        logView.setTypeface(
                android.graphics.Typeface.MONOSPACE
        );

        logView.setPadding(
                20,
                20,
                20,
                20
        );

        android.widget.ScrollView scroll =
                new android.widget.ScrollView(this);

        scroll.addView(logView);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "🍋 Lemon Music Live Logger"
                        )
                        .setView(scroll)
                        .setNegativeButton(
                                "CLOSE",
                                null
                        )
                        .setNeutralButton(
                                "CLEAR",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button clearButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_NEUTRAL
                            );

                    if (clearButton != null) {

                        clearButton.setOnClickListener(
                                v -> {

                                    LemonDebug.clear();

                                    logView.setText(
                                            LemonDebug.getLogsText()
                                    );
                                }
                        );
                    }
                }
        );

        dialog.show();
    }

    // =========================================================
    // ABOUT
    // =========================================================

    private void setupAboutButton() {

        Button aboutButton =
                findViewById(
                        R.id.aboutButton
                );

        if (aboutButton != null) {

            aboutButton.setOnClickListener(
                    v -> {

                        Intent intent =
                                new Intent(
                                        SettingsActivity.this,
                                        aboutActivity.class
                                );

                        startActivity(intent);
                    }
            );
        }
    }

    // =========================================================
    // AUTO AUTH REFRESH
    // =========================================================

    @Override
    protected void onStart() {

        super.onStart();

        if (youtubeAuthManager != null) {

            youtubeAuthManager.refreshAuthorization();
        }
    }

    // =========================================================
    // AUTH RESULT
    // =========================================================

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