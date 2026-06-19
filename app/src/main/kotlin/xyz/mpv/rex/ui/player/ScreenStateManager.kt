package xyz.mpv.rex.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import xyz.mpv.rex.preferences.PlayerPreferences

/**
 * Manager for handling screen state events (lock/unlock) and managing
 * automatic playback resumption.
 */
class ScreenStateManager(
  private val context: Context,
  private val playerPreferences: PlayerPreferences,
  private val onResumePlayback: () -> Unit,
  private val isPaused: () -> Boolean
) {
  var isActivityResumed = false
  var isActivityStarted = false
  var wasPlayingBeforePause = false

  private var pausedByScreenOff = false
  private var pendingResumeOnUnlock = false
  private var screenStateReceiverRegistered = false

  private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.action) {
        Intent.ACTION_SCREEN_OFF -> {
          // If the activity was active and playing when the screen turned off, flag it
          if (isActivityStarted && (!isPaused() || wasPlayingBeforePause)) {
            pausedByScreenOff = true
          }
        }
        Intent.ACTION_SCREEN_ON -> {
          // Fallback for devices without a lock screen
          if (pausedByScreenOff) {
            val km = context?.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (km?.isKeyguardLocked == false) {
              pendingResumeOnUnlock = true
              pausedByScreenOff = false
              if (isActivityResumed) handlePendingResumeOnUnlock()
            }
          }
        }
        Intent.ACTION_USER_PRESENT -> {
          // Screen unlocked. If we paused due to screen off, prep for resume
          if (pausedByScreenOff) {
            pendingResumeOnUnlock = true
            pausedByScreenOff = false

            // If the activity is already resumed (e.g. no lock screen delay), execute immediately
            if (isActivityResumed) {
              handlePendingResumeOnUnlock()
            }
          }
        }
      }
    }
  }

  fun setup() {
    if (!screenStateReceiverRegistered) {
      val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT)
      }
      context.registerReceiver(screenStateReceiver, filter)
      screenStateReceiverRegistered = true
    }
  }

  fun handlePendingResumeOnUnlock() {
    if (pendingResumeOnUnlock) {
      pendingResumeOnUnlock = false
      if (playerPreferences.resumeOnUnlock.get()) {
        onResumePlayback()
      }
    }
  }

  fun cleanup() {
    if (screenStateReceiverRegistered) {
      screenStateReceiverRegistered = false
      runCatching {
        context.unregisterReceiver(screenStateReceiver)
      }
    }
  }
}
