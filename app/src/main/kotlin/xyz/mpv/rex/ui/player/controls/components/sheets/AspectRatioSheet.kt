package xyz.mpv.rex.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.R
import xyz.mpv.rex.presentation.components.PlayerSheet
import xyz.mpv.rex.ui.theme.spacing

data class AspectRatio(
  val label: String,
  val ratio: Double,
  val isCustom: Boolean = false,
)

@Composable
fun AspectRatioSheet(
  currentRatio: Double?,
  customRatios: List<AspectRatio>,
  videoZoom: Float,
  videoPanX: Float,
  onZoomChange: (Float) -> Unit,
  onPanXChange: (Float) -> Unit,
  onSelectRatio: (Double) -> Unit,
  onAddCustomRatio: (String, Double) -> Unit,
  onDeleteCustomRatio: (AspectRatio) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCustomInput by remember { mutableStateOf(false) }

  val presetRatios =
    listOf(
      AspectRatio(stringResource(R.string.player_sheets_aspect_ratio_preset_default), -1.0),
      AspectRatio("4:3", 4.0 / 3.0),
      AspectRatio("16:9", 16.0 / 9.0),
      AspectRatio("16:10", 16.0 / 10.0),
      AspectRatio("21:9", 21.0 / 9.0),
      AspectRatio("32:9", 32.0 / 9.0),
      AspectRatio("2.35:1", 2.35),
      AspectRatio("2.39:1", 2.39),
    )

  PlayerSheet(onDismissRequest) {
    Column(
      modifier =
        modifier
          .verticalScroll(rememberScrollState())
          .padding(vertical = MaterialTheme.spacing.medium),
    ) {
      Text(
        text = stringResource(R.string.player_sheets_aspect_ratio_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier =
          Modifier
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(bottom = MaterialTheme.spacing.small),
      )

      // Preset ratios
      Text(
        text = stringResource(R.string.player_sheets_aspect_ratio_presets),
        style = MaterialTheme.typography.titleSmall,
        modifier =
          Modifier
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(top = MaterialTheme.spacing.small),
      )

      LazyRow(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
      ) {
        items(presetRatios, key = { it.label }) { ratio ->
          InputChip(
            selected = currentRatio?.let { abs(it - ratio.ratio) < 0.01 } ?: (ratio.ratio == -1.0),
            onClick = { onSelectRatio(ratio.ratio) },
            label = { Text(ratio.label) },
            modifier = Modifier.animateItem(),
            leadingIcon = null,
          )
        }
        item {
          InputChip(
            selected = showCustomInput,
            onClick = { showCustomInput = !showCustomInput },
            label = { Text(stringResource(R.string.player_sheets_aspect_ratio_custom_toggle)) },
            modifier = Modifier.animateItem(),
            leadingIcon = null,
          )
        }
      }

      // Custom ratios
      if (customRatios.isNotEmpty()) {
        Text(
          text = stringResource(R.string.player_sheets_aspect_ratio_custom_section),
          style = MaterialTheme.typography.titleSmall,
          modifier =
            Modifier
              .padding(horizontal = MaterialTheme.spacing.medium)
              .padding(top = MaterialTheme.spacing.medium),
        )

        LazyRow(
          modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
          items(customRatios, key = { it.label }) { ratio ->
            InputChip(
              selected = currentRatio?.let { abs(it - ratio.ratio) < 0.01 } ?: false,
              onClick = { onSelectRatio(ratio.ratio) },
              label = { Text(ratio.label) },
              leadingIcon = null,
              trailingIcon = {
                Icon(
                  Icons.Default.Close,
                  null,
                  modifier = Modifier.clickable { onDeleteCustomRatio(ratio) },
                )
              },
              modifier = Modifier.animateItem(),
            )
          }
        }
      }

      // Add custom ratio (hidden by default, toggled via Custom... chip)
      if (showCustomInput) {
        AddCustomRatioRow(
          onAdd = { label, ratio ->
            onAddCustomRatio(label, ratio)
            showCustomInput = false
          },
          modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
        )
      }

      HorizontalDivider(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
      )

      Text(
        text = stringResource(R.string.player_sheets_aspect_ratio_manual_zoom_pan_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
          .padding(horizontal = MaterialTheme.spacing.medium)
          .padding(bottom = MaterialTheme.spacing.small),
      )

      // Zoom Slider
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          val zoomFactor = java.lang.Math.pow(2.0, videoZoom.toDouble())
          Text(
            text = stringResource(R.string.player_sheets_aspect_ratio_zoom_value, zoomFactor),
            style = MaterialTheme.typography.bodyMedium
          )
          
          if (videoZoom != 0f) {
            IconButton(
              onClick = { onZoomChange(0f) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.player_sheets_aspect_ratio_reset_zoom),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Slider(
          value = videoZoom,
          onValueChange = onZoomChange,
          valueRange = -1f..3f,
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

      // Horizontal Pan Slider
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text =
              stringResource(
                R.string.player_sheets_aspect_ratio_horizontal_offset_value,
                videoPanX,
              ),
            style = MaterialTheme.typography.bodyMedium
          )
          
          if (videoPanX != 0f) {
            IconButton(
              onClick = { onPanXChange(0f) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.player_sheets_aspect_ratio_reset_pan),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Slider(
          value = videoPanX,
          onValueChange = onPanXChange,
          valueRange = -1f..1f,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun AddCustomRatioRow(
  onAdd: (String, Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  var widthText by remember { mutableStateOf("") }
  var heightText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.medium),
  ) {
    Text(
      text = stringResource(R.string.player_sheets_aspect_ratio_add_custom_title),
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(bottom = MaterialTheme.spacing.small),
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      modifier = Modifier.fillMaxWidth(),
    ) {
      // Width input
      OutlinedTextField(
        value = widthText,
        onValueChange = {
          widthText = it.filter { char -> char.isDigit() || char == '.' }
          errorMessage = null
        },
        label = { Text(stringResource(R.string.player_sheets_aspect_ratio_width)) },
        isError = errorMessage != null,
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
          ),
        modifier = Modifier.weight(1f),
        singleLine = true,
      )

      // Colon separator
      Text(
        text = ":",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraSmall),
      )

      // Height input
      OutlinedTextField(
        value = heightText,
        onValueChange = {
          heightText = it.filter { char -> char.isDigit() || char == '.' }
          errorMessage = null
        },
        label = { Text(stringResource(R.string.player_sheets_aspect_ratio_height)) },
        isError = errorMessage != null,
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
          ),
        keyboardActions =
          KeyboardActions(
            onDone = {
              val result = calculateRatio(widthText, heightText)
              if (result != null) {
                onAdd(
                  context.getString(
                    R.string.player_sheets_aspect_ratio_custom_ratio_label,
                    widthText,
                    heightText,
                  ),
                  result,
                )
                widthText = ""
                heightText = ""
                keyboardController?.hide()
              } else {
                errorMessage = context.getString(R.string.player_sheets_aspect_ratio_invalid)
              }
            },
          ),
        modifier = Modifier.weight(1f),
        singleLine = true,
      )

      // Add button
      FilledTonalIconButton(
        onClick = {
          val result = calculateRatio(widthText, heightText)
          if (result != null) {
            onAdd(
              context.getString(
                R.string.player_sheets_aspect_ratio_custom_ratio_label,
                widthText,
                heightText,
              ),
              result,
            )
            widthText = ""
            heightText = ""
            keyboardController?.hide()
          } else {
            errorMessage = context.getString(R.string.player_sheets_aspect_ratio_invalid)
          }
        },
      ) {
        Icon(
          Icons.Default.Add,
          contentDescription = stringResource(R.string.player_sheets_aspect_ratio_add),
        )
      }
    }

    errorMessage?.let { msg ->
      Text(
        text = msg,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = MaterialTheme.spacing.small, top = 4.dp),
      )
    }
  }
}

private fun calculateRatio(
  widthStr: String,
  heightStr: String,
): Double? {
  if (widthStr.isEmpty() || heightStr.isEmpty()) return null

  return try {
    val width = widthStr.toDouble()
    val height = heightStr.toDouble()
    if (width > 0 && height > 0) width / height else null
  } catch (_: NumberFormatException) {
    null
  }
}

private fun abs(value: Double): Double = if (value < 0) -value else value
