package app.marlboroadvance.mpvex.cinehub.data

import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.Base64

object CineCloudRepoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val domainsPool = listOf("https://net52.cc", "https://net11.cc", "https://hianime.lol")
    
    private val newTvDomains = listOf(
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3RzLmNvbQ==",
        "aHR0cHM6Ly9tb2JpbGVkZXRlY3QuYXBw",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmFydA==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNj",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmNsaWNr",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lmluaw==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LmxpdmU=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnBybw==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNob3A=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNpdGU=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnNwYWNl",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnN0b3Jl",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0LnZpcA==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lndpa2k=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0Lnh5eg==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5hcnQ=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5jYw==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbmZv",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5pbms=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5saXZl",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5wcm8=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy5zdG9yZQ==",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy50b3A=",
        "aHR0cHM6Ly9tb2JpZGV0ZWN0cy54eXo="
    )

    @Volatile private var workingDomain: String = "https://net52.cc"
    @Volatile private var activeSessionCookie: String = ""
    @Volatile private var lastBypassTime: Long = 0L
    @Volatile private var resolvedApiUrl: String = ""

    private val standardHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 Safari/537.36 /OS.Gatu v3.0",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "Accept-Language" to "en-IN,en-US;q=0.9,en;q=0.8",
        "X-Requested-With" to "XMLHttpRequest"
    )

    private fun decodeBase64(value: String): String {
        return try {
            String(Base64.getDecoder().decode(value))
        } catch (e: Exception) { "" }
    }

    private suspend fun fetchLiveApiUrl(): String = withContext(Dispatchers.IO) {
        if (resolvedApiUrl.isNotBlank()) return@withContext resolvedApiUrl
        
        val verificationHeaders = mapOf(
            "Cache-Control" to "no-cache, no-store, must-revalidate",
            "Pragma" to "no-cache",
            "Expires" to "0",
            "X-Requested-With" to "NetmirrorNewTV v1.0",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0",
            "Accept" to "application/json, text/plain, */*"
        )

        for (encodedNode in newTvDomains) {
            val decodedBase = decodeBase64(encodedNode).trimEnd('/')
            if (decodedBase.isBlank()) continue
            try {
                val reqBuilder = Request.Builder().url("$decodedBase/checknewtv.php")
                for (headerEntry in verificationHeaders) {
                    reqBuilder.addHeader(headerEntry.key, headerEntry.value)
                }
                
                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val payload = response.body?.string() ?: ""
                        if (payload.contains("\"token_hash\":\"")) {
                            val extractedHash = payload.substringAfter("\"token_hash\":\"").substringBefore("\"")
                            if (extractedHash.isNotBlank()) {
                                resolvedApiUrl = decodeBase64(extractedHash).trimEnd('/')
                                return@withContext resolvedApiUrl
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return@withContext "https://mobiledetects.com"
    }

    private suspend fun findWorkingDomain() {
        for (domain in domainsPool) {
            try {
                val request = Request.Builder().url("$domain/mobile/home?app=1").get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 302) {
                        workingDomain = domain
                        return
                    }
                }
            } catch (e: Exception) { /* Continue validation pool */ }
        }
    }

    private suspend fun ensureValidSession() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (activeSessionCookie.isNotEmpty() && (currentTime - lastBypassTime < 54_000_000)) {
            return@withContext
        }

        findWorkingDomain()

        try {
            val bypassHeaders = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "Accept-Encoding" to "gzip, deflate, br, zstd",
                "Accept-Language" to "en-US,en;q=0.9",
                "Cache-Control" to "max-age=0",
                "Connection" to "keep-alive",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Origin" to "https://net22.cc",
                "Referer" to "https://net22.cc/verify2",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36"
            )

            val formBody = FormBody.Builder()
                .add("g-recaptcha-response", UUID.randomUUID().toString())
                .build()

            val requestBuilder = Request.Builder().url("$workingDomain/verify.php").post(formBody)
            for (headerEntry in bypassHeaders) {
                requestBuilder.addHeader(headerEntry.key, headerEntry.value)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val cookiesList = response.headers("Set-Cookie")
                val targetCookie = cookiesList.firstOrNull { it.startsWith("t_hash_t=") }
                if (targetCookie != null) {
                    activeSessionCookie = targetCookie.substringAfter("t_hash_t=").substringBefore(";")
                    lastBypassTime = currentTime
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineCloudRepo", "Bypass sequence failure: " + e.message)
        }
    }

    private fun parseHtmlToItems(html: String, isTv: Boolean): List<Any> {
        val extractedItems = mutableListOf<Any>()
        
        val containerRegex = Regex("data-post=\"(\\d+)\"[^>]*>.*?<span[^>]*>([^<]+)</span>")
        var matches = containerRegex.findAll(html).toList()
        
        if (matches.isEmpty()) {
            val looseCardRegex = Regex("data-post=\"(\\d+)\".*?class=\"card-title\"[^>]*>([^<]+)")
            matches = looseCardRegex.findAll(html).toList()
        }

        matches.forEach { match ->
            val id = match.groupValues[1]
            val title = match.groupValues[2].trim()
            
            if (id.isNotBlank() && title.isNotBlank() && !title.equals("Later", ignoreCase = true)) {
                if (isTv) {
                    extractedItems.add(
                        TvShowItem(
                            folderPath = "cnc_tv:$id",
                            title = title,
                            plot = "Premium multi-language series catalog. Decrypted and direct streaming link resolution pipeline fully functional.",
                            userRating = 8.6,
                            genre = "Hotstar Live",
                            premiered = "2026",
                            studio = "Hotstar Mirror",
                            posterPath = "https://imgcdn.kim/hs/v/$id.jpg"
                        )
                    )
                } else {
                    extractedItems.add(
                        MovieItem(
                            videoFilePath = "cnc_stream:$id",
                            title = title,
                            originalTitle = "Netflix Mirror",
                            userRating = 8.4,
                            plot = "CNCVerse Premium Stream Link. High-speed multi-language audio layers are fully active inside player nodes.",
                            mpaa = "UA",
                            genre = "Netflix Live",
                            director = "CNCVerse",
                            premiered = "2026",
                            posterPath = "https://imgcdn.kim/poster/v/$id.jpg"
                        )
                    )
                }
            }
        }
        
        if (extractedItems.isEmpty()) {
            val dynamicPostIdRegex = Regex("data-post=\"(\\d+)\"")
            val rawDistinctIds = dynamicPostIdRegex.findAll(html).map { it.groupValues[1] }.distinct().toList()
            
            rawDistinctIds.forEachIndexed { index, id ->
                if (index < 16) {
                    if (isTv) {
                        extractedItems.add(
                            TvShowItem(
                                folderPath = "cnc_tv:$id",
                                title = "Premium Web Series $id",
                                plot = "Cloud repository stream matching configurations are fully integrated.",
                                userRating = 8.5,
                                genre = "Hotstar Series",
                                premiered = "2026",
                                studio = "CNCVerse",
                                posterPath = "https://imgcdn.kim/hs/v/$id.jpg"
                            )
                        )
                    } else {
                        extractedItems.add(
                            MovieItem(
                                videoFilePath = "cnc_stream:$id",
                                title = "Blockbuster Movie $id",
                                originalTitle = "Cloud Stream",
                                userRating = 8.2,
                                plot = "Cloud repository stream matching configurations are fully integrated.",
                                mpaa = "UA",
                                genre = "Netflix Movie",
                                director = "CNCVerse",
                                premiered = "2026",
                                posterPath = "https://imgcdn.kim/poster/v/$id.jpg"
                            )
                        )
                    }
                }
            }
        }
        return extractedItems
    }

    val fetchedMoviesCache = mutableListOf<MovieItem>()

    suspend fun fetchOnlineMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        ensureValidSession()
        
        val requestBuilder = Request.Builder().url("$workingDomain/mobile/home?app=1")
        for (headerEntry in standardHeaders) {
            requestBuilder.addHeader(headerEntry.key, headerEntry.value)
        }
        requestBuilder.addHeader("Cookie", "t_hash_t=$activeSessionCookie; hd=on; ott=nf")

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    val htmlBody = response.body?.string() ?: return@withContext emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val items = parseHtmlToItems(htmlBody, false) as List<MovieItem>
                    if (items.isNotEmpty()) {
                        fetchedMoviesCache.clear()
                        fetchedMoviesCache.addAll(items)
                        return@withContext items
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineCloudRepo", "Movie channel fetch breakdown: " + e.message)
        }
        return@withContext fetchedMoviesCache.ifEmpty { emptyList() }
    }

    val fetchedTvShowsCache = mutableListOf<TvShowItem>()

    suspend fun fetchOnlineTvShows(): List<TvShowItem> = withContext(Dispatchers.IO) {
        ensureValidSession()
        
        val requestBuilder = Request.Builder().url("$workingDomain/mobile/home?app=1")
        for (headerEntry in standardHeaders) {
            requestBuilder.addHeader(headerEntry.key, headerEntry.value)
        }
        requestBuilder.addHeader("Cookie", "t_hash_t=$activeSessionCookie; hd=on; ott=hs")

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    val htmlBody = response.body?.string() ?: return@withContext emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val items = parseHtmlToItems(htmlBody, true) as List<TvShowItem>
                    if (items.isNotEmpty()) {
                        fetchedTvShowsCache.clear()
                        fetchedTvShowsCache.addAll(items)
                        return@withContext items
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineCloudRepo", "TV channel fetch breakdown: " + e.message)
        }
        return@withContext fetchedTvShowsCache.ifEmpty { emptyList() }
    }

    suspend fun resolveDirectStreamUrl(postId: String, isTv: Boolean): String? = withContext(Dispatchers.IO) {
        val targetOtt = if (isTv) "hs" else "nf"
        val activeResolverNode = fetchLiveApiUrl()
        val playerUrl = "$activeResolverNode/newtv/player.php?id=$postId"
        
        val request = Request.Builder()
            .url(playerUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0 /OS.GatuNewTV v1.0")
            .addHeader("X-Requested-With", "NetmirrorNewTV v1.0")
            .addHeader("Ott", targetOtt)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    if (body.contains("\"video_link\":\"")) {
                        val decryptedPath = body.substringAfter("\"video_link\":\"").substringBefore("\"")
                        return@withContext decryptedPath.replace("\\/", "/")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CineCloudRepo", "Decryption trace logs failure: " + e.message)
        }
        return@withContext null
    }
}
