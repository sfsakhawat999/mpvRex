package app.marlboroadvance.mpvex.ui.player

import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages playback operations like seeking and speed control.
 */
class PlaybackManager(
    private val playerPreferences: PlayerPreferences
) {
    companion object {
        private const val TAG = "PlaybackManager"
    }

    /**
     * Performs an absolute seek to the specified position.
     * Clamps the position between 0 and duration, and optionally within AB loop.
     */
    fun seekTo(scope: CoroutineScope, position: Int, abLoopA: Double?, abLoopB: Double?) {
        scope.launch(Dispatchers.IO) {
            val maxDuration = MPVLib.getPropertyInt("duration") ?: 0
            if (maxDuration <= 0) return@launch

            var clampedPosition = position
            if (abLoopA != null && abLoopB != null) {
                val min = minOf(abLoopA.toInt(), abLoopB.toInt())
                val max = maxOf(abLoopA.toInt(), abLoopB.toInt())
                clampedPosition = clampedPosition.coerceIn(min, max)
            }

            if (clampedPosition !in 0..maxDuration) return@launch

            // Use precise seeking for short videos or if preference is enabled
            val shouldUsePreciseSeeking = playerPreferences.usePreciseSeeking.get() || maxDuration < 120
            val seekMode = if (shouldUsePreciseSeeking) "absolute+exact" else "absolute+keyframes"
            MPVLib.command("seek", clampedPosition.toString(), seekMode)
        }
    }

    /**
     * Performs a relative seek immediately.
     */
    fun seekBy(scope: CoroutineScope, offset: Int) {
        if (offset == 0) return
        
        scope.launch(Dispatchers.IO) {
            val duration = MPVLib.getPropertyInt("duration") ?: 0
            val currentPos = MPVLib.getPropertyInt("time-pos") ?: 0

            if (duration > 0 && currentPos + offset >= duration) {
                // Force seek to 100% to ensure EOF is triggered
                MPVLib.command("seek", "100", "absolute-percent+exact")
            } else {
                val shouldUsePreciseSeeking = playerPreferences.usePreciseSeeking.get() || duration < 120
                val seekMode = if (shouldUsePreciseSeeking) "relative+exact" else "relative+keyframes"
                MPVLib.command("seek", offset.toString(), seekMode)
            }
        }
    }

    fun setSpeed(speed: Float) {
        MPVLib.setPropertyFloat("speed", speed)
    }

    fun resetSpeed() {
        setSpeed(1.0f)
    }

    fun setSubSpeed(speed: Double) {
        MPVLib.setPropertyDouble("sub-speed", speed)
        MPVLib.setPropertyDouble("secondary-sub-speed", speed)
    }
}
