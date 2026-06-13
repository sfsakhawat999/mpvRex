package app.marlboroadvance.mpvex.cinehub.model

import kotlinx.serialization.Serializable

@Serializable
data class MovieItem(
    val videoFilePath: String,
    val title: String,
    val originalTitle: String,
    val userRating: Double,
    val plot: String,
    val mpaa: String,
    val genre: String,
    val director: String,
    val premiered: String,
    val posterPath: String?
)

@Serializable
data class TvShowItem(
    val folderPath: String,
    val title: String,
    val plot: String,
    val userRating: Double,
    val genre: String,
    val premiered: String,
    val studio: String,
    val posterPath: String?
)

@Serializable
data class EpisodeItem(
    val videoFilePath: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val plot: String,
    val userRating: Double,
    val aired: String
)
