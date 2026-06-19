package xyz.mpv.rex.utils.storage

import android.util.Log
import java.io.File

/**
 * File Filter Utilities
 * Handles file and folder filtering logic
 */
object FileFilterUtils {
    private const val TAG = "FileFilterUtils"

    // Folders to skip during scanning (system/cache folders)
    private val SKIP_FOLDERS = setOf(
        // System & OS Junk
        "android", "data", "obb", "system", "lost.dir", ".android_secure", "android_secure",

        // Hidden & Temp Files
        ".thumbnails", "thumbnails", "thumbs", ".thumbs",
        ".cache", "cache", "temp", "tmp", ".temp", ".tmp",

        // Trash & Recycle Bins
        ".trash", "trash", ".trashbin", ".trashed", "recycle", "recycler",

        // App Clutters
        "log", "logs", "backup", "backups",
        "stickers", "whatsapp stickers", "telegram stickers"
    )

    /**
     * Checks if a folder contains a .nomedia file
     */
    fun hasNoMediaFile(folder: File): Boolean {
        if (!folder.isDirectory || !folder.canRead()) {
            return false
        }

        return try {
            val noMediaFile = File(folder, ".nomedia")
            noMediaFile.exists()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking for .nomedia file in: ${folder.absolutePath}", e)
            false
        }
    }

    /**
     * Checks if a folder should be skipped during scanning
     */
    fun shouldSkipFolder(folder: File): Boolean {
        if (hasNoMediaFile(folder)) {
            return true
        }

        val name = folder.name.lowercase()
        val isHidden = name.startsWith(".")
        return isHidden || SKIP_FOLDERS.contains(name)
    }

    /**
     * Checks if a file should be skipped during file listing
     */
    fun shouldSkipFile(file: File): Boolean {
        return file.name.startsWith(".")
    }
}
