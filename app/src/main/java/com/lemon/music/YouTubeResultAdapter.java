package com.lemon.music;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class YouTubeResultAdapter
        extends RecyclerView.Adapter<
                YouTubeResultAdapter.ViewHolder> {

    public interface OnResultClickListener {
        void onResultClick(
                YouTubeResult result
        );
    }


    private final List<YouTubeResult> results;

    private final OnResultClickListener listener;


    public YouTubeResultAdapter(
            List<YouTubeResult> results,
            OnResultClickListener listener
    ) {

        this.results = results;
        this.listener = listener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view =
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.item_youtube_result,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        YouTubeResult result =
                results.get(position);


        holder.title.setText(
                result.title
        );


        holder.channel.setText(
                result.channel
        );


        holder.thumbnail.setImageResource(
                android.R.drawable.ic_media_play
        );


        holder.itemView.setOnClickListener(
                v -> listener.onResultClick(
                        result
                )
        );


        if (result.thumbnail != null &&
                !result.thumbnail.trim().isEmpty()) {

            new ThumbnailTask(
                    holder.thumbnail,
                    result.thumbnail
            ).execute();
        }
    }


    @Override
    public int getItemCount() {
        return results.size();
    }


    // ==================================================
    // VIEW HOLDER
    // ==================================================

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView thumbnail;

        TextView title;

        TextView channel;


        public ViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);


            thumbnail =
                    itemView.findViewById(
                            R.id.thumbnail
                    );


            title =
                    itemView.findViewById(
                            R.id.title
                    );


            channel =
                    itemView.findViewById(
                            R.id.channel
                    );
        }
    }


    // ==================================================
    // THUMBNAIL LOADER
    // ==================================================

    private static class ThumbnailTask
            extends AsyncTask<Void, Void, Bitmap> {

        private final ImageView imageView;

        private final String imageUrl;


        ThumbnailTask(
                ImageView imageView,
                String imageUrl
        ) {

            this.imageView = imageView;

            this.imageUrl = imageUrl;
        }


        @Override
        protected Bitmap doInBackground(
                Void... voids
        ) {

            HttpURLConnection connection =
                    null;

            InputStream inputStream =
                    null;


            try {

                URL url =
                        new URL(imageUrl);


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setConnectTimeout(
                        10000
                );


                connection.setReadTimeout(
                        10000
                );


                connection.setDoInput(
                        true
                );


                connection.connect();


                if (connection.getResponseCode()
                        != HttpURLConnection.HTTP_OK) {

                    return null;
                }


                inputStream =
                        connection.getInputStream();


                return BitmapFactory.decodeStream(
                        inputStream
                );


            } catch (Exception e) {

                return null;


            } finally {

                try {

                    if (inputStream != null) {
                        inputStream.close();
                    }

                } catch (Exception ignored) {
                }


                if (connection != null) {
                    connection.disconnect();
                }
            }
        }


        @Override
        protected void onPostExecute(
                Bitmap bitmap
        ) {

            if (bitmap != null) {

                imageView.setImageBitmap(
                        bitmap
                );
            }
        }
    }
}