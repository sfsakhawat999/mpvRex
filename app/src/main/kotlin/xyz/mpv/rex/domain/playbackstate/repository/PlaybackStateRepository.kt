package xyz.mpv.rex.domain.playbackstate.repository

import xyz.mpv.rex.database.entities.PlaybackStateEntity

interface PlaybackStateRepository {
  suspend fun upsert(playbackState: PlaybackStateEntity)

  suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity?

  suspend fun clearAllPlaybackStates()

  suspend fun deleteByTitle(mediaTitle: String)

  suspend fun updateMediaTitle(
    oldTitle: String,
    newTitle: String,
  )

  suspend fun getAllPlaybackStates(): List<PlaybackStateEntity>

  fun observeAllPlaybackStates(): kotlinx.coroutines.flow.Flow<List<PlaybackStateEntity>>
}
