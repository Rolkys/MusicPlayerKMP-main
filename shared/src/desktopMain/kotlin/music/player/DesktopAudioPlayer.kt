package music.player

import java.util.concurrent.atomic.AtomicBoolean
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import music.model.PlayerState
import music.model.Track
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DesktopAudioPlayer : AudioPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var currentTrack: Track? = null
    private var progressTimeline: Timeline? = null

    override suspend fun play(track: Track) {
        runOnFxThread {
            releasePlayer()

            currentTrack = track
            _state.value = PlayerState(currentTrack = track, isBuffering = true)

            try {
                val media = Media(toPlayableUri(track.uri))
                val mediaPlayer = MediaPlayer(media)

                mediaPlayer.setOnReady {
                    val duration = mediaPlayer.totalDuration?.toMillis()?.toLong() ?: 0
                    mediaPlayer.play()
                    _state.value = PlayerState(
                        currentTrack = track, 
                        isPlaying = true,
                        durationMs = duration
                    )
                    startProgressTimer(mediaPlayer, track)
                }

                mediaPlayer.setOnPlaying {
                    val duration = mediaPlayer.totalDuration?.toMillis()?.toLong() ?: _state.value.durationMs
                    _state.value = _state.value.copy(isPlaying = true, durationMs = duration)
                }

                mediaPlayer.setOnPaused {
                    _state.value = _state.value.copy(isPlaying = false)
                    stopProgressTimer()
                }

                mediaPlayer.setOnEndOfMedia {
                    _state.value = _state.value.copy(isPlaying = false, currentPositionMs = _state.value.durationMs)
                    stopProgressTimer()
                }

                mediaPlayer.setOnError {
                    _state.value = PlayerState(
                        currentTrack = track,
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = mediaPlayer.error?.message ?: "No se pudo reproducir en Desktop"
                    )
                }

                player = mediaPlayer
            } catch (error: Throwable) {
                _state.value = PlayerState(
                    currentTrack = track,
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.message ?: "No se pudo abrir el audio"
                )
            }
        }
    }

    override suspend fun pause() {
        runOnFxThread {
            val mediaPlayer = player ?: return@runOnFxThread
            mediaPlayer.pause()
            _state.value = PlayerState(currentTrack = currentTrack, isPlaying = false)
        }
    }

    override suspend fun resume() {
        runOnFxThread {
            val mediaPlayer = player ?: return@runOnFxThread
            mediaPlayer.play()
            _state.value = PlayerState(currentTrack = currentTrack, isPlaying = true)
        }
    }

    override suspend fun stop() {
        runOnFxThread {
            releasePlayer()
            _state.value = PlayerState(currentTrack = null, isPlaying = false)
        }
    }

    override fun seekTo(position: Float) {
        Platform.runLater {
            val mediaPlayer = player ?: return@runLater
            val duration = mediaPlayer.totalDuration
            if (duration != null) {
                val seekTime = duration.multiply(position.toDouble())
                mediaPlayer.seek(seekTime)
            }
        }
    }

    override fun setVolume(volume: Float) {
        Platform.runLater {
            val mediaPlayer = player ?: return@runLater
            mediaPlayer.volume = volume.toDouble()
            _state.value = _state.value.copy(volume = volume)
        }
    }

    override fun close() {
        if (!JavaFxRuntime.isStarted()) return

        Platform.runLater {
            releasePlayer()
            _state.value = PlayerState()
        }
    }

    private fun startProgressTimer(mediaPlayer: MediaPlayer, track: Track) {
        stopProgressTimer()
        
        val timeline = Timeline(
            KeyFrame(Duration.millis(500.0), {
                val currentMillis = mediaPlayer.currentTime?.toMillis()?.toLong() ?: 0
                val totalMillis = mediaPlayer.totalDuration?.toMillis()?.toLong() ?: track.durationMs ?: 0
                val progress = if (totalMillis > 0) currentMillis.toFloat() / totalMillis.toFloat() else 0f
                
                _state.value = _state.value.copy(
                    currentPositionMs = currentMillis,
                    durationMs = totalMillis,
                    progress = progress
                )
            })
        )
        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()
        progressTimeline = timeline
    }
    
    private fun stopProgressTimer() {
        progressTimeline?.stop()
        progressTimeline = null
    }

    private fun releasePlayer() {
        stopProgressTimer()
        player?.stop()
        player?.dispose()
        player = null
        currentTrack = null
    }

    private fun toPlayableUri(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file:", ignoreCase = true)) {
            return trimmed
        }

        return java.io.File(trimmed).toURI().toString()
    }

    private suspend fun <T> runOnFxThread(block: () -> T): T {
        JavaFxRuntime.ensureStarted()

        return suspendCancellableCoroutine { continuation ->
            Platform.runLater {
                try {
                    continuation.resume(block())
                } catch (error: Throwable) {
                    continuation.resumeWithException(error)
                }
            }
        }
    }

    private object JavaFxRuntime {
        private val started = AtomicBoolean(false)

        fun ensureStarted() {
            if (started.compareAndSet(false, true)) {
                JFXPanel()
            }
        }

        fun isStarted(): Boolean = started.get()
    }
}
