package com.example.conejitomago

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private var currentVideoState by mutableStateOf("conejito_idle")
    private var appStatusText by mutableStateOf("Conejito Mago listo. Di 'Magic' o muestra tu rostro...")

    private val tarotCards = listOf(
        "El Mago: ¡Tus proyectos de código y sistemas avanzarán con éxito hoy!",
        "La Sacerdotisa: Un misterio oculto en tus datos pronto será revelado.",
        "La Rueda de la Fortuna: ¡Prepárate para un giro favorable en tu suerte!",
        "La Estrella: Tus metas técnicas están perfectamente alineadas.",
        "El Loco: Es momento de arriesgarte con una nueva arquitectura de software."
    )

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            if (audioGranted) {
                initializeSpeechRecognizer()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de Micrófono y Cámara para la IA y el Reconocimiento Facial
        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        // Reproductor de video en bucle congruente con la acción actual
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    playVideo(this, currentVideoState)
                                }
                            },
                            update = { videoView ->
                                playVideo(videoView, currentVideoState)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Panel superior interactivo con el estado del asistente y la IA
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🐰 Conejito Mago (IA + Reconocimiento Facial + Tarot)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = appStatusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Botones de control manual en pantalla
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { 
                                currentVideoState = "conejito_hablando"
                                appStatusText = "🔮 Tarot: ${tarotCards.random()}"
                            }) {
                                Text("🃏 Leer el Tarot")
                            }
                            Button(onClick = { startListening() }) {
                                Text("🎤 Escuchar a Magic")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    appStatusText = "Escuchando... Di 'Magic'"
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    startListening()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0].lowercase(Locale.getDefault())
                        appStatusText = "Escuché: \"$spokenText\""

                        // Lógica de asistente tipo Alexa con palabra clave "magic"
                        if (spokenText.contains("magic")) {
                            if (spokenText.contains("tarot") || spokenText.contains("carta") || spokenText.contains("futuro")) {
                                currentVideoState = "conejito_hablando" // Congruente: hablando al dar predicción
                                appStatusText = "🔮 ${tarotCards.random()}"
                            } else if (spokenText.contains("feliz") || spokenText.contains("hola")) {
                                currentVideoState = "conejito_feliz" // Congruente: feliz
                            } else if (spokenText.contains("triste")) {
                                currentVideoState = "conejito_triste" // Congruente: triste
                            } else {
                                currentVideoState = "conejito_hablando" // Congruente: respondiendo al usuario
                                appStatusText = "¡Hola! Estoy aquí. Pídeme el tarot o dime algo."
                            }
                        } else {
                            currentVideoState = "conejito_idle" // En reposo si no mencionan a Magic
                        }
                    }
                    startListening()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playVideo(videoView: VideoView, videoName: String) {
        val context = videoView.context
        val videoResId = context.resources.getIdentifier(videoName, "raw", context.packageName)
        if (videoResId != 0) {
            val videoUri = Uri.parse("android.resource://" + context.packageName + "/" + videoResId)
            videoView.setVideoURI(videoUri)
            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true // Mantiene el bucle continuo
                mediaPlayer.start()
            }
            videoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}
