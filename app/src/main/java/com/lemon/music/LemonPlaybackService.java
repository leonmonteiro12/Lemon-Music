package com.lemon.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.C;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.ArrayList;
import java.util.List;

public class LemonPlaybackService extends Service {

    private static final String TAG =
            "LemonMusicPlayback";

    // =========================================================
    // ACTIONS
    // =========================================================

    public static final String ACTION_LOAD =
            "com.lemon.music.action.LOAD";

    public static final String ACTION_PLAY =
            "com.lemon.music.action.PLAY";

    public static final String ACTION_PAUSE =
            "com.lemon.music.action.PAUSE";

    public static final String ACTION_REWIND =
            "com.lemon.music.action.REWIND";

    public static final String ACTION_FORWARD =
            "com.lemon.music.action.FORWARD";

    public static final String ACTION_SEEK =
            "com.lemon.music.action.SEEK";

    public static final String ACTION_STOP =
            "com.lemon.music.action.STOP";

    public static final String ACTION_NEXT =
            "com.lemon.music.action.NEXT";

    public static final String ACTION_PREVIOUS =
            "com.lemon.music.action.PREVIOUS";

    // =========================================================
    // EXTRAS
    // =========================================================

    public static final String EXTRA_AUDIO_URL =
            "audio_url";

    public static final String EXTRA_TITLE =
            "title";

    public static final String EXTRA_ARTIST =
            "artist";

    public static final String EXTRA_POSITION =
            "position";

    public static final String EXTRA_VIDEO_ID =
            "video_id";

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private static final int NOTIFICATION_ID =
            5001;

    private static final String CHANNEL_ID =
            "lemon_music_playback";

    // =========================================================
    // SINGLETON
    // =========================================================

    private static LemonPlaybackService instance;

