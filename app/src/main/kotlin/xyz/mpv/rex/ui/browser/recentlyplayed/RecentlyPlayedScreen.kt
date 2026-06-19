package xyz.mpv.rex.ui.browser.recentlyplayed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.mpv.rex.database.repository.PlaylistRepository
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.domain.media.model.VideoFolder
import xyz.mpv.rex.domain.thumbnail.ThumbnailRepository
import xyz.mpv.rex.preferences.AdvancedPreferences
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.preferences.GesturePreferences
import xyz.mpv.rex.preferences.MediaLayoutMode
import xyz.mpv.rex.preferences.UiSettings
import xyz.mpv.rex.preferences.preference.collectAsState
import xyz.mpv.rex.presentation.Screen
import xyz.mpv.rex.presentation.components.ConfirmDialog
import xyz.mpv.rex.presentation.components.pullrefresh.PullRefreshBox
import xyz.mpv.rex.ui.browser.cards.FolderCard
import xyz.mpv.rex.ui.browser.cards.PlaylistCard
import xyz.mpv.rex.ui.browser.cards.VideoCard
import xyz.mpv.rex.ui.browser.components.BrowserTopBar
import xyz.mpv.rex.ui.browser.components.UnifiedExplorerContent
import xyz.mpv.rex.ui.browser.playlist.PlaylistDetailScreen
import xyz.mpv.rex.ui.browser.selection.rememberSelectionManager
import xyz.mpv.rex.ui.browser.sheets.PlayLinkSheet
import xyz.mpv.rex.ui.browser.states.EmptyState
import xyz.mpv.rex.ui.utils.LocalBackStack
import xyz.mpv.rex.utils.media.MediaUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import java.io.File

