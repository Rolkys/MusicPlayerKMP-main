package music.model

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val volume: Float = 1f,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()) else 0f
    
    val formattedCurrentTime: String
        get() = formatDuration(currentPositionMs)
    
    val formattedTotalTime: String
        get() = formatDuration(durationMs)
    
    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "0:00"
            val seconds = ms / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
        }
    }
}
