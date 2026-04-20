package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import music.controller.MusicController
import music.platform.createPlatformFactory
import music.spotify.initSpotifyContext
import ui.App

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var controller: MusicController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSpotifyContext(applicationContext)

        val factory = createPlatformFactory(applicationContext)
        controller = MusicController(
            localMusicSource = factory.createLocalMusicSource(),
            audioPlayer = factory.createAudioPlayer(),
            scope = scope
        )

        requestAudioPermissionIfNeeded()

        val pickLocalAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                controller.addLocalFile(uri.toString())
            }
        }

        setContent {
            App(controller, onPickLocalFile = { pickLocalAudio.launch(arrayOf("audio/*")) })
        }
    }

    override fun onDestroy() {
        controller.close()
        scope.cancel()
        super.onDestroy()
    }

    private fun requestAudioPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), AUDIO_PERMISSION_REQUEST)
        }
    }

    private companion object {
        private const val AUDIO_PERMISSION_REQUEST = 1001
    }
}
