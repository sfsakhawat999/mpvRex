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

## Origin and Acknowledgments

mpvRex is a fork of **[mpvEx](https://github.com/marlboro-advance/mpvEx)**, which is based on **[mpv-android](https://github.com/mpv-android/mpv-android)**. This project aims to extend the core functionality of these upstream repositories with specialized features and aesthetic refinements.

We extend our sincere gratitude to the developers of the following projects, whose work serves as the foundation for mpvRex:

*   **[mpvEx](https://github.com/marlboro-advance/mpvEx)** - Direct Upstream
*   **[mpv-android](https://github.com/mpv-android)** - Core Base
*   **[mpvKt](https://github.com/abdallahmehiz/mpvKt)** - UI Inspiration
*   **[Next player](https://github.com/anilbeesetti/nextplayer)** - Feature Ideas
*   **[Gramophone](https://github.com/FoedusProgramme/Gramophone)** - Design Concepts

---

## Features

For a detailed list of exclusive improvements and technical commits, see **[FEATURES_REX.md](FEATURES_REX.md)**.

### Advanced Gesture Controls
*   **Subtitle Swipe Seeking:** Intuitive swipe gestures to jump between subtitle lines.
*   **Instant Single Tap Logic:** Improved single tap response with exclusion zones and refined behavior.
*   **Reverse Double Tap Gestures:** Option to reverse left/right seek directions to match your preference.
*   **Accidental Tap Prevention:** Preference to ignore single taps on the seekbar to prevent accidental seeks.

### UI & Aesthetics
*   **Smart Orientation Mode:** Per-video persistence of orientation preferences with intelligent fallback logic.
*   **Enhanced Transitions:** Seamless fade-in when opening videos and elimination of white flashes on player exit.
*   **Custom Branding & Themes:** Dedicated theme, custom icons, and an "Always Dark Mode" option for controls.
*   **Layout Customization:** Option to place playback controls below the seekbar and adjust gradient opacity.

### Core Player Improvements
*   **Chapter Navigation:** Auto-scrolling chapter lists that stay synchronized with current playback.
*   **Seekbar Enhancements:** Increased bottom margins to prevent overlap and support for relative seeking.
*   **Primary Subtitle Indicator:** Visual indicator in the track menu showing the active primary subtitle.

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.
