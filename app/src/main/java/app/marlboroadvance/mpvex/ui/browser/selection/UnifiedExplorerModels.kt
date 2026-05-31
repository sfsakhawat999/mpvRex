package app.marlboroadvance.mpvex.ui.browser.selection

import android.content.Context

/**
 * Standard item types for the Unified Explorer UI
 */
enum class ItemType {
  FOLDER,
  VIDEO,
  PLAYLIST,
  RECENT
}

/**
 * Polymorphic item representing any selectable/displayable card in lists and grids
 */
interface ExplorerItem {
  val id: String
  val title: String
  val subtitle: String?
  val type: ItemType
  val originalObject: Any
}

data class FolderExplorerItem(
  override val id: String,
  override val title: String,
  override val subtitle: String?,
  override val originalObject: Any
) : ExplorerItem {
  override val type: ItemType = ItemType.FOLDER
}

data class VideoExplorerItem(
  override val id: String,
  override val title: String,
  override val subtitle: String?,
  override val originalObject: Any
) : ExplorerItem {
  override val type: ItemType = ItemType.VIDEO
}

data class PlaylistExplorerItem(
  override val id: String,
  override val title: String,
  override val subtitle: String?,
  override val originalObject: Any
) : ExplorerItem {
  override val type: ItemType = ItemType.PLAYLIST
}

data class RecentExplorerItem(
  override val id: String,
  override val title: String,
  override val subtitle: String?,
  override val originalObject: Any
) : ExplorerItem {
  override val type: ItemType = ItemType.RECENT
}

/**
 * Strategy interface driving data filter operations and custom item actions in Unified Explorer
 */
interface ExplorerStrategy {
  /**
   * Filter the given list of items based on a search query
   */
  fun filterItems(items: List<ExplorerItem>, query: String): List<ExplorerItem> {
    if (query.isBlank()) return items
    return items.filter {
      it.title.contains(query, ignoreCase = true) ||
          (it.subtitle?.contains(query, ignoreCase = true) == true)
    }
  }
}
