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
import androidx.compose.material.icons.filled.PlayArrow
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
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import androidx.compose.foundation.combinedClickable
import app.marlboroadvance.mpvex.utils.media.MediaFormatter
import org.koin.compose.koinInject
import java.util.Locale

import app.marlboroadvance.mpvex.preferences.UiSettings

@Composable
fun NetworkVideoCard(
  file: NetworkFile,
  connection: NetworkConnection,
  uiSettings: UiSettings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
) {
  val maxLines = if (uiSettings.unlimitedNameLines) Int.MAX_VALUE else 2

  BaseMediaCard(
    title = file.name,
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    isSelected = isSelected,
    maxTitleLines = maxLines,
    chipsContent = {
      if (uiSettings.showSizeChip && file.size > 0) {
        MediaMetadataChip(text = MediaFormatter.formatFileSize(file.size))
      }
      if (file.lastModified > 0) {
        MediaMetadataChip(text = MediaFormatter.formatDate(file.lastModified))
      }
    }
  )
}
