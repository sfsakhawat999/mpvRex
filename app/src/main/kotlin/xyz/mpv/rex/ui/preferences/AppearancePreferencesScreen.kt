package xyz.mpv.rex.ui.preferences

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.ThumbnailStrategy
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.GesturePreferences
import xyz.mpv.rex.preferences.MultiChoiceSegmentedButton
import xyz.mpv.rex.ui.preferences.components.ThemePicker
import xyz.mpv.rex.ui.preferences.components.SwitchPreference
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.theme.DarkMode
import xyz.mpv.rex.ui.utils.LocalBackStack
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import xyz.mpv.rex.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider

@Serializable
object AppearancePreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val preferences = koinInject<AppearancePreferences>()
        val browserPreferences = koinInject<BrowserPreferences>()
        val gesturePreferences = koinInject<GesturePreferences>()
        val backstack = LocalBackStack.current
        val systemDarkTheme = isSystemInDarkTheme()

        val darkMode by preferences.darkMode.collectAsState()
        val appTheme by preferences.appTheme.collectAsState()

        // Determine if we're in dark mode for theme preview
        val isDarkMode = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val thumbnailRepository = koinInject<ThumbnailRepository>()
        var showNetworkWarning by remember { mutableStateOf(false) }
        var pendingStrategyChange by remember { mutableStateOf<ThumbnailStrategy?>(null) }
        var pendingPositionChange by remember { mutableStateOf<Int?>(null) }

        // ملاحظة: النصوص أدناه (Toast) لم تُلمس عمداً — stringResource() لا يعمل
        // خارج composition، وتحتاج تعديل توقيع الدالة (تمرير Context.getString
        // أو نص جاهز من الطبقة العليا). تُركت للمطور الأصلي.
        fun clearCacheAndApply(onSuccess: () -> Unit, onFailure: () -> Unit = {}) {
            scope.launch(Dispatchers.IO) {
                runCatching { thumbnailRepository.clearLocalThumbnailCache() }
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                            Toast.makeText(context, "Local thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            onFailure()
                            Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_appearance_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backstack::removeLastOrNull) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            ProvidePreferenceLocals {
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_theme))
                    }

                    item {
                        PreferenceCard {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // Dark mode selector
                                MultiChoiceSegmentedButton(
                                    choices = DarkMode.entries.map { stringResource(it.titleRes) }.toImmutableList(),
                                    selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
                                    onClick = { preferences.darkMode.set(DarkMode.entries[it]) },
                                )
                            }

                            PreferenceDivider()

                            // AMOLED mode state - need it before theme picker
                            val amoledMode by preferences.amoledMode.collectAsState()

                            // Theme picker - Aniyomi style
                            ThemePicker(
                                currentTheme = appTheme,
                                isDarkMode = isDarkMode,
                                onThemeSelected = { preferences.appTheme.set(it) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )

                            PreferenceDivider()

                            // AMOLED mode toggle
                            SwitchPreference(
                                value = amoledMode,
                                onValueChange = { newValue ->
                                    preferences.amoledMode.set(newValue)
                                },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_amoled_mode_title)) },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_amoled_mode_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                enabled = darkMode != DarkMode.Light
                            )

                            PreferenceDivider()

                            // System font toggle
                            val useSystemFont by preferences.useSystemFont.collectAsState()

                            SwitchPreference(
                                value = useSystemFont,
                                onValueChange = { newValue ->
                                    preferences.useSystemFont.set(newValue)
                                },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_use_system_font_title)) },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_use_system_font_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            val matchPlayerControlsToTheme by preferences.matchPlayerControlsToTheme.collectAsState()
                            SwitchPreference(
                                value = matchPlayerControlsToTheme,
                                onValueChange = { preferences.matchPlayerControlsToTheme.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_match_player_controls_to_theme_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_match_player_controls_to_theme_summary),
                                    )
                                },
                            )

                            PreferenceDivider()

                            val hidePlayerButtonsBackground by preferences.hidePlayerButtonsBackground.collectAsState()
                            SwitchPreference(
                                value = hidePlayerButtonsBackground,
                                onValueChange = { preferences.hidePlayerButtonsBackground.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_summary),
                                    )
                                },
                            )

                            PreferenceDivider()

                            val enableGlassPlayerControls by preferences.enableGlassPlayerControls.collectAsState()
                            SwitchPreference(
                                value = enableGlassPlayerControls,
                                onValueChange = { preferences.enableGlassPlayerControls.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_enable_glass_player_controls_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_enable_glass_player_controls_summary),
                                    )
                                },
                            )

                            PreferenceDivider()

                            val enableGlassSeekbarBackground by preferences.enableGlassSeekbarBackground.collectAsState()
                            SwitchPreference(
                                value = enableGlassSeekbarBackground,
                                onValueChange = { preferences.enableGlassSeekbarBackground.set(it) },
                                enabled = enableGlassPlayerControls,
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_enable_glass_seekbar_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_enable_glass_seekbar_summary),
                                    )
                                },
                            )

                            PreferenceDivider()

                            val playerAlwaysDarkMode by preferences.playerAlwaysDarkMode.collectAsState()
                            SwitchPreference(
                                value = playerAlwaysDarkMode,
                                onValueChange = { preferences.playerAlwaysDarkMode.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_player_always_dark_mode_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_player_always_dark_mode_summary),
                                    )
                                },
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_bottom_nav))
                    }

                    item {
                        PreferenceCard {
                            SwitchPreference(
                                value = true,
                                onValueChange = {},
                                enabled = false,
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_home_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_home_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val enableShorts by browserPreferences.enableShorts.collectAsState()
                            val enableTabRecents by browserPreferences.enableTabRecents.collectAsState()
                            val enableTabPlaylists by browserPreferences.enableTabPlaylists.collectAsState()
                            val enableTabNetwork by browserPreferences.enableTabNetwork.collectAsState()

                            SwitchPreference(
                                value = enableShorts,
                                onValueChange = { browserPreferences.enableShorts.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_shorts_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_shorts_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = enableTabRecents,
                                onValueChange = { browserPreferences.enableTabRecents.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_recents_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_recents_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = enableTabPlaylists,
                                onValueChange = { browserPreferences.enableTabPlaylists.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_playlists_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_playlists_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = enableTabNetwork,
                                onValueChange = { browserPreferences.enableTabNetwork.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_network_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_tab_network_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_file_browser))
                    }

                    item {
                        PreferenceCard {
                            val unlimitedNameLines by preferences.unlimitedNameLines.collectAsState()
                            SwitchPreference(
                                value = unlimitedNameLines,
                                onValueChange = { preferences.unlimitedNameLines.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val showUnplayedOldVideoLabel by preferences.showUnplayedOldVideoLabel.collectAsState()
                            SwitchPreference(
                                value = showUnplayedOldVideoLabel,
                                onValueChange = { preferences.showUnplayedOldVideoLabel.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val unplayedOldVideoDays by preferences.unplayedOldVideoDays.collectAsState()
                            SliderPreference(
                                value = unplayedOldVideoDays.toFloat(),
                                onValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_unplayed_old_video_days_title)) },
                                valueRange = 1f..30f,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_unplayed_old_video_days_summary,
                                            unplayedOldVideoDays,
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                onSliderValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                sliderValue = unplayedOldVideoDays.toFloat(),
                                enabled = showUnplayedOldVideoLabel
                            )

                            PreferenceDivider()

                            val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
                            SwitchPreference(
                                value = autoScrollToLastPlayed,
                                onValueChange = { browserPreferences.autoScrollToLastPlayed.set(it) },
                                title = {
                                    Text(text = stringResource(R.string.pref_appearance_auto_scroll_title))
                                },
                                summary = {
                                    Text(
                                        text = stringResource(R.string.pref_appearance_auto_scroll_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val watchedThreshold by browserPreferences.watchedThreshold.collectAsState()
                            SliderPreference(
                                value = watchedThreshold.toFloat(),
                                onValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                sliderValue = watchedThreshold.toFloat(),
                                onSliderValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_watched_threshold_title)) },
                                valueRange = 50f..100f,
                                valueSteps = 9,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_watched_threshold_summary,
                                            watchedThreshold,
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            val showAudioFiles by browserPreferences.showAudioFiles.collectAsState()
                            SwitchPreference(
                                value = showAudioFiles,
                                onValueChange = { browserPreferences.showAudioFiles.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_show_audio_files_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_show_audio_files_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val showTreeViewPath by browserPreferences.showTreeViewPath.collectAsState()
                            SwitchPreference(
                                value = showTreeViewPath,
                                onValueChange = { browserPreferences.showTreeViewPath.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_show_tree_view_path_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_show_tree_view_path_summary),
                                    )
                                }
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_thumbnails))
                    }

                    item {
                        PreferenceCard {
                            val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
                            SwitchPreference(
                                value = tapThumbnailToSelect,
                                onValueChange = { gesturePreferences.tapThumbnailToSelect.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val showNetworkThumbnails by preferences.showNetworkThumbnails.collectAsState()
                            SwitchPreference(
                                value = showNetworkThumbnails,
                                onValueChange = { newValue ->
                                    if (newValue) {
                                        showNetworkWarning = true
                                    } else {
                                        preferences.showNetworkThumbnails.set(false)
                                    }
                                },
                                title = {
                                    Text(text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_title))
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            if (showNetworkWarning) {
                                AlertDialog(
                                    onDismissRequest = { showNetworkWarning = false },
                                    title = { Text(stringResource(R.string.pref_appearance_network_thumbnails_dialog_title)) },
                                    text = {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.pref_appearance_network_thumbnails_dialog_message),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            preferences.showNetworkThumbnails.set(true)
                                            showNetworkWarning = false
                                        }) {
                                            Text(stringResource(R.string.generic_confirm))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNetworkWarning = false }) {
                                            Text(stringResource(R.string.generic_cancel))
                                        }
                                    }
                                )
                            }

                            PreferenceDivider()

                            val thumbnailStrategy by preferences.thumbnailStrategy.collectAsState()
                            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.pref_appearance_thumbnail_strategy_title),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(id = R.string.pref_appearance_thumbnail_strategy_summary),
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                MultiChoiceSegmentedButton(
                                    choices = persistentListOf(
                                        stringResource(id = R.string.pref_appearance_thumbnail_strategy_first_frame_title),
                                        stringResource(id = R.string.pref_appearance_thumbnail_strategy_position_title)
                                    ),
                                    selectedIndices = persistentListOf(ThumbnailStrategy.entries.indexOf(thumbnailStrategy)),
                                    onClick = { index ->
                                        val newStrategy = ThumbnailStrategy.entries[index]
                                        if (newStrategy != thumbnailStrategy) {
                                            pendingStrategyChange = newStrategy
                                        }
                                    },
                                )

                                Text(
                                    text = if (thumbnailStrategy == ThumbnailStrategy.FirstFrame) {
                                        stringResource(id = R.string.pref_appearance_thumbnail_strategy_first_frame_summary)
                                    } else {
                                        stringResource(id = R.string.pref_appearance_thumbnail_strategy_position_summary)
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            pendingStrategyChange?.let { newStrategy ->
                                AlertDialog(
                                    onDismissRequest = { pendingStrategyChange = null },
                                    title = { Text(stringResource(R.string.pref_appearance_thumbnail_strategy_dialog_title)) },
                                    text = {
                                        Column {
                                            val summaryText = if (newStrategy == ThumbnailStrategy.FirstFrame) {
                                                stringResource(id = R.string.pref_appearance_thumbnail_strategy_first_frame_summary)
                                            } else {
                                                stringResource(id = R.string.pref_appearance_thumbnail_strategy_position_summary)
                                            }
                                            Text(
                                                text = summaryText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                            Text(
                                                text = stringResource(R.string.pref_appearance_thumbnail_strategy_dialog_message),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            clearCacheAndApply(
                                                onSuccess = {
                                                    preferences.thumbnailStrategy.set(newStrategy)
                                                    pendingStrategyChange = null
                                                },
                                                onFailure = {
                                                    pendingStrategyChange = null
                                                }
                                            )
                                        }) {
                                            Text(stringResource(R.string.generic_confirm))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { pendingStrategyChange = null }) {
                                            Text(stringResource(R.string.generic_cancel))
                                        }
                                    }
                                )
                            }

                            PreferenceDivider()

                            val thumbnailPositionPercent by preferences.thumbnailPositionPercent.collectAsState()
                            val isPositionStrategy = thumbnailStrategy == ThumbnailStrategy.Position

                            // Track the drag visually without saving it to the backend yet
                            var draftPosition by remember(thumbnailPositionPercent) { mutableStateOf(thumbnailPositionPercent.toFloat()) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SliderPreference(
                                    modifier = Modifier.weight(1f),
                                    value = thumbnailPositionPercent.toFloat(),
                                    onValueChange = { finalValue ->
                                        val newInt = finalValue.roundToInt()
                                        if (newInt != thumbnailPositionPercent) {
                                            pendingPositionChange = newInt
                                        }
                                    },
                                    sliderValue = draftPosition,
                                    onSliderValueChange = { slidingValue ->
                                        draftPosition = slidingValue
                                    },
                                    title = { Text(text = stringResource(id = R.string.pref_appearance_thumbnail_position_title)) },
                                    valueRange = 1f..100f,
                                    summary = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.pref_appearance_thumbnail_position_summary,
                                                draftPosition.roundToInt(),
                                            ),
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    },
                                    enabled = isPositionStrategy
                                )

                                IconButton(
                                    onClick = {
                                        val default = AppearancePreferences.THUMBNAIL_POSITION_DEFAULT
                                        if (thumbnailPositionPercent != default) {
                                            pendingPositionChange = default
                                        }
                                    },
                                    enabled = isPositionStrategy && thumbnailPositionPercent != AppearancePreferences.THUMBNAIL_POSITION_DEFAULT,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color = if (isPositionStrategy && thumbnailPositionPercent != AppearancePreferences.THUMBNAIL_POSITION_DEFAULT)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = stringResource(id = R.string.pref_appearance_thumbnail_position_reset),
                                        tint = if (isPositionStrategy && thumbnailPositionPercent != AppearancePreferences.THUMBNAIL_POSITION_DEFAULT)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                }
                            }

                            pendingPositionChange?.let { newPosition ->
                                AlertDialog(
                                    onDismissRequest = { 
                                        pendingPositionChange = null 
                                        draftPosition = thumbnailPositionPercent.toFloat()
                                    },
                                    title = { Text(stringResource(R.string.pref_appearance_thumbnail_position_dialog_title)) },
                                    text = {
                                        Column {
                                            Text(
                                                text = stringResource(
                                                    R.string.pref_appearance_thumbnail_position_reset_confirm,
                                                    newPosition,
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                            Text(
                                                text = stringResource(R.string.pref_appearance_thumbnail_position_dialog_message),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            clearCacheAndApply(
                                                onSuccess = {
                                                    preferences.thumbnailPositionPercent.set(newPosition)
                                                    pendingPositionChange = null
                                                },
                                                onFailure = {
                                                    pendingPositionChange = null
                                                    draftPosition = thumbnailPositionPercent.toFloat()
                                                }
                                            )
                                        }) {
                                            Text(stringResource(R.string.generic_confirm))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            pendingPositionChange = null 
                                            draftPosition = thumbnailPositionPercent.toFloat()
                                        }) {
                                            Text(stringResource(R.string.generic_cancel))
                                        }
                                    }
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
