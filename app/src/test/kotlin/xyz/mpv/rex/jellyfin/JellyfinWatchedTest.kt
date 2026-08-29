package xyz.mpv.rex.jellyfin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinWatchedTest {

  private fun isWatched(positionTicks: Long, durationTicks: Long, thresholdPct: Int): Boolean {
    if (durationTicks <= 0) return false
    val pct = positionTicks.toDouble() / durationTicks.toDouble() * 100.0
    return pct >= thresholdPct
  }

  @Test
  fun watched_belowThreshold_notWatched() {
    val dur = 100L * JellyfinTicks.TICKS_PER_SECOND
    val pos = 94L * JellyfinTicks.TICKS_PER_SECOND
    assertFalse(isWatched(pos, dur, 95))
  }

  @Test
  fun watched_atThreshold_isWatched() {
    val dur = 100L * JellyfinTicks.TICKS_PER_SECOND
    val pos = 95L * JellyfinTicks.TICKS_PER_SECOND
    assertTrue(isWatched(pos, dur, 95))
  }

  @Test
  fun watched_aboveThreshold_isWatched() {
    val dur = 100L * JellyfinTicks.TICKS_PER_SECOND
    val pos = 96L * JellyfinTicks.TICKS_PER_SECOND
    assertTrue(isWatched(pos, dur, 95))
  }

  @Test
  fun watched_zeroDuration_notWatched() {
    assertFalse(isWatched(10L * JellyfinTicks.TICKS_PER_SECOND, 0L, 95))
  }

  @Test
  fun ticksComparison_usesLong_notIntSeconds() {
    // 94.9s vs 95s threshold: int sec truncation would miss, ticks should catch correctly
    val dur = 100L * JellyfinTicks.TICKS_PER_SECOND
    val posTicks = JellyfinTicks.secondsToTicks(94.9f) // 94.9s <95%
    assertFalse(isWatched(posTicks, dur, 95))
    val posTicks2 = JellyfinTicks.secondsToTicks(95.1f)
    assertTrue(isWatched(posTicks2, dur, 95))
  }
}
