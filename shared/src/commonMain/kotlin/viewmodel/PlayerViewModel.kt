package viewmodel

import data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import music.model.PlayerState
import music.model.Track
import music.player.AudioPlayer

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val volume: Float = 1f,
    val isLoading: Boolean = false,
    val error: String? = null
)

open class CommonViewModel {
    protected val viewModelScope = CoroutineScope(Dispatchers.Main)

    fun onCleared() {
        viewModelScope.cancel()
    }
}

class PlayerViewModel(
    private val audioPlayer: AudioPlayer,
    private val repository: MusicRepository
) : CommonViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Observe player state changes
        viewModelScope.launch {
            audioPlayer.state.collect { playerState ->
                _uiState.value = _uiState.value.copy(
                    currentTrack = playerState.currentTrack,
                    isPlaying = playerState.isPlaying,
                    progress = playerState.progress ?: 0f,
                    volume = playerState.volume ?: 1f
                )
            }
        }
    }

    fun play(track: Track) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                audioPlayer.play(track)
                repository.addToRecent(track)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to play track"
                )
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            audioPlayer.pause()
        }
    }

    fun resume() {
        viewModelScope.launch {
            audioPlayer.resume()
        }
    }

    fun stop() {
        viewModelScope.launch {
            audioPlayer.stop()
        }
    }

    fun seekTo(position: Float) {
        audioPlayer.seekTo(position)
    }

    fun setVolume(volume: Float) {
        audioPlayer.setVolume(volume)
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun playNext(tracks: List<Track>) {
        val currentIndex = tracks.indexOfFirst { it.id == _uiState.value.currentTrack?.id }
        if (currentIndex >= 0 && currentIndex < tracks.size - 1) {
            play(tracks[currentIndex + 1])
        }
    }

    fun playPrevious(tracks: List<Track>) {
        val currentIndex = tracks.indexOfFirst { it.id == _uiState.value.currentTrack?.id }
        if (currentIndex > 0) {
            play(tracks[currentIndex - 1])
        }
    }
}