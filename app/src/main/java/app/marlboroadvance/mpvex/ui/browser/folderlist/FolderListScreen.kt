package app.marlboroadvance.mpvex.ui.browser.folderlist

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ContentCopy
import app.marlboroadvance.mpvex.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.FolderViewMode
import app.marlboroadvance.mpvex.preferences.FoldersPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.UiSettings
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.components.UnifiedExplorerContent
import app.marlboroadvance.mpvex.ui.browser.components.SelectionOverflowAction
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderSortDialog
import app.marlboroadvance.mpvex.ui.browser.medialibrary.MediaLibraryContent
import app.marlboroadvance.mpvex.ui.browser.filesystem.FileSystemDirectoryScreen
import app.marlboroadvance.mpvex.ui.browser.filesystem.FileSystemBrowserRootScreen
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.sheets.MarkAsBottomSheet
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import app.marlboroadvance.mpvex.ui.browser.sheets.MultiSelectionInfoSheet
import app.marlboroadvance.mpvex.ui.browser.sheets.PlayLinkSheet
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.browser.states.LoadingState
import app.marlboroadvance.mpvex.ui.browser.states.PermissionDeniedState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File

@Serializable
object FolderListScreen : Screen {
  @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val browserPreferences = koinInject<BrowserPreferences>()
    val folderViewMode by browserPreferences.folderViewMode.collectAsState()
    val folderSortType by browserPreferences.folderSortType.collectAsState()
    val folderSortOrder by browserPreferences.folderSortOrder.collectAsState()

