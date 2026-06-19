package xyz.mpv.rex.di

import xyz.mpv.rex.domain.anime4k.Anime4KManager
import xyz.mpv.rex.domain.hdr.HdrToysManager
import xyz.mpv.rex.repository.wyzie.WyzieSearchRepository
import xyz.mpv.rex.ui.player.PlaybackManager
import okhttp3.OkHttpClient
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import java.util.concurrent.TimeUnit

val domainModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    single { Anime4KManager(androidContext()) }
    single { HdrToysManager(androidContext()) }
    single { WyzieSearchRepository(androidContext(), get(), get(), get()) }
    single { PlaybackManager(get()) }
}

