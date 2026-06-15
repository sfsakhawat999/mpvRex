package app.marlboroadvance.mpvex.cinehub.data

import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object CineCloudRepoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Pure CNCVerse Premium Endpoints extracted from your shared sources
    private const val CNC_MAIN_URL = "https://net52.cc"
    private const val RESOLVER_NODE = "https://mobiledetects.com"

    private val baseHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36",
        "Accept-Language" to "en-IN,en-US;q=0.9,en;q=0.8",
        "X-Requested-With" to "XMLHttpRequest"
    )

    /**
     * Scrapes trending Movies from CNCVerse proxy networks into our native MovieItem model
     */
    suspend fun fetchOnlineMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MovieItem>()
        val targetUrl = "$CNC_MAIN_URL/mobile/home?app=1"
        
        val request = Request.Builder().url(targetUrl)
        baseHeaders.forEach { (k, v) -> request.addHeader(k, v) }
        request.addHeader("Cookie", "hd=on; ott=nf") // nf = Netflix catalog hook

        try {
            client.newCall(request.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: return@withContext emptyList()
                    val doc = Jsoup.parse(html)
                    
                    doc.select("article, .top10-post").forEach { element ->
                        val id = element.selectFirst("a")?.attr("data-post") ?: element.attr("data-post")
                        val parsedTitle = element.select(".card-title, span").text().ifBlank { "Premium Movie" }
                        
                        if (!id.isNullOrBlank()) {
                            list.add(
                                MovieItem(
                                    videoFilePath = "cnc_stream:$id", // Mark as CNC target token
                                    title = parsedTitle,
                                    originalTitle = "Netflix Mirror",
                                    userRating = 8.4,
                                    plot = "CNCVerse Premium Stream Link. High-speed multi-language audio layers are fully active inside player nodes.",
                                    mpaa = "UA",
                                    genre = "Cloud Movie",
                                    director = "CNCVerse",
                                    premiered = "2026",
                                    posterPath = "https://imgcdn.kim/poster/v/$id.jpg"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CineCloudRepo", "Failed fetching CNCVerse movies: ${e.message}")
        }
        return@withContext list.take(15)
    }

    /**
     * Scrapes trending TV Shows from CNCVerse proxy networks into our native TvShowItem model
     */
    suspend fun fetchOnlineTvShows(): List<TvShowItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TvShowItem>()
        val targetUrl = "$CNC_MAIN_URL/mobile/home?app=1"
        
        val request = Request.Builder().url(targetUrl)
        baseHeaders.forEach { (k, v) -> request.addHeader(k, v) }
        request.addHeader("Cookie", "hd=on; ott=hs") // hs = Hotstar catalog hook

        try {
            client.newCall(request.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: return@withContext emptyList()
                    val doc = Jsoup.parse(html)
                    
                    doc.select("article, .top10-post").forEach { element ->
                        val id = element.selectFirst("a")?.attr("data-post") ?: element.attr("data-post")
                        val parsedTitle = element.select(".card-title, span").text().ifBlank { "Premium Show" }
                        
                        if (!id.isNullOrBlank()) {
                            list.add(
                                TvShowItem(
                                    folderPath = "cnc_tv:$id", // Mark as CNC target token
                                    title = parsedTitle,
                                    plot = "Premium multi-language series catalog. Decrypted and direct streaming link resolution pipeline fully functional.",
                                    userRating = 8.6,
                                    genre = "Cloud TV Series",
                                    premiered = "2026",
                                    studio = "Hotstar Mirror",
                                    posterPath = "https://imgcdn.kim/hs/v/$id.jpg"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CineCloudRepo", "Failed fetching CNCVerse tvshows: ${e.message}")
        }
        return@withContext list.take(15)
    }

    /**
     * Decrypts underlying premium .m3u8 HLS direct progressive playback URLs
     */
    suspend fun resolveDirectStreamUrl(postId: String, isTv: Boolean): String? = withContext(Dispatchers.IO) {
        val targetOtt = if (isTv) "hs" else "nf"
        val url = "$RESOLVER_NODE/newtv/player.php?id=$postId"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0")
            .header("X-Requested-With", "NetmirrorNewTV v1.0")
            .header("Ott", targetOtt)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    if (body.contains("\"video_link\":\"")) {
                        val cleanLink = body.substringAfter("\"video_link\":\"").substringBefore("\"")
                        return@withContext cleanLink.replace("\\/", "/")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineCloudRepo", "Failed decrypting stream link nodes", e)
        }
        return null
    }
}
