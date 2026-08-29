package xyz.mpv.rex.jellyfin.sync

import android.content.Intent
import android.net.Uri
import android.util.Log

data class JellyfinResolved(
  val itemId: String,
  val mediaSourceId: String?,
  val playSessionId: String?,
)

object JellyfinIdentityResolver {
  private const val TAG = "JellyfinIdentity"

  // Matches /Videos/{uuid}/stream where uuid may be with or without dashes
  private val VIDEOS_STREAM_REGEX = Regex("""/Videos/([0-9a-fA-F-]{32,36})/stream""", RegexOption.IGNORE_CASE)
  private val ITEM_ID_EXTRAS = listOf("jellyfin_item_id", "item_id", "itemId", "id", "jellyfinId")

  fun resolve(intent: Intent): JellyfinResolved? {
    // 1. Primary: parse from stream URL
    val dataString = intent.dataString
    val dataUri = intent.data
    parseFromUrl(dataString)?.let { return it }
    parseFromUri(dataUri)?.let { return it }

    // Fallback: check uri extra string
    intent.getStringExtra("uri")?.let { uriStr ->
      parseFromUrl(uriStr)?.let { return it }
    }

    // 2. Explicit extras
    for (key in ITEM_ID_EXTRAS) {
      val raw = intent.getStringExtra(key) ?: continue
      val normalized = normalizeUuid(raw) ?: continue
      Log.d(TAG, "Resolved itemId from extra $key")
      return JellyfinResolved(
        itemId = normalized,
        mediaSourceId = intent.getStringExtra("mediaSourceId") ?: intent.getStringExtra("media_source_id"),
        playSessionId = intent.getStringExtra("playSessionId") ?: intent.getStringExtra("play_session_id"),
      )
    }

    Log.d(TAG, "No Jellyfin identity found for intent data=$dataString")
    return null
  }

  fun resolveFromUri(uri: Uri): JellyfinResolved? = parseFromUri(uri) ?: parseFromUrl(uri.toString())

  fun resolveFromUrl(url: String): JellyfinResolved? = parseFromUrl(url)

  private fun parseFromUrl(url: String?): JellyfinResolved? {
    if (url.isNullOrBlank()) return null
    val match = VIDEOS_STREAM_REGEX.find(url) ?: return null
    val rawId = match.groupValues[1]
    val normalized = normalizeUuid(rawId) ?: return null
    return JellyfinResolved(
      itemId = normalized,
      mediaSourceId = extractQueryParam(url, "mediaSourceId"),
      playSessionId = extractQueryParam(url, "playSessionId"),
    )
  }

  private fun extractQueryParam(url: String, key: String): String? {
    // Manual parsing to avoid android.net.Uri in unit tests (returnDefaultValues)
    val qStart = url.indexOf('?')
    if (qStart == -1) return null
    val query = url.substring(qStart + 1)
    // Strip fragment
    val cleanQuery = query.substringBefore('#')
    for (pair in cleanQuery.split('&')) {
      val idx = pair.indexOf('=')
      if (idx == -1) continue
      val k = pair.substring(0, idx)
      val v = pair.substring(idx + 1)
      if (k == key) return runCatching { java.net.URLDecoder.decode(v, "UTF-8") }.getOrDefault(v)
    }
    // Fallback to Uri for device case
    return runCatching { Uri.parse(url).getQueryParameter(key) }.getOrNull()
  }

  private fun parseFromUri(uri: Uri?): JellyfinResolved? {
    if (uri == null) return null
    return parseFromUrl(uri.toString())
  }

  internal fun normalizeUuid(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    // Already dashed UUID
    if (trimmed.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))) {
      return trimmed.lowercase()
    }
    // Undashed 32 hex
    if (trimmed.matches(Regex("[0-9a-fA-F]{32}"))) {
      val lower = trimmed.lowercase()
      return "${lower.substring(0, 8)}-${lower.substring(8, 12)}-${lower.substring(12, 16)}-${lower.substring(16, 20)}-${lower.substring(20, 32)}"
    }
    return null
  }
}
