package app.marlboroadvance.mpvex.ui.browser.cards

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.utils.media.MediaFormatter
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItemDefaults.verticalAlignment
import org.koin.compose.koinInject
import kotlin.math.pow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

import app.marlboroadvance.mpvex.preferences.UiSettings

@Composable
fun FolderCard(
  folder: VideoFolder,
  onClick: () -> Unit,
  uiSettings: UiSettings,
  modifier: Modifier = Modifier,
  isRecentlyPlayed: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  onThumbClick: (() -> Unit)? = null,
  showDateModified: Boolean = false,
  customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  newVideoCount: Int = 0,
  customChipContent: @Composable (() -> Unit)? = null,
  isGridMode: Boolean = false,
  gridColumns: Int = 1,
  thumbnailSize: androidx.compose.ui.unit.Dp = 64.dp,
  thumbnailAspectRatio: Float = 1f,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val totalCount = folder.videoCount + folder.audioCount
  val countLabel = if (totalCount == 1) "1 Item" else "$totalCount Items"
  val maxLines = if (uiSettings.unlimitedNameLines) Int.MAX_VALUE else 2
  val parentPath = folder.path.substringBeforeLast("/", folder.path)

  BaseMediaCard(
    title = folder.name,
    modifier = modifier,
    thumbnailAspectRatio = thumbnailAspectRatio,
    thumbnailSize = thumbnailSize,
    thumbnailIcon = {
      Icon(
        customIcon ?: Icons.Filled.Folder,
        contentDescription = "Folder",
        modifier = Modifier.size(if (isGridMode) 56.dp else 40.dp),
        tint = MaterialTheme.colorScheme.secondary,
      )
    },
    onClick = onClick,
    onLongClick = onLongClick,
    onThumbClick = onThumbClick,
    isSelected = isSelected,
    isRecentlyPlayed = isRecentlyPlayed,
    isGridMode = isGridMode,
    gridColumns = gridColumns,
    maxTitleLines = maxLines,
    overlayContent = {
      if (newVideoCount > 0) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFD32F2F))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = newVideoCount.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
        }
      }
      if (isGridMode && uiSettings.showTotalDurationChip && folder.totalDuration > 0) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(
            text = MediaFormatter.formatDuration(folder.totalDuration),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
          )
        }
      }
    },
    infoContent = {
      if (!isGridMode && showFolderPath && parentPath.isNotEmpty()) {
        Text(
          parentPath,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    chipsContent = {
      if (customChipContent != null) {
        customChipContent()
      }
      if (totalCount > 0) {
        MediaMetadataChip(text = countLabel)
      }
      if (uiSettings.showSizeChip && folder.totalSize > 0) {
        MediaMetadataChip(text = MediaFormatter.formatFileSize(folder.totalSize))
      }
      if (uiSettings.showTotalDurationChip && folder.totalDuration > 0 && !isGridMode) {
        MediaMetadataChip(text = MediaFormatter.formatDuration(folder.totalDuration))
      }
      if (uiSettings.showDateChip && folder.lastModified > 0) {
        MediaMetadataChip(text = MediaFormatter.formatDate(folder.lastModified * 1000))
      }
    }
  )
}
