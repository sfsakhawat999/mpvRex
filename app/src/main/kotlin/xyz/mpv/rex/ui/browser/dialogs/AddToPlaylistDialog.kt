package xyz.mpv.rex.ui.browser.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.database.entities.PlaylistEntity
import xyz.mpv.rex.database.repository.PlaylistRepository
import xyz.mpv.rex.domain.media.model.Video
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddToPlaylistDialog(
  isOpen: Boolean,
  videos: List<Video>,
  onDismiss: () -> Unit,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = koinInject<PlaylistRepository>()
  val playlistsFromDb by repository.observeAllPlaylists().collectAsState(initial = emptyList())
  val playlists = remember(playlistsFromDb) {
    playlistsFromDb.sortedBy { it.name.lowercase() }
  }
  val scope = rememberCoroutineScope()
  var showCreateDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  if (!isOpen) return

  if (showCreateDialog) {
    CreatePlaylistDialog(
      onDismiss = { showCreateDialog = false },
      onConfirm = { name ->
        scope.launch {
          val playlistId = repository.createPlaylist(name)
          val items = videos.map { video ->
            video.path to video.displayName
          }
          repository.addItemsToPlaylist(playlistId.toInt(), items)
          val message = if (videos.size == 1) {
            context.getString(R.string.video_added_to, name)
          } else {
            context.getString(R.string.n_videos_added_to, videos.size, name)
          }
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
          showCreateDialog = false
          onSuccess()
          onDismiss()
        }
      }
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.add_to_playlist),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Show video count
        Text(
          text = if (videos.size == 1) {
            stringResource(R.string.adding_one_video_to_playlist)
          } else {
            stringResource(R.string.adding_n_videos_to_playlist, videos.size)
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Create new playlist button
        OutlinedButton(
          onClick = { showCreateDialog = true },
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.extraLarge,
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.create_new_playlist),
            fontWeight = FontWeight.Medium,
          )
        }

        // Existing playlists
        if (playlists.isNotEmpty()) {
          Text(
            text = stringResource(R.string.existing_playlists),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )

          LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
          ) {
            items(playlists, key = { it.id }) { playlist ->
              PlaylistItemCard(
                playlist = playlist,
                repository = repository,
                onClick = {
                  scope.launch {
                    val items = videos.map { video ->
                      video.path to video.displayName
                    }
                    repository.addItemsToPlaylist(playlist.id, items)
                    val message = if (videos.size == 1) {
                      context.getString(R.string.video_added_to, playlist.name)
                    } else {
                      context.getString(R.string.n_videos_added_to, videos.size, playlist.name)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                  }
                },
              )
            }
          }
        } else {
          // Empty state
          EmptyPlaylistsMessage()
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSuccess()
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
        ),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(stringResource(R.string.generic_cancel), fontWeight = FontWeight.Medium)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier,
  )
}

@Composable
private fun PlaylistItemCard(
  playlist: PlaylistEntity,
  repository: PlaylistRepository,
  onClick: () -> Unit,
) {
  val itemCount by repository.observePlaylistItemCount(playlist.id).collectAsState(initial = 0)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = playlist.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.playlist_videos_count, itemCount, formatDate(playlist.updatedAt)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EmptyPlaylistsMessage() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.no_playlists_yet),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.create_first_playlist_above),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var playlistName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.create_new_playlist),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      OutlinedTextField(
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text(stringResource(R.string.playlist_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          if (playlistName.isNotBlank()) {
            onConfirm(playlistName.trim())
          }
        },
        enabled = playlistName.isNotBlank(),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(stringResource(R.string.create), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text(stringResource(R.string.generic_cancel), fontWeight = FontWeight.Medium)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}

private fun formatDate(timestamp: Long): String {
  val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
  return sdf.format(Date(timestamp))
}
