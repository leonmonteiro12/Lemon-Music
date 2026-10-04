package com.lemon.music;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class YouTubeWebActivity extends Activity {

    private static final String TAG = "LemonMusicYouTube";

    private ImageButton buttonBack;
    private ImageButton buttonMore;
    private ImageButton buttonCenterPlay;
    private ImageButton buttonPrevious;
    private ImageButton buttonRewind;
    private ImageButton buttonPlayPause;
    private ImageButton buttonForward;
    private ImageButton buttonNext;

    private TextView buttonQueue;
    private TextView buttonPlaylist;
    private TextView buttonFloating;

    private TextView songTitle;
    private TextView songArtist;

    private TextView currentTime;
    private TextView totalTime;

    private SeekBar playerSeekBar;
    private ProgressBar loadingProgress;
    private Button lyricsButton;

    private String videoId = "";
    private String videoTitle = "YouTube Music";
    private String videoArtist = "YouTube";

    private boolean extractionStarted = false;
    private boolean serviceRequestSent = false;
    private boolean activityDestroyed = false;
    private boolean userSeeking = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable progressRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (activityDestroyed) {
                        return;
                    }

                    updatePlayerUI();

                    handler.postDelayed(this, 400);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        activityDestroyed = false;

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        Log.d(TAG, "================================================");
        Log.d(TAG, "PLAYER ACTIVITY CREATED");

        readIntent();

        Log.d(TAG, "Video ID after readIntent = " + videoId);
        Log.d(TAG, "Title = " + videoTitle);
        Log.d(TAG, "Artist = " + videoArtist);

        if (videoId == null || videoId.trim().isEmpty()) {

            Log.e(TAG, "Video ID EMPTY - cannot start playback");

            Toast.makeText(
                    this,
                    "Video could not be opened.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        videoId = videoId.trim();

        setContentView(R.layout.activity_youtube_web);

        initializeViews();
        setupButtons();
        updateSongInformation();

        /*
         * IMPORTANT:
         *
         * If the Home mini-player opened this Activity for the
         * song that is ALREADY playing in LemonPlaybackService,
         * do NOT extract and load the song again.
         *
         * This prevents the existing Media3 playback from being
         * replaced/restarted just because the player UI was opened.
         */
        boolean existingPlayback =
                syncWithPlaybackService();

        if (existingPlayback) {

            Log.d(
                    TAG,
                    "Existing matching playback found."
            );

            Log.d(
                    TAG,
                    "Attaching UI to current playback."
            );

            hideLoading();

        } else {

            Log.d(
                    TAG,
                    "No matching existing playback."
            );

            Log.d(
                    TAG,
                    "Starting InnerTune playback pipeline."
            );

            startInnerTunePlayback();
        }

        handler.post(progressRunnable);
    }

    // =========================================================
    // SYNC WITH CURRENT PLAYBACK
    // =========================================================

    private boolean syncWithPlaybackService() {

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {

            Log.d(
                    TAG,
                    "syncWithPlaybackService: service NULL"
            );

            return false;
        }

        String currentVideoId =
                service.getCurrentVideoId();

        if (currentVideoId == null ||
                currentVideoId.trim().isEmpty()) {

            Log.d(
                    TAG,
                    "syncWithPlaybackService: no current video"
            );

            return false;
        }

        currentVideoId =
                currentVideoId.trim();

        /*
         * VERY IMPORTANT:
         *
         * Do not blindly replace videoId with the service ID.
         *
         * We only attach to the service if the service is already
         * playing/loading the SAME video this Activity was opened for.
         */
        if (!currentVideoId.equals(videoId)) {

            Log.d(
                    TAG,
                    "Service is playing a DIFFERENT video."
            );

            Log.d(
                    TAG,
                    "Requested video = " + videoId
            );

            Log.d(
                    TAG,
                    "Service video   = " + currentVideoId
            );

            return false;
        }

        String currentTitle =
                service.getCurrentTitle();

        String currentArtist =
                service.getCurrentArtist();

        if (currentTitle != null &&
                !currentTitle.trim().isEmpty()) {

            videoTitle =
                    currentTitle.trim();
        }

        if (currentArtist != null &&
                !currentArtist.trim().isEmpty()) {

            videoArtist =
                    currentArtist.trim();
        }

        updateSongInformation();

        Log.d(
                TAG,
                "================================================"
        );

        Log.d(
                TAG,
                "MATCHING EXISTING PLAYBACK FOUND"
        );

        Log.d(
                TAG,
                "Video ID = " + currentVideoId
        );

        Log.d(
                TAG,
                "Playing = " + service.isPlaying()
        );

        Log.d(
                TAG,
                "Loading = " + service.isLoading()
        );

        Log.d(
                TAG,
                "Position = " + service.getPosition()
        );

        return true;
    }

    // =========================================================
    // VIEWS
    // =========================================================

    private void initializeViews() {

        buttonBack = findViewById(R.id.buttonBack);
        buttonMore = findViewById(R.id.buttonMore);

        buttonCenterPlay =
                findViewById(R.id.buttonCenterPlay);

        buttonPrevious =
                findViewById(R.id.buttonPrevious);

        buttonRewind =
                findViewById(R.id.buttonRewind);

        buttonPlayPause =
                findViewById(R.id.buttonPlayPause);

        buttonForward =
                findViewById(R.id.buttonForward);

        buttonNext =
                findViewById(R.id.buttonNext);

        buttonQueue =
                findViewById(R.id.buttonQueue);

        buttonPlaylist =
                findViewById(R.id.buttonPlaylist);

        buttonFloating =
                findViewById(R.id.buttonFloating);

        songTitle =
                findViewById(R.id.songTitle);

        songArtist =
                findViewById(R.id.songArtist);

        currentTime =
                findViewById(R.id.currentTime);

        totalTime =
                findViewById(R.id.totalTime);

        playerSeekBar =
                findViewById(R.id.playerSeekBar);

        loadingProgress =
                findViewById(R.id.loadingProgress);

        lyricsButton =
                findViewById(R.id.lyricsButton);
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private void setupButtons() {

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        if (buttonCenterPlay != null) {
            buttonCenterPlay.setOnClickListener(
                    v -> togglePlayback()
            );
        }

        if (buttonPlayPause != null) {
            buttonPlayPause.setOnClickListener(
                    v -> togglePlayback()
            );
        }

        if (buttonRewind != null) {
            buttonRewind.setOnClickListener(
                    v -> sendServiceAction(
                            LemonPlaybackService.ACTION_REWIND
                    )
            );
        }

        if (buttonForward != null) {
            buttonForward.setOnClickListener(
                    v -> sendServiceAction(
                            LemonPlaybackService.ACTION_FORWARD
                    )
            );
        }

        if (buttonPrevious != null) {
            buttonPrevious.setOnClickListener(
                    v -> sendServiceAction(
                            LemonPlaybackService.ACTION_PREVIOUS
                    )
            );
        }

        if (buttonNext != null) {
            buttonNext.setOnClickListener(
                    v -> sendServiceAction(
                            LemonPlaybackService.ACTION_NEXT
                    )
            );
        }

        if (lyricsButton != null) {

            lyricsButton.setOnClickListener(v -> {

                Log.d(
                        TAG,
                        "Lyrics button clicked"
                );

                /*
                 * Refresh title/artist from the actual service
                 * immediately before opening LyricsActivity.
                 */
                syncWithPlaybackService();

                Intent intent =
                        new Intent(
                                YouTubeWebActivity.this,
                                LyricsActivity.class
                        );

                intent.putExtra(
                        "song_title",
                        videoTitle
                );

                intent.putExtra(
                        "song_artist",
                        videoArtist
                );

                startActivity(intent);
            });
        }

        if (buttonFloating != null) {
            buttonFloating.setOnClickListener(
                    v -> openFloatingWindow()
            );
        }

        if (buttonQueue != null) {

            buttonQueue.setOnClickListener(
                    v -> Toast.makeText(
                            this,
                            "Queue",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        if (buttonPlaylist != null) {

            buttonPlaylist.setOnClickListener(
                    v -> Toast.makeText(
                            this,
                            "Playlist",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        if (buttonMore != null) {

            buttonMore.setOnClickListener(
                    v -> Toast.makeText(
                            this,
                            "More options",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }

        setupSeekBar();
    }

    // =========================================================
    // SEEK BAR
    // =========================================================

    private void setupSeekBar() {

        if (playerSeekBar == null) {
            return;
        }

        playerSeekBar.setMax(1000);

        playerSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (!fromUser) {
                            return;
                        }

                        userSeeking = true;

                        int duration =
                                getServiceDuration();

                        if (duration <= 0) {
                            return;
                        }

                        int target =
                                (int) (
                                        duration *
                                                (
                                                        progress /
                                                                1000f
                                                )
                                );

                        if (currentTime != null) {

                            currentTime.setText(
                                    formatTime(target)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {

                        userSeeking = true;

                        Log.d(
                                TAG,
                                "User started seeking"
                        );
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {

                        int duration =
                                getServiceDuration();

                        if (duration > 0) {

                            int target =
                                    (int) (
                                            duration *
                                                    (
                                                            seekBar.getProgress()
                                                                    /
                                                                    1000f
                                                    )
                                    );

                            Log.d(
                                    TAG,
                                    "User seek -> " +
                                            target +
                                            " ms"
                            );

                            sendSeek(target);
                        }

                        userSeeking = false;
                    }
                }
        );
    }

    // =========================================================
    // TOGGLE
    // =========================================================

    private void togglePlayback() {

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {

            Log.d(
                    TAG,
                    "Toggle: service NULL -> ACTION_PLAY"
            );

            sendServiceAction(
                    LemonPlaybackService.ACTION_PLAY
            );

            return;
        }

        if (service.isPlaying()) {

            Log.d(
                    TAG,
                    "Toggle: currently PLAYING -> ACTION_PAUSE"
            );

            sendServiceAction(
                    LemonPlaybackService.ACTION_PAUSE
            );

        } else {

            Log.d(
                    TAG,
                    "Toggle: currently NOT PLAYING -> ACTION_PLAY"
            );

            sendServiceAction(
                    LemonPlaybackService.ACTION_PLAY
            );
        }
    }

    // =========================================================
    // SERVICE ACTION
    // =========================================================

    private void sendServiceAction(String action) {

        Log.d(
                TAG,
                "Sending service action = " + action
        );

        Intent intent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        intent.setAction(action);

        startPlaybackService(intent);
    }

    // =========================================================
    // SEEK
    // =========================================================

    private void sendSeek(int position) {

        Log.d(
                TAG,
                "Sending SEEK = " + position + " ms"
        );

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

        startPlaybackService(intent);
    }

    // =========================================================
    // START PLAYBACK SERVICE
    // =========================================================

    private void startPlaybackService(Intent intent) {

        try {

            Log.d(
                    TAG,
                    "Starting PlaybackService"
            );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Log.d(
                    TAG,
                    "PlaybackService start request SENT"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Playback service action FAILED",
                    e
            );
        }
    }

    // =========================================================
    // INNERTUNE PLAYBACK
    // =========================================================

    private synchronized void startInnerTunePlayback() {

        if (extractionStarted ||
                serviceRequestSent) {

            Log.w(
                    TAG,
                    "startInnerTunePlayback ignored - already started"
            );

            return;
        }

        extractionStarted = true;

        showLoading();

        Log.d(
                TAG,
                "================================================"
        );

        Log.d(
                TAG,
                "INNER TUNE PLAYBACK START"
        );

        Log.d(
                TAG,
                "Video ID = " + videoId
        );

        Log.d(
                TAG,
                "Calling InnerTuneAudioExtractor.extract()"
        );

        InnerTuneAudioExtractor.extract(
                videoId,
                new InnerTuneAudioExtractor.Callback() {

                    @Override
                    public void onSuccess(
                            String audioUrl,
                            String extractedTitle,
                            String extractedArtist) {

                        Log.d(
                                TAG,
                                "InnerTune extractor SUCCESS callback received"
                        );

                        runOnUiThread(() -> {

                            if (activityDestroyed) {

                                Log.w(
                                        TAG,
                                        "Activity destroyed - ignoring extraction result"
                                );

                                return;
                            }

                            if (audioUrl == null ||
                                    audioUrl.trim().isEmpty()) {

                                Log.e(
                                        TAG,
                                        "InnerTune returned EMPTY audio URL"
                                );

                                extractionStarted = false;

                                hideLoading();

                                showError(
                                        "InnerTune returned an empty audio URL."
                                );

                                return;
                            }

                            Log.d(
                                    TAG,
                                    "Audio URL received"
                            );

                            Log.d(
                                    TAG,
                                    "Audio URL length = " +
                                            audioUrl.length()
                            );

                            if (extractedTitle != null &&
                                    !extractedTitle.trim().isEmpty()) {

                                videoTitle =
                                        extractedTitle.trim();
                            }

                            if (extractedArtist != null &&
                                    !extractedArtist.trim().isEmpty()) {

                                videoArtist =
                                        extractedArtist.trim();
                            }

                            updateSongInformation();

                            Log.d(
                                    TAG,
                                    "Handing extracted URL to LemonPlaybackService"
                            );

                            startLemonPlaybackService(
                                    audioUrl.trim(),
                                    videoTitle,
                                    videoArtist
                            );
                        });
                    }

                    @Override
                    public void onError(String message) {

                        Log.e(
                                TAG,
                                "InnerTune extractor ERROR callback"
                        );

                        Log.e(
                                TAG,
                                "Video ID = " + videoId
                        );

                        Log.e(
                                TAG,
                                "Reason = " + message
                        );

                        runOnUiThread(() -> {

                            if (activityDestroyed) {
                                return;
                            }

                            extractionStarted = false;

                            hideLoading();

                            String realError =
                                    message == null ||
                                            message.trim().isEmpty()
                                            ? "Unknown InnerTune extraction error."
                                            : message.trim();

                            showError(
                                    "InnerTune: " +
                                            realError
                            );
                        });
                    }
                }
        );
    }

    // =========================================================
    // INNERTUNE → SERVICE
    // =========================================================

    private synchronized void startLemonPlaybackService(
            String audioUrl,
            String title,
            String artist) {

        if (serviceRequestSent) {

            Log.w(
                    TAG,
                    "Service request already sent - ignoring duplicate"
            );

            return;
        }

        if (audioUrl == null ||
                audioUrl.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "Cannot handoff: audio URL EMPTY"
            );

            extractionStarted = false;

            hideLoading();

            showError(
                    "No audio stream was returned."
            );

            return;
        }

        Log.d(
                TAG,
                "================================================"
        );

        Log.d(
                TAG,
                "AUDIO HANDOFF → LemonPlaybackService"
        );

        Log.d(
                TAG,
                "Video ID = " + videoId
        );

        Log.d(
                TAG,
                "Title = " + title
        );

        Log.d(
                TAG,
                "Artist = " + artist
        );

        Log.d(
                TAG,
                "Audio URL length = " +
                        audioUrl.length()
        );

        Intent intent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        intent.setAction(
                LemonPlaybackService.ACTION_LOAD
        );

        intent.putExtra(
                LemonPlaybackService.EXTRA_AUDIO_URL,
                audioUrl
        );

        intent.putExtra(
                LemonPlaybackService.EXTRA_TITLE,
                title
        );

        intent.putExtra(
                LemonPlaybackService.EXTRA_ARTIST,
                artist
        );

        intent.putExtra(
                LemonPlaybackService.EXTRA_VIDEO_ID,
                videoId
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            serviceRequestSent = true;
            extractionStarted = false;

            hideLoading();

            Log.d(
                    TAG,
                    "ACTION_LOAD SENT SUCCESSFULLY"
            );

            Log.d(
                    TAG,
                    "PlaybackService now owns playback"
            );

        } catch (Exception e) {

            serviceRequestSent = false;
            extractionStarted = false;

            Log.e(
                    TAG,
                    "InnerTune → PlaybackService HANDOFF FAILED",
                    e
            );

            hideLoading();

            showError(
                    "Could not start playback."
            );
        }
    }

    // =========================================================
    // UI UPDATE
    // =========================================================

    private void updatePlayerUI() {

        if (activityDestroyed) {
            return;
        }

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {
            return;
        }

        boolean playing =
                service.isPlaying();

        boolean loading =
                service.isLoading();

        int position =
                service.getPosition();

        int duration =
                service.getTrackDuration();

        if (loadingProgress != null) {

            loadingProgress.setVisibility(
                    loading
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (buttonPlayPause != null) {

            buttonPlayPause.setImageResource(
                    playing
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play
            );
        }

        if (buttonCenterPlay != null) {

            buttonCenterPlay.setImageResource(
                    playing
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play
            );
        }

        if (!userSeeking &&
                currentTime != null) {

            currentTime.setText(
                    formatTime(position)
            );
        }

        if (totalTime != null) {

            totalTime.setText(
                    formatTime(duration)
            );
        }

        if (!userSeeking &&
                playerSeekBar != null &&
                duration > 0) {

            playerSeekBar.setProgress(
                    (int) (
                            position /
                                    (float) duration *
                                    1000
                    )
            );
        }
    }

    // =========================================================
    // DURATION
    // =========================================================

    private int getServiceDuration() {

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service == null) {
            return 0;
        }

        return service.getTrackDuration();
    }

    // =========================================================
    // TIME
    // =========================================================

    private String formatTime(int milliseconds) {

        if (milliseconds < 0) {
            milliseconds = 0;
        }

        int seconds =
                milliseconds / 1000;

        int minutes =
                seconds / 60;

        seconds %= 60;

        return minutes +
                ":" +
                (
                        seconds < 10
                                ? "0"
                                : ""
                ) +
                seconds;
    }

    // =========================================================
    // SONG INFO
    // =========================================================

    private void updateSongInformation() {

        if (songTitle != null) {

            songTitle.setText(videoTitle);
        }

        if (songArtist != null) {

            songArtist.setText(videoArtist);
        }
    }

    // =========================================================
    // LOADING
    // =========================================================

    private void showLoading() {

        if (loadingProgress != null) {

            loadingProgress.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void hideLoading() {

        if (loadingProgress != null) {

            loadingProgress.setVisibility(
                    View.GONE
            );
        }
    }

    // =========================================================
    // FLOATING WINDOW
    // =========================================================

    private void openFloatingWindow() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            if (!android.provider.Settings.canDrawOverlays(
                    this
            )) {

                Toast.makeText(
                        this,
                        "Allow Lemon Music to display over other apps.",
                        Toast.LENGTH_LONG
                ).show();

                try {

                    Intent intent =
                            new Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse(
                                            "package:" +
                                                    getPackageName()
                                    )
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    try {

                        startActivity(
                                new Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                )
                        );

                    } catch (Exception ignored) {
                    }
                }

                return;
            }
        }

        getSharedPreferences(
                "lemon_music_player",
                MODE_PRIVATE
        )
                .edit()
                .putString(
                        "video_id",
                        videoId
                )
                .putString(
                        "video_title",
                        videoTitle
                )
                .apply();

        try {

            Intent intent =
                    new Intent(
                            this,
                            FloatingPlayerService.class
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(intent);

            } else {

                startService(intent);
            }

            Log.d(
                    TAG,
                    "FloatingPlayerService STARTED"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not start floating player",
                    e
            );

            Toast.makeText(
                    this,
                    "Could not open floating player.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // ERROR
    // =========================================================

    private void showError(String message) {

        Toast.makeText(
                YouTubeWebActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // INTENT
    // =========================================================

    private void readIntent() {

        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        String value =
                intent.getStringExtra("video_id");

        if (value == null) {

            value =
                    intent.getStringExtra("videoId");
        }

        if (value == null) {

            value =
                    intent.getStringExtra(
                            "youtube_video_id"
                    );
        }

        if (value != null) {

            videoId =
                    extractVideoId(value);
        }

        String title =
                intent.getStringExtra("video_title");

        if (title == null) {

            title =
                    intent.getStringExtra("title");
        }

        if (title != null &&
                !title.trim().isEmpty()) {

            videoTitle =
                    title.trim();
        }

        String artist =
                intent.getStringExtra("video_artist");

        if (artist == null) {

            artist =
                    intent.getStringExtra("artist");
        }

        if (artist != null &&
                !artist.trim().isEmpty()) {

            videoArtist =
                    artist.trim();
        }

        if (videoId.isEmpty()) {

            String url =
                    intent.getStringExtra("url");

            if (url != null) {

                videoId =
                        extractVideoId(url);
            }
        }
    }

    // =========================================================
    // VIDEO ID
    // =========================================================

    private String extractVideoId(String value) {

        if (value == null) {
            return "";
        }

        value = value.trim();

        if (value.isEmpty()) {
            return "";
        }

        if (!value.contains("/")
                && !value.contains("?")
                && !value.contains("=")
                && !value.contains("&")) {

            return value;
        }

        int v =
                value.indexOf("v=");

        if (v >= 0) {

            return cleanId(
                    value.substring(v + 2)
            );
        }

        String marker =
                "youtu.be/";

        int index =
                value.indexOf(marker);

        if (index >= 0) {

            return cleanId(
                    value.substring(
                            index + marker.length()
                    )
            );
        }

        marker = "/embed/";

        index =
                value.indexOf(marker);

        if (index >= 0) {

            return cleanId(
                    value.substring(
                            index + marker.length()
                    )
            );
        }

        return "";
    }

    private String cleanId(String id) {

        if (id == null) {
            return "";
        }

        int index =
                id.indexOf("?");

        if (index >= 0) {

            id =
                    id.substring(0, index);
        }

        index =
                id.indexOf("&");

        if (index >= 0) {

            id =
                    id.substring(0, index);
        }

        index =
                id.indexOf("#");

        if (index >= 0) {

            id =
                    id.substring(0, index);
        }

        return id.trim();
    }

    // =========================================================
    // RESUME
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        Log.d(
                TAG,
                "Activity ON_RESUME"
        );

        /*
         * Re-sync when returning from another Activity,
         * including LyricsActivity.
         */
        syncWithPlaybackService();

        updatePlayerUI();
    }

    // =========================================================
    // PAUSE
    // =========================================================

    @Override
    protected void onPause() {

        Log.d(
                TAG,
                "Activity ON_PAUSE - NOT stopping playback"
        );

        super.onPause();
    }

    // =========================================================
    // BACK
    // =========================================================

    @Override
    public void onBackPressed() {

        Log.d(
                TAG,
                "Back pressed - closing UI only"
        );

        finish();
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        Log.d(
                TAG,
                "Activity ON_DESTROY"
        );

        activityDestroyed = true;

        handler.removeCallbacks(
                progressRunnable
        );

        super.onDestroy();
    }
}