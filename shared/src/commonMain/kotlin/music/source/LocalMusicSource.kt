package music.source

import music.model.Track

interface LocalMusicSource {
    suspend fun scan(pathHint: String? = null): List<Track>
}
