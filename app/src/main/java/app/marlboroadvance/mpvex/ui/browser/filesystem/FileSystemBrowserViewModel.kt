package app.marlboroadvance.mpvex.ui.browser.filesystem

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.browser.PathComponent
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.media.MetadataRetrieval
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import app.marlboroadvance.mpvex.utils.storage.FileTypeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * ViewModel for FileSystem Browser - based on Fossify's ItemsFragment logic
 * Handles directory navigation, file loading, sorting, and state management
 */
class FileSystemBrowserViewModel(
  application: Application,
  initialPath: String? = null,
) : BaseBrowserViewModel<FileSystemItem>(application),
  KoinComponent {
  private val playbackStateRepository: PlaybackStateRepository by inject()
  private val browserPreferences: BrowserPreferences by inject()
  private val appearancePreferences: app.marlboroadvance.mpvex.preferences.AppearancePreferences by inject()

  // Special marker for "show storage volumes" mode
  private val STORAGE_ROOTS_MARKER = "__STORAGE_ROOTS__"

  private var homeDirectory: String? = null

  private val _currentPath = MutableStateFlow(initialPath ?: STORAGE_ROOTS_MARKER)
  val currentPath: StateFlow<String> = _currentPath.asStateFlow()

  private val _unsortedItems = MutableStateFlow<List<FileSystemItem>>(emptyList())

  // Video playback progress map
  private val _videoFilesWithPlayback = MutableStateFlow<Map<Long, Float>>(emptyMap())
  val videoFilesWithPlayback: StateFlow<Map<Long, Float>> = _videoFilesWithPlayback.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _breadcrumbs = MutableStateFlow<List<PathComponent>>(emptyList())
  val breadcrumbs: StateFlow<List<PathComponent>> = _breadcrumbs.asStateFlow()

  val isAtRoot: StateFlow<Boolean> =
    MutableStateFlow(initialPath == null).apply {
      viewModelScope.launch {
        _currentPath.collect { path ->
          value = path == STORAGE_ROOTS_MARKER || path == homeDirectory
        }
      }
    }

  private val _itemsWereDeletedOrMoved = MutableStateFlow(false)
  val itemsWereDeletedOrMoved: StateFlow<Boolean> = _itemsWereDeletedOrMoved.asStateFlow()

  private val itemCountByPath = mutableMapOf<String, Int>()

  companion object {
    private const val TAG = "FileSystemBrowserVM"

    fun factory(
      application: Application,
      initialPath: String? = null,
    ) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FileSystemBrowserViewModel(application, initialPath) as T
    }
  }

  init {
    if (initialPath == null) {
      viewModelScope.launch(Dispatchers.IO) {
        val roots = MediaFileRepository.getStorageRoots(getApplication())
        if (roots.size == 1) {
          val singleRoot = roots.first()
          homeDirectory = singleRoot.path
          _currentPath.value = singleRoot.path
        } else {
          homeDirectory = null
        }
        loadData()
      }
    } else {
      homeDirectory = initialPath
      loadData()
    }

    viewModelScope.launch(Dispatchers.IO) {
      MediaLibraryEvents.changes.collectLatest {
        MediaFileRepository.clearCache()
        loadData()
      }
    }

    viewModelScope.launch {
      combine(
        _unsortedItems,
        browserPreferences.folderSortType.changes(),
        browserPreferences.folderSortOrder.changes(),
      ) { items, sortType, sortOrder ->
        SortUtils.sortFileSystemItems(items, sortType, sortOrder)
      }.collectLatest { sortedItems ->
        _items.value = sortedItems
      }
    }
  }

  override fun loadData() {
    loadCurrentDirectory()
  }

  override fun refresh() {
    Log.d(TAG, "Hard refreshing current directory: ${_currentPath.value}")
    
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      MediaFileRepository.clearCache()
      triggerMediaScan()
      delay(800)
      loadData()
    }
  }
  
  private fun triggerMediaScan() {
    try {
      val path = _currentPath.value
      if (path == STORAGE_ROOTS_MARKER) return
      
      val folder = File(path)
      if (folder.exists() && folder.isDirectory) {
        val mediaFiles = folder.listFiles { file ->
          file.isFile && (
            FileTypeUtils.isVideoFile(file) || 
            (browserPreferences.showAudioFiles.get() && FileTypeUtils.isAudioFile(file))
          )
        }
        
        if (!mediaFiles.isNullOrEmpty()) {
          val filePaths = mediaFiles.map { it.absolutePath }.toTypedArray()
          android.media.MediaScannerConnection.scanFile(getApplication(), filePaths, null) { _, _ -> }
        }
      } else {
        val externalStorage = android.os.Environment.getExternalStorageDirectory()
        android.media.MediaScannerConnection.scanFile(getApplication(), arrayOf(externalStorage.absolutePath), null) { _, _ -> }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to trigger media scan", e)
    }
  }

  fun setItemsWereDeletedOrMoved() {
    _itemsWereDeletedOrMoved.value = true
  }

  fun deleteFolders(folders: List<FileSystemItem.Folder>): Pair<Int, Int> {
    var successCount = 0
    var failureCount = 0
    folders.forEach { folder ->
      try {
        val dir = File(folder.path)
        if (dir.exists() && dir.deleteRecursively()) {
          successCount++
        } else {
          failureCount++
        }
      } catch (e: Exception) {
        failureCount++
      }
    }
    if (successCount > 0) {
      _itemsWereDeletedOrMoved.value = true
      MediaLibraryEvents.notifyChanged()
    }
    return Pair(successCount, failureCount)
  }

  override suspend fun deleteVideos(videos: List<Video>): Pair<Int, Int> {
    val result = super.deleteVideos(videos)
    if (result.first > 0) {
      _itemsWereDeletedOrMoved.value = true
    }
    return result
  }

  override suspend fun renameVideo(video: Video, newDisplayName: String): Result<Unit> {
    return super.renameVideo(video, newDisplayName)
  }

  private fun loadCurrentDirectory() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      _error.value = null

      try {
        val path = _currentPath.value
        if (path == STORAGE_ROOTS_MARKER) {
          _breadcrumbs.value = emptyList()
          val roots = MediaFileRepository.getStorageRoots(getApplication())
          _unsortedItems.value = roots
        } else {
          _breadcrumbs.value = MediaFileRepository.getPathComponents(path)
          MediaFileRepository
            .scanDirectory(getApplication(), path, showAllFileTypes = false)
            .onSuccess { items ->
              val previousCount = itemCountByPath[path] ?: 0
              if (previousCount > 0 && items.isEmpty()) {
                _itemsWereDeletedOrMoved.value = true
              } else if (items.isNotEmpty()) {
                _itemsWereDeletedOrMoved.value = false
              }
              itemCountByPath[path] = items.size

              val filteredItems = if (!browserPreferences.showAudioFiles.get()) {
                items.filterNot { it is FileSystemItem.VideoFile && it.video.isAudio }
              } else {
                items
              }

              val enrichedItems = if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences)) {
                val videoFiles = filteredItems.filterIsInstance<FileSystemItem.VideoFile>()
                val videos = videoFiles.map { it.video }
                val enrichedVideos = MetadataRetrieval.enrichVideosIfNeeded(
                  context = getApplication(),
                  videos = videos,
                  browserPreferences = browserPreferences,
                  metadataCache = metadataCache
                )
                
                val enrichedVideoMap = enrichedVideos.associateBy { it.id }
                filteredItems.map { item ->
                  when (item) {
                    is FileSystemItem.VideoFile -> {
                      val enrichedVideo = enrichedVideoMap[item.video.id]
                      if (enrichedVideo != null) item.copy(video = enrichedVideo) else item
                    }
                    else -> item
                  }
                }
              } else {
                filteredItems
              }

              _unsortedItems.value = enrichedItems
              loadPlaybackInfo(enrichedItems)
            }.onFailure { error ->
              _error.value = error.message
              _unsortedItems.value = emptyList()
            }
        }
      } catch (e: Exception) {
        _error.value = e.message
        _unsortedItems.value = emptyList()
      } finally {
        _isLoading.value = false
      }
    }
  }

  private fun loadPlaybackInfo(items: List<FileSystemItem>) {
    viewModelScope.launch(Dispatchers.IO) {
      val videoFiles = items.filterIsInstance<FileSystemItem.VideoFile>()
      val playbackMap = mutableMapOf<Long, Float>()
      videoFiles.forEach { videoFile ->
        val video = videoFile.video
        val playbackState = playbackStateRepository.getVideoDataByTitle(video.displayName)
        if (playbackState != null && video.duration > 0) {
          val durationSeconds = video.duration / 1000
          val watched = durationSeconds - playbackState.timeRemaining.toLong()
          val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
          if (progressValue in 0.01f..0.99f) {
            playbackMap[video.id] = progressValue
          }
        }
      }
      _videoFilesWithPlayback.value = playbackMap
    }
  }
}
