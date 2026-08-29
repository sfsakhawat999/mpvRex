package xyz.mpv.rex.jellyfin.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import xyz.mpv.rex.jellyfin.preferences.JellyfinPreferences

class JellyfinRemoteClient(
  private val okHttp: OkHttpClient,
  private val json: Json,
  private val prefs: JellyfinPreferences,
  private val appContext: android.content.Context? = null,
) {
  // Optional callback for UI to handle Play (if set, client delegates instead of auto-launch)
  var onPlayCommand: ((PlayData) -> Unit)? = null
  data class PlayData(
    val itemIds: List<String>,
    val startPositionTicks: Long?,
    val playCommand: String?,
    val mediaSourceId: String?,
    val audioStreamIndex: Int?,
    val subtitleStreamIndex: Int?,
  )
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  @Volatile private var ws: WebSocket? = null
  @Volatile private var wsJob: Job? = null
  @Volatile private var capabilitiesJob: Job? = null

  companion object {
    private const val TAG = "JellyfinRemote"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
  }

  @Volatile private var isManuallyDisconnected = false
  private var lastServer: String? = null
  private var lastDeviceId: String? = null
  private var lastDeviceName: String? = null

  fun ensureRegistered() {
    if (!prefs.isConfigured()) {
      Log.w(TAG, "not configured — cannot register")
      return
    }
    if (!prefs.enableRemote) {
      Log.d(TAG, "remote disabled — skip")
      return
    }
    // Idempotent: if already connected to same server/device/name, just refresh capabilities
    val server = prefs.serverUrl?.trimEnd('/') ?: return
    val deviceId = prefs.deviceId
    val deviceName = prefs.deviceName
    if (ws != null && lastServer == server && lastDeviceId == deviceId && lastDeviceName == deviceName) {
      Log.d(TAG, "already connected to $server — refresh capabilities only")
      capabilitiesJob?.cancel()
      capabilitiesJob = scope.launch {
        val token = prefs.accessToken ?: return@launch
        postCapabilities(server, token, deviceId)
      }
      return
    }
    isManuallyDisconnected = false
    capabilitiesJob?.cancel()
    capabilitiesJob = scope.launch {
      val token = prefs.accessToken ?: return@launch
      Log.d(TAG, "ensureRegistered server=$server deviceId=${deviceId.take(8)} hasToken=${token.isNotBlank()}")
      val ok = postCapabilities(server, token, deviceId)
      if (ok) connectWebSocket(server, token, deviceId)
    }
  }

  fun disconnect() {
    isManuallyDisconnected = true
    capabilitiesJob?.cancel()
    wsJob?.cancel()
    ws?.close(1000, "disconnect")
    ws = null
    lastServer = null
    lastDeviceId = null
    lastDeviceName = null
  }

  private suspend fun postCapabilities(server: String, token: String, deviceId: String): Boolean = withContext(Dispatchers.IO) {
    // ClientCapabilitiesDto Full
    val body = """
      {
        "PlayableMediaTypes": ["Video","Audio"],
        "SupportedCommands": ["Play","PlayState","PlayNext","SetVolume","SetAudioStreamIndex","SetSubtitleStreamIndex"],
        "SupportsMediaControl": true,
        "SupportsPersistentIdentifier": true,
        "DeviceProfile": null
      }
    """.trimIndent().toRequestBody(JSON_MEDIA)

    val auth = buildAuth(token, deviceId)
    val req = Request.Builder()
      .url("$server/Sessions/Capabilities/Full")
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .post(body)
      .build()
    runCatching {
      okHttp.newCall(req).execute().use { resp: Response ->
        val b = resp.body?.string() ?: ""
        if (!resp.isSuccessful) {
          Log.w(TAG, "Capabilities POST ${resp.code} $b")
          false
        } else {
          Log.d(TAG, "Capabilities POST 204 SupportsMediaControl=true")
          true
        }
      }
    }.onFailure { e -> Log.w(TAG, "Capabilities failed: ${e.message}") }.getOrDefault(false)
  }

  private fun buildAuth(token: String, deviceId: String): String {
    val deviceName = prefs.deviceName.replace("\"", "'")
    return "MediaBrowser Client=\"mpvRex\", Device=\"$deviceName\", DeviceId=\"$deviceId\", Version=\"0.1\", Token=\"$token\""
  }

  private fun connectWebSocket(server: String, token: String, deviceId: String) {
    if (ws != null) {
      Log.d(TAG, "WS already exists — skip new connect, will reuse")
      return
    }
    lastServer = server
    lastDeviceId = deviceId
    lastDeviceName = prefs.deviceName
    val wsScheme = if (server.startsWith("https")) "wss" else "ws"
    val hostPart = server.removePrefix("https://").removePrefix("http://").trimEnd('/')
    val url = "$wsScheme://$hostPart/socket"
    val auth = buildAuth(token, deviceId)
    val req = Request.Builder()
      .url(url)
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .build()
    Log.d(TAG, "WS connecting $wsScheme://$hostPart/socket")
    ws = okHttp.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WS open $url code=${response.code}")
      }
      override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "WS message $text")
        handleMessage(webSocket, text)
      }
      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d(TAG, "WS bytes ${bytes.hex()}")
      }
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WS closing $code $reason")
        webSocket.close(code, reason)
      }
      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "WS closed $code $reason")
        if (webSocket === ws) ws = null
        if (!isManuallyDisconnected) scheduleReconnect(server, token, deviceId)
      }
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.w(TAG, "WS failure ${t.message} code=${response?.code}")
        if (webSocket === ws) ws = null
        if (!isManuallyDisconnected) scheduleReconnect(server, token, deviceId)
      }
    })
    // KeepAlive every 30s (client → server)
    wsJob?.cancel()
    wsJob = scope.launch {
      while (true) {
        delay(30_000)
        val cur = ws ?: break
        val keepAlive = """{"MessageType":"KeepAlive"}"""
        cur.send(keepAlive)
        Log.d(TAG, "WS KeepAlive sent")
      }
    }
  }

  private fun scheduleReconnect(server: String, token: String, deviceId: String) {
    // ws already nulled in onClosed/onFailure, just schedule
    scope.launch {
      delay(reconnectDelayMs.toLong())
      if (!isManuallyDisconnected && prefs.enableRemote && prefs.isConfigured() && ws == null) {
        Log.d(TAG, "WS reconnect after ${reconnectDelayMs}ms")
        connectWebSocket(server, token, deviceId)
        // Successful connect attempt — reset backoff
        reconnectDelayMs = 5_000L
      } else {
        Log.d(TAG, "WS reconnect skipped isManuallyDisconnected=$isManuallyDisconnected wsExists=${ws != null}")
        // Failed attempt — increase backoff, capped at 60s
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, 60_000L)
      }
    }
  }

  private fun handleMessage(webSocket: WebSocket, text: String) {
    runCatching {
      val obj = json.parseToJsonElement(text).jsonObject
      val type = obj["MessageType"]?.jsonPrimitive?.content ?: return
      val messageId = obj["MessageId"]?.jsonPrimitive?.content ?: ""
      val data = obj["Data"]
      when (type) {
        "ForceKeepAlive" -> {
          // Server asks for keepalive with interval in Data (e.g. 60)
          Log.d(TAG, "ForceKeepAlive received Data=$data — replying KeepAlive")
          webSocket.send("""{"MessageType":"KeepAlive"}""")
        }
        "KeepAlive" -> { /* server echo */ }
        "Play" -> {
          // Data: {ItemIds, StartPositionTicks, PlayCommand, ControllingUserId, MediaSourceId, AudioStreamIndex, SubtitleStreamIndex, StartIndex}
          Log.d("JellyfinRemote", "Play received MessageId=$messageId Data=$data")
          data?.let { objData ->
            val itemIds = objData.jsonObject["ItemIds"]?.let { arr ->
              runCatching { arr.toString() }.getOrNull() } ?: "[]"
            val startTicksStr = objData.jsonObject["StartPositionTicks"]?.jsonPrimitive?.content ?: "null"
            val playCommand = objData.jsonObject["PlayCommand"]?.jsonPrimitive?.content ?: "null"
            val mediaSourceId = objData.jsonObject["MediaSourceId"]?.jsonPrimitive?.content?.takeIf { it != "null" }
            val audioIdx = objData.jsonObject["AudioStreamIndex"]?.jsonPrimitive?.content ?: "null"
            val subIdx = objData.jsonObject["SubtitleStreamIndex"]?.jsonPrimitive?.content ?: "null"
            Log.d("JellyfinRemote", "Play parsed ItemIds=$itemIds StartPositionTicks=$startTicksStr PlayCommand=$playCommand MediaSourceId=$mediaSourceId Audio=$audioIdx Sub=$subIdx")
            // Build PlayData for callback (proper JsonArray)
            val ids = runCatching {
              val elem = objData.jsonObject["ItemIds"]
              when {
                elem is kotlinx.serialization.json.JsonArray -> elem.map { it.jsonPrimitive.content }
                elem != null -> listOf(elem.jsonPrimitive.content)
                else -> emptyList()
              }
            }.getOrDefault(emptyList())
            val ticks = objData.jsonObject["StartPositionTicks"]?.jsonPrimitive?.content?.toLongOrNull()
            val mediaId = objData.jsonObject["MediaSourceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val aIdx = objData.jsonObject["AudioStreamIndex"]?.jsonPrimitive?.content?.toIntOrNull()
            val sIdx = objData.jsonObject["SubtitleStreamIndex"]?.jsonPrimitive?.content?.toIntOrNull()
            val playData = PlayData(ids, ticks, playCommand.takeIf { it != "null" }, mediaId, aIdx, sIdx)
            if (onPlayCommand != null) {
              Log.d(TAG, "Dispatching Play to onPlayCommand $playData")
              onPlayCommand?.invoke(playData)
            } else {
              // Fallback: auto-launch via Intent
              handleRemotePlayAuto(playData)
            }
          }
        }
        "Playstate" -> {
          Log.d("JellyfinRemote", "Playstate received $data")
          handlePlaystate(data)
        }
        "GeneralCommand" -> {
          Log.d("JellyfinRemote", "GeneralCommand received $data")
          handleGeneralCommand(data)
        }
        else -> Log.d("JellyfinRemote", "Other message Type=$type Data=$data")
      }
    }.onFailure { e -> Log.w("JellyfinRemote", "Failed to parse WS message: ${e.message} text=$text") }
  }

  private fun handlePlaystate(data: kotlinx.serialization.json.JsonElement?) {
    if (data == null) return
    val obj = runCatching { data.jsonObject }.getOrNull() ?: return
    val command = obj["Command"]?.jsonPrimitive?.content ?: obj["command"]?.jsonPrimitive?.content ?: return
    val seekTicks = obj["SeekPositionTicks"]?.jsonPrimitive?.content?.toLongOrNull()
    Log.d(TAG, "Playstate command=$command seekTicks=$seekTicks")
    val activity = xyz.mpv.rex.ui.player.PlayerActivity.activeInstance
    if (activity == null && command != "Stop") {
      Log.w(TAG, "No active PlayerActivity for Playstate $command")
      return
    }
    activity?.runOnUiThread {
      when (command) {
        "Stop" -> {
          // Report Stopped before finishing so dashboard clears
          val posSec = runCatching { MPVLib.getPropertyDouble("time-pos") ?: 0.0 }.getOrDefault(0.0)
          val ticks = (posSec * 10_000_000L).toLong()
          currentRemoteItemId?.let { itemId ->
            val server = prefs.serverUrl?.trimEnd('/') ?: return@runOnUiThread
            val token = prefs.accessToken ?: return@runOnUiThread
            val deviceId = prefs.deviceId
            scope.launch { reportStopped(server, token, deviceId, itemId, currentRemoteMediaSourceId, ticks) }
          }
          activity.finish()
        }
        "Pause" -> {
          runCatching { MPVLib.setPropertyBoolean("pause", true) }
          reportRemoteProgress(isPaused = true)
        }
        "Unpause", "Play", "Resume" -> {
          runCatching { MPVLib.setPropertyBoolean("pause", false) }
          reportRemoteProgress(isPaused = false)
        }
        "PlayPause" -> runCatching {
          val paused = MPVLib.getPropertyBoolean("pause") ?: false
          MPVLib.setPropertyBoolean("pause", !paused)
          reportRemoteProgress(isPaused = !paused)
        }
        "Seek", "SeekTo" -> {
          seekTicks?.let { t ->
            val sec = (t / 10_000_000L).toInt()
            Log.d(TAG, "Seek to $sec sec ($t ticks)")
            runCatching { MPVLib.setPropertyInt("time-pos", sec) }
            runCatching { MPVLib.command("seek", sec.toString(), "absolute") }
            // Report new position immediately
            scope.launch {
              val server = prefs.serverUrl?.trimEnd('/') ?: return@launch
              val token = prefs.accessToken ?: return@launch
              val deviceId = prefs.deviceId
              val itemId = currentRemoteItemId ?: return@launch
              reportProgress(server, token, deviceId, itemId, currentRemoteMediaSourceId, t, false)
            }
          }
        }
        "NextTrack", "Next" -> runCatching { activity.playNext() }
        "PreviousTrack", "Previous" -> runCatching { activity.playPrevious() }
        else -> Log.d(TAG, "Unhandled Playstate $command")
      }
    }
    // For Stop without activity, still report
    if (command == "Stop" && activity == null) {
      currentRemoteItemId?.let { itemId ->
        val server = prefs.serverUrl?.trimEnd('/') ?: return
        val token = prefs.accessToken ?: return
        val deviceId = prefs.deviceId
        scope.launch { reportStopped(server, token, deviceId, itemId, currentRemoteMediaSourceId, 0L) }
      }
    }
  }

  private fun reportRemoteProgress(isPaused: Boolean) {
    val server = prefs.serverUrl?.trimEnd('/') ?: return
    val token = prefs.accessToken ?: return
    val deviceId = prefs.deviceId
    val itemId = currentRemoteItemId ?: return
    val posSec = runCatching { MPVLib.getPropertyDouble("time-pos") ?: 0.0 }.getOrDefault(0.0)
    val ticks = (posSec * 10_000_000L).toLong()
    scope.launch { reportProgress(server, token, deviceId, itemId, currentRemoteMediaSourceId, ticks, isPaused) }
  }

  fun onPlayerFinished() {
    val itemId = currentRemoteItemId ?: return
    val server = prefs.serverUrl?.trimEnd('/') ?: return
    val token = prefs.accessToken ?: return
    val deviceId = prefs.deviceId
    val posSec = runCatching { MPVLib.getPropertyDouble("time-pos") ?: 0.0 }.getOrDefault(0.0)
    val ticks = (posSec * 10_000_000L).toLong()
    Log.d(TAG, "Player finished, reporting Stopped ticks=$ticks")
    scope.launch { reportStopped(server, token, deviceId, itemId, currentRemoteMediaSourceId, ticks) }
  }

  private fun handleGeneralCommand(data: kotlinx.serialization.json.JsonElement?) {
    if (data == null) return
    val obj = runCatching { data.jsonObject }.getOrNull() ?: return
    val name = obj["Name"]?.jsonPrimitive?.content ?: obj["name"]?.jsonPrimitive?.content ?: return
    Log.d(TAG, "GeneralCommand $name args=${obj["Arguments"]}")
    val activity = xyz.mpv.rex.ui.player.PlayerActivity.activeInstance
    when (name) {
      "SetVolume" -> {
        val vol = obj["Arguments"]?.jsonObject?.get("Volume")?.jsonPrimitive?.content?.toIntOrNull() ?: return
        activity?.runOnUiThread { runCatching { MPVLib.setPropertyInt("volume", vol) } }
      }
      "Mute", "ToggleMute" -> {
        activity?.runOnUiThread { runCatching { MPVLib.command("cycle", "mute") } }
      }
      "Unmute" -> activity?.runOnUiThread { runCatching { MPVLib.setPropertyBoolean("mute", false) } }
      "SetAudioStreamIndex" -> {
        val idx = obj["Arguments"]?.jsonObject?.get("Index")?.jsonPrimitive?.content?.toIntOrNull() ?: return
        activity?.runOnUiThread { runCatching { activity.player.aid = idx } }
      }
      "SetSubtitleStreamIndex" -> {
        val idx = obj["Arguments"]?.jsonObject?.get("Index")?.jsonPrimitive?.content?.toIntOrNull() ?: return
        activity?.runOnUiThread { runCatching { activity.player.sid = idx } }
      }
      "DisplayMessage" -> {
        val text = obj["Arguments"]?.jsonObject?.get("Text")?.jsonPrimitive?.content ?: obj["Arguments"]?.jsonObject?.get("Header")?.jsonPrimitive?.content ?: name
        activity?.runOnUiThread { android.widget.Toast.makeText(activity, text, android.widget.Toast.LENGTH_SHORT).show() }
      }
      else -> Log.d(TAG, "GeneralCommand unhandled $name")
    }
  }

  private fun handleRemotePlayAuto(playData: PlayData) {
    val ctx = appContext ?: run {
      Log.w(TAG, "No appContext for auto Play")
      return
    }
    val server = prefs.serverUrl?.trimEnd('/') ?: run {
      Log.w(TAG, "No serverUrl for Play")
      return
    }
    val token = prefs.accessToken ?: run {
      Log.w(TAG, "No token for Play")
      return
    }
    val itemId = playData.itemIds.firstOrNull() ?: run {
      Log.w(TAG, "Play with empty ItemIds")
      return
    }
    val ticks = playData.startPositionTicks ?: 0L
    val ms = ticks / 10_000L
    val mediaSourceId = playData.mediaSourceId
    scope.launch {
      // Fetch episode/movie name for player title
      val title = fetchItemName(server, token, itemId) ?: "Jellyfin Remote Play"
      val url = buildString {
        append("$server/Videos/$itemId/stream?static=true")
        if (!mediaSourceId.isNullOrBlank()) append("&mediaSourceId=$mediaSourceId")
        append("&api_key=$token")
      }
      Log.d(TAG, "Auto Play launch itemId=${itemId.take(8)} ticks=$ticks ms=$ms title=$title")
      try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
          setDataAndType(android.net.Uri.parse(url), "video/*")
          putExtra("position", ms.toInt())
          putExtra("title", title)
          addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        Log.d(TAG, "Started PlayerActivity for remote Play title=$title")
      } catch (e: Exception) {
        Log.w(TAG, "Failed to start PlayerActivity: ${e.message}")
      }
      // Report Playing so dashboard shows Now Playing under mpvRex's own session
      val deviceId = prefs.deviceId
      val ok = reportPlaying(server, token, deviceId, itemId, mediaSourceId, ticks)
      Log.d(TAG, "Remote reportPlaying ${if (ok) "OK" else "failed"}")
    }
  }

  private suspend fun fetchItemName(server: String, token: String, itemId: String): String? = withContext(Dispatchers.IO) {
    val userId = prefs.userId ?: return@withContext null
    val url = "$server/Users/$userId/Items/$itemId"
    val auth = buildAuth(token, prefs.deviceId)
    val req = Request.Builder()
      .url(url)
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .get()
      .build()
    runCatching {
      okHttp.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@use null
        val body = resp.body?.string() ?: return@use null
        // Use proper JSON parsing so \u0026 -> & and only characters remain
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return@use null
        val name = obj["Name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val series = obj["SeriesName"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val season = obj["ParentIndexNumber"]?.jsonPrimitive?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }?.takeIf { it.isNotBlank() } ?: obj["ParentIndexNumber"]?.toString()?.trim()
        val episode = obj["IndexNumber"]?.jsonPrimitive?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }?.takeIf { it.isNotBlank() } ?: obj["IndexNumber"]?.toString()?.trim()
        // For series, prefer full episode display: Series - Sxx:Eyy - Name
        when {
          !series.isNullOrBlank() && !season.isNullOrBlank() && !episode.isNullOrBlank() && !name.isNullOrBlank() -> {
            val s = season.padStart(2, '0')
            val e = episode.padStart(2, '0')
            "$series - S${s}:E${e} - $name"
          }
          !name.isNullOrBlank() -> name
          else -> null
        }
      }
    }.getOrNull()
  }

  private var remoteProgressJob: Job? = null
  @Volatile private var currentRemoteItemId: String? = null
  @Volatile private var currentRemoteMediaSourceId: String? = null
  private var reconnectDelayMs: Long = 5_000L

  private fun startRemoteProgress(server: String, token: String, deviceId: String, itemId: String, mediaSourceId: String?) {
    currentRemoteItemId = itemId
    currentRemoteMediaSourceId = mediaSourceId
    remoteProgressJob?.cancel()
    remoteProgressJob = scope.launch {
      while (true) {
        delay(10_000)
        val posSec = runCatching { MPVLib.getPropertyDouble("time-pos") ?: 0.0 }.getOrDefault(0.0)
        val ticks = (posSec * 10_000_000L).toLong()
        val paused = runCatching { MPVLib.getPropertyBoolean("pause") ?: false }.getOrDefault(false)
        val ok = reportProgress(server, token, deviceId, itemId, mediaSourceId, ticks, paused)
        Log.d(TAG, "Remote Progress ticks=$ticks paused=$paused ${if (ok) "OK" else "failed"}")
        if (!ok) {
          // keep trying
        }
      }
    }
  }

  private fun stopRemoteProgress() {
    remoteProgressJob?.cancel()
    remoteProgressJob = null
    currentRemoteItemId = null
    currentRemoteMediaSourceId = null
  }

  private suspend fun reportPlaying(server: String, token: String, deviceId: String, itemId: String, mediaSourceId: String?, positionTicks: Long): Boolean {
    startRemoteProgress(server, token, deviceId, itemId, mediaSourceId)
    val auth = buildAuth(token, deviceId)
    val body = """
      {
        "ItemId": "$itemId",
        "MediaSourceId": ${if (mediaSourceId != null) "\"$mediaSourceId\"" else "null"},
        "PositionTicks": $positionTicks,
        "IsPaused": false,
        "IsMuted": false,
        "CanSeek": true,
        "PlayMethod": "DirectPlay"
      }
    """.trimIndent().toRequestBody(JSON_MEDIA)
    val req = Request.Builder()
      .url("$server/Sessions/Playing")
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .post(body)
      .build()
    return runCatching {
      withContext(Dispatchers.IO) {
        okHttp.newCall(req).execute().use { resp ->
          val b = resp.body?.string() ?: ""
          if (!resp.isSuccessful) {
            Log.w(TAG, "reportPlaying ${resp.code} $b")
            false
          } else {
            Log.d(TAG, "reportPlaying 204 item=${itemId.take(8)} ticks=$positionTicks")
            true
          }
        }
      }
    }.onFailure { e -> Log.w(TAG, "reportPlaying failed: ${e.message}") }.getOrDefault(false)
  }

  private suspend fun reportProgress(server: String, token: String, deviceId: String, itemId: String, mediaSourceId: String?, positionTicks: Long, isPaused: Boolean): Boolean {
    val auth = buildAuth(token, deviceId)
    val body = """
      {
        "ItemId": "$itemId",
        "MediaSourceId": ${if (mediaSourceId != null) "\"$mediaSourceId\"" else "null"},
        "PositionTicks": $positionTicks,
        "IsPaused": $isPaused,
        "IsMuted": false,
        "CanSeek": true,
        "PlayMethod": "DirectPlay"
      }
    """.trimIndent().toRequestBody(JSON_MEDIA)
    val req = Request.Builder()
      .url("$server/Sessions/Playing/Progress")
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .post(body)
      .build()
    return runCatching {
      withContext(Dispatchers.IO) {
        okHttp.newCall(req).execute().use { resp ->
          val b = resp.body?.string() ?: ""
          if (!resp.isSuccessful) {
            Log.w(TAG, "reportProgress ${resp.code} $b")
            false
          } else {
            Log.d(TAG, "reportProgress 204 ticks=$positionTicks paused=$isPaused")
            true
          }
        }
      }
    }.onFailure { e -> Log.w(TAG, "reportProgress failed: ${e.message}") }.getOrDefault(false)
  }

  private suspend fun reportStopped(server: String, token: String, deviceId: String, itemId: String, mediaSourceId: String?, positionTicks: Long): Boolean {
    stopRemoteProgress()
    val auth = buildAuth(token, deviceId)
    val body = """
      {
        "ItemId": "$itemId",
        "MediaSourceId": ${if (mediaSourceId != null) "\"$mediaSourceId\"" else "null"},
        "PositionTicks": $positionTicks,
        "CanSeek": true,
        "IsPaused": false,
        "IsMuted": false,
        "PlayMethod": "DirectPlay"
      }
    """.trimIndent().toRequestBody(JSON_MEDIA)
    val req = Request.Builder()
      .url("$server/Sessions/Playing/Stopped")
      .header("X-Emby-Authorization", auth)
      .header("X-Emby-Token", token)
      .post(body)
      .build()
    return runCatching {
      withContext(Dispatchers.IO) {
        okHttp.newCall(req).execute().use { resp ->
          val b = resp.body?.string() ?: ""
          if (!resp.isSuccessful) {
            Log.w(TAG, "reportStopped ${resp.code} $b")
            false
          } else {
            Log.d(TAG, "reportStopped 204 ticks=$positionTicks")
            true
          }
        }
      }
    }.onFailure { e -> Log.w(TAG, "reportStopped failed: ${e.message}") }.getOrDefault(false)
  }
}
