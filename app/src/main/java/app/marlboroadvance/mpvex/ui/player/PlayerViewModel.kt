package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSearchRepository
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSubtitle
import app.marlboroadvance.mpvex.utils.media.ChecksumUtils
import app.marlboroadvance.mpvex.utils.media.MediaInfoParser
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import androidx.documentfile.provider.DocumentFile
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


enum class RepeatMode {
  OFF,      // No repeat
  ONE,      // Repeat current file
  ALL       // Repeat all (playlist)
}

class PlayerViewModelProviderFactory(
  private val host: PlayerHost,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(
    modelClass: Class<T>,
    extras: CreationExtras,
  ): T {
    if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return PlayerViewModel(host) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

@Suppress("TooManyFunctions")
class PlayerViewModel(
  private val host: PlayerHost,
) : ViewModel(),
  KoinComponent {
  private val playerPreferences: PlayerPreferences by inject()
  private val gesturePreferences: GesturePreferences by inject()
  private val audioPreferences: AudioPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val json: Json by inject()
  private val playbackStateRepository: app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository by inject()
  private val recentlyPlayedRepository: app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository by inject()
  private val wyzieRepository: WyzieSearchRepository by inject()

  /**
   * Manager for playlist state and logic.
   */
  private val _playlistManager = PlaylistManager()
  val playlistManager: PlaylistManager get() = _playlistManager

  /**
   * Manager for subtitle state and operations.
   */
  private val _subtitleManager = SubtitleManager(
    context = host.context,
    wyzieRepository = wyzieRepository,
    scope = viewModelScope,
    onShowToast = { showToast(it) }
  )
  val subtitleManager: SubtitleManager get() = _subtitleManager

  /**
   * Manager for history tracking and position saving.
   */
  private val _historyManager = app.marlboroadvance.mpvex.utils.history.HistoryManager(
    context = host.context,
    recentlyPlayedRepository = recentlyPlayedRepository,
    playbackStateRepository = playbackStateRepository,
    advancedPreferences = advancedPreferences,
    scope = viewModelScope
  )
  val historyManager: app.marlboroadvance.mpvex.utils.history.HistoryManager get() = _historyManager

  /**
   * Manager for custom user-defined buttons.
   */
  private val _customButtonManager = CustomButtonManager(
    context = host.context,
    playerPreferences = playerPreferences,
    advancedPreferences = advancedPreferences,
    json = json,
    scope = viewModelScope
  )
  val customButtonManager: CustomButtonManager get() = _customButtonManager

  /**
   * Manager for playback operations (seeking, speed).
   */
  private val _playbackManager: PlaybackManager by inject()
  val playbackManager: PlaybackManager get() = _playbackManager

  // Subtitle state delegates
  val isDownloadingSub = _subtitleManager.isDownloadingSub
  val isSearchingSub = _subtitleManager.isSearchingSub
  val isOnlineSectionExpanded = _subtitleManager.isOnlineSectionExpanded
  val wyzieSearchResults = _subtitleManager.wyzieSearchResults
  val mediaSearchResults = _subtitleManager.mediaSearchResults
  val isSearchingMedia = _subtitleManager.isSearchingMedia
  val selectedTvShow = _subtitleManager.selectedTvShow
  val isFetchingTvDetails = _subtitleManager.isFetchingTvDetails
  val selectedSeason = _subtitleManager.selectedSeason
  val seasonEpisodes = _subtitleManager.seasonEpisodes
  val isFetchingEpisodes = _subtitleManager.isFetchingEpisodes
  val selectedEpisode = _subtitleManager.selectedEpisode

  // Playlist items for the playlist sheet
  private val _playlistItems = kotlinx.coroutines.flow.MutableStateFlow<List<app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistItem>>(emptyList())
  val playlistItems: kotlinx.coroutines.flow.StateFlow<List<app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistItem>> = _playlistItems.asStateFlow()

  fun toggleOnlineSection() = _subtitleManager.toggleOnlineSection()

  // Cache for video metadata to avoid re-extracting — LruCache handles bounds + thread-safety
  private val metadataCache = object : android.util.LruCache<String, Pair<String, String>>(100) {}

  private fun updateMetadataCache(key: String, value: Pair<String, String>) {
    metadataCache.put(key, value)
  }

  // MPV properties with efficient collection
  val paused by MPVLib.propBoolean["pause"].collectAsState(viewModelScope)
  val pos by MPVLib.propInt["time-pos"].collectAsState(viewModelScope)
  val duration by MPVLib.propInt["duration"].collectAsState(viewModelScope)

  // High-precision position and duration for smooth seekbar
  private val _precisePosition = MutableStateFlow(0f)
  val precisePosition = _precisePosition.asStateFlow()

  private val _preciseDuration = MutableStateFlow(0f)
  val preciseDuration = _preciseDuration.asStateFlow()

  // Audio state
  val currentVolume = MutableStateFlow(host.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
  private val volumeBoostCap by MPVLib.propInt["volume-max"].collectAsState(viewModelScope)
  
  // Gesture state for seekbar bouncing animation
  private val _isGestureSeeking = MutableStateFlow(false)
  val isGestureSeeking: StateFlow<Boolean> = _isGestureSeeking.asStateFlow()

  fun setGestureSeeking(isSeeking: Boolean) {
    _isGestureSeeking.value = isSeeking
  }

  // Gesture state for vertical bouncing animation
  val isVerticalGestureActive = MutableStateFlow(false)

  init {
    // Poll precise position only when playing
    viewModelScope.launch {
      while (isActive) {
        val time = MPVLib.getPropertyDouble("time-pos")
        if (time != null) {
          _precisePosition.value = time.toFloat()
        }
        delay(16) // ~60fps updates
      }
    }

    // Update precise duration when the integer duration changes (avoid polling)
    viewModelScope.launch {
      MPVLib.propInt["duration"].collect { _ ->
        val dur = MPVLib.getPropertyDouble("duration")
        if (dur != null && dur > 0) {
          _preciseDuration.value = dur.toFloat()

          // --- AMBIENT FIX: Adapt shader to new file dimensions ---
          ambientModeManager.resetAmbientMode()
          viewModelScope.launch {
            // Slight delay ensures MPV's video-params (w/h/crop) are fully populated
            delay(250)
            ambientModeManager.updateAmbientStretch()
          }
          // --------------------------------------------------------
        }
      }
    }
  }
  val maxVolume = host.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

  val subtitleTracks: StateFlow<List<TrackNode>> =
    observeTracks(json)
      .map { it.filter { t -> t.isSubtitle } }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val audioTracks: StateFlow<List<TrackNode>> =
    observeTracks(json)
      .map { it.filter { t -> t.isAudio } }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val chapters: StateFlow<List<dev.vivvvek.seeker.Segment>> =
    observeChapters(json)
      .stateIn(viewModelScope, SharingStarted.Lazily, persistentListOf())

  // UI state
  private val _controlsShown = MutableStateFlow(false)
  val controlsShown: StateFlow<Boolean> = _controlsShown.asStateFlow()

  private val _seekBarShown = MutableStateFlow(false)
  val seekBarShown: StateFlow<Boolean> = _seekBarShown.asStateFlow()

  private val _areControlsLocked = MutableStateFlow(false)
  val areControlsLocked: StateFlow<Boolean> = _areControlsLocked.asStateFlow()

  val playerUpdate = MutableStateFlow<PlayerUpdates>(PlayerUpdates.None)
  val isBrightnessSliderShown = MutableStateFlow(false)
  val isVolumeSliderShown = MutableStateFlow(false)
  val volumeSliderTimestamp = MutableStateFlow(0L)
  val brightnessSliderTimestamp = MutableStateFlow(0L)
  val currentBrightness =
    MutableStateFlow(
      runCatching {
        Settings.System
          .getFloat(host.hostContentResolver, Settings.System.SCREEN_BRIGHTNESS)
          .normalize(0f, 255f, 0f, 1f)
      }.getOrElse { 0f },
    )

  val sheetShown = MutableStateFlow(Sheets.None)
  val lastMoreSheetTab = MutableStateFlow(1) // Default to Controls tab (index 1)
  val panelShown = MutableStateFlow(Panels.None)
  val isSpeedLocked = MutableStateFlow(false)

  // Seek state
  private val _seekText = MutableStateFlow<String?>(null)
  val seekText: StateFlow<String?> = _seekText.asStateFlow()

  private val _doubleTapSeekAmount = MutableStateFlow(0)
  val doubleTapSeekAmount: StateFlow<Int> = _doubleTapSeekAmount.asStateFlow()

  private val _isSeekingForwards = MutableStateFlow(false)
  val isSeekingForwards: StateFlow<Boolean> = _isSeekingForwards.asStateFlow()

  // Frame navigation
  private val _currentFrame = MutableStateFlow(0)
  val currentFrame: StateFlow<Int> = _currentFrame.asStateFlow()

  private val _totalFrames = MutableStateFlow(0)
  val totalFrames: StateFlow<Int> = _totalFrames.asStateFlow()

  private val _isFrameNavigationExpanded = MutableStateFlow(false)
  val isFrameNavigationExpanded: StateFlow<Boolean> = _isFrameNavigationExpanded.asStateFlow()

  private val _isSnapshotLoading = MutableStateFlow(false)
  val isSnapshotLoading: StateFlow<Boolean> = _isSnapshotLoading.asStateFlow()

  // Video zoom
  private val _videoZoom = MutableStateFlow(0f)
  val videoZoom: StateFlow<Float> = _videoZoom.asStateFlow()

 // Video aspect ratio (now persisted via preferences)
  private val _videoAspect = MutableStateFlow(playerPreferences.defaultVideoAspect.get())
  val videoAspect: StateFlow<VideoAspect> = _videoAspect.asStateFlow()

  // Current aspect ratio value (for custom ratios and tracking)
  private val _currentAspectRatio = MutableStateFlow(playerPreferences.defaultCustomAspectRatio.get())
  val currentAspectRatio: StateFlow<Double> = _currentAspectRatio.asStateFlow()

  // Timer
  private var timerJob: Job? = null
  private val _remainingTime = MutableStateFlow(0)
  val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

  // Media title for subtitle association
  var currentMediaTitle: String = ""
  private var lastAutoSelectedMediaTitle: String? = null

  // External subtitle tracking
  val externalSubtitles: List<String> get() = _subtitleManager.externalSubtitles

  // Repeat and Shuffle state
  private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
  val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

  private val _shuffleEnabled = MutableStateFlow(false)
  val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

  // A-B Loop state
  private val _abLoopA = MutableStateFlow<Double?>(null)
  val abLoopA: StateFlow<Double?> = _abLoopA.asStateFlow()

  private val _abLoopB = MutableStateFlow<Double?>(null)
  val abLoopB: StateFlow<Double?> = _abLoopB.asStateFlow()

  private val _isABLoopExpanded = MutableStateFlow(false)
  val isABLoopExpanded: StateFlow<Boolean> = _isABLoopExpanded.asStateFlow()

  // Mirroring state
  private val _isMirrored = MutableStateFlow(false)
  val isMirrored: StateFlow<Boolean> = _isMirrored.asStateFlow()

  // Vertical flip state
  private val _isVerticalFlipped = MutableStateFlow(false)
  val isVerticalFlipped: StateFlow<Boolean> = _isVerticalFlipped.asStateFlow()

  // ==================== Ambience Mode ======================================
  // Ambient mode manager handles all ambient mode functionality
  private val ambientModeManager = AmbientModeManager(
    playerPreferences = playerPreferences,
    cacheDir = host.context.cacheDir,
    scope = viewModelScope,
    onShowText = { text -> playerUpdate.value = PlayerUpdates.ShowText(text) }
  )

  // Expose ambient mode state through the manager
  val isAmbientEnabled: StateFlow<Boolean> = ambientModeManager.isAmbientEnabled

  init {
    // Track selection is now handled by TrackSelector in PlayerActivity
    
    // Restore repeat mode and shuffle state from preferences
    _repeatMode.value = playerPreferences.repeatMode.get()
    _shuffleEnabled.value = playerPreferences.shuffleEnabled.get()

    // Observe volume boost cap changes to enforce limits dynamically (in PiP)
    viewModelScope.launch {
      audioPreferences.volumeBoostCap.changes().collect { cap ->
        val maxVol = 100 + cap
        runCatching {
          MPVLib.setPropertyString("volume-max", maxVol.toString())
          
          // Clamp current volume if it exceeds the new limit
          val currentMpvVol = MPVLib.getPropertyInt("volume") ?: 100
          if (currentMpvVol > maxVol) {
            MPVLib.setPropertyInt("volume", maxVol)
          }
        }.onFailure { e ->
          Log.e(TAG, "Error setting volume-max: $maxVol", e)
        }
      }
    }

    // Monitor duration and AB loop changes to automatically enable precise seeking
    viewModelScope.launch {
      combine(
        MPVLib.propInt["duration"],
        abLoopA,
        abLoopB
      ) { duration, loopA, loopB ->
        Triple(duration, loopA, loopB)
      }.collect { (duration, loopA, loopB) ->
        val videoDuration = duration ?: 0
        // Use precise seeking for videos shorter than 2 minutes, or if AB loop is active, or if preference is enabled
        val isLoopActive = loopA != null || loopB != null
        val shouldUsePreciseSeeking = playerPreferences.usePreciseSeeking.get() || videoDuration < 120 || isLoopActive
        
        // Update hr-seek settings dynamically
        MPVLib.setPropertyString("hr-seek", if (shouldUsePreciseSeeking) "yes" else "no")
        MPVLib.setPropertyString("hr-seek-framedrop", if (shouldUsePreciseSeeking) "no" else "yes")
      }
    }
    
    
    // Refresh custom buttons when Lua scripts are enabled/disabled or configuration changes
    viewModelScope.launch {
      combine(
        advancedPreferences.enableLuaScripts.changes().drop(1),
        playerPreferences.customButtons.changes().drop(1)
      ) { _, _ -> }.collect {
        _customButtonManager.setup()
      }
    }

    _customButtonManager.setup()
  }

  fun onMpvCoreInitialized() {
    _customButtonManager.onMpvInitialized()
  }

  // ==================== Custom Buttons ====================

    val customButtons = _customButtonManager.customButtons

    fun callCustomButton(id: String) = _customButtonManager.callButton(id)

    fun callCustomButtonLongPress(id: String) = _customButtonManager.callButtonLongPress(id)

  // Cached values
  private val doubleTapToSeekDuration by lazy { gesturePreferences.doubleTapToSeekDuration.get() }
  private val inputMethodManager by lazy {
    host.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  }

  private companion object {
    const val TAG = "PlayerViewModel"
    val VALID_SUBTITLE_EXTENSIONS =
      setOf(
        // Common & modern
        "srt", "vtt", "ass", "ssa",
        // DVD / Blu-ray
        "sub", "idx", "sup",
        // Streaming / XML / Professional
        "xml", "ttml", "dfxp", "itt", "ebu", "imsc", "usf",
        // Online platforms
        "sbv", "srv1", "srv2", "srv3", "json",
        // Legacy & niche
        "sami", "smi", "mpl", "pjs", "stl", "rt", "psb", "cap",
        // Broadcast captions
        "scc", "vttx",
        // Karaoke / lyrics
        "lrc", "krc",
        // Fallback / raw text
        "txt", "pgs"
      )
  }

  // ==================== Timer ====================

  fun startTimer(seconds: Int) {
    timerJob?.cancel()
    _remainingTime.value = seconds
    if (seconds < 1) return

    timerJob =
      viewModelScope.launch {
        for (time in seconds downTo 0) {
          _remainingTime.value = time
          delay(1000)
        }
        MPVLib.setPropertyBoolean("pause", true)
        showToast(host.context.getString(R.string.toast_sleep_timer_ended))
      }
  }

  // ==================== Decoder ====================

  // ==================== Audio/Subtitle Management ====================

  fun addAudio(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        val path =
          uri.resolveUri(host.context)
            ?: return@launch withContext(Dispatchers.Main) {
              showToast("Failed to load audio file: Invalid URI")
            }

        MPVLib.command("audio-add", path, "cached")
        withContext(Dispatchers.Main) {
          showToast("Audio track added")
        }
      }.onFailure { e ->
        withContext(Dispatchers.Main) {
          showToast("Failed to load audio: ${e.message}")
        }
        android.util.Log.e("PlayerViewModel", "Error adding audio", e)
      }
    }
  }

  fun addSubtitle(uri: Uri, select: Boolean = true, silent: Boolean = false) = _subtitleManager.addSubtitle(uri, select, silent)

  private fun scanLocalSubtitles(mediaTitle: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val saveFolderUri = subtitlesPreferences.subtitleSaveFolder.get()
      if (saveFolderUri.isBlank()) return@launch
      
      try {
        val sanitizedTitle = MediaInfoParser.parse(mediaTitle).title
        val fullTitle = mediaTitle.substringBeforeLast(".")
        val checksumTitle = ChecksumUtils.getCRC32(mediaTitle)
        val parentDir = DocumentFile.fromTreeUri(host.context, Uri.parse(saveFolderUri)) ?: return@launch
        
        // Scan potential folder names for compatibility: checksum, full, and sanitized
        listOf(checksumTitle, fullTitle, sanitizedTitle).distinct().forEach { folderName ->
          val movieDir = parentDir.findFile(folderName) ?: return@forEach
          if (movieDir.isDirectory) {
            movieDir.listFiles().forEach { file ->
              val fileName = file.name ?: ""
              val lower = fileName.lowercase()
              val isValid = lower.endsWith(".srt") || lower.endsWith(".vtt") ||
                            lower.endsWith(".ssa") || lower.endsWith(".ass")
              
              if (file.isFile && isValid) {
                withContext(Dispatchers.Main) {
                  // Don't auto-select during scan, just make available
                  addSubtitle(file.uri, select = false, silent = true)
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        android.util.Log.e("PlayerViewModel", "Error scanning local subtitles: ${e.message}", e)
      }
    }
  }

  fun setMediaTitle(mediaTitle: String) {
    if (currentMediaTitle != mediaTitle) {
      currentMediaTitle = mediaTitle
      lastAutoSelectedMediaTitle = null
      // Clear external subtitles when media changes
      _subtitleManager.clearExternalSubtitles()
      // Scan for previously downloaded/added subtitles
      scanLocalSubtitles(mediaTitle)
    }
  }


  fun removeSubtitle(id: Int) = _subtitleManager.removeSubtitle(id, subtitleTracks.value)

  // --- Media Search and Series Management ---

  fun searchMedia(query: String) = _subtitleManager.searchMedia(query)

  fun selectMedia(result: app.marlboroadvance.mpvex.repository.wyzie.WyzieTmdbResult) = _subtitleManager.selectMedia(result)

  fun selectSeason(season: app.marlboroadvance.mpvex.repository.wyzie.WyzieSeason) = _subtitleManager.selectSeason(season)

  fun selectEpisode(episode: app.marlboroadvance.mpvex.repository.wyzie.WyzieEpisode) = _subtitleManager.selectEpisode(episode, currentMediaTitle)

  fun clearMediaSelection() = _subtitleManager.clearMediaSelection()

  // --- Subtitle Search ---
  fun searchSubtitles(query: String, season: Int? = null, episode: Int? = null, year: String? = null) = _subtitleManager.searchSubtitles(query, season, episode, year)

  fun downloadSubtitle(subtitle: WyzieSubtitle) = _subtitleManager.downloadSubtitle(subtitle, currentMediaTitle)


  fun toggleSubtitle(id: Int) {
    val primarySid = MPVLib.getPropertyInt("sid") ?: 0
    val secondarySid = MPVLib.getPropertyInt("secondary-sid") ?: 0

    when {
      id == primarySid -> {
        // Unselecting primary subtitle
        if (secondarySid > 0) {
          // If there's a secondary subtitle, promote it to primary
          // First clear secondary, then set primary to that value
          val secondaryToPromote = secondarySid
          MPVLib.setPropertyString("secondary-sid", "no")
          MPVLib.setPropertyInt("sid", secondaryToPromote)
        } else {
          // No secondary, just turn off primary
          MPVLib.setPropertyString("sid", "no")
        }
      }
      id == secondarySid -> MPVLib.setPropertyString("secondary-sid", "no")
      primarySid <= 0 -> MPVLib.setPropertyInt("sid", id)
      secondarySid <= 0 -> MPVLib.setPropertyInt("secondary-sid", id)
      else -> MPVLib.setPropertyInt("sid", id)
    }
  }

  fun isSubtitleSelected(id: Int): Boolean {
    val primarySid = MPVLib.getPropertyInt("sid") ?: 0
    val secondarySid = MPVLib.getPropertyInt("secondary-sid") ?: 0
    return (id == primarySid && primarySid > 0) || (id == secondarySid && secondarySid > 0)
  }

  private fun getFileNameFromUri(uri: Uri): String? =
    when (uri.scheme) {
      "content" ->
        host.context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }

      "file" -> uri.lastPathSegment
      else -> uri.lastPathSegment
    }

  private fun isValidSubtitleFile(fileName: String): Boolean =
    fileName.substringAfterLast('.', "").lowercase() in VALID_SUBTITLE_EXTENSIONS

  // ==================== Playback Control ====================

  fun pauseUnpause() {
    viewModelScope.launch(Dispatchers.IO) {
      val isPaused = MPVLib.getPropertyBoolean("pause") ?: false
      if (isPaused) {
        // We are about to unpause, so request focus
        withContext(Dispatchers.Main) { host.requestAudioFocus() }
        MPVLib.setPropertyBoolean("pause", false)
      } else {
        // We are about to pause
        MPVLib.setPropertyBoolean("pause", true)
        withContext(Dispatchers.Main) { host.abandonAudioFocus() }
      }
    }
  }

  fun pause() {
    viewModelScope.launch(Dispatchers.IO) {
      MPVLib.setPropertyBoolean("pause", true)
      withContext(Dispatchers.Main) { host.abandonAudioFocus() }
    }
  }

  fun unpause() {
    viewModelScope.launch(Dispatchers.IO) {
      withContext(Dispatchers.Main) { host.requestAudioFocus() }
      MPVLib.setPropertyBoolean("pause", false)
    }
  }

  // ==================== UI Control ====================

  fun showControls() {
    if (sheetShown.value != Sheets.None || panelShown.value != Panels.None) return
    try {
      if (playerPreferences.showSystemStatusBar.get()) {
        host.windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
        host.windowInsetsController.isAppearanceLightStatusBars = false
      }
      if (playerPreferences.showSystemNavigationBar.get()) {
        host.windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
      }
    } catch (e: Exception) {
      // Defensive: InsetsController animation can crash under FD pressure
      // (e.g. during high-res HEVC playback on certain devices)
      Log.e(TAG, "Failed to show system bars", e)
    }
    _controlsShown.value = true
  }

  fun hideControls() {
    try {
      if (playerPreferences.showSystemStatusBar.get()) {
        host.windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
      }
      if (playerPreferences.showSystemNavigationBar.get()) {
        host.windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to hide system bars", e)
    }
    _controlsShown.value = false
    _seekBarShown.value = false
  }

  fun autoHideControls() {
    try {
      if (playerPreferences.showSystemStatusBar.get()) {
        host.windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
      }
      if (playerPreferences.showSystemNavigationBar.get()) {
        host.windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to hide system bars", e)
    }
    _controlsShown.value = false
    _seekBarShown.value = true
  }

  fun showSeekBar() {
    if (sheetShown.value == Sheets.None) {
      _seekBarShown.value = true
    }
  }

  fun hideSeekBar() {
    _seekBarShown.value = false
  }

  fun lockControls() {
    _areControlsLocked.value = true
  }

  fun unlockControls() {
    _areControlsLocked.value = false
  }

  // ==================== Seeking ====================

  fun seekBy(offset: Int) {
    _playbackManager.seekBy(viewModelScope, offset)
  }

  fun seekTo(position: Int) {
    _playbackManager.seekTo(viewModelScope, position, _abLoopA.value, _abLoopB.value)
  }

  fun setPlaybackSpeed(speed: Float) {
    _playbackManager.setSpeed(speed)
  }

  fun resetPlaybackSpeed() {
    _playbackManager.resetSpeed()
  }

  fun setSubtitleSpeed(speed: Double) {
    _playbackManager.setSubSpeed(speed)
  }

  fun leftSeek() {
    if ((pos ?: 0) > 0) {
      _doubleTapSeekAmount.value -= doubleTapToSeekDuration
    }
    _isSeekingForwards.value = false
    seekBy(-doubleTapToSeekDuration)
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun rightSeek() {
    if ((pos ?: 0) < (duration ?: 0)) {
      _doubleTapSeekAmount.value += doubleTapToSeekDuration
    }
    _isSeekingForwards.value = true
    seekBy(doubleTapToSeekDuration)
    if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
  }

  fun leftSubSeek() {
    val sid = MPVLib.getPropertyInt("sid") ?: 0
    if (sid != 0) {
      val pos1 = MPVLib.getPropertyDouble("time-pos") ?: 0.0
      MPVLib.command("sub-seek", "-1")

      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        val pos2 = MPVLib.getPropertyDouble("time-pos") ?: 0.0
        val diff = pos2 - pos1
        _isSeekingForwards.value = false
        _doubleTapSeekAmount.value += diff.toInt()
      }, 10)
      if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
    } else leftSeek()
  }

  fun rightSubSeek() {
    val sid = MPVLib.getPropertyInt("sid") ?: 0
    if (sid != 0) {
      val pos1 = MPVLib.getPropertyDouble("time-pos") ?: 0.0
      MPVLib.command("sub-seek", "1")

      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        val pos2 = MPVLib.getPropertyDouble("time-pos") ?: 0.0
        val diff = pos2 - pos1
        _isSeekingForwards.value = true
        _doubleTapSeekAmount.value += diff.toInt()
      }, 10)
      if (playerPreferences.showSeekBarWhenSeeking.get()) showSeekBar()
    } else rightSeek()
  }

  fun updateSeekAmount(amount: Int) {
    _doubleTapSeekAmount.value = amount
  }

  fun updateSeekText(text: String?) {
    _seekText.value = text
  }

  fun updateIsSeekingForwards(isForwards: Boolean) {
    _isSeekingForwards.value = isForwards
  }

  private fun seekToWithText(
    seekValue: Int,
    text: String?,
  ) {
    val currentPos = pos ?: return
    _isSeekingForwards.value = seekValue > currentPos
    _doubleTapSeekAmount.value = seekValue - currentPos
    _seekText.value = text
    seekTo(seekValue)
  }

  private fun seekByWithText(
    value: Int,
    text: String?,
  ) {
    val currentPos = pos ?: return
    val maxDuration = duration ?: return

    _doubleTapSeekAmount.update {
      if ((value < 0 && it < 0) || currentPos + value > maxDuration) 0 else it + value
    }
    _seekText.value = text
    _isSeekingForwards.value = value > 0
    seekBy(value)
  }

  // ==================== Brightness & Volume ====================

  fun changeBrightnessTo(brightness: Float) {
    val coercedBrightness = brightness.coerceIn(0f, 1f)
    host.hostWindow.attributes =
      host.hostWindow.attributes.apply {
        screenBrightness = coercedBrightness
      }
    currentBrightness.value = coercedBrightness

    // Save brightness to preferences if enabled
    if (playerPreferences.rememberBrightness.get()) {
      playerPreferences.defaultBrightness.set(coercedBrightness)
    }
  }

  fun displayBrightnessSlider() {
    isBrightnessSliderShown.value = true
    brightnessSliderTimestamp.value = System.currentTimeMillis()
  }

  fun changeVolumeBy(change: Int) {
    val mpvVolume = MPVLib.getPropertyInt("volume")
    val absoluteMaxVolume = volumeBoostCap ?: (audioPreferences.volumeBoostCap.get() + 100)

    if (absoluteMaxVolume > 100 && currentVolume.value == maxVolume) {
      if (mpvVolume == 100 && change < 0) {
        changeVolumeTo(currentVolume.value + change)
      }
      val finalMPVVolume = (mpvVolume?.plus(change))?.coerceAtLeast(100) ?: 100
      if (finalMPVVolume in 100..absoluteMaxVolume) {
        return changeMPVVolumeTo(finalMPVVolume)
      }
    }
    changeVolumeTo(currentVolume.value + change)
  }

  fun changeVolumeTo(volume: Int) {
    val newVolume = volume.coerceIn(0..maxVolume)
    host.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    currentVolume.value = newVolume
  }

  fun changeMPVVolumeTo(volume: Int) {
    MPVLib.setPropertyInt("volume", volume)
  }

  fun displayVolumeSlider() {
    isVolumeSliderShown.value = true
    volumeSliderTimestamp.value = System.currentTimeMillis()
  }

  // ==================== Video Aspect ====================

  private var _cachedVideoRotation = 0

  // Restores the user's preferred video aspect ratio and resets pan to center
  fun resetVisualPreferences() {

    // Cache the video rotation once it's available (used by stretch mode)
    _cachedVideoRotation = MPVLib.getPropertyInt("video-params/rotate") ?: 0
    
    // 1. Apply saved aspect ratio preference
    val savedAspect = playerPreferences.defaultVideoAspect.get()
    val savedCustomRatio = playerPreferences.defaultCustomAspectRatio.get()

    if (savedCustomRatio > 0) {
      setCustomAspectRatio(savedCustomRatio)
    } else {
      changeVideoAspect(savedAspect, showUpdate = false)
    }
    // 2. Reset pan to neutral for the new file
    if (_videoPanX.value != 0f || _videoPanY.value != 0f) {
      _videoPanX.value = 0f
      _videoPanY.value = 0f
      runCatching {
        MPVLib.setPropertyDouble("video-pan-x", 0.0)
        MPVLib.setPropertyDouble("video-pan-y", 0.0)
      }
    }
  }

  fun changeVideoAspect(
    aspect: VideoAspect,
    showUpdate: Boolean = true,
  ) {
    when (aspect) {
      VideoAspect.Fit -> {
        // To FIT: Reset both properties to their defaults.
        MPVLib.setPropertyDouble("panscan", 0.0)
        MPVLib.setPropertyDouble("video-aspect-override", -1.0)
      }
      VideoAspect.Crop -> {
        // To CROP: Reset aspect override first, then set panscan
        MPVLib.setPropertyDouble("video-aspect-override", -1.0)
        MPVLib.setPropertyDouble("panscan", 1.0)
      }
      VideoAspect.Stretch -> {
        // To STRETCH: Calculate screen ratio accounting for video rotation
        @Suppress("DEPRECATION")
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        host.hostWindowManager.defaultDisplay.getRealMetrics(dm)
        
        val isVideoRotated = (_cachedVideoRotation % 180 == 90)
        
        // Calculate screen ratio, inverting if video is rotated
        val screenRatio = if (isVideoRotated) {
          // Video is rotated, so invert the screen ratio
          dm.heightPixels.toDouble() / dm.widthPixels.toDouble()
        } else {
          // Video is not rotated, use normal screen ratio
          dm.widthPixels.toDouble() / dm.heightPixels.toDouble()
        }

        // Set aspect override first, then reset panscan
        // This prevents the brief flash of Fit mode
        MPVLib.setPropertyDouble("video-aspect-override", screenRatio)
        MPVLib.setPropertyDouble("panscan", 0.0)
      }
    }

    // Update the state and persist to preferences
    _videoAspect.value = aspect
    _currentAspectRatio.value = -1.0 // Reset custom ratio when using standard modes
    playerPreferences.defaultVideoAspect.set(aspect)
    playerPreferences.defaultCustomAspectRatio.set(-1.0)

    // Notify the UI
    if (showUpdate) {
      playerUpdate.value = PlayerUpdates.AspectRatio
    }
  }

  fun setCustomAspectRatio(ratio: Double) {
    MPVLib.setPropertyDouble("panscan", 0.0)
    MPVLib.setPropertyDouble("video-aspect-override", ratio)
    _currentAspectRatio.value = ratio
    playerPreferences.defaultCustomAspectRatio.set(ratio)
    playerUpdate.value = PlayerUpdates.AspectRatio
  }

  // ==================== Screen Rotation ====================

  fun cycleScreenRotations() {
    // Temporarily cycle orientation WITHOUT modifying preferences
    // Preferences remain the single source of truth and will be reapplied on next video
    host.hostRequestedOrientation =
      when (host.hostRequestedOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        -> {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
        else -> {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
      }
  }

  // ==================== Lua Invocation Handling ====================

  fun handleLuaInvocation(
    property: String,
    value: String,
  ) {
    val data = value.removeSurrounding("\"").ifEmpty { return }

    when (property.substringAfterLast("/")) {
      "show_text" -> playerUpdate.value = PlayerUpdates.ShowText(data)
      "toggle_ui" -> handleToggleUI(data)
      "show_panel" -> handleShowPanel(data)
      "seek_to_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekToWithText(seekValue.toInt(), text)
      }
      "seek_by_with_text" -> {
        val (seekValue, text) = data.split("|", limit = 2)
        seekByWithText(seekValue.toInt(), text)
      }
      "seek_by" -> seekByWithText(data.toInt(), null)
      "seek_to" -> seekToWithText(data.toInt(), null)
      "software_keyboard" -> handleSoftwareKeyboard(data)
    }

    MPVLib.setPropertyString(property, "")
  }

  private fun handleToggleUI(data: String) {
    when (data) {
      "show" -> showControls()
      "toggle" -> if (controlsShown.value) hideControls() else showControls()
      "hide" -> {
        sheetShown.value = Sheets.None
        panelShown.value = Panels.None
        hideControls()
      }
    }
  }

  private fun handleShowPanel(data: String) {
    when (data) {
      "frame_navigation" -> {
        sheetShown.value = Sheets.FrameNavigation
      }
      else -> {
        panelShown.value =
          when (data) {
            "subtitle_settings" -> Panels.SubtitleSettings
            "subtitle_delay" -> Panels.SubtitleDelay
            "audio_delay" -> Panels.AudioDelay
            "video_filters" -> Panels.VideoFilters
            else -> Panels.None
          }
      }
    }
  }

  private fun handleSoftwareKeyboard(data: String) {
    when (data) {
      "show" -> forceShowSoftwareKeyboard()
      "hide" -> forceHideSoftwareKeyboard()
      "toggle" ->
        if (!inputMethodManager.isActive) {
          forceShowSoftwareKeyboard()
        } else {
          forceHideSoftwareKeyboard()
        }
    }
  }

  @Suppress("DEPRECATION")
  private fun forceShowSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
  }

  @Suppress("DEPRECATION")
  private fun forceHideSoftwareKeyboard() {
    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
  }

  // ==================== Gesture Handling ====================

  private fun executeGestureAction(
    action: SingleActionGesture,
    isLeft: Boolean = false,
    isRight: Boolean = false,
  ) {
    when (action) {
      SingleActionGesture.Seek -> {
        if (isLeft) leftSeek() else if (isRight) rightSeek()
      }
      SingleActionGesture.SubSeek -> {
        if (isLeft) leftSubSeek() else if (isRight) rightSubSeek()
      }
      SingleActionGesture.PlayPause -> pauseUnpause()
      SingleActionGesture.Custom -> {
        viewModelScope.launch(Dispatchers.IO) {
          val keyCode = when {
            isLeft -> CustomKeyCodes.DoubleTapLeft.keyCode
            isRight -> CustomKeyCodes.DoubleTapRight.keyCode
            else -> CustomKeyCodes.DoubleTapCenter.keyCode
          }
          MPVLib.command("keypress", keyCode)
        }
      }
      SingleActionGesture.PlaylistNext -> playNext()
      SingleActionGesture.PlaylistPrev -> playPrevious()
      SingleActionGesture.None -> {}
    }
  }

  fun handleLeftDoubleTap() {
    executeGestureAction(gesturePreferences.leftSingleActionGesture.get(), isLeft = true)
  }

  fun handleCenterDoubleTap() {
    executeGestureAction(gesturePreferences.centerSingleActionGesture.get())
  }

  fun handleCenterSingleTap() {
    executeGestureAction(gesturePreferences.centerSingleActionGesture.get())
  }

  fun handleLeftSingleTap() {
    executeGestureAction(gesturePreferences.leftSingleActionGesture.get(), isLeft = true)
  }

  fun handleRightSingleTap() {
    executeGestureAction(gesturePreferences.rightSingleActionGesture.get(), isRight = true)
  }

  fun handleRightDoubleTap() {
    executeGestureAction(gesturePreferences.rightSingleActionGesture.get(), isRight = true)
  }

  // ==================== Video Zoom ====================

  fun setVideoZoom(zoom: Float) {
    _videoZoom.value = zoom
    MPVLib.setPropertyDouble("video-zoom", zoom.toDouble())
  }

  // Video pan (for pan & zoom feature)
  private val _videoPanX = MutableStateFlow(0f)
  val videoPanX: StateFlow<Float> = _videoPanX.asStateFlow()

  private val _videoPanY = MutableStateFlow(0f)
  val videoPanY: StateFlow<Float> = _videoPanY.asStateFlow()

  fun setVideoPan(x: Float, y: Float) {
    _videoPanX.value = x
    _videoPanY.value = y
    MPVLib.setPropertyDouble("video-pan-x", x.toDouble())
    MPVLib.setPropertyDouble("video-pan-y", y.toDouble())
  }

  fun resetVideoPan() {
    setVideoPan(0f, 0f)
  }

  fun resetVideoZoom() {
    setVideoZoom(0f)
  }

  // ==================== Frame Navigation ====================

  fun updateFrameInfo() {
    _currentFrame.value = MPVLib.getPropertyInt("estimated-frame-number") ?: 0

    val durationValue = MPVLib.getPropertyDouble("duration") ?: 0.0
    val fps =
      MPVLib.getPropertyDouble("container-fps")
        ?: MPVLib.getPropertyDouble("estimated-vf-fps")
        ?: 0.0

    _totalFrames.value =
      if (durationValue > 0 && fps > 0) {
        (durationValue * fps).toInt()
      } else {
        0
      }
  }

  fun toggleFrameNavigationExpanded() {
    val wasExpanded = _isFrameNavigationExpanded.value
    _isFrameNavigationExpanded.update { !it }
    // Update frame info and pause when expanding (going from false to true)
    if (!wasExpanded) {
      // Pause the video if it's playing
      if (paused != true) {
        pauseUnpause()
      }
      updateFrameInfo()
      showFrameInfoOverlay()
      resetFrameNavigationTimer()
    } else {
      // Cancel timer when manually collapsing
      frameNavigationCollapseJob?.cancel()
    }
  }

  private fun showFrameInfoOverlay() {
    playerUpdate.value = PlayerUpdates.FrameInfo(_currentFrame.value, _totalFrames.value)
  }

  fun frameStepForward() {
    viewModelScope.launch(Dispatchers.IO) {
      if (paused != true) {
        pauseUnpause()
        delay(50)
      }
      MPVLib.command("no-osd", "frame-step")
      delay(100)
      updateFrameInfo()
      withContext(Dispatchers.Main) {
        showFrameInfoOverlay()
        // Reset the inactivity timer
        resetFrameNavigationTimer()
      }
    }
  }

  fun frameStepBackward() {
    viewModelScope.launch(Dispatchers.IO) {
      if (paused != true) {
        pauseUnpause()
        delay(50)
      }
      MPVLib.command("no-osd", "frame-back-step")
      delay(100)
      updateFrameInfo()
      withContext(Dispatchers.Main) {
        showFrameInfoOverlay()
        // Reset the inactivity timer
        resetFrameNavigationTimer()
      }
    }
  }

  private var frameNavigationCollapseJob: Job? = null

  fun resetFrameNavigationTimer() {
    frameNavigationCollapseJob?.cancel()
    frameNavigationCollapseJob = viewModelScope.launch {
      delay(10000) // 10 seconds
      if (_isFrameNavigationExpanded.value) {
        _isFrameNavigationExpanded.value = false
      }
    }
  }

  fun takeSnapshot(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      _isSnapshotLoading.value = true
      try {
        val includeSubtitles = playerPreferences.includeSubtitlesInSnapshot.get()

        // Generate filename with timestamp
        val timestamp =
          java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val filename = "mpv_snapshot_$timestamp.png"

        // Create a temporary file first
        val tempFile = File(context.cacheDir, filename)

        // Take screenshot using MPV to temp file, with or without subtitles
        if (includeSubtitles) {
          MPVLib.command("screenshot-to-file", tempFile.absolutePath, "subtitles")
        } else {
          MPVLib.command("screenshot-to-file", tempFile.absolutePath, "video")
        }

        // Wait a bit for MPV to finish writing the file
        delay(200)

        // Check if file was created
        if (!tempFile.exists() || tempFile.length() == 0L) {
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to create screenshot", Toast.LENGTH_SHORT).show()
          }
          return@launch
        }

        // Use different methods based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          // Android 10+ - Use MediaStore with RELATIVE_PATH
          val contentValues =
            android.content.ContentValues().apply {
              put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
              put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
              put(
                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_PICTURES}/mpvSnaps",
              )
              put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }

          val resolver = context.contentResolver
          val imageUri =
            resolver.insert(
              android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
              contentValues,
            )

          if (imageUri != null) {
            // Copy temp file to MediaStore
            resolver.openOutputStream(imageUri)?.use { outputStream ->
              tempFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
              }
            }

            // Mark as finished
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)

            // Delete temp file
            tempFile.delete()

            // Show success toast
            withContext(Dispatchers.Main) {
              Toast
                .makeText(
                  context,
                  context.getString(R.string.player_sheets_frame_navigation_snapshot_saved),
                  Toast.LENGTH_SHORT,
                ).show()
            }
          } else {
            throw Exception("Failed to create MediaStore entry")
          }
        } else {
          // Android 9 and below - Use legacy external storage
          val picturesDir =
            android.os.Environment.getExternalStoragePublicDirectory(
              android.os.Environment.DIRECTORY_PICTURES,
            )
          val snapshotsDir = File(picturesDir, "mpvSnaps")

          // Create directory if it doesn't exist
          if (!snapshotsDir.exists()) {
            val created = snapshotsDir.mkdirs()
            if (!created && !snapshotsDir.exists()) {
              throw Exception("Failed to create mpvSnaps directory")
            }
          }

          val destFile = File(snapshotsDir, filename)
          tempFile.copyTo(destFile, overwrite = true)
          tempFile.delete()

          // Notify media scanner about the new file
          android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(destFile.absolutePath),
            arrayOf("image/png"),
            null,
          )

          withContext(Dispatchers.Main) {
            Toast
              .makeText(
                context,
                context.getString(R.string.player_sheets_frame_navigation_snapshot_saved),
                Toast.LENGTH_SHORT,
              ).show()
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(context, "Failed to save snapshot: ${e.message}", Toast.LENGTH_LONG).show()
        }
      } finally {
        _isSnapshotLoading.value = false
      }
    }
  }

  // ==================== Playlist Management ====================

  fun hasPlaylistSupport(): Boolean {
    val playlistModeEnabled = playerPreferences.playlistMode.get()
    return playlistModeEnabled && _playlistManager.playlist.value.isNotEmpty()
  }

  fun getPlaylistInfo(): String? {
    if (_playlistManager.playlist.value.isEmpty()) return null

    val totalCount = getPlaylistTotalCount()
    return "${_playlistManager.currentIndex.value + 1}/$totalCount"
  }

  fun isPlaylistM3U(): Boolean {
    return _playlistManager.isM3uPlaylist
  }

  fun getPlaylistTotalCount(): Int {
    val totalCount = _playlistManager.playlistTotalCount
    return if (totalCount > 0) totalCount else _playlistManager.playlist.value.size
  }

  fun getPlaylistData(): List<app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistItem>? {
    val activity = host as? PlayerActivity ?: return null
    if (_playlistManager.playlist.value.isEmpty()) return null

    // Get current video progress
    val currentPos = pos ?: 0
    val currentDuration = duration ?: 0
    val currentProgress = if (currentDuration > 0) {
      ((currentPos.toFloat() / currentDuration.toFloat()) * 100f).coerceIn(0f, 100f)
    } else 0f

    return _playlistManager.playlist.value.mapIndexed { index, uri ->
      val title = activity.getPlaylistItemTitle(uri)
      // Path is not used for thumbnail loading - thumbnails are loaded directly from URI
      // Keep it for cache key compatibility
      val path = uri.toString()
      val isCurrentlyPlaying = index == _playlistManager.currentIndex.value

      // Try to get from cache first (synchronized access)
      val cacheKey = uri.toString()
      val (durationStr, resolutionStr) = synchronized(metadataCache) { metadataCache[cacheKey] } ?: ("" to "")

      app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistItem(
        uri = uri,
        title = title,
        index = index + _playlistManager.playlistWindowOffset,
        isPlaying = isCurrentlyPlaying,
        path = path,
        progressPercent = if (isCurrentlyPlaying) currentProgress else 0f,
        isWatched = isCurrentlyPlaying && currentProgress >= 95f,
        duration = durationStr,
        resolution = resolutionStr,
      )
    }
  }

  private fun getVideoMetadata(uri: Uri): Pair<String, String> {
    // Skip metadata extraction for network streams and M3U playlists
    if (uri.scheme?.startsWith("http") == true || uri.scheme == "rtmp" || uri.scheme == "ftp" || uri.scheme == "rtsp" || uri.scheme == "mms") {
      return "" to ""
    }

    // Skip M3U/M3U8 files
    val uriString = uri.toString().lowercase()
    if (uriString.contains(".m3u8") || uriString.contains(".m3u")) {
      return "" to ""
    }

    // Try MediaStore first (much faster - uses cached values)
    val mediaStoreMetadata = getVideoMetadataFromMediaStore(uri)
    if (mediaStoreMetadata != null) {
      return mediaStoreMetadata
    }

    // Fallback to MediaMetadataRetriever only if MediaStore fails
    val retriever = android.media.MediaMetadataRetriever()
    return try {
      // For file:// URIs, use the path directly (faster)
      if (uri.scheme == "file") {
        retriever.setDataSource(uri.path)
      } else {
        // For content:// URIs, use context
        retriever.setDataSource(host.context, uri)
      }

      // Get duration
      val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
      val durationStr = if (durationMs != null) {
        formatDuration(durationMs.toLong())
      } else ""

      // Get resolution
      val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
      val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
      val resolutionStr = if (width != null && height != null) {
        "${width}x${height}"
      } else ""

      durationStr to resolutionStr
    } catch (e: Exception) {
      android.util.Log.e("PlayerViewModel", "Failed to get video metadata for $uri", e)
      "" to ""
    } finally {
      try {
        retriever.release()
      } catch (e: Exception) {
        // Ignore release errors
      }
    }
  }

  /**
   * Get video metadata from MediaStore (fast - uses cached system values).
   * Returns null if the video is not found in MediaStore.
   */
  private fun getVideoMetadataFromMediaStore(uri: Uri): Pair<String, String>? {
    return try {
      val projection = arrayOf(
        android.provider.MediaStore.Video.Media.DURATION,
        android.provider.MediaStore.Video.Media.WIDTH,
        android.provider.MediaStore.Video.Media.HEIGHT,
        android.provider.MediaStore.Video.Media.DATA
      )

      // Determine the query URI based on the input URI scheme
      val queryUri = when (uri.scheme) {
        "content" -> {
          // If it's already a content URI, use it directly
          if (uri.toString().startsWith(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())) {
            uri
          } else {
            // Try to find by path if available
            null
          }
        }
        "file" -> {
          // For file:// URIs, query by path
          null
        }
        else -> null
      }

      // Query by URI if we have a content URI
      if (queryUri != null) {
        host.context.contentResolver.query(
          queryUri,
          projection,
          null,
          null,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val durationColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.HEIGHT)

            val durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
            val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
            val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0

            val durationStr = formatDuration(durationMs)

            val resolutionStr = if (width > 0 && height > 0) {
              "${width}x${height}"
            } else ""

            return durationStr to resolutionStr
          }
        }
      }

      // Query by file path if we have a file:// URI or content URI without direct match
      val filePath = when (uri.scheme) {
        "file" -> uri.path
        "content" -> {
          // Try to get the file path from content URI
          host.context.contentResolver.query(
            uri,
            arrayOf(android.provider.MediaStore.Video.Media.DATA),
            null,
            null,
            null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val dataColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
              if (dataColumn >= 0) cursor.getString(dataColumn) else null
            } else null
          }
        }
        else -> null
      }

      if (filePath != null) {
        val selection = "${android.provider.MediaStore.Video.Media.DATA} = ?"
        val selectionArgs = arrayOf(filePath)

        host.context.contentResolver.query(
          android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          selectionArgs,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val durationColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DURATION)
            val widthColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.HEIGHT)

            val durationMs = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
            val width = if (widthColumn >= 0) cursor.getInt(widthColumn) else 0
            val height = if (heightColumn >= 0) cursor.getInt(heightColumn) else 0

            val durationStr = formatDuration(durationMs)

            val resolutionStr = if (width > 0 && height > 0) {
              "${width}x${height}"
            } else ""

            return durationStr to resolutionStr
          }
        }
      }

      null
    } catch (e: Exception) {
      android.util.Log.w("PlayerViewModel", "Failed to get metadata from MediaStore for $uri, will try MediaMetadataRetriever", e)
      null
    }
  }

  /**
   * Format duration in milliseconds to hh:mm:ss or mm:ss format
   */
  private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return ""
    
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
      String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format("%d:%02d", minutes, seconds)
    }
  }
  
  

  fun playPlaylistItem(index: Int) {
    val activity = host as? PlayerActivity ?: return
    activity.playPlaylistItem(index)
  }

  /**
   * Refreshes the playlist items to update the currently playing indicator.
   * Called when a new video starts playing to update the playlist UI.
   */
  fun refreshPlaylistItems() {
    viewModelScope.launch(Dispatchers.IO) {
      val updatedItems = getPlaylistData()
      if (updatedItems != null) {
        // Clear cache if playlist size changed
        if (_playlistItems.value.size != updatedItems.size) {
          metadataCache.evictAll()
        }

        _playlistItems.value = updatedItems

        // Load metadata asynchronously in the background
        loadPlaylistMetadataAsync(updatedItems)
      }
    }
  }

  /**
   * Loads metadata for all playlist items asynchronously in the background.
   * Updates the playlist items as metadata becomes available.
   * Uses batched updates to avoid O(n²) complexity with large playlists.
   * Skips metadata extraction for M3U playlists (network streams).
   */
  private fun loadPlaylistMetadataAsync(items: List<app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistItem>) {
    viewModelScope.launch(Dispatchers.IO) {
      // Skip metadata extraction for M3U playlists
      val activity = host as? PlayerActivity
      if (activity?.isCurrentPlaylistM3U() == true) {
        Log.d(TAG, "Skipping metadata extraction for M3U playlist")
        return@launch
      }

      // Limit concurrent metadata extraction to avoid overwhelming resources
      val batchSize = 5
      items.chunked(batchSize).forEach { batch ->
        val updates = mutableMapOf<String, Pair<String, String>>()

        // Extract metadata for the batch
        batch.forEach { item ->
          val cacheKey = item.uri.toString()

          // Skip if already in cache (LruCache is thread-safe)
          if (metadataCache.get(cacheKey) == null) {
            // Extract metadata
            val (durationStr, resolutionStr) = getVideoMetadata(item.uri)

            // Update cache and track update
            updateMetadataCache(cacheKey, durationStr to resolutionStr)
            updates[cacheKey] = durationStr to resolutionStr
          }
        }

        // Apply all batched updates at once (single playlist update)
        if (updates.isNotEmpty()) {
          _playlistItems.value = _playlistItems.value.map { currentItem ->
            val cacheKey = currentItem.uri.toString()
            val (durationStr, resolutionStr) = updates[cacheKey] ?: return@map currentItem
            currentItem.copy(duration = durationStr, resolution = resolutionStr)
          }
        }
      }
    }
  }

  fun hasNext(): Boolean = _playlistManager.hasNext(shouldRepeatPlaylist())

  fun hasPrevious(): Boolean = _playlistManager.hasPrevious(shouldRepeatPlaylist())

  fun playNext() {
    val nextIndex = _playlistManager.getNextIndex(shouldRepeatPlaylist())
    if (nextIndex != null) {
      (host as? PlayerActivity)?.loadPlaylistItem(nextIndex)
    }
  }

  fun playPrevious() {
    val prevIndex = _playlistManager.getPreviousIndex(shouldRepeatPlaylist())
    if (prevIndex != null) {
      (host as? PlayerActivity)?.loadPlaylistItem(prevIndex)
    }
  }

  // ==================== Repeat and Shuffle ====================

  fun applyPersistedShuffleState() {
    if (_shuffleEnabled.value) {
      _playlistManager.setShuffleEnabled(true)
    }
  }

  fun cycleRepeatMode() {
    val hasPlaylist = _playlistManager.playlist.value.isNotEmpty()

    _repeatMode.value = when (_repeatMode.value) {
      RepeatMode.OFF -> RepeatMode.ONE
      RepeatMode.ONE -> if (hasPlaylist) RepeatMode.ALL else RepeatMode.OFF
      RepeatMode.ALL -> RepeatMode.OFF
    }

    // Persist the repeat mode
    playerPreferences.repeatMode.set(_repeatMode.value)

    // Show overlay update instead of toast
    playerUpdate.value = PlayerUpdates.RepeatMode(_repeatMode.value)
  }

  fun toggleShuffle() {
    _shuffleEnabled.value = !_shuffleEnabled.value

    // Persist the shuffle state
    playerPreferences.shuffleEnabled.set(_shuffleEnabled.value)

    // Notify manager to handle shuffle state change
    _playlistManager.setShuffleEnabled(_shuffleEnabled.value)

    // Show overlay update instead of toast
    playerUpdate.value = PlayerUpdates.Shuffle(_shuffleEnabled.value)
  }

  fun shouldRepeatCurrentFile(): Boolean {
    return _repeatMode.value == RepeatMode.ONE ||
      (_repeatMode.value == RepeatMode.ALL && _playlistManager.playlist.value.isEmpty())
  }

  fun shouldRepeatPlaylist(): Boolean {
    return _repeatMode.value == RepeatMode.ALL && _playlistManager.playlist.value.isNotEmpty()
  }

  // ==================== A-B Loop ====================

  fun toggleABLoopExpanded() {
    _isABLoopExpanded.update { !it }
  }

  fun setLoopA() {
    if (_abLoopA.value != null) {
      // Toggle off - clear point A
      _abLoopA.value = null
      MPVLib.setPropertyString("ab-loop-a", "no")
      return
    }

    val currentPos = MPVLib.getPropertyDouble("time-pos") ?: return
    _abLoopA.value = currentPos
    MPVLib.setPropertyDouble("ab-loop-a", currentPos)
  }

  fun setLoopB() {
    if (_abLoopB.value != null) {
      // Toggle off - clear point B
      _abLoopB.value = null
      MPVLib.setPropertyString("ab-loop-b", "no")
      return
    }

    val currentPos = MPVLib.getPropertyDouble("time-pos") ?: return
    _abLoopB.value = currentPos
    MPVLib.setPropertyDouble("ab-loop-b", currentPos)
  }

  fun clearABLoop() {
    val hadLoop = _abLoopA.value != null || _abLoopB.value != null
    _abLoopA.value = null
    _abLoopB.value = null
    MPVLib.setPropertyString("ab-loop-a", "no")
    MPVLib.setPropertyString("ab-loop-b", "no")
  }

  fun formatTimestamp(seconds: Double): String {
    val totalSec = seconds.toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
  }

  // ==================== Mirroring ====================

  fun toggleMirroring() {
    val newMirrorState = !_isMirrored.value
    _isMirrored.value = newMirrorState
    
    // Use labeled video filter for mirroring to avoid state desync
    if (newMirrorState) {
      MPVLib.command("vf", "add", "@mpvex_hflip:hflip")
    } else {
      MPVLib.command("vf", "remove", "@mpvex_hflip")
    }
    playerUpdate.value = PlayerUpdates.ShowText(if (newMirrorState) "H-Flip On" else "H-Flip Off")
  }

  fun toggleVerticalFlip() {
    val newState = !_isVerticalFlipped.value
    _isVerticalFlipped.value = newState

    // Use labeled video filter for vflip to avoid state desync
    if (newState) {
      MPVLib.command("vf", "add", "@mpvex_vflip:vflip")
    } else {
      MPVLib.command("vf", "remove", "@mpvex_vflip")
    }

    playerUpdate.value = PlayerUpdates.ShowText(if (newState) "V-Flip On" else "V-Flip Off")
  }

  // ==================== Ambient Mode Integration ====================

  fun toggleAmbientMode() = ambientModeManager.toggleAmbientMode()

  fun onOrientationChanged(isPortrait: Boolean) = ambientModeManager.onOrientationChanged(isPortrait)

  fun resetAmbientMode() = ambientModeManager.resetAmbientMode()

  fun restartAmbientIfActive() = ambientModeManager.restartAmbientIfActive()

  fun updateAmbientStretch() = ambientModeManager.updateAmbientStretch()

  // ==================== Utility ====================

  fun showToast(message: String) {
    Toast.makeText(host.context, message, Toast.LENGTH_SHORT).show()
  }

  override fun onCleared() {
    super.onCleared()
    ambientModeManager.cleanup()
  }
}

// Extension functions
fun Float.normalize(
  inMin: Float,
  inMax: Float,
  outMin: Float,
  outMax: Float,
): Float = (this - inMin) * (outMax - outMin) / (inMax - inMin) + outMin

fun <T> Flow<T>.collectAsState(
  scope: CoroutineScope,
  initialValue: T? = null,
) = object : ReadOnlyProperty<Any?, T?> {
  private var value: T? = initialValue

  init {
    scope.launch { collect { value = it } }
  }

  override fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ) = value
}
