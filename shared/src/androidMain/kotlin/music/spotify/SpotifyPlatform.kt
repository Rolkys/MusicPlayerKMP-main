@file:JvmName("SpotifyPlatformAndroid")

package music.spotify

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private var appContext: Context? = null

fun initSpotifyContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createSpotifyHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

actual fun getSpotifyCredentials(): SpotifyCredentials? {
    val context = appContext ?: return null
    val prefs = context.getSharedPreferences("spotify", Context.MODE_PRIVATE)
    val clientId = prefs.getString("client_id", "")?.trim().orEmpty()
    val clientSecret = prefs.getString("client_secret", "")?.trim().orEmpty()

    if (clientId.isBlank() || clientSecret.isBlank()) return null

    return SpotifyCredentials(clientId, clientSecret)
}

actual fun saveSpotifyCredentials(credentials: SpotifyCredentials) {
    val context = appContext ?: return
    val prefs = context.getSharedPreferences("spotify", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("client_id", credentials.clientId)
        .putString("client_secret", credentials.clientSecret)
        .apply()
}

actual fun clearSpotifyCredentials() {
    val context = appContext ?: return
    val prefs = context.getSharedPreferences("spotify", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
}
