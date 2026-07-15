package xyz.mpv.rex.ui.browser.medialibrary

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.repository.MediaFileRepository
import xyz.mpv.rex.ui.browser.base.BaseBrowserViewModel
import xyz.mpv.rex.ui.browser.videolist.VideoWithPlaybackInfo
import xyz.mpv.rex.utils.media.MetadataRetrieval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import androidx.compose.runtime.Immutable

class MediaLibraryViewModel(
  application: Application,
) : BaseBrowserViewModel<VideoWithPlaybackInfo>(application),
  KoinComponent {
  private val appearancePreferences: xyz.mpv.rex.preferences.AppearancePreferences by inject()
  private val browserPreferences: xyz.mpv.rex.preferences.BrowserPreferences by inject()
  private val recentlyPlayedRepository: xyz.mpv.rex.domain.recentlyplayed.repository.RecentlyPlayedRepository by inject()

  private val _videos = MutableStateFlow<List<Video>>(emptyList())
  val videos: StateFlow<List<Video>> = _videos.asStateFlow()

  private val _videosWithPlaybackInfo = MutableStateFlow<List<VideoWithPlaybackInfo>>(emptyList())
  val videosWithPlaybackInfo: StateFlow<List<VideoWithPlaybackInfo>> = _videosWithPlaybackInfo.asStateFlow()

  private val tag = "MediaLibraryViewModel"

  init {
    loadData()
  }

  override fun loadData() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        _isLoading.value = true
        // Fetch all videos from repository (respecting blacklists and filters via MediaMetadataOps)
        var videoList = MediaFileRepository.getAllVideos(getApplication())
        
        // Filter out audio if disabled
        if (!browserPreferences.showAudioFiles.get()) {
          videoList = videoList.filterNot { it.isAudio }
        }

        _videos.value = videoList
        loadPlaybackInfo(videoList)
        _isLoading.value = false

        // Enrich with metadata in the background only if chips are enabled
        if (MetadataRetrieval.isVideoMetadataNeeded(browserPreferences) && videoList.isNotEmpty()) {
          val enrichedList = MetadataRetrieval.enrichVideosIfNeeded(
            context = getApplication(),
            videos = videoList,
            browserPreferences = browserPreferences,
            metadataCache = metadataCache
          )
          _videos.value = enrichedList
          loadPlaybackInfo(enrichedList)
        }
      } catch (e: Exception) {
        Log.e(tag, "Error loading media library videos", e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  override fun refresh(silent: Boolean) {
    loadData()
  }

  private suspend fun loadPlaybackInfo(videos: List<Video>) {
    val playbackStates = playbackStateRepository.getAllPlaybackStates()
    val currentTime = System.currentTimeMillis()
    val thresholdDays = appearancePreferences.unplayedOldVideoDays.get()
    val thresholdMillis = thresholdDays * 24 * 60 * 60 * 1000L
    val watchedThreshold = browserPreferences.watchedThreshold.get()

    val videosWithInfo =
      videos.map { video ->
        val playbackState = playbackStates.find { it.mediaTitle == video.displayName }

        // Map saved orientation to video
        val videoWithOrientation = if (playbackState?.savedOrientation != null) {
          video.copy(savedOrientation = playbackState.savedOrientation)
        } else {
          video
        }

        // Calculate watch progress (0.0 to 1.0)
        val progress = if (playbackState != null && video.duration > 0 && playbackState.timeRemaining != -1) {
          val durationSeconds = video.duration / 1000
          val watched = durationSeconds - playbackState.timeRemaining.toLong()
          val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
          if (progressValue in 0.01f..0.99f) progressValue else null
        } else {
          null
        }

        // Correct logic for "NEW" label
        val videoAge = currentTime - (video.dateModified * 1000)
        val isOldAndUnplayed = (playbackState == null && videoAge <= thresholdMillis) || (playbackState != null && playbackState.timeRemaining == -1)

        val isWatched = if (playbackState != null) {
          if (playbackState.hasBeenWatched) {
            true
          } else if (video.duration > 0 && playbackState.timeRemaining != -1) {
            val durationSeconds = video.duration / 1000
            val watched = durationSeconds - playbackState.timeRemaining.toLong()
            val progressValue = (watched.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
            progressValue >= (watchedThreshold / 100f)
          } else {
            false
          }
        } else {
          false
        }

        VideoWithPlaybackInfo(
          video = videoWithOrientation,
          timeRemaining = playbackState?.timeRemaining?.toLong(),
          progressPercentage = progress,
          isOldAndUnplayed = isOldAndUnplayed,
          isWatched = isWatched,
          isNeverPlayed = playbackState == null,
        )
      }
    _videosWithPlaybackInfo.value = videosWithInfo
    _items.value = videosWithInfo
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return MediaLibraryViewModel(application) as T
        }
      }
  }
}
