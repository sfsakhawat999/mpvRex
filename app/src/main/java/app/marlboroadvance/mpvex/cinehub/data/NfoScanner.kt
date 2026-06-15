package app.marlboroadvance.mpvex.cinehub.data

import android.util.Log
import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import app.marlboroadvance.mpvex.cinehub.model.EpisodeItem
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object NfoScanner {

    private fun isVideoFile(file: File): Boolean {
        val extensions = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "ts")
        return extensions.contains(file.extension.lowercase())
    }

    /**
     * Strict baseline check to ensure file is inside CineRex/movies directory path
     */
    private fun isStrictMoviePath(absolutePath: String): Boolean {
        val standardizedPath = absolutePath.replace("\\", "/")
        return standardizedPath.contains("/CineRex/movies/", ignoreCase = true) || 
               standardizedPath.contains("/Cinerex/movies/", ignoreCase = true)
    }

    /**
     * Strict baseline check to ensure file/folder is inside CineRex/tvshows directory path
     */
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
                // STRICT BOUNDARY RULE: Only index if it's within CineRex/movies/*
                if (isStrictMoviePath(file.absolutePath)) {
                    val specificNfo = File(directory, "${file.nameWithoutExtension}.nfo")
                    val genericNfo = File(directory, "movie.nfo")
                    val targetNfo = if (specificNfo.exists()) specificNfo else if (genericNfo.exists()) genericNfo else null

                    if (targetNfo != null) {
                        parseMovieNfo(targetNfo, file)?.let { movies.add(it) }
                    } else {
                        // Online metadata fallback extraction if local movie NFO file is completely missing
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

        // STRICT BOUNDARY RULE: Only check inside CineRex/tvshows/*
        if (isStrictTvShowPath(directory.absolutePath)) {
            val tvShowNfo = File(directory, "tvshow.nfo")
            
            // To find individual show folder, check if its parent is strictly the 'tvshows' folder
            val parentFolderName = directory.parentFile?.name ?: ""
            val isMainShowFolder = parentFolderName.equals("tvshows", ignoreCase = true)

            if (isMainShowFolder) {
                if (tvShowNfo.exists()) {
                    // Priority A: Pull metadata straight from local tvshow.nfo if present
                    parseTvShowNfo(tvShowNfo, directory)?.let { tvShows.add(it) }
                } else {
                    // Priority B: Pull from online TMDB repository using clean folder name if tvshow.nfo is missing
                    val onlineMeta = CineOnlineScraper.searchOnlineTvMetadata(directory.name)
                    tvShows.add(
                        TvShowItem(
                            folderPath = directory.absolutePath,
                            title = onlineMeta?.title ?: directory.name,
                            plot = onlineMeta?.plot ?: "No local or online show summary discovered.",
                            userRating = onlineMeta?.rating ?: 0.0,
                            genre = "Local Series",
                            premiered = onlineMeta?.premiered ?: "2026",
                            studio = "Unknown",
                            posterPath = onlineMeta?.posterPath
                        )
                    )
                }
            }
        }

        // Recursively navigate subdirectories under the tvshows structure (e.g., ShowName -> Season 1)
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

            MovieItem(
                videoFilePath = videoFile.absolutePath,
                title = doc.getElementsByTagName("title").item(0)?.textContent ?: videoFile.nameWithoutExtension,
                originalTitle = doc.getElementsByTagName("originaltitle").item(0)?.textContent ?: "",
                userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,
                plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "No description available.",
                mpaa = doc.getElementsByTagName("mpaa").item(0)?.textContent ?: "",
                genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "Local Movie",
                director = doc.getElementsByTagName("director").item(0)?.textContent ?: "Unknown",
                premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "2026",
                posterPath = resolvePoster(nfoFile)
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

            TvShowItem(
                folderPath = folder.absolutePath,
                title = doc.getElementsByTagName("title").item(0)?.textContent ?: folder.name,
                plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "No description available.",
                userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,
                genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "Local Series",
                premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "2026",
                studio = doc.getElementsByTagName("studio").item(0)?.textContent ?: "Unknown",
                posterPath = resolvePoster(nfoFile)
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

            EpisodeItem(
                videoFilePath = videoFile.absolutePath,
                title = doc.getElementsByTagName("title").item(0)?.textContent ?: videoFile.nameWithoutExtension,
                season = doc.getElementsByTagName("season").item(0)?.textContent?.toIntOrNull() ?: 1,
                episode = doc.getElementsByTagName("episode").item(0)?.textContent?.toIntOrNull() ?: 1,
                plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",
                userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,
                aired = doc.getElementsByTagName("aired").item(0)?.textContent ?: ""
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

    private fun resolvePoster(nfoFile: File): String? {
        val baseName = nfoFile.nameWithoutExtension
        val parentDir = nfoFile.parentFile
        return File(parentDir, "$baseName.jpg").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "$baseName.png").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "poster.jpg").takeIf { it.exists() }?.absolutePath
            ?: File(parentDir, "folder.jpg").takeIf { it.exists() }?.absolutePath
    }
}
