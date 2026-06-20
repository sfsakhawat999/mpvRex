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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.mpv.rex.cinemine.model.MineTab
import xyz.mpv.rex.cinemine.viewmodel.CineMineViewModel
import xyz.mpv.rex.features.cinehub.ui.CineHubGridCard
import xyz.mpv.rex.features.cinehub.data.NfoScanner
import xyz.mpv.rex.features.cinehub.data.CineCloudRepoClient
import xyz.mpv.rex.features.cinetube.ui.CineTubeScreen
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineMineScreen(
    onPlayRequested: (filePath: String, cleanTitle: String) -> Unit
) {
    val viewModel = remember { CineMineViewModel() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var rawMoviesBase by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.MovieItem>()) }
    var rawShowsBase by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.TvShowItem>()) }
    var rawCloudMoviesBase by remember { mutableStateOf(emptyList<xyz.mpv.rex.features.cinehub.model.MovieItem>()) }
    var isCloudSyncing by remember { mutableStateOf(false) }

    val glassShape = RoundedCornerShape(20.dp)
    val glassBorderColor = Color.White.copy(alpha = 0.12f)
    val glassContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.45f)

    LaunchedEffect(Unit) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val movieFolder = File("/sdcard/CineRex/movies")
            val tvFolder = File("/sdcard/CineRex/tvshows")
            
            if (movieFolder.exists()) {
                rawMoviesBase = NfoScanner.scanDirectoryForMovies(movieFolder)
            }
            if (tvFolder.exists()) {
                rawShowsBase = NfoScanner.scanDirectoryForTvShows(tvFolder)
                    .filter { !File(it.folderPath).name.lowercase().contains("season") }
            }
            viewModel.resetFeeds(rawMoviesBase, rawShowsBase, emptyList(), rawCloudMoviesBase)
        }

        scope.launch {
            isCloudSyncing = true
            try {
                rawCloudMoviesBase = CineCloudRepoClient.fetchOnlineMovies(context)
                viewModel.resetFeeds(rawMoviesBase, rawShowsBase, emptyList(), rawCloudMoviesBase)
            } catch (e: Exception) {
                android.util.Log.e("CineMineUI", "Network Cloud configuration fault bypass")
            } finally {
                isCloudSyncing = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))) {
                // Common Floating Glass Search Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(glassShape)
                        .background(glassContainerColor)
                        .border(BorderStroke(0.5.dp, glassBorderColor), glassShape)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { 
                            viewModel.updateSearchAndFilter(it, rawMoviesBase, rawShowsBase, emptyList(), rawCloudMoviesBase) 
                        },
                        placeholder = { Text("Global search across all nodes...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Control Tabs
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
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (viewModel.activeTab) {
                MineTab.UNIFIED -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
                    ) {
                        // Raw Local Movies Slider
                        if (viewModel.filteredLocalMovies.isNotEmpty()) {
                            item {
                                Text("Local Hub Movies", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(viewModel.filteredLocalMovies) { movie ->
                                        Box(modifier = Modifier.width(130.dp)) {
                                            CineHubGridCard(movie.title, movie.genre, movie.userRating, movie.posterPath, movie.watchProgress, movie.isCloudStream) {
                                                onPlayRequested(movie.videoFilePath, movie.title)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Raw Local TV Shows Slider
                        if (viewModel.filteredLocalShows.isNotEmpty()) {
                            item {
                                Text("Local TV Series", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(viewModel.filteredLocalShows) { show ->
                                        Box(modifier = Modifier.width(130.dp)) {
                                            CineHubGridCard(show.title, show.genre, show.userRating, show.posterPath, show.watchProgress, show.isCloudSeries) {
                                                // Dynamic trigger handling
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CineTube Navigation
                        item {
                            Text("CineTube Discoveries", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().height(90.dp).clickable { viewModel.activeTab = MineTab.CINETUBE }, contentAlignment = Alignment.Center) {
                                    Text("Tap to open full frame CineTube streaming engine", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Real Online Server Releases Slider
                        if (viewModel.filteredOnlineCloud.isNotEmpty()) {
                            item {
                                Text("Cloud Repo Releases", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(viewModel.filteredOnlineCloud) { cloudMovie ->
                                        Box(modifier = Modifier.width(130.dp)) {
                                            CineHubGridCard(cloudMovie.title, cloudMovie.genre, cloudMovie.userRating, cloudMovie.posterPath, 0f, cloudMovie.isCloudStream) {
                                                scope.launch {
                                                    val rawId = cloudMovie.videoFilePath.substringAfter("cnc_stream:").substringBefore(":")
                                                    val platformCode = cloudMovie.videoFilePath.substringAfterLast(":")
                                                    val directM3u8 = CineCloudRepoClient.resolveDirectStreamUrl(rawId, platformCode)
                                                    onPlayRequested(directM3u8 ?: "https://net52.cc/mobile/player.php?id=$rawId", cloudMovie.title)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                MineTab.CINETUBE -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CineTubeScreen(onPlayRequested = onPlayRequested)
                    }
                }
                MineTab.CINEHUB_LOCAL -> { /* Local list column view */ }
                MineTab.CINEHUB_ONLINE -> { /* Cloud repo grid view */ }
            }
        }
    }
}
