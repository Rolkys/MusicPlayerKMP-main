package music.model

enum class TrackOrigin {
    LOCAL,
    REMOTE
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val durationMs: Long? = null,
    val origin: TrackOrigin,
    val coverUrl: String? = null
)
