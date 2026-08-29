package xyz.mpv.rex.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.mpv.rex.jellyfin.sync.JellyfinIdentityResolver

class JellyfinIdentityResolverTest {

  @Test
  fun parseVideosStream_dashedUuid() {
    val url = "https://jelly.example.com/Videos/3a2952eb-8c26-4a1a-b2f3-123456789abc/stream?static=true&mediaSourceId=abc&playSessionId=def"
    val r = JellyfinIdentityResolver.resolveFromUrl(url)
    assertNotNull(r)
    assertEquals("3a2952eb-8c26-4a1a-b2f3-123456789abc", r!!.itemId)
    assertEquals("abc", r.mediaSourceId)
    assertEquals("def", r.playSessionId)
  }

  @Test
  fun parseVideosStream_undashedUuid() {
    val url = "https://jelly.example.com/Videos/3a2952eb8c264a1ab2f3123456789abc/stream"
    val r = JellyfinIdentityResolver.resolveFromUrl(url)
    assertNotNull(r)
    assertEquals("3a2952eb-8c26-4a1a-b2f3-123456789abc", r!!.itemId)
  }

  @Test
  fun parseVideosStream_invalid_returnsNull() {
    val url = "https://jelly.example.com/Videos/not-a-uuid/stream"
    assertNull(JellyfinIdentityResolver.resolveFromUrl(url))
  }

  @Test
  fun parseNonJellyfin_returnsNull() {
    val url = "https://example.com/media/movie.mp4"
    assertNull(JellyfinIdentityResolver.resolveFromUrl(url))
  }

  @Test
  fun normalizeUuid_dashed_lowercases() {
    val out = JellyfinIdentityResolver.normalizeUuid("3A2952EB-8C26-4A1A-B2F3-123456789ABC")
    assertEquals("3a2952eb-8c26-4a1a-b2f3-123456789abc", out)
  }

  @Test
  fun normalizeUuid_undashed_insertsDashes() {
    val out = JellyfinIdentityResolver.normalizeUuid("3a2952eb8c264a1ab2f3123456789abc")
    assertEquals("3a2952eb-8c26-4a1a-b2f3-123456789abc", out)
  }

  @Test
  fun parseVideosStream_queryDecoding() {
    val url = "https://jelly.example.com/Videos/3a2952eb-8c26-4a1a-b2f3-123456789abc/stream?mediaSourceId=abc%20123&playSessionId=def"
    val r = JellyfinIdentityResolver.resolveFromUrl(url)
    assertNotNull(r)
    assertEquals("abc 123", r!!.mediaSourceId)
  }

  @Test
  fun parseVideosStream_noQuery_returnsNullParams() {
    val url = "https://jelly.example.com/Videos/3a2952eb-8c26-4a1a-b2f3-123456789abc/stream"
    val r = JellyfinIdentityResolver.resolveFromUrl(url)
    assertNotNull(r)
    assertNull(r!!.mediaSourceId)
    assertNull(r.playSessionId)
  }
}
