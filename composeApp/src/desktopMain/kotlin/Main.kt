import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import music.controller.MusicController
import music.platform.createPlatformFactory
import music.platform.pickMusicFolder
import ui.App
import java.awt.FileDialog
import java.awt.Frame

fun main() = application {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val factory = remember { createPlatformFactory() }
    val controller = remember {
        MusicController(
            localMusicSource = factory.createLocalMusicSource(),
            audioPlayer = factory.createAudioPlayer(),
            scope = scope
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.close()
            scope.cancel()
        }
    }

    val pickLocalFile = {
        val dialog = FileDialog(null as Frame?, "Elegir audio", FileDialog.LOAD)
        dialog.isVisible = true
        val file = dialog.file
        val dir = dialog.directory
        if (!file.isNullOrBlank() && !dir.isNullOrBlank()) {
            controller.addLocalFile(dir + file)
        }
    }
    
    val pickMusicFolder = {
        val folder = pickMusicFolder()
        if (folder != null) {
            controller.addMusicFolder(folder)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MusicPlayerKMP - Sin anuncios",
        state = rememberWindowState(width = 1100.dp, height = 760.dp)
    ) {
        App(
            controller = controller,
            onPickLocalFile = pickLocalFile,
            onPickMusicFolder = pickMusicFolder
        )
    }
}
