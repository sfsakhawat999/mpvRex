package xyz.mpv.rex.ui.player.controls.components.sheets

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.presentation.components.PlayerSheet
import xyz.mpv.rex.presentation.components.RepeatingIconButton
import xyz.mpv.rex.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun SleepTimerSheet(
  remainingTime: Int,
  onStartTimer: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val presets = remember { listOf(15, 30, 45, 60, 90, 120) }
  val isRunning = remainingTime > 0

  // The active time in minutes
  val currentTimeMinutes = if (isRunning) {
    (remainingTime / 60f).roundToInt()
  } else {
    30
  }

  var targetMinutes by remember(currentTimeMinutes) { mutableIntStateOf(currentTimeMinutes) }

  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier = modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

      // Title and Value display (centered Column matching PlaybackSpeedSheet)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (isRunning) {
            stringResource(R.string.timer_title) + " (Remaining)"
          } else {
            stringResource(R.string.timer_title)
          },
          style = MaterialTheme.typography.bodyMedium
        )
        Text(
          text = if (isRunning) {
            DateUtils.formatElapsedTime(remainingTime.toLong())
          } else {
            "${targetMinutes}m"
          },
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
      }

      // Slider and +/- Stepper Buttons (matching PlaybackSpeedSheet layout)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        RepeatingIconButton(
          onClick = {
            val newMin = (targetMinutes - 5).coerceAtLeast(5)
            if (isRunning) {
              onStartTimer(newMin * 60)
            } else {
              targetMinutes = newMin
            }
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.Remove, null, modifier = Modifier.size(24.dp))
        }

        Slider(
          value = targetMinutes.toFloat(),
          onValueChange = {
            val mins = it.roundToInt()
            if (isRunning) {
              onStartTimer(mins * 60)
            } else {
              targetMinutes = mins
            }
          },
          valueRange = 5f..180f,
          modifier = Modifier.weight(1f)
        )

        RepeatingIconButton(
          onClick = {
            val newMax = (targetMinutes + 5).coerceAtMost(180)
            if (isRunning) {
              onStartTimer(newMax * 60)
            } else {
              targetMinutes = newMax
            }
          },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
        }
      }

      // Quick presets Chips Row (matching PlaybackSpeedSheet FilterChip row)
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium),
        contentPadding = PaddingValues(end = MaterialTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        items(presets) { preset ->
          FilterChip(
            selected = targetMinutes == preset,
            onClick = {
              if (isRunning) {
                onStartTimer(preset * 60)
              } else {
                targetMinutes = preset
              }
            },
            label = { Text("${preset}m") }
          )
        }
      }

      // Bottom Row of buttons (matching PlaybackSpeedSheet horizontal buttons layout)
      Row(
        modifier = Modifier
          .padding(horizontal = MaterialTheme.spacing.medium)
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        if (isRunning) {
          // Running State: [OK / Close] | [Cancel Timer]
          Button(
            modifier = Modifier.weight(1f),
            onClick = onDismissRequest,
          ) {
            Text(text = stringResource(id = R.string.generic_ok))
          }
          Button(
            onClick = {
              onStartTimer(0)
              onDismissRequest()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            )
          ) {
            Text(text = stringResource(id = R.string.generic_reset))
          }
        } else {
          // Setup State: [Start Timer] | [Cancel]
          Button(
            modifier = Modifier.weight(1f),
            onClick = {
              onStartTimer(targetMinutes * 60)
              onDismissRequest()
            },
          ) {
            Text(text = stringResource(R.string.timer_start))
          }
          Button(
            onClick = onDismissRequest,
          ) {
            Text(text = stringResource(id = R.string.generic_cancel))
          }
        }
      }
    }
  }
}
