package xyz.mpv.rex.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import xyz.mpv.rex.database.entities.ShortsMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortsMediaDao {
    @Upsert
    suspend fun upsert(shortsMedia: ShortsMediaEntity)

    @Query("SELECT * FROM shorts_media WHERE path = :path")
    suspend fun getShortsMediaByPath(path: String): ShortsMediaEntity?

    @Query("SELECT * FROM shorts_media")
    suspend fun getAllShortsMedia(): List<ShortsMediaEntity>

    @Query("SELECT * FROM shorts_media")
    fun observeAllShortsMedia(): Flow<List<ShortsMediaEntity>>

    @Query("DELETE FROM shorts_media WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("UPDATE shorts_media SET isLoved = :isLoved WHERE path = :path")
    suspend fun updateLoved(path: String, isLoved: Boolean)

    @Query("UPDATE shorts_media SET isBlocked = :isBlocked WHERE path = :path")
    suspend fun updateBlocked(path: String, isBlocked: Boolean)
}
