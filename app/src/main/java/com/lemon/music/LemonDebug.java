package com.lemon.music;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LemonDebug {

    private static final String TAG =
            "LEMON_MUSIC";

    private static final List<String> logs =
            new ArrayList<>();

    private LemonDebug() {
    }

    public static synchronized void log(
            String section,
            String message) {

        String line =
                timestamp()
                        + " ["
                        + section
                        + "] "
                        + message;

        Log.d(
                TAG,
                line
        );

        logs.add(line);

        limitLogs();
    }

    public static synchronized void error(
            String section,
            String message,
            Throwable throwable) {

        String line =
                timestamp()
                        + " [ERROR/"
                        + section
                        + "] "
                        + message;

        Log.e(
                TAG,
                line,
                throwable
        );

        logs.add(line);

        if (throwable != null) {

            logs.add(
                    Log.getStackTraceString(
                            throwable
                    )
            );
        }

        limitLogs();
    }

    public static synchronized List<String>
    getLogs() {

        return new ArrayList<>(
                logs
        );
    }

    public static synchronized String
    getLogsText() {

        StringBuilder builder =
                new StringBuilder();

        for (String line : logs) {

            builder
                    .append(line)
                    .append("\n");
        }

        return builder.toString();
    }

    public static synchronized void clear() {

        logs.clear();

        Log.d(
                TAG,
                "Logs cleared"
        );
    }

    private static void limitLogs() {

        while (logs.size() > 500) {

            logs.remove(0);
        }
    }

    private static String timestamp() {

        return new SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.US
        ).format(
                new Date()
        );
    }
}