@Serializable
object RecentlyPlayedScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val playlistRepository = koinInject<PlaylistRepository>()
    val viewModel: RecentlyPlayedViewModel =
      viewModel(factory = RecentlyPlayedViewModel.factory(context.applicationContext as android.app.Application))

    val recentItems by viewModel.recentItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uiSettings by viewModel.uiSettings.collectAsState()
    val recentlyPlayedFilePath by viewModel.recentlyPlayedFilePath.collectAsState()
    val deleteDialogOpen = rememberSaveable { mutableStateOf(false) }
    val deleteFilesCheckbox = rememberSaveable { mutableStateOf(false) }
    val advancedPreferences = koinInject<AdvancedPreferences>()
    val enableRecentlyPlayed by advancedPreferences.enableRecentlyPlayed.collectAsState()
    val navigationBarHeight = xyz.mpv.rex.ui.browser.LocalNavigationBarHeight.current

    val isFabVisible = remember { mutableStateOf(true) }
    val isFabExpanded = remember { mutableStateOf(false) }
    val showLinkDialog = remember { mutableStateOf(false) }
    val isRefreshing = remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Selection manager for all items (videos and playlists)
    val selectionManager =
      rememberSelectionManager(
        items = recentItems,
        getId = { item ->
          when (item) {
            is RecentlyPlayedItem.VideoItem -> "video_${item.video.id}"
            is RecentlyPlayedItem.PlaylistItem -> "playlist_${item.playlist.id}"
          }
        },
        onDeleteItems = { items, _ ->
          viewModel.deleteRecentItems(items)
        },
        onRenameItem = null, // Cannot rename from history screen
        onOperationComplete = { },
      )

    // Handle back button during selection mode or FAB menu expanded
    BackHandler(enabled = selectionManager.isInSelectionMode || isFabExpanded.value) {
      when {
        isFabExpanded.value -> isFabExpanded.value = false
        selectionManager.isInSelectionMode -> selectionManager.clear()
      }
    }
    
    // File picker for opening external files
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

    // Track scroll for FAB visibility - create states here to pass to content
    val listState = remember { LazyListState() }
    val gridState = remember { LazyGridState() }
    val browserPreferences = koinInject<BrowserPreferences>()
    val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
    xyz.mpv.rex.ui.browser.fab.FabScrollHelper.trackScrollForFabVisibility(
      listState = listState,
      gridState = if (mediaLayoutMode == MediaLayoutMode.GRID) gridState else null,
      isFabVisible = isFabVisible,
      expanded = isFabExpanded.value,
      onExpandedChange = { isFabExpanded.value = it },
    )

    LaunchedEffect(Unit) {
      xyz.mpv.rex.ui.browser.MainScreen.scrollToTopRequest.collect { tabId ->
        if (tabId == "recents") {
          coroutineScope.launch {
            if (mediaLayoutMode == MediaLayoutMode.GRID) {
              gridState.animateScrollToItem(0)
            } else {
              listState.animateScrollToItem(0)
            }
          }
        }
      }
    }

    Scaffold(
        topBar = {
          BrowserTopBar(
            title = "Recently Played",
            isInSelectionMode = selectionManager.isInSelectionMode,
            selectedCount = selectionManager.selectedCount,
            totalCount = recentItems.size,
            onBackClick = null, // No back button for recently played screen
            onCancelSelection = { selectionManager.clear() },
            onSortClick = null, // No sorting in recently played
            onSettingsClick = {
              backStack.add(xyz.mpv.rex.ui.preferences.PreferencesScreen)
            },
            isSingleSelection = selectionManager.isSingleSelection,
            onInfoClick = null, // No info in recently played
            onPlayClick = null,
            onSelectAll = { selectionManager.selectAll() },
            onInvertSelection = { selectionManager.invertSelection() },
            onDeselectAll = { selectionManager.clear() },
            onDeleteClick = { deleteDialogOpen.value = true },
          )
        },
      floatingActionButton = {
        FloatingActionButtonMenu(
          modifier = Modifier
            .padding(bottom = navigationBarHeight + 8.dp),
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
                modifier = Modifier
                  .animateFloatingActionButton(
                    visible = !selectionManager.isInSelectionMode && isFabVisible.value,
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
                val recentlyPlayedVideos = xyz.mpv.rex.utils.history.RecentlyPlayedOps.getRecentlyPlayed(limit = 1)
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
      when {
        !enableRecentlyPlayed -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            EmptyState(
              icon = Icons.Filled.History,
              title = "Recently Played is disabled",
              message = "Enable it in Advanced Settings to track your playback history",
            )
          }
        }

        isLoading && recentItems.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        recentItems.isEmpty() && !isLoading -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            contentAlignment = Alignment.Center,
          ) {
            EmptyState(
              icon = Icons.Filled.History,
              title = "No recently played videos",
              message = "Videos you play will appear here",
            )
          }
        }

        else -> {
          RecentItemsContent(
            recentItems = recentItems,
            playlistRepository = playlistRepository,
            uiSettings = uiSettings,
            recentlyPlayedFilePath = recentlyPlayedFilePath,
            selectionManager = selectionManager,
            onVideoClick = { video ->
              // Always play individual videos without creating a playlist
              // regardless of playlist mode setting
              MediaUtils.playFile(video, context, "recently_played")
            },
            onPlaylistClick = { playlistItem ->
              // Navigate to playlist detail screen
              backStack.add(PlaylistDetailScreen(playlistItem.playlist.id))
            },
            modifier = Modifier.padding(padding),
            isInSelectionMode = selectionManager.isInSelectionMode,
            listState = listState,
            gridState = gridState,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
          )
        }
      }

      // Delete confirmation dialog
      if (deleteDialogOpen.value && selectionManager.isInSelectionMode) {
        // Remove selected items from history
        val itemCount = selectionManager.selectedCount
        val itemText = if (itemCount == 1) "item" else "items"
        val deleteFiles = deleteFilesCheckbox.value

        val title = if (deleteFiles) {
          "Delete $itemCount $itemText?"
        } else {
          "Remove $itemCount $itemText from history?"
        }

        val subtitle = buildString {
          if (deleteFiles) {
            append("This will permanently delete the original video file(s) from your device storage.\n\n")
            append("This action cannot be undone.")
          } else {
            append("This will remove the selected $itemText from your recently played list. ")
            append("The original video files will not be deleted.")
          }
        }

        ConfirmDialog(
          title = title,
          subtitle = subtitle,
          customContent = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              androidx.compose.material3.Checkbox(
                checked = deleteFilesCheckbox.value,
                onCheckedChange = {
                  deleteFilesCheckbox.value = it
                },
              )
              Text(
                text = "Also delete original file(s)",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          },
          onConfirm = {
            selectionManager.deleteSelected(deleteFilesCheckbox.value)
            deleteDialogOpen.value = false
            deleteFilesCheckbox.value = false
          },
          onCancel = {
            deleteDialogOpen.value = false
            deleteFilesCheckbox.value = false
          },
        )
      }
      
      // Link dialog
      PlayLinkSheet(
        isOpen = showLinkDialog.value,
        onDismiss = { showLinkDialog.value = false },
        onPlayLink = { url -> MediaUtils.playFile(url, context, "play_link") },
      )
    }
  }
}

@Composable
private fun RecentItemsContent(
  recentItems: List<RecentlyPlayedItem>,
  playlistRepository: PlaylistRepository,
  uiSettings: UiSettings,
  recentlyPlayedFilePath: String?,
  selectionManager: xyz.mpv.rex.ui.browser.selection.SelectionManager<RecentlyPlayedItem, String>,
  onVideoClick: (Video) -> Unit,
  onPlaylistClick: suspend (RecentlyPlayedItem.PlaylistItem) -> Unit,
  modifier: Modifier = Modifier,
  isInSelectionMode: Boolean = false,
  listState: LazyListState,
  gridState: LazyGridState,
  isRefreshing: androidx.compose.runtime.MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()

  UnifiedExplorerContent(
    items = recentItems,
    isLoading = false,
    uiSettings = uiSettings,
    isSelected = { selectionManager.isSelected(it) },
    onClick = { item ->
      when (item) {
        is RecentlyPlayedItem.VideoItem -> onVideoClick(item.video)
        is RecentlyPlayedItem.PlaylistItem -> {
          coroutineScope.launch {
            onPlaylistClick(item)
          }
        }
      }
    },
    onLongClick = { item -> selectionManager.handleLongClick(item) },
    onToggleSelection = { selectionManager.toggle(it) },
    emptyTitle = "No recently played items",
    emptyMessage = "Your recently played videos and playlists will appear here",
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier,
    isInSelectionMode = isInSelectionMode,
    listState = listState,
    gridState = gridState,
  )
}
