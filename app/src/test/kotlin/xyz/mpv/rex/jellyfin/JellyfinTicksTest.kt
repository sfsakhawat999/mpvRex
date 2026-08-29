package xyz.mpv.rex.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Test

class JellyfinTicksTest {

  @Test
  fun ticksPerSecondConstant() {
    assertEquals(10_000_000L, JellyfinTicks.TICKS_PER_SECOND)
  }

  @Test
  fun ticksPerMillisecondConstant() {
    assertEquals(10_000L, JellyfinTicks.TICKS_PER_MILLISECOND)
  }

  @Test
  fun msToTicks_conversion() {
    assertEquals(0L, JellyfinTicks.msToTicks(0))
    assertEquals(10_000L, JellyfinTicks.msToTicks(1))
    assertEquals(10_000_000L, JellyfinTicks.msToTicks(1000))
    assertEquals(600_000_000L, JellyfinTicks.msToTicks(60_000))
  }

  @Test
  fun ticksToMs_conversion() {
    assertEquals(1L, JellyfinTicks.ticksToMs(10_000L))
    assertEquals(1000L, JellyfinTicks.ticksToMs(10_000_000L))
  }

  @Test
  fun ticksToSeconds_roundTrip() {
    val sec = 123.456f
    val ticks = JellyfinTicks.secondsToTicks(sec)
    val back = JellyfinTicks.ticksToSeconds(ticks)
    // Float precision ~ 1e-3
    assertEquals(sec, back, 0.001f)
  }

  @Test
  fun resumeDiff_is30Seconds() {
    assertEquals(30L * JellyfinTicks.TICKS_PER_SECOND, JellyfinTicks.RESUME_DIFF_TICKS)
  }

  @Test
  fun precisePosition_preservesLongTicks() {
    // Simulate precisePosition 90.123s -> ticks Long preserves sub-second vs Int sec truncation
    val preciseSec = 90.123f
    val ticksPrecise = JellyfinTicks.secondsToTicks(preciseSec)
    val ticksTruncated = 90L * JellyfinTicks.TICKS_PER_SECOND
    // Difference should be 0.123s = 1_230_000 ticks (allow 10k tolerance for float)
    val diff = ticksPrecise - ticksTruncated
    assertEquals(true, kotlin.math.abs(diff - 1_230_000L) <= 10_000L)
  }
}
