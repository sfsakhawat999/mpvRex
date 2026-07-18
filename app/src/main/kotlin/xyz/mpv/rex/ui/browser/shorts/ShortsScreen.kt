package xyz.mpv.rex.ui.browser.shorts

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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.MainScreen
import xyz.mpv.rex.ui.player.MPVView
import xyz.mpv.rex.ui.preferences.BlockedShortsScreen
import xyz.mpv.rex.ui.utils.LocalBackStack
import `is`.xyz.mpv.MPVLib
import org.koin.compose.koinInject
import xyz.mpv.rex.preferences.PlayerPreferences
import xyz.mpv.rex.ui.player.PlayerTutorialManager
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.ui.player.controls.components.glassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.math.abs

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
            } else if (shorts.isEmpty() && totalShortsCount > 0) {
                FinishedPageItem(
                    onBack = {
                        viewModel.clearSessionHistory()
                        if (backstack.size > 1) {
                            backstack.removeLastOrNull()
                        } else {
                            MainScreen.requestPreviousTab()
                        }
                    }
                )
            } else if (shorts.isEmpty()) {
                Text(
                    text = if (blockedOnly) stringResource(R.string.no_blocked_videos_found) else stringResource(R.string.no_vertical_videos_found),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                var mpvView by remember { mutableStateOf<MPVView?>(null) }
                var isPlayerReady by remember { mutableStateOf(false) }
                var playingPageIndex by remember { mutableIntStateOf(0) }

                var currentPlaybackProgress by remember { mutableFloatStateOf(0f) }
                var currentPlaybackPaused by remember { mutableStateOf(false) }
                var isManuallyPaused by remember { mutableStateOf(false) }
                var isFreeModeEnabled by remember { mutableStateOf(false) }

                val pagerState = rememberPagerState(pageCount = { 
                    if (isExhausted) shorts.size + 1 else shorts.size 
                })
                val lifecycleOwner = LocalLifecycleOwner.current
                val density = LocalDensity.current
                val coroutineScope = rememberCoroutineScope()

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
                                alpha = if (isPlayerReady && pagerState.settledPage < shorts.size) 1f else 0f
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
                            },
                            onProgressUpdate = { timePos, duration ->
                                currentPlaybackProgress = if (duration > 0) {
                                    (timePos / duration.toDouble()).toFloat().coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            },
                            onPauseUpdate = { paused ->
                                currentPlaybackPaused = paused
                            },
                            onPlaybackEnd = {
                                if (autoSwipe && pagerState.currentPage < shorts.size - 1) {
                                    coroutineScope.launch {
                                        delay(100)
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        )
                    }

                    val isTopScreen = backstack.lastOrNull() == this@ShortsScreen
                    LaunchedEffect(isTopScreen) {
                        if (isTopScreen) {
                            if (!isManuallyPaused && pagerState.settledPage < shorts.size) {
                                MPVLib.setPropertyBoolean("pause", false)
                            }
                        } else {
                            MPVLib.setPropertyBoolean("pause", true)
                        }
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> MPVLib.setPropertyBoolean("pause", true)
                                Lifecycle.Event.ON_RESUME -> {
                                    if (pagerState.settledPage < shorts.size && !isManuallyPaused && backstack.lastOrNull() == this@ShortsScreen) {
                                        MPVLib.setPropertyBoolean("pause", false)
                                    }
                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    val currentVideoPath = shorts.getOrNull(pagerState.settledPage)?.path
                    LaunchedEffect(pagerState.settledPage, mpvView, autoSwipe, currentVideoPath) {
                        if (mpvView != null && shorts.isNotEmpty()) {
                            if (pagerState.settledPage < shorts.size) {
                                val video = shorts[pagerState.settledPage]
                                MPVLib.command("stop") 
                                MPVLib.command("loadfile", video.path)
                                MPVLib.setPropertyString("loop-file", if (autoSwipe) "no" else "inf")
                                MPVLib.setPropertyBoolean("pause", false)
                                isManuallyPaused = false
                                viewModel.syncPlaybackSpeed()

                                // Phase B: Mark as seen in current session
                                viewModel.markAsSeen(video)
                            } else {
                                MPVLib.setPropertyBoolean("pause", true)
                                isPlayerReady = false
                            }
                        }
                    }

                    ShortsPager(
                        shorts = shorts,
                        pagerState = pagerState,
                        lovedPaths = lovedPaths,
                        blockedPaths = blockedPaths,
                        isPlayerReady = isPlayerReady,
                        isExhausted = isExhausted,
                        currentSpeed = currentSpeed,
                        playingPageIndex = playingPageIndex,
                        playbackProgress = currentPlaybackProgress,
                        playbackPaused = currentPlaybackPaused,
                        isFreeModeEnabled = isFreeModeEnabled,
                        onFreeModeToggle = { isFreeModeEnabled = !isFreeModeEnabled },
                        onTogglePause = {
                            val currentPause = MPVLib.getPropertyBoolean("pause") ?: false
                            val nextPause = !currentPause
                            MPVLib.setPropertyBoolean("pause", nextPause)
                            isManuallyPaused = nextPause
                        },
                        viewModel = viewModel,
                        onBack = { 
                            if (isExhausted && pagerState.currentPage >= shorts.size - 1) {
                                viewModel.clearSessionHistory()
                            }
                            if (backstack.size > 1) {
                                backstack.removeLastOrNull()
                            } else {
                                MainScreen.requestPreviousTab()
                            }
                        },
                        onLove = { viewModel.toggleLove(it) },
                        onBlock = { viewModel.toggleBlock(it) }
                    )

                    // System status & navigation bar protective overlays (drawn on top of sliding pages)
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusBarHeight)
                            .background(Color.Black)
                            .align(Alignment.TopCenter)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(navigationBarHeight)
                            .background(Color.Black)
                            .align(Alignment.BottomCenter)
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
private fun FinishedPageItem(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.all_videos_finished),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.all_videos_finished_desc),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 24.dp)
            ) {
                Text(stringResource(R.string.go_back), color = Color.White, fontWeight = FontWeight.Bold)
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
    playbackProgress: Float,
    playbackPaused: Boolean,
    isFreeModeEnabled: Boolean,
    onFreeModeToggle: () -> Unit,
    onTogglePause: () -> Unit,
    viewModel: ShortsViewModel,
    onBack: () -> Unit,
    onLove: (Video) -> Unit,
    onBlock: (Video) -> Unit
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { page -> if (page < shorts.size) shorts[page].path else "finished" }
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
                currentSpeed = currentSpeed,
                playbackProgress = playbackProgress,
                playbackPaused = playbackPaused,
                isFreeModeEnabled = isFreeModeEnabled,
                onFreeModeToggle = onFreeModeToggle,
                onTogglePause = onTogglePause,
                viewModel = viewModel,
                onBack = onBack,
                onLove = { onLove(video) },
                onBlock = { onBlock(video) }
            )
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
    playbackProgress: Float,
    playbackPaused: Boolean,
    isFreeModeEnabled: Boolean,
    onFreeModeToggle: () -> Unit,
    onTogglePause: () -> Unit,
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val playerPreferences = koinInject<PlayerPreferences>()
    val playerTutorialManager = koinInject<PlayerTutorialManager>()
    val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()

    var isLongPressSpeedActive by remember { mutableStateOf(false) }
    var isSpeedLockedState by remember { mutableStateOf(false) }
    var showSpeedLockHint by remember { mutableStateOf(false) }
    val view = LocalView.current
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(video.path) {
        isSpeedLockedState = false
        isLongPressSpeedActive = false
        MPVLib.setPropertyFloat("speed", 1.0f)
    }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            isSpeedLockedState = false
            isLongPressSpeedActive = false
            MPVLib.setPropertyFloat("speed", 1.0f)
        }
    }

    // --- Visual Refinements ---
    var heartTapOffset by remember { mutableStateOf(Offset.Zero) }
    var loveButtonCenter by remember { mutableStateOf(Offset.Zero) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val confettiTrigger = remember { mutableStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    var playPauseTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(video.path) {
        thumbnail = viewModel.getThumbnail(video)
    }

    LaunchedEffect(playbackProgress, isPlaying, isSeeking) {
        if (isPlaying) {
            if (!isSeeking) {
                progress = playbackProgress
            }
        } else {
            progress = 0f
        }
    }

    LaunchedEffect(playbackPaused, isPlaying) {
        if (isPlaying) {
            isPaused = playbackPaused
        } else {
            isPaused = false
        }
    }

    LaunchedEffect(isPaused) {
        if (isPlayerReady) {
            playPauseTrigger = System.currentTimeMillis()
        }
    }

    val progressBarHeight by animateDpAsState(
        targetValue = if (isSeeking) 12.dp else 4.dp,
        animationSpec = tween(300)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // 1. Taps (pause/resume) and Double Taps (like/confetti)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenHeight = size.height
                        val topThreshold = screenHeight * 0.1f
                        val bottomThreshold = screenHeight * 0.9f
                        if (offset.y in topThreshold..bottomThreshold) {
                            if (isSettled && isPlaying) {
                                onTogglePause()
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
                            // Double click only ADDS love, never removes it.
                            if (!isLoved) onLove()
                        }
                    }
                )
            }
            // 2. Left/Right long press speed-up (temporary while holding)
            .pointerInput(holdForMultipleSpeed, isSpeedLockedState) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val screenWidth = size.width.toFloat()
                        val isLeftOrRight = startX < screenWidth * 0.35f || startX > screenWidth * 0.65f

                        if (isLeftOrRight && isSettled && isPlaying && !isPaused) {
                            var longPressTriggered = false
                            val job = coroutineScope.launch {
                                delay(400)
                                longPressTriggered = true
                                isLongPressSpeedActive = true
                                MPVLib.setPropertyFloat("speed", holdForMultipleSpeed)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            }

                            var pointerId = down.id
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == pointerId }
                                if (pointer != null) {
                                    if (!pointer.pressed) {
                                        job.cancel()
                                        if (longPressTriggered) {
                                            isLongPressSpeedActive = false
                                            val targetSpeed = if (isSpeedLockedState) holdForMultipleSpeed else 1.0f
                                            MPVLib.setPropertyFloat("speed", targetSpeed)
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            job.cancel()
                        }
                    }
                }
            }
            // 3. Center drag seeking
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val screenWidth = size.width.toFloat()
                        val isCenter = offset.x in (screenWidth * 0.35f)..(screenWidth * 0.65f)
                        if (isSettled && isPlaying && isCenter) {
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

        // Central Play/Pause Animation Overlay
        if (playPauseTrigger > 0L) {
            val scale = remember { Animatable(0.6f) }
            val alpha = remember { Animatable(0f) }
            LaunchedEffect(playPauseTrigger) {
                alpha.snapTo(0f)
                scale.snapTo(0.6f)
                launch { scale.animateTo(1.2f, spring(dampingRatio = 0.5f)) }
                launch { alpha.animateTo(0.8f, tween(150)) }
                delay(300)
                launch { scale.animateTo(1.5f, tween(300)) }
                launch { alpha.animateTo(0f, tween(300)) }
            }
            if (alpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(scale.value)
                        .alpha(alpha.value)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        val isUiVisible = !isFreeModeEnabled || isPaused

        if (isUiVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(start = 16.dp, end = 96.dp, bottom = 48.dp)
            ) {
                Text(
                    text = video.displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = textWithStroke
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val res = video.resolution
                    if (res.isNotEmpty() && res != "0x0") {
                        MetadataChip(text = res)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (video.sizeFormatted.isNotEmpty()) {
                        MetadataChip(text = video.sizeFormatted)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { if (isSeeking) seekProgress else progress },
                    modifier = Modifier.fillMaxWidth().height(progressBarHeight).clip(RoundedCornerShape(50)),
                    color = if (isSeeking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }

            ActionColumn(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 48.dp, end = 16.dp),
                isLoved = isLoved,
                isBlocked = isBlocked,
                isFreeModeEnabled = isFreeModeEnabled,
                onLove = onLove,
                onBlock = onBlock,
                onFreeModeToggle = onFreeModeToggle,
                onBack = onBack,
                onMore = { showMore = true },
                onLoveButtonPositioned = { loveButtonCenter = it }
            )
        }

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
                    text = xyz.mpv.rex.utils.media.MediaFormatter.formatDuration(currentSeekTime.toLong() * 1000),
                    color = Color.White,
                    fontSize = 48.sp,
                    style = textWithStroke
                )
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(text = stringResource(R.string.video_information)) },
                text = {
                    Column {
                        Text(text = stringResource(R.string.name_label, video.displayName), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = stringResource(R.string.resolution_label, video.width, video.height))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = stringResource(R.string.path_label, video.path), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfo = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }

        if (showMore) {
            val isAutoSwipeEnabled by viewModel.autoSwipe.collectAsState()
            MoreActionsSheet(
                onDismiss = { showMore = false },
                isAutoSwipeEnabled = isAutoSwipeEnabled,
                onToggleAutoSwipe = { viewModel.toggleAutoSwipe() },
                isSpeedLocked = isSpeedLockedState,
                onToggleSpeedLock = { locked ->
                    isSpeedLockedState = locked
                    if (locked) {
                        MPVLib.setPropertyFloat("speed", holdForMultipleSpeed)
                    } else {
                        MPVLib.setPropertyFloat("speed", 1.0f)
                    }
                },
                onShowBlocked = {
                    showMore = false
                    backstack.add(BlockedShortsScreen)
                },
                onShowInfo = {
                    showMore = false
                    showInfo = true
                },
                onDeleteShort = {
                    showMore = false
                    showDeleteConfirmation = true
                }
            )
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(text = stringResource(R.string.delete_short)) },
                text = {
                    Column {
                        Text(text = stringResource(R.string.delete_short_confirm_message), modifier = Modifier.padding(bottom = 8.dp))
                        Text(text = stringResource(R.string.path_colon_label), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = video.path, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            val currentList = viewModel.shorts.value
                            val currentIndex = currentList.indexOf(video)
                            val nextVideo = if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                                currentList[currentIndex + 1]
                            } else if (currentIndex > 0) {
                                currentList[currentIndex - 1]
                            } else {
                                null
                            }

                            if (nextVideo != null) {
                                MPVLib.command("loadfile", nextVideo.path)
                            } else {
                                MPVLib.command("stop")
                            }

                            viewModel.deleteShort(video)
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.generic_cancel))
                    }
                }
            )
        }    }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MoreActionsSheet(
    onDismiss: () -> Unit,
    isAutoSwipeEnabled: Boolean,
    onToggleAutoSwipe: () -> Unit,
    isSpeedLocked: Boolean,
    onToggleSpeedLock: (Boolean) -> Unit,
    onShowBlocked: () -> Unit,
    onShowInfo: () -> Unit,
    onDeleteShort: () -> Unit
) {
    val playerPreferences = koinInject<PlayerPreferences>()
    val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
            )
        }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.auto_swipe_to_next_short)) },
                supportingContent = { Text(stringResource(R.string.auto_swipe_to_next_short_desc)) },
                leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isAutoSwipeEnabled,
                        onCheckedChange = { onToggleAutoSwipe() },
                        modifier = Modifier.scale(0.8f),
                        thumbContent = {
                            Crossfade(
                                targetState = isAutoSwipeEnabled,
                                animationSpec = tween(durationMillis = 200),
                                label = "SwitchIconAnimation"
                            ) { isChecked ->
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    )
                },
                modifier = Modifier.clickable { onToggleAutoSwipe() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.long_press_speed)) },
                supportingContent = { 
                    Text(
                        text = stringResource(R.string.long_press_speed_desc),
                        color = if (isSpeedLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = { Icon(Icons.Default.Speed, contentDescription = null) },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSpeedLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSpeedLocked) stringResource(R.string.speed_multiplier_locked, holdForMultipleSpeed) else stringResource(R.string.speed_multiplier, holdForMultipleSpeed),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                modifier = Modifier.combinedClickable(
                    onClick = {
                        val nextSpeed = when (holdForMultipleSpeed) {
                            1.5f -> 2.0f
                            2.0f -> 2.5f
                            2.5f -> 3.0f
                            3.0f -> 4.0f
                            else -> 1.5f
                        }
                        playerPreferences.holdForMultipleSpeed.set(nextSpeed)
                        if (isSpeedLocked) {
                            MPVLib.setPropertyFloat("speed", nextSpeed)
                        }
                    },
                    onLongClick = {
                        onToggleSpeedLock(!isSpeedLocked)
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                ),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.video_information)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { onShowInfo() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.blocked_videos_manager)) },
                leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                modifier = Modifier.clickable { onShowBlocked() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_short), color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text(stringResource(R.string.delete_short_desc), color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)) },
                leadingContent = { 
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    ) 
                },
                modifier = Modifier.clickable { onDeleteShort() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun ActionColumn(
    modifier: Modifier = Modifier,
    isLoved: Boolean,
    isBlocked: Boolean,
    isFreeModeEnabled: Boolean,
    onLove: () -> Unit,
    onBlock: () -> Unit,
    onFreeModeToggle: () -> Unit,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onLoveButtonPositioned: (Offset) -> Unit
) {
    val browserPreferences = koinInject<BrowserPreferences>()
    val showBackButton by browserPreferences.showShortsBackButton.collectAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Love Button (Semi-transparent as requested)
        Box(modifier = Modifier.alpha(0.8f)) {
            ActionButton(
                icon = if (isLoved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isLoved) stringResource(R.string.loved) else stringResource(R.string.love),
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
            label = if (isBlocked) stringResource(R.string.blocked) else stringResource(R.string.block), 
            iconColor = if (isBlocked) Color.Red else Color.White,
            onClick = onBlock
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Free button (Clean UI Toggle)
        ActionButton(
            icon = if (isFreeModeEnabled) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, 
            label = stringResource(R.string.free), 
            iconColor = if (isFreeModeEnabled) MaterialTheme.colorScheme.primary else Color.White,
            onClick = onFreeModeToggle
        )

        if (showBackButton) {
            Spacer(modifier = Modifier.height(12.dp))

            // Back Button (replaces speed button)
            ActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack, 
                label = stringResource(R.string.back), 
                onClick = onBack
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActionButton(icon = Icons.Filled.MoreVert, label = stringResource(R.string.more), onClick = onMore)
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
    val browserPreferences = koinInject<BrowserPreferences>()
    val enableGlass by browserPreferences.enableGlassShortsControls.collectAsState()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val buttonModifier = if (enableGlass) {
        Modifier.glassSurface(
            shape = RoundedCornerShape(50),
            backgroundColor = Color.White.copy(alpha = 0.05f),
            borderColor = Color.White.copy(alpha = 0.15f),
            borderWidth = 1.dp,
            outerShadowColor = Color.Black.copy(alpha = 0.00f),
            outerShadowBlur = 0.dp,
            outerShadowOffsetX = 0.dp,
            outerShadowOffsetY = 0.dp,
            innerHighlightColor = Color.White.copy(alpha = 0.35f),
            innerHighlightBlur = 5.dp,
            innerHighlightOffsetX = (-2).dp,
            innerHighlightOffsetY = (-2).dp,
            innerShadowColor = Color.Black.copy(alpha = 0.35f),
            innerShadowBlur = 5.dp,
            innerShadowOffsetX = 2.dp,
            innerShadowOffsetY = 2.dp
        )
    } else {
        Modifier.background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        IconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(48.dp)
                .then(buttonModifier)
                .clip(RoundedCornerShape(50))
                .padding(4.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp).graphicsLayer { translationX = 1f; translationY = 1f }
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            style = textWithStroke
        )
    }
}

@Composable
private fun MetadataChip(text: String) {
    val browserPreferences = koinInject<BrowserPreferences>()
    val enableGlass by browserPreferences.enableGlassShortsControls.collectAsState()

    val chipModifier = if (enableGlass) {
        Modifier.glassSurface(
            shape = RoundedCornerShape(6.dp),
            backgroundColor = Color.White.copy(alpha = 0.05f),
            borderColor = Color.White.copy(alpha = 0.15f),
            borderWidth = 1.dp,
            outerShadowColor = Color.Black.copy(alpha = 0.00f),
            outerShadowBlur = 0.dp,
            outerShadowOffsetX = 0.dp,
            outerShadowOffsetY = 0.dp,
            innerHighlightColor = Color.White.copy(alpha = 0.35f),
            innerHighlightBlur = 5.dp,
            innerHighlightOffsetX = (-2).dp,
            innerHighlightOffsetY = (-2).dp,
            innerShadowColor = Color.Black.copy(alpha = 0.35f),
            innerShadowBlur = 5.dp,
            innerShadowOffsetX = 2.dp,
            innerShadowOffsetY = 2.dp
        )
    } else {
        Modifier.background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
    }

    Box(
        modifier = Modifier
            .then(chipModifier)
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
