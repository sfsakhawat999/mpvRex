package xyz.mpv.rex.jellyfin.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import org.koin.android.ext.android.inject
import xyz.mpv.rex.R
import xyz.mpv.rex.jellyfin.preferences.JellyfinPreferences

class JellyfinRemoteService : Service() {

  private val remoteClient: JellyfinRemoteClient by inject()
  private val prefs: JellyfinPreferences by inject()

  companion object {
    const val ACTION_START = "xyz.mpv.rex.jellyfin.START_REMOTE"
    const val ACTION_STOP = "xyz.mpv.rex.jellyfin.STOP_REMOTE"
    private const val NOTIF_ID = 1002
    private const val CHANNEL_ID = "jellyfin_remote_channel"
  }

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    when (intent?.action) {
      ACTION_STOP -> {
        remoteClient.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        if (!prefs.isConfigured() || !prefs.enableRemote) {
          stopSelf()
          return START_NOT_STICKY
        }
        startForegroundWithNotification()
        remoteClient.ensureRegistered()
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder? = null

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(NotificationManager::class.java)
      val ch = NotificationChannel(CHANNEL_ID, "Jellyfin Remote", NotificationManager.IMPORTANCE_LOW)
      ch.description = "Keeps Jellyfin remote target discoverable"
      nm.createNotificationChannel(ch)
    }
  }

  private fun startForegroundWithNotification() {
    val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("mpvRex Jellyfin Remote")
      .setContentText("Discoverable as Play on target")
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setOngoing(true)
      .build()
    ServiceCompat.startForeground(this as android.app.Service, NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
  }
}
