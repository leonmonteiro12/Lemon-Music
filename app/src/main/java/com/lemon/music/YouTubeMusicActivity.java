package com.lemon.music;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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

public class YouTubeMusicActivity extends AppCompatActivity {

    private EditText searchInput;
    private ImageButton searchButton;
    private ImageButton backButton;
    private Button floatingPlayerButton;

    private RecyclerView resultsRecyclerView;
    private TextView emptyText;

    private final List<YouTubeResult> results =
            new ArrayList<>();

    private YouTubeResultAdapter adapter;

    private YouTubeApi youtubeApi;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_youtube_music
        );

        backButton =
                findViewById(R.id.backButton);

        searchInput =
                findViewById(R.id.youtubeSearchInput);

        searchButton =
                findViewById(R.id.youtubeSearchButton);

        floatingPlayerButton =
                findViewById(
                        R.id.floatingPlayerButton
                );

        resultsRecyclerView =
                findViewById(
                        R.id.youtubeResultsRecyclerView
                );

        emptyText =
                findViewById(
                        R.id.youtubeEmptyText
                );

        youtubeApi =
                new YouTubeApi(this);

        resultsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new YouTubeResultAdapter(
                        results,
                        this::playResult
                );

        resultsRecyclerView.setAdapter(
                adapter
        );

        // ==================================================
        // BACK
        // ==================================================

        backButton.setOnClickListener(
                v -> finish()
        );

        // ==================================================
        // FLOATING PLAYER
        // ==================================================

        floatingPlayerButton.setOnClickListener(
                v -> openFloatingPlayer()
        );

        // ==================================================
        // SEARCH
        // ==================================================

        searchButton.setOnClickListener(
                v -> performSearch()
        );

        searchInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            EditorInfo.IME_ACTION_SEARCH
                            ||
                            actionId ==
                                    EditorInfo.IME_ACTION_DONE) {

                        performSearch();

                        return true;
                    }

                    return false;
                }
        );
    }

    // ==================================================
    // FLOATING PLAYER
    // ==================================================

    private void openFloatingPlayer() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) {

            if (!Settings.canDrawOverlays(this)) {

                Toast.makeText(
                        this,
                        "Allow Lemon Music to display over other apps 🍋",
                        Toast.LENGTH_LONG
                ).show();

                try {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            );

                    intent.setData(
                            Uri.parse(
                                    "package:"
                                            + getPackageName()
                            )
                    );

                    startActivity(intent);

                } catch (Exception e) {

                    try {

                        Intent intent =
                                new Intent(
                                        Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
                                );

                        startActivity(intent);

                    } catch (Exception ignored) {
                    }
                }

                return;
            }
        }

        try {

            Intent serviceIntent =
                    new Intent(
                            this,
                            FloatingPlayerService.class
                    );

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                startForegroundService(
                        serviceIntent
                );

            } else {

                startService(
                        serviceIntent
                );
            }

            Toast.makeText(
                    this,
                    "Floating player opened 🍋",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Couldn't open floating player: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ==================================================
    // SEARCH
    // ==================================================

    private void performSearch() {

        String query =
                searchInput
                        .getText()
                        .toString()
                        .trim();

        if (query.isEmpty()) {

            Toast.makeText(
                    this,
                    "Type something to search 🍋",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        emptyText.setText(
                "Searching YouTube... 🍋"
        );

        emptyText.setVisibility(
                View.VISIBLE
        );

        searchButton.setEnabled(
                false
        );

        executor.execute(() -> {

            try {

                String response =
                        youtubeApi.search(query);

                JSONArray items =
                        YouTubeApi.getSearchItems(
                                response
                        );

                List<YouTubeResult> newResults =
                        new ArrayList<>();

                if (items != null) {

                    for (
                            int i = 0;
                            i < items.length();
                            i++
                    ) {

                        JSONObject item =
                                items.getJSONObject(i);

                        String videoId =
                                YouTubeApi.getVideoId(
                                        item
                                );

                        if (videoId == null ||
                                videoId.trim().isEmpty()) {

                            continue;
                        }

                        newResults.add(
                                new YouTubeResult(
                                        videoId,
                                        YouTubeApi.getTitle(item),
                                        YouTubeApi.getChannel(item),
                                        YouTubeApi.getThumbnail(item)
                                )
                        );
                    }
                }

                runOnUiThread(() -> {

                    results.clear();

                    results.addAll(
                            newResults
                    );

                    adapter.notifyDataSetChanged();

                    searchButton.setEnabled(
                            true
                    );

                    if (results.isEmpty()) {

                        emptyText.setText(
                                "No videos found."
                        );

                        emptyText.setVisibility(
                                View.VISIBLE
                        );

                    } else {

                        emptyText.setVisibility(
                                View.GONE
                        );
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    searchButton.setEnabled(
                            true
                    );

                    emptyText.setText(
                            "Search failed."
                    );

                    emptyText.setVisibility(
                            View.VISIBLE
                    );

                    Toast.makeText(
                            YouTubeMusicActivity.this,
                            "Search failed: "
                                    + safeErrorMessage(e),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    // ==================================================
    // PLAY RESULT
    // ==================================================

    private void playResult(
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

        String artist =
                result.channel;

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
                        title != null
                                ? title
                                : "YouTube Music"
                )
                .putString(
                        "video_artist",
                        artist != null
                                ? artist
                                : "YouTube"
                )
                .apply();

        Intent intent =
                new Intent(
                        this,
                        YouTubeWebActivity.class
                );

        intent.putExtra(
                "video_id",
                videoId
        );

        intent.putExtra(
                "video_title",
                title != null
                        ? title
                        : "YouTube Music"
        );

        intent.putExtra(
                "video_artist",
                artist != null
                        ? artist
                        : "YouTube"
        );

        startActivity(intent);
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

        executor.shutdownNow();

        super.onDestroy();
    }
}