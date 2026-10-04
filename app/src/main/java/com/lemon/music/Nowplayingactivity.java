package com.lemon.music;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class Nowplayingactivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageButton playPauseButton;
    private ImageButton skipBackButton;
    private ImageButton skipForwardButton;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;

    private SeekBar progressBar;

    private TextView songTitle;
    private TextView songArtist;
    private TextView currentTime;
    private TextView duration;

    private Button lyricsButton;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean repeatEnabled = false;
    private boolean shuffleEnabled = false;
    private boolean userIsSeeking = false;

    private String songUri = "";

    private String currentSongTitle =
            "Nothing playing";

    private String currentSongArtist =
            "Unknown artist";

    /*
     * TRUE when the currently active player is
     * LemonPlaybackService (YouTube playback).
     */
    private boolean usingLemonPlaybackService = false;

    private final Runnable progressUpdater =
            new Runnable() {

                @Override
                public void run() {

                    updateNowPlaying();

                    handler.postDelayed(
                            this,
                            500
                    );
                }
            };

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_now_playing
        );

        // =====================================================
        // VIEWS
        // =====================================================

        backButton =
                findViewById(
                        R.id.backButton
                );

        playPauseButton =
                findViewById(
                        R.id.playPauseButton
                );

        skipBackButton =
                findViewById(
                        R.id.skipBackButton
                );

        skipForwardButton =
                findViewById(
                        R.id.skipForwardButton
                );

        repeatButton =
                findViewById(
                        R.id.repeatButton
                );

        shuffleButton =
                findViewById(
                        R.id.shuffleButton
                );

        progressBar =
                findViewById(
                        R.id.progressBar
                );

        songTitle =
                findViewById(
                        R.id.songTitle
                );

        songArtist =
                findViewById(
                        R.id.songArtist
                );

        currentTime =
                findViewById(
                        R.id.currentTime
                );

        duration =
                findViewById(
                        R.id.duration
                );

        lyricsButton =
                findViewById(
                        R.id.lyricsButton
                );

        // =====================================================
        // READ INTENT
        // =====================================================

        readSongInformation();

        /*
         * IMPORTANT:
         *
         * Check LemonPlaybackService FIRST.
         *
         * This is the player that handles YouTube playback.
         */
        syncWithLemonPlaybackService();

        updateNowPlaying();

        // =====================================================
        // BACK
        // =====================================================

        backButton.setOnClickListener(
                v -> finish()
        );

        // =====================================================
        // LYRICS
        // =====================================================

        lyricsButton.setOnClickListener(
                v -> {

                    /*
                     * Refresh the actual player state before
                     * opening lyrics.
                     */
                    syncWithLemonPlaybackService();

                    Intent intent =
                            new Intent(
                                    Nowplayingactivity.this,
                                    LyricsActivity.class
                            );

                    intent.putExtra(
                            "song_title",
                            currentSongTitle
                    );

                    intent.putExtra(
                            "song_artist",
                            currentSongArtist
                    );

                    startActivity(intent);
                }
        );

        // =====================================================
        // PLAY / PAUSE
        // =====================================================

        playPauseButton.setOnClickListener(
                v -> togglePlayback()
        );

        // =====================================================
        // REWIND
        // =====================================================

        skipBackButton.setOnClickListener(
                v -> {

                    if (usingLemonPlaybackService) {

                        sendLemonAction(
                                LemonPlaybackService.ACTION_REWIND
                        );

                    } else {

                        int position =
                                MusicPlaybackService
                                        .getCurrentPosition();

                        MusicPlaybackService.seekTo(
                                Math.max(
                                        0,
                                        position - 10000
                                )
                        );
                    }

                    updateNowPlaying();
                }
        );

        // =====================================================
        // FORWARD
        // =====================================================

        skipForwardButton.setOnClickListener(
                v -> {

                    if (usingLemonPlaybackService) {

                        sendLemonAction(
                                LemonPlaybackService.ACTION_FORWARD
                        );

                    } else {

                        int position =
                                MusicPlaybackService
                                        .getCurrentPosition();

                        int length =
                                MusicPlaybackService
                                        .getDuration();

                        if (length <= 0) {
                            return;
                        }

                        MusicPlaybackService.seekTo(
                                Math.min(
                                        length,
                                        position + 10000
                                )
                        );
                    }

                    updateNowPlaying();
                }
        );

        // =====================================================
        // REPEAT
        // =====================================================

        repeatButton.setAlpha(0.45f);

        repeatButton.setOnClickListener(
                v -> {

                    repeatEnabled =
                            !repeatEnabled;

                    repeatButton.setAlpha(
                            repeatEnabled
                                    ? 1.0f
                                    : 0.45f
                    );

                    Toast.makeText(
                            this,
                            repeatEnabled
                                    ? "Repeat enabled 🔁"
                                    : "Repeat disabled",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        // =====================================================
        // SHUFFLE
        // =====================================================

        shuffleButton.setAlpha(0.45f);

        shuffleButton.setOnClickListener(
                v -> {

                    shuffleEnabled =
                            !shuffleEnabled;

                    shuffleButton.setAlpha(
                            shuffleEnabled
                                    ? 1.0f
                                    : 0.45f
                    );

                    Toast.makeText(
                            this,
                            shuffleEnabled
                                    ? "Shuffle enabled 🔀"
                                    : "Shuffle disabled",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        // =====================================================
        // SEEK BAR
        // =====================================================

        progressBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (fromUser) {

                            currentTime.setText(
                                    formatTime(progress)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {

                        userIsSeeking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {

                        userIsSeeking = false;

                        if (usingLemonPlaybackService) {

                            sendLemonSeek(
                                    seekBar.getProgress()
                            );

                        } else {

                            MusicPlaybackService.seekTo(
                                    seekBar.getProgress()
                            );
                        }

                        updateNowPlaying();
                    }
                }
        );

        // =====================================================
        // LOCAL AUDIO FALLBACK
        // =====================================================

        if (!usingLemonPlaybackService &&
                !songUri.isEmpty()) {

            startPlaybackIfNeeded();
        }

        // =====================================================
        // START UI LOOP
        // =====================================================

        updateNowPlaying();

        handler.removeCallbacks(
                progressUpdater
        );

        handler.post(
                progressUpdater
        );
    }

    // =========================================================
    // LEMON PLAYBACK SERVICE
    // =========================================================

    private boolean syncWithLemonPlaybackService() {

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {

            usingLemonPlaybackService = false;

            return false;
        }

        String videoId =
                service.getCurrentVideoId();

        String title =
                service.getCurrentTitle();

        String artist =
                service.getCurrentArtist();

        /*
         * A YouTube track has a video ID.
         *
         * Therefore this is the safest way to determine
         * whether LemonPlaybackService currently owns
         * a YouTube song.
         */
        boolean hasYouTubeTrack =
                videoId != null &&
                        !videoId.trim().isEmpty();

        if (!hasYouTubeTrack) {

            usingLemonPlaybackService = false;

            return false;
        }

        usingLemonPlaybackService = true;

        if (title != null &&
                !title.trim().isEmpty()) {

            currentSongTitle =
                    title.trim();
        }

        if (artist != null &&
                !artist.trim().isEmpty()) {

            currentSongArtist =
                    artist.trim();
        }

        if (songTitle != null) {

            songTitle.setText(
                    currentSongTitle
            );
        }

        if (songArtist != null) {

            songArtist.setText(
                    currentSongArtist
            );
        }

        return true;
    }

    // =========================================================
    // UPDATE EVERYTHING
    // =========================================================

    private void updateNowPlaying() {

        /*
         * Always check LemonPlaybackService first.
         *
         * This makes the screen immediately follow the
         * actual YouTube player.
         */
        syncWithLemonPlaybackService();

        if (usingLemonPlaybackService) {

            updateLemonPlayback();

        } else {

            updateLocalPlayback();
        }

        updatePlayButton();
    }

    // =========================================================
    // LEMON PLAYBACK PROGRESS
    // =========================================================

    private void updateLemonPlayback() {

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {
            return;
        }

        int position =
                service.getPosition();

        int length =
                service.getTrackDuration();

        if (songTitle != null) {

            songTitle.setText(
                    currentSongTitle
            );
        }

        if (songArtist != null) {

            songArtist.setText(
                    currentSongArtist
            );
        }

        if (userIsSeeking) {
            return;
        }

        if (length > 0) {

            progressBar.setMax(
                    length
            );

            progressBar.setProgress(
                    Math.min(
                            position,
                            length
                    )
            );

            currentTime.setText(
                    formatTime(position)
            );

            duration.setText(
                    formatTime(length)
            );

        } else {

            progressBar.setMax(0);
            progressBar.setProgress(0);

            currentTime.setText(
                    formatTime(position)
            );

            duration.setText(
                    "0:00"
            );
        }
    }

    // =========================================================
    // LOCAL PLAYBACK PROGRESS
    // =========================================================

    private void updateLocalPlayback() {

        if (userIsSeeking) {
            return;
        }

        int position =
                MusicPlaybackService
                        .getCurrentPosition();

        int length =
                MusicPlaybackService
                        .getDuration();

        if (length > 0) {

            progressBar.setMax(
                    length
            );

            progressBar.setProgress(
                    Math.min(
                            position,
                            length
                    )
            );

            currentTime.setText(
                    formatTime(position)
            );

            duration.setText(
                    formatTime(length)
            );

        } else {

            progressBar.setMax(0);
            progressBar.setProgress(0);

            currentTime.setText(
                    "0:00"
            );

            duration.setText(
                    "0:00"
            );
        }
    }

    // =========================================================
    // PLAY / PAUSE
    // =========================================================

    private void togglePlayback() {

        syncWithLemonPlaybackService();

        if (usingLemonPlaybackService) {

            LemonPlaybackService service =
                    LemonPlaybackService.getInstance();

            if (service == null) {
                return;
            }

            if (service.isPlaying()) {

                sendLemonAction(
                        LemonPlaybackService.ACTION_PAUSE
                );

            } else {

                sendLemonAction(
                        LemonPlaybackService.ACTION_PLAY
                );

            }

        } else {

            if (MusicPlaybackService.isPlaying()) {

                sendLocalServiceAction(
                        MusicPlaybackService.ACTION_PAUSE
                );

            } else {

                if (!songUri.isEmpty() &&
                        MusicPlaybackService.getDuration() <= 0) {

                    startPlayback();

                } else {

                    sendLocalServiceAction(
                            MusicPlaybackService.ACTION_RESUME
                    );
                }
            }
        }

        handler.postDelayed(
                this::updateNowPlaying,
                250
        );
    }

    // =========================================================
    // LEMON ACTION
    // =========================================================

    private void sendLemonAction(
            String action) {

        Intent intent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        intent.setAction(
                action
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(
                        intent
                );

            } else {

                startService(intent);
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Playback control failed.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // LEMON SEEK
    // =========================================================

    private void sendLemonSeek(
            int position) {

        Intent intent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        intent.setAction(
                LemonPlaybackService.ACTION_SEEK
        );

        intent.putExtra(
                LemonPlaybackService.EXTRA_POSITION,
                position
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(
                        intent
                );

            } else {

                startService(intent);
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // LOCAL SERVICE ACTION
    // =========================================================

    private void sendLocalServiceAction(
            String action) {

        Intent intent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        intent.setAction(
                action
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                        this,
                        intent
                );

            } else {

                startService(intent);
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // LOCAL SEEK
    // =========================================================

    private void startPlaybackIfNeeded() {

        if (songUri.isEmpty()) {
            return;
        }

        if (MusicPlaybackService.isPlaying()) {
            return;
        }

        startPlayback();
    }

    private void startPlayback() {

        if (songUri.isEmpty()) {

            Toast.makeText(
                    this,
                    "No playable audio source.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        intent.setAction(
                MusicPlaybackService.ACTION_PLAY
        );

        intent.putExtra(
                MusicPlaybackService.EXTRA_SONG_URI,
                songUri
        );

        intent.putExtra(
                MusicPlaybackService.EXTRA_SONG_TITLE,
                currentSongTitle
        );

        intent.putExtra(
                MusicPlaybackService.EXTRA_SONG_ARTIST,
                currentSongArtist
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                        this,
                        intent
                );

            } else {

                startService(intent);
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to start Lemon Music playback.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // SONG INFORMATION
    // =========================================================

    private void readSongInformation() {

        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        String title =
                intent.getStringExtra(
                        "song_title"
                );

        String artist =
                intent.getStringExtra(
                        "song_artist"
                );

        String uri =
                intent.getStringExtra(
                        "song_uri"
                );

        if (title != null &&
                !title.trim().isEmpty()) {

            currentSongTitle =
                    title.trim();
        }

        if (artist != null &&
                !artist.trim().isEmpty()) {

            currentSongArtist =
                    artist.trim();
        }

        if (uri != null &&
                !uri.trim().isEmpty()) {

            songUri =
                    uri.trim();
        }
    }

    // =========================================================
    // PLAY BUTTON
    // =========================================================

    private void updatePlayButton() {

        boolean playing = false;

        if (usingLemonPlaybackService) {

            LemonPlaybackService service =
                    LemonPlaybackService.getInstance();

            if (service != null) {

                playing =
                        service.isPlaying();
            }

        } else {

            playing =
                    MusicPlaybackService.isPlaying();
        }

        if (playPauseButton != null) {

            playPauseButton.setImageResource(
                    playing
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play
            );
        }
    }

    // =========================================================
    // TIME
    // =========================================================

    private String formatTime(
            int milliseconds) {

        if (milliseconds < 0) {
            milliseconds = 0;
        }

        int totalSeconds =
                milliseconds / 1000;

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * Reconnect to LemonPlaybackService every time
         * this screen becomes visible.
         */
        syncWithLemonPlaybackService();

        updateNowPlaying();

        handler.removeCallbacks(
                progressUpdater
        );

        handler.post(
                progressUpdater
        );
    }

    // =========================================================
    // PAUSE
    // =========================================================

    @Override
    protected void onPause() {

        super.onPause();

        /*
         * NEVER stop or pause playback here.
         */
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(
                progressUpdater
        );

        super.onDestroy();
    }
}