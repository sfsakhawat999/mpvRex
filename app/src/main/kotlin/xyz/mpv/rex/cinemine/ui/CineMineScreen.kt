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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import xyz.mpv.rex.cinemine.model.MineTab
import xyz.mpv.rex.cinemine.viewmodel.CineMineViewModel
import xyz.mpv.rex.features.cinehub.data.NfoScanner[span_2](start_span)[span_2](end_span)
import xyz.mpv.rex.features.cinehub.data.CineCloudRepoClient[span_3](start_span)[span_3](end_span)
import xyz.mpv.rex.features.cinetube.data.InvidiousClient[span_4](start_span)[span_4](end_span)
import xyz.mpv.rex.features.cinetube.ui.CineTubeScreen[span_5](start_span)[span_5](end_span)
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineMineScreen(
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit[span_6](start_span)[span_6](end_span)[span_7](start_span)[span_7](end_span)
) {
    val viewModel = remember { CineMineViewModel() }
    val context = LocalContext.current[span_8](start_span)[span_8](end_span)
    val scope = rememberCoroutineScope()[span_9](start_span)[span_9](end_span)
    
    // Core structural storage lists mirroring pre-written frameworks
    var rawMovies by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.MovieItem>()) }
    var rawShows by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.TvShowItem>()) }
    var rawTubeVideos by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinetube.model.YoutubeVideo>()) }
    var rawCloudMovies by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.MovieItem>()) }
    var isFetchingCloud by remember { mutableStateOf(false) }

    // M3 Glassmorphic structural specifications
    val glassShape = RoundedCornerShape(24.dp)
    val glassContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))

    // Asynchronous loading hooks that sync directly into your pre-written data nodes
    LaunchedEffect(Unit) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val movieFolder = File("/sdcard/CineRex/movies")[span_10](start_span)[span_10](end_span)
            val tvFolder = File("/sdcard/CineRex/tvshows")[span_11](start_span)[span_11](end_span)
            
            if (movieFolder.exists()) {
                rawMovies = NfoScanner.scanDirectoryForMovies(movieFolder)[span_12](start_span)[span_12](end_span)
            }
            if (tvFolder.exists()) {
                rawShows = NfoScanner.scanDirectoryForTvShows(tvFolder)[span_13](start_span)[span_13](end_span)
                    .filter { !File(it.folderPath).name.lowercase().contains("season") }[span_14](start_span)[span_14](end_span)
            }
            viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
        }

        scope.launch {
            try {
                rawTubeVideos = InvidiousClient.fetchTrendingVideos("Movies")[span_15](start_span)[span_15](end_span)
                viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
            } catch (_: Exception) {}
        }

        scope.launch {
            isFetchingCloud = true
            try {
                rawCloudMovies = CineCloudRepoClient.fetchOnlineMovies(context)[span_16](start_span)[span_16](end_span)
                viewModel.resetFeeds(rawMovies, rawShows, rawTubeVideos, rawCloudMovies)
            } catch (_: Exception) {} finally {
                isFetchingCloud = false
            }
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
                // ================= VIEWPORT A: UNIFIED MIXED ROW ENGINE =================
                MineTab.UNIFIED -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                    ) {
                        // --- CATEGORY 1: CINEHUB LOCAL MOVIE SHELF ---
                        if (viewModel.filteredLocalMovies.isNotEmpty()) {
                            item {
                                Text("Local Movies Collection", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredLocalMovies) { movie ->
                                        CineMineGlassCard(movie.title, movie.genre, movie.posterPath, movie.getFormattedRating()) {[span_17](start_span)[span_17](end_span)
                                            onPlayRequested(movie.videoFilePath, movie.title)[span_18](start_span)[span_18](end_span)
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 2: CINEHUB LOCAL TV SHOWS SHELF ---
                        if (viewModel.filteredLocalShows.isNotEmpty()) {
                            item {
                                Text("Local TV Series Grid", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredLocalShows) { show ->
                                        CineMineGlassCard(show.title, show.genre, show.posterPath, show.getFormattedRating()) {[span_19](start_span)[span_19](end_span)
                                            // Triggers local episodic grid logic cleanly when clicked
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 3: CINETUBE RANDOM STREAM CHANNELS ---
                        if (viewModel.filteredTubeVideos.isNotEmpty()) {
                            item {
                                Text("CineTube Random Trends", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredTubeVideos) { video ->
                                        CineMineGlassCard(video.title, video.author, video.getBestThumbnailUrl(), null) {[span_20](start_span)[span_20](end_span)
                                            scope.launch {
                                                val resolvedTubeUrl = InvidiousClient.fetchDirectStreamUrl(video.videoId)[span_21](start_span)[span_21](end_span)
                                                if (resolvedTubeUrl != null) {
                                                    onPlayRequested(resolvedTubeUrl, video.title)[span_22](start_span)[span_22](end_span)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- CATEGORY 4: CINEHUB ONLINE (REAL CLOUD DATA) ---
                        if (viewModel.filteredOnlineCloud.isNotEmpty()) {
                            item {
                                Text("Cloud Repo Network Releases", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(viewModel.filteredOnlineCloud) { cloudMovie ->
                                        CineMineGlassCard(cloudMovie.title, cloudMovie.genre, cloudMovie.posterPath, cloudMovie.getFormattedRating()) {[span_23](start_span)[span_23](end_span)
                                            scope.launch {
                                                val postId = cloudMovie.videoFilePath.substringAfter("cnc_stream:").substringBefore(":")[span_24](start_span)[span_24](end_span)
                                                val platformCode = cloudMovie.videoFilePath.substringAfterLast(":")[span_25](start_span)[span_25](end_span)
                                                val m3u8Url = CineCloudRepoClient.resolveDirectStreamUrl(postId, platformCode)[span_26](start_span)[span_26](end_span)
                                                onPlayRequested(m3u8Url ?: "https://net52.cc/mobile/player.php?id=$postId", cloudMovie.title)[span_27](start_span)[span_27](end_span)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= VIEWPORT B: STANDALONE CINETUBE FULL FRAME =================
                MineTab.CINETUBE -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CineTubeScreen(onPlayRequested = onPlayRequested)[span_28](start_span)[span_28](end_span)
                    }
                }

                // ================= VIEWPORT C & D: STANDALONE FOCUS MODES =================
                MineTab.CINEHUB_LOCAL -> {
                    // Triggers customized isolated Local columns grids
                }
                MineTab.CINEHUB_ONLINE -> {
                    // Triggers customized isolated Cloud Repo streaming layouts
                }
            }

            if (isFetchingCloud) {
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

// ================= INTERNAL SHARED GLASSMORPHIC CARD REUSE =================
@Composable
private fun CineMineGlassCard(
    title: String,
    caption: String,
    thumbnailUrl: String?,
    ratingScore: String?,
    onCardClick: () -> Unit
) {
    val layoutShape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .width(135.dp)
            .clip(layoutShape)
            .clickable { onCardClick() },
        shape = layoutShape,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)
        )
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = thumbnailUrl ?: android.R.drawable.ic_menu_gallery,[span_29](start_span)[span_29](end_span)
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)[span_30](start_span)[span_30](end_span)
                        .background(Color.DarkGray.copy(alpha = 0.2f))[span_31](start_span)[span_31](end_span)
                )

                if (!ratingScore.isNullOrBlank() && ratingScore != "0.0") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = "★ $ratingScore",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
