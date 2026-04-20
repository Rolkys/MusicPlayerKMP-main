package data.repository

import music.model.Album
import music.model.Artist
import music.model.Playlist
import music.model.Track

interface MusicRepository {
    suspend fun searchTracks(query: String): List<Track>
    suspend fun searchPlaylists(query: String): List<Playlist>
    suspend fun searchAlbums(query: String): List<Album>
    suspend fun searchArtists(query: String): List<Artist>
    suspend fun getRecommendations(seedTrackId: String): List<Track>
    suspend fun getPlaylistTracks(playlistId: String): List<Track>
    suspend fun getAlbumTracks(albumId: String): List<Track>

    // Local storage operations
    suspend fun saveTrack(track: Track)
    suspend fun getSavedTracks(): List<Track>
    suspend fun removeTrack(trackId: String)

    suspend fun createPlaylist(name: String): Playlist
    suspend fun getPlaylists(): List<Playlist>
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun deletePlaylist(playlistId: String)

    suspend fun getRecentTracks(): List<Track>
    suspend fun addToRecent(track: Track)
}