package xyz.mpv.rex.utils.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import xyz.mpv.rex.domain.media.model.Video
import xyz.mpv.rex.utils.media.MediaFormatter
import xyz.mpv.rex.utils.media.MediaInfoOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Video Scanning Utilities
 * Handles single-folder video file scanning and metadata extraction.
 */
object VideoScanUtils {
    private const val TAG = "VideoScanUtils"
    
    /**
     * Video metadata extracted from files
     */
    data class VideoMetadata(
        val duration: Long,
        val mimeType: String,
        val width: Int = 0,
        val height: Int = 0,
        val rotation: Int = 0,
        val artist: String = "",
        val album: String = "",
    )
    
    /**
     * Get all videos and audio in a specific folder
     * MediaStore first, filesystem fallback for external devices
     */
    suspend fun getVideosInFolder(
        context: Context,
        folderPath: String
    ): List<Video> = withContext(Dispatchers.IO) {
        val videosMap = mutableMapOf<String, Video>()
        
        // Try MediaStore first (fast)
        scanVideosFromMediaStore(context, folderPath, videosMap)
        scanAudioFromMediaStore(context, folderPath, videosMap)
        
        // Fallback to filesystem if MediaStore returned nothing
        val folder = File(folderPath)
        if (folder.exists() && folder.canRead() && videosMap.isEmpty()) {
            scanMediaFromFileSystem(context, folder, videosMap)
        }
        
        videosMap.values.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
    }
    
    /**
     * Scan videos from MediaStore
     */
    private fun scanVideosFromMediaStore(
        context: Context,
        folderPath: String,
        videosMap: MutableMap<String, Video>
    ) {
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        
        // Add orientation column for API 29+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Video.Media.ORIENTATION)
        }
        
        val selection = "${MediaStore.Video.Media.DATA} LIKE ? AND ${MediaStore.Video.Media.DATA} NOT LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%", "$folderPath/%/%")
        
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val orientationColumn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.ORIENTATION)
                } else -1
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    val file = File(path)
                    
                    // Only direct children
                    if (file.parent != folderPath) continue
                    if (!file.exists()) continue
                    
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn)
                    val title = file.nameWithoutExtension
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "video/*"
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val rotation = if (orientationColumn != -1) cursor.getInt(orientationColumn) else 0
                    
                    val uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    videosMap[path] = Video(
                        id = id,
                        title = title,
                        displayName = displayName,
                        path = path,
                        uri = uri,
                        duration = duration,
                        durationFormatted = MediaFormatter.formatDuration(duration),
                        size = size,
                        sizeFormatted = MediaFormatter.formatFileSize(size),
                        dateModified = dateModified,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        bucketId = folderPath,
                        bucketDisplayName = File(folderPath).name,
                        width = width,
                        height = height,
                        rotation = rotation,
                        fps = 0f,
                        resolution = MediaFormatter.formatResolution(width, height),
                        hasEmbeddedSubtitles = false,
                        subtitleCodec = "",
                        isAudio = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore video scan error", e)
        }
    }

    /**
     * Scan audio from MediaStore
     */
    private fun scanAudioFromMediaStore(
        context: Context,
        folderPath: String,
        videosMap: MutableMap<String, Video>
    ) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ? AND ${MediaStore.Audio.Media.DATA} NOT LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%", "$folderPath/%/%")
        
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn)
                    val file = File(path)
                    
                    // Only direct children
                    if (file.parent != folderPath) continue
                    if (!file.exists()) continue
                    
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn)
                    val title = file.nameWithoutExtension
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/*"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    
                    val uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    videosMap[path] = Video(
                        id = id,
                        title = title,
                        displayName = displayName,
                        path = path,
                        uri = uri,
                        duration = duration,
                        durationFormatted = MediaFormatter.formatDuration(duration),
                        size = size,
                        sizeFormatted = MediaFormatter.formatFileSize(size),
                        dateModified = dateModified,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        bucketId = folderPath,
                        bucketDisplayName = File(folderPath).name,
                        width = 0,
                        height = 0,
                        fps = 0f,
                        resolution = "",
                        hasEmbeddedSubtitles = false,
                        subtitleCodec = "",
                        isAudio = true,
                        artist = artist,
                        album = album
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore audio scan error", e)
        }
    }
    
    /**
     * Scan media from filesystem (fallback)
     */
    private fun scanMediaFromFileSystem(
        context: Context,
        folder: File,
        videosMap: MutableMap<String, Video>
    ) {
        try {
            val files = folder.listFiles() ?: return
            
            for (file in files) {
                try {
                    if (!file.isFile) continue
                    
                    val isVideo = FileTypeUtils.isVideoFile(file)
                    val isAudio = FileTypeUtils.isAudioFile(file)
                    
                    if (!isVideo && !isAudio) continue
                    
                    val path = file.absolutePath
                    if (videosMap.containsKey(path)) continue
                    
                    val uri = Uri.fromFile(file)
                    val displayName = file.name
                    val title = file.nameWithoutExtension
                    val size = file.length()
                    val dateModified = file.lastModified() / 1000
                    
                    // Extract metadata
                    val metadata = extractVideoMetadata(context, file)
                    
                    videosMap[path] = Video(
                        id = path.hashCode().toLong(),
                        title = title,
                        displayName = displayName,
                        path = path,
                        uri = uri,
                        duration = metadata.duration,
                        durationFormatted = MediaFormatter.formatDuration(metadata.duration),
                        size = size,
                        sizeFormatted = MediaFormatter.formatFileSize(size),
                        dateModified = dateModified,
                        dateAdded = dateModified,
                        mimeType = metadata.mimeType,
                        bucketId = folder.absolutePath,
                        bucketDisplayName = folder.name,
                        width = metadata.width,
                        height = metadata.height,
                        rotation = metadata.rotation,
                        fps = 0f,
                        resolution = if (isAudio) "" else MediaFormatter.formatResolution(metadata.width, metadata.height),
                        hasEmbeddedSubtitles = false,
                        subtitleCodec = "",
                        isAudio = isAudio,
                        artist = metadata.artist,
                        album = metadata.album
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing file: ${file.absolutePath}", e)
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Filesystem media scan error", e)
        }
    }
    
    /**
     * Extracts video metadata using MediaInfo library
     */
    fun extractVideoMetadata(
        context: Context,
        file: File,
    ): VideoMetadata {
        var duration = 0L
        var mimeType = "video/*"
        var width = 0
        var height = 0
        var rotation = 0
        var artist = ""
        var album = ""
        
        try {
            val uri = Uri.fromFile(file)
            val result = runBlocking {
                MediaInfoOps.extractBasicMetadata(context, uri, file.name)
            }
            
            result.onSuccess { metadata ->
                duration = metadata.durationMs
                width = metadata.width
                height = metadata.height
                rotation = metadata.rotation
                artist = metadata.artist
                album = metadata.album
                mimeType = FileTypeUtils.getMimeTypeFromExtension(file.extension.lowercase())
            }.onFailure { e ->
                Log.w(TAG, "Could not extract metadata for ${file.absolutePath}, using fallback", e)
                mimeType = FileTypeUtils.getMimeTypeFromExtension(file.extension.lowercase())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract metadata for ${file.absolutePath}, using fallback", e)
            mimeType = FileTypeUtils.getMimeTypeFromExtension(file.extension.lowercase())
        }
        
        return VideoMetadata(duration, mimeType, width, height, rotation, artist, album)
    }
    
}
