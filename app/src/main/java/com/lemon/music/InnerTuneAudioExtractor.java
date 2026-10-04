package com.lemon.music;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.zionhuang.innertube.NewPipeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InnerTuneAudioExtractor {

    private InnerTuneAudioExtractor() {
    }

    public interface Callback {

        void onSuccess(
                String audioUrl,
                String extractedTitle,
                String extractedArtist
        );

        void onError(
                String message
        );
    }

    private static final ExecutorService EXTRACTION_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(
                    Looper.getMainLooper()
            );

    public static void extract(
            String videoId,
            Callback callback
    ) {

        if (callback == null) {

            LemonDebug.log(
                    "LemonMusicExtractor",
                    "Extraction rejected: callback == null"
            );

            return;
        }

        if (videoId == null ||
                videoId.trim().isEmpty()) {

            LemonDebug.log(
                    "LemonMusicExtractor",
                    "Extraction rejected: empty video ID"
            );

            callback.onError(
                    "Video ID is empty."
            );

            return;
        }

        final String cleanVideoId =
                videoId.trim();

        LemonDebug.log(
                "LemonMusicExtractor",
                "================================"
        );

        LemonDebug.log(
                "LemonMusicExtractor",
                "EXTRACTION REQUEST"
        );

        LemonDebug.log(
                "LemonMusicExtractor",
                "Video ID = "
                        + cleanVideoId
        );

        EXTRACTION_EXECUTOR.execute(
                new Runnable() {

                    @Override
                    public void run() {

                        long startedAt =
                                System.currentTimeMillis();

                        LemonDebug.log(
                                "LemonMusicExtractor",
                                "Background extraction START"
                        );

                        LemonDebug.log(
                                "LemonMusicExtractor",
                                "Thread = "
                                        + Thread.currentThread()
                                        .getName()
                        );

                        try {

                            String audioUrl =
                                    NewPipeUtils.extractAudioUrl(
                                            cleanVideoId
                                    );

                            long elapsed =
                                    System.currentTimeMillis()
                                            - startedAt;

                            if (audioUrl == null ||
                                    audioUrl.trim().isEmpty()) {

                                LemonDebug.log(
                                        "LemonMusicExtractor",
                                        "EXTRACTION FAILED: empty audio URL"
                                );

                                LemonDebug.log(
                                        "LemonMusicExtractor",
                                        "Elapsed = "
                                                + elapsed
                                                + " ms"
                                );

                                MAIN_HANDLER.post(
                                        new Runnable() {

                                            @Override
                                            public void run() {

                                                LemonDebug.log(
                                                        "LemonMusicExtractor",
                                                        "Posting extraction ERROR to main thread"
                                                );

                                                callback.onError(
                                                        "NewPipeExtractor returned an empty audio URL."
                                                );
                                            }
                                        }
                                );

                                return;
                            }

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "EXTRACTION SUCCESS"
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Video ID = "
                                            + cleanVideoId
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Audio URL length = "
                                            + audioUrl.length()
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Audio URL host = "
                                            + getSafeHost(
                                            audioUrl
                                    )
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Elapsed = "
                                            + elapsed
                                            + " ms"
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Posting SUCCESS to main thread"
                            );

                            MAIN_HANDLER.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            LemonDebug.log(
                                                    "LemonMusicExtractor",
                                                    "SUCCESS callback delivered"
                                            );

                                            callback.onSuccess(
                                                    audioUrl,
                                                    null,
                                                    null
                                            );
                                        }
                                    }
                            );

                        } catch (Exception e) {

                            long elapsed =
                                    System.currentTimeMillis()
                                            - startedAt;

                            String realMessage =
                                    e.getMessage() == null ||
                                            e.getMessage()
                                                    .trim()
                                                    .isEmpty()
                                            ? "NewPipe extraction failed."
                                            : e.getMessage()
                                                    .trim();

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "================================"
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "EXTRACTION EXCEPTION"
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Video ID = "
                                            + cleanVideoId
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Elapsed = "
                                            + elapsed
                                            + " ms"
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Exception = "
                                            + e.getClass()
                                            .getName()
                            );

                            LemonDebug.log(
                                    "LemonMusicExtractor",
                                    "Message = "
                                            + realMessage
                            );

                            LemonDebug.error(
                                    "LemonMusicExtractor",
                                    "Full extraction exception",
                                    e
                            );

                            MAIN_HANDLER.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            LemonDebug.log(
                                                    "LemonMusicExtractor",
                                                    "ERROR callback delivered"
                                            );

                                            callback.onError(
                                                    realMessage
                                            );
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private static String getSafeHost(
            String url) {

        try {

            Uri uri =
                    Uri.parse(
                            url
                    );

            String host =
                    uri.getHost();

            if (host == null ||
                    host.trim().isEmpty()) {

                return "unknown";
            }

            return host;

        } catch (Exception e) {

            return "invalid-url";
        }
    }
}