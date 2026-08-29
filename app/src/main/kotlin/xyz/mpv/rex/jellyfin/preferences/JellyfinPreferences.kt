package xyz.mpv.rex.jellyfin.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class JellyfinPreferences(context: Context) {
  private val appContext = context.applicationContext

  private val encryptedPrefs: SharedPreferences by lazy {
    try {
      val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
      EncryptedSharedPreferences.create(
        appContext,
        "jellyfin_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
    } catch (_: Exception) {
      Log.w("JellyfinPreferences", "EncryptedSharedPreferences failed, falling back to plaintext prefs")
      // Fallback to regular prefs if device doesn't support EncryptedSharedPreferences
      appContext.getSharedPreferences("jellyfin_encrypted_prefs_fallback", Context.MODE_PRIVATE)
    }
  }

  private val regularPrefs: SharedPreferences by lazy {
    appContext.getSharedPreferences("jellyfin_prefs", Context.MODE_PRIVATE)
  }

  // Encrypted values
  var serverUrl: String?
    get() = encryptedPrefs.getString(KEY_SERVER_URL, null)
    set(value) {
      if (value == null) encryptedPrefs.edit().remove(KEY_SERVER_URL).apply()
      else encryptedPrefs.edit().putString(KEY_SERVER_URL, value).apply()
    }

  var userId: String?
    get() = encryptedPrefs.getString(KEY_USER_ID, null)
    set(value) {
      if (value == null) encryptedPrefs.edit().remove(KEY_USER_ID).apply()
      else encryptedPrefs.edit().putString(KEY_USER_ID, value).apply()
    }

  var accessToken: String?
    get() = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    set(value) {
      if (value == null) encryptedPrefs.edit().remove(KEY_ACCESS_TOKEN).apply()
      else encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()
    }

  var deviceId: String
    get() = encryptedPrefs.getString(KEY_DEVICE_ID, null) ?: generateDeviceId().also { deviceId = it }
    set(value) { encryptedPrefs.edit().putString(KEY_DEVICE_ID, value).apply() }

  // Regular prefs (non-sensitive)
  var enableRemote: Boolean
    get() = regularPrefs.getBoolean(KEY_ENABLE_REMOTE, false)
    set(value) { regularPrefs.edit().putBoolean(KEY_ENABLE_REMOTE, value).apply() }

  var deviceName: String
    get() {
      val stored = regularPrefs.getString(KEY_DEVICE_NAME, null)
      if (!stored.isNullOrBlank()) return stored.take(32).replace("\"", "'").replace("\n", " ").trim().take(32).ifBlank { defaultDeviceName() }
      return defaultDeviceName()
    }
    set(value) {
      val trimmed = value.trim().replace("\"", "'").replace("\n", " ").take(32)
      if (trimmed.isBlank()) regularPrefs.edit().remove(KEY_DEVICE_NAME).apply()
      else regularPrefs.edit().putString(KEY_DEVICE_NAME, trimmed).apply()
    }

  private fun defaultDeviceName(): String {
    val model = android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: return "mpvRex"
    return model.take(32)
  }

  var username: String?
    get() = regularPrefs.getString(KEY_USERNAME, null)
    set(value) {
      if (value == null) regularPrefs.edit().remove(KEY_USERNAME).apply()
      else regularPrefs.edit().putString(KEY_USERNAME, value).apply()
    }

  fun isConfigured(): Boolean = !serverUrl.isNullOrBlank() && !userId.isNullOrBlank() && !accessToken.isNullOrBlank()

  fun clearCredentials() {
    encryptedPrefs.edit().clear().apply()
    regularPrefs.edit().remove(KEY_USERNAME).apply()
  }

  fun serverHost(): String? = runCatching {
    val url = serverUrl ?: return null
    val uri = android.net.Uri.parse(url)
    uri.host?.lowercase()
  }.getOrNull()

  fun matchesHost(uri: android.net.Uri?): Boolean {
    if (uri == null) return false
    val configured = serverHost() ?: return false
    return uri.host?.lowercase() == configured
  }

  fun observeEnableRemote(): Flow<Boolean> = regularPrefs.booleanFlow(KEY_ENABLE_REMOTE, false)
  fun observeDeviceName(): Flow<String> = regularPrefs.stringFlow(KEY_DEVICE_NAME, defaultDeviceName())

  private fun generateDeviceId(): String = java.util.UUID.randomUUID().toString()

  private fun SharedPreferences.booleanFlow(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
      if (changedKey == key) trySend(getBoolean(key, default))
    }
    registerOnSharedPreferenceChangeListener(listener)
    trySend(getBoolean(key, default))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
  }.distinctUntilChanged()

  private fun SharedPreferences.stringFlow(key: String, default: String): Flow<String> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
      if (changedKey == key) trySend(getString(key, null) ?: default)
    }
    registerOnSharedPreferenceChangeListener(listener)
    trySend(getString(key, null) ?: default)
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
  }.distinctUntilChanged()

  companion object {
    private const val KEY_SERVER_URL = "jellyfin_server_url"
    private const val KEY_USER_ID = "jellyfin_user_id"
    private const val KEY_ACCESS_TOKEN = "jellyfin_access_token"
    private const val KEY_DEVICE_ID = "jellyfin_device_id"
    private const val KEY_ENABLE_REMOTE = "jellyfin_enable_remote"
    private const val KEY_DEVICE_NAME = "jellyfin_device_name"
    private const val KEY_USERNAME = "jellyfin_username"
  }
}
