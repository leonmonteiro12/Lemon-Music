package com.lemon.music;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView greetingText;

    private TextView miniPlayerTitle;

    private TextView miniPlayerArtist;

    private ImageButton miniPlayPause;

    private LinearLayout miniPlayer;

    private PlayerManager playerManager;

    /*
     * ONE dynamic container.
     *
     * MainActivity creates up to 50 recommendation
     * cards inside this container.
     */
    private LinearLayout recommendationsContainer;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final Handler miniPlayerHandler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService recommendationExecutor =
            Executors.newSingleThreadExecutor();

    private boolean miniPlayerSyncRunning = false;

    private static final long MINI_PLAYER_SYNC_INTERVAL = 500L;

    private static final int NOTIFICATION_PERMISSION_REQUEST = 9001;

    private boolean notificationPromptShown = false;

    private static final int MAX_RECOMMENDATIONS = 50;

    private final Runnable miniPlayerSyncRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!miniPlayerSyncRunning) {
                        return;
                    }

                    updateMiniPlayer();

                    miniPlayerHandler.postDelayed(
                            this,
                            MINI_PLAYER_SYNC_INTERVAL
                    );
                }
            };

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );

        playerManager =
                PlayerManager.getInstance(this);

        ImageButton settingsButton =
                findViewById(
                        R.id.settingsButton
                );

        TextView searchPlaceholder =
                findViewById(
                        R.id.searchPlaceholder
                );

        LinearLayout searchContainer =
                findViewById(
                        R.id.searchContainer
                );

        greetingText =
                findViewById(
                        R.id.greetingText
                );

        LinearLayout quickYouTube =
                findViewById(
                        R.id.quickYouTube
                );

        LinearLayout quickDevice =
                findViewById(
                        R.id.quickDevice
                );

        LinearLayout quickLibrary =
                findViewById(
                        R.id.quickLibrary
                );

        LinearLayout yourPlaylistsCard =
                findViewById(
                        R.id.yourPlaylistsCard
                );

        LinearLayout deviceMusicCard =
                findViewById(
                        R.id.deviceMusicCard
                );

        miniPlayer =
                findViewById(
                        R.id.miniPlayer
                );

        miniPlayerTitle =
                findViewById(
                        R.id.miniPlayerTitle
                );

        miniPlayPause =
                findViewById(
                        R.id.miniPlayPause
                );

        miniPlayerArtist =
                findOptionalTextView(
                        "miniPlayerArtist"
                );

        recommendationsContainer =
                findViewById(
                        R.id.recommendationsContainer
                );

        TextView homeButton =
                findViewById(
                        R.id.homeButton
                );

        TextView youtubeMusicButton =
                findViewById(
                        R.id.youtubeMusicButton
                );

        TextView playlistsButton =
                findViewById(
                        R.id.playlistsButton
                );

        updateGreeting();

        if (settingsButton != null) {

            settingsButton.setOnClickListener(
                    v -> openSettings()
            );
        }

        if (searchContainer != null) {

            searchContainer.setOnClickListener(
                    v -> openYouTubeMusic()
            );
        }

        if (searchPlaceholder != null) {

            searchPlaceholder.setOnClickListener(
                    v -> openYouTubeMusic()
            );
        }

        if (quickYouTube != null) {

            quickYouTube.setOnClickListener(
                    v -> openYouTubeMusic()
            );
        }

        if (quickDevice != null) {

            quickDevice.setOnClickListener(
                    v -> openDeviceLibrary()
            );
        }

        if (quickLibrary != null) {

            quickLibrary.setOnClickListener(
                    v -> openLibrary()
            );
        }

        if (yourPlaylistsCard != null) {

            yourPlaylistsCard.setOnClickListener(
                    v -> openLibrary()
            );
        }

        if (deviceMusicCard != null) {

            deviceMusicCard.setOnClickListener(
                    v -> openDeviceLibrary()
            );
        }

        if (miniPlayer != null) {

            miniPlayer.setOnClickListener(
                    v -> openNowPlaying()
            );
        }

        if (miniPlayPause != null) {

            miniPlayPause.setOnClickListener(
                    v -> togglePlayback()
            );
        }

        if (homeButton != null) {

            homeButton.setOnClickListener(
                    v -> {

                        View homeScroll =
                                findViewById(
                                        R.id.homeScroll
                                );

                        if (homeScroll != null) {

                            homeScroll.scrollTo(
                                    0,
                                    0
                            );
                        }
                    }
            );
        }

        if (youtubeMusicButton != null) {

            youtubeMusicButton.setOnClickListener(
                    v -> openYouTubeMusic()
            );
        }

        if (playlistsButton != null) {

            playlistsButton.setOnClickListener(
                    v -> openLibrary()
            );
        }

        updateMiniPlayer();

        requestNotificationPermission();

        /*
         * Recommended for You
         *
         * Uses up to 50 real videos from the
         * user's FIRST YouTube playlist.
         */
        loadRecommendedVideos();
    }

    @Override
    protected void onStart() {

        super.onStart();

        YouTubeAuthManager authManager =
                new YouTubeAuthManager(this);

        authManager.refreshAuthorization();
    }

    // =========================================================
    // RECOMMENDED VIDEOS
    // =========================================================

    private void loadRecommendedVideos() {

        if (recommendationsContainer == null) {
            return;
        }

        clearRecommendationCards();

        recommendationExecutor.execute(
                () -> {

                    try {

                        YouTubeApi api =
                                new YouTubeApi(
                                        MainActivity.this
                                );

                        if (!api.isYouTubeConnected()) {

                            return;
                        }

                        String playlistsJson =
                                api.getMyPlaylists();

                        JSONArray playlists =
                                YouTubeApi.parsePlaylists(
                                        playlistsJson
                                );

                        if (playlists == null ||
                                playlists.length() == 0) {

                            return;
                        }

                        /*
                         * FIRST PLAYLIST
                         */
                        JSONObject firstPlaylist =
                                playlists.getJSONObject(0);

                        String playlistId =
                                YouTubeApi.getPlaylistId(
                                        firstPlaylist
                                );

                        if (playlistId == null ||
                                playlistId.trim().isEmpty()) {

                            return;
                        }

                        String playlistItemsJson =
                                api.getPlaylistItems(
                                        playlistId
                                );

                        JSONArray items =
                                YouTubeApi.parsePlaylistItems(
                                        playlistItemsJson
                                );

                        if (items == null ||
                                items.length() == 0) {

                            return;
                        }

                        List<JSONObject> videos =
                                new ArrayList<>();

                        for (int i = 0;
                             i < items.length();
                             i++) {

                            JSONObject item =
                                    items.getJSONObject(i);

                            String videoId =
                                    YouTubeApi.getPlaylistVideoId(
                                            item
                                    );

                            if (videoId != null &&
                                    !videoId.trim().isEmpty()) {

                                videos.add(item);
                            }
                        }

                        if (videos.isEmpty()) {
                            return;
                        }

                        /*
                         * Shuffle so Home does not always
                         * show the same videos first.
                         */
                        Collections.shuffle(videos);

                        /*
                         * Take up to 50 videos.
                         */
                        int count =
                                Math.min(
                                        MAX_RECOMMENDATIONS,
                                        videos.size()
                                );

                        List<JSONObject> selected =
                                new ArrayList<>(
                                        videos.subList(
                                                0,
                                                count
                                        )
                                );

                        mainHandler.post(
                                () -> {

                                    if (isFinishing() ||
                                            isDestroyed()) {

                                        return;
                                    }

                                    populateAllRecommendations(
                                            selected
                                    );
                                }
                        );

                    } catch (Exception e) {

                        LemonDebug.error(
                                "Recommendations",
                                "Could not load first playlist recommendations",
                                e
                        );
                    }
                }
        );
    }

    private void populateAllRecommendations(
            List<JSONObject> videos
    ) {

        if (recommendationsContainer == null ||
                videos == null) {

            return;
        }

        recommendationsContainer.removeAllViews();

        int count =
                Math.min(
                        MAX_RECOMMENDATIONS,
                        videos.size()
                );

        for (int i = 0;
             i < count;
             i++) {

            JSONObject item =
                    videos.get(i);

            LinearLayout card =
                    createRecommendationCard(
                            item
                    );

            if (card != null) {

                recommendationsContainer.addView(
                        card
                );
            }
        }
    }

    private void clearRecommendationCards() {

        if (recommendationsContainer != null) {

            recommendationsContainer.removeAllViews();
        }
    }

    private LinearLayout createRecommendationCard(
            JSONObject item
    ) {

        if (item == null) {
            return null;
        }

        try {

            String videoId =
                    YouTubeApi.getPlaylistVideoId(
                            item
                    );

            String title =
                    YouTubeApi.getPlaylistItemTitle(
                            item
                    );

            String artist =
                    YouTubeApi.getPlaylistItemChannel(
                            item
                    );

            String thumbnail =
                    YouTubeApi.getPlaylistItemThumbnail(
                            item
                    );

            if (videoId == null ||
                    videoId.trim().isEmpty()) {

                return null;
            }

            /*
             * OUTER CARD
             */
            LinearLayout card =
                    new LinearLayout(this);

            card.setOrientation(
                    LinearLayout.VERTICAL
            );

            card.setGravity(
                    Gravity.TOP
            );

            card.setClickable(true);
            card.setFocusable(true);

            card.setBackgroundColor(
                    Color.rgb(
                            36,
                            36,
                            36
                    )
            );

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            dp(145),
                            dp(175)
                    );

            cardParams.setMargins(
                    0,
                    0,
                    dp(10),
                    0
            );

            card.setLayoutParams(
                    cardParams
            );

            /*
             * THUMBNAIL
             */
            ImageView imageView =
                    new ImageView(this);

            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            imageView.setBackgroundColor(
                    Color.rgb(
                            48,
                            48,
                            48
                    )
            );

            LinearLayout.LayoutParams imageParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(100)
                    );

            imageView.setLayoutParams(
                    imageParams
            );

            card.addView(
                    imageView
            );

            /*
             * TITLE
             */
            TextView titleView =
                    new TextView(this);

            titleView.setText(
                    title == null ||
                            title.trim().isEmpty()
                            ? "Unknown title"
                            : title
            );

            titleView.setTextColor(
                    Color.WHITE
            );

            titleView.setTextSize(
                    13
            );

            titleView.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );

            titleView.setMaxLines(
                    2
            );

            titleView.setEllipsize(
                    TextUtils.TruncateAt.END
            );

            titleView.setPadding(
                    dp(7),
                    dp(6),
                    dp(7),
                    0
            );

            card.addView(
                    titleView,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            );

            /*
             * CHANNEL
             */
            TextView artistView =
                    new TextView(this);

            artistView.setText(
                    artist == null ||
                            artist.trim().isEmpty()
                            ? "YouTube"
                            : artist
            );

            artistView.setTextColor(
                    Color.rgb(
                            170,
                            170,
                            170
                    )
            );

            artistView.setTextSize(
                    11
            );

            artistView.setMaxLines(
                    1
            );

            artistView.setEllipsize(
                    TextUtils.TruncateAt.END
            );

            artistView.setPadding(
                    dp(7),
                    dp(2),
                    dp(7),
                    dp(5)
            );

            card.addView(
                    artistView,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            );

            /*
             * CLICK → EXISTING PLAYBACK PIPELINE
             */
            card.setOnClickListener(
                    v -> openRecommendedVideo(
                            videoId,
                            title,
                            artist
                    )
            );

            /*
             * Thumbnail is loaded separately so
             * the UI thread is never blocked.
             */
            if (thumbnail != null &&
                    !thumbnail.trim().isEmpty()) {

                loadThumbnail(
                        thumbnail,
                        imageView
                );
            }

            return card;

        } catch (Exception e) {

            LemonDebug.error(
                    "Recommendations",
                    "Could not create recommendation card",
                    e
            );

            return null;
        }
    }

    private void openRecommendedVideo(
            String videoId,
            String title,
            String artist
    ) {

        if (videoId == null ||
                videoId.trim().isEmpty()) {

            return;
        }

        Intent intent =
                new Intent(
                        MainActivity.this,
                        YouTubeWebActivity.class
                );

        /*
         * EXACT extras expected by the current
         * YouTubeWebActivity.
         */
        intent.putExtra(
                "video_id",
                videoId
        );

        intent.putExtra(
                "video_title",
                title == null ||
                        title.trim().isEmpty()
                        ? "YouTube Music"
                        : title
        );

        intent.putExtra(
                "video_artist",
                artist == null ||
                        artist.trim().isEmpty()
                        ? "YouTube"
                        : artist
        );

        startActivity(
                intent
        );
    }

    private void loadThumbnail(String imageUrl, ImageView imageView) {
    RecommendationImageLoader.load(imageUrl, imageView);
}
    // =========================================================
    // OPTIONAL TEXT VIEW
    // =========================================================

    private TextView findOptionalTextView(
            String idName
    ) {

        try {

            int id =
                    getResources().getIdentifier(
                            idName,
                            "id",
                            getPackageName()
                    );

            if (id == 0) {
                return null;
            }

            View view =
                    findViewById(id);

            if (view instanceof TextView) {

                return (TextView) view;
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    // =========================================================
    // DP
    // =========================================================

    private int dp(
            int value
    ) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // =========================================================
    // GREETING
    // =========================================================

    private void updateGreeting() {

        if (greetingText == null) {
            return;
        }

        Calendar calendar =
                Calendar.getInstance();

        int hour =
                calendar.get(
                        Calendar.HOUR_OF_DAY
                );

        String greeting;

        if (hour >= 5 &&
                hour < 12) {

            greeting =
                    "Good morning ☀️";

        } else if (hour >= 12 &&
                hour < 18) {

            greeting =
                    "Good afternoon 🌤️";

        } else {

            greeting =
                    "Good evening 🌙";
        }

        greetingText.setText(
                greeting + " 🍋"
        );
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private void openSettings() {

        startActivity(
                new Intent(
                        this,
                        SettingsActivity.class
                )
        );
    }

    private void openYouTubeMusic() {

        startActivity(
                new Intent(
                        this,
                        YouTubeMusicActivity.class
                )
        );
    }

    private void openDeviceLibrary() {

        startActivity(
                new Intent(
                        this,
                        YouTubeLibraryActivity.class
                )
        );
    }

    private void openLibrary() {

        startActivity(
                new Intent(
                        this,
                        YouTubeLibraryActivity.class
                )
        );
    }

    private void openNowPlaying() {

        startActivity(
                new Intent(
                        this,
                        Nowplayingactivity.class
                )
        );
    }
// =========================================================
// PLAYBACK / MINI PLAYER
// =========================================================

private static final String PLAYBACK_PREFS =
        "lemon_music_playback_state";

private static final String PLAYBACK_TITLE =
        "title";

private static final String PLAYBACK_ARTIST =
        "artist";

private void togglePlayback() {

    LemonPlaybackService playbackService =
            LemonPlaybackService.getInstance();

    /*
     * YouTube / Media3 playback
     */
    if (playbackService != null) {

        if (playbackService.isPlaying()) {

            playbackService.pause();

        } else {

            playbackService.play();
        }

        updateMiniPlayer();

        return;
    }

    /*
     * Local playback fallback
     */
    if (playerManager == null) {
        return;
    }

    if (!playerManager.hasCurrentPlayback()) {
        return;
    }

    if (playerManager.isPlaying()) {

        playerManager.pause();

    } else {

        playerManager.resume();
    }

    updateMiniPlayer();
}
private void sendMiniPlayerAction(
        String action
) {

    try {

        Intent intent =
                new Intent(
                        this,
                        LemonPlaybackService.class
                );

        intent.setAction(
                action
        );

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(
                    intent
            );

        } else {

            startService(
                    intent
            );
        }

    } catch (Exception e) {

        LemonDebug.error(
                "MINI_PLAYER",
                "Could not send playback action: "
                        + action,
                e
        );
    }
}

private void updateMiniPlayer() {

    /*
     * =====================================================
     * FIRST: CHECK THE REAL YOUTUBE PLAYBACK SERVICE
     * =====================================================
     */

    LemonPlaybackService playbackService =
            LemonPlaybackService.getInstance();

    if (playbackService != null) {

        String title =
                playbackService.getCurrentTitle();

        String artist =
                playbackService.getCurrentArtist();

        boolean playing =
                playbackService.isPlaying();

        boolean loading =
                playbackService.isLoading();

        boolean hasYouTubePlayback =
                (
                        title != null &&
                        !title.trim().isEmpty() &&
                        !title.equals("YouTube Music")
                )
                ||
                playbackService.getCurrentVideoId() != null &&
                !playbackService
                        .getCurrentVideoId()
                        .trim()
                        .isEmpty();

        if (hasYouTubePlayback || loading) {

            if (title == null ||
                    title.trim().isEmpty()) {

                title = "YouTube Music";
            }

            if (artist == null ||
                    artist.trim().isEmpty()) {

                artist = "YouTube";
            }

            if (miniPlayerTitle != null) {

                miniPlayerTitle.setText(
                        title
                );
            }

            if (miniPlayerArtist != null) {

                miniPlayerArtist.setText(
                        artist
                );
            }

            if (miniPlayPause != null) {

                miniPlayPause.setEnabled(true);

                miniPlayPause.setImageResource(
                        playing
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play
                );
            }

            return;
        }
    }

    /*
     * =====================================================
     * FALLBACK: NORMAL LOCAL PLAYER
     * =====================================================
     */

    if (playerManager == null) {
        return;
    }

    boolean hasPlayback =
            playerManager.hasCurrentPlayback();

    if (!hasPlayback) {

        if (miniPlayerTitle != null) {

            miniPlayerTitle.setText(
                    "Nothing playing"
            );
        }

        if (miniPlayerArtist != null) {

            miniPlayerArtist.setText(
                    "Start listening"
            );
        }

        if (miniPlayPause != null) {

            miniPlayPause.setImageResource(
                    android.R.drawable.ic_media_play
            );

            miniPlayPause.setEnabled(false);
        }

        return;
    }

    String title =
            playerManager.getCurrentTitle();

    String artist =
            playerManager.getCurrentArtist();

    if (title == null ||
            title.trim().isEmpty()) {

        title = "Unknown song";
    }

    if (artist == null ||
            artist.trim().isEmpty()) {

        artist = "Unknown artist";
    }

    if (miniPlayerTitle != null) {

        miniPlayerTitle.setText(title);
    }

    if (miniPlayerArtist != null) {

        miniPlayerArtist.setText(artist);
    }

    if (miniPlayPause != null) {

        miniPlayPause.setEnabled(true);

        miniPlayPause.setImageResource(
                playerManager.isPlaying()
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );
    }
}
    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    private void requestNotificationPermission() {

        if (notificationPromptShown) {
            return;
        }

        notificationPromptShown =
                true;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission
                                        .POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST
                );
            }

            return;
        }

        if (!NotificationManagerCompat
                .from(this)
                .areNotificationsEnabled()) {

            showNotificationSettingsDialog();
        }
    }

    private void showNotificationSettingsDialog() {

        new AlertDialog.Builder(this)

                .setTitle(
                        "🔔 Enable Lemon Music notifications"
                )

                .setMessage(
                        "Notifications are used for "
                                + "background playback, playback "
                                + "controls, and showing the "
                                + "currently playing song."
                )

                .setPositiveButton(
                        "ENABLE",
                        (dialog, which) ->
                                openNotificationSettings()
                )

                .setNegativeButton(
                        "LATER",
                        null
                )

                .setCancelable(true)

                .show();
    }

    private void openNotificationSettings() {

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                Intent intent =
                        new Intent(
                                Settings
                                        .ACTION_APP_NOTIFICATION_SETTINGS
                        );

                intent.putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        getPackageName()
                );

                startActivity(intent);

                return;
            }

            Intent intent =
                    new Intent(
                            Settings
                                    .ACTION_APPLICATION_DETAILS_SETTINGS
                    );

            intent.setData(
                    Uri.parse(
                            "package:"
                                    + getPackageName()
                    )
            );

            startActivity(intent);

        } catch (Exception ignored) {

            try {

                Intent fallback =
                        new Intent(
                                Settings
                                        .ACTION_APPLICATION_DETAILS_SETTINGS
                        );

                fallback.setData(
                        Uri.parse(
                                "package:"
                                        + getPackageName()
                        )
                );

                startActivity(
                        fallback
                );

            } catch (Exception ignoredAgain) {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                NOTIFICATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                android.widget.Toast.makeText(
                        this,
                        "Notifications enabled 🍋🔔",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

            } else {

                android.widget.Toast.makeText(
                        this,
                        "Notifications disabled. "
                                + "Playback controls may not appear.",
                        android.widget.Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        updateGreeting();

        startMiniPlayerSync();

        updateMiniPlayer();

        if (notificationPromptShown &&
                NotificationManagerCompat
                        .from(this)
                        .areNotificationsEnabled()) {

            notificationPromptShown =
                    false;
        }
    }
    private void startMiniPlayerSync() {

    if (miniPlayerSyncRunning) {
        return;
    }

    miniPlayerSyncRunning = true;

    miniPlayerHandler.removeCallbacks(
            miniPlayerSyncRunnable
    );

    miniPlayerHandler.post(
            miniPlayerSyncRunnable
    );
}

private void stopMiniPlayerSync() {

    miniPlayerSyncRunning = false;

    miniPlayerHandler.removeCallbacks(
            miniPlayerSyncRunnable
    );
}

    @Override
    protected void onPause() {

        stopMiniPlayerSync();

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        stopMiniPlayerSync();

        recommendationExecutor.shutdownNow();

        super.onDestroy();
    }
}