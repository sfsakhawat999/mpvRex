package xyz.mpv.rex.jellyfin

/**
 * Jellyfin uses .NET ticks (100ns) for playback positions.
 * 1 tick = 100 nanoseconds, 10_000 ticks = 1ms, 10_000_000 ticks = 1s.
 */
object JellyfinTicks {
  const val TICKS_PER_MILLISECOND: Long = 10_000L
  const val TICKS_PER_SECOND: Long = 10_000_000L
  const val RESUME_DIFF_TICKS: Long = 30L * TICKS_PER_SECOND

  fun msToTicks(ms: Long): Long = ms * TICKS_PER_MILLISECOND

  fun ticksToMs(ticks: Long): Long = ticks / TICKS_PER_MILLISECOND

  fun secondsToTicks(seconds: Float): Long = (seconds * TICKS_PER_SECOND).toLong()

  fun ticksToSeconds(ticks: Long): Float = ticks.toFloat() / TICKS_PER_SECOND

  fun durationMsToTicks(durationMs: Long): Long = msToTicks(durationMs)
}
