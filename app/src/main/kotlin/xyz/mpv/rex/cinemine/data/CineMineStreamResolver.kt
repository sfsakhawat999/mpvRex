package xyz.mpv.rex.cinemine.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CineMineStreamResolver {

    suspend fun resolvePlaybackUrl(videoPath: String): String = withContext(Dispatchers.IO) {
        when {
            // 1. CineTube Direct Alpha-Numeric String ID Extractor Node
            !videoPath.contains(":") && videoPath.length > 5 -> {
                CineMineRepo.fetchCineTubeDirectUrl(videoPath) ?: "https://www.youtube.com/watch?v=$videoPath"
            }
            
            // 2. CineMax OTT Node Extraction Pipeline Handshake Encryption Channels
            videoPath.startsWith("cnc_stream:") -> {
                val id = videoPath.substringAfter("cnc_stream:").substringBefore(":")
                val platform = videoPath.substringAfterLast(":")
                CineMineRepo.resolveCineMaxUrl(id, platform) ?: "https://net52.cc/mobile/player.php?id=$id"
            }
            
            // 3. CineHub Local Path Fallback Pass Execution Chains
            else -> {
                videoPath
            }
        }
    }
}
