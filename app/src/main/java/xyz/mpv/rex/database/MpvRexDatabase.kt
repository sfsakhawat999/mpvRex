package xyz.mpv.rex.database

import androidx.room.Database
import androidx.room.RoomDatabase
import xyz.mpv.rex.database.dao.ExternalSubtitleDao
import xyz.mpv.rex.database.dao.PlaybackStateDao
import xyz.mpv.rex.database.dao.RecentlyPlayedDao
import xyz.mpv.rex.database.entities.ExternalSubtitleEntity
import xyz.mpv.rex.database.entities.PlaybackStateEntity
import xyz.mpv.rex.database.entities.RecentlyPlayedEntity

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    ExternalSubtitleEntity::class,
  ],
  version = 3,
)
abstract class MpvRexDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun externalSubtitleDao(): ExternalSubtitleDao
}
