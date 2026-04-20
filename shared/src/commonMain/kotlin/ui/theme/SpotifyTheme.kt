package ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1DB954), // Spotify green
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1DB954),
    onPrimaryContainer = androidx.compose.ui.graphics.Color.Black,
    secondary = androidx.compose.ui.graphics.Color(0xFF535353),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF535353),
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
    tertiary = androidx.compose.ui.graphics.Color(0xFF535353),
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF535353),
    onTertiaryContainer = androidx.compose.ui.graphics.Color.White,
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = androidx.compose.ui.graphics.Color.Black,
    errorContainer = androidx.compose.ui.graphics.Color(0xFFB00020),
    onErrorContainer = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFF121212), // Dark background
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB3B3B3),
    outline = androidx.compose.ui.graphics.Color(0xFF535353)
)

@Composable
fun SpotifyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpotifyDarkColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}