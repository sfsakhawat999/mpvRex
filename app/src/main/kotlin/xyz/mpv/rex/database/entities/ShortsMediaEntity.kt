package xyz.mpv.rex.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for vertical videos (Shorts/Reels)
 */
@Entity(tableName = "shorts_media")
data class ShortsMediaEntity(
    @PrimaryKey
    val path: String, // Absolute file path
    val isLoved: Boolean = false,
    val isBlocked: Boolean = false,
    val isManuallyAdded: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)
