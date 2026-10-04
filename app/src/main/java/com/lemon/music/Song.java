package com.lemon.music;

public class Song {

    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final long duration;
    private final String uri;

    public Song(long id, String title, String artist, String album,
                long duration, String uri) {

        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public String getUri() {
        return uri;
    }
}