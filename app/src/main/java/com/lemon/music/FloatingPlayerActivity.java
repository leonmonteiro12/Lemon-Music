package com.lemon.music;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FloatingPlayerActivity extends AppCompatActivity {

    private static final int OVERLAY_REQUEST = 7001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /*
         * This Activity is only the launcher for the
         * Android 6+ floating player.
         *
         * The actual floating window is handled by
         * FloatingPlayerService.
         */

        checkOverlayPermission();
    }

    // ==================================================
    // CHECK OVERLAY PERMISSION
    // ==================================================

    private void checkOverlayPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            if (!Settings.canDrawOverlays(this)) {

                Toast.makeText(
                        this,
                        "Enable floating windows for Lemon Music 🍋",
                        Toast.LENGTH_LONG
                ).show();

                try {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse(
                                            "package:" +
                                                    getPackageName()
                                    )
                            );

                    startActivityForResult(
                            intent,
                            OVERLAY_REQUEST
                    );

                } catch (Exception e) {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            );

                    startActivityForResult(
                            intent,
                            OVERLAY_REQUEST
                    );
                }

                return;
            }
        }

        startFloatingPlayer();
    }

    // ==================================================
    // START FLOATING PLAYER
    // ==================================================

    private void startFloatingPlayer() {

        try {

            Intent intent =
                    new Intent(
                            this,
                            FloatingPlayerService.class
                    );

            /*
             * startForegroundService() is required on
             * Android 8+.
             *
             * Android 6/7 use normal startService().
             */

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Toast.makeText(
                    this,
                    "Floating player started 🍋",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Couldn't start floating player.",
                    Toast.LENGTH_LONG
            ).show();
        }

        finish();
    }

    // ==================================================
    // RETURN FROM SETTINGS
    // ==================================================

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

        if (requestCode ==
                OVERLAY_REQUEST) {

            if (Build.VERSION.SDK_INT <
                    Build.VERSION_CODES.M ||
                    Settings.canDrawOverlays(this)) {

                startFloatingPlayer();

            } else {

                Toast.makeText(
                        this,
                        "Floating windows are still disabled.",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        }
    }
}