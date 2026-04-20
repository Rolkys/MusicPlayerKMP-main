@file:JvmName("SpotifyPlatformCommon")

package music.spotify

import io.ktor.client.HttpClient

expect fun createSpotifyHttpClient(): HttpClient

expect fun getSpotifyCredentials(): SpotifyCredentials?

expect fun saveSpotifyCredentials(credentials: SpotifyCredentials)

expect fun clearSpotifyCredentials()

fun createSpotifyClientOrNull(): SpotifyClient? {
    val creds = getSpotifyCredentials() ?: return null
    return SpotifyClient(createSpotifyHttpClient(), creds)
}
