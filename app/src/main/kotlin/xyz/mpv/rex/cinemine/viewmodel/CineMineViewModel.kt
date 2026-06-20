package xyz.mpv.rex.cinemine.viewmodel

import androidx.compose.runtime.*
import xyz.mpv.rex.cinemine.model.MineTab
import xyz.mpv.rex.features.cinehub.model.MovieItem
import xyz.mpv.rex.features.cinehub.model.TvShowItem
import xyz.mpv.rex.features.cinetube.model.YoutubeVideo

class CineMineViewModel {
    var searchQuery by mutableStateOf("")
    var activeTab by mutableStateOf(MineTab.UNIFIED)

    val filteredLocalMovies = mutableStateListOf<MovieItem>()
    val filteredLocalShows = mutableStateListOf<TvShowItem>()
    val filteredTubeVideos = mutableStateListOf<YoutubeVideo>()
    val filteredOnlineCloud = mutableStateListOf<MovieItem>()

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

        filteredLocalMovies.clear()
        filteredLocalMovies.addAll(movies.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) })

        filteredLocalShows.clear()
        filteredLocalShows.addAll(shows.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) })

        filteredTubeVideos.clear()
        filteredTubeVideos.addAll(tubeVideos.filter { it.title.lowercase().contains(target) })

        filteredOnlineCloud.clear()
        filteredOnlineCloud.addAll(cloudItems.filter { it.title.lowercase().contains(target) || it.genre.lowercase().contains(target) })
    }

    fun resetFeeds(
        movies: List<MovieItem>,
        shows: List<TvShowItem>,
        tubeVideos: List<YoutubeVideo>,
        cloudItems: List<MovieItem>
    ) {
        filteredLocalMovies.clear(); filteredLocalMovies.addAll(movies)
        filteredLocalShows.clear(); filteredLocalShows.addAll(shows)
        filteredTubeVideos.clear(); filteredTubeVideos.addAll(tubeVideos)
        filteredOnlineCloud.clear(); filteredOnlineCloud.addAll(cloudItems)
    }
}
