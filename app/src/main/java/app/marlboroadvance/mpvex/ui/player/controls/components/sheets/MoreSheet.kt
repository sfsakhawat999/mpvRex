package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.getPlayerButtonLabel
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.controls.RenderPlayerButton
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
  remainingTime: Int,
  onStartTimer: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  onEnterFiltersPanel: () -> Unit,
  onAnime4KChanged: () -> Unit = {},
  viewModel: PlayerViewModel,
  onShowSheet: (Sheets) -> Unit,
  modifier: Modifier = Modifier,
) {
  val lastTab by viewModel.lastMoreSheetTab.collectAsState()
  val pagerState = rememberPagerState(initialPage = lastTab) { 2 }
  val scope = rememberCoroutineScope()

  // Persist tab change
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }.collect {
      viewModel.lastMoreSheetTab.value = it
    }
  }

  val tabs = listOf("Settings", "Controls")

  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {
      PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = Color.Transparent,
        divider = {}
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = pagerState.currentPage == index,
            onClick = { 
              scope.launch { pagerState.animateScrollToPage(index) }
            },
            text = { Text(title) },
            icon = {
              Icon(
                imageVector = if (index == 0) Icons.Default.Settings else Icons.Default.Widgets,
                contentDescription = null
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) { page ->
        when (page) {
          0 -> SettingsTab(
            remainingTime = remainingTime,
            onStartTimer = onStartTimer,
            onEnterFiltersPanel = onEnterFiltersPanel,
            onAnime4KChanged = onAnime4KChanged,
            onDismissRequest = onDismissRequest
          )
          1 -> ControlsTab(
            viewModel = viewModel,
            onDismissRequest = onDismissRequest,
            onShowSheet = onShowSheet
          )
        }
      }
      
      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
  remainingTime: Int,
  onStartTimer: (Int) -> Unit,
  onEnterFiltersPanel: () -> Unit,
  onAnime4KChanged: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val decoderPreferences = koinInject<DecoderPreferences>()
  val anime4kManager = koinInject<Anime4KManager>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  
  val enableAnime4K by decoderPreferences.enableAnime4K.collectAsState()
  val anime4kMode by decoderPreferences.anime4kMode.collectAsState()
  val anime4kQuality by decoderPreferences.anime4kQuality.collectAsState()
  val gpuNext by decoderPreferences.gpuNext.collectAsState()
  val useVulkan by decoderPreferences.useVulkan.collectAsState()
  
  val scope = rememberCoroutineScope()
  val activity = LocalContext.current as PlayerActivity

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.medium)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
  ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(id = R.string.player_sheets_more_title),
          style = MaterialTheme.typography.titleLarge,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          var isSleepTimerDialogShown by remember { mutableStateOf(false) }
          TextButton(onClick = { isSleepTimerDialogShown = true }) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
              Icon(imageVector = Icons.Outlined.Timer, contentDescription = null)
              Text(
                text =
                  if (remainingTime == 0) {
                    stringResource(R.string.timer_title)
                  } else {
                    stringResource(
                      R.string.timer_remaining,
                      DateUtils.formatElapsedTime(remainingTime.toLong()),
                    )
                  },
              )
              if (isSleepTimerDialogShown) {
                TimePickerDialog(
                  remainingTime = remainingTime,
                  onDismissRequest = { isSleepTimerDialogShown = false },
                  onTimeSelect = onStartTimer,
                )
              }
            }
          }
          TextButton(onClick = onEnterFiltersPanel) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
              Icon(imageVector = Icons.Default.Tune, contentDescription = null)
              Text(text = stringResource(id = R.string.player_sheets_filters_title))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

      Text(
        text = stringResource(R.string.player_sheets_stats_page_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        items(6) { page ->
          FilterChip(
            label = {
              Text(
                stringResource(
                  if (page ==
                    0
                  ) {
                    R.string.player_sheets_tracks_off
                  } else {
                    R.string.player_sheets_stats_page_chip
                  },
                  page,
                ),
              )
            },
            onClick = {
              if ((page == 0) xor (statisticsPage == 0)) MPVLib.command("script-binding", "stats/display-stats-toggle")
              if (page != 0) MPVLib.command("script-binding", "stats/display-page-$page")
              advancedPreferences.enabledStatisticsPage.set(page)
            },
            selected = statisticsPage == page,
            leadingIcon = null,
          )
        }
      }
      
      // Shaders Controls
      if (enableAnime4K && (!gpuNext || useVulkan)) {
        // Auto-detect resolution to disable for 4K+
        val width = MPVLib.getPropertyInt("video-params/w") ?: 0
        val height = MPVLib.getPropertyInt("video-params/h") ?: 0
        val isHighRes = width >= 3840 || height >= 2160

        // Presets (Mode) - Now on Top
        Text(
            text = stringResource(R.string.anime4k_mode_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (isHighRes) {
            Text(
                text = "Not available for 4K/8K video",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          items(Anime4KManager.Mode.entries) { mode ->
            FilterChip(
              label = { Text(stringResource(mode.titleRes)) },
              selected = anime4kMode == mode.name,
              enabled = !isHighRes,
              leadingIcon = null,
              onClick = {
                decoderPreferences.anime4kMode.set(mode.name)
                
                // Apply shaders immediately (runtime change)
                scope.launch(Dispatchers.IO) {
                  runCatching {
                    val qualityStr = decoderPreferences.anime4kQuality.get()
                    val quality = try {
                      Anime4KManager.Quality.valueOf(qualityStr)
                    } catch (e: IllegalArgumentException) {
                      Anime4KManager.Quality.BALANCED
                    }
                    val currentMode = try {
                        Anime4KManager.Mode.valueOf(mode.name)
                    } catch (e: IllegalArgumentException) {
                        Anime4KManager.Mode.OFF
                    }

                    val shaderChain = anime4kManager.getShaderChain(currentMode, quality)

                    // Use setPropertyString for runtime changes
                    MPVLib.setPropertyString("glsl-shaders", if (shaderChain.isNotEmpty()) shaderChain else "")
                    // Restart ambient mode if it was ON (Anime4K reset wiped it)
                    onAnime4KChanged()
                  }
                }
              }
            )
          }
        }

        Text(
            text = stringResource(R.string.anime4k_quality_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          items(Anime4KManager.Quality.entries) { quality ->
             FilterChip(
              label = { Text(stringResource(quality.titleRes)) },
              selected = anime4kQuality == quality.name,
              enabled = anime4kMode != "OFF" && !isHighRes,
              leadingIcon = null,
              onClick = {
                decoderPreferences.anime4kQuality.set(quality.name)

                // Apply shaders immediately (runtime change)
                scope.launch(Dispatchers.IO) {
                  runCatching {
                    val modeStr = decoderPreferences.anime4kMode.get()
                    val modeEnum = try {
                      Anime4KManager.Mode.valueOf(modeStr)
                    } catch (e: IllegalArgumentException) {
                      Anime4KManager.Mode.OFF
                    }
                    val currentQuality = try {
                        Anime4KManager.Quality.valueOf(quality.name)
                    } catch (e: IllegalArgumentException) {
                        Anime4KManager.Quality.BALANCED
                    }

                    val shaderChain = anime4kManager.getShaderChain(modeEnum, currentQuality)

                    // Use setPropertyString for runtime changes
                    MPVLib.setPropertyString("glsl-shaders", if (shaderChain.isNotEmpty()) shaderChain else "")
                    // Restart ambient mode if it was ON (Anime4K reset wiped it)
                    onAnime4KChanged()
                  }
                }
              }
            )
          }
        }
      }
  }
}

@Composable
fun ControlsTab(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  onShowSheet: (Sheets) -> Unit,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()
  val moreSheetControlsPref by appearancePreferences.moreSheetControls.collectAsState()

  val buttons = remember(moreSheetControlsPref) {
      appearancePreferences.parseButtons(moreSheetControlsPref, mutableSetOf())
  }

  // Data needed for RenderPlayerButton
  val chapters by viewModel.chapters.collectAsState(persistentListOf())
  val currentChapter by MPVLib.propInt["chapter"].collectAsState(0)
  val playbackSpeed by MPVLib.propFloat["speed"].collectAsState(1f)
  val isSpeedNonOne by remember(playbackSpeed) {
    derivedStateOf { abs((playbackSpeed ?: 1f) - 1f) > 0.001f }
  }
  val currentZoom by viewModel.videoZoom.collectAsState()
  val aspect by viewModel.videoAspect.collectAsState()
  
  val activity = LocalContext.current as PlayerActivity
  val mpvDecoder by MPVLib.propString["hwdec-current"].collectAsState("")
  val decoder by remember { derivedStateOf { app.marlboroadvance.mpvex.ui.player.Decoder.getDecoderFromValue(mpvDecoder ?: "auto") } }

  val mediaTitle by remember(activity) {
      derivedStateOf {
          MPVLib.getPropertyString("media-title")?.takeIf { it.isNotBlank() }
              ?: activity.getTitleForControls()
      }
  }

  Column(
      modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium)
  ) {
      Text(
          text = "Extended Controls",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = MaterialTheme.spacing.medium)
      )

      LazyVerticalGrid(
          columns = GridCells.Fixed(6), // More compact grid
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
          modifier = Modifier.height(240.dp)
      ) {
          items(buttons) { button ->
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                  RenderPlayerButton(
                      button = button,
                      chapters = chapters,
                      currentChapter = currentChapter,
                      isPortrait = true, 
                      isSpeedNonOne = isSpeedNonOne,
                      currentZoom = currentZoom,
                      aspect = aspect,
                      mediaTitle = mediaTitle,
                      hideBackground = hideBackground,
                      decoder = decoder,
                      playbackSpeed = playbackSpeed ?: 1f,
                      onBackPress = { activity.onBackPressedDispatcher.onBackPressed() },
                      onOpenSheet = {
                          onDismissRequest()
                          onShowSheet(it)
                      },
                      onOpenPanel = {
                          onDismissRequest()
                          viewModel.panelShown.value = it
                      },
                      viewModel = viewModel,
                      activity = activity,
                      buttonSize = 48.dp // Slightly smaller icons for compact grid
                  )
                  Text(
                      text = getPlayerButtonLabel(button),
                      style = MaterialTheme.typography.labelSmall,
                      textAlign = TextAlign.Center,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.width(56.dp)
                  )
              }
          }
      }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimePickerDialog(
  onDismissRequest: () -> Unit,
  onTimeSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  remainingTime: Int = 0,
) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      modifier = modifier
          .width(360.dp) // Fixed wide width to fit presets
          .padding(MaterialTheme.spacing.medium),
    ) {
      Column(
        modifier =
          Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
              text = stringResource(R.string.timer_title), // "Sleep Timer"
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.timer_picker_enter_timer),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
        }

        val state =
          rememberTimePickerState(
            remainingTime / 3600,
            (remainingTime % 3600) / 60,
            is24Hour = true,
          )

        TimeInput(state = state)
        
        // Quick Presets
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Quick Presets",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val presets = listOf(15, 30, 45, 60)
                presets.forEach { minutes ->
                    FilterChip(
                        selected = false,
                        onClick = { 
                            onTimeSelect(minutes * 60)
                            onDismissRequest()
                        },
                        label = { Text("${minutes}m") },
                        leadingIcon = null,
                    )
                }
            }
        }

        // Actions
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          TextButton(onClick = {
             onTimeSelect(0)
             onDismissRequest()
          }) {
              Text(stringResource(id = R.string.generic_reset))
          }
          Spacer(Modifier.weight(1f))
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TextButton(onClick = onDismissRequest) {
              Text(stringResource(id = R.string.generic_cancel))
            }
            Button(
              onClick = {
                onTimeSelect(state.hour * 3600 + state.minute * 60)
                onDismissRequest()
              },
            ) {
              Text(stringResource(id = R.string.generic_ok))
            }
          }
        }
      }
    }
  }
}