    when (folderViewMode) {
      FolderViewMode.FileManager -> FileSystemBrowserRootScreen.Content()
      FolderViewMode.AlbumView -> MediaStoreFolderListContent(
        folderSortType = folderSortType,
        folderSortOrder = folderSortOrder
      )
      FolderViewMode.MediaLibrary -> MediaLibraryContent()
    }
  }

  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  private fun MediaStoreFolderListContent(
    folderSortType: FolderSortType,
    folderSortOrder: SortOrder,
  ) {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ViewModels and preferences
    val viewModel: FolderListViewModel = viewModel(
      factory = FolderListViewModel.factory(context.applicationContext as android.app.Application)
    )
    val browserPreferences = koinInject<BrowserPreferences>()
    val gesturePreferences = koinInject<GesturePreferences>()
    val foldersPreferences = koinInject<FoldersPreferences>()
    val advancedPreferences = koinInject<app.marlboroadvance.mpvex.preferences.AdvancedPreferences>()

    // State collection
    val videoFolders by viewModel.videoFolders.collectAsState()
    val foldersWithNewCount by viewModel.foldersWithNewCount.collectAsState()
    val uiSettings by viewModel.uiSettings.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val playedFolderPaths by viewModel.playedFolderPaths.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val hasCompletedInitialLoad by viewModel.hasCompletedInitialLoad.collectAsState()
    val foldersWereDeleted by viewModel.foldersWereDeleted.collectAsState()

    // Preferences
    val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
    val folderGridColumnsPortrait by browserPreferences.folderGridColumnsPortrait.collectAsState()
    val folderGridColumnsLandscape by browserPreferences.folderGridColumnsLandscape.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val folderGridColumns = if (isLandscape) folderGridColumnsLandscape else folderGridColumnsPortrait
    val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
    val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()
    val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()

    // UI state - use standalone states to avoid scroll issues with predictive back gesture
    val rememberedListIndex = rememberSaveable { mutableIntStateOf(0) }
    val rememberedListOffset = rememberSaveable { mutableIntStateOf(0) }
    val rememberedGridIndex = rememberSaveable { mutableIntStateOf(0) }
    val rememberedGridOffset = rememberSaveable { mutableIntStateOf(0) }

    // Sorting and filtering
    val sortedFolders = remember(videoFolders, folderSortType, folderSortOrder) {
      SortUtils.sortFolders(videoFolders, folderSortType, folderSortOrder)
    }

    val initialListIndex = if (rememberedListIndex.intValue > 0) {
      rememberedListIndex.intValue
    } else if (autoScrollToLastPlayed && recentlyPlayedFilePath != null && sortedFolders.isNotEmpty()) {
      var foundIndex = 0
      val lastPlayedParentPath = java.io.File(recentlyPlayedFilePath!!).parent ?: "/"
      for (i in sortedFolders.indices) {
        if (sortedFolders[i].path == lastPlayedParentPath) {
          foundIndex = i
          break
        }
      }
      foundIndex
    } else 0

    val initialGridIndex = if (rememberedGridIndex.intValue > 0) {
      rememberedGridIndex.intValue
    } else if (autoScrollToLastPlayed && recentlyPlayedFilePath != null && sortedFolders.isNotEmpty()) {
      var foundIndex = 0
      val lastPlayedParentPath = java.io.File(recentlyPlayedFilePath!!).parent ?: "/"
      for (i in sortedFolders.indices) {
        if (sortedFolders[i].path == lastPlayedParentPath) {
          foundIndex = i
          break
        }
      }
      foundIndex
    } else 0

    val listState = rememberLazyListState(
      initialFirstVisibleItemIndex = initialListIndex,
      initialFirstVisibleItemScrollOffset = rememberedListOffset.intValue
    )
    val gridState = rememberLazyGridState(
      initialFirstVisibleItemIndex = initialGridIndex,
      initialFirstVisibleItemScrollOffset = rememberedGridOffset.intValue
    )

    val isInitialSortLoad = remember { mutableStateOf(true) }
    LaunchedEffect(folderSortType.name, folderSortOrder.name) {
      if (isInitialSortLoad.value) {
        isInitialSortLoad.value = false
        return@LaunchedEffect
      }
      rememberedListIndex.intValue = 0
      rememberedGridIndex.intValue = 0
      listState.scrollToItem(0)
      gridState.scrollToItem(0)
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
      rememberedListIndex.intValue = listState.firstVisibleItemIndex
      rememberedListOffset.intValue = listState.firstVisibleItemScrollOffset
    }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
      rememberedGridIndex.intValue = gridState.firstVisibleItemIndex
      rememberedGridOffset.intValue = gridState.firstVisibleItemScrollOffset
    }

    val navigationBarHeight = LocalNavigationBarHeight.current
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val showLinkDialog = remember { mutableStateOf(false) }
    var showMarkAsSheet by remember { mutableStateOf(false) }
    val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
    val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
    val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
    var renameDialogOpen by rememberSaveable { mutableStateOf(false) }
    val operationProgress by CopyPasteOps.operationProgress.collectAsState()

    // Search state
    var folderSelectionInfo by remember { mutableStateOf<Triple<Int, Long, Long>?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FileSystemItem>>(emptyList()) }
    var isSearchLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Search logic
    LaunchedEffect(searchQuery, isSearching) {
      if (isSearching && searchQuery.isNotBlank()) {
        isSearchLoading = true
        try {
          val results = searchFoldersAndVideos(context, searchQuery)
          searchResults = results
        } catch (e: Exception) {
          Log.e("FolderListScreen", "Error during search", e)
          searchResults = emptyList()
        } finally {
          isSearchLoading = false
        }
      } else {
        searchResults = emptyList()
        isSearchLoading = false
      }
    }

    // Auto-focus search input when search is opened
    LaunchedEffect(isSearching) {
      if (isSearching) {
        focusRequester.requestFocus()
        keyboardController?.show()
      }
    }

    // FAB state
    val isFabVisible = remember { mutableStateOf(true) }
    val isFabExpanded = remember { mutableStateOf(false) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      uri?.let {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        MediaUtils.playFile(it.toString(), context, "open_file")
      }
    }

    val filteredFolders = sortedFolders

    // Selection manager
    val selectionManager = rememberSelectionManager<VideoFolder, String>(
      items = sortedFolders,
      getId = { it.bucketId },
      onDeleteItems = { folders, _ ->
        viewModel.deleteFolders(folders)
      },
      onOperationComplete = { viewModel.refresh() },
    )

    val treePickerLauncher = rememberLauncherForActivityResult(
      contract = OpenDocumentTreeContract(),
    ) { uri ->
      if (uri == null || operationType.value == null) return@rememberLauncherForActivityResult
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      }
      progressDialogOpen.value = true
      coroutineScope.launch {
        val selectedFolders = selectionManager.getSelectedItems()
        val selectedVideos = selectedFolders.flatMap { folder ->
          MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
        }
        if (selectedVideos.isNotEmpty()) {
          when (operationType.value) {
            is CopyPasteOps.OperationType.Copy -> CopyPasteOps.copyFilesToTreeUri(context, selectedVideos, uri)
            is CopyPasteOps.OperationType.Move -> CopyPasteOps.moveFilesToTreeUri(context, selectedVideos, uri)
            else -> {}
          }
        }
      }
    }

    // Permissions
    val permissionState = PermissionUtils.handleStoragePermission(
      onPermissionGranted = { viewModel.refresh() },
    )

    // Update MainScreen about permission state
    LaunchedEffect(permissionState.status) {
      app.marlboroadvance.mpvex.ui.browser.MainScreen.updatePermissionState(
        isDenied = permissionState.status is PermissionStatus.Denied
      )
    }

    // Lifecycle observer for refresh
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          viewModel.loadData()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Optimized back handler for immediate response
    val shouldHandleBack = selectionManager.isInSelectionMode || isSearching || isFabExpanded.value
    androidx.activity.compose.BackHandler(enabled = shouldHandleBack) {
      when {
        isFabExpanded.value -> isFabExpanded.value = false
        selectionManager.isInSelectionMode -> selectionManager.clear()
        isSearching -> {
          isSearching = false
          searchQuery = ""
        }
      }
    }

    // FAB scroll tracking
    app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper.trackScrollForFabVisibility(
      listState = listState,
      gridState = if (mediaLayoutMode == MediaLayoutMode.GRID) gridState else null,
      isFabVisible = isFabVisible,
      expanded = isFabExpanded.value,
      onExpandedChange = { isFabExpanded.value = it },
    )

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
                placeholder = { Text("Search folders and videos...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                  )
                },
                trailingIcon = {
                  IconButton(
                    onClick = {
                      isSearching = false
                      searchQuery = ""
                    },
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = "Cancel",
                    )
                  }
                },
                modifier = Modifier.focusRequester(focusRequester),
              )
            },
            expanded = false,
            onExpandedChange = { },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
          ) {
            // Empty content for SearchBar
          }
        } else {
          BrowserTopBar(
            title = stringResource(app.marlboroadvance.mpvex.R.string.app_name),
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = videoFolders.size,
            onBackClick = null,
            onCancelSelection = { selectionManager.clear() },
            onSortClick = { sortDialogOpen.value = true },
            onSearchClick = { isSearching = !isSearching },
            onSettingsClick = {
              backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
            },
            onRenameClick = null,
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = {
              val selected = selectionManager.getSelectedItems()
              folderSelectionInfo = Triple(
                selected.size,
                selected.sumOf { it.totalSize },
                selected.sumOf { it.totalDuration },
              )
            },
            onPlayClick = {
              coroutineScope.launch {
                val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                val allVideos = app.marlboroadvance.mpvex.repository.MediaFileRepository
                  .getVideosForBuckets(context, selectedIds)
                if (allVideos.isNotEmpty()) {
                  if (allVideos.size == 1) {
                    MediaUtils.playFile(allVideos.first(), context)
                  } else {
                    MediaUtils.playPlaylist(allVideos, 0, context)
                  }
                  selectionManager.clear()
                }
              }
            },
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
            selectionOverflowActions = listOf(
              SelectionOverflowAction(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = {
                  coroutineScope.launch {
                    val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
                    val allVideos = app.marlboroadvance.mpvex.repository.MediaFileRepository
                      .getVideosForBuckets(context, selectedIds)
                    if (allVideos.isNotEmpty()) {
                      MediaUtils.shareVideos(context, allVideos)
                    }
                  }
                },
              ),
              SelectionOverflowAction(
                icon = Icons.Filled.Block,
                label = "Blacklist",
                onClick = {
                  coroutineScope.launch {
                    val selectedFolders = selectionManager.getSelectedItems()
                    val blacklistedFolders = foldersPreferences.blacklistedFolders.get().toMutableSet()
                    selectedFolders.forEach { folder -> blacklistedFolders.add(folder.path) }
                    foldersPreferences.blacklistedFolders.set(blacklistedFolders)
                    selectionManager.clear()
                    viewModel.refresh()
                    android.widget.Toast.makeText(
                      context,
                      context.getString(app.marlboroadvance.mpvex.R.string.pref_folders_blacklisted),
                      android.widget.Toast.LENGTH_SHORT,
                    ).show()
                  }
                },
              ),
              SelectionOverflowAction(
                icon = Icons.Filled.ContentCopy,
                label = stringResource(R.string.copy_folder_path),
                onClick = {
                  val selectedFolders = selectionManager.getSelectedItems()
                  if (selectedFolders.isNotEmpty()) {
                    val pathsString = selectedFolders.joinToString("\n") { it.path }
                    clipboardManager.setText(AnnotatedString(pathsString))
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                  }
                }
              ),
            ),
          )
        }
      },
      floatingActionButton = {
        FloatingActionButtonMenu(
          modifier = Modifier.padding(bottom = navigationBarHeight + 8.dp),
          expanded = isFabExpanded.value,
          button = {
            TooltipBox(
              positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                if (isFabExpanded.value) {
                  TooltipAnchorPosition.Start
                } else {
                  TooltipAnchorPosition.Above
                }
              ),
              tooltip = { PlainTooltip { Text("Toggle menu") } },
              state = rememberTooltipState(),
            ) {
              ToggleFloatingActionButton(
                modifier = Modifier.animateFloatingActionButton(
                  visible = !selectionManager.isInSelectionMode && isFabVisible.value && !app.marlboroadvance.mpvex.ui.browser.MainScreen.getPermissionDeniedState(),
                  alignment = Alignment.BottomEnd,
                ),
                checked = isFabExpanded.value,
                onCheckedChange = { isFabExpanded.value = !isFabExpanded.value },
              ) {
                val imageVector by remember {
                  derivedStateOf {
                    if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.PlayArrow
                  }
                }
                Icon(
                  painter = rememberVectorPainter(imageVector),
                  contentDescription = null,
                  modifier = Modifier.animateIcon({ checkedProgress }),
                )
              }
            }
          },
        ) {
          FloatingActionButtonMenuItem(
            onClick = {
              isFabExpanded.value = false
              filePicker.launch(arrayOf("video/*"))
            },
            icon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
            text = { Text(text = "Open File") },
          )

          FloatingActionButtonMenuItem(
            onClick = {
              isFabExpanded.value = false
              coroutineScope.launch {
                val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
                val lastPlayed = recentlyPlayedVideos.firstOrNull()
                if (lastPlayed != null) {
                  MediaUtils.playFile(lastPlayed.filePath, context, "recently_played_button")
                }
              }
            },
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            text = { Text(text = "Recently Played") },
          )

          FloatingActionButtonMenuItem(
            onClick = {
              isFabExpanded.value = false
              showLinkDialog.value = true
            },
            icon = { Icon(Icons.Filled.Link, contentDescription = null) },
            text = { Text(text = "Open Link") },
          )
        }
      },
    ) { padding ->
      Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        when (permissionState.status) {
          PermissionStatus.Granted -> {
            if (isSearching) {
              UnifiedExplorerContent(
                items = searchResults,
                isLoading = isSearchLoading,
                uiSettings = uiSettings,
                isSelected = { false },
                onClick = { item ->
                  when (item) {
                    is FileSystemItem.Folder -> {
                      backstack.add(app.marlboroadvance.mpvex.ui.browser.videolist.VideoListScreen(item.path, item.name))
                    }
                    is FileSystemItem.VideoFile -> {
                      MediaUtils.playFile(item.video, context)
                    }
                  }
                },
                onLongClick = {},
                onToggleSelection = {},
                emptyTitle = if (searchQuery.isBlank()) stringResource(R.string.search_empty_title) else stringResource(R.string.search_no_results_title),
                emptyMessage = if (searchQuery.isBlank()) stringResource(R.string.search_empty_message) else stringResource(R.string.search_no_results_message),
                emptyIcon = Icons.Filled.Search,
                showSections = true,
              )
            } else {

            FolderListContent(
              folders = filteredFolders,
              foldersWithNewCount = foldersWithNewCount,
              autoScrollToLastPlayed = autoScrollToLastPlayed,
              uiSettings = uiSettings,
              scanStatus = scanStatus,
              listState = listState,
              gridState = gridState,
              isRefreshing = isRefreshing,
              isLoading = isLoading,
              hasCompletedInitialLoad = hasCompletedInitialLoad,
              foldersWereDeleted = foldersWereDeleted,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              playedFolderPaths = playedFolderPaths,
              onRefresh = { viewModel.refresh() },
              mediaLayoutMode = mediaLayoutMode,
              folderGridColumns = folderGridColumns,
              tapThumbnailToSelect = tapThumbnailToSelect,
              navigationBarHeight = navigationBarHeight,
              selectionManager = selectionManager,
              onFolderClick = { folder ->
                if (selectionManager.isInSelectionMode) {
                  selectionManager.toggle(folder)
                } else {
                  backstack.add(app.marlboroadvance.mpvex.ui.browser.videolist.VideoListScreen(folder.bucketId, folder.name))
                }
              },
              onFolderLongClick = { folder ->
                selectionManager.handleLongClick(folder)
              },
              scrollTriggerKey = "${folderSortType.name}:${folderSortOrder.name}",
            )
            }
          }

          is PermissionStatus.Denied -> {
            PermissionDeniedState(
              onRequestPermission = { permissionState.launchPermissionRequest() },
              modifier = Modifier,
            )
          }
        }
      }

      // Dialogs
      PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )

      FolderSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = folderSortType,
        sortOrder = folderSortOrder,
        onSortTypeChange = { 
          browserPreferences.folderSortType.set(it)
        },
        onSortOrderChange = { 
          browserPreferences.folderSortOrder.set(it)
        },
      )

      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
          visible = selectionManager.isInSelectionMode,
          enter = slideInVertically(
            animationSpec = tween(durationMillis = 200),
            initialOffsetY = { fullHeight -> fullHeight },
          ),
          exit = slideOutVertically(
            animationSpec = tween(durationMillis = 200),
            targetOffsetY = { fullHeight -> fullHeight },
          ),
          modifier = Modifier.align(Alignment.BottomCenter),
        ) {
          BrowserBottomBar(
            isSelectionMode = true,
            onCopyClick = {
              operationType.value = CopyPasteOps.OperationType.Copy
              if (CopyPasteOps.canUseDirectFileOperations()) {
                folderPickerOpen.value = true
              } else {
                treePickerLauncher.launch(null)
              }
            },
            onMoveClick = {
              operationType.value = CopyPasteOps.OperationType.Move
              if (CopyPasteOps.canUseDirectFileOperations()) {
                folderPickerOpen.value = true
              } else {
                treePickerLauncher.launch(null)
              }
            },
            onRenameClick = { renameDialogOpen = true },
            onDeleteClick = { deleteDialogOpen.value = true },
            onAddToPlaylistClick = { },
            showCopy = true,
            showMove = true,
            showRename = selectionManager.isSingleSelection,
            showDelete = selectionManager.selectedCount <= 1,
            showAddToPlaylist = false,
            onMarkAsClick = { showMarkAsSheet = true },
            modifier = Modifier.padding(bottom = navigationBarHeight),
          )
        }
      }

      if (showMarkAsSheet) {
        MarkAsBottomSheet(
          onDismiss = { showMarkAsSheet = false },
          onMarkAs = { state ->
            coroutineScope.launch {
              val selectedIds = selectionManager.getSelectedItems().map { it.bucketId }.toSet()
              val videos = MediaFileRepository.getVideosForBuckets(context, selectedIds)
              videos.forEach { video ->
                RecentlyPlayedOps.markAs(
                  filePath = video.path,
                  fileName = video.displayName,
                  duration = video.duration,
                  state = state,
                )
              }
            }
          },
        )
      }

      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        onDismiss = { folderPickerOpen.value = false },
        onFolderSelected = { destinationPath ->
          folderPickerOpen.value = false
          val op = operationType.value
          if (op != null) {
            coroutineScope.launch {
              val selectedFolders = selectionManager.getSelectedItems()
              if (selectedFolders.isNotEmpty()) {
                when (op) {
                  is CopyPasteOps.OperationType.Move -> {
                    val needFallback = mutableListOf<VideoFolder>()
                    for (folder in selectedFolders) {
                      val dst = File(destinationPath, folder.name)
                      if (!File(folder.path).renameTo(dst)) needFallback.add(folder)
                    }
                    if (needFallback.isNotEmpty()) {
                      progressDialogOpen.value = true
                      for (folder in needFallback) {
                        val videos = MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
                        if (videos.isNotEmpty()) {
                          val subDest = File(destinationPath, folder.name).also { it.mkdirs() }.absolutePath
                          CopyPasteOps.moveFiles(context, videos, subDest)
                        }
                      }
                    } else {
                      selectionManager.clear()
                      viewModel.refresh()
                    }
                  }
                  is CopyPasteOps.OperationType.Copy -> {
                    progressDialogOpen.value = true
                    for (folder in selectedFolders) {
                      val videos = MediaFileRepository.getVideosForBuckets(context, setOf(folder.bucketId))
                      if (videos.isNotEmpty()) {
                        val subDest = File(destinationPath, folder.name).also { it.mkdirs() }.absolutePath
                        CopyPasteOps.copyFiles(context, videos, subDest)
                      }
                    }
                  }
                }
              }
            }
          }
        },
      )

      if (operationType.value != null) {
        FileOperationProgressDialog(
          isOpen = progressDialogOpen.value,
          operationType = operationType.value!!,
          progress = operationProgress,
          onCancel = { CopyPasteOps.cancelOperation() },
          onDismiss = {
            progressDialogOpen.value = false
            operationType.value = null
            selectionManager.clear()
            viewModel.refresh()
          },
        )
      }

      if (renameDialogOpen && selectionManager.isSingleSelection) {
        val folder = selectionManager.getSelectedItems().firstOrNull()
        if (folder != null) {
          RenameDialog(
            isOpen = true,
            onDismiss = { renameDialogOpen = false },
            onConfirm = { newName ->
              renameDialogOpen = false
              coroutineScope.launch {
                val ok = viewModel.renameFolder(folder, newName)
                if (!ok) {
                  android.widget.Toast.makeText(context, "Rename failed", android.widget.Toast.LENGTH_SHORT).show()
                }
                selectionManager.clear()
                viewModel.refresh()
              }
            },
            currentName = folder.name,
            itemType = "folder",
          )
        }
      }

      DeleteConfirmationDialog(
        isOpen = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemType = "folder",
        itemCount = selectionManager.selectedCount,
        itemNames = selectionManager.getSelectedItems().map { it.name },
      )

      folderSelectionInfo?.let { (count, bytes, duration) ->
        MultiSelectionInfoSheet(
          count = count,
          totalBytes = bytes,
          totalDurationMs = duration,
          onDismiss = { folderSelectionInfo = null },
          unit = "folder",
        )
      }
    }
  }
}

