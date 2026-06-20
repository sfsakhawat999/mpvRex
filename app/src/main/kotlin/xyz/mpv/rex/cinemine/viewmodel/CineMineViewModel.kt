package xyz.mpv.rex.cinemine.viewmodel

import androidx.compose.runtime.*
import xyz.mpv.rex.cinemine.model.MineTab
import xyz.mpv.rex.features.cinehub.model.MovieItem
import xyz.mpv.rex.features.cinehub.model.TvShowItem
import xyz.mpv.rex.features.cinetube.model.YoutubeVideo

class CineMineViewModel {
    // Top bar search framework queries mapping
    var searchQuery by mutableStateOf("")
    var activeTab by mutableStateOf(MineTab.UNIFIED)

    // Reactive State Lists for direct layout UI rows rendering injection
    val filteredLocalMovies = mutableStateListOf<MovieItem>()
    val filteredLocalShows = mutableStateListOf<TvShowItem>()
    val filteredTubeVideos = mutableStateListOf<YoutubeVideo>()
    val filteredOnlineCloud = mutableStateListOf<MovieItem>()

    /**
     * Deep Global Filtration across all 3 integrated subsystems simultaneously
     */
    fun updateSearchAndFilter(
        query: String,
        movies: List<MovieItem>,
        shows: List<TvShowItem>,
        tubeVideos: List<YoutubeVideo>,
        cloudItems: List<MovieItem>
    ) {
        searchQuery = query
        val target = query.trim().lowercase()

        if (target.isEmpty()) {
            resetFeeds(movies, shows, tubeVideos, cloudItems)
            return
        }

        // 1. CineHub Local Movies Filtration Pipeline
        filteredLocalMovies.clear()
        filteredLocalMovies.addAll(
            movies.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) }
        )

        // 2. CineHub Local TV Series Filtration Pipeline
        filteredLocalShows.clear()
        filteredLocalShows.addAll(
            shows.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) }
        )

        // 3. CineTube (Invidious Streaming Node) Filtration Pipeline
        filteredTubeVideos.clear()
        filteredTubeVideos.addAll(
            tubeVideos.filter { it.title.lowercase().contains(target) || it.author.lowercase().contains(target) }
        )

        // 4. CineHub Online (Cloud Server Channels) Filtration Pipeline
        filteredOnlineCloud.clear()
        filteredOnlineCloud.addAll(
            cloudItems.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) }
        )
    }

    /**
     * Restores dynamic datasets back to their pristine row structures instantly
     */
    fun resetFeeds(
        movies: List<MovieItem>,
        shows: List<TvShowItem>,
        tubeVideos: List<YoutubeVideo>,
        cloudItems: List<MovieItem>
    ) {
        filteredLocalMovies.clear()
        filteredLocalMovies.addAll(movies)
        
        filteredLocalShows.clear()
        filteredLocalShows.addAll(shows)
        
        filteredTubeVideos.clear()
        filteredTubeVideos.addAll(tubeVideos)
        
        filteredOnlineCloud.clear()
        filteredOnlineCloud.addAll(cloudItems)
    }
}
