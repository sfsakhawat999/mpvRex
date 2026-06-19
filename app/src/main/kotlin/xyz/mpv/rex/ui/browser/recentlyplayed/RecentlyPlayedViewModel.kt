package xyz.mpv.rex.ui.browser.recentlyplayed

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.mpv.rex.database.MpvExDatabase
import xyz.mpv.rex.database.entities.RecentlyPlayedEntity
import xyz.mpv.rex.database.repository.PlaylistRepository
import xyz.mpv.rex.database.repository.VideoMetadataCacheRepository
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import xyz.mpv.rex.ui.browser.base.BaseBrowserViewModel
import xyz.mpv.rex.utils.media.MediaFormatter
import xyz.mpv.rex.utils.permission.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class RecentlyPlayedViewModel(application: Application) : 
  BaseBrowserViewModel<RecentlyPlayedItem>(application), KoinComponent {
  
  private val recentlyPlayedRepository by inject<RecentlyPlayedRepository>()
  private val playlistRepository by inject<PlaylistRepository>()

  val recentItems: StateFlow<List<RecentlyPlayedItem>> = items

  // Keep for backward compatibility
  private val _recentVideos = MutableStateFlow<List<Video>>(emptyList())
  val recentVideos: StateFlow<List<Video>> = _recentVideos.asStateFlow()

  init {
    loadData()
    
    // Observe changes and update automatically
    viewModelScope.launch {
      val db = org.koin.core.context.GlobalContext.get().get<MpvExDatabase>()
      combine(
        db.recentlyPlayedDao().observeRecentlyPlayed(),
        db.recentlyPlayedDao().observeRecentlyPlayedPlaylists()
      ) { _, _ ->
        // Trigger reload when either database or playlists change
        loadData()
      }.collectLatest { }
    }
  }

  override fun loadData() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val entities = recentlyPlayedRepository.getRecentlyPlayed(limit = 50)
        val playbackStates = playbackStateRepository.getAllPlaybackStates()
        
        val db = org.koin.core.context.GlobalContext.get().get<MpvExDatabase>()
        val playlistInfos = db.recentlyPlayedDao().getRecentlyPlayedPlaylists(limit = 20)
        
        val appearancePreferences by inject<xyz.mpv.rex.preferences.AppearancePreferences>()
        val browserPreferences by inject<xyz.mpv.rex.preferences.BrowserPreferences>()
        val watchedThreshold = browserPreferences.watchedThreshold.get()

        val videoItems = entities.mapNotNull { entity ->
          try {
            var video = createVideoFromEntity(entity)
            if (video != null) {
              // Map saved orientation and progress info if available
              val state = playbackStates.find { it.mediaTitle == video.displayName }
              
              var progress: Float? = null
              var isWatched = false
              
              if (state != null) {
                if (state.savedOrientation != null) {
                  video = video.copy(savedOrientation = state.savedOrientation)
                }
                
                if (video.duration > 0) {
                  val durationSeconds = video.duration / 1000
                  val watched = durationSeconds - state.timeRemaining.toLong()
                  val progressValue = if (durationSeconds > 0) {
                    (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                  } else 0f
                  
                  isWatched = state.hasBeenWatched || (state.timeRemaining > 0 && progressValue >= (watchedThreshold / 100f))
                  if (progressValue in 0.01f..0.99f && !isWatched) {
                    progress = progressValue
                  }
                }
              }
              
              RecentlyPlayedItem.VideoItem(
                video = video,
                timestamp = entity.timestamp,
                progress = progress,
                isWatched = isWatched
              )
            } else null
          } catch (e: Exception) { null }
        }
        
        val playlistItems = playlistInfos.mapNotNull { info ->
          val playlist = playlistRepository.getPlaylistById(info.playlistId)
          if (playlist != null) {
            RecentlyPlayedItem.PlaylistItem(
              playlist = playlist,
              videoCount = 0, 
              mostRecentVideoPath = "",
              timestamp = info.timestamp
            )
          } else null
        }
        
        val allItems = (videoItems + playlistItems).sortedByDescending { it.timestamp }
        _items.value = allItems
        _recentVideos.value = videoItems.map { it.video }
      } catch (e: Exception) {
        Log.e("RecentlyPlayedViewModel", "Error loading recent items", e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  override fun refresh(silent: Boolean) {
    loadData()
  }

  private fun createVideoFromEntity(entity: RecentlyPlayedEntity?): Video? {
    if (entity == null) return null
    val filePath = entity.filePath
    val file = File(filePath)
    
    val videoTitle = if (entity.videoTitle?.isNotBlank() == true) entity.videoTitle else file.nameWithoutExtension
    val displayName = if (entity.fileName.isNotBlank()) entity.fileName else file.name
    
    val duration = if (entity.duration > 0) entity.duration else 0L
    val size = if (entity.fileSize > 0) entity.fileSize else file.length()
    val dateModified = file.lastModified() / 1000
    val dateAdded = dateModified
    val bucketId = file.parent ?: ""
    val bucketDisplayName = File(bucketId).name

    // Force isAudio based on extension even if database says otherwise
    val isAudio = entity.isAudio || xyz.mpv.rex.utils.storage.FileTypeUtils.isAudioFile(file)

    return Video(
      id = filePath.hashCode().toLong(),
      title = videoTitle,
      displayName = displayName,
      path = filePath,
      uri = if (filePath.startsWith("/") || filePath.startsWith("file://")) {
        val path = if (filePath.startsWith("file://")) filePath.removePrefix("file://") else filePath
        Uri.fromFile(File(path))
      } else {
        Uri.parse(filePath)
      },
      duration = duration,
      durationFormatted = MediaFormatter.formatDuration(duration),
      size = size,
      sizeFormatted = MediaFormatter.formatFileSize(size),
      dateModified = dateModified,
      dateAdded = dateAdded,
      mimeType = if (isAudio) "audio/*" else "video/*",
      bucketId = bucketId,
      bucketDisplayName = bucketDisplayName,
      width = if (isAudio) 0 else entity.width,
      height = if (isAudio) 0 else entity.height,
      fps = 0f, 
      resolution = if (isAudio) "" else MediaFormatter.formatResolution(entity.width, entity.height),
      isAudio = isAudio,
      artist = entity.artist,
      album = entity.album,
    )
    }

  suspend fun deleteRecentItems(itemsToDelete: List<RecentlyPlayedItem>): Pair<Int, Int> {
    return try {
      val videoPaths = itemsToDelete.filterIsInstance<RecentlyPlayedItem.VideoItem>().map { it.video.path }
      val playlistIds = itemsToDelete.filterIsInstance<RecentlyPlayedItem.PlaylistItem>().map { it.playlist.id }
      
      var deletedCount = 0
      if (videoPaths.isNotEmpty()) {
        videoPaths.forEach { path ->
          recentlyPlayedRepository.deleteByFilePath(path)
          deletedCount++
        }
      }
      if (playlistIds.isNotEmpty()) {
        playlistIds.forEach { id ->
          recentlyPlayedRepository.deleteByPlaylistId(id)
          deletedCount++
        }
      }
      
      loadData()
      Pair(deletedCount, 0)
    } catch (e: Exception) {
      Log.e("RecentlyPlayedViewModel", "Error deleting items from history", e)
      Pair(0, itemsToDelete.size)
    }
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        RecentlyPlayedViewModel(application)
      }
    }
  }
}
