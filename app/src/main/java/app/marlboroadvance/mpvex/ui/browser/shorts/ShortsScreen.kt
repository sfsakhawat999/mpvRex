package app.marlboroadvance.mpvex.ui.browser.shorts

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                val pagerState = rememberPagerState(pageCount = { shorts.size })
                val lifecycleOwner = LocalLifecycleOwner.current
                val density = LocalDensity.current

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val heightPx = with(density) { maxHeight.toPx() }
                    
                    val scrollOffset = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * heightPx
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = -scrollOffset + (pagerState.currentPage * heightPx)
                                alpha = if (isPlayerReady) 1f else 0f
                            }
                    ) {
                        ShortsPlayerHost(
                            modifier = Modifier.fillMaxSize(),
                            onReady = { mpvView = it },
                            onPlayerReadyChange = { isPlayerReady = it }
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
                            MPVLib.command("loadfile", video.path)
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                    }

                    ShortsPager(
                        shorts = shorts,
                        pagerState = pagerState,
                        lovedPaths = lovedPaths,
                        isPlayerReady = isPlayerReady,
                        viewModel = viewModel,
                        onBack = { 
                            if (backstack.size > 1) {
                                backstack.removeLastOrNull()
                            } else {
                                MainScreen.requestTab(0)
                            }
                        },
                        onLove = { viewModel.toggleLove(it) },
                        onBlock = { viewModel.blockVideo(it) },
                        onShuffle = { viewModel.shuffleShorts() }
                    )
                }
            }
        }
    }

    @Composable
    private fun ShortsPager(
        shorts: List<Video>,
        pagerState: PagerState,
        lovedPaths: Set<String>,
        isPlayerReady: Boolean,
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
                    isPlayerReady = isPlayerReady,
                    isLoved = lovedPaths.contains(video.path),
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
        isPlayerReady: Boolean,
        isLoved: Boolean,
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
        
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        
        LaunchedEffect(video.path) {
            thumbnail = viewModel.getThumbnail(video)
        }

        LaunchedEffect(isSettled, isPressed) {
            if (isSettled && !isPressed) {
                while (isActive) {
                    val pos = MPVLib.getPropertyInt("time-pos") ?: 0
                    val duration = MPVLib.getPropertyInt("duration") ?: 1
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (isSettled) {
                                val currentPause = MPVLib.getPropertyBoolean("pause") ?: false
                                MPVLib.setPropertyBoolean("pause", !currentPause)
                                isPaused = !currentPause
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
        ) {
            // FIX: Removed isPaused from showThumbnail. Only show during swipe/load.
            val showThumbnail = !isSettled || !isPlayerReady
            
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
                            modifier = Modifier.fillMaxSize(),
                            // FIX: Changed ContentScale from Crop to Fit to match video resolution exactly
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
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            ActionColumn(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp),
                isLoved = isLoved,
                onLove = onLove,
                onBlock = onBlock,
                onShuffle = onShuffle,
                onSpeed = { 
                    if (isSettled) {
                        val currentSpeed = MPVLib.getPropertyDouble("speed") ?: 1.0
                        val nextSpeed = when {
                            currentSpeed < 1.0 -> 1.0
                            currentSpeed < 1.5 -> 1.5
                            currentSpeed < 2.0 -> 2.0
                            else -> 0.5
                        }
                        MPVLib.setPropertyDouble("speed", nextSpeed)
                    }
                },
                onInfo = { /* Show info sheet */ }
            )

            if (isPressed && isSettled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    val pos = MPVLib.getPropertyInt("time-pos") ?: 0
                    Text(
                        text = app.marlboroadvance.mpvex.utils.media.MediaFormatter.formatDuration(pos.toLong() * 1000),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
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
                Text(
                    text = video.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }

    @Composable
    private fun ActionColumn(
        modifier: Modifier = Modifier,
        isLoved: Boolean,
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
            ActionButton(icon = Icons.Filled.Shuffle, label = "Shuffle", onClick = onShuffle)
            Spacer(modifier = Modifier.height(20.dp))
            ActionButton(
                icon = if (isLoved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isLoved) "Loved" else "Love",
                iconColor = if (isLoved) Color.Red else Color.White,
                onClick = onLove
            )
            Spacer(modifier = Modifier.height(20.dp))
            ActionButton(icon = Icons.Filled.Block, label = "Block", onClick = onBlock)
            Spacer(modifier = Modifier.height(20.dp))
            ActionButton(icon = Icons.Filled.Speed, label = "Speed", onClick = onSpeed)
            Spacer(modifier = Modifier.height(20.dp))
            ActionButton(icon = Icons.Filled.Info, label = "Info", onClick = onInfo)
            Spacer(modifier = Modifier.height(20.dp))
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
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
