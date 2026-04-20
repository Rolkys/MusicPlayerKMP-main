package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import music.model.Album
import music.model.Artist
import music.model.Playlist
import music.model.Track

data class SearchUiState(
    val isSearching: Boolean = false,
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val error: String? = null
)

class SearchViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            clearResults()
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)

            try {
                val tracks = repository.searchTracks(query)
                val playlists = repository.searchPlaylists(query)
                val albums = repository.searchAlbums(query)
                val artists = repository.searchArtists(query)

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    tracks = tracks,
                    playlists = playlists,
                    albums = albums,
                    artists = artists
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun clearResults() {
        _uiState.value = SearchUiState(query = _uiState.value.query)
    }
}