package xyz.mpv.rex.utils.media

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import xyz.mpv.rex.BuildConfig
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.ui.player.PlayerActivity
import xyz.mpv.rex.utils.history.RecentlyPlayedOps
import xyz.mpv.rex.database.repository.VideoMetadataCacheRepository
import xyz.mpv.rex.domain.playbackstate.repository.PlaybackStateRepository
import `is`.xyz.mpv.Utils
import java.io.File
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Central entry point for video playback operations.
 *
 * ## Architecture
 *
 * **MediaUtils.playFile()** - High-level API (this class)
 * - Called by UI components (Video List, FAB buttons, dialogs)
 * - Creates Intent and launches PlayerActivity
 * - Handles Video objects, URI strings, and file paths
 *
 * **BaseMPVView.playFile()** - Low-level MPV control (library)
 * - Called internally by PlayerActivity.onCreate()
 * - **Do not call directly from UI code**
 *
 * ## Flow
 * ```
 * UI → MediaUtils.playFile() → Intent → PlayerActivity → BaseMPVView.playFile() → MPV
 * ```
 *
 * ## Special Cases
 * External apps use ACTION_SEND/ACTION_VIEW intents directly to PlayerActivity,
 * bypassing MediaUtils.
 */
object MediaUtils : KoinComponent {
  private val metadataCache: VideoMetadataCacheRepository by inject()
  private val playbackStateRepository: PlaybackStateRepository by inject()

  /**
   * Play video content from any source.
   *
   * Supports:
   * - Video objects (from media library)
   * - URI strings (http://, content://, file://)
   * - File paths (absolute or relative)
   *
   * @param source Video object, URI string, android.net.Uri, or file path
   * @param launchSource Analytics identifier (e.g., "open_file", "recently_played")
   */
  fun playFile(
    source: Any,
    context: Context,
    launchSource: String? = null,
  ) {
    val intent = when (source) {
      is Video -> {
        val videoUri = if (source.uri.scheme == null) {
          if (source.path.startsWith("/") || source.path.startsWith("file://")) {
            val path = if (source.path.startsWith("file://")) source.path.removePrefix("file://") else source.path
            Uri.fromFile(File(path))
          } else {
            source.uri
          }
        } else {
          source.uri
        }
        val it = Intent(Intent.ACTION_VIEW, videoUri)
        it.putExtra("width", source.width)
        it.putExtra("height", source.height)
        it.putExtra("rotation", source.rotation)
        source.savedOrientation?.let { orientation -> it.putExtra("saved_orientation", orientation) }
        it
      }

      is String -> {
        if (source.isBlank()) return
        val uri = if (source.startsWith("/") || source.startsWith("file://")) {
          val filePath = if (source.startsWith("file://")) source.removePrefix("file://") else source
          Uri.fromFile(File(filePath))
        } else {
          val parsedUri = source.toUri()
          parsedUri.scheme?.let { parsedUri } ?: Uri.parse("file://$source")
        }
        
        val it = Intent(Intent.ACTION_VIEW, uri)
        
        // Eagerly lookup metadata for local files to avoid jumpy transitions
        if (uri.scheme == "file") {
          val file = File(uri.path ?: "")
          if (file.exists()) {
            val fileName = file.name
            // Use runBlocking as this is called from UI thread but we need the metadata
            // Better would be to make playFile suspend, but that requires UI changes everywhere
            kotlinx.coroutines.runBlocking {
              // 1. Check for saved orientation in DB
              val state = playbackStateRepository.getVideoDataByTitle(fileName)
              if (state?.savedOrientation != null) {
                it.putExtra("saved_orientation", state.savedOrientation)
              }
              
              // 2. Check metadata cache for dimensions and rotation
              val metadata = metadataCache.getOrExtractMetadata(file, uri, fileName)
              if (metadata != null) {
                it.putExtra("width", metadata.width)
                it.putExtra("height", metadata.height)
                it.putExtra("rotation", metadata.rotation)
              }
            }
          }
        }
        it
      }

      is Uri -> {
        Intent(Intent.ACTION_VIEW, source)
      }

      else -> {
        android.util.Log.e("MediaUtils", "Unsupported source type: ${source::class.java}")
        return
      }
    }

    intent.setClass(context, PlayerActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.putExtra("internal_launch", true) // Enables subtitle autoload
    launchSource?.let { intent.putExtra("launch_source", it) }
    
    // For playlist items, pass the title so it shows correctly in the player
    if (source is Video && launchSource != null && (launchSource.contains("playlist") || launchSource == "m3u_playlist" || launchSource == "media_library_list")) {
      intent.putExtra("title", source.displayName)
    }

    context.startActivity(
      intent,
      ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, 0).toBundle()
    )
  }

