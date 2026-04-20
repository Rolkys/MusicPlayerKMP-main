package music.spotify

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class SpotifyCredentials(
    val clientId: String,
    val clientSecret: String
) {
    @OptIn(ExperimentalEncodingApi::class)
    val basicAuth: String
        get() = Base64.encode("$clientId:$clientSecret".encodeToByteArray())
}
