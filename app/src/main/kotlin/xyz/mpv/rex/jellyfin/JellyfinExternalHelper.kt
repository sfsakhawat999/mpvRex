package xyz.mpv.rex.jellyfin

import android.content.Intent
import android.util.Log
import xyz.mpv.rex.jellyfin.sync.JellyfinIdentityResolver

object JellyfinExternalHelper {
  private const val TAG = "JellyfinExternal"

  data class ExternalInfo(
    val itemId: String?,
    val positionMs: Int?,
  )

  fun detect(intent: Intent): ExternalInfo? {
    val hasPosition = intent.hasExtra("position")
    val positionMs: Int? = if (hasPosition) intent.getIntExtra("position", 0) else null
    val resolved = JellyfinIdentityResolver.resolve(intent)
    val isJellyfinUrl = isJellyfinUrl(intent)
    val isJellyfin = resolved != null || (hasPosition && isJellyfinUrl)
    if (!isJellyfin) return null
    // Always log detection
    if (resolved != null) {
      Log.d(TAG, "external Jellyfin intent detected itemId=${resolved.itemId} positionMs=${positionMs ?: 0}")
    } else {
      Log.d(TAG, "external Jellyfin intent detected (no itemId) positionMs=${positionMs ?: 0}")
    }
    return ExternalInfo(
      itemId = resolved?.itemId,
      positionMs = positionMs,
    )
  }

  private fun isJellyfinUrl(intent: Intent): Boolean {
    val data = intent.dataString ?: intent.getStringExtra("uri") ?: return false
    return data.contains("/Videos/", ignoreCase = true) && data.contains("/stream", ignoreCase = true)
  }

  fun logSeekApplied(positionMs: Int) {
    Log.d(TAG, "seek applied to external positionMs=$positionMs sec=${positionMs / 1000}")
  }

  fun logFinalReturn(positionMs: Int?, durationMs: Int?) {
    Log.d(TAG, "final position returned positionMs=$positionMs durationMs=$durationMs")
  }
}
