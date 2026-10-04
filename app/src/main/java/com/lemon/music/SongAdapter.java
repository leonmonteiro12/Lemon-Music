package com.lemon.music;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class SongAdapter
        extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    private final List<Song> songs;
    private final OnSongClickListener listener;

    public SongAdapter(
            List<Song> songs,
            OnSongClickListener listener
    ) {

        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_song,
                        parent,
                        false
                );

        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SongViewHolder holder,
            int position
    ) {

        Song song =
                songs.get(position);

        holder.songTitle.setText(
                song.getTitle()
        );

        holder.songArtist.setText(
                song.getArtist()
        );

        holder.songDuration.setText(
                formatDuration(
                        song.getDuration()
                )
        );

        holder.itemView.setOnClickListener(
                v -> {

                    if (listener != null) {

                        listener.onSongClick(song);
                    }
                }
        );
    }

    @Override
    public int getItemCount() {

        return songs == null
                ? 0
                : songs.size();
    }

    private String formatDuration(
            long milliseconds
    ) {

        if (milliseconds <= 0) {
            return "0:00";
        }

        long totalSeconds =
                milliseconds / 1000;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutes,
                seconds
        );
    }

    // ==================================================
    // VIEW HOLDER
    // ==================================================

    static class SongViewHolder
            extends RecyclerView.ViewHolder {

        final TextView songTitle;
        final TextView songArtist;
        final TextView songDuration;

        SongViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            songTitle =
                    itemView.findViewById(
                            R.id.songTitle
                    );

            songArtist =
                    itemView.findViewById(
                            R.id.songArtist
                    );

            songDuration =
                    itemView.findViewById(
                            R.id.songDuration
                    );
        }
    }
}