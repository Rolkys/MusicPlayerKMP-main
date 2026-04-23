package music.platform

/**
 * Función expect para seleccionar una carpeta de música.
 * Devuelve la ruta de la carpeta seleccionada o null si se canceló.
 */
expect fun pickMusicFolder(): String?

/**
 * Interfaz para persistencia simple de configuración
 */
interface SettingsStorage {
    fun saveMusicFolders(folders: List<String>)
    fun loadMusicFolders(): List<String>
    fun saveFavorites(favorites: List<String>)
    fun loadFavorites(): List<String>
    fun clear()
}

/**
 * Expect para obtener el storage de settings en cada plataforma
 */
expect fun createSettingsStorage(): SettingsStorage
