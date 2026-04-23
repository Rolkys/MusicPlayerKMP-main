package music.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Servicio para obtener letras sincronizadas usando LRCLIB
 * https://lrclib.net/
 */
class LyricsService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    private val baseUrl = "https://lrclib.net/api"
    
    /**
     * Buscar letras por artista y título
     */
    suspend fun getLyrics(artist: String, title: String): LyricsResult? {
        return try {
            val response = client.get("$baseUrl/get") {
                url {
                    parameters.append("artist_name", artist)
                    parameters.append("track_name", title)
                }
            }
            
            if (response.status.value == 200) {
                response.body<LyricsResponse>().toLyricsResult()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching lyrics: ${e.message}")
            null
        }
    }
    
    /**
     * Buscar letras por ID de LRCLIB
     */
    suspend fun getLyricsById(id: Long): LyricsResult? {
        return try {
            val response = client.get("$baseUrl/get/$id")
            if (response.status.value == 200) {
                response.body<LyricsResponse>().toLyricsResult()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching lyrics by id: ${e.message}")
            null
        }
    }
    
    fun close() {
        client.close()
    }
}

@Serializable
data class LyricsResponse(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

fun LyricsResponse.toLyricsResult(): LyricsResult {
    return LyricsResult(
        id = id,
        title = trackName,
        artist = artistName,
        album = albumName,
        duration = duration,
        isInstrumental = instrumental,
        plainLyrics = plainLyrics,
        syncedLyrics = parseSyncedLyrics(syncedLyrics)
    )
}

fun parseSyncedLyrics(syncedText: String?): List<SyncedLyricLine>? {
    if (syncedText == null) return null
    
    val lines = mutableListOf<SyncedLyricLine>()
    val regex = Regex("\\[(\\d{2}:\\d{2}\\.\\d{2,3})\\](.*)")
    
    syncedText.lines().forEach { line ->
        val match = regex.matchEntire(line.trim())
        if (match != null) {
            val timeStr = match.groupValues[1]
            val text = match.groupValues[2].trim()
            val timeMs = parseTimeToMs(timeStr)
            if (text.isNotBlank()) {
                lines.add(SyncedLyricLine(timeMs, text))
            }
        }
    }
    
    return lines.ifEmpty { null }
}

fun parseTimeToMs(timeStr: String): Long {
    // Formato: MM:SS.mmm o MM:SS.ms
    val parts = timeStr.split(":", ".")
    return when (parts.size) {
        3 -> {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val millis = parts[2].padEnd(3, '0').toLong()
            (minutes * 60 + seconds) * 1000 + millis
        }
        2 -> {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            (minutes * 60 + seconds) * 1000
        }
        else -> 0L
    }
}

data class LyricsResult(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Double?,
    val isInstrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: List<SyncedLyricLine>?
)

data class SyncedLyricLine(
    val timeMs: Long,
    val text: String
)
