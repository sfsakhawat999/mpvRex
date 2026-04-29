package app.marlboroadvance.mpvex.preferences

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

class AdvancedPreferences(
  private val preferenceStore: PreferenceStore,
) {
  val mpvConfStorageUri = preferenceStore.getString("mpv_conf_storage_location_uri")
  val mpvConf = preferenceStore.getString("mpv.conf")
  val inputConf = preferenceStore.getString("input.conf")

  val verboseLogging = preferenceStore.getBoolean("verbose_logging", BuildConfig.BUILD_TYPE != "release")

  val enabledStatisticsPage = preferenceStore.getInt("enabled_stats_page", 0)

  val enableRecentlyPlayed = preferenceStore.getBoolean("enable_recently_played", true)

  val enableMediaInfoActivity = preferenceStore.getBoolean("enable_media_info_activity", true)

  val enableLuaScripts = preferenceStore.getBoolean("enable_lua_scripts", false)
  val selectedLuaScripts = preferenceStore.getStringSet("selected_lua_scripts", emptySet())

  /**
   * Syncs the MediaInfoActivity enabled state with the preference.
   * This affects whether the activity appears in the system intent chooser.
   */
  fun syncMediaInfoActivityStatus(context: Context) {
    try {
      val componentName = ComponentName(context, "app.marlboroadvance.mpvex.MediaInfoIntentHandler")
      val enabled = enableMediaInfoActivity.get()
      val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
      }

      context.packageManager.setComponentEnabledSetting(
        componentName,
        newState,
        PackageManager.DONT_KILL_APP
      )
    } catch (e: Exception) {
      android.util.Log.e("AdvancedPreferences", "Failed to sync MediaInfoActivity status", e)
    }
  }
}
