package app.marlboroadvance.mpvex.ui.browser.shorts

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.MainScreen
import app.marlboroadvance.mpvex.ui.player.MPVView
import app.marlboroadvance.mpvex.ui.preferences.BlockedShortsScreen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.MPVLib
import app.marlboroadvance.mpvex.youtube.data.InvidiousClient
import app.marlboroadvance.mpvex.youtube.model.YoutubeVideo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class ShortsScreen(
    val initialVideoPath: String? = null,
    val blockedOnly: Boolean = false
) : Screen {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val backstack = LocalBackStack.current
        val viewModel: ShortsViewModel = viewModel(
            factory = ShortsViewModel.factory(context.applicationContext as android.app.Application)
        )

        val shorts by viewModel.shorts.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isExhausted by viewModel.isExhausted.collectAsState()
        val totalShortsCount by viewModel.totalShortsCount.collectAsState()
        val lovedPaths by viewModel.lovedPaths.collectAsState()
        val blockedPaths by viewModel.blockedPaths.collectAsState()
        val autoSwipe by viewModel.autoSwipe.collectAsState()
        val currentSpeed by viewModel.currentSpeed.collectAsState()

        // --- NEW: Online YouTube State Management Parameters ---
        var selectedSourceTab by remember { mutableIntStateOf(0) } // 0 = Local, 1 = YouTube
        var onlineShortsList by remember { mutableStateOf<List<YoutubeVideo>>(emptyList()) }
        var isOnlineLoading by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val view = LocalView.current
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        
        if (!view.isInEditMode) {
            DisposableEffect(Unit) {
                val window = (view.context as Activity).window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
                onDispose {
                    insetsController.isAppearanceLightStatusBars = !isDarkTheme
                    insetsController.isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
        }

        // Fetch local database shorts references
        LaunchedEffect(Unit) {
            viewModel.loadShorts(initialVideoPath, blockedOnly)
        }

        // --- NEW: Live Online Streams Trigger Mechanism ---
        LaunchedEffect(selectedSourceTab) {
            if (selectedSourceTab == 1 && onlineShortsList.isEmpty()) {
                isOnlineLoading = true
                onlineShortsList = InvidiousClient.fetchTrendingVideos("Anime") // Optimized high-tempo target stream tags
                isOnlineLoading = false
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Check active content pipelines conditionally based on chosen layout context switches
            if (selectedSourceTab == 0) {
                // --- ORIGINAL RENDER LOGIC FOR OFFLINE STORAGE ---
                if (isLoading && shorts.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (shorts.isEmpty() && totalShortsCount > 0) {
                    FinishedPageItem(onBack = {
                        viewModel.clearSessionHistory()
                        if (backstack.size > 1) backstack.removeLastOrNull() else MainScreen.requestPreviousTab()
                    })
                } else if (shorts.isEmpty()) {
                    Text(text = if (blockedOnly) "No blocked videos found" else "No vertical videos found", color = Color.White, modifier = Modifier.align(Alignment.Center))
                } else {
                    RenderLocalShortsContainer(shorts, pagerState = rememberPagerState(pageCount = { if (isExhausted) shorts.size + 1 else shorts.size }), lovedPaths, blockedPaths, isExhausted, autoSwipe, currentSpeed, viewModel, backstack)
                }
            } else {
                // --- NEW: RENDER PIPELINE INTEGRATION FOR ONLINE INVIDIOUS YOUTUBE STREAMS ---
                if (isOnlineLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (onlineShortsList.isEmpty()) {
                    Text(text = "Failed to load network streams.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                } else {
                    RenderOnlineShortsContainer(onlineShortsList, rememberPagerState(pageCount = { onlineShortsList.size }), autoSwipe, currentSpeed, backstack)
                }
            }

            // --- NEW: Translucent Context Swapping Overlay Tabs on Header Baseline ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row {
                        listOf("Local", "YouTube").forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (selectedSourceTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { 
                                        MPVLib.command("stop")
                                        selectedSourceTab = index 
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderLocalShortsContainer(
        shorts: List<Video>,
        pagerState: PagerState,
        lovedPaths: Set<String>,
        blockedPaths: Set<String>,
        isExhausted: Boolean,
        autoSwipe: Boolean,
        currentSpeed: Double,
        viewModel: ShortsViewModel,
        backstack: app.marlboroadvance.mpvex.ui.utils.BackStack<Screen>
    ) {
        var mpvView by remember { mutableStateOf<MPVView?>(null) }
        var isPlayerReady by remember { mutableStateOf(false) }
        var playingPageIndex by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val lifecycleOwner = LocalLifecycleOwner.current

        Box(modifier = Modifier.fillMaxSize()) {
            val heightPx = with(density) { maxHeight.toPx() }
            val totalScroll = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val scrollOffset = totalScroll * heightPx
            
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
                translationY = -scrollOffset + (playingPageIndex * heightPx)
                alpha = if (isPlayerReady && pagerState.settledPage < shorts.size) 1f else 0f
            }) {
                ShortsPlayerHost(modifier = Modifier.fillMaxSize(), onReady = { mpvView = it }, onPlayerReadyChange = { ready ->
                    isPlayerReady = ready
                    if (ready) playingPageIndex = pagerState.settledPage
                })
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) MPVLib.setPropertyBoolean("pause", true)
                    else if (event == Lifecycle.Event.ON_RESUME && pagerState.settledPage < shorts.size) MPVLib.setPropertyBoolean("pause", false)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(pagerState.settledPage, mpvView, autoSwipe) {
                if (mpvView != null && shorts.isNotEmpty()) {
                    if (pagerState.settledPage < shorts.size) {
                        val video = shorts[pagerState.settledPage]
                        MPVLib.command("stop") 
                        MPVLib.command("loadfile", video.path)
                        MPVLib.setPropertyString("loop-file", if (autoSwipe) "no" else "inf")
                        MPVLib.setPropertyBoolean("pause", false)
                        viewModel.syncPlaybackSpeed()
                        viewModel.markAsSeen(video)
                    } else {
                        MPVLib.command("stop")
                        isPlayerReady = false
                    }
                }
            }

            // Auto Swipe runtime processor loop
            LaunchedEffect(isPlayerReady, autoSwipe, pagerState.settledPage) {
                if (isPlayerReady && autoSwipe) {
                    while (isActive) {
                        val eofReached = MPVLib.getPropertyBoolean("eof-reached") ?: false
                        if (eofReached && pagerState.currentPage < shorts.size - 1) {
                            delay(100)
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            break
                        }
                        delay(500)
                    }
                }
            }

            ShortsPager(shorts = shorts, pagerState = pagerState, lovedPaths = lovedPaths, blockedPaths = blockedPaths, isPlayerReady = isPlayerReady, isExhausted = isExhausted, currentSpeed = currentSpeed, playingPageIndex = playingPageIndex, viewModel = viewModel, onBack = {
                if (isExhausted && pagerState.currentPage >= shorts.size - 1) viewModel.clearSessionHistory()
                if (backstack.size > 1) backstack.removeLastOrNull() else MainScreen.requestPreviousTab()
            }, onLove = { viewModel.toggleLove(it) }, onBlock = { viewModel.toggleBlock(it) })
        }
    }

    @Composable
    private fun RenderOnlineShortsContainer(
        onlineShorts: List<YoutubeVideo>,
        pagerState: PagerState,
        autoSwipe: Boolean,
        currentSpeed: Double,
        backstack: app.marlboroadvance.mpvex.ui.utils.BackStack<Screen>
    ) {
        var mpvView by remember { mutableStateOf<MPVView?>(null) }
        var isPlayerReady by remember { mutableStateOf(false) }
        var playingPageIndex by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            val heightPx = with(density) { maxHeight.toPx() }
            val scrollOffset = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * heightPx
            
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).graphicsLayer {
                translationY = -scrollOffset + (playingPageIndex * heightPx)
                alpha = if (isPlayerReady) 1f else 0f
            }) {
                ShortsPlayerHost(modifier = Modifier.fillMaxSize(), onReady = { mpvView = it }, onPlayerReadyChange = { ready ->
                    isPlayerReady = ready
                    if (ready) playingPageIndex = pagerState.settledPage
                })
            }

            LaunchedEffect(pagerState.settledPage, mpvView) {
                if (mpvView != null && onlineShorts.isNotEmpty()) {
                    isPlayerReady = false
                    val ytVideo = onlineShorts[pagerState.settledPage]
                    MPVLib.command("stop")
                    scope.launch {
                        val streamUrl = InvidiousClient.fetchDirectStreamUrl(ytVideo.videoId)
                        if (streamUrl != null) {
                            MPVLib.command("loadfile", streamUrl)
                            MPVLib.setPropertyString("loop-file", if (autoSwipe) "no" else "inf")
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                    }
                }
            }

            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val video = onlineShorts[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isPlayerReady || page != playingPageIndex) {
                        AsyncImage(
                            model = video.getBestThumbnailUrl(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Transparent dynamic title detail overlay card at bottom base layout
                    Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(24.dp)) {
                        Text(text = video.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                        Text(text = "@${video.author}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    private fun androidx.compose.ui.graphics.Color.luminance(): Float {
        return 0.299f * red + 0.587f * green + 0.114f * blue
    }
}

private val textWithStroke = TextStyle(
    fontWeight = FontWeight.Bold,
    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
)

@Composable
private fun FinishedPageItem(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "All videos finished", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "You've seen all vertical videos for now.", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onBack, modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 24.dp)) {
                Text("Go Back", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShortsPager(
    shorts: List<Video>,
    pagerState: PagerState,
    lovedPaths: Set<String>,
    blockedPaths: Set<String>,
    isPlayerReady: Boolean,
    isExhausted: Boolean,
    currentSpeed: Double,
    playingPageIndex: Int,
    viewModel: ShortsViewModel,
    onBack: () -> Unit,
    onLove: (Video) -> Unit,
    onBlock: (Video) -> Unit
) {
    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
        if (page < shorts.size) {
            val video = shorts[page]
            ShortPageItem(video = video, isCurrent = page == pagerState.currentPage, isSettled = page == pagerState.settledPage, isPlaying = page == playingPageIndex, isPlayerReady = isPlayerReady, isLoved = lovedPaths.contains(video.path), isBlocked = blockedPaths.contains(video.path), currentSpeed = currentSpeed, viewModel = viewModel, onBack = onBack, onLove = { onLove(video) }, onBlock = { onBlock(video) })
        } else if (isExhausted) {
            FinishedPageItem(onBack = onBack)
        }
    }
}

@Composable
private fun ShortPageItem(
    video: Video,
    isCurrent: Boolean,
    isSettled: Boolean,
    isPlaying: Boolean,
    isPlayerReady: Boolean,
    isLoved: Boolean,
    isBlocked: Boolean,
    currentSpeed: Double,
    viewModel: ShortsViewModel,
    onBack: () -> Unit,
    onLove: () -> Unit,
    onBlock: () -> Unit
) {
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    
    var heartTapOffset by remember { mutableStateOf(Offset.Zero) }
    var loveButtonCenter by remember { mutableStateOf(Offset.Zero) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val confettiTrigger = remember { mutableStateOf(0L) }
    
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(video.path) { thumbnail = viewModel.getThumbnail(video) }

    LaunchedEffect(isSettled, isPlaying, isSeeking) {
        if (isSettled && isPlaying && !isSeeking) {
            while (isActive) {
                val pos = MPVLib.getPropertyInt("time-pos") ?: 0
                val duration = MPVLib.getPropertyInt("duration") ?: 1
                progress = if (duration > 0) pos.toFloat() / duration.toFloat() else 0f
                isPaused = MPVLib.getPropertyBoolean("pause") ?: false
                delay(500)
            }
        }
    }

    val progressBarHeight by animateDpAsState(targetValue = if (isSeeking) 12.dp else 4.dp, animationSpec = tween(300), label = "")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenHeight = size.height
                        if (offset.y in (screenHeight * 0.1f)..(screenHeight * 0.9f)) {
                            if (isSettled && isPlaying) {
                                val currentPause = MPVLib.getPropertyBoolean("pause") ?: false
                                MPVLib.setPropertyBoolean("pause", !currentPause)
                                isPaused = !currentPause
                            }
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isSettled) {
                            heartTapOffset = offset
                            coroutineScope.launch {
                                heartAlpha.snapTo(1f)
                                heartScale.snapTo(0.7f)
                                confettiTrigger.value = System.currentTimeMillis()
                                heartScale.animateTo(1.5f, spring(dampingRatio = 0.5f))
                                delay(300)
                                launch { heartScale.animateTo(2f, tween(400)) }
                                launch { heartAlpha.animateTo(0f, tween(400)) }
                            }
                            if (!isLoved) onLove()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { if (isSettled && isPlaying) { isSeeking = true; seekProgress = progress } },
                    onDragEnd = {
                        if (isSeeking) {
                            val duration = MPVLib.getPropertyInt("duration") ?: 0
                            if (duration > 0) {
                                MPVLib.setPropertyInt("time-pos", (seekProgress * duration).toInt())
                                progress = seekProgress
                            }
                            isSeeking = false
                        }
                    },
                    onDragCancel = { isSeeking = false },
                    onDrag = { change, dragAmount ->
                        if (isSeeking) {
                            seekProgress = (seekProgress + (dragAmount.x / size.width.toFloat())).coerceIn(0f, 1f)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        if (!isPlaying || !isPlayerReady) {
            thumbnail?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().background(Color.Black), contentScale = ContentScale.Fit)
            } ?: Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        if (!isCurrent) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

        ConfettiBurst(trigger = confettiTrigger.value, center = loveButtonCenter)

        if (heartAlpha.value > 0f) {
            Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(100.dp).graphicsLayer {
                translationX = heartTapOffset.x - 150f
                translationY = heartTapOffset.y - 150f
                scaleX = heartScale.value
                scaleY = heartScale.value
                alpha = heartAlpha.value
            })
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 24.dp)) {
            Box {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(28.dp).graphicsLayer { translationX = 2f; translationY = 2f })
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        ActionColumn(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp), isLoved = isLoved, isBlocked = isBlocked, currentSpeed = currentSpeed, onLove = onLove, onBlock = onBlock, onSpeedClick = { viewModel.cycleSpeed() }, onMore = { showMore = true }, onLoveButtonPositioned = { loveButtonCenter = it })

        if (isSeeking) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Text(text = app.marlboroadvance.mpvex.utils.media.MediaFormatter.formatDuration((seekProgress * (MPVLib.getPropertyInt("duration") ?: 0)).toLong() * 1000), color = Color.White, fontSize = 48.sp, style = textWithStroke)
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).padding(start = 16.dp, end = 16.dp, bottom = 48.dp)) {
            LinearProgressIndicator(progress = { if (isSeeking) seekProgress else progress }, modifier = Modifier.fillMaxWidth().height(progressBarHeight), color = MaterialTheme.colorScheme.primary, trackColor = Color.White.copy(alpha = 0.3f))
        }

        if (showInfo) {
            AlertDialog(onDismissRequest = { showInfo = false }, title = { Text(text = "Video Info") }, text = {
                Column {
                    Text(text = "Name: ${video.displayName}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Resolution: ${video.width}x${video.height}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Path: ${video.path}", fontSize = 12.sp)
                }
            }, confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Close") } })
        }

        if (showMore) {
            val isAutoSwipeEnabled by viewModel.autoSwipe.collectAsState()
            MoreActionsSheet(onDismiss = { showMore = false }, isAutoSwipeEnabled = isAutoSwipeEnabled, onToggleAutoSwipe = { viewModel.toggleAutoSwipe() }, onShowBlocked = { backstack.add(BlockedShortsScreen) }, onShowInfo = { showInfo = true })
        }
    }
}

@Composable
private fun ConfettiBurst(trigger: Long, center: Offset) {
    if (trigger == 0L || center == Offset.Zero) return
    val particles = remember(trigger) {
        List(15) {
            val angle = Random.nextFloat() * 360f
            val distance = 50f + Random.nextFloat() * 150f
            Offset(center.x + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance, center.y + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance)
        }
    }
    particles.forEach { targetOffset ->
        val animProgress = remember(trigger) { Animatable(0f) }
        LaunchedEffect(trigger) { animProgress.animateTo(1f, tween(600)) }
        if (animProgress.value < 1f) {
            Box(modifier = Modifier.graphicsLayer {
                translationX = center.x + (targetOffset.x - center.x) * animProgress.value - 10f
                translationY = center.y + (targetOffset.y - center.y) * animProgress.value - 10f
                alpha = 1f - animProgress.value
                scaleX = 1f - animProgress.value * 0.5f
                scaleY = 1f - animProgress.value * 0.5f
            }.size(8.dp).clip(RoundedCornerShape(50)).background(listOf(Color.Red, Color.Yellow, Color.White, Color.Magenta).random()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreActionsSheet(onDismiss: () -> Unit, isAutoSwipeEnabled: Boolean, onToggleAutoSwipe: () -> Unit, onShowBlocked: () -> Unit, onShowInfo: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(), dragHandle = { Box(modifier = Modifier.padding(vertical = 12.dp).size(width = 32.dp, height = 4.dp).background(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape = MaterialTheme.shapes.extraLarge)) }) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(headlineContent = { Text("Auto Swipe to Next Short") }, supportingContent = { Text("Swipe automatically when video ends") }, leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) }, trailingContent = {
                Switch(checked = isAutoSwipeEnabled, onCheckedChange = { onToggleAutoSwipe() }, modifier = Modifier.scale(0.8f), thumbContent = {
                    Crossfade(targetState = isAutoSwipeEnabled, animationSpec = tween(durationMillis = 200), label = "") { isChecked ->
                        Icon(imageVector = if (isChecked) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
                    }
                })
            }, modifier = Modifier.clickable { onToggleAutoSwipe() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
            ListItem(headlineContent = { Text("Video Information") }, leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }, modifier = Modifier.clickable { onShowInfo() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
            ListItem(headlineContent = { Text("Blocked Videos Manager") }, leadingContent = { Icon(Icons.Default.Block, contentDescription = null) }, modifier = Modifier.clickable { onShowBlocked() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
        }
    }
}

@Composable
private fun ActionColumn(modifier: Modifier = Modifier, isLoved: Boolean, isBlocked: Boolean, currentSpeed: Double, onLove: () -> Unit, onBlock: () -> Unit, onSpeedClick: () -> Unit, onMore: () -> Unit, onLoveButtonPositioned: (Offset) -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.alpha(0.8f)) {
            ActionButton(icon = if (isLoved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, label = if (isLoved) "Loved" else "Love", iconColor = if (isLoved) Color.Red else Color.White, onClick = onLove, modifier = Modifier.onGloballyPositioned { coords ->
                val position = coords.positionInRoot()
                onLoveButtonPositioned(Offset(position.x + coords.size.width / 2, position.y + coords.size.height / 2))
            })
        }
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(icon = Icons.Filled.Block, label = if (isBlocked) "Blocked" else "Block", iconColor = if (isBlocked) Color.Red else Color.White, onClick = onBlock)
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(icon = Icons.Filled.Speed, label = "${currentSpeed}x", onClick = onSpeedClick)
        Spacer(modifier = Modifier.height(12.dp))
        ActionButton(icon = Icons.Filled.MoreVert, label = "More", onClick = onMore)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, iconColor: Color = Color.White, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Box {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp).graphicsLayer { translationX = 1f; translationY = 1f })
                Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(26.dp))
            }
        }
        Text(text = label, color = Color.White, fontSize = 11.sp, style = textWithStroke)
    }
}