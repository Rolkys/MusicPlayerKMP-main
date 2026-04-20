package music.platform

import music.player.AudioPlayer
import music.player.DesktopAudioPlayer
import music.source.DesktopLocalMusicSource
import music.source.LocalMusicSource

private class DesktopPlatformFactory : PlatformFactory {
    override fun createLocalMusicSource(): LocalMusicSource = DesktopLocalMusicSource()

    override fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
}

actual fun createPlatformFactory(platformContext: Any?): PlatformFactory = DesktopPlatformFactory()
