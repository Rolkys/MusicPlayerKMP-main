package music.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import music.model.Album
import music.model.Playlist
import music.model.Track
import music.model.TrackOrigin

class DeezerClient(
    private val http: HttpClient
) {
    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val response: DeezerSearchResponse = http.get(
            "https://api.deezer.com/search?q=${q.encodeURLParameter()}&limit=$limit"
        ).body()

        return response.data.mapNotNull { item ->
            val preview = item.preview ?: return@mapNotNull null
            Track(
                id = "deezer:${item.id}",
                title = item.title,
                artist = item.artist.name,
                uri = preview,
                origin = TrackOrigin.REMOTE,
                coverUrl = item.album?.cover_medium
            )
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 20): List<Playlist> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val response: DeezerSearchResponse = http.get(
            "https://api.deezer.com/search/playlist?q=${q.encodeURLParameter()}&limit=$limit"
        ).body()

        return response.data.map { item ->
            Playlist(
                id = "deezer-playlist:${item.id}",
                name = item.title,
                tracks = emptyList() // Deezer no devuelve tracks en la búsqueda de playlists
            )
        }
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): List<Album> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val response: DeezerSearchResponse = http.get(
            "https://api.deezer.com/search/album?q=${q.encodeURLParameter()}&limit=$limit"
        ).body()

        return response.data.map { item ->
            Album(
                id = "deezer-album:${item.id}",
                name = item.title,
                artist = item.artist.name,
                tracks = emptyList() // TODO: fetch tracks when needed
            )
        }
    }
}

@Serializable
private data class DeezerSearchResponse(
    val data: List<DeezerTrackItem>
)

@Serializable
private data class DeezerTrackItem(
    val id: Long,
    val title: String,
    val preview: String? = null,
    val artist: DeezerArtist,
    val album: DeezerAlbum? = null
)

@Serializable
private data class DeezerArtist(
    val name: String
)

@Serializable
private data class DeezerAlbum(
    @SerialName("cover_medium") val cover_medium: String? = null
)