package xyz.mpv.rex.features.cinetube.model

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeVideo(
    val videoId: String,
    val title: String,
    val description: String = "",
    val viewCount: Long = 0,
    val publishedText: String = "",
    val lengthSeconds: Int = 0,
    val author: String = "",
    val authorId: String = "", 
    val authorVerified: Boolean = false, // Dynamic badge rendering support
    val videoThumbnails: List<YoutubeThumbnail> = emptyList()
) {
    fun getBestThumbnailUrl(): String {
        return videoThumbnails.maxByOrNull { it.width }?.url 
            ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
}

@Serializable
data class YoutubeThumbnail(
    val quality: String = "",
    val url: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class YoutubeFormat(
    val url: String,
    val container: String = "",
    val qualityLabel: String = "",
    val type: String = "",
    val bitrate: Long = 0 
)

@Serializable
data class VideoDataResponse(
    val formatStreams: List<YoutubeFormat> = emptyList(),
    val adaptiveFormats: List<YoutubeFormat> = emptyList(),
    val description: String = "" 
)

/**
 * Type-safe data schema to handle user response loops cleanly 
 */
@Serializable
data class InvidiousAuthResponse(
    val sessionToken: String? = null,
    val error: String? = null
)
