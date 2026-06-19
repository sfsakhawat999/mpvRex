package xyz.mpv.rex.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.features.cinehub.data.CineOnlineScraper
import xyz.mpv.rex.features.cinehub.data.CineCloudRepoClient
import xyz.mpv.rex.features.cinetube.data.InvidiousClient

@Composable
fun FeaturesSettingGroup() {
    var tmdbUrl by remember { mutableStateOf(CineOnlineScraper.tmdbBaseUrl) }
    var tvMazeUrl by remember { mutableStateOf(CineOnlineScraper.tvMazeBaseUrl) }
    var currentApiKey by remember { mutableStateOf(CineOnlineScraper.apiKey) }
    
    var activeCloudMirror by remember { mutableStateOf(CineCloudRepoClient.workingDomain) }
    var currentCineTubeInstance by remember { mutableStateOf(InvidiousClient.currentInstance) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CINEHUB SECTION ---
        Text(
            text = "CineHub Scraper Proxies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedTextField(
            value = tmdbUrl,
            onValueChange = { 
                tmdbUrl = it
                CineOnlineScraper.tmdbBaseUrl = it
            },
            label = { Text("TMDB API Mirror Endpoint") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = tvMazeUrl,
            onValueChange = { 
                tvMazeUrl = it
                CineOnlineScraper.tvMazeBaseUrl = it
            },
            label = { Text("TVMaze Proxy Endpoint") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = currentApiKey,
            onValueChange = { 
                currentApiKey = it
                CineOnlineScraper.apiKey = it
            },
            label = { Text("Custom TMDB V3 API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // --- CINECLOUD CORE SECTION ---
        Text(
            text = "CineCloud Core Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = activeCloudMirror,
            onValueChange = { 
                activeCloudMirror = it
                CineCloudRepoClient.workingDomain = it
                CineCloudRepoClient.invalidateCachedEndpoints()
            },
            label = { Text("Active Streaming Domain (Override)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // --- CINETUBE SECTION ---
        Text(
            text = "CineTube Stream Core",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = currentCineTubeInstance,
            onValueChange = { 
                currentCineTubeInstance = it
                InvidiousClient.updateInstanceSetting(it)
            },
            label = { Text("Invidious Active Node URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
