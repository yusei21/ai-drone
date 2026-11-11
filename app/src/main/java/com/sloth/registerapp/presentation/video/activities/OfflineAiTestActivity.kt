package com.sloth.registerapp.presentation.video.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sloth.registerapp.features.mldetection.data.ml.YoloInferenceEngine
import com.sloth.registerapp.features.mldetection.domain.model.DetectionResult
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.domain.model.MLModelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Teste experimental OFFLINE: camera local + TFLite. Nenhuma dependencia de voo,
 * controlador, conexao DJI ou servidor para abrir esta tela.
 */
class OfflineAiTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OfflineAiTestScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
private fun OfflineAiTestScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // TFLite GPU delegate must be created and used on the same thread.
    val inferenceDispatcher = remember { Executors.newSingleThreadExecutor().asCoroutineDispatcher() }
    val inferenceMutex = remember { Mutex() }
    var engine by remember { mutableStateOf<YoloInferenceEngine?>(null) }
    var modelName by remember { mutableStateOf<String?>(null) }
    var detections by remember { mutableStateOf<List<DetectionResult>>(emptyList()) }
    var inferenceMs by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf("Importe um modelo TFLite para iniciar a deteccao.") }
    var isImporting by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (!granted) status = "Permissao de camera necessaria para teste ao vivo."
    }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        scope.launch {
            val result = runCatching {
                val file = withContext(Dispatchers.IO) {
                    importTfliteModel(context, uri)
                }
                val newModel = MLModel(
                    id = "offline-${file.nameWithoutExtension}",
                    name = file.nameWithoutExtension,
                    classes = listOf("pessoa"),
                    filePath = file.absolutePath,
                    source = MLModelSource.LOCAL_FILE,
                    sizeBytes = file.length()
                )
                inferenceMutex.withLock {
                    withContext(inferenceDispatcher) {
                        val loaded = YoloInferenceEngine.create(context, newModel)
                        engine?.close()
                        engine = loaded
                    }
                }
                modelName = newModel.name
                detections = emptyList()
                "Modelo carregado: ${newModel.name}"
            }
            status = result.getOrElse { "Falha ao importar modelo: ${it.message}" }
            isImporting = false
        }
    }

    DisposableEffect(inferenceDispatcher) {
        onDispose {
            // Activity finishing: stop work before disposing the delegate/dispatcher.
            scope.launch(inferenceDispatcher) { engine?.close() }
            // Dispatcher is intentionally not closed before the queued cleanup runs.
        }
    }

    DisposableEffect(cameraGranted, previewView, lifecycleOwner) {
        if (!cameraGranted) {
            onDispose { }
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            var active = true
            var provider: ProcessCameraProvider? = null
            future.addListener({
                if (active) {
                    runCatching {
                        val p = future.get()
                        provider = p
                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }
                        p.unbindAll()
                        p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                    }.onFailure { status = "Falha ao abrir camera: ${it.message}" }
                }
            }, ContextCompat.getMainExecutor(context))
            onDispose {
                active = false
                provider?.unbindAll()
            }
        }
    }

    LaunchedEffect(cameraGranted, engine, previewView) {
        while (isActive && cameraGranted && engine != null) {
            val snapshot: Bitmap? = withContext(Dispatchers.Main) { previewView.bitmap }
            if (snapshot != null) {
                try {
                    inferenceMutex.withLock {
                        val current = engine
                        if (current != null) {
                            val start = System.nanoTime()
                            val boxes = withContext(inferenceDispatcher) { current.detect(snapshot) }
                            inferenceMs = (System.nanoTime() - start) / 1_000_000L
                            detections = boxes
                        }
                    }
                } catch (e: Exception) {
                    detections = emptyList()
                    status = "Falha na inferencia: ${e.message}"
                } finally {
                    snapshot.recycle()
                }
            }
            delay(450L) // Limite intencional para uso inicial, nao representa FPS maximo.
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraGranted) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                for (d in detections) {
                    val r = d.normalizedRect
                    drawRect(
                        color = Color(0xFF00E5A0),
                        topLeft = Offset(r.left * size.width, r.top * size.height),
                        size = Size(
                            ((r.right - r.left) * size.width).coerceAtLeast(0f),
                            ((r.bottom - r.top) * size.height).coerceAtLeast(0f)
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = Color(0xDD0B1323), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("AI-DRONE | Teste offline de pessoas", color = Color.White)
                    Text("Sem drone, sem comandos de voo", color = Color.LightGray)
                    Text("Modelo: ${modelName ?: "nenhum"}", color = Color.White)
                    Text(
                        "Deteccoes: ${detections.size} | Inferencia: ${inferenceMs?.let { "${it} ms" } ?: "--"}",
                        color = Color(0xFF00E5A0)
                    )
                    Text(status, color = Color.LightGray)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClose) { Text("Voltar") }
                Button(onClick = { modelPicker.launch("*/*") }, enabled = !isImporting) {
                    Text("Importar .tflite")
                }
                if (isImporting) CircularProgressIndicator()
            }
            if (!cameraGranted) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Permitir camera")
                }
            }
        }
    }
}

private fun importTfliteModel(context: android.content.Context, uri: Uri): File {
    val displayName = context.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "pessoas.tflite"
    require(displayName.lowercase().endsWith(".tflite")) {
        "Selecione um arquivo com extensao .tflite"
    }
    val safeBase = displayName.removeSuffix(".tflite")
        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        .take(60)
        .ifBlank { "pessoas" }
    val dir = File(context.filesDir, "ml_models").apply { mkdirs() }
    val dest = File(dir, "$safeBase.tflite")
    val tmp = File(dir, "$safeBase.tmp")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Nao foi possivel ler o arquivo")
        require(tmp.length() > 1024) { "Arquivo do modelo vazio ou invalido" }
        require(tmp.length() < 200L * 1024 * 1024) { "Modelo muito grande para importacao" }
        check(tmp.renameTo(dest)) { "Falha ao salvar modelo" }
        File(dir, "$safeBase.txt").writeText("pessoa\n")
        return dest
    } finally {
        if (tmp.exists()) tmp.delete()
    }
}
