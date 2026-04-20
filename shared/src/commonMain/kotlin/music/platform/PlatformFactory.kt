package music.platform

import music.player.AudioPlayer
import music.source.LocalMusicSource

interface PlatformFactory {
    fun createLocalMusicSource(): LocalMusicSource
    fun createAudioPlayer(): AudioPlayer
}

expect fun createPlatformFactory(platformContext: Any? = null): PlatformFactory
