package music.platform

import android.content.Context
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity

/**
 * Implementación Android para seleccionar carpeta.
 * Nota: Esta función debe llamarse desde un Composable con rememberLauncherForActivityResult
 * por eso es diferente a Desktop.
 */
actual fun pickMusicFolder(): String? {
    // En Android, el picker se maneja diferente mediante ActivityResultLauncher
    // Esta función es un placeholder - el picker real se implementa en MainActivity
    return null
}

/**
 * Launcher para seleccionar carpetas en Android (usar desde MainActivity)
 */
class AndroidFolderPicker private constructor() {
    companion object {
        @Volatile
        private var instance: AndroidFolderPicker? = null
        private var pendingCallback: ((String?) -> Unit)? = null
        
        fun getInstance(): AndroidFolderPicker {
            return instance ?: synchronized(this) {
                instance ?: AndroidFolderPicker().also { instance = it }
            }
        }
        
        fun registerWithActivity(activity: ComponentActivity): AndroidFolderPicker {
            val picker = getInstance()
            
            // Registrar el launcher para selección de carpetas
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                val path = uri?.toString()
                pendingCallback?.invoke(path)
                pendingCallback = null
            }
            
            picker.launcher = { launcher.launch(null) }
            return picker
        }
    }
    
    private var launcher: (() -> Unit)? = null
    
    fun pickFolder(callback: (String?) -> Unit) {
        pendingCallback = callback
        launcher?.invoke()
    }
    
    fun pickFolderSync(): String? {
        // No disponible sincrónicamente en Android
        return null
    }
}

/**
 * Implementación Android de SettingsStorage usando SharedPreferences
 */
actual fun createSettingsStorage(): SettingsStorage {
    val ctx = AndroidSettingsStorage.context 
        ?: throw IllegalStateException("AndroidSettingsStorage not initialized. Call AndroidSettingsStorage.init(context) first.")
    return AndroidSettingsStorage(ctx)
}

class AndroidSettingsStorage private constructor(context: Context) : SettingsStorage {
    companion object {
        internal var context: Context? = null
        
        fun init(context: Context) {
            this.context = context.applicationContext
        }
        
        private fun getInstance(): AndroidSettingsStorage {
            val ctx = context ?: throw IllegalStateException("AndroidSettingsStorage not initialized")
            return AndroidSettingsStorage(ctx)
        }
    }
    
    private val prefs = context.getSharedPreferences("music_player_settings", Context.MODE_PRIVATE)
    private val KEY_FOLDERS = "music_folders"
    private val KEY_FAVORITES = "favorite_tracks"
    
    override fun saveMusicFolders(folders: List<String>) {
        prefs.edit().putStringSet(KEY_FOLDERS, folders.toSet()).apply()
    }
    
    override fun loadMusicFolders(): List<String> {
        val saved = prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: return emptyList()
        return saved.toList()
    }
    
    override fun saveFavorites(favorites: List<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites.toSet()).apply()
    }
    
    override fun loadFavorites(): List<String> {
        val saved = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: return emptyList()
        return saved.toList()
    }
    
    override fun clear() {
        prefs.edit().remove(KEY_FOLDERS).remove(KEY_FAVORITES).apply()
    }
}

/**
 * Helper para obtener path real de URI de documento (Android 10+)
 * Simplificado - guardamos el URI y lo resolvemos al escanear
 */
fun getPathFromUri(context: Context, uri: Uri): String? {
    // Para SAF URIs, devolvemos el URI como string
    // El escaneo se hará usando ContentResolver
    return uri.toString()
}
