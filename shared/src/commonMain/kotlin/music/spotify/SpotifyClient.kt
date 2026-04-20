package music.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import music.model.Album
import music.model.Playlist
import music.model.Track
import music.model.TrackOrigin
import music.model.Artist

class SpotifyClient(
    private val http: HttpClient,
    private val credentials: SpotifyCredentials
) {
    private var token: String? = null

    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val accessToken = ensureToken()
        val response: SpotifySearchResponse = http.get(
            "https://api.spotify.com/v1/search?type=track&limit=$limit&q=${q.encodeURLParameter()}"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.tracks.items.mapNotNull { item ->
            val preview = item.previewUrl ?: return@mapNotNull null
            Track(
                id = "spotify:${item.id}",
                title = item.name,
                artist = item.artists.firstOrNull()?.name ?: "Spotify",
                uri = preview,
                origin = TrackOrigin.REMOTE,
                coverUrl = item.album?.images?.firstOrNull()?.url
            )
        }
    }

    suspend fun searchPlaylists(query: String, limit: Int = 20): List<Playlist> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val accessToken = ensureToken()
        val response: SpotifyPlaylistSearchResponse = http.get(
            "https://api.spotify.com/v1/search?type=playlist&limit=$limit&q=${q.encodeURLParameter()}"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.playlists.items.mapNotNull { item ->
            // For playlists, we can't get tracks without additional API calls
            // So we'll create a playlist with empty tracks for now
            Playlist(
                id = "spotify-playlist:${item.id}",
                name = item.name,
                tracks = emptyList() // TODO: fetch tracks when needed
            )
        }
    }

    suspend fun getPlaylistTracks(playlistId: String): List<Track> {
        val id = playlistId.removePrefix("spotify-playlist:")
        val accessToken = ensureToken()
        val response: SpotifyPlaylistResponse = http.get(
            "https://api.spotify.com/v1/playlists/$id/tracks"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.items.mapNotNull { item ->
            val track = item.track
            val preview = track.previewUrl ?: return@mapNotNull null
            Track(
                id = "spotify:${track.id}",
                title = track.name,
                artist = track.artists.firstOrNull()?.name ?: "Spotify",
                uri = preview,
                origin = TrackOrigin.REMOTE,
                coverUrl = track.album?.images?.firstOrNull()?.url
            )
        }
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): List<Album> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val accessToken = ensureToken()
        val response: SpotifyAlbumSearchResponse = http.get(
            "https://api.spotify.com/v1/search?type=album&limit=$limit&q=${q.encodeURLParameter()}"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.albums.items.mapNotNull { item ->
            Album(
                id = "spotify-album:${item.id}",
                name = item.name,
                artist = item.artists.firstOrNull()?.name ?: "Desconocido",
                tracks = emptyList() // TODO: fetch tracks when needed
            )
        }
    }

    suspend fun searchArtists(query: String, limit: Int = 20): List<music.model.Artist> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val accessToken = ensureToken()
        val response: SpotifyArtistSearchResponse = http.get(
            "https://api.spotify.com/v1/search?type=artist&limit=$limit&q=${q.encodeURLParameter()}"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.artists.items.map { item ->
            music.model.Artist(
                id = "spotify-artist:${item.id}",
                name = item.name,
                imageUrl = item.images.firstOrNull()?.url,
                genres = item.genres
            )
        }
    }

    suspend fun recommendations(seedTrackId: String, limit: Int = 20): List<Track> {
        val accessToken = ensureToken()
        val response: SpotifyRecommendationsResponse = http.get(
            "https://api.spotify.com/v1/recommendations?limit=$limit&seed_tracks=${seedTrackId.encodeURLParameter()}"
        ) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return response.tracks.mapNotNull { item ->
            val preview = item.previewUrl ?: return@mapNotNull null
            Track(
                id = "spotify:${item.id}",
                title = item.name,
                artist = item.artists.firstOrNull()?.name ?: "Spotify",
                uri = preview,
                origin = TrackOrigin.REMOTE,
                coverUrl = item.album?.images?.firstOrNull()?.url
            )
        }
    }

    private suspend fun ensureToken(): String {
        token?.let { return it }

        val response: SpotifyTokenResponse = http.submitForm(
            url = "https://accounts.spotify.com/api/token",
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
            }
        ) {
            header("Authorization", "Basic ${credentials.basicAuth}")
        }.body()

        token = response.accessToken
        return response.accessToken
    }
}

@Serializable
private data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String
)

@Serializable
private data class SpotifySearchResponse(
    val tracks: SpotifyTracks
)

@Serializable
private data class SpotifyPlaylistSearchResponse(
    val playlists: SpotifyPlaylists
)

@Serializable
private data class SpotifyPlaylists(
    val items: List<SpotifyPlaylistItem>
)

@Serializable
private data class SpotifyPlaylistItem(
    val id: String,
    val name: String
)

@Serializable
private data class SpotifyTracks(
    val items: List<SpotifyTrackItem>
)

@Serializable
private data class SpotifyTrackItem(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum? = null,
    @SerialName("preview_url") val previewUrl: String? = null
)

@Serializable
private data class SpotifyAlbum(
    val images: List<SpotifyImage> = emptyList()
)

@Serializable
private data class SpotifyImage(
    val url: String
)

@Serializable
private data class SpotifyArtist(
    val name: String
)

@Serializable
private data class SpotifyRecommendationsResponse(
    val tracks: List<SpotifyTrackItem>
)

@Serializable
private data class SpotifyPlaylistResponse(
    val items: List<SpotifyPlaylistTrackItem>
)

@Serializable
private data class SpotifyAlbumSearchResponse(
    val albums: SpotifyAlbums
)

@Serializable
private data class SpotifyAlbums(
    val items: List<SpotifyAlbumItem>
)

@Serializable
private data class SpotifyAlbumItem(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist> = emptyList(),
    val images: List<SpotifyImage> = emptyList()
)

@Serializable
private data class SpotifyPlaylistTrackItem(
    val track: SpotifyTrackItem
)

@Serializable
private data class SpotifyArtistSearchResponse(
    val artists: SpotifyArtists
)

@Serializable
private data class SpotifyArtists(
    val items: List<SpotifyArtistItem>
)

@Serializable
private data class SpotifyArtistItem(
    val id: String,
    val name: String,
    val images: List<SpotifyImage> = emptyList(),
    val genres: List<String> = emptyList()
)
