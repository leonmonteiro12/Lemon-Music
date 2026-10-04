package com.lemon.music;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LyricsApi {

    public interface Callback {
        void onSuccess(String plainLyrics, String syncedLyrics);
        void onError(String message);
    }

    public static void search(
            String title,
            String artist,
            Callback callback) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String query =
                        "https://lrclib.net/api/get?track_name="
                                + URLEncoder.encode(
                                title,
                                "UTF-8"
                        )
                                + "&artist_name="
                                + URLEncoder.encode(
                                artist,
                                "UTF-8"
                        );

                URL url =
                        new URL(query);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        10000
                );

                connection.setReadTimeout(
                        10000
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != 200) {

                    postError(
                            callback,
                            "Lyrics not found."
                    );

                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        "UTF-8"
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                String plainLyrics =
                        json.optString(
                                "plainLyrics",
                                ""
                        );

                String syncedLyrics =
                        json.optString(
                                "syncedLyrics",
                                ""
                        );

                if (
                        plainLyrics.trim().isEmpty()
                                &&
                        syncedLyrics.trim().isEmpty()
                ) {

                    postError(
                            callback,
                            "No lyrics available for this song."
                    );

                    return;
                }

                new Handler(
                        Looper.getMainLooper()
                ).post(
                        () -> callback.onSuccess(
                                plainLyrics,
                                syncedLyrics
                        )
                );

            } catch (Exception e) {

                postError(
                        callback,
                        "Couldn't load lyrics."
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private static void postError(
            Callback callback,
            String message) {

        new Handler(
                Looper.getMainLooper()
        ).post(
                () -> callback.onError(
                        message
                )
        );
    }
}