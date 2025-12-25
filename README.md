# mpvRex

A personal fork of [mpvExtended](https://github.com/marlboro-advance/mpvEx) with additional customizations and features.

## About

mpvRex is built on top of mpvExtended, which itself is a fork of [mpv-android](https://github.com/mpv-android/mpv-android). This project adds custom features and tweaks tailored for personal use while maintaining compatibility with upstream updates.

### Original Project Credits
- **mpvExtended**: [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx) - The primary upstream project
- **mpv-android**: [mpv-android/mpv-android](https://github.com/mpv-android/mpv-android) - The original mpv player for Android
- **mpvKt**: [abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt) - Kotlin-based mpv implementation
- **Next Player**: [anilbeesetti/nextplayer](https://github.com/anilbeesetti/nextplayer) - UI/UX inspiration

---

## Custom Features

### Player Enhancements
- **Player Always Dark Mode** - Keep player controls in dark theme regardless of app theme setting
- **Bottom Controls Layout Toggle** - Switch between controls above or below the seekbar
- **Subtitle Seek Gesture** - Swipe horizontally near the bottom edge to seek between subtitles
- **Single Tap Seek** - Tap left/right side of the screen to seek backward/forward quickly
- **Relative Seeking** - Seek relative to current position for more natural navigation
- **Cache Size Customization** - Configure video cache size for smoother streaming playback
- **Enhanced Gesture System** - Improved tap detection and gesture handling

### Browser Improvements
- **View Mode Auto-Navigation** - Automatically navigate to home when switching between Tree View and Folder View
- **Tree View & Folder View** - Multiple ways to browse your media library

### Theme & Appearance
- **Custom Blue Theme** - Default theme colors matched to the app logo (#445E91)
- **AMOLED Black Mode** - Pure black backgrounds for OLED displays
- **Material You Support** - Dynamic colors on Android 12+

---

## Inherited Features from mpvExtended

- Material 3 Expressive Design
- Picture-in-Picture (PiP) support
- Background playback
- High-quality rendering via libmpv
- Network streaming (SMB/FTP/WebDAV)
- External subtitle and audio track support
- Custom playlist management
- Zoom gesture
- Search functionality
- File management operations
- Lua script support
- Advanced MPV configuration

---

## Building

### Prerequisites
- JDK 17
- Android SDK with build tools 34.0.0+
- Git

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Preview build
./gradlew assemblePreview
```

### APK Variants
- **universal**: Works on all devices
- **arm64-v8a**: Modern 64-bit ARM (recommended)
- **armeabi-v7a**: Older 32-bit ARM
- **x86 / x86_64**: Intel/AMD devices

---

## License

This project inherits the license from mpvExtended and mpv-android. See the upstream repositories for license details.