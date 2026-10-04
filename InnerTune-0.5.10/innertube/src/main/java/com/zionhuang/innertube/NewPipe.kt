package com.zionhuang.innertube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

private class NewPipeDownloaderImpl : Downloader() {

    private val client =
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()

    @Throws(
        IOException::class,
        ReCaptchaException::class
    )
    override fun execute(
        request: Request
    ): Response {

        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder =
            okhttp3.Request.Builder()
                .method(
                    httpMethod,
                    dataToSend?.toRequestBody()
                )
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
                        "Chrome/131.0.0.0 Safari/537.36"
                )

        headers.forEach { (headerName, headerValues) ->

            if (headerValues.size > 1) {

                requestBuilder.removeHeader(headerName)

                headerValues.forEach { headerValue ->
                    requestBuilder.addHeader(
                        headerName,
                        headerValue
                    )
                }

            } else if (headerValues.size == 1) {

                requestBuilder.header(
                    headerName,
                    headerValues[0]
                )
            }
        }

        val response =
            client
                .newCall(
                    requestBuilder.build()
                )
                .execute()

        if (response.code == 429) {

            response.close()

            throw ReCaptchaException(
                "reCaptcha Challenge requested",
                url
            )
        }

        val responseBody =
            response.body?.string()

        val latestUrl =
            response.request.url.toString()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            latestUrl
        )
    }
}

object NewPipeUtils {

    private var initialized = false

    private fun ensureInitialized() {

        if (initialized) {
            return
        }

        synchronized(this) {

            if (!initialized) {

                NewPipe.init(
                    NewPipeDownloaderImpl()
                )

                initialized = true
            }
        }
    }

    /**
     * Java-facing extraction method.
     *
     * IMPORTANT:
     * This returns a normal String instead of Kotlin Result<String>
     * so Java can call it without Kotlin JVM name mangling.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun extractAudioUrl(
        videoId: String
    ): String {

        ensureInitialized()

        val cleanVideoId =
            videoId.trim()

        if (cleanVideoId.isEmpty()) {

            throw IOException(
                "Empty YouTube video ID"
            )
        }

        val youtubeUrl =
            "https://www.youtube.com/watch?v=$cleanVideoId"

        val service =
            NewPipe.getService(0)

        val streamInfo =
            StreamInfo.getInfo(
                service,
                youtubeUrl
            )

        val audioStreams =
            streamInfo.audioStreams

        if (audioStreams.isEmpty()) {

            throw IOException(
                "NewPipeExtractor returned no audio streams"
            )
        }

        val playableStream =
            audioStreams.firstOrNull {
                !it.url.isNullOrBlank()
            }
                ?: throw IOException(
                    "NewPipeExtractor returned no playable audio URL"
                )

        val audioUrl =
            playableStream.url
                ?: throw IOException(
                    "NewPipeExtractor returned an empty audio URL"
                )

        return audioUrl
    }

    /**
     * Kotlin-side compatibility helper.
     */
    fun getAudioUrl(
        videoId: String
    ): Result<String> {

        return runCatching {
            extractAudioUrl(videoId)
        }
    }

    /**
     * Kotlin compatibility helper.
     */
    fun getStreamUrl(
        format: Any?,
        videoId: String
    ): Result<String> {

        return runCatching {
            extractAudioUrl(videoId)
        }
    }

    /**
     * Kotlin compatibility helper.
     */
    fun getStreamUrl(
        videoId: String
    ): Result<String> {

        return runCatching {
            extractAudioUrl(videoId)
        }
    }
}