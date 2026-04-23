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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.model.RepeatMode
import music.model.Track
import ui.components.AlbumCover

@Composable
fun PlaybackQueueScreen(
    queue: List<Track>,
    currentTrack: Track?,
    currentIndex: Int,
    onClose: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    isShuffleOn: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cola de reproducción",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info de la cola
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${queue.size} canciones",
                    color = Color(0xFF8EA0B5),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shuffle toggle
                    IconButton(onClick = onToggleShuffle) {
                        Text(
                            text = if (isShuffleOn) "🔀" else "➡️",
                            color = if (isShuffleOn) Color(0xFF1ED760) else Color(0xFF8EA0B5),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    // Repeat toggle
                    IconButton(onClick = onToggleRepeat) {
                        val repeatIcon = when (repeatMode) {
                            RepeatMode.OFF -> "🔁"
                            RepeatMode.ALL -> "🔁"
                            RepeatMode.ONE -> "🔂"
                        }
                        Text(
                            text = repeatIcon,
                            color = if (repeatMode != RepeatMode.OFF) Color(0xFF1ED760) else Color(0xFF8EA0B5),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    if (queue.isNotEmpty()) {
                        IconButton(onClick = onClearQueue) {
                            Text(
                                text = "🗑️",
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lista de canciones
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "La cola está vacía\nAñade canciones desde tu biblioteca",
                        color = Color(0xFF8EA0B5),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(queue, key = { index, track -> "${track.id}_$index" }) { index, track ->
                        val isCurrent = index == currentIndex
                        QueueTrackItem(
                            track = track,
                            index = index,
                            isCurrent = isCurrent,
                            onClick = { onSelectTrack(index) },
                            onRemove = { onRemoveTrack(index) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackItem(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1A2332) else Color(0xFF111820)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número o indicador de reproducción
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Text(
                        text = "▶",
                        color = Color(0xFF1ED760),
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        color = Color(0xFF6F7C8A),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Carátula
            AlbumCover(
                imageUrl = track.coverUrl,
                size = 48.dp,
                cornerRadius = 4.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) Color(0xFF1ED760) else Color.White,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = track.artist,
                    color = Color(0xFF8EA0B5),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Botón eliminar
            IconButton(onClick = onRemove) {
                Text(
                    text = "✕",
                    color = Color(0xFF6F7C8A),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
