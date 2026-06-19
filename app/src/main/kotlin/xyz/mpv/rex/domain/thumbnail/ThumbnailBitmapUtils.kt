package xyz.mpv.rex.domain.thumbnail

import android.graphics.Bitmap
import kotlin.math.abs

internal const val MAX_THUMBNAIL_SIZE = 512

internal fun calculateThumbnailSampleSize(
  width: Int,
  height: Int,
  maxSize: Int = MAX_THUMBNAIL_SIZE,
): Int {
  if (width <= maxSize && height <= maxSize) return 1
  var inSampleSize = 1
  val maxDimension = maxOf(width, height)
  while (maxDimension / (inSampleSize * 2) >= maxSize) {
    inSampleSize *= 2
  }
  return inSampleSize
}

internal fun Bitmap.scaleToThumbnailMax(maxSize: Int = MAX_THUMBNAIL_SIZE): Bitmap {
  if (width <= maxSize && height <= maxSize) return this
  val scale = maxSize.toFloat() / maxOf(width, height)
  val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
  val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
  val scaled = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
  if (scaled !== this && !isRecycled) recycle()
  return scaled
}

internal fun isMostlySolidThumbnail(
  bitmap: Bitmap,
  threshold: Float = 0.7f,
): Boolean {
  val width = bitmap.width
  val height = bitmap.height
  if (width <= 0 || height <= 0) return false

  val marginX = width / 10
  val marginY = height / 10
  val sampleAreaRight = width - marginX
  val sampleAreaBottom = height - marginY
  val gridSize = 10
  val stepX = (sampleAreaRight - marginX) / gridSize
  val stepY = (sampleAreaBottom - marginY) / gridSize

  if (stepX <= 0 || stepY <= 0) return false

  val sampledColors = IntArray(gridSize * gridSize)
  var validSampleCount = 0

  for (x in 0 until gridSize) {
    for (y in 0 until gridSize) {
      val pixelX = marginX + x * stepX
      val pixelY = marginY + y * stepY
      if (pixelX in 0 until width && pixelY in 0 until height) {
        sampledColors[validSampleCount++] = bitmap.getPixel(pixelX, pixelY)
      }
    }
  }

  if (validSampleCount == 0) return false

  val referenceColor = sampledColors[0]
  val referenceR = (referenceColor shr 16) and 0xFF
  val referenceG = (referenceColor shr 8) and 0xFF
  val referenceB = referenceColor and 0xFF
  val tolerance = 30

  // Iterate only up to validSampleCount to avoid trailing zeroes
  var similarCount = 0
  for (i in 0 until validSampleCount) {
    val color = sampledColors[i]
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF

    if (kotlin.math.abs(r - referenceR) <= tolerance &&
        kotlin.math.abs(g - referenceG) <= tolerance &&
        kotlin.math.abs(b - referenceB) <= tolerance) {
        similarCount++
    }
  }

  // Calculate threshold based strictly on valid samples
  return similarCount.toFloat() / validSampleCount >= threshold
}
