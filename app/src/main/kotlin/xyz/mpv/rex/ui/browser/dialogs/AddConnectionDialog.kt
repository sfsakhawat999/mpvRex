package xyz.mpv.rex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.domain.network.NetworkConnection
import xyz.mpv.rex.domain.network.NetworkProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectionSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onSave: (NetworkConnection) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  var name by remember { mutableStateOf("") }
  var protocol by remember { mutableStateOf(NetworkProtocol.SMB) }
  var host by remember { mutableStateOf("") }
  var port by remember { mutableStateOf(protocol.defaultPort.toString()) }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var path by remember { mutableStateOf("/") }
  var isAnonymous by remember { mutableStateOf(false) }
  var useHttps by remember { mutableStateOf(false) }
  var passwordVisible by remember { mutableStateOf(false) }
  var protocolMenuExpanded by remember { mutableStateOf(false) }

  val handleDismiss = {
    onDismiss()
  }

  val handleSave = {
    val connection =
      NetworkConnection(
        name = name.ifBlank { "${protocol.displayName} - $host" },
        protocol = protocol,
        host = host,
        port = port.toIntOrNull() ?: protocol.defaultPort,
        username = if (isAnonymous) "" else username,
        password = if (isAnonymous) "" else password,
        path = path.ifBlank { "/" },
        isAnonymous = isAnonymous,
        useHttps = useHttps,
      )
    onSave(connection)
  }

  AlertDialog(
    onDismissRequest = handleDismiss,
    modifier = Modifier.widthIn(min = 400.dp, max = 600.dp),
    title = {
      Text(
        text = stringResource(R.string.add_network_connection),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {

      // Name and Protocol in one row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Connection Name
              OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(0.50f),
                singleLine = true,
              )

              // Protocol Dropdown
              ExposedDropdownMenuBox(
                expanded = protocolMenuExpanded,
                onExpandedChange = { protocolMenuExpanded = it },
                modifier = Modifier.weight(0.50f),
              ) {
                OutlinedTextField(
                  value = protocol.displayName,
                  onValueChange = { },
                  readOnly = true,
                  label = { Text(stringResource(R.string.protocol), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolMenuExpanded) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                  expanded = protocolMenuExpanded,
                  onDismissRequest = { protocolMenuExpanded = false },
                ) {
                  NetworkProtocol.entries.forEach { proto ->
                    DropdownMenuItem(
                      text = { Text(proto.displayName) },
                      onClick = {
                        protocol = proto
                        port = proto.defaultPort.toString()
                        protocolMenuExpanded = false
                      },
                    )
                  }
          }
        }
      }

      // Host
      OutlinedTextField(
        value = host,
        onValueChange = { host = it },
        label = { Text(stringResource(R.string.host_ip_address), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("192.168.1.100", maxLines = 1, overflow = TextOverflow.Ellipsis) },
      )

      // Port and Path in one row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Port
              OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text(stringResource(R.string.port), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(0.3f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              )

              // Path
              OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text(stringResource(R.string.path), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(0.7f),
                singleLine = true,
                placeholder = { Text("/", maxLines = 1, overflow = TextOverflow.Ellipsis) },
              )
            }

      // Anonymous and HTTPS checkboxes
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Checkbox(
          checked = isAnonymous,
          onCheckedChange = { isAnonymous = it },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.anonymous_guest_access))
      }
      
      // HTTPS checkbox (only for WebDAV)
      if (protocol == NetworkProtocol.WEBDAV) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Checkbox(
            checked = useHttps,
            onCheckedChange = { 
              useHttps = it
              // Auto-update port when toggling HTTPS
              if (it && port == "80") {
                port = "443"
              } else if (!it && port == "443") {
                port = "80"
              }
            },
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.use_https_secure_connection))
        }
      }

      // Username and Password in one row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Username
              OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(0.50f),
                singleLine = true,
                enabled = !isAnonymous,
              )

              // Password
              OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(0.50f),
                singleLine = true,
                enabled = !isAnonymous,
              )
            }

      }
    },
    confirmButton = {
      Button(
        onClick = handleSave,
        enabled = host.isNotBlank() && (isAnonymous || username.isNotBlank()),
      ) {
        Text(
          text = stringResource(R.string.save),
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = handleDismiss) {
        Text(
          text = stringResource(R.string.generic_cancel),
          fontWeight = FontWeight.Medium,
        )
      }
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}
