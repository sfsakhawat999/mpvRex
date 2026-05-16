package app.marlboroadvance.mpvex.ui.browser.medialibrary

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoListContent
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoSortDialog
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoWithPlaybackInfo
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryContent() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val backstack = LocalBackStack.current
  val browserPreferences = koinInject<BrowserPreferences>()
  val playerPreferences = koinInject<PlayerPreferences>()
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current

  // ViewModel
  val viewModel: MediaLibraryViewModel = viewModel(
    factory = MediaLibraryViewModel.factory(context.applicationContext as android.app.Application)
  )
  val videos by viewModel.videos.collectAsState()
  val videosWithPlaybackInfo by viewModel.videosWithPlaybackInfo.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val uiSettings by viewModel.uiSettings.collectAsState()
  val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()

  // Sorting
  val videoSortType by browserPreferences.videoSortType.collectAsState()
  val videoSortOrder by browserPreferences.videoSortOrder.collectAsState()
  val sortedVideosWithInfo = remember(videosWithPlaybackInfo, videoSortType, videoSortOrder) {
    val infoById = videosWithPlaybackInfo.associateBy { it.video.path }
    val sortedVideos = SortUtils.sortVideos(videosWithPlaybackInfo.map { it.video }, videoSortType, videoSortOrder)
    sortedVideos.map { video ->
      infoById[video.path] ?: VideoWithPlaybackInfo(video)
    }
  }

  // Selection manager
  val selectionManager = rememberSelectionManager(
    items = sortedVideosWithInfo.map { it.video },
    getId = { it.path.hashCode().toLong() },
    onDeleteItems = { items, _ -> 
      coroutineScope.launch { viewModel.deleteVideos(items) }
      Pair(items.size, 0)
    },
    onRenameItem = { video, newName -> 
      coroutineScope.launch { viewModel.renameVideo(video, newName) }
      Result.success(Unit)
    },
    onOperationComplete = { viewModel.refresh() },
  )

  // UI State
  val isRefreshing = remember { mutableStateOf(false) }
  val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
  val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
  val renameDialogOpen = rememberSaveable { mutableStateOf(false) }

  // Search state
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var isSearching by rememberSaveable { mutableStateOf(false) }

  // FAB visibility state
  val isFabVisible = remember { mutableStateOf(true) }

  // Bottom bar animation state
  var showFloatingBottomBar by remember { mutableStateOf(false) }
  val animationDuration = 300

  LaunchedEffect(selectionManager.isInSelectionMode) {
    showFloatingBottomBar = selectionManager.isInSelectionMode
  }

  val shouldHandleBack = selectionManager.isInSelectionMode || isSearching
  BackHandler(enabled = shouldHandleBack) {
    when {
      selectionManager.isInSelectionMode -> selectionManager.clear()
      isSearching -> {
        isSearching = false
        searchQuery = ""
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Scaffold(
    topBar = {
      if (isSearching) {
        SearchBar(
          inputField = {
            SearchBarDefaults.InputField(
              query = searchQuery,
              onQueryChange = { searchQuery = it },
              onSearch = { },
              expanded = false,
              onExpandedChange = { },
              placeholder = { Text("Search all videos...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
              trailingIcon = {
                IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                  Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
              },
            )
          },
          expanded = false,
          onExpandedChange = { },
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          shape = RoundedCornerShape(28.dp),
          tonalElevation = 6.dp,
        ) { }
      } else {
        BrowserTopBar(
          title = stringResource(app.marlboroadvance.mpvex.R.string.app_name),
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = sortedVideosWithInfo.size,
          onBackClick = null, // Unified header: no back button at root
          onCancelSelection = { selectionManager.clear() },
          onSortClick = { sortDialogOpen.value = true },
          onSearchClick = { isSearching = true },
          onSettingsClick = {
            backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
          },
          onDeleteClick = { deleteDialogOpen.value = true },
          isSingleSelection = selectionManager.isSingleSelection,
          onInfoClick = {
            if (selectionManager.isSingleSelection) {
              val video = selectionManager.getSelectedItems().firstOrNull()
              if (video != null) {
                val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                intent.action = Intent.ACTION_VIEW
                intent.data = video.uri
                context.startActivity(intent)
                selectionManager.clear()
              }
            }
          },
          onShareClick = { selectionManager.shareSelected() },
          onPlayClick = { selectionManager.playSelected() },
          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      }
    },
    floatingActionButton = {
      if (!selectionManager.isInSelectionMode && isFabVisible.value && sortedVideosWithInfo.isNotEmpty()) {
        TooltipBox(
          positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
          tooltip = { PlainTooltip { Text("Play recently played or first video") } },
          state = rememberTooltipState(),
        ) {
          FloatingActionButton(
            modifier = Modifier
              .windowInsetsPadding(WindowInsets.systemBars)
              .padding(bottom = navigationBarHeight)
              .animateFloatingActionButton(
                visible = true,
                alignment = Alignment.BottomEnd,
              ),
            onClick = {
              coroutineScope.launch {
                val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
                val lastPlayed = recentlyPlayedVideos.firstOrNull()

                if (lastPlayed != null && sortedVideosWithInfo.any { it.video.path == lastPlayed.filePath }) {
                  MediaUtils.playFile(lastPlayed.filePath, context, "recently_played_button")
                } else {
                  MediaUtils.playFile(sortedVideosWithInfo.first().video, context, "first_video_button")
                }
              }
            },
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play recently played or first video")
          }
        }
      }
    }
  ) { padding ->
    val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
    val videosWereDeletedOrMoved = false

    val displayVideos = if (searchQuery.isBlank()) {
      sortedVideosWithInfo
    } else {
      sortedVideosWithInfo.filter { 
        it.video.displayName.contains(searchQuery, ignoreCase = true) 
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      VideoListContent(
        folderId = "media_library",
        videosWithInfo = displayVideos,
        isLoading = isLoading && videos.isEmpty(),
        uiSettings = uiSettings,
        isRefreshing = isRefreshing,
        recentlyPlayedFilePath = recentlyPlayedFilePath,
        videosWereDeletedOrMoved = videosWereDeletedOrMoved,
        autoScrollToLastPlayed = autoScrollToLastPlayed,
        onRefresh = { viewModel.refresh() },
        selectionManager = selectionManager,
        onVideoClick = { video ->
          if (selectionManager.isInSelectionMode) {
            selectionManager.toggle(video)
          } else {
            MediaUtils.playFile(video, context, "video_list")
          }
        },
        onVideoLongClick = { video -> selectionManager.toggle(video) },
        isFabVisible = isFabVisible,
        modifier = Modifier.padding(padding),
        showFloatingBottomBar = showFloatingBottomBar,
        sortType = videoSortType,
        sortOrder = videoSortOrder,
      )

      AnimatedVisibility(
        visible = showFloatingBottomBar,
        enter = slideInVertically(
          animationSpec = tween(durationMillis = animationDuration),
          initialOffsetY = { fullHeight -> fullHeight }
        ),
        exit = slideOutVertically(
          animationSpec = tween(durationMillis = animationDuration),
          targetOffsetY = { fullHeight -> fullHeight }
        ),
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        BrowserBottomBar(
          isSelectionMode = true,
          onCopyClick = { /* N/A */ },
          onMoveClick = { /* N/A */ },
          onRenameClick = { renameDialogOpen.value = true },
          onDeleteClick = { deleteDialogOpen.value = true },
          onAddToPlaylistClick = { /* N/A */ },
          showCopy = false,
          showMove = false,
          showAddToPlaylist = false,
          modifier = Modifier.padding(bottom = navigationBarHeight + 16.dp)
        )
      }
    }

    if (sortDialogOpen.value) {
      VideoSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = videoSortType,
        sortOrder = videoSortOrder,
        onSortTypeChange = { browserPreferences.videoSortType.set(it) },
        onSortOrderChange = { browserPreferences.videoSortOrder.set(it) }
      )
    }

    if (deleteDialogOpen.value) {
      DeleteConfirmationDialog(
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = {
          selectionManager.deleteSelected()
          deleteDialogOpen.value = false
        },
        itemCount = selectionManager.selectedCount,
        isOpen = deleteDialogOpen.value,
        itemType = "video"
      )
    }

    if (renameDialogOpen.value) {
      val video = selectionManager.getSelectedItems().firstOrNull()
      if (video != null) {
        RenameDialog(
          onDismiss = { renameDialogOpen.value = false },
          onConfirm = { newName ->
            selectionManager.renameSelected(newName)
            renameDialogOpen.value = false
          },
          currentName = video.displayName,
          isOpen = renameDialogOpen.value,
          itemType = "video"
        )
      }
    }
  }
}
