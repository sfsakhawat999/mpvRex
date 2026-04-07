package app.marlboroadvance.mpvex.ui.player

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the playback playlist, including index tracking, shuffling, 
 * and windowed loading for large playlists.
 */
class PlaylistManager {
    companion object {
        private const val TAG = "PlaylistManager"
    }

    // ==================== State ====================

    private val _playlist = MutableStateFlow<List<Uri>>(emptyList())
    val playlist: StateFlow<List<Uri>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffledIndices = MutableStateFlow<List<Int>>(emptyList())
    val shuffledIndices: StateFlow<List<Int>> = _shuffledIndices.asStateFlow()

    private val _shuffledPosition = MutableStateFlow(0)
    val shuffledPosition: StateFlow<Int> = _shuffledPosition.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private var _playlistId: Int? = null
    val playlistId: Int? get() = _playlistId

    private var _playlistWindowOffset: Int = 0
    val playlistWindowOffset: Int get() = _playlistWindowOffset

    private var _playlistTotalCount: Int = -1
    val playlistTotalCount: Int get() = _playlistTotalCount

    private var _isM3uPlaylist: Boolean = false
    val isM3uPlaylist: Boolean get() = _isM3uPlaylist

    // ==================== Public Methods ====================

    fun setPlaylist(
        items: List<Uri>,
        index: Int = 0,
        id: Int? = null,
        totalCount: Int = -1,
        windowOffset: Int = 0,
        isM3u: Boolean = false
    ) {
        _playlist.value = items
        _currentIndex.value = index
        _playlistId = id
        _playlistTotalCount = totalCount
        _playlistWindowOffset = windowOffset
        _isM3uPlaylist = isM3u

        if (_shuffleEnabled.value) {
            generateShuffledIndices()
        }
    }

    fun updateIndex(index: Int) {
        _currentIndex.value = index
        if (_shuffleEnabled.value) {
            val sPos = _shuffledIndices.value.indexOf(index)
            if (sPos != -1) {
                _shuffledPosition.value = sPos
            } else {
                // Current index not in shuffled list (shouldn't happen usually)
                generateShuffledIndices()
            }
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        if (_shuffleEnabled.value == enabled) return
        _shuffleEnabled.value = enabled
        if (enabled && _playlist.value.isNotEmpty()) {
            generateShuffledIndices()
        } else {
            _shuffledIndices.value = emptyList()
            _shuffledPosition.value = 0
        }
    }

    fun hasNext(repeatAll: Boolean): Boolean {
        if (_playlist.value.isEmpty()) return false
        
        val effectiveSize = if (_playlistTotalCount > 0) _playlistTotalCount else _playlist.value.size
        
        return if (_shuffleEnabled.value) {
            _shuffledPosition.value < _shuffledIndices.value.size - 1 || repeatAll
        } else {
            _currentIndex.value < effectiveSize - 1 || repeatAll
        }
    }

    fun hasPrevious(repeatAll: Boolean): Boolean {
        if (_playlist.value.isEmpty()) return false
        
        return if (_shuffleEnabled.value) {
            _shuffledPosition.value > 0 || repeatAll
        } else {
            _currentIndex.value > 0 || repeatAll
        }
    }

    /**
     * Returns the index of the next item to play, or null if there is no next item.
     */
    fun getNextIndex(repeatAll: Boolean): Int? {
        if (_playlist.value.isEmpty()) return null
        
        val effectiveSize = if (_playlistTotalCount > 0) _playlistTotalCount else _playlist.value.size
        
        return if (_shuffleEnabled.value) {
            if (_shuffledPosition.value < _shuffledIndices.value.size - 1) {
                _shuffledIndices.value[_shuffledPosition.value + 1]
            } else if (repeatAll) {
                generateShuffledIndices()
                _shuffledIndices.value[0]
            } else {
                null
            }
        } else {
            if (_currentIndex.value < effectiveSize - 1) {
                _currentIndex.value + 1
            } else if (repeatAll) {
                0
            } else {
                null
            }
        }
    }

    /**
     * Returns the index of the previous item to play, or null if there is no previous item.
     */
    fun getPreviousIndex(repeatAll: Boolean): Int? {
        if (_playlist.value.isEmpty()) return null
        
        val effectiveSize = if (_playlistTotalCount > 0) _playlistTotalCount else _playlist.value.size
        
        return if (_shuffleEnabled.value) {
            if (_shuffledPosition.value > 0) {
                _shuffledIndices.value[_shuffledPosition.value - 1]
            } else if (repeatAll) {
                _shuffledIndices.value.last()
            } else {
                null
            }
        } else {
            if (_currentIndex.value > 0) {
                _currentIndex.value - 1
            } else if (repeatAll) {
                effectiveSize - 1
            } else {
                null
            }
        }
    }

    fun moveNext() {
        val next = getNextIndex(true) // We handle repeatAll logic at caller usually or use moveNext with flag
        // This is a bit simplified, actual movement usually involves loading the item
    }

    private fun generateShuffledIndices() {
        val size = _playlist.value.size
        if (size == 0) return

        val currentIdx = _currentIndex.value
        // Create a list of all indices except the current one
        val indices = (0 until size).filter { it != currentIdx }.toMutableList()
        indices.shuffle()

        // Put current index at the beginning
        _shuffledIndices.value = listOf(currentIdx) + indices
        _shuffledPosition.value = 0
    }
}
