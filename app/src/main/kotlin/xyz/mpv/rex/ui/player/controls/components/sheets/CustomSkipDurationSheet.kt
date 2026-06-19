package xyz.mpv.rex.ui.player.controls.components.sheets

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun CustomSkipDurationSheet(
  duration: Int,
  onDurationChange: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val presets = remember { listOf(5, 10, 30, 60, 90, 120, 180) }

  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier
        .verticalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Label and Value
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = stringResource(R.string.pref_player_custom_skip_duration_title),
          style = MaterialTheme.typography.bodyMedium
        )
        Text(
          text = "${duration}s",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )
      }

      // Slider and +/- Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        RepeatingIconButton(
          onClick = { onDurationChange((duration - 5).coerceAtLeast(5)) },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.Remove, null, modifier = Modifier.size(24.dp))
        }

        Slider(
          value = duration.toFloat(),
          onValueChange = {
            onDurationChange(it.roundToInt())
          },
          valueRange = 5f..180f,
          modifier = Modifier.weight(1f)
        )

        RepeatingIconButton(
          onClick = { onDurationChange((duration + 5).coerceAtMost(180)) },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
        }
      }

      // Presets
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        items(presets) { preset ->
          FilterChip(
            selected = duration == preset,
            onClick = { onDurationChange(preset) },
            label = { Text("${preset}s") }
          )
        }
      }
    }
  }
}
