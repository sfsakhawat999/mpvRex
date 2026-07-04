# Contributing to mpvRex

Thank you for your interest in contributing to mpvRex! Whether you are writing code, designing features, reporting bugs, or translating the application, your help is highly appreciated.

This guide outlines our development practices, code guidelines, git workflow, and translation contribution process.

---

## 🐛 Reporting Bugs & Requesting Features

Before opening an issue, please search existing issues to avoid duplicates.

**Bug reports** should include:
* Device model and Android version
* mpvRex version (Settings → About)
* Steps to reproduce
* Expected vs. actual behavior, and a logcat snippet if available

**Feature requests** should describe the use case, not just the feature. If it's inspired by another player or fork, say so — it helps us evaluate fit rather than just novelty.

---

## 💻 Code Contributions

We welcome bug fixes, performance optimizations, and feature enhancements.

### Build Setup

* **SDK/JDK Requirements:** Java 17
* **Build Tool:** Gradle

#### Standard Build Commands
* **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
* **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```

---

### Core Architecture & Key Patterns

We follow a modular, **Ops/Manager-driven architecture** to keep our UI controllers thin and testing simple.

1. **Ops/Manager Pattern:**
   * Business logic belongs in manager classes, not ViewModels or Activities.
2. **Unified Media Scanning:**
   * Use `CoreMediaScanner` (via `FileSystemOps` and `MediaMetadataOps`) for all media file discovery to ensure folder counts and badge states are handled correctly.
3. **UI Consistency:**
   * Use `BaseMediaCard` for any list or grid media representations to maintain aspect ratio and badge styling consistency.

---

### Git Workflow

To keep the repository history clean and manageable, we adhere to the following Git rules:

* **Feature Branches:** Create a new branch for every feature or fix, branched off `master`.
* **Atomic Commits:** Separate structural refactoring from visual/functional fixes into distinct commits.
* **Keep Branches Updated:** Use `rebase` instead of merge to bring your branch up to date with `master`.
* **Commit Messages:** We follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g. `fix:`, `feat:`, `refactor:`, `chore:`) since our changelog is generated automatically via `git-cliff`. Non-conforming commit messages will need to be squashed or reworded before merge.

### Pull Requests

* Target the `master` branch.
* Keep PRs scoped to a single fix or feature — split unrelated changes into separate PRs.
* Reference the related issue number if one exists.
* Describe *what* changed and *why*; the diff already shows *how*.

---

## 🌐 Translation Contributions (Droidlate Workflow)

For translations, we use **[Droidlate](https://github.com/estiaksoyeb/Droidlate)** ([PyPI](https://pypi.org/project/droidlate/)) — a lightweight, local, web-based translation workspace designed specifically for Android `strings.xml` resource files.

### Why Droidlate?
* **Local Offline Workspace:** Translate and edit local files on your own machine without uploading resources to third-party servers.
* **Preserving XML Formatting & Comments:** Maintain exact XML styling, comments, structure, and formatting.
* **Tracking Outdated Translations:** Highlight which target translations need updates when base strings change.
* **Placeholder Verification & QA:** Validate Java-style placeholders to prevent runtime crashes.
* **Orphaned String Pruning:** Easily prune strings deleted from the main codebase.

---

### Step-by-Step Translation Workflow

#### 1. Install Droidlate
You can install Droidlate globally via `pipx` (recommended) or standard `pip`:

```bash
# Using pipx (recommended)
pipx install droidlate

# Or using normal pip
pip install droidlate
```

#### 2. Prepare the Repository
Ensure you have cloned the latest code for mpvRex:
```bash
git clone https://github.com/sfsakhawat999/mpvRex.git
cd mpvRex
```

#### 3. Start Droidlate
Open your terminal in the root of the mpvRex directory and run:
```bash
droidlate
```
Droidlate will start a local web server and open the interface in your default web browser (usually at `http://127.0.0.1:5000`).

#### 4. Adding a New Language
If your locale isn't listed on the dashboard:
1. Create a new directory named `values-<locale>` inside your resource directory (e.g., `app/src/main/res/values-<locale>`) following [Android's locale qualifier format](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources).
2. Place an empty `strings.xml` file inside that directory.
3. Restart Droidlate — it will detect the new locale, populate it with the base English strings, and allow you to begin translating.

#### 5. Translate in the Browser
On the dashboard, select your language locale to open the editor.
* Translate strings, view developer comments, and check for format placeholder warnings.
* Use keyboard shortcuts for productivity:
  * `Ctrl + S`: Instantly Save & Next.
  * `Alt + 1` / `Alt + 2`: Paste auto-translation suggestions (Google Translate or MyMemory).

#### 6. Prune Stale/Orphaned Keys
If any strings have been deleted from the English file, check the **"Orphaned"** tab in the editor to safely prune them and keep the files clean.

#### 7. Commit the Files & the Metadata Ledger
Saving changes updates:
1. The target XML file (e.g., `app/src/main/res/values-<locale>/strings.xml`)
2. A tracking file inside the `.translation_metadata/` directory (e.g., `.translation_metadata/values-<locale>.json`)

**Important:** You must commit both the updated `strings.xml` and its corresponding `.json` ledger file to Git.
