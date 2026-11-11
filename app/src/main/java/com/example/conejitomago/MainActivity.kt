package com.example.conejitomago

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentVideo by remember { mutableStateOf("conejito_idle") }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Reproductor de video en bucle para el conejito mago
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    val videoResId = context.resources.getIdentifier(currentVideo, "raw", context.packageName)
                                    if (videoResId != 0) {
                                        val videoUri = Uri.parse("android.resource://" + context.packageName + "/" + videoResId)
                                        setVideoURI(videoUri)
                                        setOnPreparedListener { mediaPlayer ->
                                            mediaPlayer.isLooping = true
                                            mediaPlayer.start()
                                        }
                                        start()
                                    }
                                }
                            },
                            update = { videoView ->
                                val context = videoView.context
                                val videoResId = context.resources.getIdentifier(currentVideo, "raw", context.packageName)
                                if (videoResId != 0) {
                                    val videoUri = Uri.parse("android.resource://" + context.packageName + "/" + videoResId)
                                    videoView.setVideoURI(videoUri)
                                    videoView.setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.isLooping = true
                                        mediaPlayer.start()
                                    }
                                    videoView.start()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Botones de control interactivo en pantalla
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { currentVideo = "conejito_feliz" }) {
                                Text("¡Feliz!")
                            }
                            Button(onClick = { currentVideo = "conejito_triste" }) {
                                Text("Triste")
                            }
                            Button(onClick = { currentVideo = "conejito_hablando" }) {
                                Text("Hablando")
                            }
                        }
                    }
                }
            }
        }
    }
}
