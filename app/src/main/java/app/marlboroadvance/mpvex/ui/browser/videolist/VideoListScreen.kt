package app.marlboroadvance.mpvex.ui.browser.videolist

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import app.marlboroadvance.mpvex.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.FolderViewMode
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.preferences.VideoSortType
import app.marlboroadvance.mpvex.preferences.UiSettings
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.components.BrowserBottomBar
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.components.UnifiedExplorerContent
import app.marlboroadvance.mpvex.ui.browser.components.SelectionOverflowAction
import app.marlboroadvance.mpvex.ui.browser.dialogs.AddToPlaylistDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FileOperationProgressDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.FolderPickerDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.VideoSortDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.LoadingDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.MediaInfoSheet
import app.marlboroadvance.mpvex.ui.browser.sheets.MultiSelectionInfoSheet
import app.marlboroadvance.mpvex.ui.browser.fab.FabScrollHelper
import app.marlboroadvance.mpvex.ui.browser.selection.SelectionManager
import app.marlboroadvance.mpvex.ui.browser.selection.rememberSelectionManager
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.browser.sheets.MarkAsBottomSheet
import app.marlboroadvance.mpvex.utils.history.MarkAsState
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.sort.SortUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt


@Serializable
data class VideoListScreen(
  private val bucketId: String,
  private val folderName: String,
) : Screen {
  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backstack = LocalBackStack.current
    val clipboardManager = LocalClipboardManager.current
    val browserPreferences = koinInject<BrowserPreferences>()
    val playerPreferences = koinInject<PlayerPreferences>()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current

    // ViewModel
    val viewModel: VideoListViewModel =
      viewModel(
        key = "VideoListViewModel_$bucketId",
        factory = VideoListViewModel.factory(context.applicationContext as android.app.Application, bucketId),
      )
    val videos by viewModel.videos.collectAsState()
    val videosWithPlaybackInfo by viewModel.videosWithPlaybackInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uiSettings by viewModel.uiSettings.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val lastPlayedInFolderPath by viewModel.lastPlayedInFolderPath.collectAsState()
    val playlistMode by playerPreferences.playlistMode.collectAsState()
    val videosWereDeletedOrMoved by viewModel.videosWereDeletedOrMoved.collectAsState()

    // Sorting
    val videoSortType by browserPreferences.videoSortType.collectAsState()
    val videoSortOrder by browserPreferences.videoSortOrder.collectAsState()
    val sortedVideosWithInfo =
      remember(videosWithPlaybackInfo, videoSortType, videoSortOrder) {
        val infoById = videosWithPlaybackInfo.associateBy { it.video.id }
        val sortedVideos = SortUtils.sortVideos(videosWithPlaybackInfo.map { it.video }, videoSortType, videoSortOrder)
        // Maintain the playback info mapping — O(1) lookup per item
        sortedVideos.map { video ->
          infoById[video.id] ?: VideoWithPlaybackInfo(video)
        }
      }

    // Selection manager
    val selectionManager =
      rememberSelectionManager(
        items = sortedVideosWithInfo.map { it.video },
        getId = { it.id },
        onDeleteItems = { items, _ -> viewModel.deleteVideos(items) },
        onRenameItem = { video, newName -> viewModel.renameVideo(video, newName) },
        onOperationComplete = { viewModel.refresh() },
      )

    // UI State
    val isRefreshing = remember { mutableStateOf(false) }
    val sortDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val renameDialogOpen = rememberSaveable { mutableStateOf(false) }
    val addToPlaylistDialogOpen = rememberSaveable { mutableStateOf(false) }

    // Copy/Move state
    val folderPickerOpen = rememberSaveable { mutableStateOf(false) }
    val operationType = remember { mutableStateOf<CopyPasteOps.OperationType?>(null) }
    val progressDialogOpen = rememberSaveable { mutableStateOf(false) }
    val operationProgress by CopyPasteOps.operationProgress.collectAsState()
    val treePickerLauncher =
      rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val selectedVideos = selectionManager.getSelectedItems()
        if (selectedVideos.isEmpty() || operationType.value == null) return@rememberLauncherForActivityResult

        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        }

        progressDialogOpen.value = true
        coroutineScope.launch {
          when (operationType.value) {
            is CopyPasteOps.OperationType.Copy -> {
              CopyPasteOps.copyFilesToTreeUri(context, selectedVideos, uri)
            }

            is CopyPasteOps.OperationType.Move -> {
              CopyPasteOps.moveFilesToTreeUri(context, selectedVideos, uri)
            }

            else -> {}
          }
        }
      }

    // Private space state
    val movingToPrivateSpace = rememberSaveable { mutableStateOf(false) }
    val showPrivateSpaceCompletionDialog = rememberSaveable { mutableStateOf(false) }
    val privateSpaceMovedCount = remember { mutableIntStateOf(0) }

    val displayFolderName = videos.firstOrNull()?.bucketDisplayName ?: folderName
    var mediaInfoUri by remember { mutableStateOf<Uri?>(null) }
    var multiSelectionInfo by remember { mutableStateOf<Triple<Int, Long, Long>?>(null) }

    // FAB visibility state
    val isFabVisible = remember { mutableStateOf(true) }

    // Bottom bar animation state
    var showFloatingBottomBar by remember { mutableStateOf(false) }
    var showMarkAsSheet by remember { mutableStateOf(false) }
    val animationDuration = 300

    // Handle selection mode changes with animation
    LaunchedEffect(selectionManager.isInSelectionMode) {
      if (selectionManager.isInSelectionMode) {
        // Entering selection mode: Show floating bar immediately
        showFloatingBottomBar = true
      } else {
        // Exiting selection mode: Hide floating bar
        showFloatingBottomBar = false
      }
    }

    // Predictive back: Only intercept when in selection mode
    BackHandler(enabled = selectionManager.isInSelectionMode) {
      selectionManager.clear()
    }

    // Listen for lifecycle resume events and refresh videos when coming into focus
    DisposableEffect(lifecycleOwner) {
      val observer =
        LifecycleEventObserver { _, event ->
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
        BrowserTopBar(
          title = displayFolderName,
          isInSelectionMode = selectionManager.isInSelectionMode,
          selectedCount = selectionManager.selectedCount,
          totalCount = sortedVideosWithInfo.size,
          onBackClick = {
            if (selectionManager.isInSelectionMode) {
              selectionManager.clear()
            } else {
              backstack.removeLastOrNull()
            }
          },
          onCancelSelection = { selectionManager.clear() },
          onSortClick = { sortDialogOpen.value = true },
          onSettingsClick = {
            backstack.add(app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen)
          },
          isSingleSelection = selectionManager.isSingleSelection,
          onInfoClick = {
            val selected = selectionManager.getSelectedItems()
            if (selectionManager.isSingleSelection) {
              mediaInfoUri = selected.firstOrNull()?.uri
            } else {
              multiSelectionInfo = Triple(
                selected.size,
                selected.sumOf { it.size },
                selected.sumOf { it.duration },
              )
            }
          },
          onPlayClick = { selectionManager.playSelected() },
          selectionOverflowActions = buildList {
            add(
              SelectionOverflowAction(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = { selectionManager.shareSelected() },
              )
            )
            val selectedVideos = selectionManager.getSelectedItems()
            if (selectedVideos.isNotEmpty()) {
              add(
                SelectionOverflowAction(
                  icon = Icons.Filled.ContentCopy,
                  label = stringResource(R.string.copy_video_path),
                  onClick = {
                    val paths = selectedVideos.joinToString("\n") { it.path }
                    clipboardManager.setText(AnnotatedString(paths))
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                  }
                )
              )
            }
          },

          onSelectAll = { selectionManager.selectAll() },
          onInvertSelection = { selectionManager.invertSelection() },
          onDeselectAll = { selectionManager.clear() },
        )
      },
      floatingActionButton = {
        val navigationBarHeight = app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight.current
        if (sortedVideosWithInfo.isNotEmpty()) {
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
                  visible = !selectionManager.isInSelectionMode && isFabVisible.value,
                  alignment = Alignment.BottomEnd,
                ),
              onClick = {
                coroutineScope.launch {
                  val folderPath = sortedVideosWithInfo.firstOrNull()?.video?.path?.let { File(it).parent } ?: ""
                  val recentlyPlayedVideos = RecentlyPlayedOps.getRecentlyPlayed(limit = 100)
                  val lastPlayedInFolder = recentlyPlayedVideos.firstOrNull {
                    File(it.filePath).parent == folderPath
                  }

                  if (lastPlayedInFolder != null) {
                    MediaUtils.playFile(lastPlayedInFolder.filePath, context, "recently_played_button")
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
      
      Box(modifier = Modifier.fillMaxSize()) {
        VideoListContent(
          folderId = bucketId,
          videosWithInfo = sortedVideosWithInfo,
          isLoading = isLoading && videos.isEmpty(),
          uiSettings = uiSettings,
          isRefreshing = isRefreshing,
          recentlyPlayedFilePath = lastPlayedInFolderPath ?: recentlyPlayedFilePath,
          videosWereDeletedOrMoved = videosWereDeletedOrMoved,
          autoScrollToLastPlayed = autoScrollToLastPlayed,
          onRefresh = { viewModel.refresh() },
          selectionManager = selectionManager,
          onVideoClick = { video ->
            if (selectionManager.isInSelectionMode) {
              selectionManager.toggle(video)
            } else {
              // Always use MediaUtils.playFile which lets PlayerActivity auto-generate playlist
              // This avoids TransactionTooLargeException from passing large playlists
              // PlayerActivity will auto-generate playlist from folder if playlistMode is enabled
              MediaUtils.playFile(video, context, "video_list")
            }
          },
          onVideoLongClick = { video -> selectionManager.handleLongClick(video) },
          isFabVisible = isFabVisible,
          modifier = Modifier.padding(padding),
          showFloatingBottomBar = showFloatingBottomBar,
          sortType = videoSortType,
          sortOrder = videoSortOrder,
        )
        
        // Floating Material 3 Button Group overlay with animation
        // Play Store gating is intentionally bypassed here.
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
            onRenameClick = { renameDialogOpen.value = true },
            onDeleteClick = { deleteDialogOpen.value = true },
            onAddToPlaylistClick = { addToPlaylistDialogOpen.value = true },
            onMarkAsClick = { showMarkAsSheet = true },
            showRename = selectionManager.isSingleSelection
          )
        }
      }

      // Sort Dialog
      VideoSortDialog(
        isOpen = sortDialogOpen.value,
        onDismiss = { sortDialogOpen.value = false },
        sortType = videoSortType,
        sortOrder = videoSortOrder,
        onSortTypeChange = { browserPreferences.videoSortType.set(it) },
        onSortOrderChange = { browserPreferences.videoSortOrder.set(it) },
      )

      // Delete Dialog
      DeleteConfirmationDialog(
        isOpen = deleteDialogOpen.value,
        onDismiss = { deleteDialogOpen.value = false },
        onConfirm = { selectionManager.deleteSelected() },
        itemType = "video",
        itemCount = selectionManager.selectedCount,
        itemNames = selectionManager.getSelectedItems().map { it.displayName },
      )

      // Rename Dialog
      if (renameDialogOpen.value && selectionManager.isSingleSelection) {
        val video = selectionManager.getSelectedItems().firstOrNull()
        if (video != null) {
          val baseName = video.displayName.substringBeforeLast('.')
          val extension = "." + video.displayName.substringAfterLast('.', "")
          RenameDialog(
            isOpen = true,
            onDismiss = { renameDialogOpen.value = false },
            onConfirm = { newName -> selectionManager.renameSelected(newName) },
            currentName = baseName,
            itemType = "file",
            extension = if (extension != ".") extension else null,
          )
        }
      }

      // Mark As Sheet
      if (showMarkAsSheet) {
        MarkAsBottomSheet(
          onDismiss = { showMarkAsSheet = false },
          onMarkAs = { state ->
            val selected = selectionManager.getSelectedItems()
            coroutineScope.launch {
              selected.forEach { video ->
                RecentlyPlayedOps.markAs(
                  filePath = video.path,
                  fileName = video.displayName,
                  duration = video.duration,
                  state = state,
                )
              }
              viewModel.refresh()
            }
          },
        )
      }

      // Folder Picker Dialog
      FolderPickerDialog(
        isOpen = folderPickerOpen.value,
        currentPath =
          videos.firstOrNull()?.let { File(it.path).parent }
            ?: Environment.getExternalStorageDirectory().absolutePath,
        onDismiss = { folderPickerOpen.value = false },
        onFolderSelected = { destinationPath ->
          folderPickerOpen.value = false
          val selectedVideos = selectionManager.getSelectedItems()
          if (selectedVideos.isNotEmpty() && operationType.value != null) {
            progressDialogOpen.value = true
            coroutineScope.launch {
              when (operationType.value) {
                is CopyPasteOps.OperationType.Copy -> {
                  CopyPasteOps.copyFiles(context, selectedVideos, destinationPath)
                }

                is CopyPasteOps.OperationType.Move -> {
                  CopyPasteOps.moveFiles(context, selectedVideos, destinationPath)
                }

                else -> {}
              }
            }
          }
        },
      )

      // File Operation Progress Dialog
      if (operationType.value != null) {
        FileOperationProgressDialog(
          isOpen = progressDialogOpen.value,
          operationType = operationType.value!!,
          progress = operationProgress,
          onCancel = {
            CopyPasteOps.cancelOperation()
          },
          onDismiss = {
            progressDialogOpen.value = false
            // Set flag if move operation was successful
            if (operationType.value is CopyPasteOps.OperationType.Move &&
              operationProgress.isComplete &&
              operationProgress.error == null
            ) {
              viewModel.setVideosWereDeletedOrMoved()
            }
            operationType.value = null
            selectionManager.clear()
            viewModel.refresh()
          },
        )
      }

      // Private Space Loading Dialog
      LoadingDialog(
        isOpen = movingToPrivateSpace.value,
        message = "Moving to private space...",
      )

      // Private Space Completion Dialog
      if (showPrivateSpaceCompletionDialog.value) {
        androidx.compose.material3.AlertDialog(
          onDismissRequest = { showPrivateSpaceCompletionDialog.value = false },
          title = {
            Text(
              text = "Moved to Private Space",
              style = MaterialTheme.typography.headlineSmall,
            )
          },
          text = {
            Text(
              text =
                "Successfully moved ${privateSpaceMovedCount.intValue} video(s) to private space.\n\n" +
                  "To access private space, long press on the app name at the top of the main screen.",
              style = MaterialTheme.typography.bodyMedium,
            )
          },
          confirmButton = {
            androidx.compose.material3.Button(
              onClick = { showPrivateSpaceCompletionDialog.value = false },
            ) {
              Text("Close")
            }
          },
        )
      }

      // Add to Playlist Dialog
      AddToPlaylistDialog(
        isOpen = addToPlaylistDialogOpen.value,
        videos = selectionManager.getSelectedItems(),
        onDismiss = { addToPlaylistDialogOpen.value = false },
        onSuccess = {
          selectionManager.clear()
          viewModel.refresh()
        },
      )

      mediaInfoUri?.let { uri ->
        MediaInfoSheet(uri = uri, onDismiss = { mediaInfoUri = null })
      }
      multiSelectionInfo?.let { (count, bytes, duration) ->
        MultiSelectionInfoSheet(count = count, totalBytes = bytes, totalDurationMs = duration, onDismiss = { multiSelectionInfo = null })
      }
    }
  }
}

