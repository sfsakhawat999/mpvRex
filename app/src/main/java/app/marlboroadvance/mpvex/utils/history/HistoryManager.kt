package app.marlboroadvance.mpvex.utils.history

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Centralized manager for all history-related operations, including recently played 
 * tracking.
 */
class HistoryManager(
    private val context: Context,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    private val playbackStateRepository: PlaybackStateRepository,
    private val advancedPreferences: AdvancedPreferences,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "HistoryManager"
    }

    /**
     * Records the start of playback for a media item.
     */
    fun recordPlaybackStart(
        uri: Uri,
        fileName: String,
        launchSource: String,
        playlistId: Int? = null
    ) {
        if (!advancedPreferences.enableRecentlyPlayed.get()) return

        scope.launch(Dispatchers.IO) {
            runCatching {
                val filePath = resolveFilePath(uri)
                if (shouldSkipHistory(filePath)) return@launch

                val videoTitle = MPVLib.getPropertyString("media-title")?.takeIf { it != fileName }
                val duration = (MPVLib.getPropertyDouble("duration") ?: 0.0).times(1000).toLong()
                val fileSize = getMPVFileSize()
                val (width, height) = getMPVResolution()
                val (artist, album) = getMPVMetadata()
                
                // Use extension first, then height
                val isAudio = app.marlboroadvance.mpvex.utils.storage.FileTypeUtils.isAudioFile(File(filePath)) || (height <= 0)

                addRecentlyPlayed(
                    filePath = filePath,
                    fileName = fileName,
                    videoTitle = videoTitle,
                    duration = duration,
                    fileSize = fileSize,
                    width = width,
                    height = height,
                    launchSource = launchSource,
                    playlistId = playlistId,
                    isAudio = isAudio,
                    artist = artist,
                    album = album
                )
                Log.d(TAG, "Recorded playback start: $filePath")
            }.onFailure { e ->
                Log.e(TAG, "Failed to record playback start", e)
            }
        }
    }

    /**
     * Directly adds an item to history.
     */
    suspend fun addRecentlyPlayed(
        filePath: String,
        fileName: String,
        videoTitle: String? = null,
        duration: Long = 0,
        fileSize: Long = 0,
        width: Int = 0,
        height: Int = 0,
        launchSource: String? = null,
        playlistId: Int? = null,
        isAudio: Boolean = false,
        artist: String = "",
        album: String = ""
    ) {
        if (!advancedPreferences.enableRecentlyPlayed.get()) return
        if (shouldSkipHistory(filePath)) return

        recentlyPlayedRepository.addRecentlyPlayed(
            filePath, fileName, videoTitle, duration, fileSize,
            width, height, launchSource, playlistId, isAudio, artist, album
        )
    }

    /**
     * Updates metadata for the currently playing media in history.
     */
    fun updateCurrentMediaMetadata(fileName: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val mediaTitle = MPVLib.getPropertyString("media-title")
                val duration = (MPVLib.getPropertyDouble("duration") ?: 0.0).times(1000).toLong()
                val fileSize = getMPVFileSize()
                val (width, height) = getMPVResolution()

                // Update the most recent entry
                val lastPlayed = recentlyPlayedRepository.getRecentlyPlayed(1).firstOrNull()
                if (lastPlayed != null) {
                    recentlyPlayedRepository.updateVideoMetadata(
                        lastPlayed.filePath,
                        mediaTitle,
                        duration,
                        fileSize,
                        width,
                        height
                    )
                    Log.d(TAG, "Updated recently played metadata for: ${lastPlayed.filePath}")
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to update current media metadata", e)
            }
        }
    }

    suspend fun clearAll() {
        recentlyPlayedRepository.clearAll()
    }

    suspend fun getRecentlyPlayed(limit: Int = 50): List<RecentlyPlayedEntity> {
        return recentlyPlayedRepository.getRecentlyPlayed(limit)
    }

    suspend fun getLastPlayed(): String? = withContext(Dispatchers.IO) {
        val recent = runCatching { recentlyPlayedRepository.getRecentlyPlayed(50) }.getOrDefault(emptyList())
        for (entity in recent) {
            val path = entity.filePath
            if (isNonFileUri(path) || fileExists(path)) {
                return@withContext path
            } else {
                recentlyPlayedRepository.deleteByFilePath(path)
            }
        }
        null
    }

    suspend fun getLastPlayedEntity(): RecentlyPlayedEntity? = withContext(Dispatchers.IO) {
        val recent = runCatching { recentlyPlayedRepository.getRecentlyPlayed(50) }.getOrDefault(emptyList())
        for (entity in recent) {
            val path = entity.filePath
            if (isNonFileUri(path) || fileExists(path)) {
                return@withContext entity
            } else {
                recentlyPlayedRepository.deleteByFilePath(path)
            }
        }
        null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLastPlayedPath(): Flow<String?> =
        recentlyPlayedRepository
            .observeLastPlayedForHighlight()
            .mapLatest { entity ->
                val path = entity?.filePath
                if (path.isNullOrEmpty()) null
                else if (isNonFileUri(path) || fileExists(path)) path
                else null
            }.distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    suspend fun onVideoDeleted(filePath: String) {
        if (filePath.isNotBlank()) {
            recentlyPlayedRepository.deleteByFilePath(filePath)
        }
    }

    suspend fun onVideoRenamed(oldPath: String, newPath: String) {
        if (oldPath.isBlank() || newPath.isBlank()) return
        val newFileName = File(newPath).name
        runCatching {
            recentlyPlayedRepository.updateFilePath(oldPath, newPath, newFileName)
        }
    }

    /**
     * Resolves a URI to a stable file path for history tracking.
     */
    fun resolveFilePath(uri: Uri): String {
        return when (uri.scheme) {
            "file" -> uri.path ?: uri.toString()
            "content" -> {
                try {
                    context.contentResolver.query(
                        uri,
                        arrayOf(MediaStore.MediaColumns.DATA),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (index != -1) cursor.getString(index) else null
                        } else null
                    } ?: uri.toString()
                } catch (e: Exception) {
                    uri.toString()
                }
            }
            else -> uri.toString()
        }
    }

    // ==================== Private Helpers ====================

    private fun shouldSkipHistory(path: String): Boolean {
        if (path.startsWith("smb://") || path.startsWith("ftp://") || 
            path.startsWith("ftps://") || path.startsWith("webdav://")) return true
            
        val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return false
        if (uri.host?.lowercase() in listOf("127.0.0.1", "localhost", "0.0.0.0")) return true
        return false
    }

    private fun fileExists(path: String): Boolean = runCatching {
        if (path.startsWith("/") || path.startsWith("file://")) {
            val filePath = if (path.startsWith("file://")) path.removePrefix("file://") else path
            File(filePath).exists()
        } else {
            val uri = Uri.parse(path)
            val scheme = uri.scheme
            if (scheme == null || scheme.equals("file", ignoreCase = true)) File(path).exists()
            else true
        }
    }.getOrDefault(false)

    private fun isNonFileUri(path: String): Boolean = runCatching {
        if (path.startsWith("/") || path.startsWith("file://")) false
        else {
            val scheme = Uri.parse(path).scheme
            scheme != null && !scheme.equals("file", ignoreCase = true)
        }
    }.getOrDefault(false)

    private fun getMPVFileSize(): Long = runCatching {
        MPVLib.getPropertyDouble("file-size")?.toLong()
            ?: MPVLib.getPropertyDouble("stream-end")?.toLong() ?: 0L
    }.getOrDefault(0L)

    private fun getMPVResolution(): Pair<Int, Int> {
        val w = runCatching { MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0 }.getOrDefault(0)
        val h = runCatching { MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0 }.getOrDefault(0)
        return Pair(w, h)
    }

    private fun getMPVMetadata(): Pair<String, String> {
        val artist = MPVLib.getPropertyString("metadata/by-key/performer") ?: 
                     MPVLib.getPropertyString("metadata/by-key/artist") ?: ""
        val album = MPVLib.getPropertyString("metadata/by-key/album") ?: ""
        return Pair(artist, album)
    }
}
