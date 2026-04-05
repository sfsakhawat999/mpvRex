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
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject

/**
 * Card for displaying M3U/M3U8 playlist items (streaming URLs)
 * Shows simple layout without thumbnail since no metadata is available
 */
import app.marlboroadvance.mpvex.preferences.UiSettings

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
  val maxLines = if (uiSettings.unlimitedNameLines) Int.MAX_VALUE else 2

  BaseMediaCard(
    title = title,
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    isSelected = isSelected,
    maxTitleLines = maxLines,
    thumbnailIcon = {
      Icon(
        Icons.Filled.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.secondary,
      )
    },
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


