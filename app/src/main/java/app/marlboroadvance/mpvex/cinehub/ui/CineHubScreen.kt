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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.marlboroadvance.mpvex.cinehub.model.MovieItem[cite: 28]
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem[cite: 28]
import app.marlboroadvance.mpvex.cinehub.model.EpisodeItem[cite: 28]
import app.marlboroadvance.mpvex.cinehub.data.NfoScanner[cite: 28]
import app.marlboroadvance.mpvex.cinehub.data.CineCloudRepoClient
import app.marlboroadvance.mpvex.youtube.data.InvidiousClient[cite: 28]
import app.marlboroadvance.mpvex.youtube.model.YoutubeVideo[cite: 28]
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar[cite: 28]
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineHubScreen(
    moviesList: List<MovieItem>,[cite: 28]
    tvShowsList: List<TvShowItem>,[cite: 28]
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit[cite: 28]
) {
    var tabIndex by remember { mutableIntStateOf(0) }[cite: 28]
    val tabs = listOf("Movies", "TV Shows")[cite: 28]
    
    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }[cite: 28]
    var selectedTvShow by remember { mutableStateOf<TvShowItem?>(null) }[cite: 28]

    // Cloud Stream Repository Dynamic States
    var onlineMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var onlineTvShows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()[cite: 28]
    val configuration = LocalConfiguration.current[cite: 28]
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3[cite: 28]

    // Async Network Scraper Pipeline hook matching current Tab Index parameters
    LaunchedEffect(tabIndex) {
        if (onlineMovies.isEmpty() || onlineTvShows.isEmpty()) {
            isOnlineLoading = true
            try {
                onlineMovies = CineCloudRepoClient.fetchOnlineMovies()
                onlineTvShows = CineCloudRepoClient.fetchOnlineTvShows()
            } catch (e: Exception) {
                android.util.Log.e("CineHubUI", "Failed querying cloud streams: ${e.message}")
            } finally {
                isOnlineLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),[cite: 28]
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {[cite: 28]
                BrowserTopBar(
                    title = "CineHub",[cite: 28]
                    isInSelectionMode = false,[cite: 28]
                    selectedCount = 0,[cite: 28]
                    totalCount = moviesList.size + tvShowsList.size,[cite: 28]
                    onCancelSelection = {},[cite: 28]
                    isHomeScreen = true,[cite: 28]
                    onSearchClick = {}[cite: 28]
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,[cite: 28]
                    tonalElevation = 1.dp[cite: 28]
                ) {
                    TabRow(
                        selectedTabIndex = tabIndex,[cite: 28]
                        containerColor = Color.Transparent,[cite: 28]
                        divider = {}[cite: 28]
                    ) {
                        tabs.forEachIndexed { index, title ->[cite: 28]
                            Tab(
                                selected = tabIndex == index,[cite: 28]
                                onClick = { tabIndex = index },[cite: 28]
                                text = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }[cite: 28]
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
            // ================= LAYER A: LOCAL STORAGE CONTENT (HORIZONTAL ROW ON TOP) =================
            item {
                Text(
                    text = if (tabIndex == 0) "Local Storage Movies" else "Local Storage TV Shows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                val currentLocalSource = if (tabIndex == 0) moviesList else tvShowsList
                if (currentLocalSource.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                        Text("No local items indexed inside CineRex folders.", fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentLocalSource) { mediaItem ->
                            Box(modifier = Modifier.width(135.dp)) {
                                CineHubGridCard(
                                    title = mediaItem.title,[cite: 28]
                                    genre = if (tabIndex == 0) (mediaItem as MovieItem).genre else (mediaItem as TvShowItem).genre,[cite: 28]
                                    rating = if (tabIndex == 0) (mediaItem as MovieItem).userRating else (mediaItem as TvShowItem).userRating,[cite: 28]
                                    posterPath = if (tabIndex == 0) (mediaItem as MovieItem).posterPath else (mediaItem as TvShowItem).posterPath,[cite: 28]
                                    onClick = {
                                        if (tabIndex == 0) selectedMovie = mediaItem as MovieItem else selectedTvShow = mediaItem as TvShowItem[cite: 28]
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ================= LAYER B: EXTENSIONS CLOUD STREAM CONTENT (VERTICAL MULTI-COLUMN GRIDS) =================
            item {
                Text(
                    text = if (tabIndex == 0) "Cloud Stream Extensions (Netflix/Prime)" else "Cloud Stream Extensions (Hotstar)",
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
                val currentOnlineSource = if (tabIndex == 0) onlineMovies else onlineTvShows
                if (currentOnlineSource.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Repository proxy streams offline or loading...", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    val chunkedOnlineRows = currentOnlineSource.chunked(gridColumnCount)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (rowItems in chunkedOnlineRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (onlineItem in rowItems) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            CineHubGridCard(
                                                title = onlineItem.title,
                                                genre = if (tabIndex == 0) (onlineItem as MovieItem).genre else (onlineItem as TvShowItem).genre,
                                                rating = if (tabIndex == 0) (onlineItem as MovieItem).userRating else (onlineItem as TvShowItem).userRating,
                                                posterPath = if (tabIndex == 0) (onlineItem as MovieItem).posterPath else (onlineItem as TvShowItem).posterPath,
                                                onClick = {
                                                    scope.launch {
                                                        val rawPostId = if (tabIndex == 0) {
                                                            (onlineItem as MovieItem).videoFilePath.substringAfter("cnc_stream:")
                                                        } else {
                                                            (onlineItem as TvShowItem).folderPath.substringAfter("cnc_tv:")
                                                        }
                                                        
                                                        val decryptedM3u8Url = CineCloudRepoClient.resolveDirectStreamUrl(rawPostId, isTv = (tabIndex == 1))
                                                        if (!decryptedM3u8Url.isNullOrBlank()) {
                                                            onPlayRequested(decryptedM3u8Url, onlineItem.title)
                                                        } else {
                                                            onPlayRequested("https://net52.cc/mobile/player.php?id=$rawPostId", onlineItem.title)
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

        // --- MOVIE DETAIL OVERLAY WITH AUTO-PLAY GRADIENT TRAILER WINDOW ---
        selectedMovie?.let { movie ->
            if (!movie.videoFilePath.startsWith("cnc_stream:")) {
                var trailerVideo by remember { mutableStateOf<YoutubeVideo?>(null) }[cite: 28]
                
                LaunchedEffect(movie) {[cite: 28]
                    scope.launch {[cite: 28]
                        val searchResults = InvidiousClient.fetchSearchVideos("${movie.title} official trailer")[cite: 28]
                        if (searchResults.isNotEmpty()) {[cite: 28]
                            trailerVideo = searchResults.first()[cite: 28]
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { selectedMovie = null },[cite: 28]
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)[cite: 28]
                ) {
                    LazyColumn(
                        modifier = Modifier[cite: 28]
                            .fillMaxWidth()[cite: 28]
                            .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)[cite: 28]
                    ) {
                        item {[cite: 28]
                            val infiniteTransition = rememberInfiniteTransition(label = "GradientAnim")[cite: 28]
                            val offset by infiniteTransition.animateFloat([cite: 28]
                                initialValue = 0f,[cite: 28]
                                targetValue = 1000f,[cite: 28]
                                animationSpec = infiniteRepeatable([cite: 28]
                                    animation = tween(durationMillis = 3500, easing = LinearEasing),[cite: 28]
                                    repeatMode = RepeatMode.Restart[cite: 28]
                                ),[cite: 28]
                                label = "Offset"[cite: 28]
                            )
                            
                            val animatedGradient = Brush.linearGradient([cite: 28]
                                colors = listOf([cite: 28]
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),[cite: 28]
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),[cite: 28]
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)[cite: 28]
                                )[cite: 28]
                            )

                            Box(
                                modifier = Modifier[cite: 28]
                                    .fillMaxWidth()[cite: 28]
                                    .aspectRatio(16f / 9f)[cite: 28]
                                    .clip(RoundedCornerShape(16.dp))[cite: 28]
                                    .background(Color.Black)[cite: 28]
                                    .border(BorderStroke(1.5.dp, animatedGradient), RoundedCornerShape(16.dp))[cite: 28]
                                    .clickable {[cite: 28]
                                        trailerVideo?.let { ytVideo ->[cite: 28]
                                            scope.launch {[cite: 28]
                                                val directUrl = InvidiousClient.fetchDirectStreamUrl(ytVideo.videoId)[cite: 28]
                                                if (directUrl != null) {[cite: 28]
                                                    onPlayRequested(directUrl, "${movie.title} - Trailer")[cite: 28]
                                                }
                                            }
                                        }
                                    }
                            ) {
                                if (trailerVideo != null) {[cite: 28]
                                    AsyncImage(
                                        model = trailerVideo!!.getBestThumbnailUrl(),[cite: 28]
                                        contentDescription = "Trailer Thumbnail",[cite: 28]
                                        contentScale = ContentScale.Crop,[cite: 28]
                                        modifier = Modifier.fillMaxSize()[cite: 28]
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))[cite: 28]
                                    
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),[cite: 28]
                                        shape = RoundedCornerShape(20.dp),[cite: 28]
                                        modifier = Modifier.align(Alignment.Center)[cite: 28]
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),[cite: 28]
                                            verticalAlignment = Alignment.CenterVertically[cite: 28]
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Trailer", tint = Color.White)[cite: 28]
                                            Spacer(modifier = Modifier.width(6.dp))[cite: 28]
                                            Text("Autoplay Trailer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)[cite: 28]
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier[cite: 28]
                                            .fillMaxSize()[cite: 28]
                                            .background(Color.DarkGray.copy(alpha = 0.2f)),[cite: 28]
                                        contentAlignment = Alignment.Center[cite: 28]
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))[cite: 28]
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))[cite: 28]
                        }

                        item {[cite: 28]
                            Row(modifier = Modifier.fillMaxWidth()) {[cite: 28]
                                AsyncImage(
                                    model = movie.posterPath ?: android.R.drawable.ic_menu_gallery,[cite: 28]
                                    contentDescription = movie.title,[cite: 28]
                                    modifier = Modifier[cite: 28]
                                        .width(100.dp)[cite: 28]
                                        .aspectRatio(2f / 3f)[cite: 28]
                                        .clip(RoundedCornerShape(10.dp))[cite: 28]
                                        .background(Color.Gray),[cite: 28]
                                    contentScale = ContentScale.Crop[cite: 28]
                                )
                                Spacer(modifier = Modifier.width(18.dp))[cite: 28]
                                Column(modifier = Modifier.weight(1f)) {[cite: 28]
                                    Text(movie.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)[cite: 28]
                                    Spacer(modifier = Modifier.height(4.dp))[cite: 28]
                                    Text("Rating: ★ ${movie.userRating} | Year: ${movie.premiered}", style = MaterialTheme.typography.bodyMedium)[cite: 28]
                                    Text("Genre: ${movie.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)[cite: 28]
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))[cite: 28]
                            Button(
                                onClick = {[cite: 28]
                                    selectedMovie = null[cite: 28]
                                    onPlayRequested(movie.videoFilePath, movie.title)[cite: 28]
                                },
                                modifier = Modifier.fillMaxWidth(),[cite: 28]
                                shape = RoundedCornerShape(14.dp)[cite: 28]
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")[cite: 28]
                                Spacer(modifier = Modifier.width(8.dp))[cite: 28]
                                Text("Play Full Movie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)[cite: 28]
                            }
                            Spacer(modifier = Modifier.height(18.dp))[cite: 28]
                            Text("Plot Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)[cite: 28]
                            Text(
                                movie.plot.ifEmpty { "No description available." },[cite: 28]
                                style = MaterialTheme.typography.bodyMedium,[cite: 28]
                                color = MaterialTheme.colorScheme.onSurfaceVariant,[cite: 28]
                                modifier = Modifier.padding(vertical = 6.dp)[cite: 28]
                            )
                        }
                    }
                }
            }
        }

        // --- TV SHOW DETAIL OVERLAY WITH SORTED EPISODES & MATERIAL YOU GLASSMORPHISM ---
        selectedTvShow?.let { show ->
            if (!show.folderPath.startsWith("cnc_tv:")) {
                val episodes = remember(show) {[cite: 28]
                    NfoScanner.scanTvShowEpisodes(File(show.folderPath))[cite: 28]
                        .sortedBy { it.episode }[cite: 28]
                }
                val seasons = remember(episodes) { episodes.groupBy { it.season }.toSortedMap() }[cite: 28]
                var selectedSeasonTab by remember { mutableStateOf(seasons.keys.firstOrNull() ?: 1) }[cite: 28]

                ModalBottomSheet(
                    onDismissRequest = { selectedTvShow = null },[cite: 28]
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)[cite: 28]
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)) {[cite: 28]
                        Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)[cite: 28]
                        Text("Studio: ${show.studio} | Genre: ${show.genre}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)[cite: 28]
                        Spacer(modifier = Modifier.height(14.dp))[cite: 28]
                        
                        if (seasons.keys.size > 1) {[cite: 28]
                            ScrollableTabRow([cite: 28]
                                selectedTabIndex = seasons.keys.indexOf(selectedSeasonTab).coerceAtLeast(0),[cite: 28]
                                edgePadding = 0.dp,[cite: 28]
                                divider = {}[cite: 28]
                            ) {
                                seasons.keys.forEach { seasonNum ->[cite: 28]
                                    Tab([cite: 28]
                                        selected = selectedSeasonTab == seasonNum,[cite: 28]
                                        onClick = { selectedSeasonTab = seasonNum },[cite: 28]
                                        text = { Text("Season $seasonNum", fontWeight = FontWeight.Bold) }[cite: 28]
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))[cite: 28]

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {[cite: 28]
                            items(seasons[selectedSeasonTab] ?: emptyList()) { episode ->[cite: 28]
                                Box(
                                    modifier = Modifier[cite: 28]
                                        .fillMaxWidth()[cite: 28]
                                        .padding(vertical = 6.dp)[cite: 28]
                                        .clip(RoundedCornerShape(16.dp))[cite: 28]
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))[cite: 28]
                                        .border([cite: 28]
                                            BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),[cite: 28]
                                            RoundedCornerShape(16.dp)[cite: 28]
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),[cite: 28]
                                        verticalAlignment = Alignment.CenterVertically[cite: 28]
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {[cite: 28]
                                            Text(
                                                "Episode ${episode.episode}: ${episode.title}",[cite: 28]
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),[cite: 28]
                                                color = MaterialTheme.colorScheme.onSurface[cite: 28]
                                            )
                                            if (episode.plot.isNotEmpty()) {[cite: 28]
                                                Spacer(modifier = Modifier.height(4.dp))[cite: 28]
                                                Text(
                                                    episode.plot,[cite: 28]
                                                    style = MaterialTheme.typography.bodySmall,[cite: 28]
                                                    maxLines = 2,[cite: 28]
                                                    overflow = TextOverflow.Ellipsis,[cite: 28]
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)[cite: 28]
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))[cite: 28]
                                        IconButton(
                                            onClick = {[cite: 28]
                                                selectedTvShow = null[cite: 28]
                                                onPlayRequested(episode.videoFilePath, "${show.title} - S${episode.season}E${episode.episode}")[cite: 28]
                                            },
                                            colors = IconButtonDefaults.iconButtonColors([cite: 28]
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)[cite: 28]
                                            )[cite: 28]
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Episode", tint = MaterialTheme.colorScheme.onPrimaryContainer)[cite: 28]
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