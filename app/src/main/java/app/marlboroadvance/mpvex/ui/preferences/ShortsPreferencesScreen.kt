package app.marlboroadvance.mpvex.ui.preferences

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object ShortsPreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current
        val browserPreferences = koinInject<BrowserPreferences>()
        val enableShorts by browserPreferences.enableShorts.collectAsState()
        val persistentShuffle by browserPreferences.persistentShuffle.collectAsState()
        val includeHorizontal by browserPreferences.includeShortHorizontalVideos.collectAsState()
        val maxDuration by browserPreferences.maxHorizontalVideoDurationMinutes.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "RexShorts Settings",
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        PreferenceSectionHeader(title = "General")
                    }

                    item {
                        PreferenceCard {
                            SwitchPreference(
                                value = enableShorts,
                                onValueChange = { browserPreferences.enableShorts.set(it) },
                                title = { Text("Enable RexShorts") },
                                summary = { Text("Show the Shorts tab in the bottom navigation bar") },
                                icon = {
                                    Icon(
                                        Icons.Outlined.VideoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = persistentShuffle,
                                onValueChange = { browserPreferences.persistentShuffle.set(it) },
                                title = { Text("Persistent Shuffle") },
                                summary = { Text("Always randomize videos when opening Shorts") },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Shuffle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = "Discovery")
                    }

                    item {
                        PreferenceCard {
                            SwitchPreference(
                                value = includeHorizontal,
                                onValueChange = { browserPreferences.includeShortHorizontalVideos.set(it) },
                                title = { Text("Include Short Normal Videos") },
                                summary = { Text("Show horizontal videos in the feed if they are short") },
                                icon = {
                                    Icon(
                                        Icons.Outlined.HorizontalRule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )

                            PreferenceDivider()

                            SliderPreference(
                                value = maxDuration.toFloat(),
                                onValueChange = { browserPreferences.maxHorizontalVideoDurationMinutes.set(it.roundToInt()) },
                                sliderValue = maxDuration.toFloat(),
                                onSliderValueChange = { browserPreferences.maxHorizontalVideoDurationMinutes.set(it.roundToInt()) },
                                title = { Text("Max Horizontal Duration") },
                                summary = { Text("Limit horizontal videos to $maxDuration minute${if (maxDuration > 1) "s" else ""}") },
                                valueRange = 1f..10f,
                                valueSteps = 9,
                                enabled = includeHorizontal,
                                icon = {
                                    // Empty icon for alignment if needed or specific icon
                                }
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = "Content Management")
                    }

                    item {
                        PreferenceCard {
                            Preference(
                                title = { Text("Blocked Videos") },
                                summary = { Text("View and manage blocked shorts") },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = { backstack.add(BlockedShortsScreen) }
                            )
                        }
                    }
                }
            }
        }
    }
}
