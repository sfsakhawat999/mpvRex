package app.marlboroadvance.mpvex.youtube.data

import app.marlboroadvance.mpvex.youtube.model.YoutubeVideo
import app.marlboroadvance.mpvex.youtube.model.VideoDataResponse
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object InvidiousClient {
    // Optimized connection pools with safe network timeouts to avoid thread stalling
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    
    // Switched to a premium, highly stable open-source API instance node
    private const val INSTANCE_URL = "https://inv.tux.digital"

    /**
     * Fetches trending videos from Invidious api to map on YouTube / Shorts channel feeds
     */
    suspend fun fetchTrendingVideos(type: String = "Movies"): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val url = "$INSTANCE_URL/api/v1/trending?type=$type"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("InvidiousClient", "HTTP Error response verification failed with code: ${response.code}")
                    return@withContext emptyList()
                }
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                return@withContext jsonParser.decodeFromString<List<YoutubeVideo>>(responseBody)
            }
        } catch (e: Exception) {
            android.util.Log.e("InvidiousClient", "Structural transmission failure inside trend threads", e)
            return@withContext emptyList()
        }
    }

    /**
     * Extracts direct stream mkv/mp4 download links to pass straight to internal MPV layout engine
     */
    suspend fun fetchDirectStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val url = "$INSTANCE_URL/api/v1/videos/$videoId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseBody = response.body?.string() ?: return@withContext null
                val parsed = jsonParser.decodeFromString<VideoDataResponse>(responseBody)
                
                // Return best resolution playable progressive stream url
                return@withContext parsed.formatStreams.firstOrNull { it.container == "mp4" }?.url
                    ?: parsed.formatStreams.firstOrNull()?.url
                    ?: parsed.adaptiveFormats.firstOrNull { it.container == "mp4" }?.url
            }
        } catch (e: Exception) {
            android.util.Log.e("InvidiousClient", "Format analytical link extraction failure on video token: $videoId", e)
            return@withContext null
        }
    }

    /**
     * --- NEW: Integrated Search Pipeline Engine ---
     * Searches global YouTube index database through Invidious query processing engines
     */
    suspend fun fetchSearchVideos(query: String): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val url = try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            "$INSTANCE_URL/api/v1/search?q=$encodedQuery&type=video"
        } catch (e: Exception) {
            "$INSTANCE_URL/api/v1/search?q=$query&type=video"
        }
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("InvidiousClient", "HTTP Search verification failure with code: ${response.code}")
                    return@withContext emptyList()
                }
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                return@withContext jsonParser.decodeFromString<List<YoutubeVideo>>(responseBody)
            }
        } catch (e: Exception) {
            android.util.Log.e("InvidiousClient", "Analytical search endpoint transmission failure", e)
            return@withContext emptyList()
        }
    }
}
