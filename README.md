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
  <img src="https://img.shields.io/github/stars/sfsakhawat999/mpvRex?style=flat&logo=github" />
</p>

mpvRex is an advanced, customizable video player for Android. It combines the versatility of libmpv with a modern Jetpack Compose interface and unique user-centric features.

---

## Showcase

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/player.png" width="92%">
  <p><i>Player UI — Material You adaptive controls, seek capsule OSD, and gesture zones</i></p>
</div>

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/videoscreen.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/pip.png" width="31%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/about.png" width="31%">
</div>
<p align="center"><i>Video browser · Picture-in-picture · About screen</i></p>

<div class="image-row" align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/playlistwindow.png" width="48%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/moresheet.png" width="48%">
</div>
<p align="center"><i>Playlist window · File options sheet</i></p>

---

## Features

### 🎬 Playback & Gestures

- **Seek Cancellation** — cancel a seek mid-gesture by dragging backwards, with interactive pointer-scaling feedback animations
- **Subtitle Drag-to-Reposition** — tap and drag subtitles vertically to position them anywhere on screen
- **Subtitle Swipe Seeking** — swipe horizontally to jump precisely between subtitle lines
- **Top Seek Capsule OSD** — pill-shaped overlay showing double-tap seek feedback without blocking the video
- **Dynamic A-B Loop & Frame Navigation** — set loop points with adjustable vertical bias; fine-tune with a floating, non-colliding frame-by-frame panel
- **Persistent Video Pan & Zoom** — zoom and position settings saved per video, with quick sliders in the Aspect Ratio menu
- **Keyboard-Free Sleep Timer** — touch-friendly bottom sheet to schedule sleep times without opening a keyboard
- **Onboarding Tutorials** — step-by-step in-player guides for speed lock and subtitle gestures
- **Refined Tap & Lock Logic** — custom exclusion zones, optional seekbar tap prevention, and one-tap control lock

### 🎨 UI & Aesthetics

- **Dynamic Tab Manager** — hide, show, and reorder dashboard tabs to fully customize your bottom navigation
- **Material You** — player controls dynamically match your Android system accent or app theme
- **Animated Splash Screen** — custom vector-art launch animation with light/dark mode support
- **Clean Preferences** — organized Jetpack Compose preference screens with fully localized strings

### 🗂️ File Explorer & Media Library

- **Unified Explorer Engine** — ensures every browsing mode (local storage, network shares, and playlists) looks, feels, and behaves identically
- **Multi-Select Range** — select a range of items easily by long-pressing the first file and tapping the last
- **Sectioned Grid/List Layouts** — independently customizable inside tree subdirectories
- **Folder Metadata** — recursive file counts, watched/unplayed dimming, and reactive "NEW" badges
- **Breadcrumb Navigation** — toggleable path breadcrumbs in the tree view
- **Advanced Sorting** — by Name, Date, Size, and Duration
- **Network Streaming Proxy** — high-performance proxy for WebDAV, SMB, and FTP streams with image preview caching
- **Mark As System** — mark videos as watched, skipped, or flagged; filter your library accordingly
- **Media Library View** — browse your full video collection outside the file tree
- **Shorts Mode** — vertical video playback with auto-swipe for Reels-style content

### ⚙️ Engine & Customization

- **HDR-to-SDR Tone Mapping** — high-quality tone mapping via `hdr-toys` shader pipeline
- **Smart Orientation** — force landscape/portrait per video, stored as a preference
- **Audio Support** — scan, display, and play standalone audio files directly inside the file explorer and player

### ⚡ Performance

- **Battery-Optimized Playback** — optimized playback engine designed to maximize battery life during long viewing sessions
- **Async UI Animations** — seekbar animations run in coroutine contexts, off the UI thread
- **Smart Background Service** — background playback service starts only when actually backgrounded
- **Compose Recomposition Tuning** — minimized unnecessary recompositions across all player controls

---

## Installation

<div align="center">
  <a href="https://github.com/sfsakhawat999/mpvRex/releases">
    <img src="https://img.shields.io/badge/Download-Stable_Release-blue?style=for-the-badge&logo=github" alt="Stable Release">
  </a>
  <a href="https://sfsakhawat999.github.io/mpvRex">
    <img src="https://img.shields.io/badge/Download-Preview_Build-orange?style=for-the-badge&logo=github" alt="Preview Release">
  </a>
</div>

<p align="center"><i>Preview builds may be unstable and are intended for testing only.</i></p>

---

## Translations

mpvRex is no longer hosted on Weblate. Instead, translations can be managed using **[Droidlate](https://github.com/estiaksoyeb/Droidlate)** ([PyPI](https://pypi.org/project/droidlate/)) — a local, web-based, Weblate-like UI designed for editing Android `strings.xml` translation files.

If you would like to contribute to translating mpvRex into your language, you can run Droidlate locally to update the files and submit a pull request.

---

## Credits

mpvRex has its roots in **[mpvEx](https://github.com/marlboro-advance/mpvEx)**, which itself builds on **[mpv-android](https://github.com/mpv-android/mpv-android)**. We're grateful for the foundation they laid.

Additional inspiration and reference:
[mpvKt](https://github.com/abdallahmehiz/mpvKt) · [Next Player](https://github.com/anilbeesetti/nextplayer) · [Gramophone](https://github.com/FoedusProgramme/Gramophone)

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for details.
