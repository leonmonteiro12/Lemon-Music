package com.lemon.music;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YouTubePlaylistSongsActivity
        extends AppCompatActivity {

    private RecyclerView songsRecyclerView;

    private TextView titleText;

    private TextView emptyText;

    private final List<YouTubeResult> songs =
            new ArrayList<>();

    private YouTubeResultAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private String playlistId = "";

    private String playlistName =
            "Playlist";

    private volatile boolean loading =
            false;

    // ==================================================
    // CREATE
    // ==================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_youtube_playlist_songs
        );

        ImageButton backButton =
                findViewById(
                        R.id.youtubePlaylistSongsBackButton
                );

        titleText =
                findViewById(
                        R.id.youtubePlaylistSongsTitle
                );

        emptyText =
                findViewById(
                        R.id.youtubePlaylistSongsEmptyText
                );

        songsRecyclerView =
                findViewById(
                        R.id.youtubePlaylistSongsRecyclerView
                );

        if (backButton != null) {

            backButton.setOnClickListener(
                    v -> finish()
            );
        }

        readPlaylistIntent();

        if (titleText != null) {

            titleText.setText(
                    "📁 " + playlistName
            );
        }

        if (songsRecyclerView != null) {

            songsRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this)
            );
        }

        adapter =
                new YouTubeResultAdapter(
                        songs,
                        this::playSong
                );

        if (songsRecyclerView != null) {

            songsRecyclerView.setAdapter(
                    adapter
            );
        }

        loadPlaylistSongs();
    }

    // ==================================================
    // READ INTENT
    // ==================================================

    private void readPlaylistIntent() {

        Intent intent =
                getIntent();

        if (intent == null) {
            return;
        }

        String receivedId =
                intent.getStringExtra(
                        "playlist_id"
                );

        if (receivedId != null &&
                !receivedId.trim().isEmpty()) {

            playlistId =
                    receivedId.trim();
        }

        String receivedName =
                intent.getStringExtra(
                        "playlist_name"
                );

        if (receivedName != null &&
                !receivedName.trim().isEmpty()) {

            playlistName =
                    receivedName.trim();
        }

        LemonDebug.log(
                "YouTubePlaylistSongs",
                "Playlist ID = "
                        + playlistId
        );

        LemonDebug.log(
                "YouTubePlaylistSongs",
                "Playlist Name = "
                        + playlistName
        );
    }

    // ==================================================
    // LOAD SONGS
    // ==================================================

    private void loadPlaylistSongs() {

        if (loading) {
            return;
        }

        if (playlistId == null ||
                playlistId.trim().isEmpty()) {

            showError(
                    "This playlist has no valid ID."
            );

            return;
        }

        loading = true;

        if (emptyText != null) {

            emptyText.setText(
                    "Loading playlist... 🍋"
            );

            emptyText.setVisibility(
                    View.VISIBLE
            );
        }

        if (songsRecyclerView != null) {

            songsRecyclerView.setVisibility(
                    View.GONE
            );
        }

        executor.execute(() -> {

            try {

                YouTubeApi api =
                        new YouTubeApi(this);

                String response =
                        api.getPlaylistVideos(
                                playlistId
                        );

                if (response == null ||
                        response.trim().isEmpty()) {

                    throw new Exception(
                            "Empty playlist response."
                    );
                }

                JSONObject root =
                        new JSONObject(response);

                JSONArray items =
                        root.optJSONArray(
                                "items"
                        );

                final List<YouTubeResult>
                        loadedSongs =
                        new ArrayList<>();

                if (items != null) {

                    for (
                            int i = 0;
                            i < items.length();
                            i++
                    ) {

                        JSONObject item =
                                items.optJSONObject(i);

                        if (item == null) {
                            continue;
                        }

                        JSONObject snippet =
                                item.optJSONObject(
                                        "snippet"
                                );

                        if (snippet == null) {
                            continue;
                        }

                        String videoId =
                                extractVideoId(
                                        snippet,
                                        item
                                );

                        if (videoId.isEmpty()) {
                            continue;
                        }

                        String title =
                                snippet.optString(
                                        "title",
                                        "Unknown song"
                                );

                        if (title == null ||
                                title.trim().isEmpty()) {

                            title =
                                    "Unknown song";
                        }

                        String channel =
                                snippet.optString(
                                        "videoOwnerChannelTitle",
                                        ""
                                );

                        if (channel == null ||
                                channel.trim().isEmpty()) {

                            channel =
                                    snippet.optString(
                                            "channelTitle",
                                            ""
                                    );
                        }

                        if (channel == null ||
                                channel.trim().isEmpty()) {

                            channel =
                                    "Unknown artist";
                        }

                        String thumbnail =
                                getThumbnail(
                                        snippet
                                );

                        loadedSongs.add(
                                new YouTubeResult(
                                        videoId,
                                        title.trim(),
                                        channel.trim(),
                                        thumbnail
                                )
                        );
                    }
                }

                runOnUiThread(() -> {

                    loading = false;

                    songs.clear();

                    songs.addAll(
                            loadedSongs
                    );

                    if (adapter != null) {

                        adapter.notifyDataSetChanged();
                    }

                    updateEmptyState();
                });

            } catch (Exception e) {

                e.printStackTrace();

                LemonDebug.log(
                        "YouTubePlaylistSongs",
                        "PLAYLIST ERROR = "
                                + safeErrorMessage(e)
                );

                runOnUiThread(() -> {

                    loading = false;

                    songs.clear();

                    if (adapter != null) {

                        adapter.notifyDataSetChanged();
                    }

                    showError(
                            "Couldn't load playlist songs."
                    );

                    Toast.makeText(
                            YouTubePlaylistSongsActivity.this,
                            "Playlist error: "
                                    + safeErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    // ==================================================
    // VIDEO ID
    // ==================================================

    private String extractVideoId(
            JSONObject snippet,
            JSONObject item
    ) {

        String videoId = "";

        JSONObject resourceId =
                snippet.optJSONObject(
                        "resourceId"
                );

        if (resourceId != null) {

            videoId =
                    resourceId.optString(
                            "videoId",
                            ""
                    );
        }

        if (videoId == null ||
                videoId.trim().isEmpty()) {

            JSONObject contentDetails =
                    item.optJSONObject(
                            "contentDetails"
                    );

            if (contentDetails != null) {

                videoId =
                        contentDetails.optString(
                                "videoId",
                                ""
                        );
            }
        }

        if (videoId == null ||
                videoId.trim().isEmpty()) {

            Object idObject =
                    item.opt("id");

            if (idObject instanceof String) {

                videoId =
                        (String) idObject;
            }
        }

        if (videoId == null) {
            return "";
        }

        return videoId.trim();
    }

    // ==================================================
    // PLAY SONG + BUILD QUEUE
    // ==================================================

    private void playSong(
            YouTubeResult result
    ) {

        if (result == null ||
                result.videoId == null ||
                result.videoId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid YouTube video.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String videoId =
                result.videoId.trim();

        String title =
                result.title;

        if (title == null ||
                title.trim().isEmpty()) {

            title =
                    "Unknown song";
        }

        String artist =
                result.channel;

        if (artist == null ||
                artist.trim().isEmpty()) {

            artist =
                    "Unknown artist";
        }

        String thumbnail =
                result.thumbnail;

        if (thumbnail == null) {
            thumbnail = "";
        }

        // =====================================================
        // BUILD COMPLETE PLAYLIST QUEUE
        // =====================================================

        ArrayList<LemonPlaybackService.QueueItem>
                playbackQueue =
                new ArrayList<>();

        int selectedIndex = -1;

        for (
                int i = 0;
                i < songs.size();
                i++
        ) {

            YouTubeResult song =
                    songs.get(i);

            if (song == null ||
                    song.videoId == null ||
                    song.videoId.trim().isEmpty()) {

                continue;
            }

            String songId =
                    song.videoId.trim();

            String songTitle =
                    song.title;

            if (songTitle == null ||
                    songTitle.trim().isEmpty()) {

                songTitle =
                        "Unknown song";
            }

            String songArtist =
                    song.channel;

            if (songArtist == null ||
                    songArtist.trim().isEmpty()) {

                songArtist =
                        "Unknown artist";
            }

            playbackQueue.add(
                    new LemonPlaybackService.QueueItem(
                            songId,
                            songTitle.trim(),
                            songArtist.trim()
                    )
            );

            if (songId.equals(videoId)) {

                selectedIndex =
                        playbackQueue.size() - 1;
            }
        }

        if (playbackQueue.isEmpty()) {

            Toast.makeText(
                    this,
                    "No playable songs in this playlist.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (selectedIndex < 0) {

            selectedIndex = 0;
        }

        /*
         * IMPORTANT:
         *
         * selectedIndex is modified above, so it cannot
         * directly be captured by the delayed lambda.
         *
         * Make a final copy for the lambda.
         */

        final int queueStartIndex =
                selectedIndex;

        // =====================================================
        // SEND QUEUE TO PLAYBACK SERVICE
        // =====================================================

        LemonPlaybackService service =
                LemonPlaybackService.getInstance();

        if (service != null) {

            service.setPlaylistQueue(
                    playbackQueue,
                    selectedIndex
            );

            LemonDebug.log(
                    "PlaylistQueue",
                    "QUEUE SENT TO SERVICE"
            );

            LemonDebug.log(
                    "PlaylistQueue",
                    "Queue size = "
                            + playbackQueue.size()
            );

            LemonDebug.log(
                    "PlaylistQueue",
                    "Starting index = "
                            + selectedIndex
            );

        } else {

            /*
             * Service does not exist yet.
             * Start it first.
             */

            Intent serviceIntent =
                    new Intent(
                            this,
                            LemonPlaybackService.class
                    );

            serviceIntent.setAction(
                    LemonPlaybackService.ACTION_PLAY
            );

            try {

                if (android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.O) {

                    startForegroundService(
                            serviceIntent
                    );

                } else {

                    startService(
                            serviceIntent
                    );
                }

            } catch (Exception e) {

                LemonDebug.error(
                        "PlaylistQueue",
                        "Could not start playback service",
                        e
                );
            }

            /*
             * Wait for service creation, then send queue.
             */

            new Handler(
                    Looper.getMainLooper()
            ).postDelayed(
                    () -> {

                        LemonPlaybackService
                                retryService =
                                LemonPlaybackService
                                        .getInstance();

                        if (retryService != null) {

                            retryService.setPlaylistQueue(
                                    playbackQueue,
                                    queueStartIndex
                            );

                            LemonDebug.log(
                                    "PlaylistQueue",
                                    "QUEUE SENT AFTER SERVICE START"
                            );

                        } else {

                            LemonDebug.log(
                                    "PlaylistQueue",
                                    "SERVICE STILL NOT AVAILABLE"
                            );
                        }

                    },
                    150
            );
        }

        // =====================================================
        // OPEN PLAYER
        // =====================================================

        Intent intent =
                new Intent(
                        this,
                        YouTubeWebActivity.class
                );

        intent.putExtra(
                "youtube_video_id",
                videoId
        );

        intent.putExtra(
                "video_id",
                videoId
        );

        intent.putExtra(
                "video_title",
                title.trim()
        );

        intent.putExtra(
                "video_artist",
                artist.trim()
        );

        intent.putExtra(
                "youtube_thumbnail",
                thumbnail
        );

        intent.putExtra(
                "playlist_id",
                playlistId
        );

        intent.putExtra(
                "playlist_name",
                playlistName
        );

        startActivity(intent);
    }

    // ==================================================
    // THUMBNAIL
    // ==================================================

    private String getThumbnail(
            JSONObject snippet
    ) {

        if (snippet == null) {
            return "";
        }

        JSONObject thumbnails =
                snippet.optJSONObject(
                        "thumbnails"
                );

        if (thumbnails == null) {
            return "";
        }

        String[] qualities = {
                "high",
                "medium",
                "standard",
                "default"
        };

        for (String quality : qualities) {

            JSONObject thumbnail =
                    thumbnails.optJSONObject(
                            quality
                    );

            if (thumbnail != null) {

                String url =
                        thumbnail.optString(
                                "url",
                                ""
                        );

                if (!url.isEmpty()) {

                    return url;
                }
            }
        }

        return "";
    }

    // ==================================================
    // EMPTY STATE
    // ==================================================

    private void updateEmptyState() {

        if (songs.isEmpty()) {

            if (emptyText != null) {

                emptyText.setText(
                        "This playlist has no playable videos."
                );

                emptyText.setVisibility(
                        View.VISIBLE
                );
            }

            if (songsRecyclerView != null) {

                songsRecyclerView.setVisibility(
                        View.GONE
                );
            }

        } else {

            if (emptyText != null) {

                emptyText.setVisibility(
                        View.GONE
                );
            }

            if (songsRecyclerView != null) {

                songsRecyclerView.setVisibility(
                        View.VISIBLE
                );
            }
        }
    }

    // ==================================================
    // ERROR
    // ==================================================

    private void showError(
            String message
    ) {

        if (emptyText != null) {

            emptyText.setText(
                    message
            );

            emptyText.setVisibility(
                    View.VISIBLE
            );
        }

        if (songsRecyclerView != null) {

            songsRecyclerView.setVisibility(
                    View.GONE
            );
        }
    }

    // ==================================================
    // SAFE ERROR
    // ==================================================

    private String safeErrorMessage(
            Exception e
    ) {

        if (e == null) {

            return "Unknown error.";
        }

        String message =
                e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    // ==================================================
    // CLEANUP
    // ==================================================

    @Override
    protected void onDestroy() {

        loading = false;

        executor.shutdownNow();

        super.onDestroy();
    }
}