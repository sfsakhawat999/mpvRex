package app.marlboroadvance.mpvex.domain.media.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoFolder(
  val bucketId: String,
  val name: String,
  val path: String,
  val videoCount: Int,
  val audioCount: Int = 0,
  val totalSize: Long = 0L,
  val totalDuration: Long = 0L, // in milliseconds
  val lastModified: Long = 0L,
  val newCount: Int = 0,
  val unwatchedCount: Int = 0,
  )
