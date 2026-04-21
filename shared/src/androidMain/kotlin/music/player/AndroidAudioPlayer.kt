package music.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import music.model.PlayerState
import music.model.Track

class AndroidAudioPlayer(
    private val context: Context
) : AudioPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: Track? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    override suspend fun play(track: Track) {
        releasePlayer()
        currentTrack = track

        _state.value = PlayerState(currentTrack = track, isBuffering = true)

        val player = MediaPlayer()
        mediaPlayer = player

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        player.setOnPreparedListener { mp ->
            val duration = mp.duration.toLong()
            mp.start()
            _state.value = PlayerState(
                currentTrack = track, 
                isPlaying = true,
                durationMs = duration
            )
            startProgressTimer(mp)
        }

        player.setOnCompletionListener {
            _state.value = _state.value.copy(isPlaying = false, currentPositionMs = _state.value.durationMs)
            stopProgressTimer()
        }

        player.setOnErrorListener { _, _, _ ->
            _state.value = PlayerState(
                currentTrack = track,
                isPlaying = false,
                isBuffering = false,
                errorMessage = "No se pudo reproducir en Android"
            )
            true
        }

        runCatching {
            player.setDataSource(context, Uri.parse(track.uri))
            player.prepareAsync()
        }.onFailure { error ->
            _state.value = PlayerState(
                currentTrack = track,
                isPlaying = false,
                isBuffering = false,
                errorMessage = error.message ?: "No se pudo abrir el audio"
            )
            releasePlayer()
        }
    }

    override suspend fun pause() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
        stopProgressTimer()
    }

    override suspend fun resume() {
        val player = mediaPlayer ?: return
        runCatching { player.start() }
        _state.value = _state.value.copy(isPlaying = true, isBuffering = false)
        startProgressTimer(player)
    }

    override suspend fun stop() {
        releasePlayer()
        _state.value = PlayerState()
    }

    override fun seekTo(position: Float) {
        mediaPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val seekPosition = (duration * position).toInt()
                player.seekTo(seekPosition)
            }
        }
    }

    override fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
        _state.value = _state.value.copy(volume = volume)
    }

    override fun close() {
        releasePlayer()
        _state.value = PlayerState()
    }

    private fun startProgressTimer(mediaPlayer: MediaPlayer) {
        stopProgressTimer()
        
        val runnable = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying) {
                    val current = mediaPlayer.currentPosition.toLong()
                    val duration = mediaPlayer.duration.toLong()
                    val progress = if (duration > 0) current.toFloat() / duration.toFloat() else 0f
                    
                    _state.value = _state.value.copy(
                        currentPositionMs = current,
                        progress = progress
                    )
                    progressHandler.postDelayed(this, 500)
                }
            }
        }
        progressRunnable = runnable
        progressHandler.post(runnable)
    }
    
    private fun stopProgressTimer() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun releasePlayer() {
        stopProgressTimer()
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            player.release()
        }
        mediaPlayer = null
        currentTrack = null
    }
}
