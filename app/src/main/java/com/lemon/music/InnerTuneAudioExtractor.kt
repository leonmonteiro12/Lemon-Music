package com.lemon.music

import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object InnerTuneAudioExtractor {

    interface Callback {

        fun onSuccess(
            audioUrl: String,
            extractedTitle: String?,
            extractedArtist: String?
        )

        fun onError(
            message: String
        )
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

        val cleanVideoId =
            videoId.trim()

        if (cleanVideoId.isEmpty()) {

            callback.onError(
                "Unable to play this song right now."
            )

            return
        }

        scope.launch {

            try {

                /*
                 * IMPORTANT:
                 *
                 * Do NOT use InnerTune PlayerResponse formats
                 * for audio extraction anymore.
                 *
                 * YouTube currently returns valid player data,
                 * but the returned adaptive formats may not contain
                 * directly usable URLs.
                 *
                 * NewPipeExtractor handles the complete YouTube
                 * extraction process itself.
                 */

                val audioResult =
                    NewPipeUtils.getAudioUrl(
                        cleanVideoId
                    )

                val audioUrl =
                    audioResult.getOrNull()

                if (
                    audioUrl.isNullOrBlank()
                ) {

                    callback.onError(
                        "Unable to play this song right now."
                    )

                    return@launch
                }

                /*
                 * Get metadata separately.
                 *
                 * Metadata failure must NOT prevent playback.
                 */
                var title: String? = null
                var artist: String? = null

                try {

                    val playerResult =
                        YouTube.player(
                            cleanVideoId
                        )

                    playerResult.onSuccess { playerResponse ->

                        title =
                            playerResponse
                                .videoDetails
                                ?.title

                        artist =
                            playerResponse
                                .videoDetails
                                ?.author
                    }

                } catch (_: Exception) {
                    /*
                     * Metadata is optional.
                     *
                     * The audio URL from NewPipeExtractor
                     * is already enough to start playback.
                     */
                }

                /*
                 * Send the directly playable URL to the
                 * existing Lemon Music playback system.
                 *
                 * Background playback and notification code
                 * remain completely untouched.
                 */
                callback.onSuccess(
                    audioUrl,
                    title,
                    artist
                )

            } catch (_: Exception) {

                callback.onError(
                    "Unable to play this song right now."
                )
            }
        }
    }
}