package app.marlboroadvance.mpvex.ui.browser.base

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.preferences.UiPreferences
import app.marlboroadvance.mpvex.preferences.UiSettings
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils.StorageOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base ViewModel for browser screens with shared functionality
 * handles common UI states and data management.
 * 
 * @param T The type of items displayed in the list
 */
abstract class BaseBrowserViewModel<T>(
  application: Application,
) : AndroidViewModel(application),
  KoinComponent {
  
  protected val metadataCache: VideoMetadataCacheRepository by inject()
  protected val uiPreferences: UiPreferences by inject()
  
  // Common UI States
  val uiSettings: StateFlow<UiSettings> = uiPreferences.observeUiSettings()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), uiPreferences.getUiSettings())

  protected val _items = MutableStateFlow<List<T>>(emptyList())
  val items: StateFlow<List<T>> = _items.asStateFlow()

  protected val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  /**
   * Observable recently played file path for highlighting
   */
  val recentlyPlayedFilePath: StateFlow<String?> =
    RecentlyPlayedOps
      .observeLastPlayedPath()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  /**
   * Abstract load method to be implemented by subclasses
   */
  abstract fun loadData()

  /**
   * Common hard refresh logic:
   * 1. Clear Cache
   * 2. Set Loading State
   * 3. Trigger Scan
   * 4. Reload after delay
   */
  open fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      
      // Clear core media scanner cache
      MediaFileRepository.clearCache()
      
      // Delay to allow filesystem/MediaStore sync if needed
      delay(500)
      
      loadData()
    }
  }

  /**
   * Delete videos from storage
   * Automatically removes from recently played history and invalidates cache
   *
   * @return Pair of (deletedCount, failedCount)
   */
  open suspend fun deleteVideos(videos: List<Video>): Pair<Int, Int> {
    val result = StorageOps.deleteVideos(getApplication(), videos)

    // Invalidate cache for deleted videos
    val paths = videos.map { it.path }
    metadataCache.invalidateVideos(paths)

    return result
  }

  /**
   * Rename a video
   * Automatically updates recently played history and invalidates old cache entry
   */
  open suspend fun renameVideo(
    video: Video,
    newDisplayName: String,
  ): Result<Unit> {
    val oldPath = video.path
    val result = StorageOps.renameVideo(getApplication(), video, newDisplayName)

    // Invalidate cache for old path
    result.onSuccess {
      metadataCache.invalidateVideo(oldPath)
    }

    return result
  }
}
