package xyz.mpv.rex.utils.media

import android.content.Context
import android.util.Log
import xyz.mpv.rex.domain.media.model.VideoFolder
import xyz.mpv.rex.domain.playbackstate.repository.PlaybackStateRepository
import xyz.mpv.rex.preferences.AppearancePreferences
import xyz.mpv.rex.preferences.BrowserPreferences
import xyz.mpv.rex.utils.storage.CoreMediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/**
 * Operations for mapping media files to high-level domain models like [VideoFolder].
 */
object MediaMetadataOps {
    private const val TAG = "MediaMetadataOps"

    /**
     * Scans and maps all folders containing media into [VideoFolder] domain objects.
     */
    suspend fun getAllMediaFolders(context: Context): List<VideoFolder> =
        withContext(Dispatchers.IO) {
            try {
                val koin = GlobalContext.get()
                val browserPreferences = koin.get<BrowserPreferences>()
                val appearancePreferences = koin.get<AppearancePreferences>()
                val playbackStateRepository = koin.get<PlaybackStateRepository>()
                
                val isAudioEnabled = browserPreferences.showAudioFiles.get()
                val playbackStates = playbackStateRepository.getAllPlaybackStates()
                val thresholdDays = appearancePreferences.unplayedOldVideoDays.get()
                
                val foldersPreferences = koin.get<xyz.mpv.rex.preferences.FoldersPreferences>()
                val blacklistedFolders = foldersPreferences.blacklistedFolders.get()
                
                val folders = CoreMediaScanner.getFlatMediaFolders(
                    context = context, 
                    playbackStates = playbackStates, 
                    thresholdDays = thresholdDays,
                    blacklistedFolders = blacklistedFolders
                )
                folders
                    .filter { folder -> 
                        (isAudioEnabled || folder.videoCount > 0) && folder.path !in blacklistedFolders
                    }
                    .map { folder ->
                        VideoFolder(
                            bucketId = folder.id,
                            name = folder.name,
                            path = folder.path,
                            videoCount = folder.videoCount,
                            audioCount = folder.audioCount,
                            totalSize = folder.totalSize,
                            totalDuration = folder.totalDuration,
                            lastModified = folder.lastModified,
                            newCount = folder.newCount,
                            unwatchedVideoCount = folder.unwatchedVideoCount
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping media folders", e)
                emptyList()
            }
        }
}
