package app.marlboroadvance.mpvex.cinehub.ui

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
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
import app.marlboroadvance.mpvex.cinehub.data.CineCloudRepoClient
import app.marlboroadvance.mpvex.youtube.data.InvidiousClient
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
    val tabs = listOf("Movies", "TV Shows") [cite: 1]
    
    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) } [cite: 1]
    var selectedTvShow by remember { mutableStateOf<TvShowItem?>(null) } [cite: 2]

    var onlineMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var onlineTvShows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope() [cite: 2]
    val configuration = LocalConfiguration.current [cite: 2]
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3 [cite: 2]

    LaunchedEffect(tabIndex) {
        if (onlineMovies.isEmpty() || onlineTvShows.isEmpty()) {
            isOnlineLoading = true
            scope.launch {
                try {
                    onlineMovies = CineCloudRepoClient.fetchOnlineMovies(context)
                    onlineTvShows = CineCloudRepoClient.fetchOnlineTvShows(context)
                } catch (e: Exception) {
                    android.util.Log.e("CineHubUI", "Network aggregation fault: " + e.message)
                } finally {
                    isOnlineLoading = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), [cite: 2]
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) { [cite: 2]
                BrowserTopBar(
                    title = "CineHub", [cite: 3]
                    isInSelectionMode = false, [cite: 3]
                    selectedCount = 0, [cite: 3]
                    totalCount = moviesList.size + tvShowsList.size, [cite: 3]
                    onCancelSelection = {}, [cite: 4]
                    isHomeScreen = true, [cite: 4]
                    onSearchClick = {} [cite: 4]
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow, [cite: 4]
                    tonalElevation = 1.dp [cite: 5]
                ) {
                    TabRow(
                        selectedTabIndex = tabIndex, [cite: 5]
                        containerColor = Color.Transparent, [cite: 6]
                        divider = {} [cite: 6]
                    ) {
                        tabs.forEachIndexed { index, title -> [cite: 6]
                            Tab(
                                selected = tabIndex == index, [cite: 7]
                                onClick = { tabIndex = index }, [cite: 7]
                                text = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } [cite: 8]
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ================= SECTION 1: NATIVE LOCAL MEDIA ROW SLIDER =================
            item {
                Text(
                    text = if (tabIndex == 0) "My Local Movies" else "My Local TV Series",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                if (tabIndex == 0) {
                    if (moviesList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No local files found inside target folders.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(moviesList) { movie ->
                                Box(modifier = Modifier.width(135.dp)) {
                                    CineHubGridCard(
                                        title = movie.title, [cite: 14]
                                        genre = movie.genre, [cite: 14]
                                        rating = movie.userRating, [cite: 15]
                                        posterPath = movie.posterPath, [cite: 15]
                                        onClick = { selectedMovie = movie } [cite: 15]
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (tvShowsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No local files found inside target folders.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tvShowsList) { show ->
                                Box(modifier = Modifier.width(135.dp)) {
                                    CineHubGridCard(
                                        title = show.title, [cite: 21]
                                        genre = show.genre, [cite: 21]
                                        rating = show.userRating, [cite: 22]
                                        posterPath = show.posterPath, [cite: 22]
                                        onClick = { selectedTvShow = show } [cite: 22]
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 2: MATERIAL YOU GLASSMORPHISM GRIDS =================
            item {
                Text(
                    text = "Trending Online Releases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            if (isOnlineLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                if (tabIndex == 0) {
                    if (onlineMovies.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Syncing secure server metadata networks...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        val chunkedMovies = onlineMovies.chunked(gridColumnCount)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (rowItems in chunkedMovies) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (movieItem in rowItems) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                CineHubGridCard(
                                                    title = movieItem.title,
                                                    genre = movieItem.genre,
                                                    rating = movieItem.userRating,
                                                    posterPath = movieItem.posterPath,
                                                    onClick = {
                                                        scope.launch {
                                                            val rawId = movieItem.videoFilePath.substringAfter("cnc_stream:").substringBefore(":")
                                                            val platformCode = movieItem.videoFilePath.substringAfterLast(":")
                                                            val directM3u8 = CineCloudRepoClient.resolveDirectStreamUrl(rawId, platformCode)
                                                            if (!directM3u8.isNullOrBlank()) {
                                                                onPlayRequested(directM3u8, movieItem.title)
                                                            } else {
                                                                onPlayRequested("https://net52.cc/mobile/player.php?id=$rawId", movieItem.title)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        val residualSlots = gridColumnCount - rowItems.size
                                        if (residualSlots > 0) {
                                            repeat(residualSlots) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (onlineTvShows.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Syncing secure server metadata networks...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        val chunkedTvShows = onlineTvShows.chunked(gridColumnCount)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (rowItems in chunkedTvShows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (tvShowItem in rowItems) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                CineHubGridCard(
                                                    title = tvShowItem.title,
                                                    genre = tvShowItem.genre,
                                                    rating = tvShowItem.userRating,
                                                    posterPath = tvShowItem.posterPath,
                                                    onClick = {
                                                        scope.launch {
                                                            val rawId = tvShowItem.folderPath.substringAfter("cnc_tv:").substringBefore(":")
                                                            val platformCode = tvShowItem.folderPath.substringAfterLast(":")
                                                            val directM3u8 = CineCloudRepoClient.resolveDirectStreamUrl(rawId, platformCode)
                                                            if (!directM3u8.isNullOrBlank()) {
                                                                onPlayRequested(directM3u8, tvShowItem.title)
                                                            } else {
                                                                onPlayRequested("https://net52.cc/mobile/player.php?id=$rawId", tvShowItem.title)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        val residualSlots = gridColumnCount - rowItems.size
                                        if (residualSlots > 0) {
                                            repeat(residualSlots) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MOVIES DETAIL BOTTOM SHEET ---
        selectedMovie?.let { movie ->
            if (!movie.videoFilePath.startsWith("cnc_stream:")) {
                var trailerVideo by remember { mutableStateOf<YoutubeVideo?>(null) } [cite: 24]
                
                LaunchedEffect(movie) { [cite: 25]
                    scope.launch { [cite: 25]
                        val searchResults = InvidiousClient.fetchSearchVideos("${movie.title} official trailer") [cite: 25]
                        if (searchResults.isNotEmpty()) { [cite: 25]
                            trailerVideo = searchResults.first() [cite: 26]
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { selectedMovie = null }, [cite: 27]
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) [cite: 27]
                ) {
                    LazyColumn(
                        modifier = Modifier [cite: 27]
                            .fillMaxWidth() [cite: 28]
                            .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp) [cite: 28]
                    ) {
                        item {
                            val infiniteTransition = rememberInfiniteTransition(label = "GradientAnim") [cite: 29]
                            val offset by infiniteTransition.animateFloat( [cite: 30]
                                initialValue = 0f, [cite: 30]
                                targetValue = 1000f, [cite: 30]
                                animationSpec = infiniteRepeatable( [cite: 31]
                                    animation = tween(durationMillis = 3500, easing = LinearEasing), [cite: 31]
                                    repeatMode = RepeatMode.Restart [cite: 32]
                                ),
                                label = "Offset" [cite: 32]
                            )
                            
                            val animatedGradient = Brush.linearGradient(
                                colors = listOf( [cite: 34]
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), [cite: 34]
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f), [cite: 35]
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) [cite: 35]
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth() [cite: 37]
                                    .aspectRatio(16f / 9f) [cite: 37]
                                    .clip(RoundedCornerShape(16.dp)) [cite: 37]
                                    .background(Color.Black) [cite: 38]
                                    .border(BorderStroke(1.5.dp, animatedGradient), RoundedCornerShape(16.dp)) [cite: 38]
                                    .clickable { [cite: 38]
                                        trailerVideo?.let { ytVideo -> [cite: 39]
                                            scope.launch { [cite: 39]
                                                val directUrl = InvidiousClient.fetchDirectStreamUrl(ytVideo.videoId) [cite: 40]
                                                if (directUrl != null) { [cite: 40]
                                                    onPlayRequested(directUrl, "${movie.title} - Trailer") [cite: 41]
                                                }
                                            }
                                        }
                                    }
                            ) {
                                if (trailerVideo != null) { [cite: 43]
                                    AsyncImage(
                                        model = trailerVideo!!.getBestThumbnailUrl(), [cite: 44]
                                        contentDescription = "Trailer Thumbnail", [cite: 44]
                                        contentScale = ContentScale.Crop, [cite: 45]
                                        modifier = Modifier.fillMaxSize() [cite: 45]
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f))) [cite: 46]
                                    
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f), [cite: 47]
                                        shape = RoundedCornerShape(20.dp), [cite: 48]
                                        modifier = Modifier.align(Alignment.Center) [cite: 48]
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), [cite: 49]
                                            verticalAlignment = Alignment.CenterVertically [cite: 50]
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Trailer", tint = Color.White) [cite: 51]
                                            Spacer(modifier = Modifier.width(6.dp)) [cite: 51]
                                            Text("Autoplay Trailer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) [cite: 52]
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier [cite: 53]
                                            .fillMaxSize() [cite: 54]
                                            .background(Color.DarkGray.copy(alpha = 0.2f)), [cite: 54]
                                        contentAlignment = Alignment.Center [cite: 55]
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) [cite: 56]
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp)) [cite: 57]
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth()) { [cite: 58]
                                AsyncImage(
                                    model = movie.posterPath ?: android.R.drawable.ic_menu_gallery, [cite: 58]
                                    contentDescription = movie.title, [cite: 59]
                                    modifier = Modifier
                                        .width(100.dp) [cite: 60]
                                        .aspectRatio(2f / 3f) [cite: 60]
                                        .clip(RoundedCornerShape(10.dp)) [cite: 60]
                                        .background(Color.Gray), [cite: 61]
                                    contentScale = ContentScale.Crop [cite: 61]
                                )
                                Spacer(modifier = Modifier.width(18.dp)) [cite: 62]
                                Column(modifier = Modifier.weight(1f)) { [cite: 62]
                                    Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) [cite: 63]
                                    Spacer(modifier = Modifier.height(4.dp)) [cite: 63]
                                    Text("Rating: ★ ${movie.userRating} | Year: ${movie.premiered}", style = MaterialTheme.typography.bodyMedium) [cite: 63, 64]
                                    Text("Genre: ${movie.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) [cite: 64]
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp)) [cite: 65]
                            Button(
                                onClick = { [cite: 66]
                                    selectedMovie = null [cite: 66]
                                    onPlayRequested(movie.videoFilePath, movie.title) [cite: 66]
                                },
                                modifier = Modifier.fillMaxWidth(), [cite: 67]
                                shape = RoundedCornerShape(14.dp) [cite: 67]
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play") [cite: 68]
                                Spacer(modifier = Modifier.width(8.dp)) [cite: 68]
                                Text("Play Full Movie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) [cite: 69]
                            }
                            Spacer(modifier = Modifier.height(18.dp)) [cite: 69]
                            Text("Plot Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) [cite: 70]
                            Text(
                                movie.plot.ifEmpty { "No description available." }, [cite: 70]
                                style = MaterialTheme.typography.bodyMedium, [cite: 71]
                                color = MaterialTheme.colorScheme.onSurfaceVariant, [cite: 71]
                                modifier = Modifier.padding(vertical = 6.dp) [cite: 71]
                            )
                        }
                    }
                }
            }
        }

        // --- TV SHOW DETAIL OVERLAY WITH MATERIAL YOU GLASSMORPHISM ---
        selectedTvShow?.let { show ->
            if (!show.folderPath.startsWith("cnc_tv:")) {
                val episodes = remember(show) { [cite: 73]
                    NfoScanner.scanTvShowEpisodes(File(show.folderPath)) [cite: 73]
                        .sortedBy { it.episode } [cite: 73]
                }
                val seasons = remember(episodes) { episodes.groupBy { it.season }.toSortedMap() } [cite: 74]
                var selectedSeasonTab by remember { mutableStateOf(seasons.keys.firstOrNull() ?: 1) } [cite: 74]

                ModalBottomSheet(
                    onDismissRequest = { selectedTvShow = null }, [cite: 74]
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) [cite: 75]
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)) { [cite: 75]
                        Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) [cite: 75, 76]
                        Text("Studio: ${show.studio} | Genre: ${show.genre}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) [cite: 76, 77]
                        Spacer(modifier = Modifier.height(14.dp)) [cite: 77]
                        
                        if (seasons.keys.size > 1) { [cite: 77]
                            ScrollableTabRow(
                                selectedTabIndex = seasons.keys.indexOf(selectedSeasonTab).coerceAtLeast(0), [cite: 78]
                                edgePadding = 0.dp, [cite: 78]
                                divider = {} [cite: 79]
                            ) {
                                seasons.keys.forEach { seasonNum -> [cite: 79]
                                    Tab(
                                        selected = selectedSeasonTab == seasonNum, [cite: 80]
                                        onClick = { selectedSeasonTab = seasonNum }, [cite: 81]
                                        text = { Text("Season $seasonNum", fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp)) [cite: 82]

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) { [cite: 83]
                            items(seasons[selectedSeasonTab] ?: emptyList()) { episode -> [cite: 83]
                                Box(
                                    modifier = Modifier [cite: 83]
                                        .fillMaxWidth() [cite: 84]
                                        .padding(vertical = 6.dp) [cite: 85]
                                        .clip(RoundedCornerShape(16.dp)) [cite: 85]
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) [cite: 85]
                                        .border( [cite: 85]
                                            BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), [cite: 86]
                                            RoundedCornerShape(16.dp) [cite: 87]
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(), [cite: 88]
                                        verticalAlignment = Alignment.CenterVertically [cite: 89]
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) { [cite: 89]
                                            Text(
                                                "Episode ${episode.episode}: ${episode.title}", [cite: 90]
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), [cite: 91]
                                                color = MaterialTheme.colorScheme.onSurface [cite: 91]
                                            )
                                            if (episode.plot.isNotEmpty()) { [cite: 92]
                                                Spacer(modifier = Modifier.height(4.dp)) [cite: 93]
                                                Text(
                                                    episode.plot, [cite: 94]
                                                    style = MaterialTheme.typography.bodySmall, [cite: 94]
                                                    maxLines = 2, [cite: 95]
                                                    overflow = TextOverflow.Ellipsis, [cite: 95]
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f) [cite: 96]
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp)) [cite: 98]
                                        IconButton(
                                            onClick = { [cite: 99]
                                                selectedTvShow = null [cite: 99]
                                                onPlayRequested(episode.videoFilePath, "${show.title} - S${episode.season}E${episode.episode}") [cite: 100]
                                            },
                                            colors = IconButtonDefaults.iconButtonColors( [cite: 101]
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) [cite: 101]
                                            )
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Episode", tint = MaterialTheme.colorScheme.onPrimaryContainer) [cite: 102]
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