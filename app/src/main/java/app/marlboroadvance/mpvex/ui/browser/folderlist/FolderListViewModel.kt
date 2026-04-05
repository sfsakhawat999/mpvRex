package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.media.MetadataRetrieval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
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
  private val browserPreferences: app.marlboroadvance.mpvex.preferences.BrowserPreferences by inject()
  private val playbackStateRepository: PlaybackStateRepository by inject()

  private val _allVideoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  private val _videoFolders = MutableStateFlow<List<VideoFolder>>(emptyList())
  val videoFolders: StateFlow<List<VideoFolder>> = _videoFolders.asStateFlow()

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

    // If no cached data (first launch), scan immediately. Otherwise defer to not slow down app launch
    if (!hasCachedData) {
      loadData()
    } else {
      viewModelScope.launch(Dispatchers.IO) {
        kotlinx.coroutines.delay(2000) // Wait 2 seconds before refreshing
        loadData()
      }
    }

    // Refresh folders on global media library changes
    viewModelScope.launch(Dispatchers.IO) {
      MediaLibraryEvents.changes.collectLatest {
        // Clear cache when media library changes
        MediaFileRepository.clearCache()
        loadData()
      }
    }

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
        // Calculate new video counts for each folder
        recalculateNewVideoCounts(filteredFolders)

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
      "${folder.bucketId};${folder.name};${folder.path};${folder.videoCount};${folder.audioCount};${folder.totalSize};${folder.totalDuration};${folder.lastModified}"
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
          lastModified = parts[7].toLong()
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
        if (_allVideoFolders.value.isEmpty()) {
          _isLoading.value = true
        }
        
        _scanStatus.value = "Scanning media..."
        val startTime = System.currentTimeMillis()
        
        var folders = MediaFileRepository.getAllVideoFolders(getApplication())
        
        val scanTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "Media scan completed in ${scanTime}ms, found ${folders.size} folders")

        // Enrich with metadata only if needed
        if (MetadataRetrieval.isFolderMetadataNeeded(browserPreferences)) {
          _isEnriching.value = true
          _scanStatus.value = "Extracting metadata..."
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

  fun recalculateNewVideoCounts(folders: List<VideoFolder> = videoFolders.value) {
    viewModelScope.launch(Dispatchers.Default) {
      try {
        val recentlyPlayedList = playbackStateRepository.getAllPlaybackStates()
        val foldersWithCount = folders.map { folder ->
          val folderVideos = MediaFileRepository.getVideosInFolder(getApplication(), folder.path)
          val unplayedCount = folderVideos.count { video ->
            val state = recentlyPlayedList.find { it.mediaTitle == video.displayName }
            // Video is unplayed if no playback state exists, or if it hasn't been finished (e.g. > 10s remaining)
            state == null || state.timeRemaining > 10
          }
          FolderWithNewCount(folder, unplayedCount)
        }
        _foldersWithNewCount.value = foldersWithCount
      } catch (e: Exception) {
        Log.e("FolderListViewModel", "Error calculating new video counts", e)
      }
    }
  }

  /**
   * Delete folders and update state
   */
  fun deleteFolders(foldersToDelete: List<VideoFolder>) {
    viewModelScope.launch(Dispatchers.IO) {
      // Logic to delete folders from filesystem
      // ... actual deletion logic would go here
      
      // Update local state
      val currentFolders = _allVideoFolders.value.toMutableList()
      foldersToDelete.forEach { folder ->
        currentFolders.removeAll { it.path == folder.path }
      }
      _allVideoFolders.value = currentFolders
      
      // Notify library changes
      MediaLibraryEvents.notifyChanged()
    }
  }
}
