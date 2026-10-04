package com.lemon.music;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsActivity extends AppCompatActivity {

    private TextView lyricsTitle;
    private TextView lyricsArtist;

    private LinearLayout lyricsContainer;
    private ScrollView lyricsScroll;

    private final Handler handler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final List<LrcLine> lrcLines =
            new ArrayList<>();

    private int currentLine = -1;

    private boolean syncedMode = false;

    private final Runnable syncRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (isFinishing()) {
                        return;
                    }

                    if (syncedMode) {
                        updateSyncedLyrics();
                    }

                    handler.postDelayed(
                            this,
                            100
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
                R.layout.activity_lyrics
        );

        ImageButton backButton =
                findViewById(
                        R.id.lyricsBackButton
                );

        lyricsTitle =
                findViewById(
                        R.id.lyricsSongTitle
                );

        lyricsArtist =
                findViewById(
                        R.id.lyricsSongArtist
                );

        lyricsContainer =
                findViewById(
                        R.id.lyricsContainer
                );

        lyricsScroll =
                findViewById(
                        R.id.lyricsScroll
                );

        backButton.setOnClickListener(
                v -> finish()
        );

        String title =
                getIntent().getStringExtra(
                        "song_title"
                );

        String artist =
                getIntent().getStringExtra(
                        "song_artist"
                );

        if (
                title == null
                        ||
                title.trim().isEmpty()
        ) {

            title = "Unknown song";
        }

        if (
                artist == null
                        ||
                artist.trim().isEmpty()
        ) {

            artist = "Unknown artist";
        }

        lyricsTitle.setText(
                title
        );

        lyricsArtist.setText(
                artist
        );

        showLoading();

        final String finalTitle =
                title;

        final String finalArtist =
                artist;

        LyricsApi.search(
                finalTitle,
                finalArtist,
                new LyricsApi.Callback() {

                    @Override
                    public void onSuccess(
                            String plainLyrics,
                            String syncedLyrics) {

                        if (
                                syncedLyrics != null
                                        &&
                                !syncedLyrics
                                        .trim()
                                        .isEmpty()
                        ) {

                            loadSyncedLyrics(
                                    syncedLyrics
                            );

                        } else if (
                                plainLyrics != null
                                        &&
                                !plainLyrics
                                        .trim()
                                        .isEmpty()
                        ) {

                            loadPlainLyrics(
                                    plainLyrics
                            );

                        } else {

                            showError(
                                    "No lyrics available for this song."
                            );
                        }
                    }

                    @Override
                    public void onError(
                            String message) {

                        showError(
                                message
                        );
                    }
                }
        );

        handler.post(
                syncRunnable
        );
    }

    // =========================================================
    // LOADING
    // =========================================================

    private void showLoading() {

        lyricsContainer.removeAllViews();

        TextView text =
                createLyricsText(
                        "🔎 Finding lyrics...\n\n"
                                + "🍋 Lemon Music is searching..."
                );

        lyricsContainer.addView(
                text
        );
    }

    // =========================================================
    // SYNCED LYRICS
    // =========================================================

    private void loadSyncedLyrics(
            String syncedLyrics) {

        lrcLines.clear();

        String[] lines =
                syncedLyrics.split(
                        "\\r?\\n"
                );

        Pattern pattern =
                Pattern.compile(
                        "\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\](.*)"
                );

        for (String line : lines) {

            Matcher matcher =
                    pattern.matcher(
                            line
                    );

            if (!matcher.matches()) {
                continue;
            }

            try {

                int minutes =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                int seconds =
                        Integer.parseInt(
                                matcher.group(2)
                        );

                String fraction =
                        matcher.group(3);

                int milliseconds = 0;

                if (
                        fraction != null
                                &&
                        !fraction.isEmpty()
                ) {

                    if (fraction.length() == 1) {

                        milliseconds =
                                Integer.parseInt(
                                        fraction
                                ) * 100;

                    } else if (
                            fraction.length() == 2
                    ) {

                        milliseconds =
                                Integer.parseInt(
                                        fraction
                                ) * 10;

                    } else {

                        milliseconds =
                                Integer.parseInt(
                                        fraction.substring(
                                                0,
                                                3
                                        )
                                );
                    }
                }

                long timestamp =
                        minutes * 60_000L
                                +
                                seconds * 1_000L
                                +
                                milliseconds;

                String text =
                        matcher.group(4);

                if (text == null) {
                    text = "";
                }

                lrcLines.add(
                        new LrcLine(
                                timestamp,
                                text.trim()
                        )
                );

            } catch (Exception ignored) {
            }
        }

        if (lrcLines.isEmpty()) {

            showError(
                    "Synced lyrics could not be read."
            );

            return;
        }

        syncedMode = true;

        currentLine = -1;

        lyricsContainer.removeAllViews();

        for (int i = 0;
             i < lrcLines.size();
             i++) {

            TextView line =
                    createLyricsLine(
                            lrcLines.get(i).text
                    );

            line.setTag(
                    i
            );

            lyricsContainer.addView(
                    line
            );
        }

        updateSyncedLyrics();
    }

    // =========================================================
    // SYNC
    // =========================================================

    private void updateSyncedLyrics() {

        if (lrcLines.isEmpty()) {
            return;
        }

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {
            return;
        }

        long position =
                service.getPosition();

        int activeIndex = -1;

        for (int i = 0;
             i < lrcLines.size();
             i++) {

            if (
                    position >=
                            lrcLines.get(i).timestamp
            ) {

                activeIndex = i;

            } else {

                break;
            }
        }

        if (activeIndex < 0) {
            return;
        }

        if (activeIndex == currentLine) {
            return;
        }

        currentLine =
                activeIndex;

        for (int i = 0;
             i < lyricsContainer
                     .getChildCount();
             i++) {

            TextView line =
                    (TextView)
                            lyricsContainer
                                    .getChildAt(i);

            if (i == currentLine) {

                line.setTextColor(
                        Color.WHITE
                );

                line.setTextSize(
                        23
                );

                line.setAlpha(
                        1.0f
                );

            } else {

                line.setTextColor(
                        Color.rgb(
                                120,
                                120,
                                120
                        )
                );

                line.setTextSize(
                        19
                );

                line.setAlpha(
                        0.75f
                );
            }
        }

        scrollToCurrentLine();
    }

    // =========================================================
    // AUTO SCROLL
    // =========================================================

    private void scrollToCurrentLine() {

        if (
                currentLine < 0
                        ||
                currentLine >=
                        lyricsContainer
                                .getChildCount()
        ) {
            return;
        }

        TextView line =
                (TextView)
                        lyricsContainer
                                .getChildAt(
                                        currentLine
                                );

        lyricsScroll.post(
                () -> {

                    int targetY =
                            line.getTop()
                                    -
                                    lyricsScroll
                                            .getHeight()
                                    / 2
                                    +
                                    line.getHeight()
                                    / 2;

                    if (targetY < 0) {
                        targetY = 0;
                    }

                    lyricsScroll.smoothScrollTo(
                            0,
                            targetY
                    );
                }
        );
    }

    // =========================================================
    // PLAIN LYRICS FALLBACK
    // =========================================================

    private void loadPlainLyrics(
            String plainLyrics) {

        syncedMode = false;

        lrcLines.clear();

        lyricsContainer.removeAllViews();

        String[] lines =
                plainLyrics.split(
                        "\\r?\\n"
                );

        for (String line : lines) {

            TextView text =
                    createLyricsLine(
                            line
                    );

            text.setTextColor(
                    Color.WHITE
            );

            lyricsContainer.addView(
                    text
            );
        }
    }

    // =========================================================
    // ERROR
    // =========================================================

    private void showError(
            String message) {

        syncedMode = false;

        lrcLines.clear();

        lyricsContainer.removeAllViews();

        TextView text =
                createLyricsText(
                        "🎤 Lyrics unavailable\n\n"
                                + message
                );

        lyricsContainer.addView(
                text
        );
    }

    // =========================================================
    // TEXT CREATION
    // =========================================================

    private TextView createLyricsLine(
            String text) {

        TextView view =
                new TextView(
                        this
                );

        view.setText(
                text == null
                        ? ""
                        : text
        );

        view.setTextColor(
                Color.rgb(
                        120,
                        120,
                        120
                )
        );

        view.setTextSize(
                19
        );

        view.setGravity(
                Gravity.CENTER
        );

        view.setPadding(
                12,
                16,
                12,
                16
        );

        view.setLineSpacing(
                0,
                1.1f
        );

        view.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        return view;
    }

    private TextView createLyricsText(
            String text) {

        TextView view =
                createLyricsLine(
                        text
                );

        view.setTextSize(
                18
        );

        view.setTextColor(
                Color.WHITE
        );

        return view;
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                syncRunnable
        );

        super.onDestroy();
    }

    // =========================================================
    // LRC LINE
    // =========================================================

    private static class LrcLine {

        final long timestamp;

        final String text;

        LrcLine(
                long timestamp,
                String text) {

            this.timestamp =
                    timestamp;

            this.text =
                    text;
        }
    }
}