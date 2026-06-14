package app.marlboroadvance.mpvex.youtube.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.youtube.data.InvidiousClient
import app.marlboroadvance.mpvex.youtube.model.YoutubeVideo
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeTabScreen(
    onPlayRequested: (String, String) -> Unit
) {
    var videoList by remember { mutableStateOf<List<YoutubeVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Combined Network Pipeline for Search Query vs Default Trending Stream Fetching
    LaunchedEffect(refreshTrigger, isSearching) {
        isLoading = true
        videoList = if (isSearching && searchQuery.isNotBlank()) {
            // If search bar is active, fetch from search route endpoints
            InvidiousClient.fetchSearchVideos(searchQuery)
        } else {
            // Default baseline screen mode: trending stream data binding
            InvidiousClient.fetchTrendingVideos("Movies")
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isSearching) "Search Results" else "YouTube Live Stream",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                    
                    // --- NEW: Premium Integrated Search Bar TextField Component ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, bottom = 12.dp),
                        placeholder = { Text("Search videos, anime, movies...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || isSearching) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearching = false
                                    keyboardController?.hide()
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                refreshTrigger++ // Forces trigger re-evaluation block execution
                            }
                            keyboardController?.hide()
                        })
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (videoList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSearching) "No search results found." else "Network timeout or instance down.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { refreshTrigger++ }) {
                        Text(text = if (isSearching) "Refresh Search" else "Retry Connection", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videoList) { video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val directStreamUrl = InvidiousClient.fetchDirectStreamUrl(video.videoId)
                                        if (directStreamUrl != null) {
                                            onPlayRequested(directStreamUrl, video.title)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = video.getBestThumbnailUrl(),
                                        contentDescription = video.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Live length duration stamp badge on bottom right corner of thumbnail
                                    if (video.lengthSeconds > 0) {
                                        val minutes = video.lengthSeconds / 60
                                        val seconds = video.lengthSeconds % 60
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = String.format("%d:%02d", minutes, seconds),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 4.bind { 4.dp }, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = video.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension helper for safe padding conversions
private fun Int.bind(block: () -> androidx.compose.ui.unit.Dp) = block()
