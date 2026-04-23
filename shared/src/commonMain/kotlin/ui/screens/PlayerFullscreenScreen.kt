package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.model.PlayerState
import music.model.Track
import ui.components.AlbumCover

@Composable
fun PlayerFullscreenScreen(
    playerState: PlayerState,
    track: Track?,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    lyrics: music.lyrics.LyricsResult? = null
) {
    // Gradiente de fondo tipo Spotify
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A2A3A),
            Color(0xFF0B0F14)
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Text(
                    text = "Reproduciendo",
                    color = Color(0xFF8EA0B5),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                IconButton(onClick = { /* Opciones */ }) {
                    Text(
                        text = "⋮",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Carátula grande
            if (track != null) {
                AlbumCover(
                    imageUrl = track.coverUrl,
                    size = 280.dp,
                    cornerRadius = 12.dp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Título y artista
                Text(
                    text = track.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = track.artist,
                    color = Color(0xFF8EA0B5),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Barra de progreso
                Slider(
                    value = playerState.progressPercent.coerceIn(0f, 1f),
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF1ED760),
                        inactiveTrackColor = Color(0xFF3E3E3E)
                    )
                )
                
                // Tiempos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = playerState.formattedCurrentTime,
                        color = Color(0xFF8EA0B5),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = playerState.formattedTotalTime,
                        color = Color(0xFF8EA0B5),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Controles principales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón favorito
                    IconButton(onClick = onToggleFavorite) {
                        Text(
                            text = if (isFavorite) "❤️" else "🤍",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    
                    // Anterior
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = "⏮",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    // Play/Pausa (botón grande central)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color.Black,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    // Siguiente
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text(
                            text = "⏭",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    
                    // Opciones adicionales
                    IconButton(onClick = { /* Cola */ }) {
                        Text(
                            text = "☰",
                            color = Color(0xFF8EA0B5),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                
                // Sección de letras sincronizadas
                if (lyrics != null && lyrics.syncedLyrics != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Calcular la línea actual basada en el tiempo de reproducción
                    val currentTimeMs = playerState.currentPositionMs
                    val currentLineIndex = lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentTimeMs }
                        .coerceAtLeast(0)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF1A2A3A), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        // Mostrar 3 líneas: anterior, actual y siguiente
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Línea anterior (atenuada)
                            if (currentLineIndex > 0) {
                                Text(
                                    text = lyrics.syncedLyrics[currentLineIndex - 1].text,
                                    color = Color(0xFF6F7C8A),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            
                            // Línea actual (resaltada)
                            if (currentLineIndex < lyrics.syncedLyrics.size) {
                                Text(
                                    text = lyrics.syncedLyrics[currentLineIndex].text,
                                    color = Color(0xFF1ED760),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                            
                            // Línea siguiente (atenuada)
                            if (currentLineIndex < lyrics.syncedLyrics.size - 1) {
                                Text(
                                    text = lyrics.syncedLyrics[currentLineIndex + 1].text,
                                    color = Color(0xFF6F7C8A),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                } else if (lyrics?.plainLyrics != null) {
                    // Letras sin sincronización
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = lyrics.plainLyrics.take(200) + if (lyrics.plainLyrics.length > 200) "..." else "",
                        color = Color(0xFF8EA0B5),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                
            } else {
                // Sin canción reproduciendo
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay música reproduciendo",
                        color = Color(0xFF8EA0B5),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}