  /**
   * Play multiple videos as a playlist.
   *
   * @param videos List of Video objects
   * @param startIndex Index of the video to start with
   * @param context Android context
   * @param launchSource Analytics identifier
   * @param playlistId Optional database playlist ID
   */
  fun playPlaylist(
    videos: List<Video>,
    startIndex: Int,
    context: Context,
    launchSource: String? = "playlist",
    playlistId: Int? = null,
  ) {
    if (videos.isEmpty() || startIndex < 0 || startIndex >= videos.size) return

    val firstVideo = videos[startIndex]
    val videoUri = if (firstVideo.uri.scheme == null) {
      if (firstVideo.path.startsWith("/") || firstVideo.path.startsWith("file://")) {
        val path = if (firstVideo.path.startsWith("file://")) firstVideo.path.removePrefix("file://") else firstVideo.path
        Uri.fromFile(File(path))
      } else {
        firstVideo.uri
      }
    } else {
      firstVideo.uri
    }

    val intent = Intent(Intent.ACTION_VIEW, videoUri)
    intent.setClass(context, PlayerActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.putExtra("internal_launch", true)
    
    // Pass metadata extras for the FIRST video to ensure smooth transition
    intent.putExtra("width", firstVideo.width)
    intent.putExtra("height", firstVideo.height)
    intent.putExtra("rotation", firstVideo.rotation)
    firstVideo.savedOrientation?.let { intent.putExtra("saved_orientation", it) }
    
    // Pass playlist data
    intent.putParcelableArrayListExtra("playlist", ArrayList(videos.map { it.uri }))
    intent.putExtra("playlist_index", startIndex)
    launchSource?.let { intent.putExtra("launch_source", it) }
    playlistId?.let { intent.putExtra("playlist_id", it) }
    
    // Pass title for the first video
    intent.putExtra("title", firstVideo.displayName)

    context.startActivity(
      intent,
      ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, 0).toBundle()
    )
  }

  /**
   * @deprecated Use RecentlyPlayedOps.getLastPlayed() directly
   */
  @Deprecated(
    message = "Use RecentlyPlayedOps.getLastPlayed() directly",
    replaceWith =
      ReplaceWith(
        "RecentlyPlayedOps.getLastPlayed()",
        "xyz.mpv.rex.utils.history.RecentlyPlayedOps",
      ),
  )
  suspend fun getRecentlyPlayedFile(): String? = RecentlyPlayedOps.getLastPlayed()

  /**
   * @deprecated Use RecentlyPlayedOps.hasRecentlyPlayed() directly
   */
  @Deprecated(
    message = "Use RecentlyPlayedOps.hasRecentlyPlayed() directly",
    replaceWith =
      ReplaceWith(
        "RecentlyPlayedOps.hasRecentlyPlayed()",
        "xyz.mpv.rex.utils.history.RecentlyPlayedOps",
      ),
  )
  suspend fun hasRecentlyPlayedFile(): Boolean = RecentlyPlayedOps.hasRecentlyPlayed()

  /**
   * Validate URL structure and protocol support.
   * Checks only URL format and MPV protocol support (http, https, rtsp, rtmp, etc.).
   * Network errors are detected when MPV attempts to open the stream.
   */
  fun isURLValid(url: String): Boolean =
    url.toUri().let { uri ->
      val structureOk =
        uri.isHierarchical && !uri.isRelative && (!uri.host.isNullOrBlank() || !uri.path.isNullOrBlank())
      structureOk && Utils.PROTOCOLS.contains(uri.scheme)
    }

  /**
   * Share videos via system share sheet.
   *
   * Uses ACTION_SEND for single video, ACTION_SEND_MULTIPLE for multiple videos.
   */
  fun shareVideos(
    context: Context,
    videos: List<Video>,
  ) {
    if (videos.isEmpty()) {
      android.util.Log.w("MediaUtils", "Cannot share: video list is empty")
      return
    }

    fun toSharableUri(v: Video): android.net.Uri? =
      v.uri.takeIf { it.scheme.equals("content", true) } ?: run {
        try {
          FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", File(v.path))
        } catch (e: IllegalArgumentException) {
          android.util.Log.e("MediaUtils", "FileProvider failed for ${v.path}: ${e.message}")
          null
        } catch (e: Exception) {
          android.util.Log.e("MediaUtils", "Failed to generate URI for ${v.path}", e)
          null
        }
      }

    val uris = videos.mapNotNull { toSharableUri(it) }

    if (uris.isEmpty()) {
      android.util.Log.w("MediaUtils", "Cannot share: no valid URIs generated for any videos")
      return
    }

    if (uris.size < videos.size) {
      android.util.Log.w("MediaUtils", "Only ${uris.size}/${videos.size} videos could be shared")
    }

    val intent =
      if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
          type = "video/*"
          putExtra(Intent.EXTRA_STREAM, uris.first())
          putExtra(Intent.EXTRA_SUBJECT, videos.first().displayName)
          putExtra(Intent.EXTRA_TITLE, videos.first().displayName)
          clipData = android.content.ClipData.newRawUri(videos.first().displayName, uris.first())
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
          type = "video/*"
          putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
          putExtra(Intent.EXTRA_SUBJECT, "Sharing ${uris.size} videos")
          val clip = android.content.ClipData.newRawUri(videos.first().displayName, uris.first())
          uris.drop(1).forEach { u -> clip.addItem(android.content.ClipData.Item(u)) }
          clipData = clip
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      }

    context.startActivity(
      Intent.createChooser(
        intent,
        if (uris.size == 1) "Share video" else "Share ${uris.size} videos",
      ),
    )
  }

}
