package music.controller

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import music.lyrics.LyricsResult
import music.lyrics.LyricsService
import music.model.Album
import music.model.Playlist
import music.model.PlayerState
import music.model.RepeatMode
import music.model.Track
import music.model.TrackOrigin
import music.platform.createSettingsStorage
import music.player.AudioPlayer
import music.source.LocalMusicSource
import music.spotify.DeezerClient
import music.spotify.createSpotifyHttpClient

data class LibraryUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val localTracks: List<Track> = emptyList(),
    val remoteTracks: List<Track> = emptyList(),
    val savedTracks: List<Track> = emptyList(),
    val spotifyResults: List<Track> = emptyList(),
    val spotifyPlaylists: List<Playlist> = emptyList(),
    val spotifyAlbums: List<Album> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val selectedAlbum: String? = null,
    val selectedPlaylist: String? = null,
    val playCounts: Map<String, Int> = emptyMap(),
    val message: String? = null,
    val musicFolders: List<String> = emptyList(), // Carpetas de música personalizadas
    val favoriteTrackIds: Set<String> = emptySet() // IDs de canciones favoritas
) {
    val allTracks: List<Track>
        get() = remoteTracks + localTracks + savedTracks
    
    val favoriteTracks: List<Track>
        get() = allTracks.filter { it.id in favoriteTrackIds }

    val selectedAlbumTracks: List<Track>
        get() = albums.firstOrNull { it.name == selectedAlbum }?.tracks.orEmpty()

    val selectedPlaylistTracks: List<Track>
        get() = playlists.firstOrNull { it.name == selectedPlaylist }?.tracks.orEmpty()
    
    fun isFavorite(track: Track): Boolean = track.id in favoriteTrackIds
}

