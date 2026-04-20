package music.player

import kotlinx.coroutines.flow.StateFlow
import music.model.PlayerState
import music.model.Track

interface AudioPlayer {
    val state: StateFlow<PlayerState>

    suspend fun play(track: Track)

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()

    fun seekTo(position: Float)

    fun setVolume(volume: Float)

    fun close()
}
