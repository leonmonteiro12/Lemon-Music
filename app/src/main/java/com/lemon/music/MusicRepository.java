package com.lemon.music;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class MusicRepository {

    private final Context context;

    public MusicRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Song> getSongs() {

        List<Song> songs = new ArrayList<>();

        ContentResolver resolver =
                context.getContentResolver();

        Uri collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.IS_MUSIC
        };

        String selection =
                MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String sortOrder =
                MediaStore.Audio.Media.TITLE +
                        " COLLATE NOCASE ASC";

        Cursor cursor = null;

        try {

            cursor = resolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
            );

            if (cursor == null) {
                return songs;
            }

            int idColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media._ID
                    );

            int titleColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                    );

            int artistColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                    );

            int albumColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM
                    );

            int durationColumn =
                    cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                    );

            while (cursor.moveToNext()) {

                long id =
                        cursor.getLong(idColumn);

                String title =
                        cursor.getString(titleColumn);

                String artist =
                        cursor.getString(artistColumn);

                String album =
                        cursor.getString(albumColumn);

                long duration =
                        cursor.getLong(durationColumn);

                Uri songUri =
                        Uri.withAppendedPath(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                String.valueOf(id)
                        );

                songs.add(
                        new Song(
                                id,
                                title,
                                artist,
                                album,
                                duration,
                                songUri.toString()
                        )
                );
            }

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return songs;
    }
}