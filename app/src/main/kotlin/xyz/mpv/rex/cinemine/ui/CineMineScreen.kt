package xyz.mpv.rex.cinemine.ui

import android.content.res.Configuration
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.mpv.rex.cinemine.model.MineTab
import xyz.mpv.rex.cinemine.model.MovieItem
import xyz.mpv.rex.cinemine.model.TvShowItem
import xyz.mpv.rex.cinemine.model.YoutubeVideo
import xyz.mpv.rex.cinemine.viewmodel.CineMineViewModel
import xyz.mpv.rex.cinemine.ui.components.MovieItemCard
import xyz.mpv.rex.cinemine.ui.components.TvShowItemCard
import xyz.mpv.rex.cinemine.ui.components.YoutubeVideoCard
import xyz.mpv.rex.cinemine.ui.components.TvShowDetailSheet
import xyz.mpv.rex.cinemine.data.CineMineRepo
import xyz.mpv.rex.cinemine.data.CineMineStreamResolver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineMineScreen(
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit
) {
    val viewModel = remember { CineMineViewModel() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val gridColumnCount = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 3
    
    // Core data references pool securely typed to local model package
    var rawMovies by remember { mutableStateOf(emptyList<MovieItem>()) }
    var rawShows by remember { mutableStateOf(emptyList<TvShowItem>()) }
    var rawTubeVideos by remember { mutableStateOf(emptyList<YoutubeVideo>()) }
    var rawCloudMovies by remember { mutableStateOf(emptyList<MovieItem>()) }
    var isFetchingData by remember { mutableStateOf(false) }

    // M3 Glassmorphic specifications
    val glassShape = RoundedCornerShape(24.dp)
    val glassContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))

    // Asynchronous synchronization engine pipeline loading data safely into unified states
    LaunchedEffect(Unit) {
        isFetchingData = true
        scope.launch {
            rawMovies = CineMineRepo.fetchLocalMovies()
            viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
        }
        scope.launch {
            rawShows = CineMineRepo.fetchLocalTvShows()
            viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
        }
        scope.launch {
            rawTubeVideos = CineMineRepo.fetchCineTubeTrending()
            viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
        }
        scope.launch {
            rawCloudMovies = CineMineRepo.fetchCineMaxReleases(context)
            viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
            isFetchingData = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            ) {
                // ================= COMPONENT 1: COMMON FLOATING GLASS SEARCH BAR =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(glassShape)
                        .background(glassContainerColor)
                        .border(glassBorder, glassShape)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { 
                            viewModel.updateSearchAndFilter(it, rawMovies, rawShows, rawTubeVideos, rawCloudMovies) 
                        },
                        placeholder = { 
                            Text(
                                "Search Movies, TV Shows or Streams globally...", 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            ) 
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // ================= COMPONENT 2: STANDALONE EXPLORATION TABS =================
                ScrollableTabRow(
                    selectedTabIndex = viewModel.activeTab.ordinal,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    MineTab.values().forEach { tab ->
                        val isSelected = viewModel.activeTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.activeTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
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
            when (viewModel.activeTab) {
                // ================= VIEWPORT A: UNIFIED INTEGRATED DISCOVERY VIEWPORT =================
                MineTab.UNIFIED -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                    ) {
                        // --- CATEGORY 1: LOCAL MOVIES ROW SLIDER ---
                        if (viewModel.filteredLocalMovies.isNotEmpty()) {
                            item {
                                Text("Local Movies Collection", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredLocalMovies) { movie ->
                                        MovieItemCard(movie.title, movie.genre, movie.userRating, movie.posterPath, isCloud = false) {
                                            scope.launch {
                                                val streamLink = CineMineStreamResolver.resolvePlaybackUrl(movie.videoFilePath)
                                                onPlayRequested(streamLink, movie.title)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 2: LOCAL TV SHOWS ROW SLIDER ---
                        if (viewModel.filteredLocalShows.isNotEmpty()) {
                            item {
                                Text("Local TV Series Grid", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredLocalShows) { show ->
                                        TvShowItemCard(show.title, show.genre, show.userRating, show.posterPath) {
                                            viewModel.openTvShowDetails(show)
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 3: CINETUBE ROW SLIDER ---
                        if (viewModel.filteredTubeVideos.isNotEmpty()) {
                            item {
                                Text("CineTube Random Trends", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredTubeVideos) { video ->
                                        Box(modifier = Modifier.width(220.dp)) {
                                            YoutubeVideoCard(video.title, video.author, video.lengthSeconds, video.getBestThumbnailUrl()) {
                                                scope.launch {
                                                    val resolvedTubeUrl = CineMineStreamResolver.resolvePlaybackUrl(video.videoId)
                                                    onPlayRequested(resolvedTubeUrl, video.title)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 4: CINEHUB ONLINE ROW SLIDER ---
                        if (viewModel.filteredOnlineCloud.isNotEmpty()) {
                            item {
                                Text("Cloud Repo Network Releases", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredOnlineCloud) { cloudMovie ->
                                        MovieItemCard(cloudMovie.title, cloudMovie.genre, cloudMovie.userRating, cloudMovie.posterPath, isCloud = true) {
                                            scope.launch {
                                                val streamLink = CineMineStreamResolver.resolvePlaybackUrl(cloudMovie.videoFilePath)
                                                onPlayRequested(streamLink, cloudMovie.title)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= VIEWPORT B: STANDALONE CINETUBE GRID MATRIX =================
                MineTab.CINETUBE -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 1),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.filteredTubeVideos) { video ->
                            YoutubeVideoCard(video.title, video.author, video.lengthSeconds, video.getBestThumbnailUrl()) {
                                scope.launch {
                                    val resolvedTubeUrl = CineMineStreamResolver.resolvePlaybackUrl(video.videoId)
                                    onPlayRequested(resolvedTubeUrl, video.title)
                                }
                            }
                        }
                    }
                }

                // ================= VIEWPORT C: STANDALONE FOCUS LOCAL HUB =================
                MineTab.CINEHUB_LOCAL -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumnCount),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.filteredLocalMovies) { movie ->
                            MovieItemCard(movie.title, movie.genre, movie.userRating, movie.posterPath, isCloud = false) {
                                scope.launch {
                                    val streamLink = CineMineStreamResolver.resolvePlaybackUrl(movie.videoFilePath)
                                    onPlayRequested(streamLink, movie.title)
                                }
                            }
                        }
                    }
                }

                // ================= VIEWPORT D: STANDALONE FOCUS ONLINE MODULE =================
                MineTab.CINEHUB_ONLINE -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumnCount),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.filteredOnlineCloud) { cloudMovie ->
                            MovieItemCard(cloudMovie.title, cloudMovie.genre, cloudMovie.userRating, cloudMovie.posterPath, isCloud = true) {
                                scope.launch {
                                    val streamLink = CineMineStreamResolver.resolvePlaybackUrl(cloudMovie.videoFilePath)
                                    onPlayRequested(streamLink, cloudMovie.title)
                                }
                            }
                        }
                    }
                }
            }

            // Display dynamic multi-season layout bottom sheet overlay triggered via structural states
            viewModel.selectedTvShowForSheet?.let { activeShow ->
                TvShowDetailSheet(
                    show = activeShow,
                    onDismiss = { viewModel.closeTvShowDetails() },
                    onPlayRequested = onPlayRequested
                )
            }

            if (isFetchingData) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .size(28.dp),
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
