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
        [span_7](start_span)return extensions.contains(file.extension.lowercase())[span_7](end_span)
    }

    /**
     * Verifies if the active storage node complies with strict CineRex folder naming paths
     */
    private fun isTargetCineRexFolder(file: File): Boolean {
        val name = file.name
        return name.equals("CineRex", ignoreCase = true) || name.equals("Cinerex", ignoreCase = true)
    }

    // Movies scanning execution loop integrated with online scrapers and directory boundary locks
    fun scanDirectoryForMovies(directory: File): List<MovieItem> {
        val movies = mutableListOf<MovieItem>()
        [span_8](start_span)if (!directory.exists() || !directory.isDirectory) return movies[span_8](end_span)

        directory.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                // Check if file belongs to a parent path containing CineRex to filter generic items out
                val pathHasCineRex = file.absolutePath.contains("CineRex", ignoreCase = true) || file.absolutePath.contains("Cinerex", ignoreCase = true)
                
                if (pathHasCineRex) {
                    [span_9](start_span)val specificNfo = File(directory, "${file.nameWithoutExtension}.nfo")[span_9](end_span)
                    [span_10](start_span)val genericNfo = File(directory, "movie.nfo")[span_10](end_span)
                    [span_11](start_span)val targetNfo = if (specificNfo.exists()) specificNfo else if (genericNfo.exists()) genericNfo else null[span_11](end_span)

                    if (targetNfo != null) {
                        // Priority A: Pull metadata straight from local NFO block if present
                        [span_12](start_span)parseMovieNfo(targetNfo, file)?.let { movies.add(it) }[span_12](end_span)
                    } else {
                        // Priority B: Dynamic Online Scraper routing bypass if NFO is absent!
                        val onlineMeta = CineOnlineScraper.searchOnlineMovieMetadata(file.name)
                        movies.add(
                            MovieItem(
                                videoFilePath = file.absolutePath,
                                title = onlineMeta?.title ?: file.nameWithoutExtension,
                                originalTitle = "",
                                userRating = onlineMeta?.rating ?: 0.0,
                                plot = onlineMeta?.plot ?: "No description discovered local or online.",
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
                // Keep exploring children nodes safely
                [span_13](start_span)movies.addAll(scanDirectoryForMovies(file))[span_13](end_span)
            }
        }
        [span_14](start_span)return movies[span_14](end_span)
    }

    // TV Shows scanning execution loop integrated with online scrapers and directory boundary locks
    fun scanDirectoryForTvShows(directory: File): List<TvShowItem> {
        val tvShows = mutableListOf<TvShowItem>()
        [span_15](start_span)if (!directory.exists() || !directory.isDirectory) return tvShows[span_15](end_span)

        // Only register a folder as a series if it resides inside CineRex boundary trees
        val isInsideCineRex = directory.absolutePath.contains("CineRex", ignoreCase = true) || directory.absolutePath.contains("Cinerex", ignoreCase = true)

        if (isInsideCineRex && isTargetCineRexFolder(directory.parentFile ?: directory)) {
            [span_16](start_span)val tvShowNfo = File(directory, "tvshow.nfo")[span_16](end_span)
            if (tvShowNfo.exists()) {
                [span_17](start_span)parseTvShowNfo(tvShowNfo, directory)?.let { tvShows.add(it) }[span_17](end_span)
            } else {
                // Online dynamic scraper fallback execution block for NFOless TV directories
                val onlineMeta = CineOnlineScraper.searchOnlineTvMetadata(directory.name)
                tvShows.add(
                    TvShowItem(
                        folderPath = directory.absolutePath,
                        title = onlineMeta?.title ?: directory.name,
                        plot = onlineMeta?.plot ?: "No description discovered local or online.",
                        userRating = onlineMeta?.rating ?: 0.0,
                        genre = "Local TV Series",
                        premiered = onlineMeta?.premiered ?: "2026",
                        studio = "Unknown",
                        posterPath = onlineMeta?.posterPath
                    )
                )
            }
        }

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                [span_18](start_span)tvShows.addAll(scanDirectoryForTvShows(file))[span_18](end_span)
            }
        }
        return tvShows
    }

    // Show folder episode list data collector setup
    fun scanTvShowEpisodes(showFolder: File): List<EpisodeItem> {
        val episodes = mutableListOf<EpisodeItem>()
        showFolder.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                [span_19](start_span)val nfoFile = File(showFolder, "${file.nameWithoutExtension}.nfo")[span_19](end_span)
                if (nfoFile.exists()) {
                    [span_20](start_span)parseEpisodeNfo(nfoFile, file)?.let { episodes.add(it) }[span_20](end_span)
                } else {
                    // Quick default layout wrapper mapping for episodes missing local descriptors
                    episodes.add(
                        EpisodeItem(
                            videoFilePath = file.absolutePath,
                            title = file.nameWithoutExtension,
                            season = 1,
                            episode = extractEpisodeNumber(file.name),
                            plot = "Local Media File. Cloud repository stream matching configurations are fully integrated.",
                            userRating = 0.0,
                            aired = "2026"
                        )
                    )
                }
            } else if (file.isDirectory) {
                [span_21](start_span)episodes.addAll(scanTvShowEpisodes(file))[span_21](end_span)
            }
        }
        return episodes
    }

    private fun extractEpisodeNumber(fileName: String): Int {
        val epRegex = Regex("(?i)s\\d+e(\\d+)")
        val match = epRegex.find(fileName)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun parseMovieNfo(nfoFile: File, videoFile: File): MovieItem? {
        return try {
            [span_22](start_span)val doc = getXmlDocument(nfoFile) ?: return null[span_22](end_span)
            [span_23](start_span)if (doc.documentElement.nodeName != "movie") return null[span_23](end_span)

            MovieItem(
                [span_24](start_span)videoFilePath = videoFile.absolutePath,[span_24](end_span)
                [span_25](start_span)title = doc.getElementsByTagName("title").item(0)?.textContent ?: videoFile.nameWithoutExtension,[span_25](end_span)
                [span_26](start_span)originalTitle = doc.getElementsByTagName("originaltitle").item(0)?.textContent ?: "",[span_26](end_span)
                [span_27](start_span)userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,[span_27](end_span)
                [span_28](start_span)plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",[span_28](end_span)
                [span_29](start_span)mpaa = doc.getElementsByTagName("mpaa").item(0)?.textContent ?: "",[span_29](end_span)
                [span_30](start_span)genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "",[span_30](end_span)
                [span_31](start_span)director = doc.getElementsByTagName("director").item(0)?.textContent ?: "",[span_31](end_span)
                [span_32](start_span)premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "",[span_32](end_span)
                [span_33](start_span)posterPath = resolvePoster(nfoFile)[span_33](end_span)
            )
        } catch (e: Exception) {
            [span_34](start_span)Log.e("CineHubScanner", "Error processing movie XML: ${nfoFile.name}", e)[span_34](end_span)
            [span_35](start_span)null[span_35](end_span)
        }
    }

    private fun parseTvShowNfo(nfoFile: File, folder: File): TvShowItem? {
        return try {
            [span_36](start_span)val doc = getXmlDocument(nfoFile) ?: return null[span_36](end_span)
            [span_37](start_span)if (doc.documentElement.nodeName != "tvshow") return null[span_37](end_span)

            TvShowItem(
                [span_38](start_span)folderPath = folder.absolutePath,[span_38](end_span)
                [span_39](start_span)title = doc.getElementsByTagName("title").item(0)?.textContent ?: folder.name,[span_39](end_span)
                [span_40](start_span)plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",[span_40](end_span)
                [span_41](start_span)userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,[span_41](end_span)
                [span_42](start_span)genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "",[span_42](end_span)
                [span_43](start_span)premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "",[span_43](end_span)
                [span_44](start_span)studio = doc.getElementsByTagName("studio").item(0)?.textContent ?: "",[span_44](end_span)
                [span_45](start_span)posterPath = resolvePoster(nfoFile)[span_45](end_span)
            )
        } catch (e: Exception) {
            [span_46](start_span)Log.e("CineHubScanner", "Error processing tvshow XML: ${nfoFile.name}", e)[span_46](end_span)
            null
        }
    }

    private fun parseEpisodeNfo(nfoFile: File, videoFile: File): EpisodeItem? {
        return try {
            [span_47](start_span)val doc = getXmlDocument(nfoFile) ?: return null[span_47](end_span)
            [span_48](start_span)if (doc.documentElement.nodeName != "episodedetails") return null[span_48](end_span)

            EpisodeItem(
                [span_49](start_span)videoFilePath = videoFile.absolutePath,[span_49](end_span)
                [span_50](start_span)title = doc.getElementsByTagName("title").item(0)?.textContent ?: videoFile.nameWithoutExtension,[span_50](end_span)
                [span_51](start_span)season = doc.getElementsByTagName("season").item(0)?.textContent?.toIntOrNull() ?: 1,[span_51](end_span)
                [span_52](start_span)episode = doc.getElementsByTagName("episode").item(0)?.textContent?.toIntOrNull() ?: 1,[span_52](end_span)
                [span_53](start_span)plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",[span_53](end_span)
                [span_54](start_span)userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,[span_54](end_span)
                [span_55](start_span)aired = doc.getElementsByTagName("aired").item(0)?.textContent ?: ""[span_55](end_span)
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing episode XML: ${nfoFile.name}", e)
            null
        }
    }

    private fun getXmlDocument(file: File): Document? {
        return try {
            [span_56](start_span)val factory = DocumentBuilderFactory.newInstance()[span_56](end_span)
            [span_57](start_span)val builder = factory.newDocumentBuilder()[span_57](end_span)
            [span_58](start_span)val doc = builder.parse(file)[span_58](end_span)
            [span_59](start_span)doc.documentElement.normalize()[span_59](end_span)
            [span_60](start_span)doc[span_60](end_span)
        } catch (e: Exception) { null }
    }

    private fun resolvePoster(nfoFile: File): String? {
        [span_61](start_span)val baseName = nfoFile.nameWithoutExtension[span_61](end_span)
        [span_62](start_span)val parentDir = nfoFile.parentFile[span_62](end_span)
        [span_63](start_span)return File(parentDir, "$baseName.jpg").takeIf { it.exists() }?.absolutePath[span_63](end_span)
            [span_64](start_span)?: File(parentDir, "$baseName.png").takeIf { it.exists() }?.absolutePath[span_64](end_span)
            [span_65](start_span)?: File(parentDir, "poster.jpg").takeIf { it.exists() }?.absolutePath[span_65](end_span)
            [span_66](start_span)?: File(parentDir, "folder.jpg").takeIf { it.exists() }?.absolutePath[span_66](end_span)
    }
}