@Composable
private fun FolderListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<app.marlboroadvance.mpvex.ui.browser.folderlist.FolderWithNewCount>,
  uiSettings: UiSettings,
  recentlyPlayedFilePath: String?,
  playedFolderPaths: Set<String>,
  isLoading: Boolean,
  scanStatus: String?,
  hasCompletedInitialLoad: Boolean,
  foldersWereDeleted: Boolean,
  mediaLayoutMode: MediaLayoutMode,
  folderGridColumns: Int,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  autoScrollToLastPlayed: Boolean,
  onRefresh: suspend () -> Unit,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
  scrollTriggerKey: Any? = null,
) {
  val showLoading = isLoading && !hasCompletedInitialLoad

  UnifiedExplorerContent(
    items = folders,
    isLoading = showLoading,
    uiSettings = uiSettings,
    isSelected = { selectionManager.isSelected(it) },
    onClick = { onFolderClick(it) },
    onLongClick = { onFolderLongClick(it) },
    onToggleSelection = { selectionManager.toggle(it) },
    emptyTitle = "No video folders found",
    emptyMessage = "Add videos to your device to see folders here",
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    isInSelectionMode = selectionManager.isInSelectionMode,
    recentlyPlayedFilePath = recentlyPlayedFilePath,
    playedFolderPaths = playedFolderPaths,
    autoScrollToLastPlayed = autoScrollToLastPlayed,
    listState = listState,
    gridState = gridState,
    scrollTriggerKey = scrollTriggerKey,
    gridColumns = folderGridColumns,
  )
}