    public static LemonPlaybackService getInstance() {
        return instance;
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private ExoPlayer player;

    private MediaSessionCompat mediaSession;

    private boolean loading = false;

    private boolean prepared = false;

    private boolean manuallyStopped = false;

    private String currentUrl = "";

    private String currentVideoId = "";

    private String currentTitle =
            "YouTube Music";

    private String currentArtist =
            "YouTube";

    // =========================================================
    // QUEUE
    // =========================================================

    private final List<QueueItem> playlistQueue =
            new ArrayList<>();

    private int currentIndex = -1;

    private boolean queueEnabled = false;

    // =========================================================
    // QUEUE ITEM
    // =========================================================

    public static class QueueItem {

        public final String videoId;
        public final String title;
        public final String artist;

        public QueueItem(
                String videoId,
                String title,
                String artist) {

            this.videoId =
                    videoId == null
                            ? ""
                            : videoId.trim();

            this.title =
                    title == null ||
                            title.trim().isEmpty()
                            ? "YouTube Music"
                            : title.trim();

            this.artist =
                    artist == null ||
                            artist.trim().isEmpty()
                            ? "YouTube"
                            : artist.trim();
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public void onCreate() {

        super.onCreate();

        instance = this;

        log(
                "================================================"
        );

        log(
                "SERVICE CREATED"
        );

        log(
                "Playback engine = Media3 ExoPlayer"
        );

        log(
                "Process/thread = " +
                        Thread.currentThread().getName()
        );

        createNotificationChannel();

        createMediaSession();

        createPlayer();

        try {

            startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
            );

            log(
                    "startForeground() SUCCESS"
            );

        } catch (Exception e) {

            error(
                    "startForeground() FAILED",
                    e
            );
        }
    }

    // =========================================================
    // CREATE EXOPLAYER
    // =========================================================

    private void createPlayer() {

        log(
                "Creating Media3 ExoPlayer"
        );

        try {

            DefaultHttpDataSource.Factory httpFactory =
                    new DefaultHttpDataSource.Factory()
                            .setUserAgent(
                                    "Lemon Music"
                            )
                            .setAllowCrossProtocolRedirects(
                                    true
                            );

            DefaultDataSource.Factory dataSourceFactory =
                    new DefaultDataSource.Factory(
                            this,
                            httpFactory
                    );

            player =
                    new ExoPlayer.Builder(this)
                            .setMediaSourceFactory(
                                    new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                                            dataSourceFactory
                                    )
                            )
                            .build();

            player.setHandleAudioBecomingNoisy(
                    true
            );

            player.addListener(
                    new Player.Listener() {

                        @Override
                        public void onPlaybackStateChanged(
                                int playbackState) {

                            handlePlaybackState(
                                    playbackState
                            );
                        }

                        @Override
                        public void onIsPlayingChanged(
                                boolean isPlaying) {

                            log(
                                    "ExoPlayer isPlayingChanged = " +
                                            isPlaying
                            );

                            updateMediaSessionState();

                            updateNotification();
                        }

                        @Override
                        public void onPlayerError(
                                PlaybackException error) {

                            handlePlayerError(
                                    error
                            );
                        }
                    }
            );

            log(
                    "Media3 ExoPlayer CREATED"
            );

            log(
                    "ExoPlayer listeners installed"
            );

        } catch (Exception e) {

            error(
                    "ExoPlayer creation FAILED",
                    e
            );
        }
    }

    // =========================================================
    // PLAYBACK STATE
    // =========================================================

    private synchronized void handlePlaybackState(
            int playbackState) {

        if (player == null) {
            return;
        }

        switch (playbackState) {

            case Player.STATE_IDLE:

                log(
                        "ExoPlayer STATE_IDLE"
                );

                loading = false;

                prepared = false;

                break;

            case Player.STATE_BUFFERING:

                log(
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                );

                log(
                        "ExoPlayer STATE_BUFFERING"
                );

                log(
                        "Position = " +
                                getPosition() +
                                " / " +
                                getDuration()
                );

                log(
                        "Buffered position = " +
                                player.getBufferedPosition()
                );

                log(
                        "Buffered percentage = " +
                                player.getBufferedPercentage() +
                                "%"
                );

                loading = true;

                prepared = false;

                break;

            case Player.STATE_READY:

                log(
                        "ExoPlayer STATE_READY"
                );

                log(
                        "Title = " +
                                currentTitle
                );

                log(
                        "Video ID = " +
                                currentVideoId
                );

                log(
                        "Duration = " +
                                getDuration()
                );

                log(
                        "Buffered position = " +
                                player.getBufferedPosition()
                );

                loading = false;

                prepared = true;

                manuallyStopped = false;

                break;

            case Player.STATE_ENDED:

                loading = false;

                prepared = false;

                log(
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                );

                log(
                        "ExoPlayer STATE_ENDED"
                );

                log(
                        "Completed title = " +
                                currentTitle
                );

                log(
                        "Completed video ID = " +
                                currentVideoId
                );

                log(
                        "manuallyStopped = " +
                                manuallyStopped
                );

                if (manuallyStopped) {

                    log(
                            "END ignored because manuallyStopped=true"
                    );

                    updateMediaSessionState();

                    updateNotification();

                    return;
                }

                log(
                        "Completion was NOT manual"
                );

                log(
                        "Attempting NEXT queue item"
                );

                playNextFromQueue();

                break;

            default:

                break;
        }

        updateMediaSessionState();

        updateNotification();
    }

    // =========================================================
    // PLAYER ERROR
    // =========================================================

    private synchronized void handlePlayerError(
            PlaybackException error) {

        loading = false;

        prepared = false;

        Log.e(
                TAG,
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        );

        Log.e(
                TAG,
                "ExoPlayer EVENT: ERROR"
        );

        if (error != null) {

            Log.e(
                    TAG,
                    "errorCode = " +
                            error.errorCode
            );

            Log.e(
                    TAG,
                    "errorName = " +
                            PlaybackException.getErrorCodeName(
                                    error.errorCode
                            )
            );

            Log.e(
                    TAG,
                    "message = " +
                            error.getMessage(),
                    error
            );
        }

        Log.e(
                TAG,
                "title = " +
                        currentTitle
        );

        Log.e(
                TAG,
                "videoId = " +
                        currentVideoId
        );

        Log.e(
                TAG,
                "url length = " +
                        (
                                currentUrl == null
                                        ? 0
                                        : currentUrl.length()
                        )
        );

        if (queueEnabled &&
                currentIndex >= 0 &&
                currentIndex <
                        playlistQueue.size() - 1) {

            log(
                    "ExoPlayer error -> trying NEXT queue item"
            );

            playNextFromQueue();

        } else {

            log(
                    "ExoPlayer error -> no next queue item"
            );

            updateMediaSessionState();

            updateNotification();
        }
    }

    // =========================================================
    // MEDIA SESSION
    // =========================================================

    private void createMediaSession() {

        mediaSession =
                new MediaSessionCompat(
                        this,
                        "LemonMusic"
                );

        mediaSession.setCallback(
                new MediaSessionCompat.Callback() {

                    @Override
                    public void onPlay() {

                        log(
                                "MediaSession CALLBACK: onPlay()"
                        );

                        play();
                    }

                    @Override
                    public void onPause() {

                        log(
                                "MediaSession CALLBACK: onPause()"
                        );

                        pause();
                    }

                    @Override
                    public void onStop() {

                        log(
                                "MediaSession CALLBACK: onStop()"
                        );

                        stopPlayback();
                    }

                    @Override
                    public void onSeekTo(
                            long position) {

                        log(
                                "MediaSession CALLBACK: onSeekTo(" +
                                        position +
                                        ")"
                        );

                        seekTo(
                                (int) position
                        );
                    }

                    @Override
                    public void onSkipToNext() {

                        log(
                                "MediaSession CALLBACK: onSkipToNext()"
                        );

                        playNextFromQueue();
                    }

                    @Override
                    public void onSkipToPrevious() {

                        log(
                                "MediaSession CALLBACK: onSkipToPrevious()"
                        );

                        playPreviousFromQueue();
                    }
                }
        );

        mediaSession.setActive(true);

        log(
                "MediaSession CREATED and ACTIVE"
        );
    }

    // =========================================================
    // START COMMAND
    // =========================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        log(
                "------------------------------------------------"
        );

