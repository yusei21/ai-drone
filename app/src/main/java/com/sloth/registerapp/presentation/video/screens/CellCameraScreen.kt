package com.sloth.registerapp.presentation.video.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.view.SurfaceHolder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedro.library.view.OpenGlView
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.features.detection.data.ml.DetectionBox
import com.sloth.registerapp.features.detection.data.ml.PersonDetector
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.features.streaming.data.PhoneRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.presentation.video.components.StreamingControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import io.sentry.SentryLevel

@Composable
fun CellCameraScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val objectDetectionRepo = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val detectionEnabledPersisted by objectDetectionRepo.detectionEnabled.collectAsStateWithLifecycle(initialValue = false)
    val detectionModelId by objectDetectionRepo.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )

    val openGlView = remember { OpenGlView(context).apply { holder.setFormat(PixelFormat.TRANSLUCENT) } }
    val streamer = remember { PhoneRtmpStreamer(openGlView, rtmpUrl) }
    val streamState by streamer.state.collectAsStateWithLifecycle()
    val pixelCopyThread = remember { HandlerThread("cell-camera-pixel-copy").apply { start() } }
    val pixelCopyHandler = remember(pixelCopyThread) { Handler(pixelCopyThread.looper) }
    val personDetector = remember(context) {
        runCatching {
            if (PersonDetector.isModelAvailable(context)) PersonDetector(context) else null
        }.getOrNull()
    }

    var detectionBoxes by remember { mutableStateOf<List<DetectionBox>>(emptyList()) }
    var lastInferenceMs by remember { mutableStateOf<Long?>(null) }
    var lastDetectionError by remember { mutableStateOf<String?>(null) }
    var lastDetectionFailureReportAt by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        AppObservability.screenOpened("CellCameraScreen")
    }

    LaunchedEffect(rtmpUrl) {
        streamer.updateUrl(rtmpUrl)
        if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
            streamer.stop()
            streamer.start()
        }
    }

    DisposableEffect(Unit) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            if (previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    DisposableEffect(openGlView) {
        val callback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                streamer.startPreview()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                streamer.stopPreview()
            }
        }
        openGlView.holder.addCallback(callback)
        onDispose {
            openGlView.holder.removeCallback(callback)
            personDetector?.close()
            pixelCopyThread.quitSafely()
            streamer.release()
        }
    }

    LaunchedEffect(detectionEnabledPersisted, detectionModelId, personDetector, activity) {
        val canDetect = detectionEnabledPersisted &&
            detectionModelId == ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID &&
            personDetector != null &&
            activity != null

        if (!canDetect) {
            detectionBoxes = emptyList()
            if (!detectionEnabledPersisted) {
                lastDetectionError = null
            } else if (personDetector == null) {
                AppObservability.breadcrumb(
                    category = "ml",
                    message = "Cell camera detection requested without available model",
                    level = SentryLevel.WARNING
                )
            }
            return@LaunchedEffect
        }

        AppObservability.breadcrumb(
            category = "ml",
            message = "Cell camera object detection started",
            data = mapOf("model_id" to detectionModelId)
        )

        while (isActive) {
            val frameBitmap = captureOpenGlBitmap(
                activity = activity,
                view = openGlView,
                handler = pixelCopyHandler
            )

            if (frameBitmap == null) {
                detectionBoxes = emptyList()
                lastDetectionError = "Falha ao capturar frame da camera"
                AppObservability.breadcrumb(
                    category = "camera",
                    message = "Cell camera frame capture failed",
                    level = SentryLevel.WARNING
                )
                delay(400)
                continue
            }

            val inferenceStart = System.currentTimeMillis()
            lastDetectionError = null
            detectionBoxes = runCatching {
                withContext(Dispatchers.Default) {
                    personDetector.detect(frameBitmap)
                }
            }.getOrElse { error ->
                lastDetectionError = error.message ?: "Falha na inferencia"
                val now = System.currentTimeMillis()
                if (now - lastDetectionFailureReportAt > 60_000L) {
                    lastDetectionFailureReportAt = now
                    AppObservability.captureException(
                        throwable = error,
                        area = "ml",
                        operation = "cell_camera_person_detection",
                        extras = mapOf(
                            "bitmap_width" to frameBitmap.width,
                            "bitmap_height" to frameBitmap.height
                        )
                    )
                }
                emptyList()
            }
            lastInferenceMs = System.currentTimeMillis() - inferenceStart
            frameBitmap.recycle()
            delay(350)
        }
    }

    val aiColor = if (detectionEnabledPersisted && personDetector != null) Color(0xFF00C2FF)
                  else Color.White.copy(alpha = 0.55f)
    val inferenceMs = lastInferenceMs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Feed da câmera
        AndroidView(
            factory = { openGlView },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay de detecção
        if (detectionEnabledPersisted && detectionModelId == ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID) {
            CellDetectionOverlay(
                detections = detectionBoxes,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradiente superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
                    )
                )
        )

        // Gradiente inferior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.52f))
                    )
                )
        )

        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.28f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Câmera do Celular",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                val streamColor = when (streamState) {
                    StreamState.Streaming  -> Color(0xFF00E5A0)
                    StreamState.Connecting -> Color(0xFFFFC857)
                    is StreamState.Error   -> Color(0xFFFF6B6B)
                    StreamState.Idle       -> Color.White.copy(alpha = 0.38f)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(streamColor, CircleShape)
                    )
                    Text(
                        text = when (streamState) {
                            StreamState.Idle       -> "Pronto"
                            StreamState.Connecting -> "Conectando"
                            StreamState.Streaming  -> "Transmitindo"
                            is StreamState.Error   -> "Erro RTMP"
                        },
                        color = streamColor,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Espaço equivalente ao botão de voltar para centralizar o título
            Spacer(modifier = Modifier.size(40.dp))
        }

        // Botão de IA (canto superior direito, abaixo da barra)
        Surface(
            onClick = {
                val next = !detectionEnabledPersisted
                lastDetectionError = null
                detectionBoxes = emptyList()
                lastInferenceMs = null
                AppObservability.breadcrumb(
                    category = "ml",
                    message = if (next) "Cell camera detection enabled" else "Cell camera detection disabled",
                    data = mapOf("model_id" to ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID)
                )
                scope.launch {
                    objectDetectionRepo.setActiveModelId(ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID)
                    objectDetectionRepo.setDetectionEnabled(next)
                }
            },
            enabled = personDetector != null,
            shape = CircleShape,
            color = aiColor.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, aiColor.copy(alpha = 0.55f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 10.dp)
                .size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Alternar IA",
                    tint = aiColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Badge de status da IA (quando ligada)
        AnimatedVisibility(
            visible = detectionEnabledPersisted && personDetector != null,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF070E1B).copy(alpha = 0.82f),
                border = BorderStroke(1.dp, Color(0xFF00C2FF).copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Person Detector",
                        color = Color(0xFF00C2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val latencyColor = when {
                        inferenceMs == null  -> Color.White.copy(alpha = 0.40f)
                        inferenceMs < 80L    -> Color(0xFF00E5A0)
                        inferenceMs < 200L   -> Color(0xFFFFC857)
                        else                 -> Color(0xFFFF6B6B)
                    }
                    Text(
                        text = inferenceMs?.let { "${it} ms" } ?: "aguardando...",
                        color = latencyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!lastDetectionError.isNullOrBlank()) {
                        Text(
                            text = "erro na captura",
                            color = Color(0xFFFFC857),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Área inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badge com contagem de detecções
            AnimatedVisibility(
                visible = detectionEnabledPersisted && detectionBoxes.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val count = detectionBoxes.size
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00C2FF).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF00C2FF).copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00E5A0), CircleShape)
                        )
                        Text(
                            text = "$count pessoa${if (count != 1) "s" else ""} detectada${if (count != 1) "s" else ""}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            StreamingControl(
                state = streamState,
                onStart = { streamer.start() },
                onStop = { streamer.stop() }
            )
        }

        // Aviso se modelo não disponível
        if (personDetector == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF070E1B).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.30f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFFFFC857),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Modelo não encontrado",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "person_detector.tflite não está nos assets do app.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CellDetectionOverlay(
    detections: List<DetectionBox>,
    modifier: Modifier = Modifier
) {
    if (detections.isEmpty()) return

    val boxColor = Color(0xFF00E5A0)
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 34f
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }
    val badgePaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(210, 0, 194, 255)
        }
    }

    Canvas(modifier = modifier) {
        detections.forEach { detection ->
            val left = detection.normalizedRect.left * size.width
            val top = detection.normalizedRect.top * size.height
            val right = detection.normalizedRect.right * size.width
            val bottom = detection.normalizedRect.bottom * size.height

            drawRect(
                color = boxColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(
                    width = (right - left).coerceAtLeast(0f),
                    height = (bottom - top).coerceAtLeast(0f)
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            val label = "${detection.label} ${(detection.confidence * 100).toInt()}%"
            val labelWidth = labelPaint.measureText(label) + 28f
            val badgeTop = (top - 44f).coerceAtLeast(8f)
            val badgeRect = RectF(left, badgeTop, left + labelWidth, badgeTop + 36f)

            drawContext.canvas.nativeCanvas.apply {
                drawRoundRect(badgeRect, 14f, 14f, badgePaint)
                drawText(label, badgeRect.left + 14f, badgeRect.bottom - 11f, labelPaint)
            }
        }
    }
}

private suspend fun captureOpenGlBitmap(
    activity: Activity,
    view: OpenGlView,
    handler: Handler
): Bitmap? {
    if (view.width <= 0 || view.height <= 0) return null

    return withContext(Dispatchers.Main) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        suspendCancellableCoroutine { continuation ->
            PixelCopy.request(activity.window, rect, bitmap, { result ->
                if (result == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap)
                } else {
                    bitmap.recycle()
                    continuation.resume(null)
                }
            }, handler)
        }
    }
}
