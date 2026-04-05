package app.marlboroadvance.mpvex.ui.browser.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.entities.PlaylistEntity
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
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

data class PlaylistWithCount(
  val playlist: PlaylistEntity,
  val itemCount: Int,
)

class PlaylistViewModel(
  application: Application,
) : BaseBrowserViewModel<PlaylistWithCount>(application),
  KoinComponent {
  private val repository: PlaylistRepository by inject()

  val playlistsWithCount: StateFlow<List<PlaylistWithCount>> = items

  // Track if initial load has completed to prevent empty state flicker
  private val _hasCompletedInitialLoad = MutableStateFlow(false)
  val hasCompletedInitialLoad: StateFlow<Boolean> = _hasCompletedInitialLoad.asStateFlow()

  companion object {
    private const val TAG = "PlaylistViewModel"

    fun factory(application: Application) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaylistViewModel(application) as T
      }
  }

  init {
    loadData()

    // Observe all playlists and update items
    viewModelScope.launch(Dispatchers.IO) {
      repository.observeAllPlaylists().collectLatest { playlists ->
        loadData()
      }
    }
  }

  override fun loadData() {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      try {
        val playlists = repository.getAllPlaylists()
        val playlistsWithCounts = playlists.map { playlist ->
          val count = repository.getPlaylistItemCount(playlist.id)
          PlaylistWithCount(playlist, count)
        }.sortedByDescending { it.playlist.updatedAt }

        _items.value = playlistsWithCounts
        _hasCompletedInitialLoad.value = true
      } finally {
        _isLoading.value = false
      }
    }
  }

  override fun refresh() {
    loadData()
  }

  suspend fun createPlaylist(name: String): Long {
    return repository.createPlaylist(name)
  }

  suspend fun deletePlaylists(playlistsToDelete: List<PlaylistWithCount>) {
    playlistsToDelete.forEach {
      repository.deletePlaylist(it.playlist)
    }
    loadData()
  }
}
