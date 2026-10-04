package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val api_key: String,
    val userAgent: String,
    val referer: String? = null,
    val clientId: String
) {

    fun toContext(
        locale: YouTubeLocale,
        visitorData: String?
    ) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData
        )
    )

    companion object {

        private const val REFERER_YOUTUBE_MUSIC =
            "https://music.youtube.com/"

        private const val USER_AGENT_WEB =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36"

        private const val USER_AGENT_ANDROID =
            "com.google.android.youtube/21.26.364 " +
            "(Linux; U; Android 11) gzip"

        private const val USER_AGENT_ANDROID_MUSIC =
            "com.google.android.apps.youtube.music/7.27.52 " +
            "(Linux; U; Android 11) gzip"

        /*
         * -------------------------------------------------------------
         * ANDROID MUSIC
         * -------------------------------------------------------------
         */
        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "7.27.52",
            api_key = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = USER_AGENT_ANDROID_MUSIC,
            clientId = "67"
        )

        /*
         * -------------------------------------------------------------
         * ANDROID
         * -------------------------------------------------------------
         */
        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "21.26.364",
            api_key = "AIzaSyA8eiZmM1FaDVjRydf2KTyQ_vz_yYM39w",
            userAgent = USER_AGENT_ANDROID,
            clientId = "3"
        )

        /*
         * -------------------------------------------------------------
         * PROPER TVHTML5
         * -------------------------------------------------------------
         *
         * This is the normal TV client.
         *
         * Client ID: 7
         * Client version: 7.20260707.07.00
         *
         * Used by YouTube.kt as the next extraction route when
         * ANDROID does not provide usable audio streams.
         */
        val TV = YouTubeClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260707.07.00",
            api_key = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8",
            userAgent =
                "Mozilla/5.0 (ChromiumStylePlatform) " +
                "Cobalt/25.lts.30.1034943-gold " +
                "(unlike Gecko), Unknown_TV_Unknown_0/Unknown " +
                "(Unknown, Unknown)",
            clientId = "7"
        )

        /*
         * -------------------------------------------------------------
         * TVHTML5 SIMPLY EMBEDDED
         * -------------------------------------------------------------
         *
         * Existing fallback. Kept intact.
         */
        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            api_key = "AIzaSyDCU8hByM-4DrUqRUYn-Gn-3llEO78bcxq8",
            userAgent =
                "Mozilla/5.0 (PlayStation 4 5.55) " +
                "AppleWebKit/601.2 (KHTML, like Gecko)",
            clientId = "85"
        )

        /*
         * -------------------------------------------------------------
         * WEB
         * -------------------------------------------------------------
         */
        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260114.08.00",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3",
            userAgent = USER_AGENT_WEB,
            clientId = "1"
        )

        /*
         * -------------------------------------------------------------
         * WEB REMIX / YOUTUBE MUSIC
         * -------------------------------------------------------------
         */
        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260114.03.00",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC,
            clientId = "67"
        )
    }
}