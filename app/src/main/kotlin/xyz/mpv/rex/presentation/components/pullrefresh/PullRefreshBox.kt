package xyz.mpv.rex.presentation.components.pullrefresh

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A reusable Box that wraps content with pull-to-refresh functionality.
 *
 * @param isRefreshing Tracks refresh state.
 * @param onRefresh Invoked when refresh is triggered.
 * @param modifier Box modifier.
 * @param enabled Toggles pull-to-refresh.
 * @param listState Unused. Reserved for top-scroll checks.
 * @param refreshingOffset Unused. Reserved for offset customization.
 * @param refreshThreshold Pull distance required to trigger refresh.
 * @param delayAfterRefresh Delay (ms) to keep indicator visible after completion.
 * @param content Content displayed inside the Box.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PullRefreshBox(
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  @Suppress("UNUSED_PARAMETER") listState: LazyListState? = null,
  @Suppress("UNUSED_PARAMETER") refreshingOffset: Dp = 80.dp,
  refreshThreshold: Dp = 80.dp,
  delayAfterRefresh: Long = 800L,
  content: @Composable BoxScope.() -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val state = rememberPullToRefreshState()
  val density = LocalDensity.current

  // Max pixel offset the content is pushed down.
  val maxTranslationPx = with(density) { refreshThreshold.toPx() }

  // Tracks active refresh to cancel previous jobs on rapid pulls.
  val activeJob = remember { mutableStateOf<Job?>(null) }

  // Target translation: hold at max if refreshing, otherwise track drag distance.
  val targetTranslationY = if (isRefreshing.value) {
    maxTranslationPx
  } else {
    (state.distanceFraction * maxTranslationPx).coerceAtLeast(0f)
  }

  // Spring animation for content snap/bounce.
  val animatedTranslationY by animateFloatAsState(
    targetValue = targetTranslationY,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "content_translationY",
  )

  // Indicator scale/alpha: 0 at rest, 1 at threshold/refreshing.
  val indicatorScale by animateFloatAsState(
    targetValue = if (isRefreshing.value) 1f else state.distanceFraction.coerceIn(0f, 1f),
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "indicator_scale",
  )

  // Morphing polygon sequence for indeterminate state.
  val expressivePolygons = remember {
    listOf(
      MaterialShapes.Cookie4Sided,
      MaterialShapes.SoftBurst,
      MaterialShapes.Oval,
    )
  }

  // Cached indicator size.
  val indicatorSize = 56.dp
  val indicatorSizePx = remember(density) { with(density) { indicatorSize.toPx() } }

  Box(
    modifier = modifier.pullToRefresh(
      isRefreshing = isRefreshing.value,
      state = state,
      enabled = enabled,
      onRefresh = {
        activeJob.value?.cancel()
        isRefreshing.value = true
        activeJob.value = coroutineScope.launch {
          try {
            onRefresh()
            delay(delayAfterRefresh)
          } finally {
            isRefreshing.value = false
          }
        }
      },
    ),
  ) {
    // LAYER 1: Content (moves down during drag/refresh)
    Box(
      modifier = Modifier
        .matchParentSize()
        .graphicsLayer { translationY = animatedTranslationY },
    ) {
      content()
    }

    // LAYER 2: Floating indicator (centered in the gap above content)
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .graphicsLayer {
          translationY = (animatedTranslationY / 2f) - (indicatorSizePx / 2f)
          scaleX = indicatorScale
          scaleY = indicatorScale
          alpha = indicatorScale
        }
        .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
        .size(indicatorSize)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isRefreshing.value) {
        // Indeterminate
        LoadingIndicator(
          polygons = expressivePolygons,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        // Determinate
        LoadingIndicator(
          progress = { state.distanceFraction.coerceIn(0f, 1f) },
          polygons = expressivePolygons,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
