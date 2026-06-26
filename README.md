# mpvRex

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" />
</p>

<p align="center">
  <b>Feature-rich Android video player based on libmpv.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" />
  <img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" />
  <img src="https://img.shields.io/badge/Kotlin-2.3.10-purple.svg" />
  <a href="https://github.com/sfsakhawat999/mpvRex/releases"><img src="https://img.shields.io/github/downloads/sfsakhawat999/mpvRex/total?logo=Github"/></a>
</p>

mpvRex is an advanced, customizable video player for Android. It combines the versatility of libmpv with a modern Jetpack Compose interface and unique user-centric features.

---

## Showcase

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/player.png" width="92%">
</div>

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videoscreen.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/pip.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/about.png" width="31%">
</div>

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/playlistwindow.png" width="48%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/moresheet.png" width="48%">
</div>

---

## Features

mpvRex is built on top of **mpvEx** (itself based on **mpv-android**), adding extensive gestures, UI customization, navigation features, and performance enhancements.

### 🎬 Advanced Playback & Touch Gestures
*   **Subtitle Drag-to-Reposition:** Tap and drag subtitles vertically on-screen to position them exactly where you want.
*   **Subtitle Swipe Seeking:** Swipe horizontally to jump precisely between subtitle text lines.
*   **Seek Cancellation:** Cancel a seek mid-gesture by dragging backwards/downwards, complete with interactive pointer-scaling feedback animations.
*   **Top Seek Capsule OSD:** Modern pill-shaped overlay at the top of the screen showing clean double-tap seek feedback instead of screen-blocking text.
*   **Dynamic A-B Loop & Frame Navigation:** Set loop points with adjustable vertical bias, and fine-tune playback with a floating, non-colliding Frame-by-Frame Navigation panel.
*   **Persistent Video Pan & Zoom:** Custom zoom and position settings are preserved per video, with quick sliders in the Aspect Ratio menu.
*   **Keyboard-Free Sleep Timer:** Refactored touch-friendly bottom sheet to quickly schedule player sleep times without opening a keyboard.
*   **Interactive Onboarding Tutorials:** Integrated step-by-step guides (`PlayerTutorialManager`) for speed lock and subtitle placement gestures.
*   **Refined Tap & Lock Logic:** Custom exclusion zones for single taps, optional accidental seekbar tap prevention, and a one-tap control lock.

### 🎨 Modern Compose UI & Aesthetics
*   **Material You Integration:** Adaptive player control colors that dynamically match your Android system accent or app theme.
*   **Animated Splash Screen:** Custom vector-art launch animation with full light/dark mode styling.
*   **Dynamic Tab Manager:** Personalize your browser dashboard by showing, hiding, or reordering bottom navigation tabs.
*   **Shorts Mode:** Tailored vertical video playback with auto-swipe support for vertical videos and Reels.
*   **Clean Preferences:** Organized Jetpack Compose preference screens with fully localized strings.

### 🗂️ Unified Explorer & Media Library
*   **Unified Explorer Engine (`UnifiedExplorerContent`):** Standardized, strategy-driven browser for Local Files, Folder list, Tree view, Network streams, and Playlists.
*   **Multi-Select Range Gestures:** Select multiple files or directories rapidly and securely with touch-and-drag.
*   **Sectioned Layouts:** Custom grid/list layouts for folders and media, customizable independently inside tree subdirectories.
*   **Folder Metadata Enrichment:** Recursive file counts, watched/unplayed dimmed styling, and reactive "NEW" badges.
*   **Breadcrumbs:** Toggleable path breadcrumbs at the top of the tree view for easy navigation.
*   **Advanced Sorting:** Redesigned sort options including sorting by Name, Date, Size, and Duration.
*   **Network Streaming Proxy:** Integrated high-performance proxy routing for WebDAV, SMB, and FTP streams to bypass standard player network limitations with image preview caching.

### ⚙️ Engine & Customization
*   **Ops/Manager Architecture:** Robust, decoupled player ViewModel delegating specialized tasks to dedicated managers (`PlaybackManager`, `PlaylistManager`, `SubtitleManager`, etc.).
*   **HDR-to-SDR Tone Mapping:** Support for high-quality tone-mapping shaders (`hdr-toys`).
*   **Custom Buttons & Scripts:** Direct support for custom user-created OSD buttons mapped to custom Lua scripts.
*   **Smart Orientation:** Custom orientation preferences (e.g. force landscape/portrait) stored per video.
*   **Audio Mode Support:** Load external audio tracks via a custom audio file picker dialog, or play audio files natively inside the media engine.
*   **Online Subtitle Search:** Integrated subtitle searching powered by Wyzie, with primary track indicators.

### ⚡ Performance & Efficiency
*   **Battery Optimization:** Rewritten video position tracking loop to drastically reduce JNI overhead, saving battery during long playback.
*   **UI Thread Offloading:** Wavy seekbar flattening animations executed asynchronously in coroutine contexts.
*   **Smart Background Playback:** Background audio/playback service starts only when the app is backgrounded, minimizing resource footprints.
*   **Compose Performance Tuning:** Minimized recompositions across player control elements.

---

## Installation

<div class="image-row" align="center">
  <a href="https://github.com/sfsakhawat999/mpvRex/releases">
    <img src="https://img.shields.io/badge/Download-Stable_Release-blue?style=for-the-badge&logo=github" alt="Stable Release">
  </a>
  <a href="https://sfsakhawat999.github.io/mpvRex">
    <img src="https://img.shields.io/badge/Download-Preview_Build-orange?style=for-the-badge&logo=github" alt="Preview Release">
  </a>
</div>

<div align="center">
  <i>Note: Previews may be unstable and are intended for testing purposes only.</i>
</div>

---

## Development

For detailed documentation on project architecture and developer environment, please refer to:
*   [GEMINI.md](GEMINI.md) - Architecture overview and dev environment.
*   [CODEMAP.md](CODEMAP.md) - Detailed file-level navigation and refactor status.

### Build Commands

```bash
# Local dev install (sets jniLibs from lib/, ABI to arm64-v8a only)
./gradlew installDebug -I local-env.gradle.kts

# Standard build (all ABIs)
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## Translations

We use [Weblate](https://weblate.org/) to manage translations. If you would like to help translate mpvRex into your language, you can do so on the translation project page.

[![Translation status](https://hosted.weblate.org/widgets/mpvrex/-/horizontal-auto.svg)](https://hosted.weblate.org/engage/mpvrex/)

*Hosting is provided for free by Hosted Weblate for libre software projects.*

---

## Credits

mpvRex is a fork of **[mpvEx](https://github.com/marlboro-advance/mpvEx)** (based on **[mpv-android](https://github.com/mpv-android/mpv-android)**). Special thanks for the foundation and inspiration:

[mpvEx](https://github.com/marlboro-advance/mpvEx) • [mpv-android](https://github.com/mpv-android) • [mpvKt](https://github.com/abdallahmehiz/mpvKt) • [Next player](https://github.com/anilbeesetti/nextplayer) • [Gramophone](https://github.com/FoedusProgramme/Gramophone)

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.
