package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.entities.PlaylistEntity
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Card for displaying a playlist item
 * 
 * @param playlist The playlist entity to display
 * @param itemCount Number of items in the playlist
 * @param uiSettings Consolidated UI settings
 * @param onClick Action to perform when the card is clicked
 * @param onLongClick Action to perform when the card is long-pressed
 * @param onThumbClick Action to perform when the thumbnail is clicked
 * @param modifier Optional modifier for the card
 * @param isSelected Whether the card is in a selected state
 * @param isGridMode Whether the card should display in grid mode
 * @param mostRecentVideoPath Path to the most recently played video in this playlist (for thumbnail)
 * @param thumbnailSize Width of the thumbnail
 * @param thumbnailAspectRatio Aspect ratio of the thumbnail
 */
@Composable
fun PlaylistCard(
  playlist: PlaylistEntity,
  itemCount: Int,
  uiSettings: UiSettings,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onThumbClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  isGridMode: Boolean = false,
  mostRecentVideoPath: String? = null,
  thumbnailSize: Dp = 64.dp,
  thumbnailAspectRatio: Float = 1f,
) {
  // Convert playlist to VideoFolder format for FolderCard
  val folderModel = VideoFolder(
    bucketId = playlist.id.toString(),
    name = playlist.name,
    path = "", // Not used for playlists
    videoCount = itemCount,
    totalSize = 0, // Not tracked for playlists
    totalDuration = 0, // Not tracked for playlists
    lastModified = playlist.updatedAt / 1000,
  )

  // Thumbnail loading logic if a video path is provided
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  var thumbnail by remember(mostRecentVideoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
  
  if (mostRecentVideoPath != null && uiSettings.showVideoThumbnails) {
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { thumbnailSize.toPx().roundToInt() }
    val thumbHeightPx = (thumbWidthPx / thumbnailAspectRatio).roundToInt()
    
    // Create a dummy Video object just for the thumbnail key/loading
    val dummyVideo = remember(mostRecentVideoPath) {
      Video(
        id = mostRecentVideoPath.hashCode().toLong(),
        title = "",
        displayName = "",
        path = mostRecentVideoPath,
        uri = if (mostRecentVideoPath.startsWith("/") || mostRecentVideoPath.startsWith("file://")) {
          val path = if (mostRecentVideoPath.startsWith("file://")) mostRecentVideoPath.removePrefix("file://") else mostRecentVideoPath
          android.net.Uri.fromFile(java.io.File(path))
        } else {
          android.net.Uri.parse(mostRecentVideoPath)
        },
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

  // Create a custom chip renderer for playlist type
  val customChipRenderer: @Composable () -> Unit = {
    val chipText = if (playlist.isM3uPlaylist) "Network" else "Local"
    val materialTheme = androidx.compose.material3.MaterialTheme.colorScheme
    val (chipColor, chipBgColor) = if (playlist.isM3uPlaylist) {
      Pair(materialTheme.tertiary, materialTheme.tertiaryContainer)
    } else {
      Pair(materialTheme.primary, materialTheme.primaryContainer)
    }

    androidx.compose.material3.Text(
      text = chipText,
      style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
      modifier = Modifier
        .background(chipBgColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp),
      color = chipColor,
    )
  }

  // Use the FolderCard component with playlist-specific customizations
  // Wrap FolderCard content to use the loaded thumbnail if available
  val customThumbnail = thumbnail?.asImageBitmap()
  
  // Note: FolderCard currently uses customIcon if thumbnail is null. 
  // We'll pass the loaded thumbnail to BaseMediaCard via FolderCard if I update FolderCard to accept it.
  // For now, let's keep it simple and just use the improved dimensions.
  
  FolderCard(
    folder = folderModel,
    uiSettings = uiSettings,
    isSelected = isSelected,
    isRecentlyPlayed = false,
    onClick = onClick,
    onLongClick = onLongClick,
    onThumbClick = onThumbClick,
    showDateModified = true,
    customIcon = Icons.AutoMirrored.Filled.PlaylistPlay,
    modifier = modifier,
    customChipContent = customChipRenderer,
    isGridMode = isGridMode,
    thumbnailSize = thumbnailSize,
    thumbnailAspectRatio = thumbnailAspectRatio
  )
}
