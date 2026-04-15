# mpvRex Exclusive Features

This document tracks the unique features and improvements implemented in **mpvRex** that are not present in the upstream **mpvEx** project.

---

### 1. Advanced Gesture Controls

*   **Subtitle Swipe Seeking**
    *   ([bbbb10b](https://github.com/sfsakhawat999/mpvRex/commit/bbbb10bf22e0f393f00064d524324989ee6207a5)) - Initial implementation of subtitle seeking gesture and primary subtitle indicator.
    *   ([cc4a629](https://github.com/sfsakhawat999/mpvRex/commit/cc4a6295b1fbbb0b23da448fb5d1d7602b94bb4b)) - Refinement: Implement swipe to subtitle seek specifically in top/bottom screen regions.
    *   ([3f71d5e](https://github.com/sfsakhawat999/mpvRex/commit/3f71d5e5148b305f3f38a8d6ab6443271176a536)) - Refinement: Improved fallback to normal seek and subtitle availability checks.

*   **Instant Single Tap Logic**
    *   ([2719ccf](https://github.com/sfsakhawat999/mpvRex/commit/2719ccf541d5ed7f57513a3029e5c510b23ecf41)) - Initial implementation with exclusion zones.
    *   ([0ffa87e](https://github.com/sfsakhawat999/mpvRex/commit/0ffa87eb31a618edc553b1f2facc05d40271e441)) - Refinement: Added single tap gesture support for left and right regions.
    *   ([68cbede](https://github.com/sfsakhawat999/mpvRex/commit/68cbedec928a6592338168af0a0e161c0e58927a)) - Refinement: Prevent single tap action after long press.
    *   ([f589580](https://github.com/sfsakhawat999/mpvRex/commit/f589580725ba6ff3f6e0c0ccdf8a05b1b8618701)) - Refinement: Prevent double tap gestures in single tap exclusion zones.

*   **Reverse Double Tap Gestures**
    *   ([50cb8f1](https://github.com/sfsakhawat999/mpvRex/commit/50cb8f1c9d28686c17def515d0ea590d7f5a766e)) - Option to reverse left and right double tap gestures.

*   **Accidental Tap Prevention**
    *   ([e305c26](https://github.com/sfsakhawat999/mpvRex/commit/e305c26b2a332805151633ac179b709b5a50c6ce)) - Implement accidental tap prevention and relative seeking for all seekbar types.

---

### 2. UI & Aesthetics

*   **Smart Orientation Mode**
    *   ([be4c9dc](https://github.com/sfsakhawat999/mpvRex/commit/be4c9dc586c73f2cd6ce9ff5d292e583fc9995cb)) - Initial implementation with per-video persistence.
    *   ([bdd7d99](https://github.com/sfsakhawat999/mpvRex/commit/bdd7d99b46907394b4fb704753b02a167894cf96)) - Refinement: Logic improvements and Video mode fallback.

*   **Enhanced Transitions**
    *   ([bf35b36](https://github.com/sfsakhawat999/mpvRex/commit/bf35b367bccb70aa951cd89695d29483031c2de1)) - Smooth fade-in transition when opening videos.
    *   ([9f5c751](https://github.com/sfsakhawat999/mpvRex/commit/9f5c751d1d3adb18a4cb45bb5af593235c360e25)) - Eliminated white flash and jumpy transitions on player exit.

*   **Custom Branding & Themes**
    *   ([40dc388](https://github.com/sfsakhawat999/mpvRex/commit/40dc3882e97e1ca2a1bbe4546b9295c95360a836)) - Custom branding, theme, and icons.
    *   ([f1eb7c2](https://github.com/sfsakhawat999/mpvRex/commit/f1eb7c2c1b82d6a24c9b14e5cac0d8dc484bed6a)) - Player Control "Always Dark Mode" option.
    *   ([8e9c0b8](https://github.com/sfsakhawat999/mpvRex/commit/8e9c0b85cc83d89ec4046a18ff095d5626d091d2)) - Option to adjust player gradient opacity.

*   **Layout Customization**
    *   ([aff35d1](https://github.com/sfsakhawat999/mpvRex/commit/aff35d1e5b411faa0eede9a532bc24d23eed576c)) - Bottom controls layout option in Player Layout settings.

---

### 3. Core Player Improvements

*   **Chapter Navigation**
    *   ([b226e80](https://github.com/sfsakhawat999/mpvRex/commit/b226e80ead8a8fd76a9eeedc8abd0f780649451b)) - Auto-scroll to current chapter in chapters sheet.

*   **Seekbar Enhancements**
    *   ([a469af8](https://github.com/sfsakhawat999/mpvRex/commit/a469af86ed47d57680c7c71d49259db4893f1dc0)) - Increased seekbar bottom margin to prevent overlap in portrait mode.
    *   ([df3e392](https://github.com/sfsakhawat999/mpvRex/commit/df3e3928846ff51d9ac64e16f6abd7bae2864366)) - Respected `showSeekBarWhenSeeking` preference in gesture seeking.

---

### 4. Modernized File Browser

*   **Standardized Architecture**
    *   **Ops/Manager Pattern:** Refactored from a procedural model to a highly modular architecture using specialized logic handlers (`FileSystemOps`, `MediaMetadataOps`).
    *   **Unified Media Scanner:** Centralized media discovery via `CoreMediaScanner` for consistent hierarchical scanning across local and network storage.
    *   **Standardized UI (BaseMediaCard):** All browser items now follow a unified card architecture for consistent 16:9 aspect ratios, reactive "NEW" badges, and recursive folder counts.
    *   **Reactive Browser State:** Implementation of `BaseBrowserViewModel` to ensure UI state remains synchronized with background playback events and history updates.

---

### 5. Build & Maintenance

*   **Signing & CI/CD**
    *   ([34a086a](https://github.com/sfsakhawat999/mpvRex/commit/34a086a00f0f38d383e94621a31fa0978af6d09d)) - Setup GitHub Actions workflows and project renaming.
    *   ([06f3ef5](https://github.com/sfsakhawat999/mpvRex/commit/06f3ef5982b00743c8b41b69fa03d83f7598e3f0)) - Conditional release signing configuration.
    *   ([99db28c](https://github.com/sfsakhawat999/mpvRex/commit/99db28ce52d133f0db983dc4c56e6dbb39fd0c47)) - Support for 4-part version numbers in release tags.
