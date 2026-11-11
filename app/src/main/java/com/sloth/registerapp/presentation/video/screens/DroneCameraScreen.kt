package com.sloth.registerapp.presentation.video.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Log
import android.widget.Toast
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Canvas
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.mission.ActiveMissionStatus
import com.sloth.registerapp.core.network.ConnectivityMonitor
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.core.settings.MediaStorageSettingsRepository
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.core.utils.MeasurementConverter
import com.sloth.registerapp.features.mldetection.domain.model.DetectionResult
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.presentation.MLDetectionViewModel
import com.sloth.registerapp.features.objectdetection.data.repository.ObjectDetectionPreferencesRepository
import com.sloth.registerapp.features.streaming.data.DjiRtmpStreamer
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.report.data.manager.MissionMediaManager
import com.sloth.registerapp.features.report.domain.model.MissionMediaSource
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSummary
import com.sloth.registerapp.presentation.video.components.VideoFeedView
import com.sloth.registerapp.presentation.mission.components.MapboxMiniMapView
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.sentry.SentryLevel

private data class DetectionModelOption(
    val id: String,
    val name: String,
    val summary: String,
    val classes: String,
    val installed: Boolean
)

private fun buildDetectionModelOptions(
    installedModels: List<MLModel>
): List<DetectionModelOption> {
    return installedModels.map { model ->
        DetectionModelOption(
            id = model.id,
            name = model.name,
            summary = if (model.source.name == "ASSET") {
                "Modelo embarcado no app e pronto para uso offline."
            } else {
                "Modelo local instalado dinamicamente no dispositivo."
            },
            classes = model.classes.joinToString().ifBlank { "Objeto" },
            installed = true
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DroneCameraScreen(
    droneController: DroneCommandManager,
    onCellCameraClick: () -> Unit,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onSurfaceTextureDestroyed: () -> Boolean,
    onBackClick: () -> Unit = {}
) {
    val tag = "DroneCameraScreen"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val colorScheme = MaterialTheme.colorScheme
    val missionManager = remember { DroneMissionManager(DJIConnectionHelper) }
    val connectivityMonitor = remember { ConnectivityMonitor.getInstance(context) }

    // Estados
    var visible by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showMissionOverlay by rememberSaveable { mutableStateOf(true) }
    var showAiSheet by rememberSaveable { mutableStateOf(false) }
    var detectionEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedDetectionModelId by rememberSaveable {
        mutableStateOf(ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID)
    }
    var previewCaptureError by remember { mutableStateOf<String?>(null) }

    val droneState by droneController.droneState.collectAsStateWithLifecycle()
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val product by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val isFeedAvailable = product != null
    val mlDetectionViewModel: MLDetectionViewModel = viewModel()

    val rtmpRepo = remember { RtmpSettingsRepository.getInstance(context) }
    val mediaStorageRepo = remember { MediaStorageSettingsRepository.getInstance(context) }
    val measurementRepo = remember { MeasurementSettingsRepository.getInstance(context) }
    val objectDetectionRepo = remember { ObjectDetectionPreferencesRepository.getInstance(context) }
    val mediaManager = remember { MissionMediaManager.getInstance(context) }
    val weatherModule = remember { WeatherModule.getInstance(context) }
    val activeMissionId by ActiveMissionSessionManager.activeMissionId.collectAsStateWithLifecycle()
    val activeMissionSession by ActiveMissionSessionManager.session.collectAsStateWithLifecycle()
    val hasValidatedInternet by connectivityMonitor.isConnected.collectAsStateWithLifecycle()
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsStateWithLifecycle(initialValue = RtmpSettingsRepository.DEFAULT_URL)
    val mediaStorageTarget by mediaStorageRepo.mediaStorageTarget.collectAsStateWithLifecycle(
        initialValue = MediaStorageSettingsRepository.TARGET_PHONE
    )
    val measurementSystem by measurementRepo.measurementSystem.collectAsStateWithLifecycle(
        initialValue = MeasurementSettingsRepository.SYSTEM_METRIC
    )
    val persistedDetectionEnabled by objectDetectionRepo.detectionEnabled.collectAsStateWithLifecycle(initialValue = false)
    val persistedDetectionModelId by objectDetectionRepo.activeModelId.collectAsStateWithLifecycle(
        initialValue = ObjectDetectionPreferencesRepository.DEFAULT_MODEL_ID
    )
    val availableModels by mlDetectionViewModel.availableModels.collectAsStateWithLifecycle()
    val activeDetectionModelId by mlDetectionViewModel.activeModelId.collectAsStateWithLifecycle()
    val detectionBoxes by mlDetectionViewModel.detectionBoxes.collectAsStateWithLifecycle()
    val lastInferenceMs by mlDetectionViewModel.lastInferenceMs.collectAsStateWithLifecycle()
    val lastDetectionError by mlDetectionViewModel.lastError.collectAsStateWithLifecycle()
    val downloadProgress by mlDetectionViewModel.downloadProgress.collectAsStateWithLifecycle()
    val rtmpStreamer = remember { DjiRtmpStreamer(rtmpUrl) }
    val streamState by rtmpStreamer.state.collectAsStateWithLifecycle()
    val previewEnabled = streamState !is StreamState.Streaming && streamState !is StreamState.Connecting

    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val operatorLocation = locationUiState.location
    val weatherQueryLocation = remember(telemetry.latitude, telemetry.longitude, operatorLocation) {
        resolveWeatherQueryLocation(
            telemetry = telemetry,
            operatorLocation = operatorLocation
        )
    }
    var weatherSummary by remember { mutableStateOf<WeatherSummary?>(null) }
    var lastWeatherRequestMs by remember { mutableLongStateOf(0L) }
    var lastPreviewCaptureBreadcrumbAt by remember { mutableLongStateOf(0L) }
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val codecManager = remember { mutableStateOf<DJICodecManager?>(null) }
    val surfaceRef = remember { mutableStateOf<SurfaceTexture?>(null) }
    val surfaceSize = remember { mutableStateOf(Pair(0, 0)) }
    val textureViewRef = remember { mutableStateOf<TextureView?>(null) }
    val detectionModelOptions = remember(availableModels) {
        buildDetectionModelOptions(installedModels = availableModels)
    }
    val videoDataListener = remember {
        VideoFeeder.VideoDataListener { videoBuffer, size ->
            Log.d(tag, "Video frame received: $size bytes")
            codecManager.value?.sendDataToDecoder(videoBuffer, size)
        }
    }

    fun attachPreview() {
        val surface = surfaceRef.value ?: return
        val (width, height) = surfaceSize.value
        if (width <= 0 || height <= 0) return
        if (codecManager.value != null) return
        codecManager.value = DJICodecManager(context, surface, width, height)
        VideoFeeder.getInstance().primaryVideoFeed?.addVideoDataListener(videoDataListener)
        Log.d(tag, "Preview attached")
    }

    fun detachPreview() {
        VideoFeeder.getInstance().primaryVideoFeed?.removeVideoDataListener(videoDataListener)
        codecManager.value?.cleanSurface()
        codecManager.value = null
        Log.d(tag, "Preview detached")
    }

    // Oculta barras do sistema (status/navigation) durante o video feed
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val view = activity?.window?.decorView
        if (window != null && view != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null && view != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        AppObservability.screenOpened("DroneCameraScreen")
        visible = true
        if (!locationPermissions.allPermissionsGranted) {
            AppObservability.breadcrumb(
                category = "permissions",
                message = "Drone camera requested location permissions",
                data = mapOf("screen" to "DroneCameraScreen")
            )
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(persistedDetectionEnabled, persistedDetectionModelId) {
        detectionEnabled = persistedDetectionEnabled
        selectedDetectionModelId = persistedDetectionModelId
        mlDetectionViewModel.selectModel(persistedDetectionModelId)
    }

    LaunchedEffect(activeDetectionModelId, availableModels) {
        val resolvedModelId = activeDetectionModelId ?: return@LaunchedEffect
        val selectedExists = availableModels.any { it.id == selectedDetectionModelId }
        if (selectedDetectionModelId != resolvedModelId && !selectedExists) {
            selectedDetectionModelId = resolvedModelId
            objectDetectionRepo.setActiveModelId(resolvedModelId)
        }
    }

    LaunchedEffect(rtmpUrl) {
        rtmpStreamer.updateUrl(rtmpUrl)
        if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
            AppObservability.breadcrumb(
                category = "streaming",
                message = "DJI RTMP URL changed while streaming; restarting stream"
            )
            rtmpStreamer.stop()
            rtmpStreamer.start()
        }
    }

    LaunchedEffect(previewEnabled) {
        if (previewEnabled) {
            attachPreview()
        } else {
            detachPreview()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
    }

    LaunchedEffect(activeMissionSession.hasMission) {
        if (activeMissionSession.hasMission) {
            showMissionOverlay = true
        }
    }

    LaunchedEffect(weatherQueryLocation, hasValidatedInternet) {
        val queryLocation = weatherQueryLocation ?: return@LaunchedEffect
        if (!hasValidatedInternet) {
            Log.d(tag, "Internet validada indisponível; ignorando atualização de clima")
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        if (now - lastWeatherRequestMs < WEATHER_MIN_REFRESH_MS) {
            return@LaunchedEffect
        }

        weatherSummary = runCatching {
            weatherModule.getWeatherSummaryUseCase(
                lat = queryLocation.latitude,
                lon = queryLocation.longitude,
                ttlMs = 60_000L
            )
        }.onFailure { error ->
            AppObservability.captureException(
                throwable = error,
                area = "weather",
                operation = "load_weather_summary",
                level = SentryLevel.WARNING,
                extras = mapOf("source" to if (operatorLocation != null) "operator_or_telemetry" else "telemetry")
            )
        }.getOrNull()
        lastWeatherRequestMs = now
    }

    LaunchedEffect(
        detectionEnabled,
        selectedDetectionModelId,
        activeDetectionModelId,
        previewEnabled,
        isFeedAvailable
    ) {
        val canDetect = detectionEnabled &&
            previewEnabled &&
            isFeedAvailable &&
            selectedDetectionModelId == activeDetectionModelId &&
            availableModels.any { it.id == selectedDetectionModelId }

        if (!canDetect) {
            mlDetectionViewModel.clearDetections()
            if (!detectionEnabled) {
                previewCaptureError = null
            } else {
                AppObservability.breadcrumb(
                    category = "ml",
                    message = "Drone camera detection waiting for prerequisites",
                    data = mapOf(
                        "preview_enabled" to previewEnabled,
                        "feed_available" to isFeedAvailable,
                        "selected_model_id" to selectedDetectionModelId,
                        "active_model_id" to activeDetectionModelId
                    )
                )
            }
            return@LaunchedEffect
        }

        AppObservability.breadcrumb(
            category = "ml",
            message = "Drone camera detection started",
            data = mapOf("model_id" to selectedDetectionModelId)
        )

        while (isActive) {
            val textureView = textureViewRef.value
            val frameBitmap = withContext(Dispatchers.Main) {
                textureView?.bitmap?.copy(Bitmap.Config.ARGB_8888, false)
            }

            if (frameBitmap == null) {
                previewCaptureError = "Preview indisponivel para captura"
                val now = System.currentTimeMillis()
                if (now - lastPreviewCaptureBreadcrumbAt > 30_000L) {
                    lastPreviewCaptureBreadcrumbAt = now
                    AppObservability.breadcrumb(
                        category = "camera",
                        message = "Drone preview frame unavailable",
                        level = SentryLevel.WARNING
                    )
                }
                delay(400)
                continue
            }

            previewCaptureError = null
            mlDetectionViewModel.processFrame(frameBitmap)
            delay(350)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        VideoFeedView(
            modifier = Modifier.fillMaxSize(),
            onTextureViewReady = { textureViewRef.value = it },
            onSurfaceTextureAvailable = { surface, width, height ->
                Log.d(tag, "Surface available: ${width}x${height}")
                surfaceRef.value = surface
                surfaceSize.value = Pair(width, height)
                if (previewEnabled) {
                    attachPreview()
                }
                onSurfaceTextureAvailable(surface, width, height)
            },
            onSurfaceTextureDestroyed = {
                Log.d(tag, "Surface destroyed")
                detachPreview()
                surfaceRef.value = null
                surfaceSize.value = Pair(0, 0)
                onSurfaceTextureDestroyed()
            }
        )

        if (detectionEnabled && detectionBoxes.isNotEmpty()) {
            PersonDetectionOverlay(
                detections = detectionBoxes,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isFeedAvailable) {
            Log.d(tag, "Video feed unavailable (product not connected)")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(
                        "Vídeo Feed Indisponível",
                        color = colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Conecte-se ao drone para visualizar",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Gradientes de leitura da HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                    )
                )
        )

        // Header fino estilo DJI
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { -it }) {
            CompactHeader(
                droneState = droneState,
                batteryLevel = telemetry.batteryLevel,
                onBackClick = onBackClick,
                onToggleControls = { showControls = !showControls }
            )
        }

        // HUD de telemetria linear (sem card)
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            TelemetryHudStrip(
                telemetry = telemetry,
                measurementSystem = measurementSystem
            )
        }

        weatherSummary?.let { summary ->
            WeatherCompactBadge(
                summary = summary,
                measurementSystem = measurementSystem,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 58.dp)
            )
        }

        AnimatedVisibility(
            visible = activeMissionSession.hasMission,
            enter = fadeIn() + slideInHorizontally { it / 3 },
            exit = fadeOut() + slideOutHorizontally { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 72.dp)
        ) {
            AnimatedVisibility(
                visible = showMissionOverlay,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 }
            ) {
                MissionExecutionOverlay(
                    missionName = activeMissionSession.missionName ?: "Missao pronta",
                    missionStatus = activeMissionSession.status,
                    onStartClick = {
                        scope.launch {
                            runCatching { missionManager.startMission() }
                                .onSuccess {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.EXECUTING)
                                    Toast.makeText(context, "Missão iniciada", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                                    Toast.makeText(
                                        context,
                                        "Erro ao iniciar missão: ${it.message ?: "desconhecido"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    onPauseClick = {
                        scope.launch {
                            runCatching { missionManager.pauseMission() }
                                .onSuccess {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.PAUSED)
                                    Toast.makeText(context, "Missão pausada", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                                    Toast.makeText(
                                        context,
                                        "Erro ao pausar missão: ${it.message ?: "desconhecido"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    onStopClick = {
                        scope.launch {
                            runCatching { missionManager.stopMission() }
                                .onSuccess {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.STOPPED)
                                    Toast.makeText(context, "Missão parada", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                                    Toast.makeText(
                                        context,
                                        "Erro ao parar missão: ${it.message ?: "desconhecido"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                )
            }
        }

        // Mini mapa no canto inferior esquerdo
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    modifier = Modifier
                        .size(width = 164.dp, height = 118.dp)
                        .border(1.dp, colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (operatorLocation != null) {
                            MapboxMiniMapView(
                                modifier = Modifier.fillMaxSize(),
                                operatorLocation = operatorLocation,
                                operatorAccuracyMeters = locationUiState.accuracyMeters,
                                operatorIsStationary = locationUiState.isStationary,
                                styleUri = Style.STANDARD
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = locationUiState.errorMessage ?: "Aguardando GPS...",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Controles de câmera
        AnimatedVisibility(
            visible = showControls && visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 10.dp)
        ) {
            CameraControls(
                isRecording = isRecording,
                streamState = streamState,
                hasMission = activeMissionSession.hasMission,
                missionExpanded = showMissionOverlay,
                detectionEnabled = detectionEnabled,
                onCellCameraClick = onCellCameraClick,
                onTakePhotoClick = {
                    droneController.takePhoto { success, message ->
                        val text = if (success) "Foto capturada" else "Erro ao fotografar: ${message ?: "desconhecido"}"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        if (success && !activeMissionId.isNullOrBlank()) {
                            scope.launch {
                                val now = System.currentTimeMillis()
                                if (mediaStorageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) {
                                    mediaManager.registerPhotoCapture(
                                        missionId = activeMissionId!!,
                                        dronePath = "drone://photo/$now.jpg",
                                        source = MissionMediaSource.DRONE_SD
                                    )
                                } else {
                                    mediaManager.registerPhotoCapture(
                                        missionId = activeMissionId!!,
                                        localPath = "content://vantly/local/photo/$now.jpg",
                                        source = MissionMediaSource.PHONE_LOCAL
                                    )
                                }
                            }
                        }
                    }
                },
                onRecordClick = {
                    if (isRecording) {
                        droneController.stopRecording { success, message ->
                            if (success) {
                                isRecording = false
                            }
                            val text = if (success) "Gravação parada" else "Erro ao parar gravação: ${message ?: "desconhecido"}"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            if (success && !activeMissionId.isNullOrBlank()) {
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    if (mediaStorageTarget == MediaStorageSettingsRepository.TARGET_DRONE_SD) {
                                        mediaManager.registerVideoCapture(
                                            missionId = activeMissionId!!,
                                            dronePath = "drone://video/$now.mp4",
                                            source = MissionMediaSource.DRONE_SD
                                        )
                                    } else {
                                        mediaManager.registerVideoCapture(
                                            missionId = activeMissionId!!,
                                            localPath = "content://vantly/local/video/$now.mp4",
                                            source = MissionMediaSource.PHONE_LOCAL
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        droneController.startRecording { success, message ->
                            if (success) {
                                isRecording = true
                            }
                            val text = if (success) "Gravação iniciada" else "Erro ao iniciar gravação: ${message ?: "desconhecido"}"
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onStreamToggle = {
                    if (!isFeedAvailable) {
                        Toast.makeText(context, "Conecte o drone para transmitir", Toast.LENGTH_SHORT).show()
                        return@CameraControls
                    }
                    if (streamState is StreamState.Streaming || streamState is StreamState.Connecting) {
                        rtmpStreamer.stop()
                    } else {
                        rtmpStreamer.start()
                    }
                },
                onMissionToggle = {
                    showMissionOverlay = !showMissionOverlay
                },
                onAiToggle = {
                    showAiSheet = true
                }
            )
        }

        if (!previewEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(12.dp)
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Preview desativado durante transmissão",
                    color = colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }

    }

    if (showAiSheet) {
        DetectionModelPickerSheet(
            detectionEnabled = detectionEnabled,
            selectedModelId = selectedDetectionModelId,
            models = detectionModelOptions,
            lastInferenceMs = lastInferenceMs,
            lastError = previewCaptureError ?: lastDetectionError,
            downloadProgress = downloadProgress,
            onDismiss = { showAiSheet = false },
            onDetectionEnabledChange = {
                detectionEnabled = it
                scope.launch {
                    objectDetectionRepo.setDetectionEnabled(it)
                }
            },
            onModelSelected = {
                selectedDetectionModelId = it
                mlDetectionViewModel.selectModel(it)
                scope.launch {
                    objectDetectionRepo.setActiveModelId(it)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            rtmpStreamer.release()
            droneController.stop()
            missionManager.destroy()
        }
    }
}

private const val WEATHER_MIN_REFRESH_MS = 60_000L

private data class WeatherQueryLocation(
    val latitude: Double,
    val longitude: Double,
    val source: WeatherLocationSource
)

private enum class WeatherLocationSource {
    DRONE_TELEMETRY,
    OPERATOR_GPS
}

private fun resolveWeatherQueryLocation(
    telemetry: DroneTelemetry,
    operatorLocation: Point?
): WeatherQueryLocation? {
    val telemetryLatitude = telemetry.latitude
    val telemetryLongitude = telemetry.longitude
    if (isValidCoordinate(telemetryLatitude, telemetryLongitude)) {
        return WeatherQueryLocation(
            latitude = telemetryLatitude,
            longitude = telemetryLongitude,
            source = WeatherLocationSource.DRONE_TELEMETRY
        )
    }

    val fallbackLocation = operatorLocation ?: return null
    val fallbackLatitude = fallbackLocation.latitude()
    val fallbackLongitude = fallbackLocation.longitude()
    if (!isValidCoordinate(fallbackLatitude, fallbackLongitude)) {
        return null
    }

    return WeatherQueryLocation(
        latitude = fallbackLatitude,
        longitude = fallbackLongitude,
        source = WeatherLocationSource.OPERATOR_GPS
    )
}

private fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
    return latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
}

// ===============================================
//               SUB-COMPONENTES
// ===============================================

@Composable
private fun MissionPanelToggleButton(
    expanded: Boolean,
    hasMission: Boolean,
    onClick: () -> Unit
) {
    CompactControlButton(
        icon = if (expanded && hasMission) Icons.Default.VisibilityOff else Icons.Default.Route,
        onClick = onClick,
        enabled = true,
        color = if (hasMission) Color(0xFF00E5A0) else Color.White.copy(alpha = 0.70f),
        size = 38.dp
    )
}

@Composable
private fun MissionExecutionOverlay(
    missionName: String,
    missionStatus: ActiveMissionStatus,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val (statusLabel, statusColor) = when (missionStatus) {
        ActiveMissionStatus.LOADING   -> "Carregando"     to Color(0xFFFFC857)
        ActiveMissionStatus.READY     -> "Pronta"         to Color(0xFF00E5A0)
        ActiveMissionStatus.EXECUTING -> "Em execução"    to Color(0xFF00C2FF)
        ActiveMissionStatus.PAUSED    -> "Pausada"        to Color(0xFFFFC857)
        ActiveMissionStatus.STOPPED   -> "Interrompida"   to Color(0xFFFF6B6B)
        ActiveMissionStatus.COMPLETED -> "Concluída"      to Color(0xFF00E5A0)
        ActiveMissionStatus.ERROR     -> "Falha"          to Color(0xFFFF6B6B)
        ActiveMissionStatus.IDLE      -> "Aguardando"     to Color.White.copy(alpha = 0.5f)
    }

    val isPulsing = missionStatus == ActiveMissionStatus.EXECUTING
    val infiniteTransition = rememberInfiniteTransition(label = "mission_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = Color(0xFF080F1C).copy(alpha = 0.82f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .width(192.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = statusColor.copy(alpha = if (isPulsing) pulseAlpha else 0.9f),
                            shape = CircleShape
                        )
                )
                Text(
                    text = missionName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = statusColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (missionStatus) {
                    ActiveMissionStatus.READY,
                    ActiveMissionStatus.PAUSED,
                    ActiveMissionStatus.STOPPED -> {
                        MissionOverlayButton(
                            icon = Icons.Default.PlayArrow,
                            accent = Color(0xFF00E5A0),
                            modifier = Modifier.weight(1f),
                            onClick = onStartClick
                        )
                    }
                    ActiveMissionStatus.EXECUTING -> {
                        MissionOverlayButton(
                            icon = Icons.Default.Pause,
                            accent = Color(0xFFFFC857),
                            modifier = Modifier.weight(1f),
                            onClick = onPauseClick
                        )
                    }
                    else -> Unit
                }

                if (missionStatus == ActiveMissionStatus.EXECUTING ||
                    missionStatus == ActiveMissionStatus.PAUSED
                ) {
                    MissionOverlayButton(
                        icon = Icons.Default.Stop,
                        accent = Color(0xFFFF6B6B),
                        modifier = if (missionStatus == ActiveMissionStatus.EXECUTING ||
                            missionStatus == ActiveMissionStatus.PAUSED
                        ) Modifier.weight(1f) else Modifier,
                        onClick = onStopClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionOverlayButton(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun CompactHeader(
    droneState: DroneState,
    batteryLevel: Int,
    onBackClick: () -> Unit,
    onToggleControls: () -> Unit
) {
    val statusText = when (droneState) {
        DroneState.ON_GROUND -> "Pronto"
        DroneState.IN_AIR -> "No ar"
        DroneState.TAKING_OFF -> "Decolando"
        DroneState.LANDING -> "Pousando"
        DroneState.DISCONNECTED -> "Desconectado"
        DroneState.EMERGENCY_STOP -> "Emergência"
        DroneState.GOING_HOME -> "Retornando"
        DroneState.ERROR -> "Erro"
    }
    val colorScheme = MaterialTheme.colorScheme
    val safeBattery = batteryLevel.coerceIn(0, 100)
    val batteryColor = when {
        safeBattery > 50 -> colorScheme.secondary
        safeBattery > 20 -> colorScheme.tertiary
        else -> colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = statusText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${safeBattery}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(Icons.Default.BatteryFull, null, tint = batteryColor, modifier = Modifier.size(18.dp))
        }
        IconButton(
            onClick = onToggleControls,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.25f), CircleShape)
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Controles",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CameraControls(
    isRecording: Boolean,
    streamState: StreamState,
    hasMission: Boolean,
    missionExpanded: Boolean,
    detectionEnabled: Boolean,
    onCellCameraClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onStreamToggle: () -> Unit,
    onMissionToggle: () -> Unit,
    onAiToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val buttonGray = colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactControlButton(
            icon = Icons.Default.CameraAlt,
            onClick = onTakePhotoClick,
            enabled = true,
            color = buttonGray
        )
        SmallStreamingButton(
            streamState = streamState,
            onToggle = onStreamToggle,
            color = buttonGray
        )
        CompactControlButton(
            icon = Icons.Default.Videocam,
            onClick = onRecordClick,
            enabled = true,
            color = if (isRecording) colorScheme.error else buttonGray
        )
        CompactControlButton(
            icon = Icons.Default.PhoneAndroid,
            onClick = onCellCameraClick,
            enabled = true,
            color = buttonGray
        )
        AiPanelButton(
            enabled = detectionEnabled,
            onClick = onAiToggle
        )
        MissionPanelToggleButton(
            expanded = missionExpanded,
            hasMission = hasMission,
            onClick = onMissionToggle
        )
    }
}

@Composable
private fun AiPanelButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    CompactControlButton(
        icon = Icons.Default.Psychology,
        onClick = onClick,
        enabled = true,
        color = if (enabled) Color(0xFF00C2FF) else Color.White.copy(alpha = 0.70f),
        size = 38.dp
    )
}

@Composable
private fun PersonDetectionOverlay(
    detections: List<DetectionResult>,
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
            val badgeRect = RectF(
                left,
                badgeTop,
                left + labelWidth,
                badgeTop + 36f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawRoundRect(badgeRect, 14f, 14f, badgePaint)
                drawText(label, badgeRect.left + 14f, badgeRect.bottom - 11f, labelPaint)
            }
        }
    }
}

@Composable
fun TelemetryHudStrip(
    telemetry: DroneTelemetry,
    measurementSystem: String
) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudMetric(
            icon = Icons.Default.Height,
            value = MeasurementConverter.formatAltitude(telemetry.altitude, measurementSystem)
        )
        HudMetric(
            icon = Icons.Default.Speed,
            value = MeasurementConverter.formatSpeed(telemetry.speed, measurementSystem)
        )
        HudMetric(
            icon = Icons.Default.TripOrigin,
            value = MeasurementConverter.formatDistance(telemetry.distanceFromHome, measurementSystem)
        )
        HudMetric(icon = Icons.Default.GpsFixed, value = "${telemetry.gpsSatellites}")
    }
}

@Composable
fun WeatherCompactBadge(
    summary: WeatherSummary,
    measurementSystem: String,
    modifier: Modifier = Modifier
) {
    val tint = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Color(0xFF22C55E)
        WeatherSafetyLevel.CAUTION -> Color(0xFFF59E0B)
        WeatherSafetyLevel.NO_FLY -> Color(0xFFEF4444)
    }

    val icon = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Icons.Default.WbSunny
        WeatherSafetyLevel.CAUTION -> Icons.Default.Cloud
        WeatherSafetyLevel.NO_FLY -> Icons.Default.Thunderstorm
    }

    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(
            MeasurementConverter.formatSpeed((summary.snapshot.windSpeedMs ?: 0.0).toFloat(), measurementSystem),
            color = Color.White,
            fontSize = 11.sp
        )
        Text(
            "Clima favorável para voo",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectionModelPickerSheet(
    detectionEnabled: Boolean,
    selectedModelId: String,
    models: List<DetectionModelOption>,
    lastInferenceMs: Long?,
    lastError: String?,
    downloadProgress: Float?,
    onDismiss: () -> Unit,
    onDetectionEnabledChange: (Boolean) -> Unit,
    onModelSelected: (String) -> Unit
) {
    val selectedModel = models.firstOrNull { it.id == selectedModelId }
    val canEnableDetection = selectedModel?.installed == true

    val AiBlue   = Color(0xFF00C2FF)
    val AiGreen  = Color(0xFF00E5A0)
    val AiAmber  = Color(0xFFFFC857)
    val AiRed    = Color(0xFFFF6B6B)
    val SheetBg  = Color(0xFF070E1B)
    val CardBg   = Color(0xFF0C1626)
    val Divider  = Color.White.copy(alpha = 0.07f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AiBlue.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AiBlue,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Detecção de objetos",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pipeline YOLO em tempo real",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = detectionEnabled,
                    onCheckedChange = onDetectionEnabledChange,
                    enabled = canEnableDetection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AiBlue,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))

            // Status / telemetry row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = BorderStroke(1.dp, Divider)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiMetricChip(
                            label = "Modelo",
                            value = selectedModel?.name ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        AiMetricChip(
                            label = "Latência",
                            value = lastInferenceMs?.let { "${it} ms" } ?: "—",
                            valueColor = when {
                                lastInferenceMs == null -> Color.White.copy(alpha = 0.7f)
                                lastInferenceMs < 80   -> AiGreen
                                lastInferenceMs < 200  -> AiAmber
                                else                   -> AiRed
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (downloadProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Baixando modelo...",
                                    color = AiBlue,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    color = AiBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = AiBlue,
                                trackColor = AiBlue.copy(alpha = 0.15f)
                            )
                        }
                    }

                    if (!lastError.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = AiAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = lastError,
                                color = AiAmber,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (!canEnableDetection && models.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AiAmber.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AiAmber.copy(alpha = 0.20f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AiAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Nenhum modelo YOLO disponível. Embarque um modelo no APK ou baixe via servidor.",
                            color = AiAmber.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Model list
            if (models.isNotEmpty()) {
                Text(
                    text = "MODELOS INSTALADOS",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                models.forEach { model ->
                    val isSelected = selectedModelId == model.id
                    val accent = if (isSelected) AiBlue else Color.White.copy(alpha = 0.10f)

                    Surface(
                        onClick = { if (model.installed) onModelSelected(model.id) },
                        enabled = model.installed,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) AiBlue.copy(alpha = 0.06f) else CardBg,
                        border = BorderStroke(1.dp, accent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isSelected) {
                                Surface(shape = CircleShape, color = AiBlue.copy(alpha = 0.12f)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AiBlue,
                                        modifier = Modifier.padding(6.dp).size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.White.copy(alpha = 0.06f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = model.classes,
                                    color = Color.White.copy(alpha = 0.50f),
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AiBlue.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "ATIVO",
                                        color = AiBlue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White.copy(alpha = 0.85f)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, color = Color.White.copy(alpha = 0.40f), fontSize = 10.sp)
            Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HudMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SmallStreamingButton(
    streamState: StreamState,
    onToggle: () -> Unit,
    color: Color
) {
    val isConnecting = streamState is StreamState.Connecting
    val isStreaming = streamState is StreamState.Streaming
    val icon = when {
        isConnecting -> Icons.Default.HourglassTop
        isStreaming -> Icons.Default.StopCircle
        else -> Icons.Default.WifiTethering
    }
    val tint = when {
        isStreaming -> MaterialTheme.colorScheme.error
        else -> color
    }
    CompactControlButton(
        icon = icon,
        onClick = onToggle,
        enabled = !isConnecting,
        color = tint,
        size = 36.dp
    )
}

@Composable
fun EmergencyStopButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(colorScheme.error, CircleShape)
            .shadow(8.dp, CircleShape)
    ) {
        Icon(Icons.Default.Warning, "Emergência", tint = colorScheme.onError, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun FlightActionControls(
    modifier: Modifier = Modifier,
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactControlButton(
            icon = Icons.Default.FlightTakeoff,
            onClick = onTakeoffClick,
            enabled = droneState == DroneState.ON_GROUND,
            color = colorScheme.secondary,
            size = 44.dp
        )
        CompactControlButton(
            icon = Icons.Default.FlightLand,
            onClick = onLandClick,
            enabled = droneState == DroneState.IN_AIR,
            color = colorScheme.tertiary,
            size = 44.dp
        )
    }
}

@Composable
fun VerticalMovementControls(
    enabled: Boolean,
    onMoveUpStart: () -> Unit,
    onMoveDownStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowUp,
            enabled = enabled,
            onPressStart = onMoveUpStart,
            onPressEnd = onStop
        )
        HoldToMoveButton(
            icon = Icons.Default.KeyboardArrowDown,
            enabled = enabled,
            onPressStart = onMoveDownStart,
            onPressEnd = onStop
        )
    }
}

@Composable
fun HoldToMoveButton(
    icon: ImageVector,
    enabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .size(46.dp)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        coroutineScope {
                            val repeatJob = launch {
                                while (isActive) {
                                    onPressStart()
                                    delay(300)
                                }
                            }
                            try {
                                tryAwaitRelease()
                            } finally {
                                repeatJob.cancel()
                                onPressEnd()
                            }
                        }
                    }
                )
            },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CompactControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    size: Dp = 36.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = { if (enabled) onClick() },
        shape = CircleShape,
        color = if (enabled) color.copy(alpha = 0.2f) else colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.5f) else colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.size(size),
        shadowElevation = if(enabled) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) color else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
