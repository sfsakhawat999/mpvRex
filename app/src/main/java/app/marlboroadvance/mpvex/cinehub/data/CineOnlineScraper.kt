package app.marlboroadvance.mpvex.cinehub.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object CineOnlineScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    
    // Stable Free Public Access TMDB Credentials
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
    private const val API_KEY = "38a73d59546aa8789c007d3dbd96cdbc"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    /**
     * Cleans raw media files to strip quality metrics, release tags, and technical noise
     */
    fun cleanMediaFileName(fileName: String): Pair<String, String?> {
        var cleanName = fileName.replace(Regex("(?i)\\.(mp4|mkv|avi|mov|webm|flv|ts)\$"), "")
            .replace(Regex("[\\.\\-_]"), " ")

        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val match = yearRegex.find(cleanName)
        val year = match?.value

        if (year != null) {
            cleanName = cleanName.substring(0, match.range.first).trim()
        }
        
        // Strip out secondary encoding noise parameters safely
        cleanName = cleanName.replace(Regex("(?i)\\b(1080p|720p|2160p|4k|bluray|webrip|hdrip|x264|x265|h264|h265|dual|audio|hindi|english)\\b.*"), "").trim()
        return Pair(cleanName, year)
    }

    /**
     * Scrapes live movie information directly via TMDB online portal endpoints
     */
    fun searchOnlineMovieMetadata(fileName: String): OnlineMediaMetadata? {
        try {
            val (cleanTitle, year) = cleanMediaFileName(fileName)
            if (cleanTitle.isBlank()) return null
            
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            var url = "$TMDB_BASE_URL/search/movie?api_key=$API_KEY&query=$encodedTitle&language=en-US"
            if (year != null) url += "&primary_release_year=$year"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val parsed = jsonParser.decodeFromString<TMDBMovieSearchWrapper>(body)
                    val result = parsed.results.firstOrNull()
                    if (result != null) {
                        return OnlineMediaMetadata(
                            title = result.title ?: cleanTitle,
                            plot = result.overview ?: "No online overview available.",
                            rating = result.vote_average,
                            posterPath = result.poster_path?.let { "$IMAGE_BASE_URL$it" },
                            premiered = result.release_date ?: "2026"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CineOnlineScraper", "Movie online metadata bypass failed: ${e.message}")
        }
        return null
    }

    /**
     * Scrapes live tv show information directly via TMDB online portal endpoints
     */
    fun searchOnlineTvMetadata(folderName: String): OnlineMediaMetadata? {
        try {
            val (cleanTitle, year) = cleanMediaFileName(folderName)
            if (cleanTitle.isBlank()) return null
            
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            var url = "$TMDB_BASE_URL/search/tv?api_key=$API_KEY&query=$encodedTitle&language=en-US"
            if (year != null) url += "&first_air_date_year=$year"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    val parsed = jsonParser.decodeFromString<TMDBTvSearchWrapper>(body)
                    val result = parsed.results.firstOrNull()
                    if (result != null) {
                        return OnlineMediaMetadata(
                            title = result.name ?: cleanTitle,
                            plot = result.overview ?: "No online overview available.",
                            rating = result.vote_average,
                            posterPath = result.poster_path?.let { "$IMAGE_BASE_URL$it" },
                            premiered = result.first_air_date ?: "2026"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CineOnlineScraper", "TV show online metadata bypass failed: ${e.message}")
        }
        return null
    }
}

// Strict Data Containers matching Kotlinx Serialization Rules
@kotlinx.serialization.Serializable
data class TMDBMovieSearchWrapper(val results: List<TMDBMovieNode>)
@kotlinx.serialization.Serializable
data class TMDBMovieNode(val title: String? = null, val overview: String? = null, val poster_path: String? = null, val vote_average: Double = 0.0, val release_date: String? = null)

@kotlinx.serialization.Serializable
data class TMDBTvSearchWrapper(val results: List<TMDBTvNode>)
@kotlinx.serialization.Serializable
data class TMDBTvNode(val name: String? = null, val overview: String? = null, val poster_path: String? = null, val vote_average: Double = 0.0, val first_air_date: String? = null)

data class OnlineMediaMetadata(val title: String, val plot: String, val rating: Double, val posterPath: String?, val premiered: String)
