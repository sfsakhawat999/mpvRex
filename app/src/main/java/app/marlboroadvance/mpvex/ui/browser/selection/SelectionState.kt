package app.marlboroadvance.mpvex.ui.browser.selection

import androidx.compose.runtime.Stable

/**
 * State holder for item selection in browser screens
 */
@Stable
data class SelectionState<ID>(
  val selectedIds: Set<ID> = emptySet(),
  val lastSelectedId: ID? = null,
) {
  /**
   * Whether selection mode is active
   */
  val isInSelectionMode: Boolean
    get() = selectedIds.isNotEmpty()

  /**
   * Number of selected items
   */
  val selectedCount: Int
    get() = selectedIds.size

  /**
   * Whether a single item is selected
   */
  val isSingleSelection: Boolean
    get() = selectedIds.size == 1

  /**
   * Check if an item is selected
   */
  fun isSelected(id: ID): Boolean = selectedIds.contains(id)

  /**
   * Toggle selection of an item
   */
  fun toggle(id: ID): SelectionState<ID> {
    val isCurrentlySelected = selectedIds.contains(id)
    val nextSelectedIds = if (isCurrentlySelected) {
      selectedIds - id
    } else {
      selectedIds + id
    }
    return copy(
      selectedIds = nextSelectedIds,
      lastSelectedId = if (isCurrentlySelected) {
        if (lastSelectedId == id) nextSelectedIds.lastOrNull() else lastSelectedId
      } else {
        id
      }
    )
  }

  /**
   * Clear all selections
   */
  fun clear(): SelectionState<ID> = copy(selectedIds = emptySet(), lastSelectedId = null)

  /**
   * Select all items
   */
  fun selectAll(ids: List<ID>): SelectionState<ID> = copy(
    selectedIds = ids.toSet(),
    lastSelectedId = ids.lastOrNull()
  )

  /**
   * Invert selection (select unselected, unselect selected)
   */
  fun invertSelection(ids: List<ID>): SelectionState<ID> {
    val allIds = ids.toSet()
    val invertedIds = allIds - selectedIds
    return copy(
      selectedIds = invertedIds,
      lastSelectedId = invertedIds.lastOrNull()
    )
  }

  /**
   * Select a range of items from the lastSelectedId to the target id.
   */
  fun selectRange(targetId: ID, allIds: List<ID>): SelectionState<ID> {
    val anchor = lastSelectedId
    if (anchor == null || !allIds.contains(anchor)) {
      return toggle(targetId)
    }

    val startIndex = allIds.indexOf(anchor)
    val endIndex = allIds.indexOf(targetId)
    if (startIndex == -1 || endIndex == -1) {
      return toggle(targetId)
    }

    val start = minOf(startIndex, endIndex)
    val end = maxOf(startIndex, endIndex)
    val idsInRange = allIds.subList(start, end + 1)

    return copy(
      selectedIds = selectedIds + idsInRange,
      lastSelectedId = targetId
    )
  }

  /**
   * Get selected items from a list
   */
  fun <T> getSelected(
    items: List<T>,
    getId: (T) -> ID,
  ): List<T> = items.filter { selectedIds.contains(getId(it)) }
}