@Composable
private fun GridContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<app.marlboroadvance.mpvex.ui.browser.folderlist.FolderWithNewCount>,
  uiSettings: UiSettings,
  recentlyPlayedFilePath: String?,
  playedFolderPaths: Set<String>,
  folderGridColumns: Int,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
  scrollbarAlpha: Float,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(folderGridColumns),
      state = gridState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = if (folderGridColumns == 1) 20.dp else 8.dp,
        end = if (folderGridColumns == 1) 20.dp else 8.dp,
        top = if (folderGridColumns == 1) 20.dp else 8.dp,
        bottom = navigationBarHeight
      ),
      horizontalArrangement = Arrangement.spacedBy(if (folderGridColumns == 1) 0.dp else 4.dp),
      verticalArrangement = Arrangement.spacedBy(if (folderGridColumns == 1) 20.dp else 4.dp),
    ) {
      items(folders.size) { index ->
        val folder = folders[index]
        val isRecentlyPlayed = recentlyPlayedFilePath?.let {
          java.io.File(it).parent == folder.path
        } ?: false
        val isNeverPlayed = folder.path !in playedFolderPaths
        val newCount = foldersWithNewCount
          .find { it.folder.bucketId == folder.bucketId }
          ?.newVideoCount ?: 0

        FolderCard(
          folder = folder,
          uiSettings = uiSettings,
          isSelected = selectionManager.isSelected(folder),
          isRecentlyPlayed = isRecentlyPlayed,
          isNeverPlayed = isNeverPlayed,
          isWatched = (folder.videoCount > 0 || folder.audioCount > 0) && folder.unwatchedVideoCount == 0,
          onClick = { onFolderClick(folder) },
          onLongClick = { onFolderLongClick(folder) },
          onThumbClick = if (tapThumbnailToSelect) {
            { onFolderLongClick(folder) }
          } else {
            { onFolderClick(folder) }
          },
          newVideoCount = newCount,
          isGridMode = true,
          gridColumns = folderGridColumns,
        )
      }
    }

    // Scrollbar with bottom padding
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = navigationBarHeight)
    ) {
      LazyVerticalGridScrollbar(
        state = gridState,
        settings = ScrollbarSettings(
          thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
          thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
        ),
      ) {
        // Empty content - scrollbar only
      }
    }
  }
}

