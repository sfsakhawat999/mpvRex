package xyz.mpv.rex.ui.player.controls

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.ui.player.Decoder
import xyz.mpv.rex.ui.player.Panels
import xyz.mpv.rex.ui.player.Sheets
import xyz.mpv.rex.ui.player.TrackNode
import xyz.mpv.rex.ui.player.controls.components.sheets.AspectRatioSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.AudioTracksSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.ChaptersSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.DecodersSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.FrameNavigationSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.MoreSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.PlaybackSpeedSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.SubtitleDownloadSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.SubtitlesSheet
import xyz.mpv.rex.ui.player.controls.components.sheets.VideoZoomSheet
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject
import androidx.compose.runtime.collectAsState as composeCollectAsState

@Composable
fun PlayerSheets(
  sheetShown: Sheets,
  viewModel: xyz.mpv.rex.ui.player.PlayerViewModel,
  // subtitles sheet
  subtitles: ImmutableList<TrackNode>,
  onAddSubtitle: (Uri) -> Unit,
  onSelectSubtitle: (Int) -> Unit,
  onRemoveSubtitle: (Int) -> Unit,
  // audio sheet
  audioTracks: ImmutableList<TrackNode>,
  onAddAudio: (Uri) -> Unit,
  onSelectAudio: (TrackNode) -> Unit,
  // chapters sheet
  chapter: Segment?,
  chapters: ImmutableList<Segment>,
  onSeekToChapter: (Int) -> Unit,
  // Decoders sheet
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  // Speed sheet
  speed: Float,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetSpeedPresets: () -> Unit,
  onMakeDefaultSpeed: (Float) -> Unit,
  onResetDefaultSpeed: () -> Unit,
  // More sheet
  sleepTimerTimeRemaining: Int,
  onStartSleepTimer: (Int) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  onShowSheet: (Sheets) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val externalSubtitleMetadata by viewModel.externalSubtitleMetadata.composeCollectAsState()

  when (sheetShown) {
    Sheets.None -> {}
    Sheets.SubtitleTracks -> {
      val subtitlesPicker =
        rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocument(),
        ) {
          if (it == null) return@rememberLauncherForActivityResult
          onAddSubtitle(it)
        }
      SubtitlesSheet(
        tracks = subtitles.toImmutableList(),
        onSelect = onSelectSubtitle,
        onAddSubtitle = {
          subtitlesPicker.launch(
            arrayOf(
              "text/plain",
              "text/srt",
              "text/vtt",
              "application/x-subrip",
              "application/x-subtitle",
              "text/x-ssa",
              "*/*", // Fallback for systems that don't recognize subtitle MIME types
            ),
          )
        },
        onRemoveSubtitle = onRemoveSubtitle,
        onOpenSubtitleSettings = { onOpenPanel(Panels.SubtitleSettings) },
        onOpenSubtitleDelay = { onOpenPanel(Panels.SubtitleDelay) },
        onOpenOnlineSearch = { onShowSheet(Sheets.SubDL) },
        onDismissRequest = onDismissRequest,
        externalSubtitleMetadata = externalSubtitleMetadata,
      )
    }

    Sheets.SubDL -> {
      // Get the current media title for search query
      val mediaTitle = viewModel.getCurrentMediaTitle()
      SubtitleDownloadSheet(
        visible = true,
        onDismissRequest = onDismissRequest,
        viewModel = viewModel,
        initialQuery = mediaTitle,
      )
    }

    Sheets.AudioTracks -> {
      val audioPicker =
        rememberLauncherForActivityResult(
          ActivityResultContracts.OpenDocument(),
        ) {
          if (it == null) return@rememberLauncherForActivityResult
          onAddAudio(it)
        }
      AudioTracksSheet(
        tracks = audioTracks,
        onSelect = onSelectAudio,
        onAddAudioTrack = { audioPicker.launch(arrayOf("*/*")) },
        onOpenDelayPanel = { onOpenPanel(Panels.AudioDelay) },
        onDismissRequest,
      )
    }

    Sheets.Chapters -> {
      if (chapter == null) return
      ChaptersSheet(
        chapters,
        currentChapter = chapter,
        onClick = { onSeekToChapter(chapters.indexOf(it)) },
        onDismissRequest,
      )
    }

    Sheets.Decoders -> {
      DecodersSheet(
        selectedDecoder = decoder,
        onSelect = onUpdateDecoder,
        onDismissRequest,
      )
    }

    Sheets.More -> {
      MoreSheet(
        remainingTime = sleepTimerTimeRemaining,
        onStartTimer = onStartSleepTimer,
        onDismissRequest = onDismissRequest,
        onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
      )
    }

    Sheets.PlaybackSpeed -> {
      PlaybackSpeedSheet(
        speed,
        onSpeedChange = onSpeedChange,
        speedPresets = speedPresets,
        onAddSpeedPreset = onAddSpeedPreset,
        onRemoveSpeedPreset = onRemoveSpeedPreset,
        onResetPresets = onResetSpeedPresets,
        onMakeDefault = onMakeDefaultSpeed,
        onResetDefault = onResetDefaultSpeed,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.VideoZoom -> {
      val videoZoom by viewModel.videoZoom.composeCollectAsState()
      VideoZoomSheet(
        videoZoom = videoZoom,
        onSetVideoZoom = viewModel::setVideoZoom,
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.AspectRatios -> {
      val playerPreferences = koinInject<xyz.mpv.rex.preferences.PlayerPreferences>()
      val customRatiosSet by playerPreferences.customAspectRatios.collectAsState()
      val currentRatio by playerPreferences.currentAspectRatio.collectAsState()
      val customRatios =
        customRatiosSet.mapNotNull { str ->
          val parts = str.split("|")
          if (parts.size == 2) {
            xyz.mpv.rex.ui.player.controls.components.sheets.AspectRatio(
              label = parts[0],
              ratio = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
              isCustom = true,
            )
          } else {
            null
          }
        }

      AspectRatioSheet(
        currentRatio = currentRatio.toDouble(),
        customRatios = customRatios,
        onSelectRatio = { ratio ->
          viewModel.setCustomAspectRatio(ratio)
        },
        onAddCustomRatio = { label, ratio ->
          playerPreferences.customAspectRatios.set(customRatiosSet + "$label|$ratio")
          viewModel.setCustomAspectRatio(ratio)
        },
        onDeleteCustomRatio = { ratio ->
          val toRemove = "${ratio.label}|${ratio.ratio}"
          playerPreferences.customAspectRatios.set(customRatiosSet - toRemove)
          // If the deleted ratio is currently active, reset to default
          if (kotlin.math.abs(currentRatio - ratio.ratio) < 0.01) {
            viewModel.setCustomAspectRatio(-1.0)
          }
        },
        onDismissRequest = onDismissRequest,
      )
    }

    Sheets.FrameNavigation -> {
      val currentFrame by viewModel.currentFrame.composeCollectAsState()
      val totalFrames by viewModel.totalFrames.composeCollectAsState()
      FrameNavigationSheet(
        currentFrame = currentFrame,
        totalFrames = totalFrames,
        onUpdateFrameInfo = viewModel::updateFrameInfo,
        onPause = viewModel::pause,
        onUnpause = viewModel::unpause,
        onPauseUnpause = viewModel::pauseUnpause,
        onSeekTo = { position, _ -> viewModel.seekTo(position) },
        onDismissRequest = onDismissRequest,
      )
    }
  }
}
