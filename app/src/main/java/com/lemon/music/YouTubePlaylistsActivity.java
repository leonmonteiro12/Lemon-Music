package com.lemon.music;

import android.content.Intent;
import android.os.Bundle;
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

public class YouTubePlaylistsActivity
        extends AppCompatActivity {

    private RecyclerView playlistsRecyclerView;

    private TextView emptyText;

    private final List<String> playlists =
            new ArrayList<>();

    private final List<String> playlistIds =
            new ArrayList<>();

    private YouTubePlaylistAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private boolean loading = false;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_youtube_playlists
        );

        ImageButton backButton =
                findViewById(
                        R.id.youtubePlaylistsBackButton
                );

        if (backButton != null) {

            backButton.setOnClickListener(
                    v -> finish()
            );
        }

        playlistsRecyclerView =
                findViewById(
                        R.id.youtubePlaylistsRecyclerView
                );

        emptyText =
                findViewById(
                        R.id.youtubePlaylistsEmptyText
                );

        playlistsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new YouTubePlaylistAdapter(
                        playlists,
                        playlist -> {

                            int position =
                                    playlists.indexOf(
                                            playlist
                                    );

                            if (position < 0 ||
                                    position >=
                                            playlistIds.size()) {

                                Toast.makeText(
                                        this,
                                        "Couldn't open this playlist.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            String playlistId =
                                    playlistIds.get(
                                            position
                                    );

                            if (playlistId == null ||
                                    playlistId.trim().isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "This playlist has no valid ID.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            openPlaylist(
                                    playlistId,
                                    playlist
                            );
                        }
                );

        playlistsRecyclerView.setAdapter(
                adapter
        );

        loadPlaylists();
    }

    // ==================================================
    // OPEN PLAYLIST
    // ==================================================

    private void openPlaylist(
            String playlistId,
            String playlistName
    ) {

        Intent intent =
                new Intent(
                        this,
                        YouTubePlaylistSongsActivity.class
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
    // LOAD PLAYLISTS
    // ==================================================

    private void loadPlaylists() {

        if (loading) {
            return;
        }

        loading = true;

        emptyText.setText(
                "Loading your YouTube playlists... 🍋"
        );

        emptyText.setVisibility(
                View.VISIBLE
        );

        playlistsRecyclerView.setVisibility(
                View.GONE
        );

        executor.execute(() -> {

            try {

                YouTubeApi api =
                        new YouTubeApi(this);

                String response =
                        api.getMyPlaylists();

                if (response == null ||
                        response.trim().isEmpty()) {

                    throw new Exception(
                            "Empty response from YouTube."
                    );
                }

                JSONObject root =
                        new JSONObject(response);

                JSONArray items =
                        root.optJSONArray("items");

                final List<String> loadedNames =
                        new ArrayList<>();

                final List<String> loadedIds =
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

                        String playlistId =
                                item.optString(
                                        "id",
                                        ""
                                );

                        if (playlistId == null ||
                                playlistId.trim().isEmpty()) {

                            continue;
                        }

                        JSONObject snippet =
                                item.optJSONObject(
                                        "snippet"
                                );

                        String title =
                                "Untitled playlist";

                        if (snippet != null) {

                            title =
                                    snippet.optString(
                                            "title",
                                            "Untitled playlist"
                                    );
                        }

                        loadedIds.add(
                                playlistId
                        );

                        loadedNames.add(
                                title
                        );
                    }
                }

                runOnUiThread(() -> {

                    loading = false;

                    playlists.clear();

                    playlistIds.clear();

                    playlists.addAll(
                            loadedNames
                    );

                    playlistIds.addAll(
                            loadedIds
                    );

                    adapter.notifyDataSetChanged();

                    updateEmptyState();
                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> {

                    loading = false;

                    playlists.clear();

                    playlistIds.clear();

                    adapter.notifyDataSetChanged();

                    emptyText.setText(
                            "Couldn't load your YouTube playlists."
                    );

                    emptyText.setVisibility(
                            View.VISIBLE
                    );

                    playlistsRecyclerView.setVisibility(
                            View.GONE
                    );

                    Toast.makeText(
                            YouTubePlaylistsActivity.this,
                            "Playlist error: "
                                    + safeErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    // ==================================================
    // EMPTY STATE
    // ==================================================

    private void updateEmptyState() {

        if (playlists.isEmpty()) {

            emptyText.setText(
                    "No YouTube playlists found."
            );

            emptyText.setVisibility(
                    View.VISIBLE
            );

            playlistsRecyclerView.setVisibility(
                    View.GONE
            );

        } else {

            emptyText.setVisibility(
                    View.GONE
            );

            playlistsRecyclerView.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ==================================================
    // ERROR
    // ==================================================

    private String safeErrorMessage(
            Exception e
    ) {

        if (e == null ||
                e.getMessage() == null ||
                e.getMessage().trim().isEmpty()) {

            return "Unknown error.";
        }

        return e.getMessage();
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