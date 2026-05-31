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

@Composable
fun <T> UnifiedExplorerContent(
  items: List<T>,
  isLoading: Boolean,
  uiSettings: UiSettings,
  isSelected: (T) -> Boolean,
  onClick: (T) -> Unit,
  onLongClick: (T) -> Unit,
  modifier: Modifier = Modifier,
  emptyTitle: String = "No items",
  emptyMessage: String = "This folder is empty",
  onThumbClick: ((T) -> Unit)? = null,
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

    if (mediaLayoutMode == MediaLayoutMode.GRID) {
      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(
          items = items,
          key = { getItemId(it) }
        ) { item ->
          ExplorerItemCard(
            item = item,
            isSelected = isSelected(item),
            showSubtitleIndicator = showSubtitleIndicator,
            isGridMode = true,
            columns = columns,
            uiSettings = uiSettings,
            onClick = { onClick(item) },
            onLongClick = { onLongClick(item) },
            onThumbClick = {
              if (onThumbClick != null) {
                onThumbClick(item)
              } else if (tapThumbnailToSelect) {
                onLongClick(item)
              } else {
                onClick(item)
              }
            }
          )
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(
          items = items,
          key = { getItemId(it) }
        ) { item ->
          ExplorerItemCard(
            item = item,
            isSelected = isSelected(item),
            showSubtitleIndicator = showSubtitleIndicator,
            isGridMode = false,
            columns = 1,
            uiSettings = uiSettings,
            onClick = { onClick(item) },
            onLongClick = { onLongClick(item) },
            onThumbClick = {
              if (onThumbClick != null) {
                onThumbClick(item)
              } else if (tapThumbnailToSelect) {
                onLongClick(item)
              } else {
                onClick(item)
              }
            }
          )
        }
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
) {
  when (item) {
    is VideoFolder -> {
      FolderCard(
        folder = item,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        isGridMode = isGridMode,
        gridColumns = columns
      )
    }
    is Video -> {
      VideoCard(
        video = item,
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
        isWatched = item.isWatched
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
      FolderCard(
        folder = folderModel,
        uiSettings = uiSettings,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        isGridMode = isGridMode,
        gridColumns = columns
      )
    }
    is FileSystemItem.VideoFile -> {
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
