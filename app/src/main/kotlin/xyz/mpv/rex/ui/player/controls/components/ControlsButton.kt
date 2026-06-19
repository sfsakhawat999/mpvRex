package xyz.mpv.rex.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.ui.player.controls.LocalPlayerButtonsClickEvent
import xyz.mpv.rex.ui.theme.controlColor
import xyz.mpv.rex.ui.theme.spacing
import org.koin.compose.koinInject

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  title: String? = null,
  color: Color? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val appearancePreferences = koinInject<AppearancePreferences>()
  val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()

  val clickEvent = LocalPlayerButtonsClickEvent.current
  val matchTheme by appearancePreferences.matchPlayerControlsToTheme.collectAsState()

  val buttonColor = when {
    color != null -> color
    matchTheme -> MaterialTheme.colorScheme.primary
    else -> if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
  }

  val surfaceColor = when {
    hideBackground -> Color.Transparent
    matchTheme -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
  }

  val contentColor = when {
    color != null -> color
    matchTheme -> {
      if (hideBackground) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.onPrimaryContainer
    }
    else -> if (hideBackground) controlColor else MaterialTheme.colorScheme.onSurface
  }

  Surface(
    modifier =
      modifier
        .clip(CircleShape)
        .combinedClickable(
          onClick = {
            clickEvent()
            onClick()
          },
          onLongClick = onLongClick,
          interactionSource = interactionSource,
          indication = ripple(),
        ),
    shape = CircleShape,
    color = surfaceColor,
    contentColor = contentColor,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border =
      if (hideBackground) {
        null
      } else {
        BorderStroke(
          1.dp,
          if (matchTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
          else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
      },
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = contentColor,
      modifier =
        Modifier
          .padding(MaterialTheme.spacing.smaller)
          .size(24.dp),
    )
  }
}

@Composable
fun ControlsGroup(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit,
) {
  val spacing = MaterialTheme.spacing

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      androidx.compose.foundation.layout.Arrangement
        .spacedBy(spacing.extraSmall),
    content = content,
  )
}

@Preview
@Composable
private fun PreviewControlsButton() {
  ControlsButton(
    Icons.Default.CatchingPokemon,
    onClick = {},
  )
}