        log(
                "onStartCommand()"
        );

        log(
                "startId = " +
                        startId
        );

        log(
                "flags = " +
                        flags
        );

        if (intent == null) {

            log(
                    "Intent = NULL"
            );

            return START_STICKY;
        }

        String action =
                intent.getAction();

        log(
                "ACTION = " +
                        action
        );

        log(
                "Current title = " +
                        currentTitle
        );

        log(
                "Current video ID = " +
                        currentVideoId
        );

        log(
                "prepared = " +
                        prepared +
                        " loading = " +
                        loading +
                        " playing = " +
                        isPlaying()
        );

        if (action == null) {

            log(
                    "ACTION is NULL"
            );

            return START_STICKY;
        }

        switch (action) {

            case ACTION_LOAD:

                log(
                        "ACTION_LOAD received"
                );

                manuallyStopped = false;

                String loadUrl =
                        intent.getStringExtra(
                                EXTRA_AUDIO_URL
                        );

                String loadTitle =
                        intent.getStringExtra(
                                EXTRA_TITLE
                        );

                String loadArtist =
                        intent.getStringExtra(
                                EXTRA_ARTIST
                        );

                String loadVideoId =
                        intent.getStringExtra(
                                EXTRA_VIDEO_ID
                        );

                log(
                        "LOAD video ID = " +
                                loadVideoId
                );

                log(
                        "LOAD title = " +
                                loadTitle
                );

                log(
                        "LOAD artist = " +
                                loadArtist
                );

                log(
                        "LOAD URL length = " +
                                (
                                        loadUrl == null
                                                ? 0
                                                : loadUrl.length()
                                )
                );

                loadTrack(
                        loadUrl,
                        loadTitle,
                        loadArtist,
                        loadVideoId
                );

                break;

            case ACTION_PLAY:

                log(
                        "ACTION_PLAY received"
                );

                manuallyStopped = false;

                play();

                break;

            case ACTION_PAUSE:

                log(
                        "!!!!!!!!!!!!!!!! ACTION_PAUSE RECEIVED !!!!!!!!!!!!!!!!"
                );

                pause();

                break;

            case ACTION_REWIND:

                log(
                        "ACTION_REWIND received"
                );

                rewind();

                break;

            case ACTION_FORWARD:

                log(
                        "ACTION_FORWARD received"
                );

                forward();

                break;

            case ACTION_SEEK:

                int seekPosition =
                        intent.getIntExtra(
                                EXTRA_POSITION,
                                0
                        );

                log(
                        "ACTION_SEEK received = " +
                                seekPosition
                );

                seekTo(
                        seekPosition
                );

                break;

            case ACTION_NEXT:

                log(
                        "ACTION_NEXT received"
                );

                playNextFromQueue();

                break;

            case ACTION_PREVIOUS:

                log(
                        "ACTION_PREVIOUS received"
                );

                playPreviousFromQueue();

                break;

            case ACTION_STOP:

                log(
                        "!!!!!!!!!!!!!!!! ACTION_STOP RECEIVED !!!!!!!!!!!!!!!!"
                );

                stopPlayback();

                break;

            default:

                log(
                        "UNKNOWN ACTION = " +
                                action
                );

                break;
        }