@Composable
private fun ListContent(
  folders: List<VideoFolder>,
  foldersWithNewCount: List<FolderWithNewCount>,
  uiSettings: UiSettings,
  recentlyPlayedFilePath: String?,
  playedFolderPaths: Set<String>,
  tapThumbnailToSelect: Boolean,
  navigationBarHeight: androidx.compose.ui.unit.Dp,
  listState: LazyListState,
  scrollbarAlpha: Float,
  selectionManager: app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager<VideoFolder, String>,
  onFolderClick: (VideoFolder) -> Unit,
  onFolderLongClick: (VideoFolder) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(2.dp),
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        bottom = navigationBarHeight
      ),
    ) {
      items(folders) { folder ->
        val isRecentlyPlayed = recentlyPlayedFilePath?.let {
          java.io.File(it).parent == folder.path
        } ?: false
        val isNeverPlayed = folder.path !in playedFolderPaths
        val newCount = foldersWithNewCount
          .find { it.folder.bucketId == folder.bucketId }
          ?.newVideoCount ?: 0

        FolderCard(
          folder = folder,
          uiSettings = uiSettings,
          isSelected = selectionManager.isSelected(folder),
          isRecentlyPlayed = isRecentlyPlayed,
          isNeverPlayed = isNeverPlayed,
          isWatched = (folder.videoCount > 0 || folder.audioCount > 0) && folder.unwatchedVideoCount == 0,
          onClick = { onFolderClick(folder) },
          onLongClick = { onFolderLongClick(folder) },
          onThumbClick = if (tapThumbnailToSelect) {
            { onFolderLongClick(folder) }
          } else {
            { onFolderClick(folder) }
          },
          newVideoCount = newCount,
          isGridMode = false,
        )
      }
    }

    // Scrollbar with bottom padding
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = navigationBarHeight)
    ) {
      LazyColumnScrollbar(
        state = listState,
        settings = ScrollbarSettings(
          thumbUnselectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f * scrollbarAlpha),
          thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = scrollbarAlpha),
        ),
      ) {
        // Empty content - scrollbar only
      }
      
      // Show background enrichment progress if list is visible but still processing

    }
  }
}



