package music.platform

import java.awt.Frame
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

/**
 * Implementación Desktop para seleccionar carpeta de música usando JFileChooser
 */
actual fun pickMusicFolder(): String? {
    val chooser = JFileChooser(FileSystemView.getFileSystemView()).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Seleccionar carpeta de música"
        isAcceptAllFileFilterUsed = false
        
        // Intentar empezar en la carpeta Music del usuario
        val home = System.getProperty("user.home") ?: ""
        val musicDir = File(home, "Music")
        if (musicDir.exists()) {
            currentDirectory = musicDir
        }
    }
    
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else null
}

/**
 * Implementación Desktop de SettingsStorage usando Java Preferences API
 */
actual fun createSettingsStorage(): SettingsStorage = DesktopSettingsStorage()

class DesktopSettingsStorage : SettingsStorage {
    private val prefs = Preferences.userNodeForPackage(DesktopSettingsStorage::class.java)
    private val KEY_FOLDERS = "music_folders"
    private val KEY_FAVORITES = "favorite_tracks"
    
    override fun saveMusicFolders(folders: List<String>) {
        prefs.put(KEY_FOLDERS, folders.joinToString("|"))
        prefs.flush()
    }
    
    override fun loadMusicFolders(): List<String> {
        val saved = prefs.get(KEY_FOLDERS, "") ?: return emptyList()
        if (saved.isBlank()) return emptyList()
        return saved.split("|").filter { it.isNotBlank() && File(it).exists() }
    }
    
    override fun saveFavorites(favorites: List<String>) {
        prefs.put(KEY_FAVORITES, favorites.joinToString("|"))
        prefs.flush()
    }
    
    override fun loadFavorites(): List<String> {
        val saved = prefs.get(KEY_FAVORITES, "") ?: return emptyList()
        if (saved.isBlank()) return emptyList()
        return saved.split("|").filter { it.isNotBlank() }
    }
    
    override fun clear() {
        prefs.remove(KEY_FOLDERS)
        prefs.remove(KEY_FAVORITES)
        prefs.flush()
    }
}
