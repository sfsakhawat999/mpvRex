package xyz.mpv.rex.cinemine.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CineMineStreamResolver {

    /**
     * Resolves incoming custom layout URI markers directly using the newly self-contained repo methods
     */
    suspend fun resolvePlaybackUrl(videoPath: String): String = withContext(Dispatchers.IO) {
        when {
            // 1. CineTube Live Redirection Node
            !videoPath.contains(":") && videoPath.length > 5 -> {
                CineMineRepo.fetchCineTubeDirectUrl(videoPath) ?: "https://www.youtube.com/watch?v=$videoPath"
            }
            
            // 2. CineMax (formerly CineHub Online) Decryption Handshake
            videoPath.startsWith("cnc_stream:") -> {
                val id = videoPath.substringAfter("cnc_stream:").substringBefore(":")
                val platform = videoPath.substringAfterLast(":")
                CineMineRepo.resolveCineMaxUrl(id, platform) ?: "https://net52.cc/mobile/player.php?id=$id"
            }
            
            // 3. CineHub Local Raw Filesystem Path Pass
            else -> {
                videoPath
            }
        }
    }
}
