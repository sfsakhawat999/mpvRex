package app.marlboroadvance.mpvex.ui.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistWithCount
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistVideoItem
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedItem
import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.ui.browser.cards.FolderCard
import app.marlboroadvance.mpvex.ui.browser.cards.PlaylistCard
import app.marlboroadvance.mpvex.ui.browser.cards.VideoCard
import app.marlboroadvance.mpvex.ui.browser.videolist.VideoWithPlaybackInfo
import app.marlboroadvance.mpvex.preferences.UiSettings
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.pullrefresh.PullRefreshBox
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight

@Composable
fun <T> UnifiedExplorerContent(
  items: List<T>,
  isLoading: Boolean,
  uiSettings: UiSettings,
  isSelected: (T) -> Boolean,
  onClick: (T) -> Unit,
  onLongClick: (T) -> Unit,
  onToggleSelection: (T) -> Unit,
  modifier: Modifier = Modifier,
  emptyTitle: String = "No items",
  emptyMessage: String = "This folder is empty",
  onThumbClick: ((T) -> Unit)? = null,
  isRefreshing: MutableState<Boolean>? = null,
  onRefresh: (suspend () -> Unit)? = null,
  isInSelectionMode: Boolean = false,
  recentlyPlayedFilePath: String? = null,
  playedFolderPaths: Set<String> = emptySet(),
  newVideoIds: Set<Long> = emptySet(),
  watchedVideoIds: Set<Long> = emptySet(),
  autoScrollToLastPlayed: Boolean = false,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val gesturePreferences = koinInject<GesturePreferences>()

  val mediaLayoutMode by browserPreferences.mediaLayoutMode.collectAsState()
  val videoGridColumnsPortrait by browserPreferences.videoGridColumnsPortrait.collectAsState()
  val videoGridColumnsLandscape by browserPreferences.videoGridColumnsLandscape.collectAsState()

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
  val columns = if (isLandscape) videoGridColumnsLandscape else videoGridColumnsPortrait

  val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
  val showSubtitleIndicator by browserPreferences.showSubtitleIndicator.collectAsState()
  val navigationBarHeight = LocalNavigationBarHeight.current

  if (isLoading && items.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(bottom = 80.dp),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  } else if (items.isEmpty()) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      EmptyState(
        icon = Icons.Filled.VideoLibrary,
        title = emptyTitle,
        message = emptyMessage,
      )
    }
  } else {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    if (autoScrollToLastPlayed && recentlyPlayedFilePath != null && items.isNotEmpty()) {
      val lastPlayedIndex = items.indexOfFirst { item ->
        when (item) {
          is Video -> item.path == recentlyPlayedFilePath
          is VideoWithPlaybackInfo -> item.video.path == recentlyPlayedFilePath
          is RecentlyPlayedItem.VideoItem -> item.video.path == recentlyPlayedFilePath
          is FileSystemItem.VideoFile -> item.video.path == recentlyPlayedFilePath
          is VideoFolder -> recentlyPlayedFilePath.let { java.io.File(it).parent == item.path }
          is FileSystemItem.Folder -> recentlyPlayedFilePath.let { java.io.File(it).parent == item.path }
          else -> false
        }
      }
      if (lastPlayedIndex != -1) {
        LaunchedEffect(recentlyPlayedFilePath) {
          if (mediaLayoutMode == MediaLayoutMode.GRID) {
            gridState.scrollToItem(lastPlayedIndex)
          } else {
            listState.scrollToItem(lastPlayedIndex)
          }
        }
      }
    }

    val contentBlock: @Composable BoxScope.() -> Unit = {
      if (mediaLayoutMode == MediaLayoutMode.GRID) {
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          state = gridState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = navigationBarHeight + 8.dp
          ),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            items = items,
            key = { getItemId(it) }
          ) { item ->
            val effectiveOnClick = {
              if (isInSelectionMode) {
                onToggleSelection(item)
              } else {
                onClick(item)
              }
            }
            val effectiveOnThumbClick = {
              if (isInSelectionMode) {
                onToggleSelection(item)
              } else if (onThumbClick != null) {
                onThumbClick(item)
              } else if (tapThumbnailToSelect) {
                onToggleSelection(item)
              } else {
                onClick(item)
              }
            }

            ExplorerItemCard(
              item = item,
              isSelected = isSelected(item),
              showSubtitleIndicator = showSubtitleIndicator,
              isGridMode = true,
              columns = columns,
              uiSettings = uiSettings,
              onClick = effectiveOnClick,
              onLongClick = { onLongClick(item) },
              onThumbClick = effectiveOnThumbClick,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              playedFolderPaths = playedFolderPaths,
              newVideoIds = newVideoIds,
              watchedVideoIds = watchedVideoIds
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = navigationBarHeight + 8.dp
          ),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            items = items,
            key = { getItemId(it) }
          ) { item ->
            val effectiveOnClick = {
              if (isInSelectionMode) {
                onToggleSelection(item)
              } else {
                onClick(item)
              }
            }
            val effectiveOnThumbClick = {
              if (isInSelectionMode) {
                onToggleSelection(item)
              } else if (onThumbClick != null) {
                onThumbClick(item)
              } else if (tapThumbnailToSelect) {
                onToggleSelection(item)
              } else {
                onClick(item)
              }
            }

            ExplorerItemCard(
              item = item,
              isSelected = isSelected(item),
              showSubtitleIndicator = showSubtitleIndicator,
              isGridMode = false,
              columns = 1,
              uiSettings = uiSettings,
              onClick = effectiveOnClick,
              onLongClick = { onLongClick(item) },
              onThumbClick = effectiveOnThumbClick,
              recentlyPlayedFilePath = recentlyPlayedFilePath,
              playedFolderPaths = playedFolderPaths,
              newVideoIds = newVideoIds,
              watchedVideoIds = watchedVideoIds
            )
          }
        }
      }
    }

    if (isRefreshing != null && onRefresh != null) {
      PullRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        listState = listState,
        content = contentBlock
      )
    } else {
      Box(modifier = modifier.fillMaxSize()) {
        contentBlock()
      }
    }
  }
}

