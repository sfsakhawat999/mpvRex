package app.marlboroadvance.mpvex.repository

import android.content.Context
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.browser.PathComponent
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.utils.storage.CoreMediaScanner
import app.marlboroadvance.mpvex.utils.storage.VideoScanUtils
import app.marlboroadvance.mpvex.utils.storage.FileSystemOps
import app.marlboroadvance.mpvex.utils.media.MediaMetadataOps
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * Unified repository for ALL media file operations.
 * Now refactored to delegate to specialized logic classes (Ops).
 */
object MediaFileRepository {

  /**
   * Clears all caches
   */
  fun clearCache() {
    CoreMediaScanner.clearCache()
  }

  // ==================== FOLDER OPERATIONS ====================

  suspend fun getAllVideoFolders(context: Context): List<VideoFolder> =
    MediaMetadataOps.getAllMediaFolders(context)

  suspend fun getAllVideoFoldersFast(context: Context, onProgress: ((Int) -> Unit)? = null): List<VideoFolder> = 
    getAllVideoFolders(context)

  // ==================== VIDEO FILE OPERATIONS ====================

  suspend fun getVideosInFolder(context: Context, bucketId: String): List<Video> =
    withContext(Dispatchers.IO) {
      runCatching { VideoScanUtils.getVideosInFolder(context, bucketId) }.getOrDefault(emptyList())
    }

  suspend fun getVideosForBuckets(context: Context, bucketIds: Set<String>): List<Video> =
    withContext(Dispatchers.IO) {
      bucketIds.flatMap { id ->
        runCatching { VideoScanUtils.getVideosInFolder(context, id) }.getOrDefault(emptyList())
      }
    }

  // ==================== FILE SYSTEM BROWSING ====================

  fun getPathComponents(path: String): List<PathComponent> =
    FileSystemOps.getPathComponents(path)

  suspend fun scanDirectory(
    context: Context,
    path: String,
    showAllFileTypes: Boolean = false,
    useFastCount: Boolean = false,
  ): Result<List<FileSystemItem>> =
    withContext(Dispatchers.IO) {
      try {
        val directory = File(path)
        if (!directory.exists() || !directory.canRead() || !directory.isDirectory) {
          return@withContext Result.failure(Exception("Invalid directory: $path"))
        }

        val items = mutableListOf<FileSystemItem>()
        val koin = GlobalContext.get()
        val browserPreferences = koin.get<BrowserPreferences>()
        val appearancePreferences = koin.get<AppearancePreferences>()
        val playbackStateRepository = koin.get<PlaybackStateRepository>()
        
        val isAudioEnabled = browserPreferences.showAudioFiles.get()
        val playbackStates = playbackStateRepository.getAllPlaybackStates()
        val thresholdDays = appearancePreferences.unplayedOldVideoDays.get()
        val watchedThreshold = browserPreferences.watchedThreshold.get()
        
        // Get blacklisted folders
        val foldersPreferences = koin.get<app.marlboroadvance.mpvex.preferences.FoldersPreferences>()
        val blacklistedFolders = foldersPreferences.blacklistedFolders.get()

        // Get folders using CoreMediaScanner
        val folders = CoreMediaScanner.getFoldersInDirectory(
            context = context, 
            parentPath = path, 
            playbackStates = playbackStates, 
            thresholdDays = thresholdDays,
            watchedThreshold = watchedThreshold,
            blacklistedFolders = blacklistedFolders
        )
        folders
          .filter { data -> 
            (isAudioEnabled || data.videoCount > 0)
          }
          .forEach { folderData ->
            items.add(
              FileSystemItem.Folder(
                name = folderData.name,
                path = folderData.path,
                lastModified = folderData.lastModified,
                videoCount = folderData.videoCount,
                audioCount = folderData.audioCount,
                totalSize = folderData.totalSize,
                totalDuration = folderData.totalDuration,
                hasSubfolders = folderData.hasSubfolders,
                newCount = folderData.newCount,
                unwatchedCount = folderData.unwatchedCount
              ),
            )
          }

        // Get videos in current directory - Hide if current path is blacklisted
        if (path !in blacklistedFolders) {
          val videos = VideoScanUtils.getVideosInFolder(context, path)
          videos
            .filter { video -> isAudioEnabled || !video.isAudio }
            .forEach { video ->
              items.add(
                FileSystemItem.VideoFile(
                  name = video.displayName,
                  path = video.path,
                  lastModified = File(video.path).lastModified(),
                  video = video,
                ),
              )
            }
        }

        Result.success(items)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun getStorageRoots(context: Context): List<FileSystemItem.Folder> =
    FileSystemOps.getStorageRoots(context)
}
