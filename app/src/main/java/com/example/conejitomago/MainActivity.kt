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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private var currentVideoState by mutableStateOf("conejito_idle")
    private var appStatusText by mutableStateOf("Conejito Mago con IA listo. Di 'Magic'...")

    // ⚠️ Reemplaza esto con tu clave de API de Gemini oficial cuando la tengas
    private val GEMINI_API_KEY = "AQ.Ab8RN6LZqSWn4Ss9YCDvLXZqClePM-ciSq5msAxnBLVZbmBjvA"

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

                        // Reproductor de video en bucle congruente con el estado de la IA
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

                        // Panel superior interactivo con la respuesta de la IA
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🐰 Conejito Mago (Conectado a IA)",
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

                        // Botones de control táctil en pantalla
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { 
                                consultarIA("Magic, dame una lectura de tarot mágica")
                            }) {
                                Text("🃏 Tarot con IA")
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

                        // Si menciona la palabra clave "magic", mandamos la orden directamente a la IA
                        if (spokenText.contains("magic")) {
                            currentVideoState = "conejito_hablando" // Animación congruente respondiendo
                            consultarIA(spokenText)
                        } else {
                            currentVideoState = "conejito_idle"
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

    private fun consultarIA(prompt: String) {
        appStatusText = "Pensando con IA..."
        currentVideoState = "conejito_hablando"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put("parts", org.json.JSONArray().put(
                            JSONObject().put("text", "Eres un conejito mago asistente virtual inteligente y místico. Responde de forma breve y mágica en español a esta solicitud: $prompt")
                        ))
                    ))
                }

                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val candidateText = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    withContext(Dispatchers.Main) {
                        appStatusText = "Magic: $candidateText"
                        currentVideoState = "conejito_feliz"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        appStatusText = "Error al conectar con la IA."
                        currentVideoState = "conejito_triste"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appStatusText = "Excepción de red en IA."
                    currentVideoState = "conejito_triste"
                }
            }
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
                mediaPlayer.isLooping = true
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
