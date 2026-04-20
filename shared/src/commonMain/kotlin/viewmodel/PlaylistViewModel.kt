package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import music.model.Playlist
import music.model.Track

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PlaylistViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val playlists = repository.getPlaylists()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playlists = playlists
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load playlists"
                )
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            try {
                val playlist = repository.createPlaylist(name)
                loadPlaylists() // Refresh the list
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to create playlist")
            }
        }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _uiState.value = _uiState.value.copy(selectedPlaylist = playlist)
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            try {
                repository.addTrackToPlaylist(playlistId, track)
                loadPlaylists() // Refresh to show updated playlist
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to add track")
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            try {
                repository.removeTrackFromPlaylist(playlistId, trackId)
                loadPlaylists() // Refresh to show updated playlist
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to remove track")
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlistId)
                loadPlaylists() // Refresh the list
                if (_uiState.value.selectedPlaylist?.id == playlistId) {
                    _uiState.value = _uiState.value.copy(selectedPlaylist = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to delete playlist")
            }
        }
    }

    fun refresh() {
        loadPlaylists()
    }
}