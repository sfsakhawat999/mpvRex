package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import app.marlboroadvance.mpvex.ui.browser.shorts.ShortsScreen
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.cinehub.ui.CineHubScreen
import app.marlboroadvance.mpvex.cinehub.data.NfoScanner
import app.marlboroadvance.mpvex.cinehub.model.MovieItem
import app.marlboroadvance.mpvex.cinehub.model.TvShowItem
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import android.content.Intent
import android.net.Uri
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Serializable
object MainScreen : Screen {
  private var persistentSelectedTab: Int = 0
  private var persistentPreviousTab: Int = 0
  
  private val _tabRequest = MutableSharedFlow<Int>(extraBufferCapacity = 1)
  val tabRequest = _tabRequest.asSharedFlow()

  private val _scrollToTopRequest = MutableSharedFlow<String>(extraBufferCapacity = 1)
  val scrollToTopRequest = _scrollToTopRequest.asSharedFlow()

  fun requestTab(tab: Int) {
    _tabRequest.tryEmit(tab)
  }
  
  fun requestPreviousTab() {
    _tabRequest.tryEmit(persistentPreviousTab)
  }

  @Volatile private var isInSelectionModeShared: Boolean = false  
  @Volatile private var shouldHideNavigationBar: Boolean = false  
  @Volatile private var isBrowserBottomBarVisible: Boolean = false  
  @Volatile private var sharedVideoSelectionManager: Any? = null
  @Volatile private var onlyVideosSelected: Boolean = false
  @Volatile private var isPermissionDenied: Boolean = false
  
  fun updateSelectionState(isInSelectionMode: Boolean, isOnlyVideosSelected: Boolean, selectionManager: Any?) {
    this.isInSelectionModeShared = isInSelectionMode
    this.onlyVideosSelected = isOnlyVideosSelected
    this.sharedVideoSelectionManager = selectionManager
    this.shouldHideNavigationBar = isInSelectionMode && isOnlyVideosSelected
  }
  
  fun updatePermissionState(isDenied: Boolean) { this.isPermissionDenied = isDenied }
  fun getPermissionDeniedState(): Boolean = isPermissionDenied
  fun updateBottomBarVisibility(shouldShow: Boolean) { this.shouldHideNavigationBar = !shouldShow }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by remember { mutableIntStateOf(persistentSelectedTab) }
    var previousTab by remember { mutableIntStateOf(persistentPreviousTab) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val isShortsEnabled by browserPreferences.enableShorts.collectAsState()
    val enableTabRecents by browserPreferences.enableTabRecents.collectAsState()
    val enableTabPlaylists by browserPreferences.enableTabPlaylists.collectAsState()
    val enableTabNetwork by browserPreferences.enableTabNetwork.collectAsState()
    
    // Live collecting toggle state value from data layer preferences configuration mapping
    val enableCineHub by browserPreferences.enableCineHub.collectAsState()

    // Persistent storage states to keep metadata memory footprints minimal and instant
    var cachedMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var cachedTvShows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }

    // Thread offloading: Shifts heavy file tree searching onto background IO routine pools
    LaunchedEffect(Unit) {
      withContext(Dispatchers.IO) {
        val rootDir = android.os.Environment.getExternalStorageDirectory()
        try {
          val movies = NfoScanner.scanDirectoryForMovies(rootDir)
          val tvShows = NfoScanner.scanDirectoryForTvShows(rootDir)
          withContext(Dispatchers.Main) {
            cachedMovies = movies
            cachedTvShows = tvShows
          }
        } catch (e: Exception) {
          android.util.Log.e("CineHubInit", "Failed to compile asynchronous storage assets structure", e)
        }
      }
    }

