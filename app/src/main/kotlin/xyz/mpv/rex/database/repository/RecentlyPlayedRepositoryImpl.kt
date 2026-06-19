package xyz.mpv.rex.database.repository

import xyz.mpv.rex.database.dao.RecentlyPlayedDao
import xyz.mpv.rex.database.entities.RecentlyPlayedEntity
import xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import kotlinx.coroutines.flow.Flow

class RecentlyPlayedRepositoryImpl(
  private val recentlyPlayedDao: RecentlyPlayedDao,
) : RecentlyPlayedRepository {
  override suspend fun addRecentlyPlayed(
    filePath: String,
    fileName: String,
    videoTitle: String?,
    duration: Long,
    fileSize: Long,
    width: Int,
    height: Int,
    launchSource: String?,
    playlistId: Int?,
    isAudio: Boolean,
    artist: String,
    album: String,
  ) {
    // Check if there's an existing entry for this file
    val existingEntry = recentlyPlayedDao.getByFilePath(filePath)
    
    if (existingEntry != null) {
      // Update existing entry with the latest info
      val entity = RecentlyPlayedEntity(
        id = existingEntry.id,
        filePath = filePath,
        fileName = fileName,
        videoTitle = videoTitle ?: existingEntry.videoTitle,
        duration = if (duration > 0) duration else existingEntry.duration,
        fileSize = if (fileSize > 0) fileSize else existingEntry.fileSize,
        width = if (width > 0) width else existingEntry.width,
        height = if (height > 0) height else existingEntry.height,
        timestamp = System.currentTimeMillis(),
        // Update launch source if a new one is provided, otherwise keep the old one
        launchSource = launchSource ?: existingEntry.launchSource,
        playlistId = playlistId ?: existingEntry.playlistId,
        isAudio = isAudio,
        artist = if (artist.isNotBlank()) artist else existingEntry.artist,
        album = if (album.isNotBlank()) album else existingEntry.album,
      )
      recentlyPlayedDao.insert(entity)
    } else {
      // Create a new entry
      val entity = RecentlyPlayedEntity(
        filePath = filePath,
        fileName = fileName,
        videoTitle = videoTitle,
        duration = duration,
        fileSize = fileSize,
        width = width,
        height = height,
        timestamp = System.currentTimeMillis(),
        launchSource = launchSource,
        playlistId = playlistId,
        isAudio = isAudio,
        artist = artist,
        album = album,
      )
      recentlyPlayedDao.insert(entity)
    }
  }

  override suspend fun getLastPlayed(): RecentlyPlayedEntity? = recentlyPlayedDao.getLastPlayed()

  override fun observeLastPlayed(): Flow<RecentlyPlayedEntity?> = recentlyPlayedDao.observeLastPlayed()

  override suspend fun getLastPlayedForHighlight(): RecentlyPlayedEntity? =
    recentlyPlayedDao.getLastPlayedForHighlight()

  override fun observeLastPlayedForHighlight(): Flow<RecentlyPlayedEntity?> =
    recentlyPlayedDao.observeLastPlayedForHighlight()

  override suspend fun getRecentlyPlayed(limit: Int): List<RecentlyPlayedEntity> =
    recentlyPlayedDao.getRecentlyPlayed(limit)

  override fun observeRecentlyPlayed(limit: Int): Flow<List<RecentlyPlayedEntity>> =
    recentlyPlayedDao.observeRecentlyPlayed(limit)

  override suspend fun getRecentlyPlayedBySource(
    launchSource: String,
    limit: Int,
  ): List<RecentlyPlayedEntity> = recentlyPlayedDao.getRecentlyPlayedBySource(launchSource, limit)

  override suspend fun clearAll() {
    recentlyPlayedDao.clearAll()
  }

  override suspend fun deleteByFilePath(filePath: String) {
    recentlyPlayedDao.deleteByFilePath(filePath)
  }

  override suspend fun deleteByPlaylistId(playlistId: Int) {
    recentlyPlayedDao.deleteByPlaylistId(playlistId)
  }

  override suspend fun updateFilePath(
    oldPath: String,
    newPath: String,
    newFileName: String,
  ) {
    recentlyPlayedDao.updateFilePath(oldPath, newPath, newFileName)
  }

  override suspend fun updateVideoTitle(
    filePath: String,
    videoTitle: String,
  ) {
    recentlyPlayedDao.updateVideoTitle(filePath, videoTitle)
  }

  override suspend fun updateVideoMetadata(
    filePath: String,
    videoTitle: String?,
    duration: Long,
    fileSize: Long,
    width: Int,
    height: Int,
  ) {
    recentlyPlayedDao.updateVideoMetadata(filePath, videoTitle, duration, fileSize, width, height)
  }
}
