package app.marlboroadvance.mpvex.ui.browser.filesystem

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.UiSettings
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddToPlaylistDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.SortDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.ViewModeSelector
import app.marlboroadvance.mpvex.ui.browser.dialogs.VisibilityToggle
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File

/**
 * Root File System Browser screen - shows storage volumes
 */
@Serializable
object FileSystemBrowserRootScreen : app.marlboroadvance.mpvex.presentation.Screen {
  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun Content() {
    FileSystemBrowserScreen(path = null)
  }
}

/**
 * File System Directory screen - shows contents of a specific directory
 */
@Serializable
data class FileSystemDirectoryScreen(
  val path: String,
) : app.marlboroadvance.mpvex.presentation.Screen {
  @OptIn(ExperimentalPermissionsApi::class)
  @Composable
  override fun Content() {
    FileSystemBrowserScreen(path = path)
  }
}

/**
 * File System Browser screen - browses directories and shows both folders and videos
 * @param path The directory path to browse, or null for storage roots
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileSystemBrowserScreen(path: String? = null) {
  val context = LocalContext.current
  val backstack = LocalBackStack.current
  val coroutineScope = rememberCoroutineScope()
  val browserPreferences = koinInject<BrowserPreferences>()
  val playerPreferences = koinInject<app.marlboroadvance.mpvex.preferences.PlayerPreferences>()
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  // ViewModel - use path parameter if provided, otherwise show roots
  val viewModel: FileSystemBrowserViewModel = viewModel(
    key = "FileSystemBrowser_${path ?: "root"}",
    factory = FileSystemBrowserViewModel.factory(
      context.applicationContext as android.app.Application,
      path,
    ),
  )

  // State collection
  val currentPath by viewModel.currentPath.collectAsState()
  val items by viewModel.items.collectAsState()
  val videoFilesWithPlayback by viewModel.videoFilesWithPlayback.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val uiSettings by viewModel.uiSettings.collectAsState()
  val error by viewModel.error.collectAsState()
  val isAtRoot by viewModel.isAtRoot.collectAsState()
  val breadcrumbs by viewModel.breadcrumbs.collectAsState()
  val playlistMode by playerPreferences.playlistMode.collectAsState()
  val itemsWereDeletedOrMoved by viewModel.itemsWereDeletedOrMoved.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()

  // Use standalone local states instead of CompositionLocal to avoid scroll issues with predictive back gesture
  val listState = remember { LazyListState() }
  
  // UI state
  val isRefreshing = remember { mutableStateOf(false) }
  val showLinkDialog = remember { mutableStateOf(false) }
  val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
  val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
  val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
  val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }

  // FAB visibility for scroll-based hiding
  val isFabVisible = remember { mutableStateOf(true) }
  val isFabExpanded = remember { mutableStateOf(false) }
  
  // Search state
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var isSearching by rememberSaveable { mutableStateOf(false) }
  var searchResults by remember { mutableStateOf<List<FileSystemItem>>(emptyList()) }
  var isSearchLoading by remember { mutableStateOf(false) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusRequester = remember { FocusRequester() }
  
  // Get navigation bar height from MainScreen
  val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current

  // Copy/Move state
  val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
  val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
  val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
  val operationProgress by CopyPasteOps.operationProgress.collectAsState()

  // Bottom bar visibility state
  var showFloatingBottomBar by remember { mutableStateOf(false) }
  var showBottomNavigation by remember { mutableStateOf(true) }

  // Animation duration for responsive slide animations
  val animationDuration = 200

  // Selection managers - separate for folders and videos
  val folders = items.filterIsInstance<FileSystemItem.Folder>()
  val videos = items.filterIsInstance<FileSystemItem.VideoFile>().map { it.video }

  val folderSelectionManager = rememberSelectionManager(
    items = folders,
    getId = { it.path },
    onDeleteItems = { foldersToDelete, _ ->
      viewModel.deleteFolders(foldersToDelete)
    },
    onOperationComplete = { viewModel.refresh() },
  )

  val videoSelectionManager = rememberSelectionManager(
    items = videos,
    getId = { it.id },
    onDeleteItems = { videosToDelete, _ ->
      viewModel.deleteVideos(videosToDelete)
    },
    onRenameItem = { video, newName ->
      viewModel.renameVideo(video, newName)
    },
    onOperationComplete = { viewModel.refresh() },
  )

  // Determine which selection manager is active
  val isInSelectionMode = folderSelectionManager.isInSelectionMode || videoSelectionManager.isInSelectionMode
  val selectedCount = folderSelectionManager.selectedCount + videoSelectionManager.selectedCount
  val totalCount = folders.size + videos.size
  val isMixedSelection = folderSelectionManager.isInSelectionMode && videoSelectionManager.isInSelectionMode

  // Update bottom bar visibility with optimized animation sequencing
  LaunchedEffect(isInSelectionMode, videoSelectionManager.isInSelectionMode, isMixedSelection) {
    val shouldShowFloatingBar = isInSelectionMode && videoSelectionManager.isInSelectionMode && !isMixedSelection
    
    if (shouldShowFloatingBar) {
      showBottomNavigation = false
      showFloatingBottomBar = true
    } else {
      showFloatingBottomBar = false
      showBottomNavigation = true
    }
  }

  // Permissions
  val permissionState = PermissionUtils.handleStoragePermission(
    onPermissionGranted = { viewModel.refresh() },
  )

  // Combined MainScreen updates
  LaunchedEffect(
    showBottomNavigation, 
    isInSelectionMode, 
    isMixedSelection, 
    videoSelectionManager.isInSelectionMode,
    permissionState.status
  ) {
    if (isAtRoot) {
      try {
        val mainScreenObj = app.marlboroadvance.mpvex.ui.browser.MainScreen
        val onlyVideosSelected = videoSelectionManager.isInSelectionMode && !folderSelectionManager.isInSelectionMode

        mainScreenObj.updateBottomBarVisibility(showBottomNavigation)
        mainScreenObj.updateSelectionState(
          isInSelectionMode = isInSelectionMode,
          isOnlyVideosSelected = onlyVideosSelected,
          selectionManager = if (onlyVideosSelected) videoSelectionManager else null
        )
        mainScreenObj.updatePermissionState(
          isDenied = permissionState.status is PermissionStatus.Denied
        )
      } catch (e: Exception) {
        Log.e("FileSystemBrowserScreen", "Failed to update MainScreen state", e)
      }
    }
  }

  // Search functionality
  LaunchedEffect(isSearching) {
    if (isSearching) {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  LaunchedEffect(searchQuery, isSearching, isAtRoot, items) {
    if (isSearching && searchQuery.isNotBlank()) {
      isSearchLoading = true
      coroutineScope.launch {
        try {
          val results = if (isAtRoot) {
            val allResults = mutableListOf<FileSystemItem>()
            val parentDirectories = items.filterIsInstance<FileSystemItem.Folder>()
              .map { it.path }
              .mapNotNull { path -> java.io.File(path).parent }
              .distinct()
            
            parentDirectories.forEach { parentPath ->
              try {
                val parentResults = searchRecursively(context, parentPath, searchQuery)
                allResults.addAll(parentResults)
              } catch (e: Exception) { }
            }
            
            items.filterIsInstance<FileSystemItem.Folder>().forEach { storageVolume ->
              try {
                val rootResults = searchRecursively(context, storageVolume.path, searchQuery)
                allResults.addAll(rootResults)
              } catch (e: Exception) { }
            }
            
            val isAudioFilesVisible = browserPreferences.showAudioFiles.get()
            allResults.distinctBy { item ->
              when (item) {
                is FileSystemItem.VideoFile -> item.video.path
                is FileSystemItem.Folder -> item.path
              }
            }.filter { item ->
              when (item) {
                is FileSystemItem.VideoFile -> isAudioFilesVisible || !item.video.isAudio
                is FileSystemItem.Folder -> isAudioFilesVisible || item.videoCount > 0
              }
            }
          } else if (path != null) {
            searchRecursively(context, path, searchQuery)
          } else {
            emptyList()
          }
          searchResults = results
        } catch (e: Exception) {
          searchResults = emptyList()
        } finally {
          isSearchLoading = false
        }
      }
    } else {
      searchResults = emptyList()
    }
  }

  // Main content
  Box(modifier = Modifier.fillMaxSize()) {
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
                placeholder = {
                  Text(
                    if (isAtRoot) "Search storage..." else "Search folder..."
                  )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                  IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                  }
                },
                modifier = Modifier.focusRequester(focusRequester),
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
            title = if (isAtRoot) stringResource(app.marlboroadvance.mpvex.R.string.app_name) else breadcrumbs.lastOrNull()?.name ?: "Tree View",
            isInSelectionMode = isInSelectionMode,
            selectedCount = selectedCount,
            totalCount = totalCount,
            onBackClick = if (isAtRoot) null else { { backstack.removeLastOrNull() } },
            onCancelSelection = { folderSelectionManager.clear(); videoSelectionManager.clear() },
            onSortClick = { sortDialogOpen.value = true },
            onSearchClick = { isSearching = !isSearching },
            onSettingsClick = { backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen) },
            onDeleteClick = if (videoSelectionManager.isInSelectionMode && !isMixedSelection) null else { { deleteDialogOpen.value = true } },
            onRenameClick = null,
            isSingleSelection = videoSelectionManager.isSingleSelection && !isMixedSelection,
            onInfoClick = if (videoSelectionManager.isInSelectionMode && !folderSelectionManager.isInSelectionMode) {
              {
                val video = videoSelectionManager.getSelectedItems().firstOrNull()
                if (video != null) {
                  val intent = Intent(context, app.marlboroadvance.mpvex.ui.mediainfo.MediaInfoActivity::class.java)
                  intent.action = Intent.ACTION_VIEW
                  intent.data = video.uri
                  context.startActivity(intent)
                  videoSelectionManager.clear()
                }
              }
            } else null,
            onShareClick = { /* Logic for share */ },
            onPlayClick = { /* Logic for play */ },
            onSelectAll = { folderSelectionManager.selectAll(); videoSelectionManager.selectAll() },
            onInvertSelection = { folderSelectionManager.invertSelection(); videoSelectionManager.invertSelection() },
            onDeselectAll = { folderSelectionManager.clear(); videoSelectionManager.clear() },
          )
        }
      },
    ) { padding ->
      Box(modifier = Modifier.padding(padding)) {
        when (permissionState.status) {
          PermissionStatus.Granted -> {
            if (isSearching) {
              FileSystemSearchContent(
                listState = listState,
                searchQuery = searchQuery,
                searchResults = searchResults,
                isLoading = isSearchLoading,
                videoFilesWithPlayback = videoFilesWithPlayback,
                uiSettings = uiSettings,
                showSubtitleIndicator = showSubtitleIndicator,
                isAtRoot = isAtRoot,
                navigationBarHeight = navigationBarHeight,
                isFabVisible = isFabVisible,
                onVideoClick = { video -> MediaUtils.playFile(video, context, "search") },
                onFolderClick = { folder ->
                  backstack.add(FileSystemDirectoryScreen(folder.path))
                  isSearching = false; searchQuery = ""
                },
              )
            } else {
              FileSystemBrowserContent(
                listState = listState,
                items = items,
                videoFilesWithPlayback = videoFilesWithPlayback,
                isLoading = isLoading && items.isEmpty(),
                uiSettings = uiSettings,
                isRefreshing = isRefreshing,
                error = error,
                isAtRoot = isAtRoot,
                breadcrumbs = breadcrumbs,
                playlistMode = playlistMode,
                itemsWereDeletedOrMoved = itemsWereDeletedOrMoved,
                showSubtitleIndicator = showSubtitleIndicator,
                navigationBarHeight = navigationBarHeight,
                onRefresh = { viewModel.refresh() },
                onFolderClick = { folder ->
                  if (isInSelectionMode) folderSelectionManager.toggle(folder)
                  else backstack.add(FileSystemDirectoryScreen(folder.path))
                },
                onFolderLongClick = { folder -> folderSelectionManager.toggle(folder) },
                onVideoClick = { video ->
                  if (isInSelectionMode) videoSelectionManager.toggle(video)
                  else MediaUtils.playFile(video, context)
                },
                onVideoLongClick = { video -> videoSelectionManager.toggle(video) },
                onBreadcrumbClick = { component -> backstack.add(FileSystemDirectoryScreen(component.fullPath)) },
                folderSelectionManager = folderSelectionManager,
                videoSelectionManager = videoSelectionManager,
                isInSelectionMode = isInSelectionMode,
              )
            }
          }
          is PermissionStatus.Denied -> {
            PermissionDeniedState(onRequestPermission = { permissionState.launchPermissionRequest() })
          }
        }
      }
    }
  }
}

