package app.marlboroadvance.mpvex.cinehub.data

import android.util.Log
import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import app.marlboroadvance.mpvex.cinehub.model.EpisodeItem
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object NfoScanner {

    private fun isVideoFile(file: File): Boolean {
        val extensions = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "ts")
        return extensions.contains(file.extension.lowercase())
    }

    private fun isStrictMoviePath(absolutePath: String): Boolean {
        val standardizedPath = absolutePath.replace("\\", "/")
        return standardizedPath.contains("/CineRex/movies/", ignoreCase = true) || 
               standardizedPath.contains("/Cinerex/movies/", ignoreCase = true)
    }

    private fun isStrictTvShowPath(absolutePath: String): Boolean {
        val standardizedPath = absolutePath.replace("\\", "/")
        return standardizedPath.contains("/CineRex/tvshows/", ignoreCase = true) || 
               standardizedPath.contains("/Cinerex/tvshows/", ignoreCase = true)
    }

    // --- MOVIES SCANNING ENGINE ---
    fun scanDirectoryForMovies(directory: File): List<MovieItem> {
        val movies = mutableListOf<MovieItem>()
        if (!directory.exists() || !directory.isDirectory) return movies

        directory.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                if (isStrictMoviePath(file.absolutePath)) {
                    val specificNfo = File(directory, "${file.nameWithoutExtension}.nfo")
                    val genericNfo = File(directory, "movie.nfo")
                    val targetNfo = if (specificNfo.exists()) specificNfo else if (genericNfo.exists()) genericNfo else null

                    if (targetNfo != null) {
                        parseMovieNfo(targetNfo, file)?.let { movies.add(it) }
                    } else {
                        val onlineMeta = CineOnlineScraper.searchOnlineMovieMetadata(file.name)
                        movies.add(
                            MovieItem(
                                videoFilePath = file.absolutePath,
                                title = onlineMeta?.title ?: file.nameWithoutExtension,
                                originalTitle = "",
                                userRating = onlineMeta?.rating ?: 0.0,
                                plot = onlineMeta?.plot ?: "No local or online description available.",
                                mpaa = "",
                                genre = "Local Movie",
                                director = "Unknown",
                                premiered = onlineMeta?.premiered ?: "2026",
                                posterPath = onlineMeta?.posterPath
                            )
                        )
                    }
                }
            } else if (file.isDirectory) {
                movies.addAll(scanDirectoryForMovies(file))
            }
        }
        return movies
    }

    // --- TV SHOWS SCANNING ENGINE ---
    fun scanDirectoryForTvShows(directory: File): List<TvShowItem> {
        val tvShows = mutableListOf<TvShowItem>()
        if (!directory.exists() || !directory.isDirectory) return tvShows

        if (isStrictTvShowPath(directory.absolutePath)) {
            val tvShowNfo = File(directory, "tvshow.nfo")
            val parentFolderName = directory.parentFile?.name ?: ""
            val isMainShowFolder = parentFolderName.equals("tvshows", ignoreCase = true)

            if (isMainShowFolder) {
                if (tvShowNfo.exists()) {
                    parseTvShowNfo(tvShowNfo, directory)?.let { tvShows.add(it) }
                } else {
                    val onlineMeta = CineOnlineScraper.searchOnlineTvMetadata(directory.name)
                    tvShows.add(
                        TvShowItem(
                            folderPath = directory.absolutePath,
                            title = onlineMeta?.title ?: directory.name,
                            plot = "No description available.",
                            userRating = 0.0,
                            genre = "Local Series",
                            premiered = "2026",
                            studio = "Unknown",
                            posterPath = null
                        )
                    )
                }
            }
        }

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                tvShows.addAll(scanDirectoryForTvShows(file))
            }
        }
        return tvShows
    }

    // --- TV EPISODES COLLECTION ROUTER ---
    fun scanTvShowEpisodes(showFolder: File): List<EpisodeItem> {
        val episodes = mutableListOf<EpisodeItem>()
        if (!showFolder.exists() || !showFolder.isDirectory) return episodes

        showFolder.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val nfoFile = File(showFolder, "${file.nameWithoutExtension}.nfo")
                if (nfoFile.exists()) {
                    parseEpisodeNfo(nfoFile, file)?.let { episodes.add(it) }
                } else {
                    episodes.add(
                        EpisodeItem(
                            videoFilePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            season = extractSeasonNumber(showFolder.name),
                            episode = extractEpisodeNumber(file.name),
                            plot = "Local Media File. Multi-source scraping pipeline links active.",
                            userRating = 0.0,
                            aired = "2026"
                        )
                    )
                }
            } else if (file.isDirectory) {
                episodes.addAll(scanTvShowEpisodes(file))
            }
        }
        return episodes
    }

    private fun extractSeasonNumber(folderName: String): Int {
        val seasonRegex = Regex("(?i)season\\s*(\\d+)|s(\\d+)")
        val match = seasonRegex.find(folderName)
        return match?.groupValues?.find { it.isNotBlank() && it.toIntOrNull() != null }?.toIntOrNull() ?: 1
    }

    private fun extractEpisodeNumber(fileName: String): Int {
        val epRegex = Regex("(?i)s\\d+e(\\d+)|e(\\d+)")
        val match = epRegex.find(fileName)
        return match?.groupValues?.find { it.isNotBlank() && it.toIntOrNull() != null }?.toIntOrNull() ?: 1
    }

    private fun parseMovieNfo(nfoFile: File, videoFile: File): MovieItem? {
        return try {
            val doc = getXmlDocument(nfoFile) ?: return null
            if (doc.documentElement.nodeName != "movie") return null

            val root = doc.documentElement
            val title = getTagText(root, "title").ifBlank { videoFile.nameWithoutExtension }
            val originalTitle = getTagText(root, "originaltitle")
            val userRating = getTagText(root, "userrating").toDoubleOrNull() ?: 0.0
            val plot = getTagText(root, "plot").ifBlank { "No description available." }
            val mpaa = getTagText(root, "mpaa")
            val genre = getTagText(root, "genre").ifBlank { "Local Movie" }
            val director = getTagText(root, "director").ifBlank { "Unknown" }
            val premiered = getTagText(root, "premiered").ifBlank { getTagText(root, "year").ifBlank { "2026" } }

            MovieItem(
                videoFilePath = videoFile.absolutePath,
                title = title,
                originalTitle = originalTitle,
                userRating = userRating,
                plot = plot,
                mpaa = mpaa,
                genre = genre,
                director = director,
                premiered = premiered,
                posterPath = resolvePosterWithFallback(nfoFile, root)
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing movie XML: ${nfoFile.name}", e)
            null
        }
    }

    private fun parseTvShowNfo(nfoFile: File, folder: File): TvShowItem? {
        return try {
            val doc = getXmlDocument(nfoFile) ?: return null
            if (doc.documentElement.nodeName != "tvshow") return null

            val root = doc.documentElement
            val title = getTagText(root, "title").ifBlank { folder.name }
            val plot = getTagText(root, "plot").ifBlank { "No description available." }
            val userRating = getTagText(root, "userrating").toDoubleOrNull() ?: 0.0
            val genre = getTagText(root, "genre").ifBlank { "Local Series" }
            val premiered = getTagText(root, "premiered").ifBlank { getTagText(root, "year").ifBlank { "2026" } }
            val studio = getTagText(root, "studio").ifBlank { "Unknown" }

            TvShowItem(
                folderPath = folder.absolutePath,
                title = title,
                plot = plot,
                userRating = userRating,
                genre = genre,
                premiered = premiered,
                studio = studio,
                posterPath = resolvePosterWithFallback(nfoFile, root)
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing tvshow XML: ${nfoFile.name}", e)
            null
        }
    }

    private fun parseEpisodeNfo(nfoFile: File, videoFile: File): EpisodeItem? {
        return try {
            val doc = getXmlDocument(nfoFile) ?: return null
            if (doc.documentElement.nodeName != "episodedetails") return null

            val root = doc.documentElement
            val title = getTagText(root, "title").ifBlank { videoFile.nameWithoutExtension }
            val season = getTagText(root, "season").toIntOrNull() ?: 1
            val episode = getTagText(root, "episode").toIntOrNull() ?: 1
            val plot = getTagText(root, "plot")
            val userRating = getTagText(root, "userrating").toDoubleOrNull() ?: 0.0
            val aired = getTagText(root, "aired")

            EpisodeItem(
                videoFilePath = videoFile.absolutePath,
                title = title,
                season = season,
                episode = episode,
                plot = plot,
                userRating = userRating,
                aired = aired
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing episode XML: ${nfoFile.name}", e)
            null
        }
    }

    private fun getXmlDocument(file: File): Document? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(file)
            doc.documentElement.normalize()
            doc
        } catch (e: Exception) { null }
    }

    private fun getTagText(element: Element, tagName: String): String {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            return nodeList.item(0)?.textContent?.trim() ?: ""
        }
        return ""
    }

    /**
     * Advanced Failover Artwork Engine: Extracts local cover files first,
     * and fallback parses direct high-quality cloud URLs inside <thumb> blocks natively.
     */
    private fun resolvePosterWithFallback(nfoFile: File, rootElement: Element): String? {
        val baseName = nfoFile.nameWithoutExtension
        val parentDir = nfoFile.parentFile

        // Step 1: Check Local Target Directory Files
        val localCheck = File(parentDir, "$baseName.jpg").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "$baseName.png").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "poster.jpg").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "folder.jpg").takeIf { it.exists() }?.absolutePath
        if (localCheck != null) return localCheck

        // Step 2: Fallback Parse Embedded XML <thumb> tags
        val thumbList = rootElement.getElementsByTagName("thumb")
        for (i in 0 until thumbList.length) {
            val thumbNode = thumbList.item(i)
            if (thumbNode != null && thumbNode.nodeType == Node.ELEMENT_NODE) {
                val thumbElement = thumbNode as Element
                val aspect = thumbElement.getAttribute("aspect")
                // Give absolute priority to standard vertical posters
                if (aspect == "poster" || aspect.isBlank()) {
                    val url = thumbElement.textContent?.trim() ?: ""
                    if (url.startsWith("http")) return url
                }
            }
        }

        // Final fallback to any valid url inside thumb blocks
        if (thumbList.length > 0) {
            val firstUrl = thumbList.item(0)?.textContent?.trim() ?: ""
            if (firstUrl.startsWith("http")) return firstUrl
        }

        return null
    }

    /**
     * Extracts deep actor information blocks including avatar portraits from NFO schemas
     */
    fun parseActorsFromNfo(nfoFile: File): List<Pair<String, String>> {
        val actorsList = mutableListOf<Pair<String, String>>()
        try {
            val doc = getXmlDocument(nfoFile) ?: return actorsList
            val actorNodes = doc.getElementsByTagName("actor")
            for (i in 0 until actorNodes.length) {
                val node = actorNodes.item(i)
                if (node != null && node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val name = getTagText(element, "name")
                    val thumb = getTagText(element, "thumb")
                    if (name.isNotBlank()) {
                        actorsList.add(Pair(name, thumb))
                    }
                }
            }
        } catch (_: Exception) {}
        return actorsList
    }

    /**
     * Parses content resume state ratios to correctly track video progress layers
     */
    fun parseWatchProgress(nfoFile: File): Float {
        try {
            val doc = getXmlDocument(nfoFile) ?: return 0f
            val resumeNodes = doc.getElementsByTagName("resume")
            if (resumeNodes.length > 0) {
                val element = resumeNodes.item(0) as Element
                val position = getTagText(element, "position").toFloatOrNull() ?: 0f
                val total = getTagText(element, "total").toFloatOrNull() ?: 0f
                if (total > 0f) {
                    return (position / total).coerceIn(0f, 1f)
                }
            }
        } catch (_: Exception) {}
        return 0f
    }
}
