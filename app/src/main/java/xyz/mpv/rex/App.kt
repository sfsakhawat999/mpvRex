package xyz.mpv.rex

import android.app.Application
import xyz.mpv.rex.di.DatabaseModule
import xyz.mpv.rex.di.FileManagerModule
import xyz.mpv.rex.di.PreferencesModule
import xyz.mpv.rex.di.networkModule
import xyz.mpv.rex.presentation.crash.CrashActivity
import xyz.mpv.rex.presentation.crash.GlobalExceptionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class App :
  Application(),
  KoinStartup {
  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))
  }

  override fun onKoinStartup() =
    koinConfiguration {
      androidContext(this@App)
      modules(
        PreferencesModule,
        DatabaseModule,
        FileManagerModule,
        networkModule,
      )
    }
}
