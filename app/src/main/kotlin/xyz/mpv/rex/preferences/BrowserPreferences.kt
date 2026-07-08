package xyz.mpv.rex.preferences

import xyz.mpv.rex.R
import xyz.mpv.rex.preferences.preference.PreferenceStore
import xyz.mpv.rex.preferences.preference.getEnum

/**
 * Preferences for the video browser (folder and video lists)
 */
class BrowserPreferences(
  preferenceStore: PreferenceStore,
  context: android.content.Context,
) {
  // Folder sorting preferences
  val folderSortType = preferenceStore.getEnum("folder_sort_type", FolderSortType.Title)
  val folderSortOrder = preferenceStore.getEnum("folder_sort_order", SortOrder.Ascending)

  // Video sorting preferences
  val videoSortType = preferenceStore.getEnum("video_sort_type", VideoSortType.Title)
  val videoSortOrder = preferenceStore.getEnum("video_sort_order", SortOrder.Ascending)

  val folderViewMode = preferenceStore.getEnum("folder_view_mode", FolderViewMode.AlbumView)

  private val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
  val folderGridColumnsPortrait = preferenceStore.getInt("folder_grid_columns_portrait", if (isTablet) 4 else 3)
  val folderGridColumnsLandscape = preferenceStore.getInt("folder_grid_columns_landscape", 5)

  val videoGridColumnsPortrait = preferenceStore.getInt("video_grid_columns_portrait", if (isTablet) 4 else 2)
  val videoGridColumnsLandscape = preferenceStore.getInt("video_grid_columns_landscape", 4)

  // Visibility preferences for video card chips
  val showVideoThumbnails = preferenceStore.getBoolean("show_video_thumbnails", true)
  val showSizeChip = preferenceStore.getBoolean("show_size_chip", true)
  // Metadata-dependent chips (disabled by default for better performance)
  val showResolutionChip = preferenceStore.getBoolean("show_resolution_chip", false)
  val showFramerateInResolution = preferenceStore.getBoolean("show_framerate_in_resolution", false)
  val showSubtitleIndicator = preferenceStore.getBoolean("show_subtitle_indicator", false)
  val showProgressBar = preferenceStore.getBoolean("show_progress_bar", true)
  val mediaLayoutMode = preferenceStore.getEnum("media_layout_mode", MediaLayoutMode. LIST)

  // Visibility preferences for folder card chips
  val showTotalVideosChip = preferenceStore.getBoolean("show_total_videos_chip", true)
  // Metadata-dependent chips (disabled by default for better performance)
  val showTotalDurationChip = preferenceStore.getBoolean("show_total_duration_chip", false)
  val showTotalSizeChip = preferenceStore.getBoolean("show_total_size_chip", true)
  val showDateChip = preferenceStore.getBoolean("show_date_chip", false)
  val showFolderPath = preferenceStore.getBoolean("show_folder_path", true)

  // Auto-scroll to last played media preference (like MX Player)
  val autoScrollToLastPlayed = preferenceStore.getBoolean("auto_scroll_to_last_played", false)

  // Watched threshold preference (percentage 1-100)
  val watchedThreshold = preferenceStore.getInt("watched_threshold", 95)

  // Media visibility preferences
  val showAudioFiles = preferenceStore.getBoolean("show_audio_files", false)
  val showTreeViewPath = preferenceStore.getBoolean("show_tree_view_path", false)

  // RexShorts preferences
  val enableShorts = preferenceStore.getBoolean("enable_shorts", true)
  val autoSwipeShorts = preferenceStore.getBoolean("shorts_auto_swipe", false)
  val includeShortHorizontalVideos = preferenceStore.getBoolean("shorts_include_horizontal", false)
  val maxHorizontalVideoDurationMinutes = preferenceStore.getInt("shorts_max_horizontal_minutes", 1)
  val shortsSourceFolders = preferenceStore.getStringSet("shorts_source_folders", emptySet())

  // Bottom navigation visibility preferences
  val enableTabRecents = preferenceStore.getBoolean("enable_tab_recents", true)
  val enableTabPlaylists = preferenceStore.getBoolean("enable_tab_playlists", true)
  val enableTabNetwork = preferenceStore.getBoolean("enable_tab_network", true)
}

/**
 * Sort order options
 */
enum class SortOrder {
  Ascending,
  Descending,
  ;

  val isAscending: Boolean
    get() = this == Ascending
}

/**
 * Folder sorting options
 */
enum class FolderSortType {
  Title,
  Duration,
  Date,
  Size,
  VideoCount,
  ;

  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Duration -> "Duration"
        Date -> "Date"
        Size -> "Size"
        VideoCount -> "Count"
      }

  val displayNameResId: Int
    get() =
      when (this) {
        Title -> R.string.sort_type_title
        Duration -> R.string.sort_type_duration
        Date -> R.string.sort_type_date
        Size -> R.string.sort_type_size
        VideoCount -> R.string.sort_type_count
      }
}

/**
 * Video sorting options
 */
enum class VideoSortType {
  Title,
  Duration,
  Date,
  Size,
  ;

  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Duration -> "Duration"
        Date -> "Date"
        Size -> "Size"
      }

  val displayNameResId: Int
    get() =
      when (this) {
        Title -> R.string.sort_type_title
        Duration -> R.string.sort_type_duration
        Date -> R.string.sort_type_date
        Size -> R.string.sort_type_size
      }
}

/**
 * Folder view mode options
 */
enum class FolderViewMode {
  AlbumView,
  FileManager,
  MediaLibrary,
  ;

  val displayName: String
    get() =
      when (this) {
        AlbumView -> "Folder View"
        FileManager -> "Tree View"
        MediaLibrary -> "Media Library"
      }

  val displayNameResId: Int
    get() =
      when (this) {
        AlbumView -> R.string.sort_dialog_view_mode_folder
        FileManager -> R.string.sort_dialog_view_mode_tree
        MediaLibrary -> R.string.sort_dialog_view_mode_library
      }
}

enum class MediaLayoutMode {
  LIST,
  GRID,
  ;

  val displayName:  String
    get() = when (this) {
      LIST -> "List"
      GRID -> "Grid"
    }

  val displayNameResId: Int
    get() = when (this) {
      LIST -> R.string.sort_dialog_layout_list
      GRID -> R.string.sort_dialog_layout_grid
    }
}
