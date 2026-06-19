package xyz.mpv.rex.features.cinehub.model

import kotlinx.serialization.Serializable

@Serializable
data class ActorItem(
    val name: String,
    val thumbUrl: String
) {
    fun getBestActorThumb(): String {
        return if (!thumbUrl.isNullOrBlank() && thumbUrl.startsWith("http")) thumbUrl 
            else "https://image.tmdb.org/t/p/w185/abstract_profile.jpg" // Safe profile placeholder
    }
}

@Serializable
data class MovieItem(
    val videoFilePath: String,
    val title: String,
    val originalTitle: String = "",
    val userRating: Double = 0.0,
    val plot: String = "",
    val mpaa: String = "",
    val genre: String = "",
    val director: String = "",
    val premiered: String = "",
    val posterPath: String? = null,
    val watchProgress: Float = 0f,
    val isCloudStream: Boolean = videoFilePath.startsWith("cnc_stream:") || videoFilePath.startsWith("vidsrc_"), // Dynamic indicator tag
    val actors: List<ActorItem> = emptyList()
) {
    fun getFormattedRating(): String = String.format("%.1f", userRating)
}

@Serializable
data class TvShowItem(
    val folderPath: String,
    val title: String,
    val plot: String = "",
    val userRating: Double = 0.0,
    val genre: String = "",
    val premiered: String = "",
    val studio: String = "",
    val posterPath: String? = null,
    val watchProgress: Float = 0f,
    val isCloudSeries: Boolean = folderPath.startsWith("cnc_tv:") || folderPath.startsWith("vidsrc_"), // Dynamic indicator tag
    val actors: List<ActorItem> = emptyList()
) {
    fun getFormattedRating(): String = String.format("%.1f", userRating)
}

@Serializable
data class EpisodeItem(
    val videoFilePath: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val plot: String = "",
    val userRating: Double = 0.0,
    val aired: String = "",
    val watchProgress: Float = 0f
) {
    fun getEpisodeCode(): String = String.format("S%02dE%02d", season, episode)
}
