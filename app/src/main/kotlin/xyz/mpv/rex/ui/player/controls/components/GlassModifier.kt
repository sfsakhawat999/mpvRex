package xyz.mpv.rex.ui.player.controls.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Custom Glass Surface Modifier that renders true directional inner shadows and clipped outer shadows
fun Modifier.glassSurface(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.00f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    borderWidth: Dp = 1.dp,
    
    // Outer shadow
    outerShadowColor: Color = Color.Black.copy(alpha = 0.00f),
    outerShadowBlur: Dp = 0.dp,
    outerShadowOffsetX: Dp = 0.dp,
    outerShadowOffsetY: Dp = 0.dp,
    
    // Inner Highlight (Top-left)
    innerHighlightColor: Color = Color.White.copy(alpha = 0.35f),
    innerHighlightBlur: Dp = 5.dp,
    innerHighlightOffsetX: Dp = (-2).dp,
    innerHighlightOffsetY: Dp = (-2).dp,
    
    // Inner Shadow (Bottom-right)
    innerShadowColor: Color = Color.Black.copy(alpha = 0.35f),
    innerShadowBlur: Dp = 5.dp,
    innerShadowOffsetX: Dp = 2.dp,
    innerShadowOffsetY: Dp = 2.dp
) = this.drawWithContent {
    val density = this
    val outerShadowBlurPx = with(density) { outerShadowBlur.toPx() }
    val outerShadowOffsetXPx = with(density) { outerShadowOffsetX.toPx() }
    val outerShadowOffsetYPx = with(density) { outerShadowOffsetY.toPx() }
    
    val cornerRadiusPx = with(density) { shape.topStart.toPx(size, density) }
    
    drawIntoCanvas { canvas ->
        if (outerShadowBlurPx > 0f && outerShadowColor.alpha > 0f) {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                )
            }
            canvas.save()
            canvas.clipPath(path, clipOp = ClipOp.Difference)
            
            val shadowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    outerShadowBlurPx,
                    outerShadowOffsetXPx,
                    outerShadowOffsetYPx,
                    outerShadowColor.toArgb()
                )
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadiusPx, cornerRadiusPx,
                shadowPaint
            )
            canvas.restore()
        }
    }
    
    drawRoundRect(
        color = backgroundColor,
        cornerRadius = CornerRadius(cornerRadiusPx)
    )
    
    val innerShadowBlurPx = with(density) { innerShadowBlur.toPx() }
    val innerShadowOffsetXPx = with(density) { innerShadowOffsetX.toPx() }
    val innerShadowOffsetYPx = with(density) { innerShadowOffsetY.toPx() }
    
    val innerHighlightBlurPx = with(density) { innerHighlightBlur.toPx() }
    val innerHighlightOffsetXPx = with(density) { innerHighlightOffsetX.toPx() }
    val innerHighlightOffsetYPx = with(density) { innerHighlightOffsetY.toPx() }
    
    drawIntoCanvas { canvas ->
        val rectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            )
        }
        
        canvas.save()
        canvas.clipPath(rectPath)
        
        if (innerHighlightBlurPx > 0f && innerHighlightColor.alpha > 0f) {
            val highlightPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    innerHighlightBlurPx,
                    innerHighlightOffsetXPx,
                    innerHighlightOffsetYPx,
                    innerHighlightColor.toArgb()
                )
                style = android.graphics.Paint.Style.FILL
            }
            
            val invertedPath = android.graphics.Path().apply {
                addRect(
                    -innerHighlightBlurPx - 20f,
                    -innerHighlightBlurPx - 20f,
                    size.width + innerHighlightBlurPx + 20f,
                    size.height + innerHighlightBlurPx + 20f,
                    android.graphics.Path.Direction.CW
                )
                val buttonPath = android.graphics.Path().apply {
                    val rectF = android.graphics.RectF(0f, 0f, size.width, size.height)
                    addRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
                }
                op(buttonPath, android.graphics.Path.Op.DIFFERENCE)
            }
            canvas.nativeCanvas.drawPath(invertedPath, highlightPaint)
        }
        
        if (innerShadowBlurPx > 0f && innerShadowColor.alpha > 0f) {
            val shadowPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    innerShadowBlurPx,
                    innerShadowOffsetXPx,
                    innerShadowOffsetYPx,
                    innerShadowColor.toArgb()
                )
                style = android.graphics.Paint.Style.FILL
            }
            
            val invertedShadowPath = android.graphics.Path().apply {
                addRect(
                    -innerShadowBlurPx - 20f,
                    -innerShadowBlurPx - 20f,
                    size.width + innerShadowBlurPx + 20f,
                    size.height + innerShadowBlurPx + 20f,
                    android.graphics.Path.Direction.CW
                )
                val buttonPath = android.graphics.Path().apply {
                    val rectF = android.graphics.RectF(0f, 0f, size.width, size.height)
                    addRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
                }
                op(buttonPath, android.graphics.Path.Op.DIFFERENCE)
            }
            canvas.nativeCanvas.drawPath(invertedShadowPath, shadowPaint)
        }
        
        canvas.restore()
    }
    
    drawContent()
    
    if (borderWidth > 0.dp && borderColor.alpha > 0f) {
        val borderWidthPx = with(density) { borderWidth.toPx() }
        drawRoundRect(
            color = borderColor,
            cornerRadius = CornerRadius(cornerRadiusPx),
            style = Stroke(width = borderWidthPx)
        )
    }
}
