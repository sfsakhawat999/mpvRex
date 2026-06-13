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

    // Movies scan karne ke liye intact loop
    fun scanDirectoryForMovies(directory: File): List<MovieItem> {
        val movies = mutableListOf<MovieItem>()
        if (!directory.exists() || !directory.isDirectory) return movies

        directory.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val specificNfo = File(directory, "${file.nameWithoutExtension}.nfo")
                val genericNfo = File(directory, "movie.nfo")
                val targetNfo = if (specificNfo.exists()) specificNfo else if (genericNfo.exists()) genericNfo else null

                if (targetNfo != null) {
                    parseMovieNfo(targetNfo, file)?.let { movies.add(it) }
                }
            } else if (file.isDirectory) {
                movies.addAll(scanDirectoryForMovies(file))
            }
        }
        return movies
    }

    // NAYA: TV Shows dhoondhne ke liye sequence scanning execution loop
    fun scanDirectoryForTvShows(directory: File): List<TvShowItem> {
        val tvShows = mutableListOf<TvShowItem>()
        if (!directory.exists() || !directory.isDirectory) return tvShows

        val tvShowNfo = File(directory, "tvshow.nfo")
        if (tvShowNfo.exists()) {
            parseTvShowNfo(tvShowNfo, directory)?.let { tvShows.add(it) }
        }

        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                tvShows.addAll(scanDirectoryForTvShows(file))
            }
        }
        return tvShows
    }

    // NAYA: Show folder ke andar jitne episodes hain unhe collect karne ka method
    fun scanTvShowEpisodes(showFolder: File): List<EpisodeItem> {
        val episodes = mutableListOf<EpisodeItem>()
        showFolder.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val nfoFile = File(showFolder, "${file.nameWithoutExtension}.nfo")
                if (nfoFile.exists()) {
                    parseEpisodeNfo(nfoFile, file)?.let { episodes.add(it) }
                }
            } else if (file.isDirectory) {
                episodes.addAll(scanTvShowEpisodes(file))
            }
        }
        return episodes
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
                plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",
                mpaa = doc.getElementsByTagName("mpaa").item(0)?.textContent ?: "",
                genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "",
                director = doc.getElementsByTagName("director").item(0)?.textContent ?: "",
                premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "",
                posterPath = resolvePoster(nfoFile)
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing movie XML: ${nfoFile.name}", e)
            null
        }
    }

    // NAYA: tvshow.nfo file data nodes reading mapping parser
    private fun parseTvShowNfo(nfoFile: File, folder: File): TvShowItem? {
        return try {
            val doc = getXmlDocument(nfoFile) ?: return null
            if (doc.documentElement.nodeName != "tvshow") return null

            TvShowItem(
                folderPath = folder.absolutePath,
                title = doc.getElementsByTagName("title").item(0)?.textContent ?: folder.name,
                plot = doc.getElementsByTagName("plot").item(0)?.textContent ?: "",
                userRating = doc.getElementsByTagName("userrating").item(0)?.textContent?.toDoubleOrNull() ?: 0.0,
                genre = doc.getElementsByTagName("genre").item(0)?.textContent ?: "",
                premiered = doc.getElementsByTagName("premiered").item(0)?.textContent ?: "",
                studio = doc.getElementsByTagName("studio").item(0)?.textContent ?: "",
                posterPath = resolvePoster(nfoFile)
            )
        } catch (e: Exception) {
            Log.e("CineHubScanner", "Error processing tvshow XML: ${nfoFile.name}", e)
            null
        }
    }

    // NAYA: individual episode XML details mapper logic
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
