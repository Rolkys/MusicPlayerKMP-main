@file:JvmName("SpotifyPlatformDesktop")

package music.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.Properties
import kotlinx.serialization.json.Json

private val credentialsFile: File by lazy {
    val home = System.getProperty("user.home") ?: "."
    File(home, ".musicplayerkmp/spotify.properties")
}

actual fun createSpotifyHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

actual fun getSpotifyCredentials(): SpotifyCredentials? {
    val envId = System.getenv("SPOTIFY_CLIENT_ID")?.trim().orEmpty()
    val envSecret = System.getenv("SPOTIFY_CLIENT_SECRET")?.trim().orEmpty()

    if (envId.isNotBlank() && envSecret.isNotBlank()) {
        return SpotifyCredentials(envId, envSecret)
    }

    if (!credentialsFile.exists()) return null

    val props = Properties()
    credentialsFile.inputStream().use { props.load(it) }

    val clientId = props.getProperty("client_id").orEmpty().trim()
    val clientSecret = props.getProperty("client_secret").orEmpty().trim()

    if (clientId.isBlank() || clientSecret.isBlank()) return null

    return SpotifyCredentials(clientId, clientSecret)
}

actual fun saveSpotifyCredentials(credentials: SpotifyCredentials) {
    val dir = credentialsFile.parentFile
    if (dir != null && !dir.exists()) {
        dir.mkdirs()
    }

    val props = Properties()
    props.setProperty("client_id", credentials.clientId)
    props.setProperty("client_secret", credentials.clientSecret)

    credentialsFile.outputStream().use { props.store(it, "Spotify Credentials") }
}

actual fun clearSpotifyCredentials() {
    if (credentialsFile.exists()) {
        credentialsFile.delete()
    }
}
