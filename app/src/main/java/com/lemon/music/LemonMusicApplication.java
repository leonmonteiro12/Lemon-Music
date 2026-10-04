package com.lemon.music;

import android.app.Application;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LemonMusicApplication extends Application {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> {

                    try {

                        File folder =
                                getExternalFilesDir(
                                        Environment.DIRECTORY_DOCUMENTS
                                );

                        if (folder != null) {

                            if (!folder.exists()) {
                                folder.mkdirs();
                            }

                            File crashFile =
                                    new File(
                                            folder,
                                            "lemon_music_crash.txt"
                                    );

                            FileWriter writer =
                                    new FileWriter(
                                            crashFile,
                                            false
                                    );

                            PrintWriter out =
                                    new PrintWriter(writer);

                            out.println(
                                    "=== LEMON MUSIC CRASH REPORT ==="
                            );

                            out.println(
                                    "Time: " +
                                    new SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss",
                                            Locale.getDefault()
                                    ).format(new Date())
                            );

                            out.println(
                                    "Android: " +
                                    Build.VERSION.RELEASE
                            );

                            out.println(
                                    "SDK: " +
                                    Build.VERSION.SDK_INT
                            );

                            out.println(
                                    "Device: " +
                                    Build.MANUFACTURER +
                                    " " +
                                    Build.MODEL
                            );

                            out.println();

                            out.println(
                                    "=== EXCEPTION ==="
                            );

                            throwable.printStackTrace(out);

                            out.println();

                            out.println(
                                    "=== CAUSE ==="
                            );

                            Throwable cause =
                                    throwable.getCause();

                            while (cause != null) {

                                cause.printStackTrace(out);

                                cause =
                                        cause.getCause();
                            }

                            out.close();
                            writer.close();
                        }

                    } catch (Exception ignored) {
                        // Never crash while saving the crash report.
                    }

                    if (defaultHandler != null) {

                        defaultHandler.uncaughtException(
                                thread,
                                throwable
                        );
                    }
                }
        );
    }
}