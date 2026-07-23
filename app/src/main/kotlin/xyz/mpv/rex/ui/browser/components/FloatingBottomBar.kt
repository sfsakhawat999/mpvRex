package xyz.mpv.rex.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Material 3 Floating Button Bar for file/folder operations
 * Icon-only buttons in a floating pill-shaped surface
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowserBottomBar(
  isSelectionMode: Boolean,
  onCopyClick: () -> Unit,
  onMoveClick: () -> Unit,
  onRenameClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onAddToPlaylistClick: () -> Unit,
  modifier: Modifier = Modifier,
  showCopy: Boolean = true,
  showMove: Boolean = true,
  showRename: Boolean = true,
  showDelete: Boolean = true,
  showAddToPlaylist: Boolean = true,
  onMarkAsClick: (() -> Unit)? = null,
) {
  AnimatedVisibility(
    visible = isSelectionMode,
    modifier = modifier,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Surface(
      modifier = Modifier
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 20.dp, vertical = 8.dp),
      shape = RoundedCornerShape(32.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 3.dp,
      shadowElevation = 8.dp
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        FilledTonalIconButton(
          onClick = onCopyClick,
          enabled = showCopy,
          modifier = Modifier.size(42.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Icon(
            Icons.Filled.ContentCopy,
            contentDescription = stringResource(R.string.copy),
            modifier = Modifier.size(20.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onMoveClick,
          enabled = showMove,
          modifier = Modifier.size(42.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Icon(
            Icons.AutoMirrored.Filled.DriveFileMove,
            contentDescription = stringResource(R.string.move),
            modifier = Modifier.size(20.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onRenameClick,
          enabled = showRename,
          modifier = Modifier.size(42.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Icon(
            Icons.Filled.DriveFileRenameOutline,
            contentDescription = stringResource(R.string.rename),
            modifier = Modifier.size(20.dp)
          )
        }

        FilledTonalIconButton(
          onClick = onAddToPlaylistClick,
          enabled = showAddToPlaylist,
          modifier = Modifier.size(42.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors()
        ) {
          Icon(
            Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(R.string.add_to_playlist),
            modifier = Modifier.size(20.dp)
          )
        }

        if (onMarkAsClick != null) {
          FilledTonalIconButton(
            onClick = onMarkAsClick,
            modifier = Modifier.size(42.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
          ) {
            Icon(
              Icons.Filled.Bookmarks,
              contentDescription = stringResource(R.string.mark_as),
              modifier = Modifier.size(20.dp)
            )
          }
        }

        FilledTonalIconButton(
          onClick = onDeleteClick,
          enabled = showDelete,
          modifier = Modifier.size(42.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          )
        ) {
          Icon(
            Icons.Filled.Delete,
            contentDescription = stringResource(R.string.delete),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
