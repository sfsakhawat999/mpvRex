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
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import android.content.Intent
import android.net.Uri
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
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

  // Shared state that can be updated by FileSystemBrowserScreen
  @Volatile
  private var isInSelectionModeShared: Boolean = false  // Controls FAB visibility
  
  @Volatile
  private var shouldHideNavigationBar: Boolean = false  // Controls navigation bar visibility
  
  @Volatile
  private var isBrowserBottomBarVisible: Boolean = false  // Tracks browser bottom bar visibility
  
  @Volatile
  private var sharedVideoSelectionManager: Any? = null
  
  // Check if the selection contains only videos and update navigation bar visibility accordingly
  @Volatile
  private var onlyVideosSelected: Boolean = false
  
  // Track when permission denied screen is showing to hide FAB
  @Volatile
  private var isPermissionDenied: Boolean = false
  
  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    this.isInSelectionModeShared = isInSelectionMode
    this.onlyVideosSelected = isOnlyVideosSelected
    this.sharedVideoSelectionManager = selectionManager
    
    // Only hide navigation bar when videos are selected AND in selection mode
    // This fixes the issue where bottom bar disappears when only videos are selected
    this.shouldHideNavigationBar = isInSelectionMode && isOnlyVideosSelected
  }
  
  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    this.isPermissionDenied = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = isPermissionDenied

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    // Hide bottom navigation when floating bottom bar is visible
    this.shouldHideNavigationBar = !shouldShow
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    var selectedTab by remember {
      mutableIntStateOf(persistentSelectedTab)
    }
    
    var previousTab by remember {
      mutableIntStateOf(persistentPreviousTab)
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val isShortsEnabled by browserPreferences.enableShorts.collectAsState()
    val enableTabRecents by browserPreferences.enableTabRecents.collectAsState()
    val enableTabPlaylists by browserPreferences.enableTabPlaylists.collectAsState()
    val enableTabNetwork by browserPreferences.enableTabNetwork.collectAsState()

    val visibleTabs = remember(isShortsEnabled, enableTabRecents, enableTabPlaylists, enableTabNetwork) {
      buildList {
        add(
          VisibleTab("home", "Home", Icons.Filled.Home) {
            FolderListScreen.Content()
          }
        )
        
        // --- UPDATED: CineHub Integration with both Movies and TV Shows scanning ---
        add(
          VisibleTab("cinehub", "CineHub", Icons.Filled.Movie) {
            val rootDir = remember { android.os.Environment.getExternalStorageDirectory() }
            val moviesData = remember {
              try {
                NfoScanner.scanDirectoryForMovies(rootDir)
              } catch (e: Exception) {
                emptyList()
              }
            }
            val tvShowsData = remember {
              try {
                NfoScanner.scanDirectoryForTvShows(rootDir)
              } catch (e: Exception) {
                emptyList()
              }
            }
            
            CineHubScreen(
              moviesList = moviesData,
              tvShowsList = tvShowsData,
              onPlayRequested = { filePath, cleanTitle ->
                val intent = Intent(context, PlayerActivity::class.java).apply {
                  action = Intent.ACTION_VIEW
                  data = Uri.fromFile(File(filePath))
                  // Passes clean titles to avoid showing raw filename.mkv inside the player
                  putExtra("title", cleanTitle)
                  putExtra("force_title", cleanTitle)
                }
                context.startActivity(intent)
              }
            )
          }
        )
        
        if (isShortsEnabled) {
          add(
            VisibleTab("shorts", "Shorts", Icons.Filled.VideoLibrary) {
              ShortsScreen().Content()
            }
          )
        }
        if (enableTabRecents) {
          add(
            VisibleTab("recents", "Recents", Icons.Filled.History) {
              RecentlyPlayedScreen.Content()
            }
          )
        }
        if (enableTabPlaylists) {
          add(
            VisibleTab("playlists", "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay) {
              PlaylistScreen.Content()
            }
          )
        }
        if (enableTabNetwork) {
          add(
            VisibleTab("network", "Network", Icons.Filled.Language) {
              NetworkStreamingScreen.Content()
            }
          )
        }
      }
    }

    // Ensure selectedTab is always clamped within active tabs range
    LaunchedEffect(visibleTabs) {
      if (selectedTab >= visibleTabs.size) {
        selectedTab = 0
      }
    }

    // Intercept back button when on Shorts tab to return to previous tab
    val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
    androidx.activity.compose.BackHandler(enabled = shortsIdx != -1 && selectedTab == shortsIdx) {
      selectedTab = previousTab
    }

    // Shared state (across the app)
    val isInSelectionMode = remember { mutableStateOf(isInSelectionModeShared) }
    val hideNavigationBar = remember { mutableStateOf(shouldHideNavigationBar) }
    val videoSelectionManager = remember { mutableStateOf<SelectionManager<*, *>?>(sharedVideoSelectionManager as? SelectionManager<*, *>) }
    
    // Check for state changes to ensure UI updates
    LaunchedEffect(Unit) {
      while (true) {
        // Update FAB visibility state
        if (isInSelectionMode.value != isInSelectionModeShared) {
          isInSelectionMode.value = isInSelectionModeShared
          android.util.Log.d("MainScreen", "Selection mode changed to: $isInSelectionModeShared")
        }
        
        // Update navigation bar visibility state - now considers if only videos are selected
        if (hideNavigationBar.value != shouldHideNavigationBar) {
          hideNavigationBar.value = shouldHideNavigationBar
          android.util.Log.d("MainScreen", "Navigation bar visibility changed to: ${!shouldHideNavigationBar}, onlyVideosSelected: $onlyVideosSelected")
        }
        
        // Update selection manager
        val currentManager = sharedVideoSelectionManager as? SelectionManager<*, *>
        if (videoSelectionManager.value != currentManager) {
          videoSelectionManager.value = currentManager
        }
        
        // Minimal delay for polling
        delay(16) // Roughly matches a frame at 60fps for responsive updates
      }
    }
    
    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      if (selectedTab != persistentSelectedTab) {
        previousTab = persistentSelectedTab
        persistentPreviousTab = previousTab
      }
      android.util.Log.d("MainScreen", "selectedTab changed to: $selectedTab (was ${persistentSelectedTab}), previousTab is $previousTab")
      persistentSelectedTab = selectedTab
    }

    // Handle tab requests from other screens
    LaunchedEffect(Unit) {
      tabRequest.collect { tab ->
        selectedTab = tab
      }
    }

    // Scaffold with bottom navigation bar
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
        // Animated bottom navigation bar with slide animations
        // Also hide if Shorts tab is active (index 1 when enabled, index -1 when disabled)
        val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
        val isShortsTabActive = isShortsEnabled && shortsIdx != -1 && selectedTab == shortsIdx
        
        AnimatedVisibility(
          visible = !hideNavigationBar.value && !isShortsTabActive && visibleTabs.size > 1,
          enter = slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { fullHeight -> fullHeight }
          ),
          exit = slideOutVertically(
            animationSpec = tween(durationMillis = 300),
            targetOffsetY = { fullHeight -> fullHeight }
          )
        ) {
          NavigationBar(
            modifier = Modifier
              .clip(
                RoundedCornerShape(
                  topStart = 28.dp,
                  topEnd = 28.dp,
                  bottomStart = 0.dp,
                  bottomEnd = 0.dp
                )
              ),
            containerColor = if (isShortsTabActive) Color.Transparent else NavigationBarDefaults.containerColor,
            contentColor = if (isShortsTabActive) Color.White else MaterialTheme.colorScheme.onSurface,
          ) {
            val itemColors = if (isShortsTabActive) {
              NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                indicatorColor = Color.White.copy(alpha = 0.2f)
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
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { -slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            } else {
              (slideInHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                initialOffsetX = { -slideDistance }
              ) + fadeIn(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                )
              )) togetherWith (slideOutHorizontally(
                animationSpec = tween(
                  durationMillis = animationDuration,
                  easing = FastOutSlowInEasing
                ),
                targetOffsetX = { slideDistance }
              ) + fadeOut(
                animationSpec = tween(
                  durationMillis = animationDuration / 2,
                  easing = FastOutSlowInEasing
                )
              ))
            }
          },
          label = "tab_animation"
        ) { targetTab ->
          val shortsIdx = visibleTabs.indexOfFirst { it.id == "shorts" }
          val isShortsTabActive = isShortsEnabled && shortsIdx != -1 && selectedTab == shortsIdx
          val isNavBarVisible = !hideNavigationBar.value && !isShortsTabActive && visibleTabs.size > 1
          
          CompositionLocalProvider(
            LocalNavigationBarHeight provides if (isNavBarVisible) fabBottomPadding else 0.dp
          ) {
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

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class VisibleTab(
  val id: String,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val content: @Composable () -> Unit
)
