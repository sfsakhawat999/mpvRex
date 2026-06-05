# Feature Origin and Implementation Comparison: mpvRx vs. mpvRex

---

## Purpose of This Document

This document is not intended as a legal accusation. Its purpose is to preserve a historical record of feature origins and implementation timelines across the two projects. The core concern addressed here is the repeated appearance of highly similar implementations in the mpvRx project. An additional observation is the absence of references to the corresponding mpvRex commits within the examined commits and files. This report exists for transparency and attribution purposes, documenting technical similarities for the community.

---

## Executive Summary

Multiple features and UI improvements appear in mpvRex commits that predate corresponding implementations observed in mpvRx. Several cases involve near-identical code structures, naming conventions, and runtime behaviors. This report documents these similarities, matching files, and their relative release timelines based on public git commits.

---

## Timeline of Comparable Implementations

| Feature / Fix | Original Commit (mpvRex) | mpvRx Commit | Time Difference | Similarity Type |
| :--- | :--- | :--- | :--- | :--- |
| **Swipe Subtitles to Seek** | [`cc4a629`](https://github.com/sfsakhawat999/mpvRex/commit/cc4a629) (Mar 10, 2026)<br>Author: `estiaksoyeb` | [`f014ca2`](https://github.com/Riteshp2001/mpvRx/commit/f014ca2) (Jun 1, 2026)<br>Author: `Arnab Sadhukhan` | ~83 days later | **Logic & Concept Similarity** |
| **Unified Media Library View** | [`4f2e04c`](https://github.com/sfsakhawat999/mpvRex/commit/4f2e04c) (May 15, 2026)<br>Author: `estiaksoyeb` | [`239b1bc`](https://github.com/Riteshp2001/mpvRx/commit/239b1bc) (May 31, 2026)<br>Author: `Riteshp2001` | 16 days later | **High Code Similarity** |
| **Multi-Select Range Selection** | [`da5dbd7`](https://github.com/sfsakhawat999/mpvRex/commit/da5dbd7) (May 31, 2026, 00:14)<br>Author: `estiaksoyeb` | [`f9bcba7`](https://github.com/Riteshp2001/mpvRx/commit/f9bcba7) (May 31, 2026, 10:05)<br>Author: `Riteshp2001` | **10 hours, 21 minutes later** | **Near-Identical Code Structure** |
| **Delete Button Selection Topbar Fix** | [`099c8d7`](https://github.com/sfsakhawat999/mpvRex/commit/099c8d7) (Jun 1, 2026, 02:40)<br>Author: `estiaksoyeb` | [`7108d6e`](https://github.com/Riteshp2001/mpvRx/commit/7108d6e) (Jun 2, 2026, 14:03)<br>Author: `Arnab Sadhukhan` | **~35 hours later** | **Similar UI Modification** |
| **Sort/View Dialog Centralization** | [`9e46b6d`](https://github.com/sfsakhawat999/mpvRex/commit/9e46b6d) (Jun 3, 2026, 14:13)<br>Author: `estiaksoyeb` | [`b5693d0`](https://github.com/Riteshp2001/mpvRx/commit/b5693d0) (Jun 3, 2026, 15:23)<br>Author: `Arnab Sadhukhan` | **1 hour, 40 minutes later** | **High Code Similarity** |

---

## Attribution Review

As of the date of this report:
- No references to the corresponding mpvRex commits were found in the examined mpvRx commits.
- No commit messages referencing the original mpvRex implementations were observed.
- No in-code comments attributing the compared implementations to mpvRex were identified.
- No references to the mpvRex repository or commits were identified in the examined commit messages, code comments, or pull request descriptions associated with the compared implementations.

This observation is limited to the commits and files examined in this report.

---

## Detailed Feature Comparisons

### Case 1: Unified Media Library View Mode
* **mpvRex Commit:** [`4f2e04c`](https://github.com/sfsakhawat999/mpvRex/commit/4f2e04c) (May 15, 2026)
* **mpvRx Commit:** [`239b1bc`](https://github.com/Riteshp2001/mpvRx/commit/239b1bc) (May 31, 2026)

#### Observed Similarities:
- **ViewModel Architecture (`MediaLibraryViewModel.kt`):** The class structures, variable names (`_videos`, `_videosWithPlaybackInfo`), Koin injection setup, and watch-progress calculations are highly similar.
- **UI Layout (`MediaLibraryContent.kt`):** The scaffold structure, search handling, selection manager bindings, back button behavior, and floating play action button match closely. The code differences are primarily limited to integration details with differences in underlying data models (e.g., mpvRex's `CoreMediaScanner` vs. mpvRx's older `FolderViewScanner`/`TreeViewScanner`).

---

### Case 2: Multi-Select Range Selection
* **mpvRex Commit:** [`da5dbd7`](https://github.com/sfsakhawat999/mpvRex/commit/da5dbd7) (Sun May 31, 2026, 00:14 +0600 — `May 30, 18:14 UTC`)
* **mpvRx Commit:** [`f9bcba7`](https://github.com/Riteshp2001/mpvRx/commit/f9bcba7) (Sun May 31, 2026, 10:05 +0530 — `May 31, 04:35 UTC`)
* **Time Difference:** **10 hours and 21 minutes**

#### Observed Code Similarities:
- **Identifiers:** Match on function names (`selectRange`, `selectRangeTo`), parameter names (`targetId`, `allIds`), and local variable names (`anchor`, `startIndex`, `endIndex`, `idsInRange`).
- **Control Flow:** Matching bounds checking and fallback behavior (returning early on missing anchor).
- **Strategy:** Identical approach of finding minimum/maximum indices and slicing the list via `subList` to resolve the selected range.

#### Technical Code Comparison (`SelectionState.kt`):
```kotlin
  fun selectRange(targetId: ID, allIds: List<ID>): SelectionState<ID> {
    val anchor = lastSelectedId
    if (anchor == null || !allIds.contains(anchor)) {
      return select(targetId) // Or toggle(targetId)
    }

    val startIndex = allIds.indexOf(anchor)
    val endIndex = allIds.indexOf(targetId)
    if (startIndex == -1 || endIndex == -1) {
      return select(targetId) // Or toggle(targetId)
    }

    val start = minOf(startIndex, endIndex)
    val end = maxOf(startIndex, endIndex)
    val idsInRange = allIds.subList(start, end + 1)

    return copy(
      selectedIds = selectedIds + idsInRange,
      lastSelectedId = targetId,
    )
  }
```

---

### Case 3: Sort & View Dialog Centralization
* **mpvRex Commit:** [`9e46b6d`](https://github.com/sfsakhawat999/mpvRex/commit/9e46b6d) (Wed Jun 3, 2026, 14:13 +0600 — `Jun 3, 08:13 UTC`)
* **mpvRx Commit:** [`b5693d0`](https://github.com/Riteshp2001/mpvRx/commit/b5693d0) (Wed Jun 3, 2026, 15:23 +0530 — `Jun 3, 09:53 UTC`)
* **Time Difference:** **1 hour and 40 minutes**

#### Observed Similarities:
The refactoring pattern to unify specific sorting dialogs (Folder, Video, FileSystem) into a centralized, modular `SortDialog` composable is highly similar. The base `SortDialog` implementation in both codebases shares identical parameter structures, layout components (including view mode selectors, layout mode selectors, grid column sliders, and visibility toggles), and data classes (`VisibilityToggle`, `ContentToggle`).

---

### Case 4: Subtitle Dialog Seek Gesture (Horizontal Swipe)
* **mpvRex Commit:** [`cc4a629`](https://github.com/sfsakhawat999/mpvRex/commit/cc4a629) (Mar 10, 2026)
* **mpvRx Commit:** [`f014ca2`](https://github.com/Riteshp2001/mpvRx/commit/f014ca2) (Jun 1, 2026)

#### Observed Similarities:
Both projects implemented a horizontal swipe gesture targeting subtitles to trigger previous/next subtitle seeking. While the gesture hit-zone calculations and configurations differ slightly to fit their respective layouts, both implementations configure the gesture to execute the MPV command `sub-seek` with directions `1` or `-1` and trigger a standard text overlay indicator.

---

### Case 5: Removal of Delete Button from Selection Topbar
* **mpvRex Commit:** [`099c8d7`](https://github.com/sfsakhawat999/mpvRex/commit/099c8d7) (Jun 1, 2026, 02:40 +0600)
* **mpvRx Commit:** [`7108d6e`](https://github.com/Riteshp2001/mpvRx/commit/7108d6e) (Jun 2, 2026, 14:03 +0530)

#### Observed Similarities:
Both projects removed the "delete" option from the selection mode top action bar within Tree and Folder list browser views, relocating or eliminating the action.

---

## Disclaimer

This report presents observed similarities and timelines. Readers should review the referenced commits and code comparisons and draw their own conclusions.

---

## Conclusion

A recurring pattern of highly similar implementations was observed between mpvRex and mpvRx. Several features and structural refactors appeared in the mpvRx codebase shortly after their introduction in mpvRex. While this report raises attribution concerns due to the high degree of similarity in code and timing, it does not attempt to determine intent, but only to document the historical record and technical similarities for reference.
