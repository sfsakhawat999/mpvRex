package app.marlboroadvance.mpvex.ui.browser.shorts

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
            viewModel.loadShorts(initialVideoPath, blockedOnly)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (isLoading && shorts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (shorts.isEmpty()) {
                Text(
                    text = if (blockedOnly) "No blocked videos found" else "No vertical videos found",
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
                        onToggleShuffle = { viewModel.toggleShuffle(pagerState.currentPage) }
                    )
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
    shadow = Shadow(
        color = Color.Black,
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
)

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
    onToggleShuffle: () -> Unit
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
                onToggleShuffle = onToggleShuffle
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
    onToggleShuffle: () -> Unit
) {
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    
    // --- Visual Refinements ---
    var heartTapOffset by remember { mutableStateOf(Offset.Zero) }
    var loveButtonCenter by remember { mutableStateOf(Offset.Zero) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val confettiTrigger = remember { mutableStateOf(0L) }
    
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(video.path) {
        thumbnail = viewModel.getThumbnail(video)
    }

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
                            heartTapOffset = offset
                            coroutineScope.launch {
                                // Premium Animation
                                heartAlpha.snapTo(1f)
                                heartScale.snapTo(0.7f)
                                // Confetti triggered here, but logic below anchors it to button center
                                confettiTrigger.value = System.currentTimeMillis()
                                
                                heartScale.animateTo(1.5f, spring(dampingRatio = 0.5f))
                                delay(300)
                                launch { heartScale.animateTo(2f, tween(400)) }
                                launch { heartAlpha.animateTo(0f, tween(400)) }
                            }
                            // Double click only ADDS love, never removes it.
                            if (!isLoved) onLove()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
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

        // Confetti Effect - now anchored to loveButtonCenter
        ConfettiBurst(trigger = confettiTrigger.value, center = loveButtonCenter)

        // Premium Heart Animation Overlay (still at tap location for visual feedback)
        if (heartAlpha.value > 0f) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        translationX = heartTapOffset.x - 150f
                        translationY = heartTapOffset.y - 150f
                        scaleX = heartScale.value
                        scaleY = heartScale.value
                        alpha = heartAlpha.value
                    }
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 24.dp)
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
            currentSpeed = currentSpeed,
            onLove = onLove,
            onBlock = onBlock,
            onMore = { showMore = true },
            onLoveButtonPositioned = { loveButtonCenter = it }
        )

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

        if (showMore) {
            MoreActionsSheet(
                onDismiss = { showMore = false },
                isShuffleEnabled = isShuffleEnabled,
                onToggleShuffle = onToggleShuffle,
                onShowBlocked = { 
                    showMore = false
                    backstack.add(BlockedShortsScreen)
                },
                onShowInfo = {
                    showMore = false
                    showInfo = true
                }
            )
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
            Offset(
                x = center.x + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance,
                y = center.y + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance
            )
        }
    }

    particles.forEach { targetOffset ->
        val animProgress = remember(trigger) { Animatable(0f) }
        LaunchedEffect(trigger) {
            animProgress.animateTo(1f, tween(600))
        }
        
        if (animProgress.value < 1f) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = center.x + (targetOffset.x - center.x) * animProgress.value - 10f
                        translationY = center.y + (targetOffset.y - center.y) * animProgress.value - 10f
                        alpha = 1f - animProgress.value
                        scaleX = 1f - animProgress.value * 0.5f
                        scaleY = 1f - animProgress.value * 0.5f
                    }
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(listOf(Color.Red, Color.Yellow, Color.White, Color.Magenta).random())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreActionsSheet(
    onDismiss: () -> Unit,
    isShuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onShowBlocked: () -> Unit,
    onShowInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text(if (isShuffleEnabled) "Stop Shuffling" else "Shuffle Shorts") },
                leadingContent = { Icon(Icons.Default.Shuffle, contentDescription = null) },
                modifier = Modifier.clickable { 
                    onToggleShuffle()
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Video Information") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onShowInfo() }
            )
            ListItem(
                headlineContent = { Text("Blocked Videos Manager") },
                leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                modifier = Modifier.clickable { onShowBlocked() }
            )
        }
    }
}

@Composable
private fun ActionColumn(
    modifier: Modifier = Modifier,
    isLoved: Boolean,
    isBlocked: Boolean,
    currentSpeed: Double,
    onLove: () -> Unit,
    onBlock: () -> Unit,
    onMore: () -> Unit,
    onLoveButtonPositioned: (Offset) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Love Button (Semi-transparent as requested)
        Box(modifier = Modifier.alpha(0.8f)) {
            ActionButton(
                icon = if (isLoved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isLoved) "Loved" else "Love",
                iconColor = if (isLoved) Color.Red else Color.White,
                onClick = onLove,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val position = coords.positionInRoot()
                    onLoveButtonPositioned(
                        Offset(
                            position.x + coords.size.width / 2,
                            position.y + coords.size.height / 2
                        )
                    )
                }
            )
        }
        
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
            onClick = {} // Speed is now info-only or we can keep toggle here.
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ActionButton(icon = Icons.Filled.MoreVert, label = "More", onClick = onMore)
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
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
