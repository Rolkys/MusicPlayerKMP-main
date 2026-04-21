package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import music.controller.LibraryUiState
import music.controller.MusicController
import music.model.Album
import music.model.Playlist
import music.model.Track
import music.model.TrackOrigin

private val SidebarWidth = 240.dp
private val BottomBarHeight = 86.dp

private enum class Screen {
    HOME,
    SEARCH,
    LIBRARY,
    ALBUMS
}

@Composable
fun App(
    controller: MusicController,
    onPickLocalFile: () -> Unit,
    onPickMusicFolder: () -> Unit = {}
) {
    val libraryState by controller.libraryState.collectAsStateSafe()
    val playerState by controller.playerState.collectAsStateSafe()

    var localPath by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var albumName by remember { mutableStateOf("") }
    var spotifyQuery by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf(Screen.HOME) }

    LaunchedEffect(Unit) {
        controller.loadSavedFolders()
    }

    SpotifyTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar()
                Box(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        screen = screen,
                        albums = libraryState.albums,
                        playlists = libraryState.playlists,
                        selectedAlbum = libraryState.selectedAlbum,
                        selectedPlaylist = libraryState.selectedPlaylist,
                        onSelectScreen = { screen = it },
                        onSelectAlbum = controller::selectAlbum,
                        onSelectPlaylist = controller::selectPlaylist
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = SidebarWidth, bottom = BottomBarHeight)
                            .padding(20.dp)
                    ) {
                        when (screen) {
                            Screen.HOME -> HomeScreen(libraryState)
                            Screen.SEARCH -> SearchScreen(
                                isSearching = libraryState.isSearching,
                                spotifyQuery = spotifyQuery,
                                onQueryChange = { spotifyQuery = it },
                                results = libraryState.spotifyResults,
                                playlists = libraryState.spotifyPlaylists,
                                albums = libraryState.spotifyAlbums,
                                onSearch = { controller.searchSpotify(spotifyQuery) },
                                onPlay = controller::play,
                                onCreateSimilar = { track ->
                                    controller.createSimilarAlbumFromSpotify(track, albumName)
                                },
                                albumName = albumName,
                                onAlbumNameChange = { albumName = it },
                                onAddToAlbum = { track -> controller.addTrackToAlbum(track, albumName) },
                                onSaveToLibrary = controller::saveTrackToLibrary,
                                onSavePlaylistToLibrary = controller::savePlaylistToLibrary,
                                onSaveAlbumToLibrary = controller::saveAlbumToLibrary
                            )
                            Screen.LIBRARY -> LibraryScreen(
                                localPath = localPath,
                                remoteUrl = remoteUrl,
                                albumName = albumName,
                                tracks = libraryState.allTracks,
                                musicFolders = libraryState.musicFolders,
                                onLocalPathChange = { localPath = it },
                                onRemoteUrlChange = { remoteUrl = it },
                                onAlbumNameChange = { albumName = it },
                                onAddLocal = {
                                    controller.addLocalFile(localPath)
                                    localPath = ""
                                },
                                onPickLocalFile = onPickLocalFile,
                                onPickMusicFolder = onPickMusicFolder,
                                onAddRemote = {
                                    controller.addRemoteUrl(remoteUrl)
                                    remoteUrl = ""
                                },
                                onCreateAlbum = { controller.createAlbum(albumName) },
                                onPlay = controller::play,
                                onAddToAlbum = { track -> controller.addTrackToAlbum(track, albumName) },
                                onRemoveFolder = { folder -> controller.removeMusicFolder(folder) },
                                onRefreshFolders = { controller.refreshLocal() }
                            )
                            Screen.ALBUMS -> AlbumsScreen(
                                albums = libraryState.albums,
                                playlists = libraryState.playlists,
                                selectedAlbum = libraryState.selectedAlbum,
                                selectedPlaylist = libraryState.selectedPlaylist,
                                selectedTracks = if (libraryState.selectedPlaylist != null) libraryState.selectedPlaylistTracks else libraryState.selectedAlbumTracks,
                                onSelectAlbum = controller::selectAlbum,
                                onSelectPlaylist = controller::selectPlaylist,
                                onPlay = controller::play
                            )
                        }

                        libraryState.message?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF1E2630))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = Color(0xFFCAD6E2))
                            Button(onClick = controller::clearMessage) { Text("OK") }
                        }
                    }

                    BottomBar(
                        playerState = playerState,
                        onToggle = {
                            if (playerState.isPlaying) controller.pause() else controller.resume()
                        },
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0F14))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MusicPlayerKMP", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Local + Spotify", color = Color(0xFF8EA0B5))
    }
}

