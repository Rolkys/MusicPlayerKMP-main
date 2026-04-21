package music.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import music.model.Track
import music.model.TrackOrigin

class AndroidLocalMusicSource(
    private val context: Context
) : LocalMusicSource {
    
    private val prefs = context.getSharedPreferences("music_source_settings", Context.MODE_PRIVATE)
    private val KEY_CUSTOM_FOLDERS = "custom_music_folders"
    private var customFoldersCache: List<String>? = null

    override suspend fun scan(pathHint: String?): List<Track> = withContext(Dispatchers.IO) {
        // Si hay carpetas personalizadas, escanear solo esas
        val customFolders = getCustomFolders()
        if (customFolders.isNotEmpty()) {
            return@withContext scanCustomFolders(customFolders)
        }
        
        // Si no hay carpetas personalizadas, escanear toda la música del dispositivo
        scanAllMusic()
    }
    
    /**
     * Escanear todas las carpetas personalizadas
     */
    override suspend fun scanCustomFolders(folders: List<String>): List<Track> = withContext(Dispatchers.IO) {
        if (folders.isEmpty()) {
            return@withContext emptyList<Track>()
        }
        
        val allTracks = mutableListOf<Track>()
        
        // Para cada carpeta URI, intentamos obtener los archivos de audio
        for (folderUri in folders) {
            val tracksFromFolder = scanFolder(Uri.parse(folderUri))
            allTracks.addAll(tracksFromFolder)
        }
        
        allTracks.distinctBy { it.id }.sortedBy { it.title.lowercase() }
    }
    
    /**
     * Escanear una carpeta específica usando su URI de documento
     */
    private fun scanFolder(folderUri: Uri): List<Track> {
        val tracks = mutableListOf<Track>()
        
        try {
            // Para URIs de documentos SAF, listar documentos hijos
            val childrenUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            
            // Query para obtener archivos de audio que estén dentro de esta carpeta
            // Nota: Android 10+ tiene restricciones de acceso a carpetas específicas
            // Usamos MediaStore para obtener todas las canciones y filtrar por ruta
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            
            context.contentResolver.query(
                childrenUri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Sin titulo"
                    val artist = cursor.getString(artistColumn) ?: "Artista desconocido"
                    val duration = cursor.getLong(durationColumn)
                    val dataPath = cursor.getString(dataColumn) ?: ""
                    
                    // Si tenemos un pathHint, verificar si el archivo está en esa carpeta
                    // Para simplificar, aceptamos todas las canciones por ahora
                    // En una implementación real, compararíamos rutas
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    tracks += Track(
                        id = "android:$id",
                        title = title,
                        artist = artist,
                        uri = contentUri.toString(),
                        durationMs = duration,
                        origin = TrackOrigin.LOCAL
                    )
                }
            }
        } catch (e: Exception) {
            // Ignorar errores de acceso
            e.printStackTrace()
        }
        
        return tracks
    }
    
    /**
     * Escanear toda la música del dispositivo usando MediaStore
     */
    private fun scanAllMusic(): List<Track> {
        val tracks = mutableListOf<Track>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Sin titulo"
                val artist = cursor.getString(artistColumn) ?: "Artista desconocido"
                val duration = cursor.getLong(durationColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                tracks += Track(
                    id = "android:$id",
                    title = title,
                    artist = artist,
                    uri = contentUri.toString(),
                    durationMs = duration,
                    origin = TrackOrigin.LOCAL
                )
            }
        }
        
        return tracks
    }
    
    override fun addCustomFolder(folderPath: String) {
        val current = getCustomFolders().toMutableList()
        if (!current.contains(folderPath)) {
            current.add(folderPath)
            saveCustomFolders(current)
        }
    }
    
    override fun getCustomFolders(): List<String> {
        customFoldersCache?.let { return it }
        
        val saved = prefs.getStringSet(KEY_CUSTOM_FOLDERS, emptySet()) ?: emptySet()
        val folders = saved.toList()
        customFoldersCache = folders
        return folders
    }
    
    override fun removeCustomFolder(folderPath: String) {
        val current = getCustomFolders().toMutableList()
        current.remove(folderPath)
        saveCustomFolders(current)
    }
    
    override fun clearCustomFolders() {
        prefs.edit().remove(KEY_CUSTOM_FOLDERS).apply()
        customFoldersCache = emptyList()
    }
    
    private fun saveCustomFolders(folders: List<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_FOLDERS, folders.toSet()).apply()
        customFoldersCache = folders
    }
}
