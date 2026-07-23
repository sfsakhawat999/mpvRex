package xyz.mpv.rex.ui.browser.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.domain.network.NetworkConnection

@Composable
fun NetworkConnectionCard(
  connection: NetworkConnection,
  onConnect: (NetworkConnection) -> Unit,
  onDisconnect: (NetworkConnection) -> Unit,
  onEdit: (NetworkConnection) -> Unit,
  onDelete: (NetworkConnection) -> Unit,
  onBrowse: (NetworkConnection) -> Unit,
  onAutoConnectChange: (NetworkConnection, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  isConnected: Boolean = false,
  isConnecting: Boolean = false,
  error: String? = null,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      // Header with name and actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = connection.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.network_connection_protocol_host, connection.protocol.displayName, connection.host, connection.port),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row {
          IconButton(onClick = { onEdit(connection) }) {
            Icon(
              Icons.Filled.Edit,
              contentDescription = stringResource(R.string.network_connection_edit),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = { onDelete(connection) }) {
            Icon(
              Icons.Filled.Delete,
              contentDescription = stringResource(R.string.network_connection_delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      // Connection details
      if (connection.path != "/") {
        Text(
          text = stringResource(R.string.network_connection_path_prefix, connection.path),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      if (connection.username.isNotEmpty() && !connection.isAnonymous) {
        Text(
          text = stringResource(R.string.network_connection_user_prefix, connection.username),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      // Error message
      if (error != null) {
        Text(
          text = stringResource(R.string.network_connection_error_prefix, error),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 8.dp),
        )
      }

      // Auto-connect checkbox
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = connection.autoConnect,
          onCheckedChange = { checked ->
            onAutoConnectChange(connection, checked)
          },
        )
        Text(
          text = stringResource(R.string.network_connection_auto_connect),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Connection button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        when {
          isConnecting -> {
            FilledTonalButton(
              onClick = { },
              enabled = false,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
                Text(
                  stringResource(R.string.network_connection_connecting),
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
              }
            }
          }

          isConnected -> {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilledTonalButton(
                onClick = { onBrowse(connection) },
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              ) {
                Icon(
                  Icons.Filled.FolderOpen,
                  contentDescription = null,
                  modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.network_connection_browse))
              }

              FilledTonalButton(
                onClick = { onDisconnect(connection) },
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              ) {
                Icon(
                  Icons.Filled.LinkOff,
                  contentDescription = null,
                  modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.network_connection_disconnect))
              }
            }
          }

          else -> {
            FilledTonalButton(
              onClick = { onConnect(connection) },
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            ) {
              Icon(
                Icons.Filled.Link,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
              )
              Text(stringResource(R.string.network_connection_connect))
            }
          }
        }
      }
    }
  }
}
