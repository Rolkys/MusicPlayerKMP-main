package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun AlbumCover(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 4.dp,
    placeholder: @Composable () -> Unit = { DefaultPlaceholder(size) }
) {
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            placeholder()
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop,
            placeholder = null,
            error = null
        )
    }
}

@Composable
private fun DefaultPlaceholder(size: Dp) {
    Text(
        text = "🎵",
        style = if (size > 60.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun ArtistImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👤",
                style = if (size > 60.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size / 2)),
            contentScale = ContentScale.Crop
        )
    }
}

// Función de utilidad para extraer color dominante (placeholder)
fun extractDominantColor(imageUrl: String?): Color? {
    // TODO: Implementar extracción de color dominante usando Coil o Palette
    return null
}

// Función para generar gradiente tipo Spotify basado en imagen
@Composable
fun SpotifyGradientBackground(
    imageUrl: String?,
    content: @Composable () -> Unit
) {
    // Por ahora usa un gradiente estático, en el futuro podría ser dinámico basado en la imagen
    content()
}
