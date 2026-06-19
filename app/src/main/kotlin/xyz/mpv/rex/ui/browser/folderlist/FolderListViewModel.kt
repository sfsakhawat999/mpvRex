package xyz.mpv.rex.ui.browser.folderlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import xyz.mpv.rex.database.repository.VideoMetadataCacheRepository
import xyz.mpv.rex.domain.media.model.VideoFolder
import xyz.mpv.rex.domain.playbackstate.repository.PlaybackStateRepository
import xyz.mpv.rex.repository.MediaFileRepository
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.FoldersPreferences
import xyz.mpv.rex.ui.browser.base.BaseBrowserViewModel
import xyz.mpv.rex.utils.media.MediaLibraryEvents
import xyz.mpv.rex.utils.media.MetadataRetrieval
import xyz.mpv.rex.utils.storage.FileTypeUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class FolderWithNewCount(
  val folder: VideoFolder,
  val newVideoCount: Int = 0,
)

class FolderListViewModel(
  application: Application,
) : BaseBrowserViewModel<FolderWithNewCount>(application),
  KoinComponent {
  private val foldersPreferences: FoldersPreferences by inject()
  private val appearancePreferences: AppearancePreferences by inject()
  private val browserPreferences: xyz.mpv.rex.preferences.BrowserPreferences by inject()
  private val recentlyPlayedRepository: RecentlyPlayedRepository by inject()

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

  // Set of folder paths that have ever had a video played from them
  val playedFolderPaths: StateFlow<Set<String>> =
    recentlyPlayedRepository
      .observeRecentlyPlayed(limit = 1000)
      .map { list ->
        list.mapNotNull { java.io.File(it.filePath).parent }.toSet()
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  private val _foldersWithNewCount = MutableStateFlow<List<FolderWithNewCount>>(emptyList())
  val foldersWithNewCount: StateFlow<List<FolderWithNewCount>> = _foldersWithNewCount.asStateFlow()

  // Track if initial load has completed to prevent empty state flicker
  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  // Track if folders were deleted leaving list empty
  private val _foldersWereDeleted = MutableStateFlow(false)
  val foldersWereDeleted: StateFlow<Boolean> = _foldersWereDeleted.asStateFlow()

  // Track previous folder count to detect if all folders were deleted
  private var previousFolderCount = 0

  /*
   * TRACKING LOADING STATE
   */
  private val _scanStatus = MutableStateFlow<String?>(null)
  val scanStatus: StateFlow<String?> = _scanStatus.asStateFlow()

  private val _isEnriching = MutableStateFlow(false)
  val isEnriching: StateFlow<Boolean> = _isEnriching.asStateFlow()

  // Track the current scan job to prevent concurrent scans
  private var currentScanJob: Job? = null

  companion object {
    private const val TAG = "FolderListViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FolderListViewModel(application) as T
      }
  }

  init {
    // Load cached folders instantly for immediate display
    val hasCachedData = loadCachedFolders()

    if (hasCachedData) {
      // If we have cached data, show it immediately and refresh silently in background
      _hasCompletedInitialLoad.value = true
      _isLoading.value = false
      viewModelScope.launch(Dispatchers.IO) {
        loadData() 
      }
    } else {
      // No cache, must show scanning UI
      loadData()
    }

    // Note: BaseBrowserViewModel handles MediaLibraryEvents.changes and playback state observation centrally.

    // Filter folders based on blacklist and audio visibility
    viewModelScope.launch {
      combine(
        _allVideoFolders, 
        foldersPreferences.blacklistedFolders.changes(),
        browserPreferences.showAudioFiles.changes()
      ) { folders, blacklist, showAudio ->
        folders.filter { folder -> 
          folder.path !in blacklist && (showAudio || folder.videoCount > 0)
        }
      }.collectLatest { filteredFolders ->
        // Check if folders became empty after having folders
        if (previousFolderCount > 0 && filteredFolders.isEmpty()) {
          _foldersWereDeleted.value = true
          Log.d(TAG, "Folders became empty (had $previousFolderCount folders before)")
        } else if (filteredFolders.isNotEmpty()) {
          // Reset flag if folders now exist
          _foldersWereDeleted.value = false
        }

        // Update previous count
        previousFolderCount = filteredFolders.size

        _videoFolders.value = filteredFolders
        
        // Map to FolderWithNewCount using the pre-calculated newCount from repository
        _foldersWithNewCount.value = filteredFolders.map { 
          FolderWithNewCount(it, it.newCount) 
        }

        // Save to cache for next app launch (save unfiltered list)
        saveFoldersToCache(_allVideoFolders.value)
      }
    }
  }

  private fun loadCachedFolders(): Boolean {
    var hasCachedData = false
    val prefs =
      getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
    val cachedJson = prefs.getString("folders", null)

    if (cachedJson != null) {
      try {
        val folders = parseFoldersFromJson(cachedJson)
        if (folders.isNotEmpty()) {
          _allVideoFolders.value = folders
          hasCachedData = true
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error parsing cached folders", e)
      }
    }
    return hasCachedData
  }

  private fun saveFoldersToCache(folders: List<VideoFolder>) {
    try {
      val json = serializeFoldersToJson(folders)
      val prefs =
        getApplication<Application>().getSharedPreferences("folder_cache", android.content.Context.MODE_PRIVATE)
      prefs.edit().putString("folders", json).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Error saving folders to cache", e)
    }
  }

  // Basic manual JSON serialization to avoid adding GSON/Kotlinx Serialization dependencies if not present
  // In a real app, use a proper library
  private fun serializeFoldersToJson(folders: List<VideoFolder>): String {
    // For now using a simple approach since we only cache basic info
    return folders.joinToString("|") { folder ->
      "${folder.bucketId};${folder.name};${folder.path};${folder.videoCount};${folder.audioCount};${folder.totalSize};${folder.totalDuration};${folder.lastModified};${folder.newCount}"
    }
  }

  private fun parseFoldersFromJson(json: String): List<VideoFolder> {
    if (json.isBlank()) return emptyList()
    return json.split("|").mapNotNull { line ->
      try {
        val parts = line.split(";")
        VideoFolder(
          bucketId = parts[0],
          name = parts[1],
          path = parts[2],
          videoCount = parts[3].toInt(),
          audioCount = parts[4].toInt(),
          totalSize = parts[5].toLong(),
          totalDuration = parts[6].toLong(),
          lastModified = parts[7].toLong(),
          newCount = if (parts.size > 8) parts[8].toInt() else 0
        )
      } catch (e: Exception) {
        null
      }
    }
  }

  override fun loadData() {
    loadVideoFolders()
  }

  private fun loadVideoFolders() {
    // Prevent multiple concurrent scans
    if (currentScanJob?.isActive == true) {
      Log.d(TAG, "Scan already in progress, skipping")
      return
    }

    currentScanJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        val isFirstLoad = _allVideoFolders.value.isEmpty()
        if (isFirstLoad) {
          _isLoading.value = true
          _scanStatus.value = "Scanning media..."
        }
        
        val startTime = System.currentTimeMillis()
        var folders = MediaFileRepository.getAllVideoFolders(getApplication())
        
        val scanTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Media scan completed in ${scanTime}ms, found ${folders.size} folders")

        // Enrich with metadata only if needed
        if (MetadataRetrieval.isFolderMetadataNeeded(browserPreferences)) {
          if (isFirstLoad) {
            _isEnriching.value = true
            _scanStatus.value = "Extracting metadata..."
          }
          folders = MetadataRetrieval.enrichFoldersIfNeeded(
            context = getApplication(),
            folders = folders,
            browserPreferences = browserPreferences,
            metadataCache = metadataCache
          )
          _isEnriching.value = false
        }

        _allVideoFolders.value = folders
        _isLoading.value = false
        _hasCompletedInitialLoad.value = true
        _scanStatus.value = null
      } catch (e: Exception) {
        Log.e(TAG, "Error loading video folders", e)
        _isLoading.value = false
        _hasCompletedInitialLoad.value = true
        _scanStatus.value = "Error loading folders"
      }
    }
  }

  suspend fun renameFolder(folder: VideoFolder, newName: String): Boolean {
    val src = java.io.File(folder.path)
    val dst = java.io.File(src.parent ?: return false, newName)
    if (dst.exists()) return false
    val ok = src.renameTo(dst)
    if (ok) {
      android.media.MediaScannerConnection.scanFile(getApplication(), arrayOf(dst.absolutePath), null, null)
      _foldersWereDeleted.value = true
    }
    return ok
  }

  /**
   * Delete folders and update state
   */
  suspend fun deleteFolders(foldersToDelete: List<VideoFolder>): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val showAudio = browserPreferences.showAudioFiles.get()
    var successCount = 0
    var failureCount = 0

    foldersToDelete.forEach { folder ->
      try {
        val dir = File(folder.path)
        if (!dir.exists() || !dir.isDirectory) {
          failureCount++
          return@forEach
        }

        // Targeted deletion: Only delete media files in the immediate folder
        val children = dir.listFiles() ?: emptyArray()
        var filesDeleted = 0
        
        children.forEach { file ->
          if (file.isDirectory) return@forEach // Never recurse into subfolders
          
          val isVideo = FileTypeUtils.isVideoFile(file)
          val isSubtitle = FileTypeUtils.isSubtitleFile(file)
          val isAudio = showAudio && FileTypeUtils.isAudioFile(file)
          
          if (isVideo || isSubtitle || isAudio) {
            if (file.delete()) {
              filesDeleted++
            }
          }
        }

        if (filesDeleted > 0) {
          successCount++
        } else {
          failureCount++
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete folder contents: ${folder.path}", e)
        failureCount++
      }
    }

    // Update local state if anything was deleted
    if (successCount > 0) {
      val currentFolders = _allVideoFolders.value.toMutableList()
      foldersToDelete.forEach { folder ->
        // For simplicity, we remove all requested folders from UI state if we attempted cleaning
        currentFolders.removeAll { it.path == folder.path }
      }
      _allVideoFolders.value = currentFolders
      _foldersWereDeleted.value = true
      MediaLibraryEvents.notifyChanged()
    }

    Pair(successCount, failureCount)
  }
}
