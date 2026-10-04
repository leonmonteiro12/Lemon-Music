package com.lemon.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceAudioActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST = 1001;

    private RecyclerView songsRecyclerView;
    private TextView emptyText;
    private Button refreshButton;

    private MusicRepository musicRepository;

    private final List<Song> songs =
            new ArrayList<>();

    private SongAdapter adapter;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_device_audio
        );

        // ==================================================
        // BACK BUTTON
        // ==================================================

        ImageButton backButton =
                findViewById(
                        R.id.deviceAudioBackButton
                );

        backButton.setOnClickListener(
                v -> finish()
        );

        // ==================================================
        // VIEWS
        // ==================================================

        songsRecyclerView =
                findViewById(
                        R.id.deviceSongsRecyclerView
                );

        emptyText =
                findViewById(
                        R.id.deviceEmptyText
                );

        refreshButton =
                findViewById(
                        R.id.deviceRefreshButton
                );

        // ==================================================
        // MUSIC REPOSITORY
        // ==================================================

        musicRepository =
                new MusicRepository(this);

        // ==================================================
        // RECYCLER VIEW
        // ==================================================

        songsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new SongAdapter(
                        songs,
                        this::playSong
                );

        songsRecyclerView.setAdapter(
                adapter
        );

        // ==================================================
        // REFRESH
        // ==================================================

        refreshButton.setOnClickListener(
                v -> checkPermissionAndLoad()
        );

        // ==================================================
        // INITIAL LOAD
        // ==================================================

        checkPermissionAndLoad();
    }

    // ==================================================
    // PERMISSION
    // ==================================================

    private void checkPermissionAndLoad() {

        String permission;

        if (Build.VERSION.SDK_INT >= 33) {

            permission =
                    Manifest.permission.READ_MEDIA_AUDIO;

        } else if (Build.VERSION.SDK_INT >= 23) {

            permission =
                    Manifest.permission.READ_EXTERNAL_STORAGE;

        } else {

            loadSongs();

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                permission
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            permission
                    },
                    STORAGE_PERMISSION_REQUEST
            );

            return;
        }

        loadSongs();
    }

    // ==================================================
    // LOAD DEVICE SONGS
    // ==================================================

    private void loadSongs() {

        songs.clear();

        songs.addAll(
                musicRepository.getSongs()
        );

        adapter.notifyDataSetChanged();

        updateEmptyState();

        Toast.makeText(
                this,
                songs.size() +
                        " device songs found 🎵",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ==================================================
    // EMPTY STATE
    // ==================================================

    private void updateEmptyState() {

        if (songs.isEmpty()) {

            emptyText.setVisibility(
                    TextView.VISIBLE
            );

            songsRecyclerView.setVisibility(
                    RecyclerView.GONE
            );

        } else {

            emptyText.setVisibility(
                    TextView.GONE
            );

            songsRecyclerView.setVisibility(
                    RecyclerView.VISIBLE
            );
        }
    }

    // ==================================================
    // PLAY SONG
    // ==================================================

    private void playSong(
            Song song
    ) {

        if (song == null) {

            Toast.makeText(
                    this,
                    "Invalid song.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uri =
                song.getUri();

        if (uri == null ||
                uri.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "This song has no playable audio URI.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // ==================================================
        // START LEMON MUSIC BACKGROUND PLAYBACK SERVICE
        // ==================================================

        Intent playbackIntent =
                new Intent(
                        this,
                        MusicPlaybackService.class
                );

        playbackIntent.setAction(
                MusicPlaybackService.ACTION_PLAY
        );

        playbackIntent.putExtra(
                MusicPlaybackService.EXTRA_SONG_URI,
                uri
        );

        playbackIntent.putExtra(
                MusicPlaybackService.EXTRA_SONG_TITLE,
                song.getTitle()
        );

        playbackIntent.putExtra(
                MusicPlaybackService.EXTRA_SONG_ARTIST,
                song.getArtist()
        );

        /*
         * Android 8+ requires a foreground-service
         * start for background media playback.
         *
         * The service itself immediately promotes
         * itself to the foreground.
         */

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            startForegroundService(
                    playbackIntent
            );

        } else {

            startService(
                    playbackIntent
            );
        }

        // ==================================================
        // OPEN NOW PLAYING
        // ==================================================

        Intent nowPlayingIntent =
                new Intent(
                        DeviceAudioActivity.this,
                        Nowplayingactivity.class
                );

        nowPlayingIntent.putExtra(
                "song_id",
                song.getId()
        );

        nowPlayingIntent.putExtra(
                "song_title",
                song.getTitle()
        );

        nowPlayingIntent.putExtra(
                "song_artist",
                song.getArtist()
        );

        nowPlayingIntent.putExtra(
                "song_album",
                song.getAlbum()
        );

        nowPlayingIntent.putExtra(
                "song_duration",
                song.getDuration()
        );

        nowPlayingIntent.putExtra(
                "song_uri",
                song.getUri()
        );

        startActivity(
                nowPlayingIntent
        );
    }

    // ==================================================
    // PERMISSION RESULT
    // ==================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                STORAGE_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                loadSongs();

            } else {

                Toast.makeText(
                        this,
                        "Music permission is required to show device audio.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}