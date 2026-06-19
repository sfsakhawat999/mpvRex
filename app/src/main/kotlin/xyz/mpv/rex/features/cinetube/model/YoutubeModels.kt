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
    val authorId: String = "", // Added for clean search filtering and channel reference mapping
    val videoThumbnails: List<YoutubeThumbnail> = emptyList()
) {
    fun getBestThumbnailUrl(): String {
        // High-performance thumbnail fallback mechanism if response items are nested
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
    val bitrate: Long = 0 // Added to trace high-definition audio/video streams instantly
)

@Serializable
data class VideoDataResponse(
    val formatStreams: List<YoutubeFormat> = emptyList(),
    val adaptiveFormats: List<YoutubeFormat> = emptyList(),
    val description: String = "" // Added to handle full metadata fetch inside player layouts
)
