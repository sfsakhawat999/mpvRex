package xyz.mpv.rex.cinemine.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import xyz.mpv.rex.cinemine.model.MovieItem
import xyz.mpv.rex.cinemine.model.TvShowItem
import xyz.mpv.rex.cinemine.model.EpisodeItem
import xyz.mpv.rex.cinemine.model.YoutubeVideo
import xyz.mpv.rex.cinemine.model.YoutubeThumbnail

object CineMineRepo {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    
    // --- TMDB KEY CONFIGS (For Local Metadata Scraper Fallback) ---
    private const val TMDB_API_KEY = "38a73d59546aa8789c007d3dbd96cdbc"
    private const val TMDB_BASE = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    // --- CINETUBE (INVIDIOUS AUTOMATIC LOAD BALANCING NODES) ---
    private val INVIDIOUS_INSTANCES = listOf(
        "https://invidious.projectsegfau.lt",
        "https://yewtu.be",
        "https://invidious.privacydev.net"
    )

    // --- CINEMAX (CLOUD MIRROR REPO POOLS Handshake Encryption Tokens) ---
    private val DOMAINS_POOL = mutableListOf("https://net52.cc", "https://net11.cc", "https://hianime.lol")
    private val NEW_TV_DOMAINS = listOf(
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3RzLmNvbQ==",
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3QuYXBw",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmFydA==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNj"
    )

    @Volatile private var workingDomain = "https://net52.cc"
    @Volatile private var resolvedApiUrl = ""

    // =====================================================================
    // ====================== MODULE 1: CINEHUB (LOCAL) =====================
    // =====================================================================

    suspend fun fetchLocalMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        val movies = mutableListOf<MovieItem>()
        val dir = File("/sdcard/CineRex/movies")
        if (!dir.exists() || !dir.isDirectory) return@withContext movies

        dir.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val specificNfo = File(dir, "${file.nameWithoutExtension}.nfo")
                val genericNfo = File(dir, "movie.nfo")
                val targetNfo = if (specificNfo.exists()) specificNfo else if (genericNfo.exists()) genericNfo else null

