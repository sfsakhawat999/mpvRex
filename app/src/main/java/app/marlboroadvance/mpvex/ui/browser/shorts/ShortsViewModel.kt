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
import `is`.xyz.mpv.MPVLib
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

    private val _isExhausted = MutableStateFlow(false)
    val isExhausted: StateFlow<Boolean> = _isExhausted.asStateFlow()

    private val _totalShortsCount = MutableStateFlow(0)
    val totalShortsCount: StateFlow<Int> = _totalShortsCount.asStateFlow()

    val lovedPaths: StateFlow<Set<String>> = shortsMediaDao.observeAllShortsMedia()
        .map { list -> list.filter { it.isLoved }.map { it.path }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val blockedPaths: StateFlow<Set<String>> = shortsMediaDao.observeAllShortsMedia()
        .map { list -> list.filter { it.isBlocked }.map { it.path }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val isShuffleEnabled: StateFlow<Boolean> = browserPreferences.persistentShuffle.changes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), browserPreferences.persistentShuffle.get())

    private val _currentSpeed = MutableStateFlow(1.0)
    val currentSpeed: StateFlow<Double> = _currentSpeed.asStateFlow()
    
    private val seenPaths = mutableSetOf<String>()

    fun loadShorts(initialVideoPath: String? = null, blockedOnly: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val finalShorts = if (blockedOnly) {
                val blockedInDb = shortsMediaDao.getAllShortsMedia().filter { it.isBlocked }
                val flatFolders = app.marlboroadvance.mpvex.utils.storage.CoreMediaScanner.getFlatMediaFolders(getApplication())
                val allVideos = flatFolders.flatMap { folder ->
                    app.marlboroadvance.mpvex.utils.storage.VideoScanUtils.getVideosInFolder(getApplication(), folder.path)
                }.filter { !it.isAudio }
                
                val blockedPathsSet = blockedInDb.map { it.path }.toSet()
                allVideos.filter { it.path in blockedPathsSet }
            } else {
                ShortsDiscoveryOps.discoverShorts(
                    getApplication(),
                    shortsMediaDao,
                    metadataCache,
                    browserPreferences
                )
            }

            _totalShortsCount.value = finalShorts.size

            val interleavedShorts = if (blockedOnly) {
                finalShorts
            } else {
                // Partition into Loved and Others
                val loved = finalShorts.filter { lovedPaths.value.contains(it.path) }.shuffled().toMutableList()
                val others = finalShorts.filter { !lovedPaths.value.contains(it.path) }.shuffled().toMutableList()
                
                // Apply Claude's Elastic Interleaving Algorithm
                buildElasticFeed(loved, others)
            }
            
            // Apply Strict Session Filtering (No repeats)
            // But we keep the initialVideoPath if specified, even if seen.
            val filteredShorts = interleavedShorts.filter { it.path !in seenPaths || it.path == initialVideoPath }
            
            if (finalShorts.isNotEmpty() && filteredShorts.isEmpty()) {
                _isExhausted.value = true
            } else {
                _isExhausted.value = false
            }

            // Move initial video to the front
            val orderedShorts = if (initialVideoPath != null) {
                val initial = filteredShorts.find { it.path == initialVideoPath }
                if (initial != null) {
                    listOf(initial) + filteredShorts.filter { it.path != initialVideoPath }
                } else {
                    filteredShorts
                }
            } else {
                filteredShorts
            }
            
            _shorts.value = orderedShorts
            _isLoading.value = false
        }
    }

    private fun buildElasticFeed(
        loved: MutableList<Video>,
        others: MutableList<Video>,
        targetOthersPerLoved: Int = 2
    ): List<Video> {
        if (loved.isEmpty()) return others.shuffled()
        if (others.isEmpty()) return loved.shuffled()

        val interleaved = mutableListOf<Video>()
        
        // Calculate dynamic ratio based on pool sizes
        val effectiveRatio = if (others.size >= loved.size * targetOthersPerLoved) {
            targetOthersPerLoved
        } else {
            // Gracefully degrade to 1:1 if we don't have enough "others"
            maxOf(1, others.size / loved.size)
        }

        while (loved.isNotEmpty() || others.isNotEmpty()) {
            val othersToTake = minOf(effectiveRatio, others.size)
            repeat(othersToTake) { interleaved.add(others.removeAt(0)) }

            if (loved.isNotEmpty()) interleaved.add(loved.removeAt(0))

            // If others are exhausted, dump the rest of the loved videos
            if (others.isEmpty() && loved.isNotEmpty()) {
                interleaved.addAll(loved.shuffled())
                loved.clear()
            }
        }
        return interleaved
    }
    
    fun markAsSeen(video: Video) {
        seenPaths.add(video.path)
        if (seenPaths.size >= _totalShortsCount.value && _totalShortsCount.value > 0) {
            _isExhausted.value = true
        }
    }
    
    fun clearSessionHistory() {
        seenPaths.clear()
        _isExhausted.value = false
    }

    suspend fun getThumbnail(video: Video): Bitmap? {
        return thumbnailRepository.getThumbnail(video, 1080, 1920)
    }

    fun syncPlaybackSpeed() {
        MPVLib.setPropertyDouble("speed", _currentSpeed.value)
    }

    fun setPlaybackSpeed(speed: Double) {
        MPVLib.setPropertyDouble("speed", speed)
        _currentSpeed.value = speed
    }

    fun cycleSpeed() {
        val speeds = listOf(1.0, 1.5, 2.0, 0.5)
        val current = _currentSpeed.value
        val nextIndex = (speeds.indexOf(current) + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }

    fun toggleShuffle(currentIndex: Int) {
        val newState = !browserPreferences.persistentShuffle.get()
        browserPreferences.persistentShuffle.set(newState)
        
        if (newState) {
            shuffleShorts(currentIndex)
        }
    }

    fun shuffleShorts(currentIndex: Int) {
        val currentList = _shorts.value
        if (currentList.isEmpty()) return
        
        val currentVideo = currentList.getOrNull(currentIndex) ?: return
        
        val mutableList = currentList.toMutableList()
        mutableList.removeAt(currentIndex)
        mutableList.shuffle()
        mutableList.add(currentIndex, currentVideo)
        
        _shorts.value = mutableList
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

    fun toggleBlock(video: Video) {
        viewModelScope.launch {
            val current = shortsMediaDao.getShortsMediaByPath(video.path)
            val isBlocked = current?.isBlocked ?: false
            val newEntity = current?.copy(isBlocked = !isBlocked)
                ?: ShortsMediaEntity(path = video.path, isBlocked = true, addedDate = System.currentTimeMillis())
            shortsMediaDao.upsert(newEntity)
        }
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShortsViewModel(application) as T
        }
    }
}
