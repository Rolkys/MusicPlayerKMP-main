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

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentTracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val recentTracks = repository.getRecentTracks()
                val playlists = repository.getPlaylists()
                // For now, use user's playlists as featured
                val featuredPlaylists = playlists.take(6)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recentTracks = recentTracks,
                    playlists = playlists,
                    featuredPlaylists = featuredPlaylists
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading home data"
                )
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}