@Composable
fun VideoListContent(
  folderId: String,
  videosWithInfo: List<VideoWithPlaybackInfo>,
  isLoading: Boolean,
  uiSettings: UiSettings,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  recentlyPlayedFilePath: String?,
  videosWereDeletedOrMoved: Boolean,
  autoScrollToLastPlayed: Boolean,
  onRefresh: suspend () -> Unit,
  selectionManager: SelectionManager<Video, Long>,
  onVideoClick: (Video) -> Unit,
  onVideoLongClick: (Video) -> Unit,
  isFabVisible: androidx.compose.runtime.MutableState<Boolean>,
  modifier: Modifier = Modifier,
  showFloatingBottomBar: Boolean = false,
  sortType: VideoSortType = VideoSortType.Title,
  sortOrder: SortOrder = SortOrder.Ascending,
  searchQuery: String? = null,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
  val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
  val density = androidx.compose.ui.platform.LocalDensity.current
  val thumbWidthDp = if (mediaLayoutMode == MediaLayoutMode.GRID) 160.dp else 128.dp
  val aspect = 16f / 9f
  val thumbWidthPx = with(density) { thumbWidthDp.roundToPx() }
  val thumbHeightPx = (thumbWidthPx / aspect).toInt()

  LaunchedEffect(folderId, showVideoThumbnails, videosWithInfo.size, thumbWidthPx, thumbHeightPx) {
    if (showVideoThumbnails && videosWithInfo.isNotEmpty()) {
      thumbnailRepository.startFolderThumbnailGeneration(
        folderId = folderId,
        videos = videosWithInfo.map { it.video },
        widthPx = thumbWidthPx,
        heightPx = thumbHeightPx,
      )
    }
  }

  UnifiedExplorerContent(
    items = videosWithInfo,
    isLoading = isLoading,
    uiSettings = uiSettings,
    isSelected = { selectionManager.isSelected(it.video) },
    onClick = { onVideoClick(it.video) },
    onLongClick = { onVideoLongClick(it.video) },
    onToggleSelection = { selectionManager.toggle(it.video) },
    modifier = modifier,
    emptyTitle = if (searchQuery != null) {
      if (searchQuery.isBlank()) stringResource(R.string.search_empty_title) else stringResource(R.string.search_no_results_title)
    } else {
      "No videos in this folder"
    },
    emptyMessage = if (searchQuery != null) {
      if (searchQuery.isBlank()) stringResource(R.string.search_empty_message) else stringResource(R.string.search_no_results_message)
    } else {
      "Videos you add to this folder will appear here"
    },
    emptyIcon = if (searchQuery != null) Icons.Filled.Search else Icons.Filled.VideoLibrary,
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    isInSelectionMode = selectionManager.isInSelectionMode,
    recentlyPlayedFilePath = recentlyPlayedFilePath,
    autoScrollToLastPlayed = autoScrollToLastPlayed,
    scrollTriggerKey = "${sortType.name}:${sortOrder.name}"
  )
}