    val visibleTabs = remember(isShortsEnabled, enableTabRecents, enableTabPlaylists, enableTabNetwork, enableCineHub, cachedMovies, cachedTvShows) {
      buildList {
        add(
          VisibleTab("home", "Home", Icons.Filled.Home) {
            FolderListScreen.Content()
          }
        )
        
        // --- UPDATED: CineHub tab wrapping bounded via conditional global state key checks ---
        if (enableCineHub) {
          add(
            VisibleTab("cinehub", "CineHub", Icons.Filled.Movie) {
              CineHubScreen(
                moviesList = cachedMovies,
                tvShowsList = cachedTvShows,
                onPlayRequested = { filePath, cleanTitle ->
                  val intent = Intent(context, PlayerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.fromFile(File(filePath))
                    putExtra("title", cleanTitle)
                    putExtra("force_title", cleanTitle)
                  }
                  context.startActivity(intent)
                }
              )
            }
          )
        }
        
        if (isShortsEnabled) {
          add(VisibleTab("shorts", "Shorts", Icons.Filled.VideoLibrary) { ShortsScreen().Content() })
        }
        if (enableTabRecents) {
          add(VisibleTab("recents", "Recents", Icons.Filled.History) { RecentlyPlayedScreen.Content() })
        }
        if (enableTabPlaylists) {
          add(VisibleTab("playlists", "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay) { PlaylistScreen.Content() })
        }
        if (enableTabNetwork) {
          add(VisibleTab("network", "Network", Icons.Filled.Language) { NetworkStreamingScreen.Content() })
        }
      }
    }

    LaunchedEffect(visibleTabs) {
      if (selectedTab >= visibleTabs.size) { selectedTab = 0 }
    }

    val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
    androidx.activity.compose.BackHandler(enabled = shortsIdx != -1 && selectedTab == shortsIdx) {
      selectedTab = previousTab
    }

    val isInSelectionMode = remember { mutableStateOf(isInSelectionModeShared) }
    val hideNavigationBar = remember { mutableStateOf(shouldHideNavigationBar) }
    val videoSelectionManager = remember { mutableStateOf<SelectionManager<*, *>?>(sharedVideoSelectionManager as? SelectionManager<*, *>) }
    
    LaunchedEffect(Unit) {
      while (true) {
        if (isInSelectionMode.value != isInSelectionModeShared) { isInSelectionMode.value = isInSelectionModeShared }
        if (hideNavigationBar.value != shouldHideNavigationBar) { hideNavigationBar.value = shouldHideNavigationBar }
        val currentManager = sharedVideoSelectionManager as? SelectionManager<*, *>
        if (videoSelectionManager.value != currentManager) { videoSelectionManager.value = currentManager }
        delay(16)
      }
    }
    
    LaunchedEffect(selectedTab) {
      if (selectedTab != persistentSelectedTab) {
        previousTab = persistentSelectedTab
        persistentPreviousTab = previousTab
      }
      persistentSelectedTab = selectedTab
    }

    LaunchedEffect(Unit) {
      tabRequest.collect { tab -> selectedTab = tab }
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
        val isShortsTabActive = isShortsEnabled && shortsIdx != -1 && selectedTab == shortsIdx
        
        AnimatedVisibility(
          visible = !hideNavigationBar.value && !isShortsTabActive && visibleTabs.size > 1,
          enter = slideInVertically(animationSpec = tween(300), initialOffsetY = { it }),
          exit = slideOutVertically(animationSpec = tween(300), targetOffsetY = { it })
        ) {
          NavigationBar(
            modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            containerColor = if (isShortsTabActive) Color.Transparent else NavigationBarDefaults.containerColor,
            contentColor = if (isShortsTabActive) Color.White else MaterialTheme.colorScheme.onSurface,
          ) {
            val itemColors = if (isShortsTabActive) {
              NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White, selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(0.7f), unselectedTextColor = Color.White.copy(0.7f),
                indicatorColor = Color.White.copy(0.2f)
              )
            } else {
              NavigationBarItemDefaults.colors()
            }

            visibleTabs.forEachIndexed { index, tab ->
              NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = selectedTab == index,
                onClick = {
                  if (selectedTab == index) {
                    _scrollToTopRequest.tryEmit(tab.id)
                  } else {
                    selectedTab = index
                  }
                },
                colors = itemColors
              )
            }
          }
        }
      }
    ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize()) {
        val fabBottomPadding = 80.dp

        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            val slideDistance = with(density) { 48.dp.roundToPx() }
            val animationDuration = 250
            if (targetState > initialState) {
              (slideInHorizontally(tween(animationDuration, easing = FastOutSlowInEasing)) { slideDistance } + fadeIn(tween(animationDuration, easing = FastOutSlowInEasing))) togetherWith 
              (slideOutHorizontally(tween(animationDuration, easing = FastOutSlowInEasing)) { -slideDistance } + fadeOut(tween(animationDuration / 2, easing = FastOutSlowInEasing)))
            } else {
              (slideInHorizontally(tween(animationDuration, easing = FastOutSlowInEasing)) { -slideDistance } + fadeIn(tween(animationDuration, easing = FastOutSlowInEasing))) togetherWith 
              (slideOutHorizontally(tween(animationDuration, easing = FastOutSlowInEasing)) { slideDistance } + fadeOut(tween(animationDuration / 2, easing = FastOutSlowInEasing)))
            }
          },
          label = "tab_animation"
        ) { targetTab ->
          val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
          val isShortsTabActive = isShortsEnabled && shortsIdx != -1 && selectedTab == shortsIdx
          val isNavBarVisible = !hideNavigationBar.value && !isShortsTabActive && visibleTabs.size > 1
          
          CompositionLocalProvider(LocalNavigationBarHeight provides if (isNavBarVisible) fabBottomPadding else 0.dp) {
            if (targetTab in visibleTabs.indices) {
              visibleTabs[targetTab].content()
            } else {
              FolderListScreen.Content()
            }
          }
        }
      }
    }
  }
}

val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class VisibleTab(
  val id: String,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val content: @Composable () -> Unit
)
