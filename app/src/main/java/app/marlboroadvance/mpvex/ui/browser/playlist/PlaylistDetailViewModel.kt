package app.marlboroadvance.mpvex.ui.browser.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.entities.PlaylistEntity
import app.marlboroadvance.mpvex.database.entities.PlaylistItemEntity
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.ui.browser.base.BaseBrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

data class PlaylistVideoItem(
  val playlistItem: PlaylistItemEntity,
  val video: Video,
)

class PlaylistDetailViewModel(
  application: Application,
  private val playlistId: Int,
) : BaseBrowserViewModel<PlaylistVideoItem>(application),
  KoinComponent {
  private val playlistRepository: PlaylistRepository by inject()

  private val _playlist = MutableStateFlow<PlaylistEntity?>(null)
  val playlist: StateFlow<PlaylistEntity?> = _playlist.asStateFlow()

  // Re-add for screen compatibility
  val videoItems: StateFlow<List<PlaylistVideoItem>> = items

  companion object {
    private const val TAG = "PlaylistDetailViewModel"

    fun factory(
      application: Application,
      playlistId: Int,
    ) = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlaylistDetailViewModel(application, playlistId) as T
    }
  }

  init {
    // Observe playlist info
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.observePlaylistById(playlistId).collectLatest { playlist ->
        _playlist.value = playlist
      }
    }

    // Observe playlist items and load video metadata
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.observePlaylistItems(playlistId).collectLatest { items ->
        loadData()
      }
    }
  }

  override fun loadData() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      try {
        val itemsList = playlistRepository.getPlaylistItems(playlistId)
        val playbackStates = playbackStateRepository.getAllPlaybackStates()
        
        if (itemsList.isEmpty()) {
          _items.value = emptyList()
        } else {
          val playlist = _playlist.value
          val isM3uPlaylist = playlist?.isM3uPlaylist == true

          if (isM3uPlaylist) {
            val videoItemsList = itemsList.mapNotNull { item ->
              try {
                var video = Video(
                  id = item.id.toLong(),
                  title = item.fileName,
                  displayName = item.fileName,
                  path = item.filePath,
                  uri = if (item.filePath.startsWith("/") || item.filePath.startsWith("file://")) {
                    val path = if (item.filePath.startsWith("file://")) item.filePath.removePrefix("file://") else item.filePath
                    android.net.Uri.fromFile(java.io.File(path))
                  } else {
                    android.net.Uri.parse(item.filePath)
                  },
                  duration = 0L,
                  durationFormatted = "00:00",
                  size = 0L,
                  sizeFormatted = "0 B",
                  dateModified = item.addedAt,
                  dateAdded = item.addedAt,
                  mimeType = "video/*",
                  bucketId = "m3u_playlist_$playlistId",
                  bucketDisplayName = playlist?.name ?: "M3U Playlist",
                  width = 0,
                  height = 0,
                  fps = 0f,
                  resolution = "Unknown"
                )
                
                // Map saved orientation
                val state = playbackStates.find { it.mediaTitle == video.displayName }
                if (state?.savedOrientation != null) {
                  video = video.copy(savedOrientation = state.savedOrientation)
                }
                
                PlaylistVideoItem(item, video)
              } catch (e: Exception) {
                null
              }
            }
            _items.value = videoItemsList
          } else {
            val bucketIds = itemsList.map { item -> File(item.filePath).parent ?: "" }.toSet()
            val allVideos = MediaFileRepository.getVideosForBuckets(getApplication(), bucketIds)
            val videoItemsList = itemsList.mapNotNull { item ->
              var matchedVideo = allVideos.find { video -> video.path == item.filePath }
              if (matchedVideo != null) {
                // Map saved orientation
                val state = playbackStates.find { it.mediaTitle == matchedVideo.displayName }
                if (state?.savedOrientation != null) {
                  matchedVideo = matchedVideo.copy(savedOrientation = state.savedOrientation)
                }
                PlaylistVideoItem(item, matchedVideo)
              } else null
            }
            _items.value = videoItemsList
          }
        }
      } finally {
        _isLoading.value = false
      }
    }
  }

  override fun refresh(silent: Boolean) {
    loadData()
  }

  // Compatibility method for Screen
  suspend fun refreshNow() {
    loadData()
  }

  suspend fun updatePlaylistName(newName: String) {
    _playlist.value?.let { playlist ->
      playlistRepository.updatePlaylist(playlist.copy(name = newName))
    }
  }

  suspend fun removeVideoFromPlaylist(item: PlaylistVideoItem) {
    playlistRepository.removeItemFromPlaylist(item.playlistItem)
  }

  suspend fun removeVideosFromPlaylist(items: List<PlaylistVideoItem>) {
    playlistRepository.removeItemsFromPlaylist(items.map { it.playlistItem })
  }

  suspend fun updatePlayHistory(filePath: String, position: Long = 0) {
    playlistRepository.updatePlayHistory(playlistId, filePath, position)
  }

  suspend fun reorderPlaylistItems(fromIndex: Int, toIndex: Int) {
    val currentItems = _items.value.toMutableList()
    if (fromIndex < 0 || fromIndex >= currentItems.size || toIndex < 0 || toIndex >= currentItems.size) {
      return
    }

    val item = currentItems.removeAt(fromIndex)
    currentItems.add(toIndex, item)
    _items.value = currentItems

    val newOrder = currentItems.map { it.playlistItem.id }
    playlistRepository.reorderPlaylistItems(playlistId, newOrder)
  }

  suspend fun refreshM3UPlaylist(): Result<Unit> {
    return try {
      _isLoading.value = true
      playlistRepository.refreshM3UPlaylist(playlistId)
    } finally {
      _isLoading.value = false
    }
  }
}
