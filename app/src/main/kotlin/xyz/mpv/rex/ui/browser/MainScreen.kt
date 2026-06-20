package xyz.mpv.rex.ui.browser

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
import androidx.compose.material.icons.filled.FeaturedPlayList
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
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.ui.browser.folderlist.FolderListScreen
import xyz.mpv.rex.ui.browser.networkstreaming.NetworkStreamingScreen
import xyz.mpv.rex.ui.browser.playlist.PlaylistScreen
import xyz.mpv.rex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import xyz.mpv.rex.ui.browser.shorts.ShortsScreen
import xyz.mpv.rex.ui.browser.selection.SelectionManager
import xyz.mpv.rex.cinemine.ui.CineMineScreen // Premium CineMine Screen integration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

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

  @Volatile
  private var isInSelectionModeShared: Boolean = false  
  
  @Volatile
  private var shouldHideNavigationBar: Boolean = false  
  
  @Volatile
  private var isBrowserBottomBarVisible: Boolean = false  
  
  @Volatile
  private var sharedVideoSelectionManager: Any? = null
  
  @Volatile
  private var onlyVideosSelected: Boolean = false
  
  @Volatile
  private var isPermissionDenied: Boolean = false
  
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    this.isInSelectionModeShared = isInSelectionMode
    this.onlyVideosSelected = isOnlyVideosSelected
    this.sharedVideoSelectionManager = selectionManager
    this.shouldHideNavigationBar = isInSelectionMode && isOnlyVideosSelected
  }
  
  fun updatePermissionState(isDenied: Boolean) {
    this.isPermissionDenied = isDenied
  }

  fun getPermissionDeniedState(): Boolean = isPermissionDenied

  fun updateBottomBarVisibility(shouldShow: Boolean) {
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
        // --- FIXED PIPELINE WIRING: Replaced old Features tab with CineMine Core Matrix Node ---
        add(
          VisibleTab("cinemine", "CineMine", Icons.Filled.FeaturedPlayList) {
            CineMineScreen(
              onPlayRequested = { streamUrl, streamTitle ->
                // Direct high-performance interface navigation intent mapping for active local media playback
                val intent = android.content.Intent(context, xyz.mpv.rex.ui.player.PlayerActivity::class.java).apply {
                    putExtra("playlist", arrayOf(streamUrl))
                    putExtra("title", streamTitle)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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

    LaunchedEffect(visibleTabs) {
      if (selectedTab >= visibleTabs.size) {
        selectedTab = 0
      }
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
        if (isInSelectionMode.value != isInSelectionModeShared) {
          isInSelectionMode.value = isInSelectionModeShared
        }
        if (hideNavigationBar.value != shouldHideNavigationBar) {
          hideNavigationBar.value = shouldHideNavigationBar
        }
        val currentManager = sharedVideoSelectionManager as? SelectionManager<*, *>
        if (videoSelectionManager.value != currentManager) {
          videoSelectionManager.value = currentManager
        }
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
      tabRequest.collect { tab ->
        selectedTab = tab
      }
    }

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      bottomBar = {
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

val LocalNavigationBarHeight = compositionLocalOf { 0.dp }

private data class VisibleTab(
  val id: String,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val content: @Composable () -> Unit
)
