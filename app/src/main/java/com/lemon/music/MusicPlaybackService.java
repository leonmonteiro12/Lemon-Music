package com.lemon.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MusicPlaybackService extends Service {

    // ==================================================
    // DEBUG LOGGING
    // ==================================================

    private static final String TAG =
            "LemonMusicPlayback";

    private static void log(
            String message
    ) {
        Log.d(
                TAG,
                message
        );
    }

    private static void logError(
            String message,
            Throwable error
    ) {
        Log.e(
                TAG,
                message,
                error
        );
    }

    // ==================================================
    // ACTIONS
    // ==================================================

    public static final String ACTION_PLAY =
            "com.lemon.music.ACTION_PLAY";

    public static final String ACTION_PAUSE =
            "com.lemon.music.ACTION_PAUSE";

    public static final String ACTION_RESUME =
            "com.lemon.music.ACTION_RESUME";

    public static final String ACTION_STOP =
            "com.lemon.music.ACTION_STOP";

    public static final String ACTION_NEXT =
            "com.lemon.music.ACTION_NEXT";

    public static final String ACTION_PREVIOUS =
            "com.lemon.music.ACTION_PREVIOUS";

    public static final String ACTION_SEEK_TO =
            "com.lemon.music.ACTION_SEEK_TO";

    public static final String ACTION_YOUTUBE_START =
            "com.lemon.music.ACTION_YOUTUBE_START";

    public static final String ACTION_YOUTUBE_STOP =
            "com.lemon.music.ACTION_YOUTUBE_STOP";

    // ==================================================
    // EXTRAS
    // ==================================================

    public static final String EXTRA_SONG_URI =
            "song_uri";

    public static final String EXTRA_SONG_TITLE =
            "song_title";

    public static final String EXTRA_SONG_ARTIST =
            "song_artist";

    public static final String EXTRA_SEEK_POSITION =
            "seek_position";

    public static final String EXTRA_YOUTUBE_TITLE =
            "youtube_title";

    // ==================================================
    // NOTIFICATION
    // ==================================================

    private static final String CHANNEL_ID =
            "lemon_music_playback";

    private static final int NOTIFICATION_ID =
            1001;

    // ==================================================
    // SAVED STATE
    // ==================================================

    private static final String PREFS =
            "lemon_music_playback_state";

    private static final String PREF_URI =
            "uri";

    private static final String PREF_TITLE =
            "title";

    private static final String PREF_ARTIST =
            "artist";

    private static final String PREF_POSITION =
            "position";

    // ==================================================
    // INSTANCE
    // ==================================================

    private static volatile MusicPlaybackService instance;

    // ==================================================
    // PLAYER
    // ==================================================

    private ExoPlayer player;

    private MediaSessionCompat mediaSession;

    private PowerManager.WakeLock wakeLock;

    private WifiManager.WifiLock wifiLock;

    private AudioManager audioManager;

    private AudioFocusRequest audioFocusRequest;

    // ==================================================
    // CURRENT SONG
    // ==================================================

    private String currentUri;

    private String currentTitle =
            "Lemon Music";

    private String currentArtist =
            "Nothing playing";

    // ==================================================
    // STATE
    // ==================================================

    private boolean foregroundStarted =
            false;

    private boolean youtubeBackgroundMode =
            false;

    private static volatile boolean playing =
            false;

    private static volatile int currentPosition =
            0;

    private static volatile int duration =
            0;

    // ==================================================
    // DEBUG STATE
    // ==================================================

    private int playRequestCount =
            0;

    private int pauseRequestCount =
            0;

    private int resumeRequestCount =
            0;

    private int bufferingCount =
            0;

    private int errorCount =
            0;

    private int readyCount =
            0;

    private int endedCount =
            0;

    // ==================================================
    // CREATE
    // ==================================================

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        log(
                "=================================================="
        );

        log(
                "MusicPlaybackService CREATED"
        );

        log(
                "Android SDK = " +
                Build.VERSION.SDK_INT
        );

        log(
                "=================================================="
        );

        // ==================================================
        // RESTORE SAVED METADATA
        // ==================================================

        restoreSavedPlayback();

        log(
                "Restored URI = " +
                currentUri
        );

        log(
                "Restored title = " +
                currentTitle
        );

        log(
                "Restored position = " +
                currentPosition
        );

        // ==================================================
        // NOTIFICATION
        // ==================================================

        createNotificationChannel();

        // ==================================================
        // AUDIO MANAGER
        // ==================================================

        audioManager =
                (AudioManager)
                        getSystemService(
                                AUDIO_SERVICE
                        );

        log(
                "AudioManager = " +
                (audioManager != null)
        );

        // ==================================================
        // CPU WAKE LOCK
        // ==================================================

        PowerManager powerManager =
                (PowerManager)
                        getSystemService(
                                POWER_SERVICE
                        );

        if (powerManager != null) {

            wakeLock =
                    powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "LemonMusic::Playback"
                    );

            wakeLock.setReferenceCounted(
                    false
            );

            log(
                    "CPU WakeLock created"
            );
        }

        // ==================================================
        // WIFI LOCK
        // ==================================================

        WifiManager wifiManager =
                (WifiManager)
                        getApplicationContext()
                                .getSystemService(
                                        WIFI_SERVICE
                                );

        if (wifiManager != null) {

            try {

                wifiLock =
                        wifiManager.createWifiLock(
                                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                                "LemonMusic::NetworkPlayback"
                        );

                wifiLock.setReferenceCounted(
                        false
                );

                log(
                        "WiFi lock created"
                );

            } catch (Exception e) {

                logError(
                        "Failed to create WiFi lock",
                        e
                );
            }
        }

        // ==================================================
        // EXOPLAYER
        // ==================================================

        player =
                new ExoPlayer.Builder(
                        this
                ).build();

        log(
                "ExoPlayer CREATED"
        );

        player.setAudioAttributes(
                new androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(
                                C.USAGE_MEDIA
                        )
                        .setContentType(
                                C.AUDIO_CONTENT_TYPE_MUSIC
                        )
                        .build(),
                true
        );

        log(
                "ExoPlayer audio attributes configured"
        );

        player.addListener(
                new Player.Listener() {

                    @Override
                    public void onPlaybackStateChanged(
                            int playbackState
                    ) {

                        String stateName =
                                getPlaybackStateName(
                                        playbackState
                                );

                        log(
                                "PLAYER STATE -> " +
                                stateName +
                                " | position=" +
                                getPlayerPosition() +
                                " | duration=" +
                                getPlayerDuration() +
                                " | isPlaying=" +
                                isPlayerActuallyPlaying()
                        );

                        handlePlayerState(
                                playbackState
                        );
                    }

                    @Override
                    public void onIsPlayingChanged(
                            boolean isPlaying
                    ) {

                        log(
                                "PLAYER isPlaying CHANGED -> " +
                                isPlaying +
                                " | state=" +
                                getPlaybackStateName(
                                        player != null
                                                ? player.getPlaybackState()
                                                : -1
                                ) +
                                " | position=" +
                                getPlayerPosition()
                        );

                        playing =
                                isPlaying;

                        updateStaticState();

                        if (isPlaying) {

                            log(
                                    "PLAYER STARTED PLAYING"
                            );

                            startPlaybackForeground();

                            requestAudioFocus();

                            acquireWakeLock();

                            updatePlaybackState(
                                    PlaybackStateCompat.STATE_PLAYING
                            );

                            updateNotification(
                                    true
                            );

                        } else {

                            log(
                                    "PLAYER STOPPED PLAYING / NOT PLAYING" +
                                    " | playerState=" +
                                    getPlaybackStateName(
                                            player != null
                                                    ? player.getPlaybackState()
                                                    : -1
                                    )
                            );

                            updateStaticState();

                            if (player != null &&
                                    player.getPlaybackState() ==
                                            Player.STATE_READY) {

                                log(
                                        "PLAYER is READY but not playing -> PAUSED state"
                                );

                                updatePlaybackState(
                                        PlaybackStateCompat.STATE_PAUSED
                                );

                                updateNotification(
                                        false
                                );
                            }
                        }
                    }

                    @Override
                    public void onPlayerError(
                            PlaybackException error
                    ) {

                        errorCount++;

                        logError(
                                "PLAYER ERROR #" +
                                errorCount +
                                " | errorCode=" +
                                error.errorCode +
                                " | name=" +
                                PlaybackException
                                        .getErrorCodeName(
                                                error.errorCode
                                        ),
                                error
                        );

                        setErrorState(
                                "Playback error."
                        );
                    }

                    @Override
                    public void onPlayWhenReadyChanged(
                            boolean playWhenReady,
                            int reason
                    ) {

                        log(
                                "playWhenReady CHANGED -> " +
                                playWhenReady +
                                " | reason=" +
                                reason +
                                " (" +
                                getPlayWhenReadyReason(
                                        reason
                                ) +
                                ")" +
                                " | state=" +
                                getPlaybackStateName(
                                        player != null
                                                ? player.getPlaybackState()
                                                : -1
                                )
                        );
                    }

                    @Override
                    public void onPlaybackSuppressionReasonChanged(
                            int playbackSuppressionReason
                    ) {

                        log(
                                "PLAYBACK SUPPRESSION -> " +
                                playbackSuppressionReason +
                                " (" +
                                getSuppressionReasonName(
                                        playbackSuppressionReason
                                ) +
                                ")"
                        );
                    }

                    @Override
                    public void onMediaItemTransition(
                            @Nullable MediaItem mediaItem,
                            int reason
                    ) {

                        log(
                                "MEDIA ITEM TRANSITION -> " +
                                (
                                        mediaItem != null
                                                ? mediaItem.mediaId
                                                : "null"
                                ) +
                                " | reason=" +
                                reason
                        );
                    }

                    @Override
                    public void onPositionDiscontinuity(
                            Player.PositionInfo oldPosition,
                            Player.PositionInfo newPosition,
                            int reason
                    ) {

                        log(
                                "POSITION DISCONTINUITY" +
                                " | old=" +
                                oldPosition.positionMs +
                                "ms" +
                                " | new=" +
                                newPosition.positionMs +
                                "ms" +
                                " | reason=" +
                                reason +
                                " (" +
                                getDiscontinuityReasonName(
                                        reason
                                ) +
                                ")"
                        );
                    }
                }
        );

        // ==================================================
        // MEDIA SESSION
        // ==================================================

        mediaSession =
                new MediaSessionCompat(
                        this,
                        "LemonMusicSession"
                );

        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(
                new MediaSessionCompat.Callback() {

                    @Override
                    public void onPlay() {

                        log(
                                "MEDIA SESSION -> onPlay()"
                        );

                        resumePlayback();
                    }

                    @Override
                    public void onPause() {

                        log(
                                "MEDIA SESSION -> onPause()"
                        );

                        pausePlayback();
                    }

                    @Override
                    public void onStop() {

                        log(
                                "MEDIA SESSION -> onStop()"
                        );

                        stopPlayback();
                    }

                    @Override
                    public void onSeekTo(
                            long position
                    ) {

                        log(
                                "MEDIA SESSION -> onSeekTo(" +
                                position +
                                ")"
                        );

                        performSeek(
                                (int) position
                        );
                    }

                    @Override
                    public void onSkipToNext() {

                        log(
                                "MEDIA SESSION -> onSkipToNext()"
                        );

                        handleNext();
                    }

                    @Override
                    public void onSkipToPrevious() {

                        log(
                                "MEDIA SESSION -> onSkipToPrevious()"
                        );

                        handlePrevious();
                    }
                }
        );

        mediaSession.setActive(
                true
        );

        updatePlaybackState(
                PlaybackStateCompat.STATE_NONE
        );

        log(
                "MusicPlaybackService initialization COMPLETE"
        );
    }

    // ==================================================
    // START COMMAND
    // ==================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        log(
                "onStartCommand()" +
                " | startId=" +
                startId +
                " | flags=" +
                flags +
                " | intent=" +
                (intent != null)
        );

        if (currentUri == null ||
                currentUri.trim().isEmpty()) {

            restoreSavedPlayback();
        }

        if (intent == null) {

            log(
                    "onStartCommand -> NULL intent -> START_STICKY"
            );

            return START_STICKY;
        }

        String action =
                intent.getAction();

        log(
                "COMMAND RECEIVED -> " +
                action
        );

        // ==================================================
        // YOUTUBE KEEP ALIVE
        // ==================================================

        if (ACTION_YOUTUBE_START.equals(action)) {

            log(
                    "YOUTUBE_START received"
            );

            youtubeBackgroundMode =
                    true;

            String title =
                    intent.getStringExtra(
                            EXTRA_YOUTUBE_TITLE
                    );

            currentTitle =
                    safeString(
                            title,
                            "YouTube Music"
                    );

            currentArtist =
                    "Lemon Music • YouTube";

            startPlaybackForeground();

            requestAudioFocus();

            acquireWakeLock();

            playing =
                    true;

            updatePlaybackState(
                    PlaybackStateCompat.STATE_PLAYING
            );

            updateNotification(
                    true
            );

            log(
                    "YOUTUBE_START COMPLETE" +
                    " | playing=" +
                    playing +
                    " | youtubeBackgroundMode=" +
                    youtubeBackgroundMode
            );

            return START_STICKY;
        }

        // ==================================================
        // YOUTUBE STOP
        // ==================================================

        if (ACTION_YOUTUBE_STOP.equals(action)) {

            log(
                    "YOUTUBE_STOP received"
            );

            youtubeBackgroundMode =
                    false;

            playing =
                    false;

            releaseAudioFocus();

            releaseWakeLock();

            updatePlaybackState(
                    PlaybackStateCompat.STATE_NONE
            );

            updateNotification(
                    false
            );

            if (player == null ||
                    !player.isPlaying()) {

                stopPlaybackForeground();

                stopSelf();
            }

            return START_NOT_STICKY;
        }

        // ==================================================
        // PLAY
        // ==================================================

        if (ACTION_PLAY.equals(action)) {

            playRequestCount++;

            log(
                    "ACTION_PLAY #" +
                    playRequestCount
            );

            String uri =
                    intent.getStringExtra(
                            EXTRA_SONG_URI
                    );

            String title =
                    intent.getStringExtra(
                            EXTRA_SONG_TITLE
                    );

            String artist =
                    intent.getStringExtra(
                            EXTRA_SONG_ARTIST
                    );

            log(
                    "PLAY URI = " +
                    uri
            );

            log(
                    "PLAY TITLE = " +
                    title
            );

            log(
                    "PLAY ARTIST = " +
                    artist
            );

            if (uri == null ||
                    uri.trim().isEmpty()) {

                log(
                        "ACTION_PLAY FAILED -> empty URI"
                );

                setErrorState(
                        "No playable audio source."
                );

                return START_NOT_STICKY;
            }

            currentUri =
                    uri.trim();

            currentTitle =
                    safeString(
                            title,
                            "Unknown song"
                    );

            currentArtist =
                    safeString(
                            artist,
                            "Unknown artist"
                    );

            currentPosition =
                    0;

            duration =
                    0;

            youtubeBackgroundMode =
                    false;

            playing =
                    false;

            savePlaybackInfo();

            startPlaybackForeground();

            requestAudioFocus();

            acquireWakeLock();

            playSong(
                    currentUri
            );

        } else if (
                ACTION_PAUSE.equals(action)
        ) {

            pauseRequestCount++;

            log(
                    "ACTION_PAUSE #" +
                    pauseRequestCount
            );

            pausePlayback();

        } else if (
                ACTION_RESUME.equals(action)
        ) {

            resumeRequestCount++;

            log(
                    "ACTION_RESUME #" +
                    resumeRequestCount
            );

            resumePlayback();

        } else if (
                ACTION_STOP.equals(action)
        ) {

            log(
                    "ACTION_STOP received"
            );

            stopPlayback();

        } else if (
                ACTION_SEEK_TO.equals(action)
        ) {

            int position =
                    intent.getIntExtra(
                            EXTRA_SEEK_POSITION,
                            0
                    );

            log(
                    "ACTION_SEEK_TO -> " +
                    position +
                    "ms"
            );

            performSeek(
                    position
            );

        } else if (
                ACTION_NEXT.equals(action)
        ) {

            log(
                    "ACTION_NEXT received"
            );

            handleNext();

        } else if (
                ACTION_PREVIOUS.equals(action)
        ) {

            log(
                    "ACTION_PREVIOUS received"
            );

            handlePrevious();

        } else {

            log(
                    "UNKNOWN ACTION -> " +
                    action
            );
        }

        return START_STICKY;
    }

    // ==================================================
    // PLAY SONG
    // ==================================================

    private void playSong(
            String uri
    ) {

        log(
                "=================================================="
        );

        log(
                "playSong() START"
        );

        log(
                "URI = " +
                uri
        );

        log(
                "Player exists = " +
                (player != null)
        );

        try {

            if (player == null) {

                log(
                        "playSong() ABORTED -> player == null"
                );

                return;
            }

            log(
                    "Before player.stop()" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | position=" +
                    player.getCurrentPosition()
            );

            player.stop();

            log(
                    "player.stop() COMPLETE"
            );

            player.clearMediaItems();

            log(
                    "player.clearMediaItems() COMPLETE"
            );

            MediaItem mediaItem =
                    MediaItem.fromUri(
                            Uri.parse(uri)
                    );

            player.setMediaItem(
                    mediaItem
            );

            log(
                    "player.setMediaItem() COMPLETE"
            );

            player.prepare();

            log(
                    "player.prepare() COMPLETE" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    )
            );

            player.play();

            log(
                    "player.play() CALLED" +
                    " | playWhenReady=" +
                    player.getPlayWhenReady()
            );

            playing =
                    true;

            startPlaybackForeground();

            requestAudioFocus();

            acquireWakeLock();

            updateStaticState();

            savePlaybackInfo();

            updatePlaybackState(
                    PlaybackStateCompat.STATE_PLAYING
            );

            updateNotification(
                    true
            );

            log(
                    "playSong() COMPLETE" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | position=" +
                    getPlayerPosition()
            );

            log(
                    "=================================================="
            );

        } catch (Exception e) {

            logError(
                    "playSong() EXCEPTION",
                    e
            );

            setErrorState(
                    "Unable to load audio."
            );
        }
    }

    // ==================================================
    // PLAYER STATE
    // ==================================================

    private void handlePlayerState(
            int state
    ) {

        if (state ==
                Player.STATE_READY) {

            readyCount++;

            log(
                    "STATE_READY #" +
                    readyCount +
                    " | position=" +
                    getPlayerPosition() +
                    " | duration=" +
                    getPlayerDuration() +
                    " | isPlaying=" +
                    isPlayerActuallyPlaying()
            );

            updateStaticState();

            if (player != null &&
                    player.isPlaying()) {

                playing =
                        true;

                updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING
                );

                updateNotification(
                        true
                );
            }

            return;
        }

        if (state ==
                Player.STATE_BUFFERING) {

            bufferingCount++;

            log(
                    "STATE_BUFFERING #" +
                    bufferingCount +
                    " | position=" +
                    getPlayerPosition() +
                    " | playWhenReady=" +
                    (
                            player != null &&
                            player.getPlayWhenReady()
                    ) +
                    " | isPlaying=" +
                    isPlayerActuallyPlaying()
            );

            /*
             * IMPORTANT:
             *
             * Buffering is NOT pause.
             *
             * We intentionally do not set playing=false here.
             */

            updateStaticState();

            return;
        }

        if (state ==
                Player.STATE_ENDED) {

            endedCount++;

            log(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            log(
                    "STATE_ENDED #" +
                    endedCount +
                    " -> PLAYER THINKS SONG IS FINISHED"
            );

            log(
                    "ENDED position=" +
                    getPlayerPosition() +
                    " / duration=" +
                    getPlayerDuration()
            );

            log(
                    "ENDED currentUri=" +
                    currentUri
            );

            log(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
            );

            playing =
                    false;

            updateStaticState();

            updatePlaybackState(
                    PlaybackStateCompat.STATE_NONE
            );

            releaseAudioFocus();

            releaseWakeLock();

            updateNotification(
                    false
            );

            handleNext();
        }
    }

    // ==================================================
    // PAUSE
    // ==================================================

    private void pausePlayback() {

        log(
                "pausePlayback() CALLED" +
                " | playerExists=" +
                (player != null)
        );

        if (player == null) {

            log(
                    "pausePlayback() -> player == null"
            );

            return;
        }

        try {

            log(
                    "PAUSE BEFORE" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | playWhenReady=" +
                    player.getPlayWhenReady() +
                    " | position=" +
                    getPlayerPosition()
            );

            updateStaticState();

            player.pause();

            log(
                    "player.pause() CALLED"
            );

            playing =
                    false;

            savePlaybackInfo();

            updatePlaybackState(
                    PlaybackStateCompat.STATE_PAUSED
            );

            updateNotification(
                    false
            );

            releaseWakeLock();

            log(
                    "PAUSE AFTER" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | playWhenReady=" +
                    player.getPlayWhenReady() +
                    " | position=" +
                    getPlayerPosition()
            );

        } catch (Exception e) {

            logError(
                    "pausePlayback() EXCEPTION",
                    e
            );
        }
    }

    // ==================================================
    // RESUME
    // ==================================================

    private void resumePlayback() {

        log(
                "resumePlayback() CALLED" +
                " | playerExists=" +
                (player != null)
        );

        if (player == null) {

            log(
                    "resumePlayback() -> player == null"
            );

            return;
        }

        try {

            log(
                    "RESUME BEFORE" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | playWhenReady=" +
                    player.getPlayWhenReady() +
                    " | position=" +
                    getPlayerPosition()
            );

            requestAudioFocus();

            acquireWakeLock();

            player.play();

            log(
                    "player.play() CALLED from resume"
            );

            playing =
                    true;

            startPlaybackForeground();

            updateStaticState();

            savePlaybackInfo();

            updatePlaybackState(
                    PlaybackStateCompat.STATE_PLAYING
            );

            updateNotification(
                    true
            );

            log(
                    "RESUME AFTER" +
                    " | state=" +
                    getPlaybackStateName(
                            player.getPlaybackState()
                    ) +
                    " | isPlaying=" +
                    player.isPlaying() +
                    " | playWhenReady=" +
                    player.getPlayWhenReady() +
                    " | position=" +
                    getPlayerPosition()
            );

        } catch (Exception e) {

            logError(
                    "resumePlayback() EXCEPTION",
                    e
            );

            setErrorState(
                    "Unable to resume playback."
            );
        }
    }

    // ==================================================
    // SEEK
    // ==================================================

    private void performSeek(
            int position
    ) {

        if (player == null) {

            log(
                    "performSeek() -> player == null"
            );

            return;
        }

        try {

            long safePosition =
                    Math.max(
                            0,
                            position
                    );

            if (duration > 0) {

                safePosition =
                        Math.min(
                                safePosition,
                                duration
                        );
            }

            log(
                    "SEEK -> " +
                    safePosition +
                    "ms"
            );

            player.seekTo(
                    safePosition
            );

            currentPosition =
                    (int) safePosition;

            savePlaybackInfo();

            updatePlaybackState(
                    playing
                            ? PlaybackStateCompat.STATE_PLAYING
                            : PlaybackStateCompat.STATE_PAUSED
            );

        } catch (Exception e) {

            logError(
                    "performSeek() EXCEPTION",
                    e
            );
        }
    }

    // ==================================================
    // NEXT
    // ==================================================

    private void handleNext() {

        log(
                "handleNext() CALLED"
        );

        /*
         * Queue support remains connected here.
         */
    }

    // ==================================================
    // PREVIOUS
    // ==================================================

    private void handlePrevious() {

        log(
                "handlePrevious() CALLED"
        );

        /*
         * Queue support remains connected here.
         */
    }

    // ==================================================
    // STOP
    // ==================================================

    private void stopPlayback() {

        log(
                "=================================================="
        );

        log(
                "stopPlayback() CALLED"
        );

        youtubeBackgroundMode =
                false;

        if (player != null) {

            try {

                log(
                        "STOP BEFORE" +
                        " | state=" +
                        getPlaybackStateName(
                                player.getPlaybackState()
                        ) +
                        " | isPlaying=" +
                        player.isPlaying() +
                        " | position=" +
                        getPlayerPosition()
                );

                player.stop();

                log(
                        "player.stop() COMPLETE"
                );

                player.clearMediaItems();

            } catch (Exception e) {

                logError(
                        "stopPlayback() player exception",
                        e
                );
            }
        }

        currentUri =
                null;

        currentTitle =
                "Lemon Music";

        currentArtist =
                "Nothing playing";

        currentPosition =
                0;

        duration =
                0;

        playing =
                false;

        clearSavedPlayback();

        releaseAudioFocus();

        releaseWakeLock();

        updatePlaybackState(
                PlaybackStateCompat.STATE_STOPPED
        );

        updateNotification(
                false
        );

        stopPlaybackForeground();

        stopSelf();

        log(
                "stopPlayback() COMPLETE"
        );

        log(
                "=================================================="
        );
    }

    // ==================================================
    // ERROR
    // ==================================================

    private void setErrorState(
            String message
    ) {

        log(
                "setErrorState() -> " +
                message
        );

        playing =
                false;

        updateStaticState();

        updatePlaybackState(
                PlaybackStateCompat.STATE_ERROR
        );

        if (player != null) {

            try {

                log(
                        "Stopping player because of error"
                );

                player.stop();

            } catch (Exception e) {

                logError(
                        "Error while stopping player after error",
                        e
                );
            }
        }

        releaseAudioFocus();

        releaseWakeLock();

        updateNotification(
                false
        );
    }

    // ==================================================
    // AUDIO FOCUS
    // ==================================================

    private void requestAudioFocus() {

        if (audioManager == null) {

            log(
                    "requestAudioFocus() -> AudioManager null"
            );

            return;
        }

        try {

            log(
                    "REQUESTING AUDIO FOCUS"
            );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                AudioAttributes attributes =
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_MEDIA
                                )
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_MUSIC
                                )
                                .build();

                audioFocusRequest =
                        new AudioFocusRequest.Builder(
                                AudioManager.AUDIOFOCUS_GAIN
                        )
                                .setAudioAttributes(
                                        attributes
                                )
                                .setAcceptsDelayedFocusGain(
                                        false
                                )
                                .setOnAudioFocusChangeListener(
                                        focusChange -> {

                                            log(
                                                    "AUDIO FOCUS CHANGE -> " +
                                                    focusChange +
                                                    " (" +
                                                    getAudioFocusName(
                                                            focusChange
                                                    ) +
                                                    ")"
                                            );
                                        }
                                )
                                .build();

                int result =
                        audioManager.requestAudioFocus(
                                audioFocusRequest
                        );

                log(
                        "AUDIO FOCUS RESULT -> " +
                        result +
                        " (" +
                        (
                                result ==
                                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                                        ? "GRANTED"
                                        : "NOT_GRANTED"
                        ) +
                        ")"
                );

            } else {

                int result =
                        audioManager.requestAudioFocus(
                                focusChange -> {

                                    log(
                                            "AUDIO FOCUS CHANGE -> " +
                                            focusChange +
                                            " (" +
                                            getAudioFocusName(
                                                    focusChange
                                            ) +
                                            ")"
                                    );
                                },
                                AudioManager.STREAM_MUSIC,
                                AudioManager.AUDIOFOCUS_GAIN
                        );

                log(
                        "AUDIO FOCUS RESULT -> " +
                        result
                );
            }

        } catch (Exception e) {

            logError(
                    "requestAudioFocus() EXCEPTION",
                    e
            );
        }
    }

    // ==================================================
    // RELEASE AUDIO FOCUS
    // ==================================================

    private void releaseAudioFocus() {

        if (audioManager == null) {

            return;
        }

        try {

            log(
                    "RELEASING AUDIO FOCUS"
            );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                if (audioFocusRequest != null) {

                    audioManager.abandonAudioFocusRequest(
                            audioFocusRequest
                    );
                }

            } else {

                audioManager.abandonAudioFocus(
                        null
                );
            }

        } catch (Exception e) {

            logError(
                    "releaseAudioFocus() EXCEPTION",
                    e
            );
        }
    }

    // ==================================================
    // WAKE LOCK + WIFI LOCK
    // ==================================================

    private void acquireWakeLock() {

        log(
                "ACQUIRE LOCKS"
        );

        if (wakeLock != null) {

            try {

                if (!wakeLock.isHeld()) {

                    wakeLock.acquire();

                    log(
                            "CPU WakeLock ACQUIRED"
                    );
                }

            } catch (Exception e) {

                logError(
                        "CPU WakeLock acquire failed",
                        e
                );
            }
        }

        if (wifiLock != null) {

            try {

                if (!wifiLock.isHeld()) {

                    wifiLock.acquire();

                    log(
                            "WiFi Lock ACQUIRED"
                    );
                }

            } catch (Exception e) {

                logError(
                        "WiFi Lock acquire failed",
                        e
                );
            }
        }
    }

    // ==================================================
    // RELEASE WAKE LOCK + WIFI LOCK
    // ==================================================

    private void releaseWakeLock() {

        log(
                "RELEASE LOCKS"
        );

        if (wakeLock != null) {

            try {

                if (wakeLock.isHeld()) {

                    wakeLock.release();

                    log(
                            "CPU WakeLock RELEASED"
                    );
                }

            } catch (Exception e) {

                logError(
                        "CPU WakeLock release failed",
                        e
                );
            }
        }

        if (wifiLock != null) {

            try {

                if (wifiLock.isHeld()) {

                    wifiLock.release();

                    log(
                            "WiFi Lock RELEASED"
                    );
                }

            } catch (Exception e) {

                logError(
                        "WiFi Lock release failed",
                        e
                );
            }
        }
    }

    // ==================================================
    // FOREGROUND SERVICE
    // ==================================================

    private void startPlaybackForeground() {

        if (foregroundStarted) {

            return;
        }

        try {

            log(
                    "STARTING FOREGROUND PLAYBACK SERVICE"
            );

            Notification notification =
                    createNotification(
                            playing
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );
            }

            foregroundStarted =
                    true;

            log(
                    "FOREGROUND SERVICE STARTED"
            );

        } catch (Exception e) {

            foregroundStarted =
                    false;

            logError(
                    "startPlaybackForeground() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // STOP FOREGROUND
    // ==================================================

    private void stopPlaybackForeground() {

        log(
                "STOPPING FOREGROUND SERVICE"
        );

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.N) {

                stopForeground(
                        STOP_FOREGROUND_REMOVE
                );

            } else {

                stopForeground(
                        true
                );
            }

        } catch (Exception e) {

            logError(
                    "stopPlaybackForeground() FAILED",
                    e
            );
        }

        foregroundStarted =
                false;
    }

    // ==================================================
    // CHANNEL
    // ==================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Lemon Music Playback",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Lemon Music background playback"
        );

        channel.setShowBadge(
                false
        );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager != null) {

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    // ==================================================
    // NOTIFICATION
    // ==================================================

    private Notification createNotification(
            boolean isPlaying
    ) {

        Intent openIntent =
                new Intent(
                        this,
                        Nowplayingactivity.class
                );

        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this,
                        100,
                        openIntent,
                        getPendingIntentFlags()
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                );

        builder.setSmallIcon(
                android.R.drawable.ic_media_play
        );

        builder.setContentTitle(
                currentTitle
        );

        builder.setContentText(
                currentArtist
        );

        builder.setContentIntent(
                contentIntent
        );

        builder.setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
        );

        builder.setPriority(
                NotificationCompat.PRIORITY_LOW
        );

        builder.setCategory(
                NotificationCompat.CATEGORY_TRANSPORT
        );

        builder.setOnlyAlertOnce(
                true
        );

        builder.setShowWhen(
                false
        );

        builder.setOngoing(
                true
        );

        // ==================================================
        // PREVIOUS
        // ==================================================

        Intent previousIntent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        previousIntent.setAction(
                ACTION_PREVIOUS
        );

        PendingIntent previousPendingIntent =
                PendingIntent.getService(
                        this,
                        101,
                        previousIntent,
                        getPendingIntentFlags()
                );

        // ==================================================
        // PLAY / PAUSE
        // ==================================================

        Intent playPauseIntent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        playPauseIntent.setAction(
                isPlaying
                        ? ACTION_PAUSE
                        : ACTION_RESUME
        );

        PendingIntent playPausePendingIntent =
                PendingIntent.getService(
                        this,
                        102,
                        playPauseIntent,
                        getPendingIntentFlags()
                );

        // ==================================================
        // NEXT
        // ==================================================

        Intent nextIntent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        nextIntent.setAction(
                ACTION_NEXT
        );

        PendingIntent nextPendingIntent =
                PendingIntent.getService(
                        this,
                        103,
                        nextIntent,
                        getPendingIntentFlags()
                );

        // ==================================================
        // STOP
        // ==================================================

        Intent stopIntent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        stopIntent.setAction(
                ACTION_STOP
        );

        PendingIntent stopPendingIntent =
                PendingIntent.getService(
                        this,
                        104,
                        stopIntent,
                        getPendingIntentFlags()
                );

        // ==================================================
        // BUTTONS
        // ==================================================

        builder.addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousPendingIntent
        );

        builder.addAction(
                isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play,
                isPlaying
                        ? "Pause"
                        : "Play",
                playPausePendingIntent
        );

        builder.addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
        );

        builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
        );

        return builder.build();
    }

    // ==================================================
    // UPDATE NOTIFICATION
    // ==================================================

    private void updateNotification(
            boolean isPlaying
    ) {

        try {

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            if (manager != null) {

                manager.notify(
                        NOTIFICATION_ID,
                        createNotification(
                                isPlaying
                        )
                );
            }

        } catch (Exception e) {

            logError(
                    "updateNotification() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // MEDIA SESSION
    // ==================================================

    private void updatePlaybackState(
            int state
    ) {

        if (mediaSession == null) {

            return;
        }

        updateStaticState();

        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        float speed =
                playing
                        ? 1.0f
                        : 0.0f;

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(
                                actions
                        )
                        .setState(
                                state,
                                currentPosition,
                                speed
                        )
                        .build();

        mediaSession.setPlaybackState(
                playbackState
        );

        MediaMetadataCompat metadata =
                new MediaMetadataCompat.Builder()
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_TITLE,
                                currentTitle
                        )
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_ARTIST,
                                currentArtist
                        )
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM,
                                "Lemon Music"
                        )
                        .putLong(
                                MediaMetadataCompat.METADATA_KEY_DURATION,
                                duration
                        )
                        .build();

        mediaSession.setMetadata(
                metadata
        );
    }

    // ==================================================
    // STATE UPDATE
    // ==================================================

    private void updateStaticState() {

        if (player == null) {

            return;
        }

        try {

            currentPosition =
                    (int)
                            Math.max(
                                    0,
                                    player.getCurrentPosition()
                            );

            duration =
                    (int)
                            Math.max(
                                    0,
                                    player.getDuration()
                            );

            playing =
                    player.isPlaying();

        } catch (Exception e) {

            logError(
                    "updateStaticState() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // DEBUG PLAYER HELPERS
    // ==================================================

    private long getPlayerPosition() {

        if (player == null) {

            return -1;
        }

        try {

            return player.getCurrentPosition();

        } catch (Exception ignored) {

            return -1;
        }
    }

    private long getPlayerDuration() {

        if (player == null) {

            return -1;
        }

        try {

            return player.getDuration();

        } catch (Exception ignored) {

            return -1;
        }
    }

    private boolean isPlayerActuallyPlaying() {

        if (player == null) {

            return false;
        }

        try {

            return player.isPlaying();

        } catch (Exception ignored) {

            return false;
        }
    }

    private String getPlaybackStateName(
            int state
    ) {

        switch (state) {

            case Player.STATE_IDLE:
                return "IDLE";

            case Player.STATE_BUFFERING:
                return "BUFFERING";

            case Player.STATE_READY:
                return "READY";

            case Player.STATE_ENDED:
                return "ENDED";

            default:
                return "UNKNOWN(" +
                        state +
                        ")";
        }
    }

    private String getPlayWhenReadyReason(
            int reason
    ) {

        switch (reason) {

            case Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST:
                return "USER_REQUEST";

            case Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY:
                return "AUDIO_BECOMING_NOISY";

            case Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS:
                return "AUDIO_FOCUS_LOSS";

            case Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM:
                return "END_OF_MEDIA_ITEM";

            case Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE:
                return "REMOTE";

            default:
                return "UNKNOWN(" +
                        reason +
                        ")";
        }
    }

    private String getSuppressionReasonName(
            int reason
    ) {

        switch (reason) {

            case Player.PLAYBACK_SUPPRESSION_REASON_NONE:
                return "NONE";

            case Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS:
                return "TRANSIENT_AUDIO_FOCUS_LOSS";

            default:
                return "UNKNOWN(" +
                        reason +
                        ")";
        }
    }

    private String getDiscontinuityReasonName(
            int reason
    ) {

        switch (reason) {

            case Player.DISCONTINUITY_REASON_AUTO_TRANSITION:
                return "AUTO_TRANSITION";

            case Player.DISCONTINUITY_REASON_SEEK:
                return "SEEK";

            case Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT:
                return "SEEK_ADJUSTMENT";

            case Player.DISCONTINUITY_REASON_SKIP:
                return "SKIP";

            case Player.DISCONTINUITY_REASON_REMOVE:
                return "REMOVE";

            case Player.DISCONTINUITY_REASON_INTERNAL:
                return "INTERNAL";

            default:
                return "UNKNOWN(" +
                        reason +
                        ")";
        }
    }

    private String getAudioFocusName(
            int change
    ) {

        switch (change) {

            case AudioManager.AUDIOFOCUS_GAIN:
                return "GAIN";

            case AudioManager.AUDIOFOCUS_LOSS:
                return "LOSS";

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                return "LOSS_TRANSIENT";

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                return "LOSS_TRANSIENT_CAN_DUCK";

            default:
                return "UNKNOWN(" +
                        change +
                        ")";
        }
    }

    // ==================================================
    // STATIC API
    // ==================================================

    public static boolean isPlaying() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            service.updateStaticState();
        }

        return playing;
    }

    public static int getCurrentPosition() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            service.updateStaticState();
        }

        return currentPosition;
    }

    public static int getDuration() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            service.updateStaticState();
        }

        return duration;
    }

    public static boolean hasCurrentPlayback() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            return service.currentUri != null &&
                    !service.currentUri.trim().isEmpty();
        }

        return false;
    }

    public static String getCurrentTitle() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            return service.safeString(
                    service.currentTitle,
                    "Lemon Music"
            );
        }

        return "Lemon Music";
    }

    public static String getCurrentArtist() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            return service.safeString(
                    service.currentArtist,
                    "Nothing playing"
            );
        }

        return "Nothing playing";
    }

    public static String getCurrentUri() {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            return service.currentUri;
        }

        return null;
    }

    public static void seekTo(
            int position
    ) {

        MusicPlaybackService service =
                instance;

        if (service != null) {

            service.performSeek(
                    position
            );
        }
    }

    // ==================================================
    // SAVE
    // ==================================================

    private void savePlaybackInfo() {

        try {

            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            PREF_URI,
                            currentUri
                    )
                    .putString(
                            PREF_TITLE,
                            currentTitle
                    )
                    .putString(
                            PREF_ARTIST,
                            currentArtist
                    )
                    .putInt(
                            PREF_POSITION,
                            currentPosition
                    )
                    .apply();

        } catch (Exception e) {

            logError(
                    "savePlaybackInfo() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // RESTORE
    // ==================================================

    private void restoreSavedPlayback() {

        try {

            SharedPreferences prefs =
                    getSharedPreferences(
                            PREFS,
                            MODE_PRIVATE
                    );

            currentUri =
                    prefs.getString(
                            PREF_URI,
                            null
                    );

            currentTitle =
                    prefs.getString(
                            PREF_TITLE,
                            "Lemon Music"
                    );

            currentArtist =
                    prefs.getString(
                            PREF_ARTIST,
                            "Nothing playing"
                    );

            currentPosition =
                    prefs.getInt(
                            PREF_POSITION,
                            0
                    );

        } catch (Exception e) {

            logError(
                    "restoreSavedPlayback() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // CLEAR
    // ==================================================

    private void clearSavedPlayback() {

        try {

            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            )
                    .edit()
                    .clear()
                    .apply();

        } catch (Exception e) {

            logError(
                    "clearSavedPlayback() FAILED",
                    e
            );
        }
    }

    // ==================================================
    // PENDING INTENT FLAGS
    // ==================================================

    private int getPendingIntentFlags() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            return PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    // ==================================================
    // SAFE STRING
    // ==================================================

    private String safeString(
            String value,
            String fallback
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }

    // ==================================================
    // TASK REMOVED
    // ==================================================

    @Override
    public void onTaskRemoved(
            Intent rootIntent
    ) {

        log(
                "onTaskRemoved()" +
                " | playing=" +
                playing +
                " | youtubeBackgroundMode=" +
                youtubeBackgroundMode
        );

        if (playing ||
                youtubeBackgroundMode) {

            startPlaybackForeground();

            savePlaybackInfo();
        }

        super.onTaskRemoved(
                rootIntent
        );
    }

    // ==================================================
    // DESTROY
    // ==================================================

    @Override
    public void onDestroy() {

        log(
                "=================================================="
        );

        log(
                "MusicPlaybackService DESTROYED"
        );

        log(
                "Final counters:" +
                " play=" +
                playRequestCount +
                " pause=" +
                pauseRequestCount +
                " resume=" +
                resumeRequestCount +
                " buffering=" +
                bufferingCount +
                " ready=" +
                readyCount +
                " ended=" +
                endedCount +
                " errors=" +
                errorCount
        );

        updateStaticState();

        savePlaybackInfo();

        if (player != null) {

            try {

                player.release();

                log(
                        "ExoPlayer RELEASED"
                );

            } catch (Exception e) {

                logError(
                        "ExoPlayer release failed",
                        e
                );
            }

            player =
                    null;
        }

        releaseAudioFocus();

        releaseWakeLock();

        if (mediaSession != null) {

            mediaSession.setActive(
                    false
            );

            mediaSession.release();

            mediaSession =
                    null;
        }

        stopPlaybackForeground();

        playing =
                false;

        youtubeBackgroundMode =
                false;

        instance =
                null;

        super.onDestroy();

        log(
                "MusicPlaybackService destroy COMPLETE"
        );

        log(
                "=================================================="
        );
    }

    // ==================================================
    // BIND
    // ==================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}