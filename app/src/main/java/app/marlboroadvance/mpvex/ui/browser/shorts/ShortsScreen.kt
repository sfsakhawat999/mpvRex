package app.marlboroadvance.mpvex.ui.browser.shorts

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable

@Serializable
object ShortsScreen : Screen {

    private val textWithStroke = TextStyle(
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val backstack = LocalBackStack.current
        val viewModel: ShortsViewModel = viewModel(
            factory = ShortsViewModel.factory(context.applicationContext as android.app.Application)
        )

        val shorts by viewModel.shorts.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val lovedPaths by viewModel.lovedPaths.collectAsState()
        val blockedPaths by viewModel.blockedPaths.collectAsState()
        val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
        val currentSpeed by viewModel.currentSpeed.collectAsState()
        
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

        LaunchedEffect(Unit) {
            viewModel.loadShorts()
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (isLoading && shorts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (shorts.isEmpty()) {
                Text(
                    text = "No vertical videos found",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                var mpvView by remember { mutableStateOf<MPVView?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                var playingPageIndex by remember { mutableIntStateOf(0) }
                
                val pagerState = rememberPagerState(pageCount = { shorts.size })
                val lifecycleOwner = LocalLifecycleOwner.current
                val density = LocalDensity.current

                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    val heightPx = with(density) { maxHeight.toPx() }
                    val totalScroll = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val scrollOffset = totalScroll * heightPx
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .graphicsLayer {
                                translationY = -scrollOffset + (playingPageIndex * heightPx)
                                alpha = if (isPlayerReady) 1f else 0f
                            }
                    ) {
                        ShortsPlayerHost(
                            modifier = Modifier.fillMaxSize(),
                            onReady = { mpvView = it },
                            onPlayerReadyChange = { ready ->
                                isPlayerReady = ready
                                if (ready) {
                                    playingPageIndex = pagerState.settledPage
                                }
                            }
                        )
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> MPVLib.setPropertyBoolean("pause", true)
                                Lifecycle.Event.ON_RESUME -> MPVLib.setPropertyBoolean("pause", false)
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    LaunchedEffect(pagerState.settledPage, mpvView) {
                        if (mpvView != null && shorts.isNotEmpty() && pagerState.settledPage < shorts.size) {
                            val video = shorts[pagerState.settledPage]
                            MPVLib.command("stop") 
                            MPVLib.command("loadfile", video.path)
                            MPVLib.setPropertyBoolean("pause", false)
                            viewModel.updatePlaybackSpeed()
                        }
                    }

                    ShortsPager(
                        shorts = shorts,
                        pagerState = pagerState,
                        lovedPaths = lovedPaths,
                        blockedPaths = blockedPaths,
                        isPlayerReady = isPlayerReady,
                        isShuffleEnabled = isShuffleEnabled,
                        currentSpeed = currentSpeed,
                        playingPageIndex = playingPageIndex,
                        viewModel = viewModel,
                        onBack = { 
                            if (backstack.size > 1) {
                                backstack.removeLastOrNull()
                            } else {
                                MainScreen.requestTab(0)
                            }
                        },
                        onLove = { viewModel.toggleLove(it) },
                        onBlock = { viewModel.toggleBlock(it) },
                        onShuffle = { viewModel.toggleShuffle(pagerState.currentPage) }
                    )
                }
            }
        }
    }

    private fun androidx.compose.ui.graphics.Color.luminance(): Float {
        return 0.299f * red + 0.587f * green + 0.114f * blue
    }

    @Composable
    private fun ShortsPager(
        shorts: List<Video>,
        pagerState: PagerState,
        lovedPaths: Set<String>,
        blockedPaths: Set<String>,
        isPlayerReady: Boolean,
        isShuffleEnabled: Boolean,
        currentSpeed: Double,
        playingPageIndex: Int,
        viewModel: ShortsViewModel,
        onBack: () -> Unit,
        onLove: (Video) -> Unit,
        onBlock: (Video) -> Unit,
        onShuffle: () -> Unit
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            if (page < shorts.size) {
                val video = shorts[page]
                ShortPageItem(
                    video = video,
                    isCurrent = page == pagerState.currentPage,
                    isSettled = page == pagerState.settledPage,
                    isPlaying = page == playingPageIndex,
                    isPlayerReady = isPlayerReady,
                    isLoved = lovedPaths.contains(video.path),
                    isBlocked = blockedPaths.contains(video.path),
                    isShuffleEnabled = isShuffleEnabled,
                    currentSpeed = currentSpeed,
                    viewModel = viewModel,
                    onBack = onBack,
                    onLove = { onLove(video) },
                    onBlock = { onBlock(video) },
                    onShuffle = onShuffle
                )
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
        isShuffleEnabled: Boolean,
        currentSpeed: Double,
        viewModel: ShortsViewModel,
        onBack: () -> Unit,
        onLove: () -> Unit,
        onBlock: () -> Unit,
        onShuffle: () -> Unit
    ) {
        var progress by remember { mutableFloatStateOf(0f) }
        var isPaused by remember { mutableStateOf(false) }
        var showHeart by remember { mutableStateOf(false) }
        var heartOffset by remember { mutableStateOf(Offset.Zero) }
        var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
        var showInfo by remember { mutableStateOf(false) }
        
        // --- Phase 2: Seeking States ---
        var isSeeking by remember { mutableStateOf(false) }
        var seekProgress by remember { mutableFloatStateOf(0f) }
        
        val interactionSource = remember { MutableInteractionSource() }
        
        LaunchedEffect(video.path) {
            thumbnail = viewModel.getThumbnail(video)
        }

        // Track progress if this is the settled page
        LaunchedEffect(isSettled, isPlaying, isSeeking) {
            if (isSettled && isPlaying && !isSeeking) {
                while (isActive) {
                    val pos = MPVLib.getPropertyInt("time-pos") ?: 0
                    val duration = MPVLib.getPropertyInt("duration") ?: 1
                    // Correctly handle duration for accurate progress
                    progress = if (duration > 0) pos.toFloat() / duration.toFloat() else 0f
                    isPaused = MPVLib.getPropertyBoolean("pause") ?: false
                    delay(500)
                }
            }
        }

        val heartScale by animateFloatAsState(
            targetValue = if (showHeart) 1.5f else 0f,
            animationSpec = spring(),
            finishedListener = { if (it == 1.5f) showHeart = false }
        )

        val progressBarHeight by animateDpAsState(
            targetValue = if (isSeeking) 12.dp else 4.dp,
            animationSpec = tween(300)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val screenHeight = size.height
                            val topThreshold = screenHeight * 0.1f
                            val bottomThreshold = screenHeight * 0.9f
                            
                            if (offset.y in topThreshold..bottomThreshold) {
                                if (isSettled && isPlaying) {
                                    val currentPause = MPVLib.getPropertyBoolean("pause") ?: false
                                    MPVLib.setPropertyBoolean("pause", !currentPause)
                                    isPaused = !currentPause
                                }
                            }
                        },
                        onDoubleTap = { offset ->
                            if (isSettled) {
                                heartOffset = offset
                                showHeart = true
                                if (!isLoved) onLove()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Implement Press-and-Hold to Seek
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            if (isSettled && isPlaying) {
                                isSeeking = true
                                seekProgress = progress
                            }
                        },
                        onDragEnd = {
                            if (isSeeking) {
                                val duration = MPVLib.getPropertyInt("duration") ?: 0
                                if (duration > 0) {
                                    val newPos = (seekProgress * duration).toInt()
                                    MPVLib.setPropertyInt("time-pos", newPos)
                                    progress = seekProgress
                                }
                                isSeeking = false
                            }
                        },
                        onDragCancel = { isSeeking = false },
                        onDrag = { change, dragAmount ->
                            if (isSeeking) {
                                // Horizontal drag changes progress
                                val screenWidth = size.width.toFloat()
                                val delta = dragAmount.x / screenWidth
                                seekProgress = (seekProgress + delta).coerceIn(0f, 1f)
                                change.consume()
                            }
                        }
                    )
                }
        ) {
            val showThumbnail = !isPlaying || !isPlayerReady
            
            Crossfade(
                targetState = showThumbnail,
                animationSpec = tween(300),
                label = "thumbnail_fade"
            ) { targetShowThumbnail ->
                if (targetShowThumbnail) {
                    thumbnail?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().background(Color.Black),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }

            if (!isCurrent) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }

            if (showHeart || heartScale > 0f) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            translationX = heartOffset.x - 150f
                            translationY = heartOffset.y - 150f
                            scaleX = heartScale
                            scaleY = heartScale
                            alpha = (1.5f - heartScale).coerceIn(0f, 1f)
                        }
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 24.dp)
                    .graphicsLayer {
                        shadowElevation = 0f
                    }
            ) {
                Box {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp).graphicsLayer { translationX = 2f; translationY = 2f }
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            ActionColumn(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp),
                isLoved = isLoved,
                isBlocked = isBlocked,
                isShuffleEnabled = isShuffleEnabled,
                currentSpeed = currentSpeed,
                onLove = onLove,
                onBlock = onBlock,
                onShuffle = onShuffle,
                onSpeed = { 
                    if (isSettled && isPlaying) {
                        val currentSpd = MPVLib.getPropertyDouble("speed") ?: 1.0
                        val nextSpeed = when {
                            currentSpd < 1.0 -> 1.0
                            currentSpd < 1.5 -> 1.5
                            currentSpd < 2.0 -> 2.0
                            else -> 0.5
                        }
                        MPVLib.setPropertyDouble("speed", nextSpeed)
                        viewModel.updatePlaybackSpeed()
                    }
                },
                onInfo = { showInfo = true }
            )

