package xyz.mpv.rex.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.features.cinehub.data.CineOnlineScraper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesSettingGroup() {
    var tmdbUrl by remember { mutableStateOf(CineOnlineScraper.tmdbBaseUrl) }
    var tvMazeUrl by remember { mutableStateOf(CineOnlineScraper.tvMazeBaseUrl) }
    var currentApiKey by remember { mutableStateOf(CineOnlineScraper.apiKey) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CineHub Scraper Proxies Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
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
    }
}
