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

Built on top of upstream mpvEx, mpvRex extends its full feature set with targeted optimizations and new capabilities.

*   **Subtitle Swipe Seeking:** Intuitive gestures to jump between subtitle lines.
*   **Refined Tap Logic:** Enhanced single-tap response with exclusion zones and reverse double-tap options.
*   **Accidental Seek Prevention:** Optional ignore-single-tap on seekbar to prevent mistakes.
*   **Smart Orientation:** Persistent per-video orientation preferences with intelligent fallback.
*   **Themed Player Controls:** Adaptive controls that dynamically match your app theme or system accent (Material You).
*   **Shorts Mode:** Optimized vertical playback experience with auto-swipe support for "Shorts" and Reels.
*   **Audio Support:** Integrated capability to play audio files directly within the media engine.
*   **Advanced Thumbnails:** Extraction strategy choice (First Frame vs. Specific Position) and network stream previews.
*   **Modern Aesthetics:** Seamless transitions, custom branding, and specialized "Always Dark Mode" for player.
*   **Modular Architecture:** Robust Ops/Manager-driven file browser with a unified discovery engine.
*   **Unified UI:** Standardized media cards featuring reactive "NEW" badges and recursive file/folder counts.
*   **Enhanced Navigation:** Auto-scrolling synchronized chapters and support for relative seeking.
*   **Subtitle Management:** Visual indicators for primary tracks and integrated online search.

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

## Credits

mpvRex is a fork of **[mpvEx](https://github.com/marlboro-advance/mpvEx)** (based on **[mpv-android](https://github.com/mpv-android/mpv-android)**). Special thanks for the foundation and inspiration:

[mpvEx](https://github.com/marlboro-advance/mpvEx) • [mpv-android](https://github.com/mpv-android) • [mpvKt](https://github.com/abdallahmehiz/mpvKt) • [Next player](https://github.com/anilbeesetti/nextplayer) • [Gramophone](https://github.com/FoedusProgramme/Gramophone)

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.
