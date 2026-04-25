package app.marlboroadvance.mpvex.ui.browser.shorts

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.database.dao.ShortsMediaDao
import app.marlboroadvance.mpvex.database.entities.ShortsMediaEntity
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.utils.media.ShortsDiscoveryOps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShortsViewModel(
    application: Application
) : AndroidViewModel(application), KoinComponent {

    private val shortsMediaDao: ShortsMediaDao by inject()
    private val browserPreferences: BrowserPreferences by inject()
    private val metadataCache: VideoMetadataCacheRepository by inject()
    private val thumbnailRepository: ThumbnailRepository by inject()

    private val _shorts = MutableStateFlow<List<Video>>(emptyList())
    val shorts: StateFlow<List<Video>> = _shorts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val lovedPaths: StateFlow<Set<String>> = shortsMediaDao.observeAllShortsMedia()
        .map { list -> list.filter { it.isLoved }.map { it.path }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun loadShorts() {
        viewModelScope.launch {
            _isLoading.value = true
            val discoveredShorts = ShortsDiscoveryOps.discoverShorts(
                getApplication(),
                shortsMediaDao,
                metadataCache,
                browserPreferences
            )
            _shorts.value = discoveredShorts
            _isLoading.value = false
        }
    }

    suspend fun getThumbnail(video: Video): Bitmap? {
        // High quality thumbnails for full screen
        return thumbnailRepository.getThumbnail(video, 1080, 1920)
    }

    fun shuffleShorts() {
        _shorts.value = _shorts.value.shuffled()
    }

    fun toggleLove(video: Video) {
        viewModelScope.launch {
            val current = shortsMediaDao.getShortsMediaByPath(video.path)
            val isLoved = current?.isLoved ?: false
            val newEntity = current?.copy(isLoved = !isLoved) 
                ?: ShortsMediaEntity(path = video.path, isLoved = true)
            shortsMediaDao.upsert(newEntity)
        }
    }

    fun blockVideo(video: Video) {
        viewModelScope.launch {
            val current = shortsMediaDao.getShortsMediaByPath(video.path)
            val newEntity = current?.copy(isBlocked = true)
                ?: ShortsMediaEntity(path = video.path, isBlocked = true)
            shortsMediaDao.upsert(newEntity)
            
            // Remove from current list
            _shorts.value = _shorts.value.filter { it.path != video.path }
        }
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShortsViewModel(application) as T
        }
    }
}
