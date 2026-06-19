package xyz.mpv.rex.utils.history

enum class MarkAsState {
    New,        // Remove playback progress — card appears unplayed/unwatched
    LastPlayed, // Bump recently-played timestamp to now
    Finished,   // Set position = duration, mark hasBeenWatched = true
    None,       // Delete all history and playback state for the file
}
