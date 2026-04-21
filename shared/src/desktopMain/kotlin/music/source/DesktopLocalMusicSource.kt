package music.source

import java.io.File
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import music.model.Track
import music.model.TrackOrigin

class DesktopLocalMusicSource : LocalMusicSource {
    
    private val prefs = Preferences.userNodeForPackage(DesktopLocalMusicSource::class.java)
    private val KEY_CUSTOM_FOLDERS = "custom_music_folders"
    
    // Cache en memoria de carpetas personalizadas
    private var customFoldersCache: List<String>? = null
    
    override suspend fun scan(pathHint: String?): List<Track> = withContext(Dispatchers.IO) {
        val roots = resolveRoots(pathHint)
        scanFilesFromRoots(roots)
    }
    
    override suspend fun scanCustomFolders(folders: List<String>): List<Track> = withContext(Dispatchers.IO) {
        val validFolders = folders.map { File(it) }.filter { it.exists() && it.isDirectory }
        scanFilesFromRoots(validFolders)
    }
    
    private fun scanFilesFromRoots(roots: List<File>): List<Track> {
        return roots.asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { root -> root.walkTopDown().asSequence() }
            .filter { file -> file.isFile && file.extension.lowercase() in SUPPORTED_EXTENSIONS }
            .map { file ->
                // Intentar extraer metadata del nombre de archivo
                val (title, artist) = extractMetadata(file.nameWithoutExtension)
                Track(
                    id = file.absolutePath,
                    title = title,
                    artist = artist,
                    uri = file.toURI().toString(),
                    durationMs = null,
                    origin = TrackOrigin.LOCAL
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }
            .toList()
    }
    
    /**
     * Extrae título y artista del nombre de archivo.
     * Formatos soportados: "Artista - Titulo", "Artista_Titulo", "01. Artista - Titulo", etc.
     */
    private fun extractMetadata(filename: String): Pair<String, String> {
        // Remover números de pista al inicio (ej: "01. ", "01 - ", "01 ")
        var cleanName = filename.replace(Regex("^\\d+\\.?\\s*[-.]?\\s*"), "")
        
        // Buscar separador de artista - título
        val separators = listOf(" - ", " – ", " _ ", "-", "_", " — ")
        for (sep in separators) {
            val parts = cleanName.split(sep, limit = 2)
            if (parts.size == 2) {
                val artist = parts[0].trim()
                val title = parts[1].trim()
                if (artist.isNotBlank() && title.isNotBlank()) {
                    return Pair(title, artist)
                }
            }
        }
        
        // Si no hay separador, usar el nombre completo como título
        return Pair(cleanName.trim(), "Artista desconocido")
    }
    
    override fun addCustomFolder(folderPath: String) {
        val current = getCustomFolders().toMutableList()
        if (!current.contains(folderPath) && File(folderPath).exists()) {
            current.add(folderPath)
            saveCustomFolders(current)
        }
    }
    
    override fun getCustomFolders(): List<String> {
        // Usar cache si está disponible
        customFoldersCache?.let { return it }
        
        val saved = prefs.get(KEY_CUSTOM_FOLDERS, "") ?: ""
        val folders = if (saved.isBlank()) {
            emptyList()
        } else {
            saved.split("|").filter { it.isNotBlank() && File(it).exists() }
        }
        customFoldersCache = folders
        return folders
    }
    
    override fun removeCustomFolder(folderPath: String) {
        val current = getCustomFolders().toMutableList()
        current.remove(folderPath)
        saveCustomFolders(current)
    }
    
    override fun clearCustomFolders() {
        prefs.remove(KEY_CUSTOM_FOLDERS)
        prefs.flush()
        customFoldersCache = emptyList()
    }
    
    private fun saveCustomFolders(folders: List<String>) {
        prefs.put(KEY_CUSTOM_FOLDERS, folders.joinToString("|"))
        prefs.flush()
        customFoldersCache = folders
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
        
        // Combinar carpetas por defecto + carpetas personalizadas
        val defaultFolders = listOf(
            File(home, "Music"),
            File(home, "Downloads")
        )
        
        val customFolders = getCustomFolders().map { File(it) }.filter { it.exists() }
        
        return (defaultFolders + customFolders).distinctBy { it.absolutePath }
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg", "opus", "wma")
    }
}
