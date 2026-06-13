package app.marlboroadvance.mpvex.cinehub.model

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
