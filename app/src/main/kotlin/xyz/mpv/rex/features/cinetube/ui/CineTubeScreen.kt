package xyz.mpv.rex.features.cinetube.ui

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.mpv.rex.features.cinetube.data.InvidiousClient
import xyz.mpv.rex.features.cinetube.model.YoutubeVideo
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineTubeScreen(
    onPlayRequested: (String, String) -> Unit
) {
    var videoList by remember { mutableStateOf<List<YoutubeVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isSearchBarVisible by remember { mutableStateOf(false) } 
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    // Bottom Sheets & Dialogs States
    var selectedVideoForQuality by remember { mutableStateOf<YoutubeVideo?>(null) }
    var isQualitySheetOpen by remember { mutableStateOf(false) }
    var isLoginDialogOpen by remember { mutableStateOf(false) }
    
    // Auth Form States
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAuthInFlight by remember { mutableStateOf(false) }
    var userSessionActive by remember { mutableStateOf(InvidiousClient.userAuthToken != null) }

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Network Engine Loop for Personalized vs Regional Feeds
    LaunchedEffect(refreshTrigger, isSearching, userSessionActive) {
        isLoading = true
        videoList = if (isSearching && searchQuery.isNotBlank()) {
            InvidiousClient.fetchSearchVideos(searchQuery)
        } else if (userSessionActive) {
            InvidiousClient.fetchPersonalizedFeed()
        } else {
            InvidiousClient.fetchTrendingVideos("Movies")
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BrowserTopBar(
                    title = if (isSearching) "Search Results" else if (userSessionActive) "Personal Feed" else "CineTube Popular",
                    isInSelectionMode = false,
                    selectedCount = 0,
                    totalCount = videoList.size,
                    onCancelSelection = {},
                    isHomeScreen = true, 
                    onSearchClick = { isSearchBarVisible = !isSearchBarVisible }
                )

                AnimatedVisibility(
                    visible = isSearchBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp),
                            placeholder = { Text("Search videos popular in India...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty() || isSearching) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        isSearching = false
                                        isSearchBarVisible = false
                                        keyboardController?.hide()
                                        refreshTrigger++
                                    }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    refreshTrigger++ 
                                }
                                keyboardController?.hide()
                            })
                        )
                    }
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
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Personalized Header Info Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (userSessionActive) "Personalized Account Feed Active" else "Guest Mode (India Content Preferred)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    if (userSessionActive) {
                                        InvidiousClient.userAuthToken = null
                                        userSessionActive = false
                                        refreshTrigger++
                                    } else {
                                        isLoginDialogOpen = true
                                    }
                                }
                            ) {
                                Text(if (userSessionActive) "Logout" else "Account Login", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (videoList.isEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSearching) "No search results found for India." else "Network timeout or node offline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { refreshTrigger++ }) {
                                Text(text = "Retry Core Connection", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            items(videoList) { video ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedVideoForQuality = video
                                            isQualitySheetOpen = true
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
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = video.author,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (video.authorVerified) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Verified Profile",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
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
            }

            // Invidious Secure Account Login Modal
            if (isLoginDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isLoginDialogOpen = false },
                    title = { Text("CineTube Login", fontWeight = FontWeight.Black) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Log in to your active instance node to fetch custom playlists and feeds instantly.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = !isAuthInFlight && username.isNotBlank() && password.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    isAuthInFlight = true
                                    val success = InvidiousClient.loginUser(username, password)
                                    isAuthInFlight = false
                                    if (success) {
                                        userSessionActive = true
                                        isLoginDialogOpen = false
                                        username = ""
                                        password = ""
                                    }
                                }
                            }
                        ) {
                            if (isAuthInFlight) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Authorize", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isLoginDialogOpen = false }) { Text("Cancel") }
                    }
                )
            }

            // Quality Selection Bottom Sheet Matrix
            if (isQualitySheetOpen && selectedVideoForQuality != null) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        isQualitySheetOpen = false
                        selectedVideoForQuality = null
                    },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 36.dp)
                    ) {
                        Text(text = "Select Video Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = selectedVideoForQuality!!.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))

                        val qualityOptions = listOf(
                            Pair("1080p FHD Stream", "Premium quality rendering mode"),
                            Pair("720p HD Stream", "Optimal playback configuration"),
                            Pair("360p SD Stream", "Low bandwidth performance matrix")
                        )

                        qualityOptions.forEach { option ->
                            ListItem(
                                headlineContent = { Text(option.first, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(option.second) },
                                leadingContent = { Icon(Icons.Default.HighQuality, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val targetVideo = selectedVideoForQuality!!
                                        isQualitySheetOpen = false
                                        selectedVideoForQuality = null
                                        
                                        scope.launch {
                                            val directStreamUrl = InvidiousClient.fetchDirectStreamUrl(targetVideo.videoId)
                                            if (directStreamUrl != null) {
                                                onPlayRequested(directStreamUrl, targetVideo.title)
                                            }
                                        }
                                    },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
