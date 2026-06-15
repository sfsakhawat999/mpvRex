package app.marlboroadvance.mpvex.cinehub.ui

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import app.marlboroadvance.mpvex.cinehub.model.EpisodeItem
import app.marlboroadvance.mpvex.cinehub.data.NfoScanner
import app.marlboroadvance.mpvex.youtube.data.InvidiousClient // Imported YouTube client integration
import app.marlboroadvance.mpvex.youtube.model.YoutubeVideo
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import kotlinx.coroutines.launch
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

    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3

    Scaffold(
        modifier = Modifier.fillMaxSize(), 
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BrowserTopBar(
                    title = "CineHub",
                    isInSelectionMode = false,
                    selectedCount = 0,
                    totalCount = moviesList.size + tvShowsList.size,
                    onCancelSelection = {},
                    isHomeScreen = true,
                    onSearchClick = {}
                )

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

            // --- MOVIE DETAIL OVERLAY WITH AUTO-PLAY GRADIENT TRAILER WINDOW ---
            selectedMovie?.let { movie ->
                var trailerVideo by remember { mutableStateOf<YoutubeVideo?>(null) }
                
                // Automatically queries YouTube index for the movie's official trailer upon click
                LaunchedEffect(movie) {
                    scope.launch {
                        val searchResults = InvidiousClient.fetchSearchVideos("${movie.title} official trailer")
                        if (searchResults.isNotEmpty()) {
                            trailerVideo = searchResults.first()
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { selectedMovie = null },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)
                    ) {
                        // --- NEW: Premium Gradient Trailer Autoplay Window ---
                        item {
                            val infiniteTransition = rememberInfiniteTransition(label = "GradientAnim")
                            val offset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1000f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 3500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "Offset"
                            )
                            
                            val animatedGradient = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .border(BorderStroke(1.5.dp, animatedGradient), RoundedCornerShape(16.dp))
                                    .clickable {
                                        trailerVideo?.let { ytVideo ->
                                            scope.launch {
                                                val directUrl = InvidiousClient.fetchDirectStreamUrl(ytVideo.videoId)
                                                if (directUrl != null) {
                                                    onPlayRequested(directUrl, "${movie.title} - Trailer")
                                                }
                                            }
                                        }
                                    }
                            ) {
                                if (trailerVideo != null) {
                                    AsyncImage(
                                        model = trailerVideo!!.getBestThumbnailUrl(),
                                        contentDescription = "Trailer Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Soft overlay to mask thumbnail into layout aesthetics
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                                    
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Trailer", tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Autoplay Trailer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    // Animated shimmer loader layout before stream asset mapping completes
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color.Gray.copy(alpha = 0.1f), Color.DarkGray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.1f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = movie.posterPath ?: android.R.drawable.ic_menu_gallery,
                                    contentDescription = movie.title,
                                    modifier = Modifier
                                        .width(100.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Rating: ★ ${movie.userRating} | Year: ${movie.premiered}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Genre: ${movie.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    selectedMovie = null
                                    onPlayRequested(movie.videoFilePath, movie.title)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play Full Movie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text("Plot Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                movie.plot.ifEmpty { "No description available." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // --- TV SHOW DETAIL OVERLAY WITH SORTED EPISODES & MATERIAL YOU GLASSMORPHISM ---
            selectedTvShow?.let { show ->
                // FIX: Sorting episodes in strict ascending sequence (Episode 1 -> 2 -> 3...)
                val episodes = remember(show) { 
                    NfoScanner.scanTvShowEpisodes(File(show.folderPath))
                        .sortedBy { it.episode } 
                }
                val seasons = remember(episodes) { episodes.groupBy { it.season }.toSortedMap() }
                var selectedSeasonTab by remember { mutableStateOf(seasons.keys.firstOrNull() ?: 1) }

                ModalBottomSheet(
                    onDismissRequest = { selectedTvShow = null },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)) {
                        Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Studio: ${show.studio} | Genre: ${show.genre}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(14.dp))
                        
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
                                        text = { Text("Season $seasonNum", fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                            items(seasons[selectedSeasonTab] ?: emptyList()) { episode ->
                                // --- UPGRADED: Material You Glassmorphic Layout Card ---
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) // Semitransparent layer
                                        .border(
                                            BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                            RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Episode ${episode.episode}: ${episode.title}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (episode.plot.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    episode.plot,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = {
                                                selectedTvShow = null
                                                onPlayRequested(episode.videoFilePath, "${show.title} - S${episode.season}E${episode.episode}")
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            )
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