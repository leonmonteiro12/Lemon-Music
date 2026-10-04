package com.lemon.music;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class YouTubeApi {

    private final Context context;

    private static final String YOUTUBE_PREFS =
            "lemon_music_youtube";

    private static final String ACCESS_TOKEN =
            "access_token";

    private static final String YOUTUBE_CONNECTED =
            "youtube_connected";

    public YouTubeApi(Context context) {
        this.context =
                context.getApplicationContext();
    }

    // ==================================================
    // ACCESS TOKEN
    // ==================================================

    private String getSavedAccessToken() {

        return context
                .getSharedPreferences(
                        YOUTUBE_PREFS,
                        Context.MODE_PRIVATE
                )
                .getString(
                        ACCESS_TOKEN,
                        ""
                );
    }

    // ==================================================
    // YOUTUBE CONNECTION
    // ==================================================

    public boolean isYouTubeConnected() {

        String token =
                getSavedAccessToken();

        boolean connected =
                context
                        .getSharedPreferences(
                                YOUTUBE_PREFS,
                                Context.MODE_PRIVATE
                        )
                        .getBoolean(
                                YOUTUBE_CONNECTED,
                                false
                        );

        return connected
                && token != null
                && !token.trim().isEmpty();
    }

    // ==================================================
    // REQUIRE OAUTH
    // ==================================================

    private String requireAccessToken()
            throws Exception {

        String token =
                getSavedAccessToken();

        if (token == null ||
                token.trim().isEmpty()) {

            throw new Exception(
                    "YouTube account is not connected. " +
                    "Connect your Google account in Settings first."
            );
        }

        return token.trim();
    }

    // ==================================================
    // SEARCH
    // ==================================================

    public String search(
            String query
    ) throws Exception {

        if (query == null ||
                query.trim().isEmpty()) {

            throw new Exception(
                    "Search query is empty."
            );
        }

        String token =
                requireAccessToken();

        String encodedQuery =
                URLEncoder.encode(
                        query.trim(),
                        "UTF-8"
                );

        String urlString =
                "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet"
                        + "&type=video"
                        + "&maxResults=20"
                        + "&q="
                        + encodedQuery;

        return makeRequest(
                urlString,
                token
        );
    }

    // ==================================================
    // MY PLAYLISTS
    // ==================================================

    public String getMyPlaylists()
            throws Exception {

        String token =
                requireAccessToken();

        String urlString =
                "https://www.googleapis.com/youtube/v3/playlists"
                        + "?part=snippet,contentDetails"
                        + "&mine=true"
                        + "&maxResults=50";

        return makeRequest(
                urlString,
                token
        );
    }

    // ==================================================
    // PLAYLIST ITEMS
    // ==================================================

    public String getPlaylistItems(
            String playlistId
    ) throws Exception {

        if (playlistId == null ||
                playlistId.trim().isEmpty()) {

            throw new Exception(
                    "Invalid playlist ID."
            );
        }

        String token =
                requireAccessToken();

        String encodedId =
                URLEncoder.encode(
                        playlistId.trim(),
                        "UTF-8"
                );

        String urlString =
                "https://www.googleapis.com/youtube/v3/playlistItems"
                        + "?part=snippet,contentDetails"
                        + "&playlistId="
                        + encodedId
                        + "&maxResults=50";

        return makeRequest(
                urlString,
                token
        );
    }

    // ==================================================
    // COMPATIBILITY
    // ==================================================

    public String getPlaylistVideos(
            String playlistId
    ) throws Exception {

        return getPlaylistItems(
                playlistId
        );
    }

    // ==================================================
    // VIDEO DETAILS
    // ==================================================

    public String getVideo(
            String videoId
    ) throws Exception {

        if (videoId == null ||
                videoId.trim().isEmpty()) {

            throw new Exception(
                    "Invalid video ID."
            );
        }

        String token =
                requireAccessToken();

        String encodedId =
                URLEncoder.encode(
                        videoId.trim(),
                        "UTF-8"
                );

        String urlString =
                "https://www.googleapis.com/youtube/v3/videos"
                        + "?part=snippet,contentDetails"
                        + "&id="
                        + encodedId;

        return makeRequest(
                urlString,
                token
        );
    }

    // ==================================================
    // HTTP REQUEST
    // ==================================================

    private String makeRequest(
            String urlString,
            String accessToken
    ) throws Exception {

        HttpURLConnection connection = null;
        InputStream stream = null;
        BufferedReader reader = null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setUseCaches(
                    false
            );

            connection.setDoInput(
                    true
            );

            // ==================================================
            // OAUTH
            // ==================================================

            if (accessToken == null ||
                    accessToken.trim().isEmpty()) {

                throw new Exception(
                        "YouTube account is not connected."
                );
            }

            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + accessToken.trim()
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.connect();

            int responseCode =
                    connection.getResponseCode();

            if (responseCode >= 200 &&
                    responseCode < 300) {

                stream =
                        connection.getInputStream();

            } else {

                stream =
                        connection.getErrorStream();
            }

            if (stream == null) {

                throw new Exception(
                        "YouTube returned HTTP "
                                + responseCode
                );
            }

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    stream,
                                    "UTF-8"
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            String response =
                    result.toString();

            if (responseCode < 200 ||
                    responseCode >= 300) {

                if (responseCode == 401) {

                    throw new Exception(
                            "YouTube authorization expired or was rejected. " +
                            "Please reconnect your Google account."
                    );
                }

                if (responseCode == 403) {

                    throw new Exception(
                            "YouTube denied this request. " +
                            response
                    );
                }

                throw new Exception(
                        "YouTube API error "
                                + responseCode
                                + ": "
                                + response
                );
            }

            return response;

        } finally {

            try {

                if (reader != null) {
                    reader.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (stream != null) {
                    stream.close();
                }

            } catch (Exception ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ==================================================
    // SEARCH HELPERS
    // ==================================================

    public static JSONArray getSearchItems(
            String json
    ) throws Exception {

        return new JSONObject(json)
                .optJSONArray("items");
    }

    public static String getVideoId(
            JSONObject item
    ) throws Exception {

        if (item == null) {
            return "";
        }

        JSONObject id =
                item.optJSONObject("id");

        if (id == null) {
            return "";
        }

        return id.optString(
                "videoId",
                ""
        );
    }

    public static String getTitle(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "Unknown title";
        }

        return snippet.optString(
                "title",
                "Unknown title"
        );
    }

    public static String getChannel(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "Unknown channel";
        }

        return snippet.optString(
                "channelTitle",
                "Unknown channel"
        );
    }

    public static String getThumbnail(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "";
        }

        return getThumbnailFromSnippet(
                snippet
        );
    }

    // ==================================================
    // PLAYLIST HELPERS
    // ==================================================

    public static JSONArray parsePlaylists(
            String json
    ) throws Exception {

        return new JSONObject(json)
                .optJSONArray("items");
    }

    public static String getPlaylistId(
            JSONObject item
    ) throws Exception {

        if (item == null) {
            return "";
        }

        return item.optString(
                "id",
                ""
        );
    }

    public static String getPlaylistTitle(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "Untitled playlist";
        }

        return snippet.optString(
                "title",
                "Untitled playlist"
        );
    }

    public static String getPlaylistDescription(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "";
        }

        return snippet.optString(
                "description",
                ""
        );
    }

    public static String getPlaylistThumbnail(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject("snippet");

        if (snippet == null) {
            return "";
        }

        return getThumbnailFromSnippet(
                snippet
        );
    }

    public static int getPlaylistVideoCount(
            JSONObject item
    ) throws Exception {

        JSONObject contentDetails =
                item.optJSONObject(
                        "contentDetails"
                );

        if (contentDetails == null) {
            return 0;
        }

        return contentDetails.optInt(
                "itemCount",
                0
        );
    }

    // ==================================================
    // PLAYLIST VIDEO HELPERS
    // ==================================================

    public static JSONArray parsePlaylistItems(
            String json
    ) throws Exception {

        return new JSONObject(json)
                .optJSONArray("items");
    }

    public static String getPlaylistVideoId(
            JSONObject item
    ) throws Exception {

        if (item == null) {
            return "";
        }

        JSONObject contentDetails =
                item.optJSONObject(
                        "contentDetails"
                );

        if (contentDetails == null) {
            return "";
        }

        return contentDetails.optString(
                "videoId",
                ""
        );
    }

    public static String getPlaylistItemTitle(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject(
                        "snippet"
                );

        if (snippet == null) {
            return "Unknown title";
        }

        return snippet.optString(
                "title",
                "Unknown title"
        );
    }

    public static String getPlaylistItemChannel(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject(
                        "snippet"
                );

        if (snippet == null) {
            return "";
        }

        return snippet.optString(
                "videoOwnerChannelTitle",
                snippet.optString(
                        "channelTitle",
                        ""
                )
        );
    }

    public static String getPlaylistItemThumbnail(
            JSONObject item
    ) throws Exception {

        JSONObject snippet =
                item.optJSONObject(
                        "snippet"
                );

        if (snippet == null) {
            return "";
        }

        return getThumbnailFromSnippet(
                snippet
        );
    }

    // ==================================================
    // THUMBNAILS
    // ==================================================

    private static String getThumbnailFromSnippet(
            JSONObject snippet
    ) {

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
}