package music.source

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import music.model.Track
import music.model.TrackOrigin

class DesktopLocalMusicSource : LocalMusicSource {
    override suspend fun scan(pathHint: String?): List<Track> = withContext(Dispatchers.IO) {
        val roots = resolveRoots(pathHint)

        roots.asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { root -> root.walkTopDown().asSequence() }
            .filter { file -> file.isFile && file.extension.lowercase() in SUPPORTED_EXTENSIONS }
            .map { file ->
                Track(
                    id = file.absolutePath,
                    title = file.nameWithoutExtension,
                    artist = "Local file",
                    uri = file.toURI().toString(),
                    durationMs = null,
                    origin = TrackOrigin.LOCAL
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }
            .toList()
    }

    private fun resolveRoots(pathHint: String?): List<File> {
        val fromHint = pathHint
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)

        if (fromHint != null) {
            return listOf(fromHint)
        }

        val home = System.getProperty("user.home") ?: ""

        return listOf(
            File(home, "Music"),
            File(home, "Downloads")
        )
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg", "opus", "wma")
    }
}
