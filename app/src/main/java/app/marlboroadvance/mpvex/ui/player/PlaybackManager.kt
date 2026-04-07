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
    private val playerPreferences: PlayerPreferences,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PlaybackManager"
        private const val SEEK_COALESCE_DELAY_MS = 300L
    }

    private var pendingSeekOffset: Int = 0
    private var seekCoalesceJob: Job? = null

    /**
     * Performs an absolute seek to the specified position.
     * Clamps the position between 0 and duration, and optionally within AB loop.
     */
    fun seekTo(position: Int, abLoopA: Double? = null, abLoopB: Double? = null) {
        scope.launch(Dispatchers.IO) {
            val maxDuration = MPVLib.getPropertyInt("duration") ?: 0
            var clampedPosition = position.coerceIn(0, maxDuration)

            // Clamp within AB loop if active
            if (abLoopA != null && abLoopB != null) {
                val min = minOf(abLoopA.toInt(), abLoopB.toInt())
                val max = maxOf(abLoopA.toInt(), abLoopB.toInt())
                clampedPosition = clampedPosition.coerceIn(min, max)
            }

            if (clampedPosition !in 0..maxDuration) return@launch

            // Cancel pending relative seek before absolute seek
            seekCoalesceJob?.cancel()
            pendingSeekOffset = 0

            // Use precise seeking for short videos or if preference is enabled
            val shouldUsePreciseSeeking = playerPreferences.usePreciseSeeking.get() || maxDuration < 120
            val seekMode = if (shouldUsePreciseSeeking) "absolute+exact" else "absolute+keyframes"
            MPVLib.command("seek", clampedPosition.toString(), seekMode)
        }
    }

    /**
     * Performs a relative seek with coalescing for smooth performance.
     */
    fun seekBy(offset: Int) {
        pendingSeekOffset += offset
        seekCoalesceJob?.cancel()
        seekCoalesceJob = scope.launch(Dispatchers.IO) {
            delay(SEEK_COALESCE_DELAY_MS)
            val toApply = pendingSeekOffset
            pendingSeekOffset = 0

            if (toApply != 0) {
                val duration = MPVLib.getPropertyInt("duration") ?: 0
                val currentPos = MPVLib.getPropertyInt("time-pos") ?: 0

                if (duration > 0 && currentPos + toApply >= duration) {
                    // Force seek to 100% to ensure EOF is triggered
                    MPVLib.command("seek", "100", "absolute-percent+exact")
                } else {
                    val shouldUsePreciseSeeking = playerPreferences.usePreciseSeeking.get() || duration < 120
                    val seekMode = if (shouldUsePreciseSeeking) "relative+exact" else "relative+keyframes"
                    MPVLib.command("seek", toApply.toString(), seekMode)
                }
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
