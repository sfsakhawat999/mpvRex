package xyz.mpv.rex.di

import androidx.room.Room
import androidx.room.RoomDatabase
import xyz.mpv.rex.database.Migrations
import xyz.mpv.rex.database.MpvRexDatabase
import xyz.mpv.rex.database.repository.PlaybackStateRepositoryImpl
import xyz.mpv.rex.database.repository.RecentlyPlayedRepositoryImpl
import xyz.mpv.rex.domain.playbackstate.repository.PlaybackStateRepository
import xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import xyz.mpv.rex.domain.subtitle.repository.ExternalSubtitleRepository
import xyz.mpv.rex.domain.thumbnail.ThumbnailRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DatabaseModule =
  module {
    // Provide kotlinx.serialization Json as a singleton (used by PlayerViewModel)
    single<Json> {
      Json {
        isLenient = true
        ignoreUnknownKeys = true
      }
    }

    single<MpvRexDatabase> {
      val context = androidContext()
      Room
        .databaseBuilder(context, MpvRexDatabase::class.java, "mpvrex.db")
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addMigrations(*Migrations.ALL)
        .build()
    }

    singleOf(::PlaybackStateRepositoryImpl).bind(PlaybackStateRepository::class)

    single<RecentlyPlayedRepository> {
      RecentlyPlayedRepositoryImpl(get<MpvRexDatabase>().recentlyPlayedDao())
    }

    single<ExternalSubtitleRepository> {
      ExternalSubtitleRepository(
        context = androidContext(),
        dao = get<MpvRexDatabase>().externalSubtitleDao(),
      )
    }

    single { ThumbnailRepository(androidContext()) }
  }
