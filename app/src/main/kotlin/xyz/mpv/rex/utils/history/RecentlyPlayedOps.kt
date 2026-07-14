package xyz.mpv.rex.utils.history

import android.net.Uri
import xyz.mpv.rex.database.entities.RecentlyPlayedEntity
import xyz.mpv.rex.domain.playbackstate.repository.PlaybackStateRepository
import xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import xyz.mpv.rex.preferences.AdvancedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Legacy wrapper for HistoryManager to maintain backward compatibility.
 * Delegates all operations to a centralized HistoryManager instance.
 */
object RecentlyPlayedOps {
  private val repository: RecentlyPlayedRepository by inject(RecentlyPlayedRepository::class.java)
  private val playbackStateRepository: PlaybackStateRepository by inject(PlaybackStateRepository::class.java)
  private val preferences: AdvancedPreferences by inject(AdvancedPreferences::class.java)

  // Lazy delegation to HistoryManager
  private val historyManager by lazy {
    HistoryManager(
      context = org.koin.core.context.GlobalContext.get().get(),
      recentlyPlayedRepository = repository,
      playbackStateRepository = playbackStateRepository,
      advancedPreferences = preferences,
      scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
    )
  }

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
    album: String = "",
  ) {
    historyManager.addRecentlyPlayed(
      filePath, fileName, videoTitle, duration, fileSize,
      width, height, launchSource, playlistId, isAudio, artist, album
    )
  }

  suspend fun clearAll() {
    historyManager.clearAll()
  }

  suspend fun updateVideoTitle(
    filePath: String,
    videoTitle: String,
  ) {
    repository.updateVideoTitle(filePath, videoTitle)
  }

  suspend fun updateVideoMetadata(
    filePath: String,
    videoTitle: String?,
    duration: Long,
    fileSize: Long,
    width: Int,
    height: Int,
  ) {
    repository.updateVideoMetadata(filePath, videoTitle, duration, fileSize, width, height)
  }

  suspend fun getLastPlayed(): String? = historyManager.getLastPlayed()

  suspend fun getLastPlayedEntity(): RecentlyPlayedEntity? = historyManager.getLastPlayedEntity()

  suspend fun hasRecentlyPlayed(): Boolean = historyManager.getLastPlayed() != null

  suspend fun getRecentlyPlayed(limit: Int = 50): List<RecentlyPlayedEntity> {
    return historyManager.getRecentlyPlayed(limit)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun observeLastPlayedPath(): Flow<String?> = historyManager.observeLastPlayedPath()

  fun observeRecentlyPlayedPaths(limit: Int = 100): Flow<Set<String>> {
    return repository.observeRecentlyPlayed(limit).map { list -> list.map { it.filePath }.toSet() }
  }

  suspend fun onVideoDeleted(filePath: String) {
    historyManager.onVideoDeleted(filePath)
  }

  suspend fun markAs(filePath: String, fileName: String, duration: Long, state: MarkAsState) {
    historyManager.markAs(filePath, fileName, duration, state)
  }

  suspend fun onVideoRenamed(
    oldPath: String,
    newPath: String,
  ) {
    historyManager.onVideoRenamed(oldPath, newPath)
  }
}
