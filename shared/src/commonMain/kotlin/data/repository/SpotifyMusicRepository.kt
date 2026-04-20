package data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import music.model.Album
import music.model.Artist
import music.model.Playlist
import music.model.Track
import music.spotify.SpotifyClient

class SpotifyMusicRepository(
    private val spotifyClient: SpotifyClient
) : MusicRepository {

    // In-memory storage for now - later replace with database
    private val _savedTracks = MutableStateFlow<List<Track>>(emptyList())
    val savedTracks: Flow<List<Track>> = _savedTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: Flow<List<Playlist>> = _playlists.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: Flow<List<Track>> = _recentTracks.asStateFlow()

    override suspend fun searchTracks(query: String): List<Track> {
        return spotifyClient.searchTracks(query)
    }

    override suspend fun searchPlaylists(query: String): List<Playlist> {
        return spotifyClient.searchPlaylists(query)
    }

    override suspend fun searchAlbums(query: String): List<Album> {
        return spotifyClient.searchAlbums(query)
    }

    override suspend fun searchArtists(query: String): List<Artist> {
        return spotifyClient.searchArtists(query)
    }

    override suspend fun getRecommendations(seedTrackId: String): List<Track> {
        val id = seedTrackId.removePrefix("spotify:")
        return spotifyClient.recommendations(id)
    }

    override suspend fun getPlaylistTracks(playlistId: String): List<Track> {
        return spotifyClient.getPlaylistTracks(playlistId)
    }

    override suspend fun getAlbumTracks(albumId: String): List<Track> {
        // TODO: Implement album tracks fetching from Spotify
        return emptyList()
    }

    override suspend fun saveTrack(track: Track) {
        val current = _savedTracks.value
        if (!current.any { it.id == track.id }) {
            _savedTracks.value = current + track
        }
    }

    override suspend fun getSavedTracks(): List<Track> {
        return _savedTracks.value
    }

    override suspend fun removeTrack(trackId: String) {
        _savedTracks.value = _savedTracks.value.filter { it.id != trackId }
    }

    override suspend fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(
            id = "local:${System.currentTimeMillis()}",
            name = name,
            tracks = emptyList()
        )
        _playlists.value = _playlists.value + playlist
        return playlist
    }

    override suspend fun getPlaylists(): List<Playlist> {
        return _playlists.value
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(tracks = playlist.tracks + track)
            } else {
                playlist
            }
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(tracks = playlist.tracks.filter { it.id != trackId })
            } else {
                playlist
            }
        }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
    }

    override suspend fun getRecentTracks(): List<Track> {
        return _recentTracks.value.take(20)
    }

    override suspend fun addToRecent(track: Track) {
        val current = _recentTracks.value.filter { it.id != track.id }
        _recentTracks.value = listOf(track) + current
    }
}