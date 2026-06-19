package xyz.mpv.rex.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.preference.PreferenceStore
import xyz.mpv.rex.preferences.preference.getEnum
import xyz.mpv.rex.ui.theme.AppTheme
import xyz.mpv.rex.ui.theme.DarkMode
import xyz.mpv.rex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

class AppearancePreferences(
  preferenceStore: PreferenceStore,
) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val appTheme = preferenceStore.getEnum("app_theme", AppTheme.Default)
  val materialYou = preferenceStore.getBoolean("material_you", true)
  val amoledMode = preferenceStore.getBoolean("amoled_mode", false)
  val matchPlayerControlsToTheme = preferenceStore.getBoolean("match_player_controls_to_theme", false)
  val useSystemFont = preferenceStore.getBoolean("use_system_font", false)
  val unlimitedNameLines = preferenceStore.getBoolean("unlimited_name_lines", false)
  val hidePlayerButtonsBackground = preferenceStore.getBoolean("hide_player_buttons_background", false)
  val enableBounceAnimation = preferenceStore.getBoolean("enable_bounce_animation", false)
  val showCommunityIcon = preferenceStore.getBoolean("show_community_icon", true)

  val showHiddenFiles = preferenceStore.getBoolean("show_hidden_files", false)
  val showUnplayedOldVideoLabel = preferenceStore.getBoolean("show_unplayed_old_video_label", true)
  val unplayedOldVideoDays = preferenceStore.getInt("unplayed_old_video_days", 7)
  val showNetworkThumbnails = preferenceStore.getBoolean("show_network_thumbnails", false)
  val seekbarStyle = preferenceStore.getEnum("seekbar_style", SeekbarStyle.Wavy)
  val playerAlwaysDarkMode = preferenceStore.getBoolean("player_always_dark_mode", true)

  val thumbnailStrategy = preferenceStore.getEnum("thumbnail_strategy", ThumbnailStrategy.FirstFrame)
  val thumbnailPositionPercent = preferenceStore.getInt("thumbnail_position_percent", THUMBNAIL_POSITION_DEFAULT)

  companion object {
      const val THUMBNAIL_POSITION_DEFAULT = 30
  }

  val topLeftControls =
    preferenceStore.getString(
      "top_left_controls",
      "BACK_ARROW,VIDEO_TITLE",
    )

  val topRightControls =
    preferenceStore.getString(
      "top_right_controls",
      "CURRENT_CHAPTER,DECODER,AUDIO_TRACK,SUBTITLES,AMBIENT_MODE,MORE_OPTIONS",
    )

  val bottomRightControls =
    preferenceStore.getString(
      "bottom_right_controls",
      "FRAME_NAVIGATION,VIDEO_ZOOM,PICTURE_IN_PICTURE,ASPECT_RATIO",
    )

  val bottomLeftControls =
    preferenceStore.getString(
      "bottom_left_controls",
      "BACKGROUND_PLAYBACK,LOCK_CONTROLS,SCREEN_ROTATION,PLAYBACK_SPEED,REPEAT_MODE,SHUFFLE,AB_LOOP",
    )

  val portraitBottomControls =
    preferenceStore.getString(
      "portrait_bottom_controls",
      "DECODER,AUDIO_TRACK,SUBTITLES,PLAYBACK_SPEED,REPEAT_MODE,SHUFFLE,SCREEN_ROTATION,LOCK_CONTROLS,BACKGROUND_PLAYBACK,PICTURE_IN_PICTURE,ASPECT_RATIO,MORE_OPTIONS",
    )

  val moreSheetControls =
    preferenceStore.getString(
      "more_sheet_controls",
      "SCREEN_ROTATION,DECODER,AUDIO_TRACK,SUBTITLES,PLAYBACK_SPEED,REPEAT_MODE,SHUFFLE,VIDEO_ZOOM,FRAME_NAVIGATION,ASPECT_RATIO,PICTURE_IN_PICTURE,LOCK_CONTROLS",
    )

  fun parseButtons(
    csv: String,
    usedButtons: MutableSet<PlayerButton>,
  ): List<PlayerButton> =
    csv
      .splitToSequence(',')
      .map { it.trim().uppercase() }
      .mapNotNull { name ->
        try {
          PlayerButton.valueOf(name)
        } catch (_: IllegalArgumentException) {
          null
        }
      }.filter { it != PlayerButton.NONE }
      .filter { usedButtons.add(it) }
      .toList()
}

enum class ThumbnailStrategy {
  FirstFrame,
  Position
}

@Composable
fun MultiChoiceSegmentedButton(
  choices: ImmutableList<String>,
  selectedIndices: ImmutableList<Int>,
  onClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  MultiChoiceSegmentedButtonRow(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
  ) {
    choices.forEachIndexed { index, choice ->
      SegmentedButton(
        checked = selectedIndices.contains(index),
        onCheckedChange = { onClick(index) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
      ) {
        Text(text = choice)
      }
    }
  }
}
