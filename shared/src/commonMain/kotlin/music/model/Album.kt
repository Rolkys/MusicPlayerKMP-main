package music.model

data class Album(
    val id: String = "",
    val name: String,
    val artist: String = "",
    val tracks: List<Track>
)
