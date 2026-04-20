package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import music.model.Track
import viewmodel.PlayerViewModel

@Composable
fun NowPlaying(
    viewModel: PlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("🎵", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                uiState.currentTrack?.let { track ->
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                } ?: Text(
                    text = "No track playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play/pause button
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Text(
                    if (uiState.isPlaying) "⏸️" else "▶️",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}