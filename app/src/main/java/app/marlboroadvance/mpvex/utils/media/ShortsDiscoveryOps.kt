package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.util.Log
import app.marlboroadvance.mpvex.database.dao.ShortsMediaDao
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.utils.storage.CoreMediaScanner
import app.marlboroadvance.mpvex.utils.storage.VideoScanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Operations for discovering and filtering vertical videos (Shorts).
 */
object ShortsDiscoveryOps {
    private const val TAG = "ShortsDiscoveryOps"

    /**
     * Discovers all vertical videos across all scanned media folders.
     */
    suspend fun discoverShorts(
        context: Context,
        shortsMediaDao: ShortsMediaDao,
        metadataCache: VideoMetadataCacheRepository,
        browserPreferences: BrowserPreferences
    ): List<Video> = withContext(Dispatchers.IO) {
        try {
            // 1. Get all folders that contain media
            val flatFolders = CoreMediaScanner.getFlatMediaFolders(context)
            
            // 2. Extract all videos from these folders
            val allVideos = flatFolders.flatMap { folder ->
                VideoScanUtils.getVideosInFolder(context, folder.path)
            }.filter { !it.isAudio }

            // 3. Enrich videos with metadata
            val enrichedVideos = MetadataRetrieval.enrichVideosIfNeeded(
                context, allVideos, browserPreferences, metadataCache
            )

            // 4. Get shorts metadata from DB
            val shortsMetadata = shortsMediaDao.getAllShortsMedia().associateBy { it.path }
            
            // 5. Get Discovery Preferences
            val includeHorizontal = browserPreferences.includeShortHorizontalVideos.get()
            val maxHorizontalMs = browserPreferences.maxHorizontalVideoDurationMinutes.get() * 60 * 1000L

            // 6. Filter for shorts based on orientation or user-defined short-duration preference
            enrichedVideos.filter { video ->
                val metadata = shortsMetadata[video.path]
                val isBlocked = metadata?.isBlocked ?: false
                if (isBlocked) return@filter false

                val isManuallyAdded = metadata?.isManuallyAdded ?: false
                
                var width = video.width
                var height = video.height
                
                // Force dimension extraction if missing
                if (width == 0 || height == 0) {
                   val file = java.io.File(video.path)
                   if (file.exists()) {
                       val meta = metadataCache.getOrExtractMetadata(file, video.uri, video.displayName)
                       if (meta != null) {
                           width = meta.width
                           height = meta.height
                       }
                   }
                }

                val isVertical = height > width && height > 0
                
                // --- Phase 3: Horizontal Inclusion Logic ---
                val isShortHorizontal = includeHorizontal && 
                                        height <= width && 
                                        video.duration > 0 && 
                                        video.duration <= maxHorizontalMs

                isVertical || isManuallyAdded || isShortHorizontal
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering shorts", e)
            emptyList()
        }
    }
}
