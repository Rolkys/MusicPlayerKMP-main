package music.platform

import android.content.Context
import music.player.AndroidAudioPlayer
import music.player.AudioPlayer
import music.source.AndroidLocalMusicSource
import music.source.LocalMusicSource

private class AndroidPlatformFactory(
    private val appContext: Context
) : PlatformFactory {
    override fun createLocalMusicSource(): LocalMusicSource = AndroidLocalMusicSource(appContext)

    override fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer(appContext)
}

actual fun createPlatformFactory(platformContext: Any?): PlatformFactory {
    val context = platformContext as? Context
        ?: error("Android requiere Context para crear PlatformFactory")

    return AndroidPlatformFactory(context.applicationContext)
}
