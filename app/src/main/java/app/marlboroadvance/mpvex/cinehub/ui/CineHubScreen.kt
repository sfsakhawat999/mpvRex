package app.marlboroadvance.mpvex.cinehub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.cinehub.model.MovieItem

@Composable
fun CineHubScreen(
    moviesList: List<MovieItem>,
    onMovieClick: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Movies", "TV Shows")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                )
            }
        }

        when (tabIndex) {
            0 -> {
                if (moviesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No movies with NFO metadata found.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    ) {
                        items(moviesList) { movie ->
                            CineHubGridCard(movie = movie, onClick = { onMovieClick(movie.videoFilePath) })
                        }
                    }
                }
            }
            1 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("TV Shows integration coming soon.")
                }
            }
        }
    }
}
