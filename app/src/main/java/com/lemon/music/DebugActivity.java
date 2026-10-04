package com.lemon.music;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

public class DebugActivity extends Activity {

    private TextView logText;

    private final Handler handler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final Runnable refresh =
            new Runnable() {

                @Override
                public void run() {

                    refreshLogs();

                    handler.postDelayed(
                            this,
                            500
                    );
                }
            };

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_debug
        );

        logText =
                findViewById(
                        R.id.logText
                );

        Button clear =
                findViewById(
                        R.id.buttonClearLogs
                );

        Button close =
                findViewById(
                        R.id.buttonCloseLogs
                );

        clear.setOnClickListener(
                v -> {

                    LemonDebug.clear();

                    refreshLogs();
                }
        );

        close.setOnClickListener(
                v -> finish()
        );

        refreshLogs();
    }

    private void refreshLogs() {

        if (logText != null) {

            logText.setText(
                    LemonDebug.getLogsText()
            );
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        handler.post(
                refresh
        );
    }

    @Override
    protected void onPause() {

        handler.removeCallbacks(
                refresh
        );

        super.onPause();
    }
}