package music.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val genres: List<String> = emptyList()
)