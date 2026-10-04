package com.lemon.music;

public class YouTubeResult {

    public String videoId;
    public String title;
    public String channel;
    public String thumbnail;

    public YouTubeResult(
            String videoId,
            String title,
            String channel,
            String thumbnail
    ) {
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.thumbnail = thumbnail;
    }
}