/**
 * Searches for folders and videos matching the query
 * Returns FileSystemItem results containing matching folders and videos
 */
private suspend fun searchFoldersAndVideos(
  context: Context,
  query: String,
): List<FileSystemItem> {
  val results = mutableListOf<FileSystemItem>()
  
  try {
    Log.d("FolderListScreen", "Searching for: $query")
    
    // Get all video folders
    val folders = MediaFileRepository.getAllVideoFoldersFast(context)
    
    // Search in folders
    folders.forEach { folder: app.marlboroadvance.mpvex.domain.media.model.VideoFolder ->
      if (folder.name.contains(query, ignoreCase = true) || 
          folder.path.contains(query, ignoreCase = true)) {
        results.add(
          FileSystemItem.Folder(
            name = folder.name,
            path = folder.path,
            lastModified = folder.lastModified,
            videoCount = folder.videoCount,
            totalSize = folder.totalSize,
            totalDuration = folder.totalDuration,
            hasSubfolders = false, // Not easily known during search
            newCount = folder.newCount
          )
        )
      }
      
      // Also search within videos in this folder
      val videos = app.marlboroadvance.mpvex.repository.MediaFileRepository
        .getVideosInFolder(context, folder.bucketId)
      
      videos.forEach { video ->
        if (video.displayName.contains(query, ignoreCase = true)) {
          results.add(
            FileSystemItem.VideoFile(
              name = video.displayName,
              path = video.path,
              lastModified = video.dateModified,
              video = video,
            )
          )
        }
      }
    }
    
    Log.d("FolderListScreen", "Found ${results.size} results for: $query")
  } catch (e: Exception) {
    Log.e("FolderListScreen", "Error searching folders and videos", e)
  }
  
  return results
}
