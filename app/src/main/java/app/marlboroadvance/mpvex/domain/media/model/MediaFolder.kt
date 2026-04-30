package app.marlboroadvance.mpvex.domain.media.model

import androidx.compose.runtime.Immutable

/**
 * Unified model for a media folder used across all views (Folder View, Tree View, etc.)
 * 
 * @param id Unique identifier (usually the absolute path)
 * @param name Display name of the folder
 * @param path Absolute path on the filesystem
 * @param videoCount Number of videos in this folder (or recursively if [isRecursive] is true)
 * @param audioCount Number of audio files in this folder (or recursively if [isRecursive] is true)
 * @param totalSize Combined size of all media in bytes
 * @param totalDuration Combined duration of all media in milliseconds
 * @param lastModified Latest modification timestamp
 * @param hasSubfolders True if this folder contains subdirectories
 * @param isRecursive True if the counts and totals include descendants (used for Tree View roots)
 */
@Immutable
data class MediaFolder(
  val id: String,
  val name: String,
  val path: String,
  val videoCount: Int,
  val audioCount: Int = 0,
  val totalSize: Long = 0L,
  val totalDuration: Long = 0L,
  val lastModified: Long = 0L,
  val hasSubfolders: Boolean = false,
  val isRecursive: Boolean = false,
  val newCount: Int = 0,
  val unwatchedCount: Int = 0,
)