                if (targetNfo != null) {
                    parseMovieNfo(targetNfo, file)?.let { movies.add(it) }
                } else {
                    val onlineMeta = scrapeOnlineMovieMeta(file.name)
                    movies.add(
                        MovieItem(
                            videoFilePath = file.absolutePath,
                            title = onlineMeta?.get(0) ?: file.nameWithoutExtension.replace(Regex("[\\._\\-]"), " ").trim(),
                            originalTitle = "",
                            userRating = onlineMeta?.get(2)?.toDoubleOrNull() ?: 0.0,
                            plot = onlineMeta?.get(1) ?: "Local Media File. Scan successful.",
                            genre = "Local Movie",
                            director = "Unknown",
                            premiered = "2026",
                            posterPath = onlineMeta?.get(3)?.ifBlank { null },
                            watchProgress = 0f,
                            actors = emptyList()
                        )
                    )
                }
            }
        }
        return@withContext movies
    }

    suspend fun fetchLocalTvShows(): List<TvShowItem> = withContext(Dispatchers.IO) {
        val tvShows = mutableListOf<TvShowItem>()
        val dir = File("/sdcard/CineRex/tvshows")
        if (!dir.exists() || !dir.isDirectory) return@withContext tvShows

        dir.listFiles()?.forEach { subFolder ->
            if (subFolder.isDirectory && !subFolder.name.lowercase().contains("season")) {
                val tvshowNfo = File(subFolder, "tvshow.nfo")
                if (tvshowNfo.exists()) {
                    parseTvShowNfo(tvshowNfo, subFolder)?.let { tvShows.add(it) }
                } else {
                    val onlineMeta = scrapeOnlineTvMeta(subFolder.name)
                    tvShows.add(
                        TvShowItem(
                            folderPath = subFolder.absolutePath,
                            title = onlineMeta?.get(0) ?: subFolder.name.replace(Regex("[\\._\\-]"), " ").trim(),
                            plot = onlineMeta?.get(1) ?: "Local TV Series folder.",
                            userRating = onlineMeta?.get(2)?.toDoubleOrNull() ?: 0.0,
                            genre = "Local Series",
                            premiered = "2026",
                            studio = "Unknown",
                            posterPath = onlineMeta?.get(3)?.ifBlank { null },
                            watchProgress = 0f,
                            actors = emptyList()
                        )
                    )
                }
            }
        }
        return@withContext tvShows
    }

    /**
     * Scans isolated TV Show directory nodes to cleanly map episodic .nfo details dynamically
     */
    suspend fun fetchLocalEpisodes(showFolderPath: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val episodes = mutableListOf<EpisodeItem>()
        val folder = File(showFolderPath)
        if (!folder.exists() || !folder.isDirectory) return@withContext episodes

        folder.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val nfoFile = File(folder, "${file.nameWithoutExtension}.nfo")
                if (nfoFile.exists()) {
                    try {
                        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(nfoFile)
                        val root = doc.documentElement
                        val title = root.getElementsByTagName("title").item(0)?.textContent ?: file.nameWithoutExtension
                        val season = root.getElementsByTagName("season").item(0)?.textContent?.toIntOrNull() ?: 1
                        val episode = root.getElementsByTagName("episode").item(0)?.textContent?.toIntOrNull() ?: 1
                        val plot = root.getElementsByTagName("plot").item(0)?.textContent ?: ""

                        episodes.add(
                            EpisodeItem(
                                videoFilePath = file.absolutePath,
                                title = title,
                                season = season,
                                episode = episode,
                                plot = plot,
                                userRating = 0.0,
                                aired = "2026",
                                watchProgress = 0f
                            )
                        )
                    } catch (_: Exception) {}
                } else {
                    episodes.add(
                        EpisodeItem(
                            videoFilePath = file.absolutePath,
                            title = file.nameWithoutExtension.replace(Regex("[\\._\\-]"), " ").trim(),
                            season = 1,
                            episode = 1,
                            plot = "Local Episode tracking node.",
                            userRating = 0.0,
                            aired = "2026",
                            watchProgress = 0f
                        )
                    )
                }
            } else if (file.isDirectory) {
                episodes.addAll(fetchLocalEpisodes(file.absolutePath))
            }
        }
        return@withContext episodes.sortedWith(compareBy({ it.season }, { it.episode }))
    }

    private fun isVideoFile(file: File): Boolean {
        return setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "ts").contains(file.extension.lowercase())
    }

    private fun cleanMediaFileName(fileName: String): Pair<String, String?> {
        var name = fileName.replace(Regex("(?i)\\.(mp4|mkv|avi|mov|webm|flv|ts)\$"), "").replace(Regex("[\\.\\-_]"), " ")
        val match = Regex("\\b(19|20)\\d{2}\\b").find(name)
        val year = match?.value
        if (year != null) name = name.substring(0, match.range.first).trim()
        name = name.replace(Regex("(?i)\\b(1080p|720p|2160p|4k|bluray|webrip|hdrip|dual|audio|hindi|english)\\b.*"), "").trim()
        return Pair(name, year)
    }

    private fun scrapeOnlineMovieMeta(fileName: String): List<String>? {
        return try {
            val (cleanTitle, year) = cleanMediaFileName(fileName)
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            var url = "$TMDB_BASE/search/movie?api_key=$TMDB_API_KEY&query=$encoded&language=en-US"
            if (year != null) url += "&primary_release_year=$year"

            val res = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return null
            val results = Json.parseToJsonElement(res).jsonObject["results"]?.jsonArray
            if (results != null && results.isNotEmpty()) {
                val node = results[0].jsonObject
                val title = node["title"]?.jsonPrimitive?.content ?: cleanTitle
                val plot = node["overview"]?.jsonPrimitive?.content ?: ""
                val rating = node["vote_average"]?.jsonPrimitive?.doubleOrNull?.toString() ?: "0.0"
                val poster = node["poster_path"]?.jsonPrimitive?.contentOrNull?.let { "$IMAGE_BASE_URL$it" }
                return listOf(title, plot, rating, poster ?: "")
            }
            null
        } catch (e: Exception) { null }
    }

    private fun scrapeOnlineTvMeta(folderName: String): List<String>? {
        return try {
            val (cleanTitle, year) = cleanMediaFileName(folderName)
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            var url = "$TMDB_BASE/search/tv?api_key=$TMDB_API_KEY&query=$encoded&language=en-US"
            if (year != null) url += "&first_air_date_year=$year"

            val res = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return null
            val results = Json.parseToJsonElement(res).jsonObject["results"]?.jsonArray
            if (results != null && results.isNotEmpty()) {
                val node = results[0].jsonObject
                val name = node["name"]?.jsonPrimitive?.content ?: cleanTitle
                val plot = node["overview"]?.jsonPrimitive?.content ?: ""
                val rating = node["vote_average"]?.jsonPrimitive?.doubleOrNull?.toString() ?: "0.0"
                val poster = node["poster_path"]?.jsonPrimitive?.contentOrNull?.let { "$IMAGE_BASE_URL$it" }
                return listOf(name, plot, rating, poster ?: "")
            }
            null
        } catch (e: Exception) { null }
    }

    private fun parseMovieNfo(file: File, videoFile: File): MovieItem? {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val root = doc.documentElement
            val title = root.getElementsByTagName("title").item(0)?.textContent ?: videoFile.nameWithoutExtension
            val plot = root.getElementsByTagName("plot").item(0)?.textContent ?: ""
            val rating = root.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0
            val poster = root.getElementsByTagName("thumb").item(0)?.textContent

            MovieItem(
                videoFilePath = videoFile.absolutePath, title = title, originalTitle = "",
                userRating = rating, plot = plot, mpaa = "", genre = "Local Movie",
                director = "Unknown", premiered = "2026", posterPath = poster, watchProgress = 0f, actors = emptyList()
            )
        } catch (e: Exception) { null }
    }

    private fun parseTvShowNfo(file: File, folder: File): TvShowItem? {
        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val root = doc.documentElement
            val title = root.getElementsByTagName("title").item(0)?.textContent ?: folder.name
            val plot = root.getElementsByTagName("plot").item(0)?.textContent ?: ""
            val rating = root.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0
            val poster = root.getElementsByTagName("thumb").item(0)?.textContent

            TvShowItem(
                folderPath = folder.absolutePath, title = title, plot = plot, userRating = rating,
                genre = "Local Series", premiered = "2026", studio = "Unknown", posterPath = poster, watchProgress = 0f, actors = emptyList()
            )
        } catch (e: Exception) { null }
    }

    // =====================================================================
    // ====================== MODULE 2: CINETUBE (YT/IV) ===================
    // =====================================================================

    suspend fun fetchCineTubeTrending(): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<YoutubeVideo>()
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val url = "$instance/api/v1/trending?type=Movies&region=IN"
                val response = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: continue
                val array = Json.parseToJsonElement(response).jsonArray
                
                array.forEach { elem ->
                    val obj = elem.jsonObject
                    val id = obj["videoId"]?.jsonPrimitive?.content ?: ""
                    val title = obj["title"]?.jsonPrimitive?.content ?: ""
                    val author = obj["author"]?.jsonPrimitive?.content ?: ""
                    val length = obj["lengthSeconds"]?.jsonPrimitive?.intOrNull ?: 0
                    
                    val thumbUrl = "$instance/vi/$id/hqdefault.jpg"
                    videos.add(
                        YoutubeVideo(
                            videoId = id, title = title, author = author, lengthSeconds = length,
                            videoThumbnails = listOf(YoutubeThumbnail(url = thumbUrl))
                        )
                    )
                }
                if (videos.isNotEmpty()) return@withContext videos
            } catch (e: Exception) {
                Log.w("CineMineRepo", "Invidious network fallback configuration active.")
            }
        }
        return@withContext videos
    }

    suspend fun fetchCineTubeDirectUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val url = "$instance/api/v1/videos/$videoId"
                val res = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: continue
                val parsed = json.parseToJsonElement(res).jsonObject
                val streams = parsed["formatStreams"]?.jsonArray
                val direct = streams?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content
                if (direct != null) return@withContext direct
            } catch (e: Exception) { }
        }
        return@withContext null
    }

    // =====================================================================
    // ====================== MODULE 3: CINEMAX (CLOUD REPO) ===============
    // =====================================================================

    private fun decodeB64(v: String): String = String(Base64.getDecoder().decode(v))

    private suspend fun resolveLiveApiUrl(): String = withContext(Dispatchers.IO) {
        if (resolvedApiUrl.isNotBlank()) return@withContext resolvedApiUrl
        for (encoded in NEW_TV_DOMAINS) {
            val decodedBase = decodeB64(encoded).trimEnd('/')
            try {
                val req = Request.Builder().url("$decodedBase/checknewtv.php")
                    .addHeader("X-Requested-With", "NetmirrorNewTV v1.0")
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        if (body.contains("\"token_hash\":\"")) {
                            val hash = body.substringAfter("\"token_hash\":\"").substringBefore("\"")
                            resolvedApiUrl = decodeB64(hash).trimEnd('/')
                            return@withContext resolvedApiUrl
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return@withContext "https://mobiledetects.com"
    }

    private suspend fun syncCineMaxSession(context: Context) {
        val prefs = context.getSharedPreferences("NetflixMirrorPrefs", Context.MODE_PRIVATE)
        val savedCookie = prefs.getString("nf_cookie", "")
        val savedTime = prefs.getLong("nf_cookie_timestamp", 0L)
        if (!savedCookie.isNullOrEmpty() && (System.currentTimeMillis() - savedTime < 54000000)) return

        for (domain in DOMAINS_POOL) {
            try {
                if (client.newCall(Request.Builder().url("$domain/mobile/home?app=1").build()).execute().code == 200) {
                    workingDomain = domain
                    break
                }
            } catch (e: Exception) {}
        }

        try {
            val form = FormBody.Builder().add("g-recaptcha-response", UUID.randomUUID().toString()).build()
            val request = Request.Builder().url("$workingDomain/verify.php").post(form)
                .addHeader("Referer", "$workingDomain/verify2")
                .build()
            client.newCall(request).execute().use { res ->
                val targetCookie = res.headers("Set-Cookie").firstOrNull { it.startsWith("t_hash_t=") }
                if (targetCookie != null) {
                    val value = targetCookie.substringAfter("t_hash_t=").substringBefore(";")
                    prefs.edit().putString("nf_cookie", value).putLong("nf_cookie_timestamp", System.currentTimeMillis()).apply()
                }
            }
        } catch (e: Exception) {}
    }

    suspend fun fetchCineMaxReleases(context: Context): List<MovieItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MovieItem>()
        try {
            syncCineMaxSession(context)
            val prefs = context.getSharedPreferences("NetflixMirrorPrefs", Context.MODE_PRIVATE)
            val cookie = prefs.getString("nf_cookie", "") ?: ""

            val request = Request.Builder().url("$workingDomain/mobile/home?app=1")
                .addHeader("Cookie", "t_hash_t=$cookie; hd=on; ott=nf")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            val regex = Regex("data-post=\"(\\d+)\"[^>]*>.*?class=\"card-title\"[^>]*>([^<]+)")
            
            regex.findAll(html).forEach { match ->
                val id = match.groupValues[1]
                val title = match.groupValues[2].trim()
                if (id.isNotBlank() && title.isNotBlank()) {
                    list.add(
                        MovieItem(
                            videoFilePath = "cnc_stream:$id:nf",
                            title = title, originalTitle = "Netflix Cloud", userRating = 8.5,
                            plot = "Premium cloud streaming release active.",
                            genre = "Netflix Stream", director = "CineMax", premiered = "2026",
                            posterPath = "https://imgcdn.kim/poster/v/$id.jpg", watchProgress = 0f, actors = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        return@withContext list
    }

    suspend fun resolveCineMaxUrl(postId: String, platform: String): String? = withContext(Dispatchers.IO) {
        try {
            val api = resolveLiveApiUrl()
            val request = Request.Builder().url("$api/newtv/player.php?id=$postId")
                .addHeader("X-Requested-With", "NetmirrorNewTV v1.0")
                .addHeader("Ott", platform)
                .build()

            client.newCall(request).execute().use { res ->
                val body = res.body?.string() ?: ""
                if (body.contains("\"video_link\":\"")) {
                    return@withContext body.substringAfter("\"video_link\":\"").substringBefore("\"").replace("\\/", "/")
                }
            }
        } catch (e: Exception) {}
        return@withContext null
    }
}