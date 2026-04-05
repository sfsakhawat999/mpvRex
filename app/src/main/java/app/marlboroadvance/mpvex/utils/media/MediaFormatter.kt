package app.marlboroadvance.mpvex.utils.media

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Unified formatting utilities for media-related data
 */
object MediaFormatter {

    /**
     * Formats a timestamp in milliseconds to a human-readable date (MMM dd)
     */
    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "--"
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Formats duration in milliseconds to HH:MM:SS or MM:SS
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0s"

        val seconds = durationMs / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format(Locale.getDefault(), "%d:%02d", minutes, secs)
            else -> "${secs}s"
        }
    }

    /**
     * Formats file size in bytes to human-readable string (KB, MB, GB, etc.)
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return try {
            String.format(
                Locale.getDefault(),
                "%.1f %s",
                bytes / 1024.0.pow(digitGroups.toDouble()),
                units[digitGroups]
            )
        } catch (e: Exception) {
            "0 B"
        }
    }

    /**
     * Formats resolution into standard labels (1080p, 720p, etc.)
     */
    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "--"

        return when {
            width >= 7680 || height >= 4320 -> "4320p"
            width >= 3840 || height >= 2160 -> "2160p"
            width >= 2560 || height >= 1440 -> "1440p"
            width >= 1920 || height >= 1080 -> "1080p"
            width >= 1280 || height >= 720 -> "720p"
            width >= 854 || height >= 480 -> "480p"
            width >= 640 || height >= 360 -> "360p"
            width >= 426 || height >= 240 -> "240p"
            else -> "${height}p"
        }
    }

    /**
     * Formats resolution including FPS
     */
    fun formatResolutionWithFps(width: Int, height: Int, fps: Float): String {
        val baseResolution = formatResolution(width, height)
        if (baseResolution == "--" || fps <= 0f) return baseResolution

        // Show only the integer part for frame rates
        val fpsFormatted = fps.toInt().toString()
        return "$baseResolution@$fpsFormatted"
    }
}
