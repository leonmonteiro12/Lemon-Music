package com.lemon.music;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class PlayerManager {

    private static volatile PlayerManager instance;

    private final Context context;

    private volatile Song currentSong;

    // ==================================================
    // YOUTUBE WEB PLAYER
    // ==================================================

    private volatile YouTubeController youtubeController;

    private volatile boolean youtubePlaying = false;
    private volatile boolean youtubePlaybackActive = false;

    private volatile String youtubeTitle = "";
    private volatile String youtubeArtist = "";
    private volatile String youtubeVideoId = "";

    // ==================================================
    // CONTROLLER
    // ==================================================

    public interface YouTubeController {

        void play();

        void pause();

        void resume();

        void stop();

        void seekTo(int position);

        boolean isPlaying();

        int getCurrentPosition();

        int getDuration();
    }

    // ==================================================
    // CONSTRUCTOR
    // ==================================================

    private PlayerManager(Context context) {

        this.context = context.getApplicationContext();
    }

    // ==================================================
    // INSTANCE
    // ==================================================

    public static synchronized PlayerManager getInstance(Context context) {

        if (instance == null) {

            instance = new PlayerManager(context);
        }

        return instance;
    }

    // ==================================================
    // NORMAL MUSIC PLAY
    // ==================================================

    public synchronized void play(Song song) {

        if (song == null) {
            return;
        }

        String uri = song.getUri();

        if (uri == null || uri.trim().isEmpty()) {

            return;
        }

        // YouTube playback is no longer active.
        clearYouTubePlayback();

        currentSong = song;

        Intent intent = new Intent(context, MusicPlaybackService.class);

        intent.setAction(MusicPlaybackService.ACTION_PLAY);

        intent.putExtra(MusicPlaybackService.EXTRA_SONG_URI, uri.trim());

        intent.putExtra(
                MusicPlaybackService.EXTRA_SONG_TITLE, safeString(song.getTitle(), "Unknown song"));

        intent.putExtra(
                MusicPlaybackService.EXTRA_SONG_ARTIST,
                safeString(song.getArtist(), "Unknown artist"));

        startPlaybackService(intent);
    }

    // ==================================================
    // PAUSE
    // ==================================================

    public void pause() {

        YouTubeController controller = youtubeController;

        if (youtubePlaybackActive && controller != null) {

            try {

                controller.pause();

                youtubePlaying = false;

            } catch (Exception ignored) {
            }

            return;
        }

        sendAction(MusicPlaybackService.ACTION_PAUSE);
    }

    // ==================================================
    // RESUME
    // ==================================================

    public void resume() {

        YouTubeController controller = youtubeController;

        if (youtubePlaybackActive && controller != null) {

            try {

                controller.resume();

                youtubePlaying = true;

            } catch (Exception ignored) {
            }

            return;
        }

        sendAction(MusicPlaybackService.ACTION_RESUME);
    }

    // ==================================================
    // STOP
    // ==================================================

    public void stop() {

        YouTubeController controller = youtubeController;

        if (youtubePlaybackActive && controller != null) {

            try {

                controller.stop();

            } catch (Exception ignored) {
            }

            clearYouTubePlayback();

            return;
        }

        currentSong = null;

        sendAction(MusicPlaybackService.ACTION_STOP);
    }

    // ==================================================
    // NEXT
    // ==================================================

    public void next() {

        if (youtubePlaybackActive) {
            return;
        }

        sendAction(MusicPlaybackService.ACTION_NEXT);
    }

    // ==================================================
    // PREVIOUS
    // ==================================================

    public void previous() {

        if (youtubePlaybackActive) {
            return;
        }

        sendAction(MusicPlaybackService.ACTION_PREVIOUS);
    }

    // ==================================================
    // STATE
    // ==================================================

    public boolean isPlaying() {

        if (youtubePlaybackActive) {

            YouTubeController controller = youtubeController;

            if (controller != null) {

                try {

                    youtubePlaying = controller.isPlaying();

                } catch (Exception ignored) {
                }
            }

            return youtubePlaying;
        }

        return MusicPlaybackService.isPlaying();
    }

    // ==================================================
    // POSITION
    // ==================================================

    public int getCurrentPosition() {

        if (youtubePlaybackActive) {

            YouTubeController controller = youtubeController;

            if (controller != null) {

                try {

                    return controller.getCurrentPosition();

                } catch (Exception ignored) {
                }
            }

            return 0;
        }

        return MusicPlaybackService.getCurrentPosition();
    }

    // ==================================================
    // DURATION
    // ==================================================

    public int getDuration() {

        if (youtubePlaybackActive) {

            YouTubeController controller = youtubeController;

            if (controller != null) {

                try {

                    return controller.getDuration();

                } catch (Exception ignored) {
                }
            }

            return 0;
        }

        return MusicPlaybackService.getDuration();
    }

    // ==================================================
    // CURRENT PLAYBACK
    // ==================================================

    public boolean hasCurrentPlayback() {

        if (youtubePlaybackActive) {
            return true;
        }

        return MusicPlaybackService.hasCurrentPlayback();
    }

    // ==================================================
    // TITLE
    // ==================================================

    public String getCurrentTitle() {

        if (youtubePlaybackActive) {

            return safeString(youtubeTitle, "YouTube Music");
        }

        return MusicPlaybackService.getCurrentTitle();
    }

    // ==================================================
    // ARTIST
    // ==================================================

    public String getCurrentArtist() {

        if (youtubePlaybackActive) {

            return safeString(youtubeArtist, "YouTube");
        }

        return MusicPlaybackService.getCurrentArtist();
    }

    // ==================================================
    // URI
    // ==================================================

    public String getCurrentUri() {

        if (youtubePlaybackActive) {

            if (youtubeVideoId == null || youtubeVideoId.trim().isEmpty()) {

                return "";
            }

            return "https://www.youtube.com/watch?v=" + youtubeVideoId;
        }

        return MusicPlaybackService.getCurrentUri();
    }

    // ==================================================
    // SEEK
    // ==================================================

    public void seekTo(int position) {

        if (position < 0) {
            position = 0;
        }

        if (youtubePlaybackActive) {

            YouTubeController controller = youtubeController;

            if (controller != null) {

                try {

                    controller.seekTo(position);

                } catch (Exception ignored) {
                }
            }

            return;
        }

        Intent intent = new Intent(context, MusicPlaybackService.class);

        intent.setAction(MusicPlaybackService.ACTION_SEEK_TO);

        intent.putExtra(MusicPlaybackService.EXTRA_SEEK_POSITION, position);

        startPlaybackService(intent);
    }

    // ==================================================
    // CURRENT SONG
    // ==================================================

    public synchronized Song getCurrentSong() {

        return currentSong;
    }

    public synchronized void setCurrentSong(Song song) {

        currentSong = song;
    }

    // ==================================================
    // REGISTER YOUTUBE PLAYER
    // ==================================================

    public synchronized void registerYouTubePlayer(YouTubeController controller) {

        youtubeController = controller;
    }

    // ==================================================
    // UNREGISTER YOUTUBE PLAYER
    // ==================================================

    public synchronized void unregisterYouTubePlayer(YouTubeController controller) {

        if (youtubeController == controller) {

            youtubeController = null;
        }
    }

    // ==================================================
    // START YOUTUBE PLAYBACK
    // ==================================================

    public synchronized void startYouTubePlayback(String videoId, String title, String artist) {

        // Normal music is no longer the active source.
        currentSong = null;

        youtubeVideoId = safeString(videoId, "");

        youtubeTitle = safeString(title, "YouTube Music");

        youtubeArtist = safeString(artist, "YouTube");

        youtubePlaybackActive = true;

        youtubePlaying = false;
    }

    // ==================================================
    // YOUTUBE PLAYING
    // ==================================================

    public synchronized void setYouTubePlaying(boolean playing) {

        if (!youtubePlaybackActive) {
            return;
        }

        youtubePlaying = playing;
    }

    // ==================================================
    // YOUTUBE TITLE
    // ==================================================

    public synchronized void setYouTubeTitle(String title) {

        if (title == null || title.trim().isEmpty()) {

            return;
        }

        youtubeTitle = title.trim();
    }

    // ==================================================
    // YOUTUBE ARTIST
    // ==================================================

    public synchronized void setYouTubeArtist(String artist) {

        if (artist == null || artist.trim().isEmpty()) {

            return;
        }

        youtubeArtist = artist.trim();
    }

    // ==================================================
    // YOUTUBE VIDEO ID
    // ==================================================

    public synchronized String getYouTubeVideoId() {

        return youtubeVideoId;
    }

    // ==================================================
    // CLEAR YOUTUBE PLAYBACK
    // ==================================================

    public synchronized void clearYouTubePlayback() {

        youtubePlaybackActive = false;

        youtubePlaying = false;

        youtubeVideoId = "";

        youtubeTitle = "";

        youtubeArtist = "";

        youtubeController = null;
    }

    // ==================================================
    // IS YOUTUBE PLAYBACK
    // ==================================================

    public boolean isYouTubePlayback() {

        return youtubePlaybackActive;
    }

    // ==================================================
    // NORMAL SERVICE ACTION
    // ==================================================

    private void sendAction(String action) {

        if (action == null || action.trim().isEmpty()) {

            return;
        }

        Intent intent = new Intent(context, MusicPlaybackService.class);

        intent.setAction(action);

        startPlaybackService(intent);
    }

    // ==================================================
    // START SERVICE
    // ==================================================

    private void startPlaybackService(Intent intent) {

        if (intent == null) {
            return;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(context, intent);

            } else {

                context.startService(intent);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // ==================================================
    // SAFE STRING
    // ==================================================

    private String safeString(String value, String fallback) {

        if (value == null || value.trim().isEmpty()) {

            return fallback;
        }

        return value.trim();
    }
}
