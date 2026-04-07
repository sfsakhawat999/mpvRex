package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSearchRepository
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSubtitle
import app.marlboroadvance.mpvex.repository.wyzie.WyzieTmdbResult
import app.marlboroadvance.mpvex.repository.wyzie.WyzieTvShowDetails
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSeason
import app.marlboroadvance.mpvex.repository.wyzie.WyzieEpisode
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages subtitle state and operations, including local scanning and online search.
 */
class SubtitleManager(
    private val context: Context,
    private val wyzieRepository: WyzieSearchRepository,
    private val scope: CoroutineScope,
    private val onShowToast: (String) -> Unit
) {
    companion object {
        private const val TAG = "SubtitleManager"
    }

    // ==================== State ====================

    private val _wyzieSearchResults = MutableStateFlow<List<WyzieSubtitle>>(emptyList())
    val wyzieSearchResults = _wyzieSearchResults.asStateFlow()

    private val _mediaSearchResults = MutableStateFlow<List<WyzieTmdbResult>>(emptyList())
    val mediaSearchResults = _mediaSearchResults.asStateFlow()

    private val _selectedTvShow = MutableStateFlow<WyzieTvShowDetails?>(null)
    val selectedTvShow = _selectedTvShow.asStateFlow()

    private val _selectedSeason = MutableStateFlow<WyzieSeason?>(null)
    val selectedSeason = _selectedSeason.asStateFlow()

    private val _seasonEpisodes = MutableStateFlow<List<WyzieEpisode>>(emptyList())
    val seasonEpisodes = _seasonEpisodes.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<WyzieEpisode?>(null)
    val selectedEpisode = _selectedEpisode.asStateFlow()

    private val _isDownloadingSub = MutableStateFlow(false)
    val isDownloadingSub = _isDownloadingSub.asStateFlow()

    private val _isSearchingSub = MutableStateFlow(false)
    val isSearchingSub = _isSearchingSub.asStateFlow()

    private val _isSearchingMedia = MutableStateFlow(false)
    val isSearchingMedia = _isSearchingMedia.asStateFlow()

    private val _isFetchingTvDetails = MutableStateFlow(false)
    val isFetchingTvDetails = _isFetchingTvDetails.asStateFlow()

    private val _isFetchingEpisodes = MutableStateFlow(false)
    val isFetchingEpisodes = _isFetchingEpisodes.asStateFlow()

    private val _isOnlineSectionExpanded = MutableStateFlow(true)
    val isOnlineSectionExpanded = _isOnlineSectionExpanded.asStateFlow()

    private val _externalSubtitles = mutableListOf<String>()
    val externalSubtitles: List<String> get() = _externalSubtitles.toList()

    private val mpvPathToUriMap = mutableMapOf<String, String>()

    // ==================== Actions ====================

    fun toggleOnlineSection() {
        _isOnlineSectionExpanded.value = !_isOnlineSectionExpanded.value
    }

    fun addSubtitle(uri: Uri, select: Boolean = true, silent: Boolean = false) {
        val uriString = uri.toString()
        if (_externalSubtitles.contains(uriString)) {
            Log.d(TAG, "Subtitle already tracked, skipping: $uriString")
            return
        }

        val fileName = uri.lastPathSegment ?: "subtitle"
        if (!isValidSubtitleFile(fileName)) {
            if (!silent) onShowToast("Invalid subtitle file")
            return
        }

        // Take persistent URI permission for content:// URIs
        if (uri.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.i(TAG, "Persistent permission not taken for $uri")
            }
        }

        scope.launch(Dispatchers.IO) {
            runCatching {
                val mpvPath = uri.resolveUri(context) ?: uri.toString()
                val mode = if (select) "select" else "auto"
                
                // Store mapping for reliable physical deletion later
                mpvPathToUriMap[mpvPath] = uri.toString()
                
                MPVLib.command("sub-add", mpvPath, mode)

                if (!_externalSubtitles.contains(uriString)) {
                    _externalSubtitles.add(uriString)
                }

                if (!silent) {
                    withContext(Dispatchers.Main) {
                        onShowToast("Subtitle added: ${fileName.take(30)}")
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to add subtitle", e)
                if (!silent) {
                    withContext(Dispatchers.Main) {
                        onShowToast("Failed to load subtitle")
                    }
                }
            }
        }
    }

    fun removeSubtitle(id: Int, tracks: List<TrackNode>) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val trackToRemove = tracks.firstOrNull { it.id == id }
                
                // If it's external, physically delete the file if we can find its URI
                if (trackToRemove?.external == true && trackToRemove.externalFilename != null) {
                    val mpvPath = trackToRemove.externalFilename
                    val originalUriString = mpvPathToUriMap[mpvPath] ?: mpvPath
                    val uri = Uri.parse(originalUriString)
                    
                    val deleted = wyzieRepository.deleteSubtitleFile(uri)
                    
                    if (deleted) {
                        _externalSubtitles.remove(originalUriString)
                        mpvPathToUriMap.remove(mpvPath)
                        withContext(Dispatchers.Main) {
                            onShowToast("Subtitle deleted")
                        }
                    }
                }
                
                MPVLib.command("sub-remove", id.toString())
            }.onFailure {
                Log.e(TAG, "Failed to remove subtitle", it)
            }
        }
    }

    fun clearExternalSubtitles() {
        _externalSubtitles.clear()
        mpvPathToUriMap.clear()
    }

    // ==================== Online Search ====================

    fun searchMedia(query: String) {
        if (query.isBlank()) {
            _mediaSearchResults.value = emptyList()
            return
        }
        scope.launch {
            _isSearchingMedia.value = true
            wyzieRepository.searchMedia(query)
                .onSuccess { results ->
                    _mediaSearchResults.value = results
                }
                .onFailure {
                    Log.e(TAG, "Media search failed", it)
                }
            _isSearchingMedia.value = false
        }
    }

    fun selectMedia(result: WyzieTmdbResult) {
        _wyzieSearchResults.value = emptyList()
        if (result.mediaType == "movie") {
            searchSubtitles(result.title)
            _selectedTvShow.value = null
        } else {
            fetchTvShowDetails(result.id)
        }
    }

    private fun fetchTvShowDetails(id: Int) {
        scope.launch {
            _isFetchingTvDetails.value = true
            wyzieRepository.getTvShowDetails(id)
                .onSuccess { details ->
                    val validSeasons = details.seasons.filter { it.season_number > 0 }.sortedBy { it.season_number }
                    _selectedTvShow.value = details.copy(seasons = validSeasons)
                    _selectedSeason.value = null
                    _seasonEpisodes.value = emptyList()
                }
                .onFailure {
                    Log.e(TAG, "Failed to fetch TV details", it)
                    onShowToast("Failed to load series details")
                }
            _isFetchingTvDetails.value = false
        }
    }

    fun selectSeason(season: WyzieSeason) {
        val tvShowId = _selectedTvShow.value?.id ?: return
        _selectedSeason.value = season
        scope.launch {
            _isFetchingEpisodes.value = true
            wyzieRepository.getSeasonEpisodes(tvShowId, season.season_number)
                .onSuccess { episodes ->
                    val validEpisodes = episodes.filter { it.episode_number > 0 }.sortedBy { it.episode_number }
                    _seasonEpisodes.value = validEpisodes
                    _selectedEpisode.value = null
                }
                .onFailure {
                    Log.e(TAG, "Failed to fetch episodes", it)
                    onShowToast("Failed to load episodes")
                }
            _isFetchingEpisodes.value = false
        }
    }

    fun selectEpisode(episode: WyzieEpisode, fallbackTitle: String) {
        val tvShowName = _selectedTvShow.value?.name ?: fallbackTitle
        _selectedEpisode.value = episode
        searchSubtitles(tvShowName, episode.season_number, episode.episode_number)
    }

    fun searchSubtitles(query: String, season: Int? = null, episode: Int? = null, year: String? = null) {
        scope.launch {
            _isSearchingSub.value = true
            wyzieRepository.search(query, season, episode, year)
                .onSuccess { results ->
                    _wyzieSearchResults.value = results
                }
                .onFailure {
                    Log.e(TAG, "Subtitle search failed", it)
                    onShowToast("Search failed")
                }
            _isSearchingSub.value = false
        }
    }

    fun downloadSubtitle(subtitle: WyzieSubtitle, mediaTitle: String) {
        scope.launch {
            _isDownloadingSub.value = true
            wyzieRepository.download(subtitle, mediaTitle)
                .onSuccess { uri ->
                    addSubtitle(uri)
                }
                .onFailure {
                    Log.e(TAG, "Subtitle download failed", it)
                    onShowToast("Download failed")
                }
            _isDownloadingSub.value = false
        }
    }

    fun clearMediaSelection() {
        _selectedTvShow.value = null
        _selectedSeason.value = null
        _selectedEpisode.value = null
        _seasonEpisodes.value = emptyList()
        _mediaSearchResults.value = emptyList()
        _wyzieSearchResults.value = emptyList()
    }

    // ==================== Utilities = ====================

    private fun isValidSubtitleFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".srt") || lower.endsWith(".vtt") ||
                lower.endsWith(".ssa") || lower.endsWith(".ass")
    }
}
