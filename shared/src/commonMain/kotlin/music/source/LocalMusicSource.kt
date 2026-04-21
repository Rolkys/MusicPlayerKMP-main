package music.source

import music.model.Track

interface LocalMusicSource {
    suspend fun scan(pathHint: String? = null): List<Track>
    
    /**
     * Escanear múltiples carpetas personalizadas
     */
    suspend fun scanCustomFolders(folders: List<String>): List<Track>
    
    /**
     * Agregar una carpeta a las carpetas escaneadas
     */
    fun addCustomFolder(folderPath: String)
    
    /**
     * Obtener lista de carpetas personalizadas
     */
    fun getCustomFolders(): List<String>
    
    /**
     * Eliminar una carpeta de la lista
     */
    fun removeCustomFolder(folderPath: String)
    
    /**
     * Limpiar todas las carpetas personalizadas
     */
    fun clearCustomFolders()
}