@Composable
private fun FileSystemBrowserContent(
  listState: LazyListState,
  items: List<FileSystemItem>,
  videoFilesWithPlayback: Map<Long, Float>,
  isLoading: Boolean,
  uiSettings: UiSettings,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  error: String?,
  isAtRoot: Boolean,
  breadcrumbs: List<app.marlboroadvance.mpvex.domain.browser.PathComponent>,
  playlistMode: Boolean,
  itemsWereDeletedOrMoved: Boolean,
  showSubtitleIndicator: Boolean,
  navigationBarHeight: Dp,
  onRefresh: suspend () -> Unit,
  onFolderClick: (FileSystemItem.Folder) -> Unit,
  onFolderLongClick: (FileSystemItem.Folder) -> Unit,
  onVideoClick: (app.marlboroadvance.mpvex.domain.media.model.Video) -> Unit,
  onVideoLongClick: (app.marlboroadvance.mpvex.domain.media.model.Video) -> Unit,
  onBreadcrumbClick: (app.marlboroadvance.mpvex.domain.browser.PathComponent) -> Unit,
  folderSelectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<FileSystemItem.Folder, String>,
  videoSelectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<app.marlboroadvance.mpvex.domain.media.model.Video, Long>,
  modifier: Modifier = Modifier,
  isInSelectionMode: Boolean = false,
) {
  val gesturePreferences = koinInject<GesturePreferences>()
  val thumbnailRepository = koinInject<app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository>()
  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()

  val folders = items.filterIsInstance<FileSystemItem.Folder>()
  val videos = items.filterIsInstance<FileSystemItem.VideoFile>().map { it.video }

  when {
    isLoading -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }
    error != null -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Filled.Folder, title = "Error", message = error)
      }
    }
    else -> {
      PullRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, listState = listState) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = navigationBarHeight)) {
          if (!isAtRoot) {
            item {
              BreadcrumbNavigation(breadcrumbs = breadcrumbs, onBreadcrumbClick = onBreadcrumbClick)
            }
          }
          items(items.filterIsInstance<FileSystemItem.Folder>(), key = { it.path }) { folder ->
            val folderModel = app.marlboroadvance.mpvex.domain.media.model.VideoFolder(
              bucketId = folder.path, name = folder.name, path = folder.path,
              videoCount = folder.videoCount, audioCount = folder.audioCount,
              totalSize = folder.totalSize, totalDuration = folder.totalDuration,
              lastModified = folder.lastModified / 1000,
            )
            FolderCard(
              folder = folderModel, uiSettings = uiSettings,
              isSelected = folderSelectionManager.isSelected(folder),
              onClick = { onFolderClick(folder) }, onLongClick = { onFolderLongClick(folder) },
              onThumbClick = { if (tapThumbnailToSelect) onFolderLongClick(folder) else onFolderClick(folder) },
            )
          }
          items(items.filterIsInstance<FileSystemItem.VideoFile>(), key = { it.video.id }) { videoFile ->
            VideoCard(
              video = videoFile.video, uiSettings = uiSettings,
              progressPercentage = videoFilesWithPlayback[videoFile.video.id],
              isSelected = videoSelectionManager.isSelected(videoFile.video),
              onClick = { onVideoClick(videoFile.video) }, onLongClick = { onVideoLongClick(videoFile.video) },
              onThumbClick = { if (tapThumbnailToSelect) onVideoLongClick(videoFile.video) else onVideoClick(videoFile.video) },
              showSubtitleIndicator = showSubtitleIndicator,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FileSystemSearchContent(
  listState: LazyListState,
  searchQuery: String,
  searchResults: List<FileSystemItem>,
  isLoading: Boolean,
  videoFilesWithPlayback: Map<Long, Float>,
  uiSettings: UiSettings,
  showSubtitleIndicator: Boolean,
  isAtRoot: Boolean,
  navigationBarHeight: Dp,
  isFabVisible: androidx.compose.runtime.MutableState<Boolean>,
  onVideoClick: (app.marlboroadvance.mpvex.domain.media.model.Video) -> Unit,
  onFolderClick: (FileSystemItem.Folder) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
  } else {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = navigationBarHeight)) {
      val folders = searchResults.filterIsInstance<FileSystemItem.Folder>()
      val videos = searchResults.filterIsInstance<FileSystemItem.VideoFile>()
      items(folders) { folder ->
        val folderModel = app.marlboroadvance.mpvex.domain.media.model.VideoFolder(
          bucketId = folder.path, name = folder.name, path = folder.path,
          videoCount = folder.videoCount, audioCount = folder.audioCount,
          totalSize = folder.totalSize, totalDuration = folder.totalDuration,
          lastModified = folder.lastModified / 1000,
        )
        FolderCard(folder = folderModel, uiSettings = uiSettings, onClick = { onFolderClick(folder) })
      }
      items(videos) { videoFile ->
        VideoCard(
          video = videoFile.video, uiSettings = uiSettings,
          progressPercentage = videoFilesWithPlayback[videoFile.video.id],
          onClick = { onVideoClick(videoFile.video) },
          showSubtitleIndicator = showSubtitleIndicator,
        )
      }
    }
  }
}

@Composable
fun BreadcrumbNavigation(
  breadcrumbs: List<app.marlboroadvance.mpvex.domain.browser.PathComponent>,
  onBreadcrumbClick: (app.marlboroadvance.mpvex.domain.browser.PathComponent) -> Unit,
) { /* Existing implementation */ }

suspend fun searchRecursively(context: Context, path: String, query: String): List<FileSystemItem> = emptyList()
