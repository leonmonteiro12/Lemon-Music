package com.zionhuang.innertube

import android.util.Log
import com.zionhuang.innertube.models.response.PlayerResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object InnerTuneBridge {

    private const val TAG = "InnerTune"

    interface Callback {
        fun onSuccess(
            audioUrl: String?,
            title: String?,
            artist: String?
        )

        fun onError(message: String)
    }

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    @JvmStatic
    fun extract(
        videoId: String,
        callback: Callback
    ) {
        if (callback == null) {
            return
        }

        val cleanVideoId =
            videoId.trim()

        if (cleanVideoId.isEmpty()) {
            callback.onError(
                "Video ID is empty."
            )
            return
        }

        scope.launch {

            try {

                Log.e(
                    TAG,
                    "================================"
                )

                Log.e(
                    TAG,
                    "INNER TUNE EXTRACTION START"
                )

                Log.e(
                    TAG,
                    "Video ID = $cleanVideoId"
                )

                /*
                 * IMPORTANT:
                 *
                 * Do NOT manually rotate through
                 * Android / Web / TV clients here.
                 *
                 * YouTube.player() now contains the
                 * original InnerTune 0.5.10 strategy:
                 *
                 * ANDROID_MUSIC
                 *       ↓
                 * TVHTML5
                 *       ↓
                 * Piped audio streams
                 */

                val result =
                    YouTube
                        .player(
                            cleanVideoId,
                            null
                        )

                val response =
                    result.getOrElse { error ->

                        Log.e(
                            TAG,
                            "YouTube.player() failed",
                            error
                        )

                        callback.onError(
                            error.message
                                ?: "Player extraction failed."
                        )

                        return@launch
                    }

                val status =
                    response
                        .playabilityStatus
                        .status

                val reason =
                    response
                        .playabilityStatus
                        .reason

                Log.e(
                    TAG,
                    "Final playability = $status"
                )

                if (status != "OK") {

                    callback.onError(
                        "Playability failed: " +
                            (
                                reason
                                    ?: status
                            )
                    )

                    return@launch
                }

                val streamingData =
                    response.streamingData

                if (streamingData == null) {

                    callback.onError(
                        "No streaming data was returned."
                    )

                    return@launch
                }

                Log.e(
                    TAG,
                    "Adaptive formats = " +
                        streamingData
                            .adaptiveFormats
                            .size
                )

                val audioFormat =
                    streamingData
                        .adaptiveFormats
                        .asSequence()
                        .filter {
                            it.isAudio
                        }
                        .filter {
                            !it.url.isNullOrBlank()
                        }
                        .maxByOrNull {
                            it.bitrate
                        }

                if (audioFormat == null) {

                    callback.onError(
                        "No playable audio stream was found."
                    )

                    return@launch
                }

                val audioUrl =
                    audioFormat.url

                if (audioUrl.isNullOrBlank()) {

                    callback.onError(
                        "The audio stream URL was empty."
                    )

                    return@launch
                }

                val title =
                    response
                        .videoDetails
                        ?.title
                        ?: "Unknown"

                val artist =
                    response
                        .videoDetails
                        ?.author
                        ?: "Unknown"

                Log.e(
                    TAG,
                    "================================"
                )

                Log.e(
                    TAG,
                    "AUDIO EXTRACTION SUCCESS"
                )

                Log.e(
                    TAG,
                    "Mime = ${audioFormat.mimeType}"
                )

                Log.e(
                    TAG,
                    "Bitrate = ${audioFormat.bitrate}"
                )

                Log.e(
                    TAG,
                    "Title = $title"
                )

                Log.e(
                    TAG,
                    "Artist = $artist"
                )

                Log.e(
                    TAG,
                    "================================"
                )

                callback.onSuccess(
                    audioUrl,
                    title,
                    artist
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "INNER TUNE EXTRACTION EXCEPTION",
                    e
                )

                callback.onError(
                    e.message
                        ?: "Player extraction failed."
                )
            }
        }
    }
}