            // --- Phase 2: Seeking Tooltip ---
            if (isSeeking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    val duration = MPVLib.getPropertyInt("duration") ?: 0
                    val currentSeekTime = (seekProgress * duration).toInt()
                    Text(
                        text = app.marlboroadvance.mpvex.utils.media.MediaFormatter.formatDuration(currentSeekTime.toLong() * 1000),
                        color = Color.White,
                        fontSize = 48.sp,
                        style = textWithStroke
                    )
                }
            }

            // Bottom Progress
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(start = 16.dp, end = 16.dp, bottom = 48.dp)
            ) {
                // FIX: Title removed as it's available in Info button
                LinearProgressIndicator(
                    progress = { if (isSeeking) seekProgress else progress },
                    modifier = Modifier.fillMaxWidth().height(progressBarHeight),
                    color = if (isSeeking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }

            if (showInfo) {
                AlertDialog(
                    onDismissRequest = { showInfo = false },
                    title = { Text(text = "Video Info") },
                    text = {
                        Column {
                            Text(text = "Name: ${video.displayName}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Resolution: ${video.width}x${video.height}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Path: ${video.path}", fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfo = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ActionColumn(
        modifier: Modifier = Modifier,
        isLoved: Boolean,
        isBlocked: Boolean,
        isShuffleEnabled: Boolean,
        currentSpeed: Double,
        onLove: () -> Unit,
        onBlock: () -> Unit,
        onShuffle: () -> Unit,
        onSpeed: () -> Unit,
        onInfo: () -> Unit
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionButton(
                icon = Icons.Filled.Shuffle, 
                label = if (isShuffleEnabled) "Shuffled" else "Shuffle",
                iconColor = if (isShuffleEnabled) Color(0xFF2E7D32) else Color.White,
                onClick = onShuffle
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                icon = if (isLoved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isLoved) "Loved" else "Love",
                iconColor = if (isLoved) Color.Red else Color.White,
                onClick = onLove
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                icon = Icons.Filled.Block, 
                label = if (isBlocked) "Blocked" else "Block", 
                iconColor = if (isBlocked) Color.Red else Color.White,
                onClick = onBlock
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                icon = Icons.Filled.Speed, 
                label = "${currentSpeed}x", 
                onClick = onSpeed
            )
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(icon = Icons.Filled.Info, label = "Info", onClick = onInfo)
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(icon = Icons.Filled.MoreVert, label = "More") { /* TODO */ }
        }
    }

    @Composable
    private fun ActionButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        iconColor: Color = Color.White,
        onClick: () -> Unit
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp).graphicsLayer { translationX = 1f; translationY = 1f }
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                style = textWithStroke
            )
        }
    }
}