class MusicController(
    private val localMusicSource: LocalMusicSource,
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope
) {
/*     init {
        // Configuración automática con credenciales de ejemplo
        // ¡IMPORTANTE! Reemplaza estas credenciales con las tuyas reales de Spotify
        // Las credenciales de ejemplo NO funcionarán
        // Si no tienes API de Spotify, comenta estas líneas y usa solo música local
        configureSpotifyApi(
            clientId = "TU_CLIENT_ID_REAL_AQUI",
            clientSecret = "TU_CLIENT_SECRET_REAL_AQUI"
        )
    }
*/
    private val _libraryState = MutableStateFlow(LibraryUiState())
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()

    val playerState: StateFlow<PlayerState> = audioPlayer.state

    private var deezerClient: DeezerClient? = null
    private val lyricsService = LyricsService()
    
    // Estado para letras de la canción actual
    private val _currentLyrics = MutableStateFlow<LyricsResult?>(null)
    val currentLyrics: StateFlow<LyricsResult?> = _currentLyrics.asStateFlow()
    
    // Cola de reproducción para navegación previous/next
    private var playbackQueue: List<Track> = emptyList()
    private var currentQueueIndex: Int = -1
    private var isShuffleOn: Boolean = false
    private var repeatMode: RepeatMode = RepeatMode.OFF
    
    // Estado observable de la cola
    private val _playbackQueueState = MutableStateFlow(PlaybackQueueState())
    val playbackQueueState: StateFlow<PlaybackQueueState> = _playbackQueueState.asStateFlow()
    
    data class PlaybackQueueState(
        val queue: List<Track> = emptyList(),
        val currentIndex: Int = -1,
        val isShuffleOn: Boolean = false,
        val repeatMode: RepeatMode = RepeatMode.OFF
    )
    
    /**
     * Actualizar el estado de la cola
     */
    private fun updateQueueState() {
        _playbackQueueState.value = PlaybackQueueState(
            queue = playbackQueue,
            currentIndex = currentQueueIndex,
            isShuffleOn = isShuffleOn,
            repeatMode = repeatMode
        )
    }
    
    /**
     * Eliminar una canción de la cola
     */
    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= playbackQueue.size) return
        
        val newQueue = playbackQueue.toMutableList()
        newQueue.removeAt(index)
        
        // Ajustar el índice actual si es necesario
        if (index < currentQueueIndex) {
            currentQueueIndex--
        } else if (index == currentQueueIndex) {
            // Si eliminamos la canción actual, pasamos a la siguiente
            if (currentQueueIndex >= newQueue.size) {
                currentQueueIndex = 0
            }
        }
        
        playbackQueue = newQueue
        updateQueueState()
    }
    
    /**
     * Limpiar toda la cola
     */
    fun clearQueue() {
        playbackQueue = emptyList()
        currentQueueIndex = -1
        updateQueueState()
    }
    
    /**
     * Saltar a una canción específica de la cola
     */
    fun playFromQueue(index: Int) {
        if (index < 0 || index >= playbackQueue.size) return
        
        currentQueueIndex = index
        val track = playbackQueue[index]
        play(track)
        updateQueueState()
    }
    
    /**
     * Alternar modo aleatorio
     */
    fun toggleShuffle() {
        isShuffleOn = !isShuffleOn
        updateQueueState()
    }
    
    /**
     * Alternar modo repetición
     */
    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updateQueueState()
    }

    init {
        // Inicializar cliente Deezer (no requiere credenciales)
        deezerClient = DeezerClient(createSpotifyHttpClient())
        
        // Observar cambios de canción para cargar letras
        scope.launch {
            playerState.collect { state ->
                if (state.currentTrack != null) {
                    loadLyricsForTrack(state.currentTrack)
                } else {
                    _currentLyrics.value = null
                }
            }
        }
        
        // Cargar favoritos guardados
        loadFavorites()
    }
    
    /**
     * Cargar letras para una canción específica
     */
    private suspend fun loadLyricsForTrack(track: Track) {
        runCatching {
            val lyrics = lyricsService.getLyrics(track.artist, track.title)
            _currentLyrics.value = lyrics
        }.onFailure { error ->
            println("Error al cargar letras: ${error.message}")
            _currentLyrics.value = null
        }
    }

    // Deezer no requiere configuración de credenciales.
    // El método mantiene compatibilidad con la UI existente.
    fun configureSpotifyApi(clientId: String, clientSecret: String) {
        _libraryState.value = _libraryState.value.copy(message = "Deezer está disponible sin configuración")
    }

    fun saveSpotifyAuth(clientId: String, clientSecret: String) {
        _libraryState.value = _libraryState.value.copy(message = "Deezer está disponible sin configuración")
    }

    fun clearSpotifyAuth() {
        _libraryState.value = _libraryState.value.copy(message = "Deezer siempre disponible")
    }

    fun refreshLocal(pathHint: String? = null) {
        scope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true, message = null)

            runCatching {
                localMusicSource.scan(pathHint).sortedBy { it.title.lowercase() }
            }.onSuccess { tracks ->
                // Actualizar carpetas guardadas en el estado
                val folders = localMusicSource.getCustomFolders()
                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    localTracks = tracks,
                    musicFolders = folders,
                    message = "Local: ${tracks.size} pistas"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    message = error.message ?: "Error al escanear local"
                )
            }
        }
    }
    
    /**
     * Agregar una carpeta de música y escanear su contenido
     */
    fun addMusicFolder(folderPath: String) {
        localMusicSource.addCustomFolder(folderPath)
        
        // Escanear la carpeta recién agregada
        scope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true, message = null)
            
            runCatching {
                val folders = localMusicSource.getCustomFolders()
                val newTracks = localMusicSource.scanCustomFolders(listOf(folderPath))
                val existingTracks = _libraryState.value.localTracks
                
                // Combinar tracks existentes con nuevos (sin duplicados)
                val combinedTracks = (existingTracks + newTracks)
                    .distinctBy { it.id }
                    .sortedBy { it.title.lowercase() }
                
                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    localTracks = combinedTracks,
                    musicFolders = folders,
                    message = "Carpeta agregada: ${newTracks.size} canciones nuevas"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    message = "Error al agregar carpeta: ${error.message}"
                )
            }
        }
    }
    
    /**
     * Eliminar una carpeta de música
     */
    fun removeMusicFolder(folderPath: String) {
        localMusicSource.removeCustomFolder(folderPath)
        
        // Recargar todas las canciones (sin la carpeta eliminada)
        refreshLocal()
    }
    
    /**
     * Obtener lista de carpetas de música guardadas
     */
    fun getMusicFolders(): List<String> {
        return localMusicSource.getCustomFolders()
    }
    
    /**
     * Cargar carpetas guardadas al iniciar
     */
    fun loadSavedFolders() {
        val folders = localMusicSource.getCustomFolders()
        if (folders.isNotEmpty()) {
            _libraryState.value = _libraryState.value.copy(musicFolders = folders)
            // Escanear todas las carpetas guardadas
            scope.launch {
                _libraryState.value = _libraryState.value.copy(isLoading = true)
                
                runCatching {
                    val tracks = localMusicSource.scanCustomFolders(folders)
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        localTracks = tracks,
                        message = "Cargadas ${tracks.size} canciones de ${folders.size} carpetas"
                    )
                }.onFailure { error ->
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        message = "Error al cargar carpetas: ${error.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Alternar estado de favorito de una canción
     */
    fun toggleFavorite(track: Track) {
        val currentFavorites = _libraryState.value.favoriteTrackIds.toMutableSet()
        val isNowFavorite = if (track.id in currentFavorites) {
            currentFavorites.remove(track.id)
            false
        } else {
            currentFavorites.add(track.id)
            true
        }
        
        _libraryState.value = _libraryState.value.copy(
            favoriteTrackIds = currentFavorites,
            message = if (isNowFavorite) "❤️ Añadido a favoritos" else "💔 Eliminado de favoritos"
        )
        
        // Persistir favoritos
        saveFavorites(currentFavorites)
    }
    
    /**
     * Verificar si una canción es favorita
     */
    fun isFavorite(track: Track): Boolean {
        return track.id in _libraryState.value.favoriteTrackIds
    }
    
    /**
     * Cargar favoritos guardados
     */
    fun loadFavorites() {
        val settings = createSettingsStorage()
        val saved = settings.loadFavorites()
        if (saved.isNotEmpty()) {
            _libraryState.value = _libraryState.value.copy(favoriteTrackIds = saved.toSet())
        }
    }
    
    /**
     * Guardar favoritos en persistencia
     */
    private fun saveFavorites(favorites: Set<String>) {
        val settings = createSettingsStorage()
        settings.saveFavorites(favorites.toList())
    }

    fun addLocalFile(pathOrUri: String) {
        val normalized = pathOrUri.trim()

        if (normalized.isBlank()) {
            _libraryState.value = _libraryState.value.copy(message = "Pon una ruta local")
            return
        }

        if (normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true)) {
            _libraryState.value = _libraryState.value.copy(message = "Eso es remoto. Usa la caja remota")
            return
        }

        val extension = extensionOf(normalized)
        if (extension.isNotEmpty() && extension !in SUPPORTED_AUDIO_EXTENSIONS) {
            _libraryState.value = _libraryState.value.copy(message = "Formato no soportado: .$extension")
            return
        }

        val fileName = fileNameOf(normalized)
        val title = fileName.substringBeforeLast('.', fileName).ifBlank { "Pista local" }

        val track = Track(
            id = "local-manual:$normalized",
            title = title,
            artist = "Equipo local",
            uri = normalized,
            origin = TrackOrigin.LOCAL
        )

        val updated = (_libraryState.value.localTracks + track)
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }

        _libraryState.value = _libraryState.value.copy(
            localTracks = updated,
            message = "Pista local agregada"
        )

        play(track)
    }

    fun addRemoteUrl(url: String) {
        val normalized = normalizeRemoteUrl(url)
        if (normalized == null) {
            _libraryState.value = _libraryState.value.copy(message = "URL invalida. Usa http:// o https://")
            return
        }

        val generatedTitle = normalized.substringAfterLast('/').substringBefore('?').ifBlank { "Stream remoto" }

        val track = Track(
            id = "remote:$normalized",
            title = generatedTitle,
            artist = "Remoto",
            uri = normalized,
            origin = TrackOrigin.REMOTE
        )

        val updated = (_libraryState.value.remoteTracks + track)
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }

        _libraryState.value = _libraryState.value.copy(
            remoteTracks = updated,
            message = "Fuente remota agregada"
        )

        play(track)
    }

    fun searchSpotify(query: String) {
        val client = deezerClient
        if (client == null) {
            // Sin API de Deezer, buscar en música local
            searchLocalMusic(query)
            return
        }

        scope.launch {
            _libraryState.value = _libraryState.value.copy(isSearching = true, message = null)

            runCatching {
                // Buscar pistas, playlists y álbumes en paralelo
                val tracksDeferred = async { client.searchTracks(query) }
                val playlistsDeferred = async { client.searchPlaylists(query) }
                val albumsDeferred = async { client.searchAlbums(query) }

                val tracks = tracksDeferred.await()
                val playlists = playlistsDeferred.await()
                val albums = albumsDeferred.await()

                Triple(tracks, playlists, albums)
            }.onSuccess { (tracks, playlists, albums) ->
                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    spotifyResults = tracks,
                    spotifyPlaylists = playlists,
                    spotifyAlbums = albums,
                    message = "Encontrados en Deezer: ${tracks.size} pistas, ${playlists.size} playlists, ${albums.size} álbumes"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    message = error.message ?: "Error Deezer"
                )
            }
        }
    }

    fun searchSpotifyPlaylists(query: String) {
        val client = deezerClient
        if (client == null) {
            _libraryState.value = _libraryState.value.copy(message = "Deezer no está disponible")
            return
        }

        scope.launch {
            _libraryState.value = _libraryState.value.copy(isSearching = true, message = null)

            runCatching {
                client.searchPlaylists(query)
            }.onSuccess { results ->
                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    spotifyPlaylists = results,
                    message = if (results.isEmpty()) "Sin resultados" else "Playlists Deezer: ${results.size}"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    message = error.message ?: "Error Deezer"
                )
            }
        }
    }

    private fun searchLocalMusic(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _libraryState.value = _libraryState.value.copy(
                isSearching = false,
                spotifyResults = emptyList(),
                spotifyPlaylists = emptyList(),
                spotifyAlbums = emptyList(),
                message = "Búsqueda vacía"
            )
            return
        }

        // Buscar en pistas locales
        val matchingTracks = _libraryState.value.allTracks.filter { track ->
            track.title.lowercase().contains(q) ||
            track.artist.lowercase().contains(q)
        }

        // Buscar en playlists
        val matchingPlaylists = _libraryState.value.playlists.filter { playlist ->
            playlist.name.lowercase().contains(q)
        }

        // Buscar en álbumes
        val matchingAlbums = _libraryState.value.albums.filter { album ->
            album.name.lowercase().contains(q) ||
            album.artist.lowercase().contains(q)
        }

        _libraryState.value = _libraryState.value.copy(
            isSearching = false,
            spotifyResults = matchingTracks,
            spotifyPlaylists = matchingPlaylists,
            spotifyAlbums = matchingAlbums,
            message = "Encontrados localmente: ${matchingTracks.size} pistas, ${matchingPlaylists.size} playlists, ${matchingAlbums.size} álbumes"
        )
    }

    fun createSimilarAlbumFromSpotify(seedTrack: Track, albumName: String?) {
        val client = deezerClient
        if (client == null) {
            _libraryState.value = _libraryState.value.copy(message = "Deezer no está disponible")
            return
        }

        val name = albumName?.trim().takeIf { !it.isNullOrBlank() } ?: "Similar a ${seedTrack.title}"

        scope.launch {
            _libraryState.value = _libraryState.value.copy(isSearching = true, message = null)

            runCatching {
                client.searchTracks(seedTrack.artist, limit = 15)
            }.onSuccess { recs ->
                val updatedAlbums = _libraryState.value.albums.toMutableList()
                val index = updatedAlbums.indexOfFirst { it.name.equals(name, ignoreCase = true) }
                val newTracks = (listOf(seedTrack) + recs).distinctBy { it.id }

                if (index == -1) {
                    updatedAlbums += Album(name = name, artist = seedTrack.artist, tracks = newTracks)
                } else {
                    val existing = updatedAlbums[index]
                    updatedAlbums[index] = existing.copy(tracks = (existing.tracks + newTracks).distinctBy { it.id })
                }

                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    albums = updatedAlbums,
                    selectedAlbum = name,
                    message = "Album similar creado"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isSearching = false,
                    message = error.message ?: "Error Deezer"
                )
            }
        }
    }

    fun createAlbum(name: String) {
        val normalized = name.trim()
        if (normalized.isBlank()) {
            _libraryState.value = _libraryState.value.copy(message = "Nombre de album requerido")
            return
        }

        val exists = _libraryState.value.albums.any { it.name.equals(normalized, ignoreCase = true) }
        if (exists) {
            _libraryState.value = _libraryState.value.copy(message = "Ese album ya existe")
            return
        }

        val updated = _libraryState.value.albums + Album(name = normalized, tracks = emptyList())

        _libraryState.value = _libraryState.value.copy(
            albums = updated,
            selectedAlbum = normalized,
            message = "Album creado"
        )
    }

    fun addTrackToAlbum(track: Track, albumName: String) {
        val normalized = albumName.trim()
        if (normalized.isBlank()) {
            _libraryState.value = _libraryState.value.copy(message = "Nombre de album requerido")
            return
        }

        val albums = _libraryState.value.albums.toMutableList()
        val index = albums.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }

        if (index == -1) {
            albums += Album(name = normalized, tracks = listOf(track))
        } else {
            val album = albums[index]
            val updatedTracks = (album.tracks + track).distinctBy { it.id }
            albums[index] = album.copy(tracks = updatedTracks)
        }

        _libraryState.value = _libraryState.value.copy(
            albums = albums,
            selectedAlbum = normalized,
            message = "Pista agregada a album"
        )
    }

    fun saveTrackToLibrary(track: Track) {
        val updated = (_libraryState.value.savedTracks + track)
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }

        _libraryState.value = _libraryState.value.copy(
            savedTracks = updated,
            message = "Pista guardada en biblioteca"
        )
    }

    fun savePlaylistToLibrary(playlist: Playlist) {
        val client = deezerClient
        if (client == null) {
            _libraryState.value = _libraryState.value.copy(message = "Deezer no está disponible")
            return
        }

        val playlistId = playlist.id.substringAfter(":", playlist.id)
        if (playlistId.isBlank()) {
            _libraryState.value = _libraryState.value.copy(message = "ID de playlist inválida")
            return
        }

        scope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true, message = null)

            runCatching {
                // For now, just save the playlist as-is since Deezer doesn't provide track fetching
                playlist
            }.onSuccess { fullPlaylist ->
                val updated = (_libraryState.value.playlists + fullPlaylist)
                    .distinctBy { it.id }

                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    playlists = updated,
                    message = "Playlist guardada"
                )
            }.onFailure { error ->
                _libraryState.value = _libraryState.value.copy(
                    isLoading = false,
                    message = error.message ?: "Error al guardar playlist"
                )
            }
        }
    }

    fun saveAlbumToLibrary(album: Album) {
        val client = deezerClient
        if (client == null) {
            _libraryState.value = _libraryState.value.copy(message = "Deezer no está disponible")
            return
        }

        val albumId = album.id.substringAfter(":", album.id)
        val updatedAlbum = if (albumId.isNotBlank()) {
            // For now, just use the album as-is since Deezer doesn't provide track fetching
            album
        } else {
            album
        }

        val updated = (_libraryState.value.albums + updatedAlbum)
            .distinctBy { album -> album.id.takeIf { it.isNotBlank() } ?: album.name }

        _libraryState.value = _libraryState.value.copy(
            albums = updated,
            message = "Album guardado en biblioteca"
        )
    }

    fun selectAlbum(name: String?) {
        _libraryState.value = _libraryState.value.copy(selectedAlbum = name)
    }

    fun play(track: Track, queue: List<Track>? = null) {
        // Si se proporciona una cola, usarla. Si no, usar todas las tracks disponibles
        playbackQueue = queue ?: _libraryState.value.allTracks
        currentQueueIndex = playbackQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        
        incrementPlayCount(track)
        scope.launch {
            audioPlayer.play(track)
        }
        updateQueueState()
    }
    
    fun previous() {
        if (playbackQueue.isEmpty()) return
        
        val newIndex = when {
            currentQueueIndex > 0 -> currentQueueIndex - 1
            repeatMode == RepeatMode.ALL -> playbackQueue.size - 1
            else -> return // No hay anterior
        }
        
        playFromQueue(newIndex)
    }
    
    fun next() {
        if (playbackQueue.isEmpty()) return
        
        val newIndex = when {
            currentQueueIndex < playbackQueue.size - 1 -> currentQueueIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> return // No hay siguiente
        }
        
        playFromQueue(newIndex)
    }
    
    fun seekTo(position: Float) {
        audioPlayer.seekTo(position.coerceIn(0f, 1f))
    }
    
    fun pause() {
        scope.launch {
            audioPlayer.pause()
        }
    }

    fun resume() {
        scope.launch {
            audioPlayer.resume()
        }
    }

    fun stop() {
        scope.launch {
            audioPlayer.stop()
        }
    }

    fun clearMessage() {
        _libraryState.value = _libraryState.value.copy(message = null)
    }

    fun selectPlaylist(name: String?) {
        _libraryState.value = _libraryState.value.copy(selectedPlaylist = name)
    }

    fun close() {
        audioPlayer.close()
    }

    private fun incrementPlayCount(track: Track) {
        val counts = _libraryState.value.playCounts.toMutableMap()
        val current = counts[track.id] ?: 0
        counts[track.id] = current + 1
        _libraryState.value = _libraryState.value.copy(playCounts = counts)
    }

    private fun normalizeRemoteUrl(raw: String): String? {
        val value = raw.trim()
        return when {
            value.startsWith("https://", ignoreCase = true) -> value
            value.startsWith("http://", ignoreCase = true) -> value
            else -> null
        }
    }

    private fun fileNameOf(pathOrUri: String): String {
        return pathOrUri.substringAfterLast('/').substringAfterLast('\\')
    }

    private fun extensionOf(pathOrUri: String): String {
        val name = fileNameOf(pathOrUri)
        if (!name.contains('.')) return ""
        return name.substringAfterLast('.').lowercase()
    }

    private fun spotifyIdOf(rawId: String): String? {
        if (!rawId.startsWith("spotify:")) return null
        return rawId.removePrefix("spotify:").takeIf { it.isNotBlank() }
    }

    private companion object {
        val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg", "opus", "wma", "m3u8")
    }
}