        return START_STICKY;
    }

    // =========================================================
    // QUEUE API
    // =========================================================

    public synchronized void setPlaylistQueue(
            List<QueueItem> items,
            int startIndex) {

        playlistQueue.clear();

        if (items != null) {

            playlistQueue.addAll(items);
        }

        queueEnabled =
                !playlistQueue.isEmpty();

        if (!queueEnabled) {

            currentIndex = -1;

            log(
                    "Playlist queue CLEARED"
            );

            return;
        }

        if (startIndex < 0) {
            startIndex = 0;
        }

        if (startIndex >= playlistQueue.size()) {

            startIndex =
                    playlistQueue.size() - 1;
        }

        currentIndex =
                startIndex;

        log(
                "Playlist queue SET"
        );

        log(
                "Queue size = " +
                        playlistQueue.size()
        );

        log(
                "Start index = " +
                        currentIndex
        );
    }

    public synchronized void clearPlaylistQueue() {

        playlistQueue.clear();

        currentIndex = -1;

        queueEnabled = false;

        log(
                "Playlist queue CLEARED"
        );
    }

    public synchronized int getQueueSize() {

        return playlistQueue.size();
    }

    public synchronized int getCurrentIndex() {

        return currentIndex;
    }

    public synchronized boolean hasNext() {

        return queueEnabled &&
                currentIndex >= 0 &&
                currentIndex <
                        playlistQueue.size() - 1;
    }

    public synchronized boolean hasPrevious() {

        return queueEnabled &&
                currentIndex > 0 &&
                currentIndex <
                        playlistQueue.size();
    }

    // =========================================================
    // NEXT
    // =========================================================

    public synchronized void playNextFromQueue() {

        if (!queueEnabled) {

            log(
                    "NEXT ignored: queue disabled"
            );

            updateMediaSessionState();

            updateNotification();

            return;
        }

        if (currentIndex + 1 >=
                playlistQueue.size()) {

            log(
                    "Playlist reached END"
            );

            prepared = false;

            loading = false;

            updateMediaSessionState();

            updateNotification();

            return;
        }

        currentIndex++;

        QueueItem item =
                playlistQueue.get(
                        currentIndex
                );

        log(
                "NEXT -> index " +
                        currentIndex +
                        " / " +
                        playlistQueue.size()
        );

        extractAndPlayQueueItem(
                item
        );
    }

    // =========================================================
    // PREVIOUS
    // =========================================================

    public synchronized void playPreviousFromQueue() {

        if (!queueEnabled) {

            log(
                    "PREVIOUS with no queue -> REWIND"
            );

            rewind();

            return;
        }

        if (prepared &&
                getPosition() > 3000) {

            log(
                    "PREVIOUS while >3 seconds -> seek 0"
            );

            seekTo(0);

            return;
        }

        if (currentIndex - 1 < 0) {

            log(
                    "PREVIOUS at queue beginning -> seek 0"
            );

            seekTo(0);

            return;
        }

        currentIndex--;

        QueueItem item =
                playlistQueue.get(
                        currentIndex
                );

        log(
                "PREVIOUS -> index " +
                        currentIndex
        );

        extractAndPlayQueueItem(
                item
        );
    }

    // =========================================================
    // QUEUE EXTRACTION
    // =========================================================

    private synchronized void extractAndPlayQueueItem(
            QueueItem item) {

        if (item == null ||
                item.videoId == null ||
                item.videoId.trim().isEmpty()) {

            log(
                    "Invalid queue item"
            );

            playNextFromQueue();

            return;
        }

        currentVideoId =
                item.videoId;

        currentTitle =
                item.title;

        currentArtist =
                item.artist;

        currentUrl = "";

        loading = true;

        prepared = false;

        manuallyStopped = false;

        log(
                "QUEUE EXTRACTION START"
        );

        log(
                "Video ID = " +
                        currentVideoId
        );

        log(
                "Title = " +
                        currentTitle
        );

        updateMediaSessionState();

        updateNotification();

        NewPipeAudioExtractor.extract(
                currentVideoId,
                new NewPipeAudioExtractor.Callback() {

                    @Override
                    public void onSuccess(
                            String audioUrl,
                            String extractedTitle,
                            String extractedArtist) {

                        log(
                                "QUEUE EXTRACTION SUCCESS"
                        );

                        log(
                                "Audio URL length = " +
                                        (
                                                audioUrl == null
                                                        ? 0
                                                        : audioUrl.length()
                                        )
                        );

                        if (audioUrl == null ||
                                audioUrl.trim().isEmpty()) {

                            log(
                                    "Queue extraction returned EMPTY URL"
                            );

                            loading = false;

                            prepared = false;

                            playNextFromQueue();

                            return;
                        }

                        if (extractedTitle != null &&
                                !extractedTitle.trim().isEmpty()) {

                            currentTitle =
                                    extractedTitle.trim();
                        }

                        if (extractedArtist != null &&
                                !extractedArtist.trim().isEmpty()) {

                            currentArtist =
                                    extractedArtist.trim();
                        }

                        loadExtractedAudio(
                                audioUrl
                        );
                    }

                    @Override
                    public void onError(
                            String message) {

                        Log.e(
                                TAG,
                                "QUEUE EXTRACTION FAILED: " +
                                        message
                        );

                        loading = false;

                        prepared = false;

                        playNextFromQueue();
                    }
                }
        );
    }

    // =========================================================
    // LOAD EXTRACTED AUDIO
    // =========================================================

    private synchronized void loadExtractedAudio(
            String audioUrl) {

        if (player == null) {

            Log.e(
                    TAG,
                    "loadExtractedAudio(): ExoPlayer NULL"
            );

            return;
        }

        if (audioUrl == null ||
                audioUrl.trim().isEmpty()) {

            Log.e(
                    TAG,
                    "loadExtractedAudio(): EMPTY URL"
            );

            loading = false;

            prepared = false;

            return;
        }

        currentUrl =
                audioUrl.trim();

        log(
                "Loading extracted audio into Media3 ExoPlayer"
        );

        log(
                "URL length = " +
                        currentUrl.length()
        );

        try {

            loading = true;

            prepared = false;

            manuallyStopped = false;

            MediaItem mediaItem =
                    MediaItem.fromUri(
                            currentUrl
                    );

            log(
                    "Calling ExoPlayer.setMediaItem()"
            );

            player.setMediaItem(
                    mediaItem
            );

            log(
                    "Calling ExoPlayer.prepare()"
            );

            player.prepare();

            log(
                    "Calling ExoPlayer.play()"
            );

            player.play();

            log(
                    "ExoPlayer.play() RETURNED"
            );

            updateMediaSessionState();

            updateNotification();

        } catch (Exception e) {

            loading = false;

            prepared = false;

            error(
                    "Could not load extracted audio into ExoPlayer",
                    e
            );

            playNextFromQueue();
        }
    }

    // =========================================================
    // DIRECT LOAD
    // =========================================================

    private synchronized void loadTrack(
            String url,
            String title,
            String artist,
            String videoId) {

        log(
                "================================================"
        );

        log(
                "DIRECT LOAD START"
        );

        if (url == null ||
                url.trim().isEmpty()) {

            log(
                    "DIRECT LOAD FAILED: EMPTY URL"
            );

            return;
        }

        if (videoId != null &&
                !videoId.trim().isEmpty()) {

            currentVideoId =
                    videoId.trim();
        }

        currentUrl =
                url.trim();

        if (title != null &&
                !title.trim().isEmpty()) {

            currentTitle =
                    title.trim();
        }

        if (artist != null &&
                !artist.trim().isEmpty()) {

            currentArtist =
                    artist.trim();
        }

        log(
                "Video ID = " +
                        currentVideoId
        );

        log(
                "Title = " +
                        currentTitle
        );

        log(
                "Artist = " +
                        currentArtist
        );

        log(
                "Audio URL length = " +
                        currentUrl.length()
        );

        loading = true;

        prepared = false;

        manuallyStopped = false;

        try {

            MediaItem mediaItem =
                    MediaItem.fromUri(
                            currentUrl
                    );

            log(
                    "ExoPlayer.setMediaItem()"
            );

            player.setMediaItem(
                    mediaItem
            );

            log(
                    "ExoPlayer.prepare()"
            );

            player.prepare();

            log(
                    "ExoPlayer.play()"
            );

            player.play();

            log(
                    "DIRECT LOAD ExoPlayer.play() RETURNED"
            );

            updateMediaSessionState();

            updateNotification();

        } catch (Exception e) {

            loading = false;

            prepared = false;

            error(
                    "Could not load audio",
                    e
            );

            updateNotification();
        }
    }

    // =========================================================
    // PLAY
    // =========================================================

    public synchronized void play() {

        log(
                "PLAY() called"
        );

        log(
                "prepared = " +
                        prepared +
                        ", loading = " +
                        loading
        );

        if (player == null) {

            log(
                    "PLAY ignored: ExoPlayer NULL"
            );

            return;
        }

        try {

            manuallyStopped = false;

            log(
                    "ExoPlayer playbackState = " +
                            playbackStateName(
                                    player.getPlaybackState()
                            )
            );

            log(
                    "ExoPlayer isPlaying BEFORE PLAY = " +
                            player.isPlaying()
            );

            /*
             * IMPORTANT:
             *
             * Unlike the old MediaPlayer implementation,
             * ExoPlayer can accept play() while BUFFERING.
             *
             * It will start automatically as soon as enough
             * data is available.
             */

            player.play();

            log(
                    "ExoPlayer.play() RETURNED"
            );

            updateMediaSessionState();

            updateNotification();

        } catch (Exception e) {

            error(
                    "PLAY failed",
                    e
            );
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    public synchronized void pause() {

        Log.w(
                TAG,
                "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        );

        Log.w(
                TAG,
                "PAUSE() CALLED"
        );

        Log.w(
                TAG,
                "Current title = " +
                        currentTitle
        );

        Log.w(
                TAG,
                "Current video ID = " +
                        currentVideoId
        );

        if (player == null) {

            Log.w(
                    TAG,
                    "PAUSE ignored: ExoPlayer NULL"
            );

            return;
        }

        try {

            boolean wasPlaying =
                    player.isPlaying();

            Log.w(
                    TAG,
                    "ExoPlayer.isPlaying() BEFORE PAUSE = " +
                            wasPlaying
            );

            player.pause();

            Log.w(
                    TAG,
                    "ExoPlayer.pause() RETURNED"
            );

            updateMediaSessionState();

            updateNotification();

        } catch (Exception e) {

            error(
                    "PAUSE failed",
                    e
            );
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    public synchronized void stopPlayback() {

        Log.w(
                TAG,
                "!!!!!!!!!!!!!!!! ACTION STOP / stopPlayback() !!!!!!!!!!!!!!!!"
        );

        manuallyStopped = true;

        try {

            if (player != null) {

                log(
                        "Calling ExoPlayer.stop()"
                );

                player.stop();

                log(
                        "ExoPlayer.stop() RETURNED"
                );
            }

        } catch (Exception e) {

            error(
                    "ExoPlayer.stop() failed",
                    e
            );
        }

        prepared = false;

        loading = false;

        updateMediaSessionState();

        updateNotification();
    }

    // =========================================================
    // REWIND
    // =========================================================

    public synchronized void rewind() {

        if (player == null) {

            log(
                    "REWIND ignored: ExoPlayer NULL"
            );

            return;
        }

        try {

            long current =
                    player.getCurrentPosition();

            long target =
                    Math.max(
                            0L,
                            current - 10000L
                    );

            log(
                    "REWIND " +
                            current +
                            " -> " +
                            target
            );

            player.seekTo(
                    target
            );

            updateMediaSessionState();

        } catch (Exception e) {

            error(
                    "REWIND failed",
                    e
            );
        }
    }

    // =========================================================
    // FORWARD
    // =========================================================

    public synchronized void forward() {

        if (player == null) {

            log(
                    "FORWARD ignored: ExoPlayer NULL"
            );

            return;
        }

        try {

            long current =
                    player.getCurrentPosition();

            long duration =
                    player.getDuration();

            long target;

            if (duration <= 0 ||
                    duration == C.TIME_UNSET) {

                target =
                        current + 10000L;

            } else {

                target =
                        Math.min(
                                duration,
                                current + 10000L
                        );
            }

            log(
                    "FORWARD " +
                            current +
                            " -> " +
                            target
            );

            player.seekTo(
                    target
            );

            updateMediaSessionState();

        } catch (Exception e) {

            error(
                    "FORWARD failed",
                    e
            );
        }
    }

    // =========================================================
    // SEEK
    // =========================================================

    public synchronized void seekTo(
            int position) {

        if (player == null) {

            log(
                    "SEEK ignored: ExoPlayer NULL"
            );

            return;
        }

        try {

            long duration =
                    player.getDuration();

            long original =
                    position;

            long target =
                    Math.max(
                            0L,
                            position
                    );

            if (duration > 0 &&
                    duration != C.TIME_UNSET) {

                target =
                        Math.min(
                                duration,
                                target
                        );
            }

            log(
                    "SEEK " +
                            original +
                            " -> " +
                            target +
                            " duration=" +
                            duration
            );

            player.seekTo(
                    target
            );

            updateMediaSessionState();

        } catch (Exception e) {

            error(
                    "SEEK failed",
                    e
            );
        }
    }

    // =========================================================
    // STATE
    // =========================================================

    public boolean isPlaying() {

        try {

            return player != null &&
                    player.isPlaying();

        } catch (Exception e) {

            return false;
        }
    }

    public boolean isLoading() {

        if (player == null) {

            return loading;
        }

        try {

            return loading ||
                    player.getPlaybackState() ==
                            Player.STATE_BUFFERING;

        } catch (Exception e) {

            return loading;
        }
    }

    public int getPosition() {

        try {

            if (player == null) {

                return 0;
            }

            long position =
                    player.getCurrentPosition();

            if (position < 0) {

                return 0;
            }

            if (position > Integer.MAX_VALUE) {

                return Integer.MAX_VALUE;
            }

            return (int) position;

        } catch (Exception e) {

            return 0;
        }
    }

    public int getTrackDuration() {

        try {

            if (player == null) {

                return 0;
            }

            long duration =
                    player.getDuration();

            if (duration <= 0 ||
                    duration == C.TIME_UNSET) {

                return 0;
            }

            if (duration > Integer.MAX_VALUE) {

                return Integer.MAX_VALUE;
            }

            return (int) duration;

        } catch (Exception e) {

            return 0;
        }
    }

    public int getDuration() {

        return getTrackDuration();
    }

    public String getCurrentTitle() {

        return currentTitle;
    }

    public String getCurrentArtist() {

        return currentArtist;
    }

    public String getCurrentVideoId() {

        return currentVideoId;
    }

    // =========================================================
    // MEDIA SESSION STATE
    // =========================================================

    private void updateMediaSessionState() {

        if (mediaSession == null) {
            return;
        }

        long actions =
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_STOP;

        if (hasNext()) {

            actions |=
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }

        if (hasPrevious()) {

            actions |=
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }

        int state;

        if (loading ||
                isPlayerBuffering()) {

            state =
                    PlaybackStateCompat.STATE_BUFFERING;

        } else if (isPlaying()) {

            state =
                    PlaybackStateCompat.STATE_PLAYING;

        } else {

            state =
                    PlaybackStateCompat.STATE_PAUSED;
        }

        long position =
                getPosition();

        PlaybackStateCompat playbackState =
                new PlaybackStateCompat.Builder()
                        .setActions(actions)
                        .setState(
                                state,
                                position,
                                1.0f
                        )
                        .build();

        mediaSession.setPlaybackState(
                playbackState
        );
    }

    private boolean isPlayerBuffering() {

        try {

            return player != null &&
                    player.getPlaybackState() ==
                            Player.STATE_BUFFERING;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Lemon Music Playback",
                        NotificationManager.IMPORTANCE_LOW
                );

        channel.setDescription(
                "Lemon Music background playback controls"
        );

        channel.setShowBadge(false);

        manager.createNotificationChannel(
                channel
        );

        log(
                "Notification channel created"
        );
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private Notification buildNotification() {

        Intent openIntent =
                new Intent(
                        this,
                        YouTubeWebActivity.class
                );

        openIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        if (currentVideoId != null &&
                !currentVideoId.isEmpty()) {

            openIntent.putExtra(
                    "video_id",
                    currentVideoId
            );

            openIntent.putExtra(
                    "video_title",
                    currentTitle
            );

            openIntent.putExtra(
                    "video_artist",
                    currentArtist
            );
        }

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this,
                        100,
                        openIntent,
                        pendingIntentFlags()
                );

        Intent previousIntent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        previousIntent.setAction(
                ACTION_PREVIOUS
        );

        PendingIntent previous =
                PendingIntent.getService(
                        this,
                        101,
                        previousIntent,
                        pendingIntentFlags()
                );

        Intent playPauseIntent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        playPauseIntent.setAction(
                isPlaying()
                        ? ACTION_PAUSE
                        : ACTION_PLAY
        );

        PendingIntent playPause =
                PendingIntent.getService(
                        this,
                        102,
                        playPauseIntent,
                        pendingIntentFlags()
                );

        Intent nextIntent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        nextIntent.setAction(
                ACTION_NEXT
        );

        PendingIntent next =
                PendingIntent.getService(
                        this,
                        103,
                        nextIntent,
                        pendingIntentFlags()
                );

        NotificationCompat.Builder builder;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            builder =
                    new NotificationCompat.Builder(
                            this,
                            CHANNEL_ID
                    );

        } else {

            builder =
                    new NotificationCompat.Builder(
                            this
                    );
        }

        builder
                .setSmallIcon(
                        android.R.drawable.ic_media_play
                )
                .setContentTitle(
                        currentTitle
                )
                .setContentText(
                        currentArtist
                )
                .setContentIntent(
                        contentIntent
                )
                .setOngoing(
                        isPlaying() || loading
                )
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                )
                .addAction(
                        android.R.drawable.ic_media_previous,
                        "Previous",
                        previous
                )
                .addAction(
                        isPlaying()
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        isPlaying()
                                ? "Pause"
                                : "Play",
                        playPause
                )
                .addAction(
                        android.R.drawable.ic_media_next,
                        "Next",
                        next
                );

        if (mediaSession != null) {

            builder.setStyle(
                    new MediaStyle()
                            .setMediaSession(
                                    mediaSession.getSessionToken()
                            )
                            .setShowActionsInCompactView(
                                    0,
                                    1,
                                    2
                            )
            );
        }

        return builder.build();
    }

    // =========================================================
    // UPDATE NOTIFICATION
    // =========================================================

    private void updateNotification() {

        try {

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );

            if (manager != null) {

                manager.notify(
                        NOTIFICATION_ID,
                        buildNotification()
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Notification update FAILED",
                    e
            );
        }
    }

    // =========================================================
    // PENDING INTENT FLAGS
    // =========================================================

    private int pendingIntentFlags() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            return PendingIntent.FLAG_UPDATE_CURRENT |
                    PendingIntent.FLAG_IMMUTABLE;

        } else {

            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    // =========================================================
    // TASK REMOVED
    // =========================================================

    @Override
    public void onTaskRemoved(
            Intent rootIntent) {

        log(
                "================================================"
        );

        log(
                "TASK REMOVED"
        );

        log(
                "Playback service intentionally remains alive"
        );

        super.onTaskRemoved(
                rootIntent
        );
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        Log.w(
                TAG,
                "================================================"
        );

        Log.w(
                TAG,
                "!!!!!!!! SERVICE DESTROYED !!!!!!!!"
        );

        Log.w(
                TAG,
                "Title = " +
                        currentTitle
        );

        Log.w(
                TAG,
                "Video ID = " +
                        currentVideoId
        );

        Log.w(
                TAG,
                "Playing = " +
                        isPlaying()
        );

        Log.w(
                TAG,
                "Prepared = " +
                        prepared
        );

        Log.w(
                TAG,
                "Loading = " +
                        loading
        );

        if (mediaSession != null) {

            mediaSession.setActive(false);

            mediaSession.release();

            mediaSession = null;
        }

        if (player != null) {

            try {

                player.release();

            } catch (Exception e) {

                error(
                        "ExoPlayer.release() failed",
                        e
                );
            }

            player = null;
        }

        playlistQueue.clear();

        currentIndex = -1;

        queueEnabled = false;

        instance = null;

        super.onDestroy();
    }

    // =========================================================
    // BINDER
    // =========================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }

    // =========================================================
    // PLAYBACK STATE NAME
    // =========================================================

    private String playbackStateName(
            int state) {

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

    // =========================================================
    // LOG HELPERS
    // =========================================================

    private void log(
            String message) {

        Log.d(
                TAG,
                message
        );

        try {

            LemonDebug.log(
                    "Playback",
                    message
            );

        } catch (Exception ignored) {
        }
    }

    private void error(
            String message,
            Exception exception) {

        Log.e(
                TAG,
                message,
                exception
        );

        try {

            LemonDebug.error(
                    "Playback",
                    message,
                    exception
            );

        } catch (Exception ignored) {
        }
    }
}