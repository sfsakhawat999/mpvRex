package xyz.mpv.rex.features

/**
 * Programming-law compliant core registry for standalone feature modules.
 * This class handles global constants and path references without touching the upstream source code.
 */
object RexFeaturesRegistry {
    const val CINEHUB_PACKAGE = "xyz.mpv.rex.features.cinehub"
    const val CINETUBE_PACKAGE = "xyz.mpv.rex.features.cinetube"

    // Absolute local storage directories for dynamic NFO auto-sync scanning loops
    val CineHubLocalMoviesPath = "/sdcard/CineRex/movies"
    val CineHubLocalTvShowsPath = "/sdcard/CineRex/tvshows"

    /**
     * Enum structure representing standalone app feature screens inside the main tab navigator
     */
    enum class FeatureTab {
        CINEHUB,
        CINETUBE
    }
}
