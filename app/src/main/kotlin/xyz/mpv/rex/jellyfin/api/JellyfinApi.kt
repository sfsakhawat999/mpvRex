package xyz.mpv.rex.jellyfin.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import xyz.mpv.rex.jellyfin.JellyfinTicks
import xyz.mpv.rex.jellyfin.preferences.JellyfinPreferences
import java.net.URLEncoder

class JellyfinApi(
  private val client: OkHttpClient,
  private val json: Json,
  private val preferences: JellyfinPreferences,
) {
  companion object {
    private const val TAG = "JellyfinApi"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val CLIENT_NAME = "mpvRex"
    private const val CLIENT_VERSION = "1.0"
  }

  @Serializable
  data class AuthRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val pw: String,
  )

  @Serializable
  data class AuthResponse(
    @SerialName("AccessToken") val accessToken: String? = null,
    @SerialName("User") val user: AuthUser? = null,
    @SerialName("SessionInfo") val sessionInfo: SessionInfo? = null,
  )

  @Serializable
  data class AuthUser(
    @SerialName("Id") val id: String? = null,
  )

  @Serializable
  data class SessionInfo(
    @SerialName("Id") val id: String? = null,
  )

  @Serializable
  data class JellyfinItem(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("UserData") val userData: UserData? = null,
  )

  @Serializable
  data class UserData(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerialName("Played") val played: Boolean = false,
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
  )

  suspend fun authenticate(serverUrl: String, username: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
    runCatching {
      val normalized = serverUrl.trim().trimEnd('/')
      val deviceId = preferences.deviceId
      val url = "$normalized/Users/AuthenticateByName"
      val body = json.encodeToString(AuthRequest.serializer(), AuthRequest(username, password))
        .toRequestBody(JSON_MEDIA_TYPE)
      val request = Request.Builder()
        .url(url)
        .header("X-Emby-Authorization", buildEmbyAuthHeader(deviceId, null))
        .post(body)
        .build()
      client.newCall(request).execute().use { resp ->
        val text = resp.body?.string() ?: ""
        if (!resp.isSuccessful) {
          throw IllegalStateException("Auth failed ${resp.code}: $text")
        }
        json.decodeFromString(AuthResponse.serializer(), text)
      }
    }
  }

  suspend fun getItem(itemId: String): Result<JellyfinItem> = withContext(Dispatchers.IO) {
    runCatching {
      val prefs = preferences
      val server = prefs.serverUrl?.trimEnd('/') ?: throw IllegalStateException("No server configured")
      val userId = prefs.userId ?: throw IllegalStateException("No userId")
      val token = prefs.accessToken ?: throw IllegalStateException("No token")
      val url = "$server/Users/$userId/Items/$itemId"
      val request = Request.Builder()
        .url(url)
        .header("X-Emby-Token", token)
        .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
        .get()
        .build()
      client.newCall(request).execute().use { resp ->
        val text = resp.body?.string() ?: ""
        if (!resp.isSuccessful) throw IllegalStateException("getItem ${resp.code}: $text")
        json.decodeFromString(JellyfinItem.serializer(), text)
      }
    }
  }

  suspend fun reportPlaying(
    itemId: String,
    mediaSourceId: String?,
    playSessionId: String,
    positionTicks: Long,
    isPaused: Boolean,
    canSeek: Boolean = true,
  ): Result<Unit> = postPlayback("/Sessions/Playing", itemId, mediaSourceId, playSessionId, positionTicks, isPaused, canSeek)

  suspend fun reportProgress(
    itemId: String,
    mediaSourceId: String?,
    playSessionId: String,
    positionTicks: Long,
    isPaused: Boolean,
    isMuted: Boolean = false,
  ): Result<Unit> = postPlayback("/Sessions/Playing/Progress", itemId, mediaSourceId, playSessionId, positionTicks, isPaused, true, isMuted)

  suspend fun reportStopped(
    itemId: String,
    mediaSourceId: String?,
    playSessionId: String,
    positionTicks: Long,
  ): Result<Unit> = postPlayback("/Sessions/Playing/Stopped", itemId, mediaSourceId, playSessionId, positionTicks, false, true)

  private suspend fun postPlayback(
    path: String,
    itemId: String,
    mediaSourceId: String?,
    playSessionId: String,
    positionTicks: Long,
    isPaused: Boolean,
    canSeek: Boolean = true,
    isMuted: Boolean = false,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val prefs = preferences
      val server = prefs.serverUrl?.trimEnd('/') ?: throw IllegalStateException("No server")
      val token = prefs.accessToken ?: throw IllegalStateException("No token")
      val url = "$server$path"
      // Build minimal JSON manually to avoid null serialization issues
      val payload = buildString {
        append("{")
        append("\"ItemId\":\"$itemId\"")
        if (mediaSourceId != null) append(",\"MediaSourceId\":\"$mediaSourceId\"")
        append(",\"PlaySessionId\":\"$playSessionId\"")
        append(",\"PositionTicks\":$positionTicks")
        append(",\"IsPaused\":$isPaused")
        append(",\"IsMuted\":$isMuted")
        append(",\"CanSeek\":$canSeek")
        append(",\"PlayMethod\":\"DirectPlay\"")
        append("}")
      }
      val request = Request.Builder()
        .url(url)
        .header("X-Emby-Token", token)
        .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
        .post(payload.toRequestBody(JSON_MEDIA_TYPE))
        .build()
      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          val text = resp.body?.string() ?: ""
          throw IllegalStateException("$path ${resp.code}: $text")
        }
      }
    }.onFailure { e ->
      Log.w(TAG, "post $path failed: ${e.message}")
    }
  }

  suspend fun markPlayed(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val prefs = preferences
      val server = prefs.serverUrl?.trimEnd('/') ?: throw IllegalStateException("No server")
      val token = prefs.accessToken ?: throw IllegalStateException("No token")
      val userId = prefs.userId ?: throw IllegalStateException("No userId")
      // POST /Users/{userId}/PlayedItems/{itemId} is current, fallback to /UserPlayedItems/{itemId}
      val url = "$server/Users/$userId/PlayedItems/$itemId"
      val request = Request.Builder()
        .url(url)
        .header("X-Emby-Token", token)
        .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
        .post("".toRequestBody(JSON_MEDIA_TYPE))
        .build()
      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          val text = resp.body?.string() ?: ""
          // Try legacy endpoint
          if (resp.code == 404) {
            val legacyUrl = "$server/UserPlayedItems/$itemId"
            val legacyReq = Request.Builder().url(legacyUrl)
              .header("X-Emby-Token", token)
              .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
              .post("".toRequestBody(JSON_MEDIA_TYPE)).build()
            client.newCall(legacyReq).execute().use { lr ->
              if (!lr.isSuccessful) throw IllegalStateException("markPlayed ${lr.code}: ${lr.body?.string()}")
            }
          } else {
            throw IllegalStateException("markPlayed ${resp.code}: $text")
          }
        }
      }
    }
  }

  suspend fun markUnplayed(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val prefs = preferences
      val server = prefs.serverUrl?.trimEnd('/') ?: throw IllegalStateException("No server")
      val token = prefs.accessToken ?: throw IllegalStateException("No token")
      val userId = prefs.userId ?: throw IllegalStateException("No userId")
      val url = "$server/Users/$userId/PlayedItems/$itemId"
      val request = Request.Builder()
        .url(url)
        .header("X-Emby-Token", token)
        .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
        .delete()
        .build()
      client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
          val text = resp.body?.string() ?: ""
          if (resp.code == 404) {
            val legacyUrl = "$server/UserPlayedItems/$itemId"
            val legacyReq = Request.Builder().url(legacyUrl)
              .header("X-Emby-Token", token)
              .header("X-Emby-Authorization", buildEmbyAuthHeader(prefs.deviceId, token))
              .delete().build()
            client.newCall(legacyReq).execute().use { lr ->
              if (!lr.isSuccessful) throw IllegalStateException("markUnplayed ${lr.code}: ${lr.body?.string()}")
            }
          } else throw IllegalStateException("markUnplayed ${resp.code}: $text")
        }
      }
    }
  }

  private fun buildEmbyAuthHeader(deviceId: String, token: String?): String = buildString {
    append("MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$CLIENT_NAME\", DeviceId=\"$deviceId\", Version=\"$CLIENT_VERSION\"")
    if (token != null) append(", Token=\"$token\"")
  }
}
