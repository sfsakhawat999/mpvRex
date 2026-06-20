package xyz.mpv.rex.cinemine.model

import xyz.mpv.rex.features.cinehub.model.MovieItem
import xyz.mpv.rex.features.cinehub.model.TvShowItem
import xyz.mpv.rex.features.cinetube.model.YoutubeVideo

enum class MineTab(val label: String) {
    UNIFIED("All-in-One"),
    CINEHUB_LOCAL("Local Hub"),
    CINETUBE("CineTube"),
    CINEHUB_ONLINE("Cloud Repo")
}

sealed class CineMineFeedItem {
    data class LocalMovie(val movie: MovieItem) : CineMineFeedItem()
    data class LocalTvShow(val tvShow: TvShowItem) : CineMineFeedItem()
    data class TubeStream(val video: YoutubeVideo) : CineMineFeedItem()
    data class CloudStream(val onlineMovie: MovieItem) : CineMineFeedItem()
}
