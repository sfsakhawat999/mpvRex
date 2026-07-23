package xyz.mpv.rex.ui.browser.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import xyz.mpv.rex.R
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import xyz.mpv.rex.utils.history.RecentlyPlayedOps
import xyz.mpv.rex.utils.media.MediaUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayLinkSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onPlayLink: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  var linkInputUrl by remember { mutableStateOf("") }
  var isLinkInputUrlValid by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(true) {
    if (isOpen) {
      linkInputUrl = ""
      isLinkInputUrlValid = true
    }
  }

  val handleDismiss = {
    onDismiss()
  }

  val handleConfirm = {
    val url = linkInputUrl.trim()
    if (url.isNotBlank() && MediaUtils.isURLValid(url)) {
      // Optimistically record in history so it shows up immediately
      coroutineScope.launch {
        val uri = url.toUri()
        val name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { url } ?: url
        RecentlyPlayedOps.addRecentlyPlayed(
          filePath = url,
          fileName = name,
          launchSource = "play_link",
          isAudio = false, // Assume video for links unless we probe
        )
      }
      onPlayLink(url)
      onDismiss()
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

  ModalBottomSheet(
    onDismissRequest = handleDismiss,
    sheetState = sheetState,
    dragHandle = {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(width = 32.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraLarge
                )
        )
    },
    modifier = modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Title
      Text(
        text = stringResource(R.string.play_link),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // URL Input
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = linkInputUrl,
          onValueChange = { newValue ->
            linkInputUrl = newValue
            isLinkInputUrlValid = newValue.isBlank() || MediaUtils.isURLValid(newValue)
          },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.enter_url)) },
          placeholder = { Text("https://example.com/video.mp4") },
          singleLine = true,
          isError = linkInputUrl.isNotBlank() && !isLinkInputUrlValid,
          trailingIcon = {
            if (linkInputUrl.isNotBlank()) {
              ValidationIcon(isValid = isLinkInputUrlValid)
            }
          },
        )

        if (linkInputUrl.isNotBlank() && !isLinkInputUrlValid) {
          Text(
            text = stringResource(R.string.invalid_url_protocol),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      // Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = handleDismiss) {
          Text(
            text = stringResource(R.string.generic_cancel),
            fontWeight = FontWeight.Medium,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = handleConfirm,
          enabled = linkInputUrl.isNotBlank() && isLinkInputUrlValid,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
          Text(
            text = stringResource(R.string.play),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun ValidationIcon(isValid: Boolean) {
  if (isValid) {
    Icon(
      Icons.Filled.CheckCircle,
      contentDescription = stringResource(R.string.valid_url),
      tint = MaterialTheme.colorScheme.primary,
    )
  } else {
    Icon(
      Icons.Filled.Info,
      contentDescription = stringResource(R.string.invalid_url),
      tint = MaterialTheme.colorScheme.error,
    )
  }
}
