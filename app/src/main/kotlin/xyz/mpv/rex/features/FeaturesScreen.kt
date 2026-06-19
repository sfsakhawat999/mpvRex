package xyz.mpv.rex.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import xyz.mpv.rex.features.cinehub.ui.CineHubScreen
import xyz.mpv.rex.features.cinetube.ui.YoutubeTabScreen

@Composable
fun FeaturesScreen(onPlayRequested: (filePath: String, cleanTitle: String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("CineHub", "CineTube")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            tabTitles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, fontWeight = FontWeight.Bold) })
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> CineHubScreen(moviesList = emptyList(), tvShowsList = emptyList(), onPlayRequested = onPlayRequested)
                1 -> YoutubeTabScreen()
            }
        }
    }
}
