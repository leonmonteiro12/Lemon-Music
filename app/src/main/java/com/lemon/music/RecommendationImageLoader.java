package com.lemon.music;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RecommendationImageLoader {

    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(3);

    private RecommendationImageLoader() {
    }

    public static void load(final String imageUrl, final ImageView imageView) {

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        final String requestedUrl = imageUrl.trim();

        imageView.setTag(requestedUrl);

        EXECUTOR.execute(() -> {

            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                URL url = new URL(requestedUrl);

                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setUseCaches(true);
                connection.setDoInput(true);
                connection.setRequestMethod("GET");

                connection.connect();

                if (connection.getResponseCode() >= 200
                        && connection.getResponseCode() < 300) {

                    inputStream = connection.getInputStream();

                    BitmapFactory.Options options =
                            new BitmapFactory.Options();

                    options.inPreferredConfig =
                            Bitmap.Config.RGB_565;

                    options.inSampleSize = 2;

                    bitmap = BitmapFactory.decodeStream(
                            inputStream,
                            null,
                            options
                    );
                }

            } catch (Exception e) {

                LemonDebug.error(
                        "RECOMMENDATIONS",
                        "Thumbnail load failed: " + requestedUrl,
                        e
                );

            } finally {

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }

            final Bitmap result = bitmap;

            imageView.post(() -> {

                Object tag = imageView.getTag();

                if (result != null
                        && tag instanceof String
                        && requestedUrl.equals(tag)) {

                    imageView.setImageBitmap(result);
                }
            });
        });
    }
}