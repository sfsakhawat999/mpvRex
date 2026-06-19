package xyz.mpv.rex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject
import androidx.compose.runtime.*
import xyz.mpv.rex.preferences.UiSettings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.domain.thumbnail.ThumbnailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Card for displaying M3U/M3U8 playlist items (streaming URLs)
 * Shows simple layout without thumbnail since no metadata is available
 */

@Composable
fun M3UVideoCard(
  title: String,
  url: String,
  uiSettings: UiSettings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  isRecentlyPlayed: Boolean = false,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  var thumbnail by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }

  if (uiSettings.showNetworkThumbnails) {
    val density = LocalDensity.current
    val targetThumbnailSize = 128.dp
    val thumbWidthPx = with(density) { targetThumbnailSize.toPx().roundToInt() }
    val thumbHeightPx = (thumbWidthPx / (16f / 9f)).roundToInt()

    val dummyVideo = remember(url) {
      Video(
        id = url.hashCode().toLong(),
        title = title,
        displayName = title,
        path = url,
        uri = android.net.Uri.parse(url),
        duration = 0,
        durationFormatted = "",
        size = 0,
        sizeFormatted = "",
        dateModified = 0,
        dateAdded = 0,
        mimeType = "video/*",
        bucketId = "",
        bucketDisplayName = "",
        width = 0,
        height = 0,
        fps = 0f,
        resolution = ""
      )
    }

    val thumbnailKey = remember(dummyVideo.id, thumbWidthPx, thumbHeightPx) {
      thumbnailRepository.thumbnailKey(dummyVideo, thumbWidthPx, thumbHeightPx)
    }

    LaunchedEffect(thumbnailKey) {
      thumbnailRepository.thumbnailReadyKeys.filter { it == thumbnailKey }.collect {
        thumbnail = thumbnailRepository.getThumbnailFromMemory(dummyVideo, thumbWidthPx, thumbHeightPx)
      }
    }

    LaunchedEffect(thumbnailKey) {
      if (thumbnail == null) {
        thumbnail = withContext(Dispatchers.IO) {
          thumbnailRepository.getThumbnail(dummyVideo, thumbWidthPx, thumbHeightPx)
        }
      }
    }
  }

  val maxLines = if (uiSettings.unlimitedNameLines) Int.MAX_VALUE else 2

  BaseMediaCard(
    title = title,
    modifier = modifier,
    thumbnailSize = 128.dp,
    thumbnail = thumbnail?.asImageBitmap(),
    thumbnailAspectRatio = 16f / 9f,
    onClick = onClick,
    onLongClick = onLongClick,
    isSelected = isSelected,
    maxTitleLines = maxLines,
    thumbnailIcon = if (thumbnail == null) {
      {
        Icon(
          Icons.Filled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }
    } else null,
    infoContent = {
      Text(
        url,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  )
}