@Composable
private fun Sidebar(
    screen: Screen,
    albums: List<Album>,
    playlists: List<Playlist>,
    selectedAlbum: String?,
    selectedPlaylist: String?,
    onSelectScreen: (Screen) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSelectPlaylist: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .width(SidebarWidth)
            .fillMaxHeight()
            .background(Color(0xFF0F141A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NavItem("Inicio", screen == Screen.HOME) { onSelectScreen(Screen.HOME) }
        NavItem("Buscar", screen == Screen.SEARCH) { onSelectScreen(Screen.SEARCH) }
        NavItem("Biblioteca", screen == Screen.LIBRARY) { onSelectScreen(Screen.LIBRARY) }
        NavItem("Albums", screen == Screen.ALBUMS) { onSelectScreen(Screen.ALBUMS) }
        HorizontalDivider(color = Color(0xFF1E2630))
        Text("Tus playlists", color = Color(0xFF9FB2C8))
        Spacer(modifier = Modifier.height(6.dp))
        if (playlists.isEmpty()) {
            Text("Sin playlists", color = Color(0xFF6F7C8A))
        } else {
            playlists.forEach { playlist ->
                NavItem(playlist.name, selectedPlaylist == playlist.name) {
                    onSelectPlaylist(playlist.name)
                    onSelectScreen(Screen.ALBUMS) // Reuse ALBUMS screen for playlists
                }
            }
        }
        HorizontalDivider(color = Color(0xFF1E2630))
        Text("Tus albums", color = Color(0xFF9FB2C8))
        Spacer(modifier = Modifier.height(6.dp))
        if (albums.isEmpty()) {
            Text("Sin albums", color = Color(0xFF6F7C8A))
        } else {
            albums.forEach { album ->
                NavItem(album.name, selectedAlbum == album.name) {
                    onSelectAlbum(album.name)
                    onSelectScreen(Screen.ALBUMS)
                }
            }
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Color(0xFF1ED760) else Color(0xFFCAD6E2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = color)
    }
}

@Composable
private fun HomeScreen(libraryState: LibraryUiState) {
    Text("Inicio", color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    val topAlbums = mostPlayedAlbums(libraryState)
    if (topAlbums.isEmpty()) {
        Text("Sin reproducciones aun", color = Color(0xFF9FB2C8))
        return
    }

    SectionTitle("Albums mas escuchados")
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topAlbums.forEach { albumStat ->
            Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111820))) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(albumStat.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("Reproducciones: ${albumStat.plays}", color = Color(0xFF8EA0B5))
                        Text("Pistas: ${albumStat.tracks}", color = Color(0xFF6F7C8A))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    isSearching: Boolean,
    spotifyQuery: String,
    onQueryChange: (String) -> Unit,
    results: List<Track>,
    playlists: List<Playlist>,
    albums: List<Album>,
    onSearch: () -> Unit,
    onPlay: (Track) -> Unit,
    onCreateSimilar: (Track) -> Unit,
    albumName: String,
    onAlbumNameChange: (String) -> Unit,
    onAddToAlbum: (Track) -> Unit,
    onSaveToLibrary: (Track) -> Unit,
    onSavePlaylistToLibrary: (Playlist) -> Unit,
    onSaveAlbumToLibrary: (Album) -> Unit
) {
    Text("Buscar Música (Deezer)", color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = spotifyQuery,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Buscar artista, canción o álbum") }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onSearch, enabled = !isSearching) {
        Text(if (isSearching) "Buscando..." else "Buscar")
    }

    Spacer(modifier = Modifier.height(16.dp))
    SectionTitle("Album destino")
    OutlinedTextField(
        value = albumName,
        onValueChange = onAlbumNameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Nombre de album") }
    )

    Spacer(modifier = Modifier.height(16.dp))
    SectionTitle("Resultados")

    if (results.isEmpty()) {
        Text("Sin resultados", color = Color(0xFF9FB2C8))
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    albumName = albumName,
                    onPlay = onPlay,
                    onAddToAlbum = { onAddToAlbum(track) },
                    addLabel = if (albumName.isBlank()) "Album" else "Agregar",
                    extraActionLabel = "Similar",
                    onExtraAction = { onCreateSimilar(track) },
                    onSaveToLibrary = { onSaveToLibrary(track) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    SectionTitle("Álbumes")

    if (albums.isEmpty()) {
        Text("Sin álbumes", color = Color(0xFF9FB2C8))
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(albums, key = { it.name }) { album ->
                AlbumRow(
                    album = album,
                    onSaveToLibrary = { onSaveAlbumToLibrary(album) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun LibraryScreen(
    localPath: String,
    remoteUrl: String,
    albumName: String,
    tracks: List<Track>,
    musicFolders: List<String>,
    onLocalPathChange: (String) -> Unit,
    onRemoteUrlChange: (String) -> Unit,
    onAlbumNameChange: (String) -> Unit,
    onAddLocal: () -> Unit,
    onPickLocalFile: () -> Unit,
    onPickMusicFolder: () -> Unit,
    onAddRemote: () -> Unit,
    onCreateAlbum: () -> Unit,
    onPlay: (Track) -> Unit,
    onAddToAlbum: (Track) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRefreshFolders: () -> Unit
) {
    Text("Biblioteca", color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    // Sección de Carpetas de Música
    SectionTitle("Tus carpetas de música")
    Spacer(modifier = Modifier.height(8.dp))
    
    Button(
        onClick = onPickMusicFolder,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
    ) {
        Text("+ Añadir carpeta de música", color = Color.Black, fontWeight = FontWeight.Medium)
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    if (musicFolders.isEmpty()) {
        Text(
            "No has añadido carpetas. Pulsa el botón arriba para añadir tu música.",
            color = Color(0xFF8EA0B5),
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        Text(
            "${tracks.size} canciones en ${musicFolders.size} carpetas",
            color = Color(0xFF8EA0B5),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 150.dp)
        ) {
            items(musicFolders, key = { it }) { folder ->
                FolderRow(
                    folderPath = folder,
                    onRemove = { onRemoveFolder(folder) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onRefreshFolders,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C25))
        ) {
            Text("🔄 Refrescar carpetas", color = Color(0xFFCAD6E2))
        }
    }

    HorizontalDivider(color = Color(0xFF1E2630), modifier = Modifier.padding(vertical = 16.dp))

    SectionTitle("Agregar archivo individual")
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = localPath,
        onValueChange = onLocalPathChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Ruta local o file://") }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onAddLocal) { Text("Agregar local") }
        Button(onClick = onPickLocalFile) { Text("Elegir archivo") }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SectionTitle("Remoto")
    OutlinedTextField(
        value = remoteUrl,
        onValueChange = onRemoteUrlChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("URL remota http/https") }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onAddRemote) { Text("Agregar remoto") }

    Spacer(modifier = Modifier.height(18.dp))
    SectionTitle("Crear album")
    OutlinedTextField(
        value = albumName,
        onValueChange = onAlbumNameChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Nombre de album") }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onCreateAlbum) { Text("Crear album") }

    Spacer(modifier = Modifier.height(18.dp))
    SectionTitle("Todas las pistas")
    TrackList(
        tracks = tracks,
        albumName = albumName,
        onPlay = onPlay,
        onAddToAlbum = onAddToAlbum
    )
}

@Composable
private fun FolderRow(
    folderPath: String,
    onRemove: () -> Unit
) {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF151C25)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = folderPath.substringAfterLast('/').substringAfterLast('\\').takeIf { it.isNotBlank() } ?: folderPath,
                color = Color(0xFFCAD6E2),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Text("✕", color = Color(0xFFE81123))
            }
        }
    }
}

@Composable
private fun AlbumsScreen(
    albums: List<Album>,
    playlists: List<Playlist>,
    selectedAlbum: String?,
    selectedPlaylist: String?,
    selectedTracks: List<Track>,
    onSelectAlbum: (String?) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onPlay: (Track) -> Unit
) {
    Text("Albums y Playlists", color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    if (playlists.isNotEmpty()) {
        Text("Playlists", color = Color(0xFF9FB2C8))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            playlists.forEach { playlist ->
                Button(
                    onClick = { onSelectPlaylist(playlist.name) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlaylist == playlist.name) Color(0xFF1ED760) else Color(0xFF151C25)
                    )
                ) {
                    Text(playlist.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (albums.isNotEmpty()) {
        Text("Albums", color = Color(0xFF9FB2C8))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            albums.forEach { album ->
                Button(
                    onClick = { onSelectAlbum(album.name) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAlbum == album.name) Color(0xFF1ED760) else Color(0xFF151C25)
                    )
                ) {
                    Text(album.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (albums.isEmpty() && playlists.isEmpty()) {
        Text("Sin albums ni playlists", color = Color(0xFF9FB2C8))
        return
    }

    val title = selectedAlbum ?: selectedPlaylist ?: "Selecciona un album o playlist"
    SectionTitle(title)

    TrackList(tracks = selectedTracks, albumName = "", onPlay = onPlay, onAddToAlbum = { })
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    albumName: String,
    onPlay: (Track) -> Unit,
    onAddToAlbum: (Track) -> Unit
) {
    if (tracks.isEmpty()) {
        Text("No hay pistas", color = Color(0xFF9FB2C8))
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tracks, key = { it.id }) { track ->
            TrackRow(track = track, albumName = albumName, onPlay = onPlay, onAddToAlbum = onAddToAlbum)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    albumName: String,
    onPlay: (Track) -> Unit,
    onAddToAlbum: (Track) -> Unit,
    addLabel: String = "Agregar",
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
    onSaveToLibrary: ((Track) -> Unit)? = null
) {
    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111820))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlay(track) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(track.title, color = Color.White, fontWeight = FontWeight.Medium)
                Text(track.artist, color = Color(0xFF8EA0B5))
                Text(
                    if (track.origin == TrackOrigin.REMOTE) "Remoto" else "Local",
                    color = Color(0xFF6F7C8A)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSaveToLibrary != null) {
                    Button(
                        onClick = { onSaveToLibrary(track) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
                    ) {
                        Text("Guardar")
                    }
                }
                if (albumName.isNotBlank()) {
                    Button(
                        onClick = { onAddToAlbum(track) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
                    ) {
                        Text(addLabel)
                    }
                }
                if (extraActionLabel != null && onExtraAction != null) {
                    Button(
                        onClick = onExtraAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
                    ) {
                        Text(extraActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onSaveToLibrary: () -> Unit
) {
    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111820))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(playlist.name, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Playlist", color = Color(0xFF8EA0B5))
                Text("${playlist.tracks.size} pistas", color = Color(0xFF6F7C8A))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveToLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    album: Album,
    onSaveToLibrary: () -> Unit
) {
    Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111820))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(album.name, color = Color.White, fontWeight = FontWeight.Medium)
                Text(album.artist, color = Color(0xFF8EA0B5))
                Text("${album.tracks.size} pistas", color = Color(0xFF6F7C8A))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveToLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    playerState: music.model.PlayerState,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .background(Color(0xFF0B1016))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val now = playerState.currentTrack
        Column {
            Text(now?.title ?: "Sin reproduccion", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(now?.artist ?: "", color = Color(0xFF7E8FA3))
        }
        Button(
            onClick = onToggle,
            enabled = now != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760))
        ) {
            Text(if (playerState.isPlaying) "Pausa" else "Play")
        }
    }
}

@Composable
private fun SpotifyTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFF1ED760),
        secondary = Color(0xFF1ED760),
        background = Color(0xFF0B0F14),
        surface = Color(0xFF111820),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(colorScheme = scheme, content = content)
}

private data class AlbumStat(
    val name: String,
    val plays: Int,
    val tracks: Int
)

private fun mostPlayedAlbums(state: LibraryUiState): List<AlbumStat> {
    if (state.albums.isEmpty()) return emptyList()

    val counts = state.playCounts

    return state.albums.map { album ->
        val plays = album.tracks.sumOf { counts[it.id] ?: 0 }
        AlbumStat(album.name, plays, album.tracks.size)
    }.sortedByDescending { it.plays }.filter { it.plays > 0 }.take(6)
}

@Composable
private fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState()
