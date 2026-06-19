package xyz.mpv.rex.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import xyz.mpv.rex.database.converters.NetworkProtocolConverter
import xyz.mpv.rex.database.dao.NetworkConnectionDao
import xyz.mpv.rex.database.dao.PlaybackStateDao
import xyz.mpv.rex.database.dao.PlaylistDao
import xyz.mpv.rex.database.dao.RecentlyPlayedDao
import xyz.mpv.rex.database.dao.VideoMetadataDao
import xyz.mpv.rex.database.dao.ShortsMediaDao
import xyz.mpv.rex.database.entities.PlaybackStateEntity
import xyz.mpv.rex.database.entities.PlaylistEntity
import xyz.mpv.rex.database.entities.PlaylistItemEntity
import xyz.mpv.rex.database.entities.RecentlyPlayedEntity
import xyz.mpv.rex.database.entities.ShortsMediaEntity
import xyz.mpv.rex.database.entities.VideoMetadataEntity
import xyz.mpv.rex.domain.network.NetworkConnection

@Database(
  entities = [
    PlaybackStateEntity::class,
    RecentlyPlayedEntity::class,
    VideoMetadataEntity::class,
    NetworkConnection::class,
    PlaylistEntity::class,
    PlaylistItemEntity::class,
    ShortsMediaEntity::class,
  ],
  version = 13,
  exportSchema = true,
)
@TypeConverters(NetworkProtocolConverter::class)
abstract class MpvExDatabase : RoomDatabase() {
  abstract fun videoDataDao(): PlaybackStateDao

  abstract fun recentlyPlayedDao(): RecentlyPlayedDao

  abstract fun videoMetadataDao(): VideoMetadataDao

  abstract fun networkConnectionDao(): NetworkConnectionDao

  abstract fun playlistDao(): PlaylistDao

  abstract fun shortsMediaDao(): ShortsMediaDao
}
