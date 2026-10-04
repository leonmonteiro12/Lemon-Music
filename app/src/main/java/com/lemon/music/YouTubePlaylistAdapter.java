package com.lemon.music;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class YouTubePlaylistAdapter
        extends RecyclerView.Adapter<YouTubePlaylistAdapter.PlaylistViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(String playlist);
    }

    private final List<String> playlists;
    private final OnPlaylistClickListener listener;

    public YouTubePlaylistAdapter(
            List<String> playlists,
            OnPlaylistClickListener listener) {

        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_youtube_playlist,
                                parent,
                                false
                        );

        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull PlaylistViewHolder holder,
            int position) {

        String playlist =
                playlists.get(position);

        holder.playlistName.setText(playlist);

        holder.itemView.setOnClickListener(
                v -> listener.onPlaylistClick(playlist)
        );
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder
            extends RecyclerView.ViewHolder {

        TextView playlistName;

        PlaylistViewHolder(
                @NonNull View itemView) {

            super(itemView);

            playlistName =
                    itemView.findViewById(
                            R.id.youtubePlaylistName
                    );
        }
    }
}