private fun <T> getItemId(item: T): String {
  return when (item) {
    is VideoFolder -> item.bucketId
    is Video -> item.path
    is VideoWithPlaybackInfo -> item.video.path
    is PlaylistWithCount -> item.playlist.id.toString()
    is RecentlyPlayedItem.VideoItem -> item.video.path
    is RecentlyPlayedItem.PlaylistItem -> item.playlist.id.toString()
    is FileSystemItem.Folder -> item.path
    is FileSystemItem.VideoFile -> item.path
    is PlaylistVideoItem -> item.playlistItem.id.toString()
    else -> item.hashCode().toString()
  }
}

@Composable
private fun <T> ExplorerItemCard(
  item: T,
  isSelected: Boolean,
  showSubtitleIndicator: Boolean,
  isGridMode: Boolean,
  columns: Int,
  uiSettings: UiSettings,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onThumbClick: () -> Unit,
  recentlyPlayedFilePath: String? = null,
  playedFolderPaths: Set<String> = emptySet(),
  newVideoIds: Set<Long> = emptySet(),
  watchedVideoIds: Set<Long> = emptySet(),
) {
  when (item) {
    is VideoFolder -> {
      val isRecentlyPlayed = recentlyPlayedFilePath?.let {
        java.io.File(it).parent == item.path
      } ?: false
      val isNeverPlayed = item.path !in playedFolderPaths
      val isWatched = (item.videoCount > 0 || item.audioCount > 0) && item.unwatchedVideoCount == 0

      FolderCard(
        folder = item,
        uiSettings = uiSettings,
        isSelected = isSelected,
        isRecentlyPlayed = isRecentlyPlayed,
        isNeverPlayed = isNeverPlayed,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        newVideoCount = item.newCount
      )
    }
    is Video -> {
      val isOldAndUnplayed = newVideoIds.contains(item.id)
      val isWatched = watchedVideoIds.contains(item.id)
      val isRecentlyPlayed = recentlyPlayedFilePath == item.path

      VideoCard(
        video = item,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        showSubtitleIndicator = showSubtitleIndicator,
        isOldAndUnplayed = isOldAndUnplayed,
        isWatched = isWatched,
        isRecentlyPlayed = isRecentlyPlayed
      )
    }
    is VideoWithPlaybackInfo -> {
      VideoCard(
        video = item.video,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        showSubtitleIndicator = showSubtitleIndicator,
        progressPercentage = item.progressPercentage,
        isWatched = item.isWatched,
        isOldAndUnplayed = item.isOldAndUnplayed,
        isNeverPlayed = item.isNeverPlayed
      )
    }
    is PlaylistWithCount -> {
      PlaylistCard(
        playlist = item.playlist,
        itemCount = item.itemCount,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        thumbnailSize = if (isGridMode) 160.dp else 128.dp,
        thumbnailAspectRatio = 16f / 9f
      )
    }
    is RecentlyPlayedItem.VideoItem -> {
      VideoCard(
        video = item.video,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        showSubtitleIndicator = showSubtitleIndicator,
        progressPercentage = item.progress,
        isWatched = item.isWatched
      )
    }
    is RecentlyPlayedItem.PlaylistItem -> {
      PlaylistCard(
        playlist = item.playlist,
        itemCount = item.videoCount,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        thumbnailSize = if (isGridMode) 160.dp else 128.dp,
        thumbnailAspectRatio = 16f / 9f
      )
    }
    is FileSystemItem.Folder -> {
      val folderModel = VideoFolder(
        bucketId = item.path,
        name = item.name,
        path = item.path,
        videoCount = item.videoCount,
        audioCount = item.audioCount,
        totalSize = item.totalSize,
        totalDuration = item.totalDuration,
        lastModified = item.lastModified / 1000,
        newCount = item.newCount,
        unwatchedVideoCount = item.unwatchedVideoCount,
      )
      val isRecentlyPlayed = recentlyPlayedFilePath?.let {
        java.io.File(it).parent == item.path
      } ?: false
      val isNeverPlayed = item.path !in playedFolderPaths
      val isWatched = (item.videoCount > 0 || item.audioCount > 0) && item.unwatchedVideoCount == 0

      FolderCard(
        folder = folderModel,
        uiSettings = uiSettings,
        isSelected = isSelected,
        isRecentlyPlayed = isRecentlyPlayed,
        isNeverPlayed = isNeverPlayed,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        newVideoCount = item.newCount
      )
    }
    is FileSystemItem.VideoFile -> {
      val isOldAndUnplayed = newVideoIds.contains(item.video.id)
      val isWatched = watchedVideoIds.contains(item.video.id)
      val isRecentlyPlayed = recentlyPlayedFilePath == item.video.path

      VideoCard(
        video = item.video,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        showSubtitleIndicator = showSubtitleIndicator,
        isOldAndUnplayed = isOldAndUnplayed,
        isWatched = isWatched,
        isRecentlyPlayed = isRecentlyPlayed
      )
    }
    is PlaylistVideoItem -> {
      VideoCard(
        video = item.video,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        onThumbClick = onThumbClick,
        isGridMode = isGridMode,
        gridColumns = columns,
        showSubtitleIndicator = showSubtitleIndicator
      )
    }
  }
}
