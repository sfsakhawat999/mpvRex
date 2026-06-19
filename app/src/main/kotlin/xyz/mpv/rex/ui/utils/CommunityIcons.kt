package xyz.mpv.rex.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.dp

/**
 * Custom SVG Telegram logo (paper plane) for Jetpack Compose.
 */
val TelegramIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Telegram",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black), // Black default so it paints and tints correctly
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(1.946f, 9.315f)
        curveToRelative(-0.522f, -0.174f, -0.527f, -0.456f, 0.01f, -0.67f)
        lineTo(22.17f, 0.835f)
        curveToRelative(0.956f, -0.37f, 1.61f, 0.199f, 1.254f, 1.3f)
        lineTo(19.53f, 20.22f)
        curveToRelative(-0.29f, 1.29f, -1.07f, 1.6f, -2.16f, 0.99f)
        lineToRelative(-5.8f, -4.27f)
        lineToRelative(-2.8f, 2.7f)
        curveToRelative(-0.31f, 0.31f, -0.57f, 0.57f, -1.16f, 0.57f)
        lineToRelative(0.416f, -5.9f)
        lineTo(19.93f, 4.6f)
        curveToRelative(0.467f, -0.417f, -0.1f, -0.65f, -0.72f, -0.24f)
        lineTo(4.776f, 12.44f)
        lineTo(0.946f, 9.315f)
        close()
    }.build()

/**
 * Custom SVG Discord logo for Jetpack Compose.
 */
val DiscordIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Discord",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(18.974f, 8.058f)
        curveToRelative(-1.285f, -0.593f, -2.682f, -1.03f, -4.14f, -1.285f)
        curveToRelative(-0.02f, -0.01f, -0.04f, 0f, -0.048f, 0.024f)
        curveToRelative(-0.179f, 0.319f, -0.378f, 0.739f, -0.517f, 1.07f)
        curveToRelative(-1.23f, -0.18f, -2.46f, -0.18f, -3.64f, 0.001f)
        curveToRelative(-0.139f, -0.331f, -0.343f, -0.751f, -0.522f, -1.07f)
        curveToRelative(-0.01f, -0.02f, -0.03f, -0.03f, -0.048f, -0.024f)
        curveToRelative(-1.46f, 0.255f, -2.857f, 0.692f, -4.142f, 1.286f)
        curveToRelative(-0.01f, 0f, -0.02f, 0.01f, -0.022f, 0.019f)
        curveToRelative(-2.617f, 3.911f, -3.327f, 7.724f, -2.977f, 11.492f)
        curveToRelative(0f, 0.02f, 0.01f, 0.03f, 0.019f, 0.035f)
        curveToRelative(1.727f, 1.269f, 3.4f, 2.039f, 5.045f, 2.547f)
        curveToRelative(0.02f, 0.01f, 0.04f, 0f, 0.054f, -0.017f)
        curveToRelative(0.39f, -0.533f, 0.738f, -1.1f, 1.03f, -1.696f)
        curveToRelative(0.01f, -0.02f, 0f, -0.04f, -0.026f, -0.067f)
        curveToRelative(-0.38f, -0.14f, -0.77f, -0.32f, -1.164f, -0.555f)
        curveToRelative(-0.02f, -0.01f, -0.02f, -0.04f, -0.005f, -0.081f)
        curveToRelative(0.08f, -0.06f, 0.159f, -0.12f, 0.235f, -0.183f)
        curveToRelative(0.01f, -0.01f, 0.03f, -0.01f, 0.049f, -0.006f)
        curveToRelative(3.303f, 1.516f, 6.883f, 1.516f, 10.15f, 0f)
        curveToRelative(0.02f, -0.01f, 0.03f, -0.01f, 0.05f, 0.006f)
        curveToRelative(0.076f, 0.063f, 0.155f, 0.123f, 0.235f, 0.183f)
        curveToRelative(0.02f, 0.02f, 0.01f, 0.05f, -0.005f, 0.081f)
        curveToRelative(-0.39f, 0.23f, -0.78f, 0.41f, -1.164f, 0.555f)
        curveToRelative(-0.02f, 0.01f, -0.03f, 0.03f, -0.026f, 0.067f)
        curveToRelative(0.292f, 0.597f, 0.64f, 1.163f, 0.93f, 1.696f)
        curveToRelative(0.01f, 0.02f, 0.03f, 0.03f, 0.053f, 0.017f)
        curveToRelative(1.648f, -0.508f, 3.321f, -1.278f, 5.048f, -2.547f)
        curveToRelative(0.01f, -0.01f, 0.02f, -0.02f, 0.019f, -0.035f)
        curveToRelative(0.421f, -4.32f, -0.294f, -8.118f, -2.979f, -11.492f)
        curveToRelative(0f, -0.01f, -0.01f, -0.02f, -0.02f, -0.019f)
        close()
        moveTo(8.97f, 14.866f)
        curveToRelative(-0.988f, 0f, -1.802f, -0.907f, -1.802f, -2.024f)
        curveToRelative(0f, -1.117f, 0.794f, -2.024f, 1.802f, -2.024f)
        curveToRelative(1.015f, 0f, 1.815f, 0.914f, 1.802f, 2.024f)
        curveToRelative(0f, 1.117f, -0.787f, 2.024f, -1.802f, 2.024f)
        close()
        moveTo(15.03f, 14.866f)
        curveToRelative(-0.988f, 0f, -1.802f, -0.907f, -1.802f, -2.024f)
        curveToRelative(0f, -1.117f, 0.794f, -2.024f, 1.802f, -2.024f)
        curveToRelative(1.015f, 0f, 1.814f, 0.914f, 1.802f, 2.024f)
        curveToRelative(0f, 1.117f, -0.787f, 2.024f, -1.802f, 2.024f)
        close()
    }.build()

/**
 * Custom SVG Forum/Community speech bubbles logo for Jetpack Compose.
 */
val CommunityIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Community",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(20f, 2f)
        lineTo(4f, 2f)
        curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
        verticalLineToRelative(18f)
        lineToRelative(4f, -4f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        lineTo(22f, 4f)
        curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
        close()
        moveTo(20f, 16f)
        lineTo(5.2f, 16f)
        lineTo(4f, 17.2f)
        lineTo(4f, 4f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(12f)
        close()
        moveTo(6f, 9f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(2f)
        lineTo(6f, 11f)
        close()
        moveTo(6f, 5f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(2f)
        lineTo(6f, 7f)
        close()
        moveTo(6f, 13f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(2f)
        lineTo(6f, 15f)
        close()
    }.build()
