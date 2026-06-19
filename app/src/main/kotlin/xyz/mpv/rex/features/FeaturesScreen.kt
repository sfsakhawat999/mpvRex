package xyz.mpv.rex.features

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import xyz.mpv.rex.features.cinehub.ui.CineHubScreen

@Composable
fun FeaturesScreen(
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit
) {
    // Top tab row entirely stripped to completely eliminate the overlapping CineHub | CineTube text layer.
    // The control state is directly governed inside the screen viewport.
    Box(modifier = Modifier.fillMaxSize()) {
        CineHubScreen(
            moviesList = emptyList(), 
            tvShowsList = emptyList(), 
            onPlayRequested = onPlayRequested
        )
    }
}
