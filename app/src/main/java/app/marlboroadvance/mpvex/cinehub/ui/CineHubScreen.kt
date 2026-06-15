package app.marlboroadvance.mpvex.cinehub.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import app.marlboroadvance.mpvex.cinehub.model.EpisodeItem
import app.marlboroadvance.mpvex.cinehub.data.NfoScanner
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineHubScreen(
    moviesList: List<MovieItem>,
    tvShowsList: List<TvShowItem>,
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Movies", "TV Shows")
    
    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
    var selectedTvShow by remember { mutableStateOf<TvShowItem?>(null) }

    // Dynamic grid layout response matching device orientation state
    val configuration = LocalConfiguration.current
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3

    Scaffold(
        modifier = Modifier.fillMaxSize(), 
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- INTEGRATED UNIFIED BROWSER TOP BAR COMPONENT ---
                BrowserTopBar(
                    title = "CineHub",
                    isInSelectionMode = false,
                    selectedCount = 0,
                    totalCount = moviesList.size + tvShowsList.size,
                    onCancelSelection = {},
                    isHomeScreen = true, // Enables matching circular theme transition effects
                    onSearchClick = {}
                )

                // Sub-tab row navigation perfectly nested under the unified header design
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = tabIndex == index,
                                onClick = { tabIndex = index },
                                text = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tabIndex) {
                0 -> {
                    if (moviesList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No local movies discovered.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(moviesList) { movie ->
                                CineHubGridCard(
                                    title = movie.title,
                                    genre = movie.genre,
                                    rating = movie.userRating,
                                    posterPath = movie.posterPath,
                                    onClick = { selectedMovie = movie }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (tvShowsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No local TV series discovered.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumnCount),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(tvShowsList) { show ->
                                CineHubGridCard(
                                    title = show.title,
                                    genre = show.genre,
                                    rating = show.userRating,
                                    posterPath = show.posterPath,
                                    onClick = { selectedTvShow = show }
                                )
                            }
                        }
                    }
                }
            }

            // --- MOVIE DETAIL OVERLAY IN MATERIAL3 BOTTOM SHEET ---
            selectedMovie?.let { movie ->
                ModalBottomSheet(
                    onDismissRequest = { selectedMovie = null },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)
                    ) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = movie.posterPath ?: android.R.drawable.ic_menu_gallery,
                                    contentDescription = movie.title,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .aspectRatio(2f / 3f)
                                        .background(Color.Gray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    if (movie.originalTitle.isNotEmpty() && movie.originalTitle != movie.title) {
                                        Text(movie.originalTitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Rating: ★ ${movie.userRating} | Year: ${movie.premiered}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Genre: ${movie.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    selectedMovie = null
                                    onPlayRequested(movie.videoFilePath, movie.title)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play Movie", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Plot Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                movie.plot.ifEmpty { "No description available." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // --- TV SHOW SEASON & EPISODES DETAIL OVERLAY ---
            selectedTvShow?.let { show ->
                val episodes = remember(show) { NfoScanner.scanTvShowEpisodes(File(show.folderPath)) }
                val seasons = remember(episodes) { episodes.groupBy { it.season }.toSortedMap() }
                var selectedSeasonTab by remember { mutableStateOf(seasons.keys.firstOrNull() ?: 1) }

                ModalBottomSheet(
                    onDismissRequest = { selectedTvShow = null },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)) {
                        Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Studio: ${show.studio} | Genre: ${show.genre}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (seasons.keys.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = seasons.keys.indexOf(selectedSeasonTab).coerceAtLeast(0),
                                edgePadding = 0.dp,
                                divider = {}
                            ) {
                                seasons.keys.forEach { seasonNum ->
                                    Tab(
                                        selected = selectedSeasonTab == seasonNum,
                                        onClick = { selectedSeasonTab = seasonNum },
                                        text = { Text("Season $seasonNum", fontWeight = FontWeight.Medium) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            items(seasons[selectedSeasonTab] ?: emptyList()) { episode ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Episode ${episode.episode}: ${episode.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (episode.plot.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    episode.plot,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                selectedTvShow = null
                                                onPlayRequested(episode.videoFilePath, "${show.title} - S${episode.season}E${episode.episode}")
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Episode", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
