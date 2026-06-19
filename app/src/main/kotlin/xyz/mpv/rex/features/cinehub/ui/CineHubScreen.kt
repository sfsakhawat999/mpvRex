package xyz.mpv.rex.features.cinehub.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Movie
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
import xyz.mpv.rex.features.cinehub.model.MovieItem
import xyz.mpv.rex.features.cinehub.model.TvShowItem
import xyz.mpv.rex.features.cinehub.model.EpisodeItem
import xyz.mpv.rex.features.cinehub.model.ActorItem
import xyz.mpv.rex.features.cinehub.data.NfoScanner
import xyz.mpv.rex.features.cinehub.data.CineCloudRepoClient
import xyz.mpv.rex.features.cinetube.data.InvidiousClient
import xyz.mpv.rex.features.cinetube.model.YoutubeVideo
import xyz.mpv.rex.features.cinetube.ui.CineTubeScreen
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineHubScreen(
    moviesList: List<MovieItem>,
    tvShowsList: List<TvShowItem>,
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit
) {
    // 0 = CineHub Standard Matrix, 1 = CineTube Streaming Engine Node
    var moduleModeSwitch by remember { mutableIntStateOf(0) }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Movies", "TV Shows")
    
    var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
    var selectedTvShow by remember { mutableStateOf<TvShowItem?>(null) }
    var activeActorLookup by remember { mutableStateOf<String?>(null) }

    // State placeholders for decoupled real-time synchronization observers
    var localMoviesState by remember { mutableStateOf(moviesList) }
    var localTvShowsState by remember { mutableStateOf(tvShowsList) }
    var onlineMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var onlineTvShows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3

    // Dynamic execution engine triggers folder auto-sync scanners instantly on launch
    LaunchedEffect(Unit) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val movieFolder = File("/sdcard/CineRex/movies")
            val tvFolder = File("/sdcard/CineRex/tvshows")
            
            if (movieFolder.exists()) {
                val scanned = NfoScanner.scanDirectoryForMovies(movieFolder)
                if (scanned.isNotEmpty()) localMoviesState = scanned
            }
            if (tvFolder.exists()) {
                val scanned = NfoScanner.scanDirectoryForTvShows(tvFolder)
                if (scanned.isNotEmpty()) localTvShowsState = scanned
            }
        }
    }

    LaunchedEffect(tabIndex) {
        if (onlineMovies.isEmpty() || onlineTvShows.isEmpty()) {
            isOnlineLoading = true
            scope.launch {
                try {
                    onlineMovies = CineCloudRepoClient.fetchOnlineMovies(context)
                    onlineTvShows = CineCloudRepoClient.fetchOnlineTvShows(context)
                } catch (e: Exception) {
                    android.util.Log.e("CineHubUI", "Network fault bypass: " + e.message)
                } verify {
                    isOnlineLoading = false
                }
            }
        }
    }

    // Main Engine Branch Control Router Panel
    if (moduleModeSwitch == 1) {
        Box(modifier = Modifier.fillMaxSize()) {
            CineTubeScreen(onPlayRequested = onPlayRequested)
            
            // Premium Floating Action Return Toggle
            FilledTonalIconButton(
                onClick = { moduleModeSwitch = 0 },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
                    .size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(imageVector = Icons.Default.Movie, contentDescription = "Switch to CineHub")
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BrowserTopBar(
                        title = "CineHub",
                        isInSelectionMode = false,
                        selectedCount = 0,
                        totalCount = localMoviesState.size + localTvShowsState.size,
                        onCancelSelection = {},
                        isHomeScreen = true,
                        onSearchClick = {}
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TabRow(
                                selectedTabIndex = tabIndex,
                                containerColor = Color.Transparent,
                                divider = {},
                                modifier = Modifier.weight(1f)
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = tabIndex == index,
                                        onClick = { tabIndex = index },
                                        text = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            // Mode Switch Hook resembling standard day/night triggers
                            IconButton(
                                onClick = { moduleModeSwitch = 1 },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Switch to CineTube",
                                    tint = MaterialTheme.colorScheme.primary
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
                // ================= LAYER 1: LOCAL MEDIA ROW SLIDER =================
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
                    val currentLocalList = if (tabIndex == 0) localMoviesState else localTvShowsState
                    if (currentLocalList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No local items detected inside /sdcard/CineRex/ folder index.", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (tabIndex == 0) {
                                items(localMoviesState) { movie ->
                                    Box(modifier = Modifier.width(135.dp)) {
                                        CineHubGridCard(
                                            title = movie.title,
                                            genre = movie.genre,
                                            rating = movie.userRating,
                                            posterPath = movie.posterPath,
                                            watchProgress = movie.watchProgress,
                                            isCloudItem = movie.isCloudStream,
                                            onClick = { selectedMovie = movie }
                                        )
                                    }
                                }
                            } else {
                                items(localTvShowsState) { show ->
                                    Box(modifier = Modifier.width(135.dp)) {
                                        CineHubGridCard(
                                            title = show.title,
                                            genre = show.genre,
                                            rating = show.userRating,
                                            posterPath = show.posterPath,
                                            watchProgress = show.watchProgress,
                                            isCloudItem = show.isCloudSeries,
                                            onClick = { selectedTvShow = show }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= LAYER 2: TRENDING RELEASES MATRICES =================
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
                                                        watchProgress = 0f,
                                                        isCloudItem = movieItem.isCloudStream,
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
                                                        watchProgress = 0f,
                                                        isCloudItem = tvShowItem.isCloudSeries,
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
        }

        // --- MOVIES DETAIL OVERLAY ---
        selectedMovie?.let { movie ->
            if (!movie.videoFilePath.startsWith("cnc_stream:")) {
                var trailerVideo by remember { mutableStateOf<YoutubeVideo?>(null) }
                val movieActors = remember(movie) { NfoScanner.parseActorsFromNfo(NfoScanner.getXmlDocument(File(movie.videoFilePath.replace(File(movie.videoFilePath).name, "movie.nfo"))) ?: return@remember emptyList()) }
                
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
                                )
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.DarkGray.copy(alpha = 0.2f)),
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
                                    Text("Rating: ★ ${movie.getFormattedRating()} | Year: ${movie.premiered}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Genre: ${movie.genre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (movie.actors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Cast & Characters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    items(movie.actors) { actor ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(76.dp)
                                                .clickable { activeActorLookup = actor.name }
                                        ) {
                                            AsyncImage(
                                                model = actor.getBestActorThumb(),
                                                contentDescription = actor.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.LightGray)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(actor.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                        }
                                    }
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
        }

        // --- TV SHOWS DETAIL OVERLAY ---
        selectedTvShow?.let { show ->
            if (!show.folderPath.startsWith("cnc_tv:")) {
                val episodes = remember(show) { NfoScanner.scanTvShowEpisodes(File(show.folderPath)).sortedBy { it.episode } }
                val seasons = remember(episodes) { episodes.groupBy { it.season }.toSortedMap() }
                var selectedSeasonTab by remember { mutableStateOf(seasons.keys.firstOrNull() ?: 1) }

                ModalBottomSheet(
                    onDismissRequest = { selectedTvShow = null },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 8.dp)) {
                        Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Studio: ${show.studio} | Genre: ${show.genre}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (show.actors.isNotEmpty()) {
                            Text("Cast & Characters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                items(show.actors) { actor ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clickable { activeActorLookup = actor.name }
                                    ) {
                                        AsyncImage(
                                            model = actor.getBestActorThumb(),
                                            contentDescription = actor.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.LightGray)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(actor.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .border(
                                            BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                            RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${episode.getEpisodeCode()}: ${episode.title}",
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
                                                    onPlayRequested(episode.videoFilePath, "${show.title} - ${episode.getEpisodeCode()}")
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

        // --- SUB-BOTTOM SHEET: FILMOGRAPHY LOOKUP ---
        activeActorLookup?.let { actorName ->
            val (actorMovies, actorShows) = remember(actorName) { NfoScanner.getSharedFilmography(actorName, moviesList, tvShowsList) }
            ModalBottomSheet(
                onDismissRequest = { activeActorLookup = null },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(text = "Filmography: $actorName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp), 
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        items(actorMovies) { movie ->
                            CineHubGridCard(movie.title, movie.genre, movie.userRating, movie.posterPath, movie.watchProgress, movie.isCloudStream) {
                                activeActorLookup = null
                                selectedMovie = movie
                            }
                        }
                        items(actorShows) { show ->
                            CineHubGridCard(show.title, show.genre, show.userRating, show.posterPath, show.watchProgress, show.isCloudSeries) {
                                activeActorLookup = null
                                selectedTvShow = show
                            }
                        }
                    }
                }
            }
        }
    }
}