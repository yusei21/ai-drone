package com.sloth.registerapp.presentation.mission.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.network.ConnectivityMonitor
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.runtime.RuntimeCapabilityResolver
import com.sloth.registerapp.core.ui.theme.RegisterAppTheme
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.mission.data.mapper.inDisplayOrder
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import com.sloth.registerapp.features.report.data.manager.FlightReportManager
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.presentation.mission.screens.MissionControlScreen
import com.sloth.registerapp.presentation.mission.screens.MissionStatus
import com.sloth.registerapp.presentation.mission.model.Waypoint
import com.sloth.registerapp.presentation.mission.viewmodels.DroneExecutionViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.DroneExecutionViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

class MissionControlActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MissionControlActivity"
    }

    private val viewModel: DroneExecutionViewModel by viewModels { DroneExecutionViewModelFactory() }
    private val droneController by lazy { DroneCommandManager() }
    private lateinit var flightReportManager: FlightReportManager
    private lateinit var weatherModule: WeatherModule
    private var weatherLat: Double? = null
    private var weatherLon: Double? = null
    private var reportStarted = false
    private var missionStartedAtMs: Long? = null
    private var initialBatteryLevel: Int? = null
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val connectivityMonitor by lazy { ConnectivityMonitor.getInstance(this) }
    private val runtimeCapabilityResolver by lazy { RuntimeCapabilityResolver(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val missionId = intent.getIntExtra("MISSION_ID", -1)
        if (missionId == -1) {
            loadErrorMessage.value = "ID da missão não informado."
        }
        flightReportManager = FlightReportManager(this)
        weatherModule = WeatherModule.getInstance(this)

        val missionRepository = MissionRepositoryImpl(
            context = this,
            apiService = RetrofitClient.getInstance(this),
            tokenRepository = TokenRepository.getInstance(this)
        )

        if (missionId != -1) {
            lifecycleScope.launch {
                val capabilitiesBeforeLoad = runtimeCapabilityResolver.resolve(missionId)
                val result = missionRepository.getMission(missionId)
                result.onSuccess { mission ->
                    if (mission != null) {
                        ActiveMissionSessionManager.startMissionSession(
                            missionId = missionId.toString(),
                            missionName = mission.name
                        )
                        weatherLat = mission.poi_latitude
                        weatherLon = mission.poi_longitude
                        viewModel.loadMission(mission)
                        loadErrorMessage.value = null
                    } else {
                        loadErrorMessage.value = if (!capabilitiesBeforeLoad.canOpenMissionControl) {
                            "Missão indisponível offline neste dispositivo. Conecte-se à internet para sincronizá-la."
                        } else {
                            "Missão não encontrada localmente nem no servidor."
                        }
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Falha ao carregar missão $missionId", error)
                    loadErrorMessage.value = when {
                        capabilitiesBeforeLoad.hasMissionCache ->
                            "A missão parece existir no cache, mas não pôde ser lida. Verifique integridade local."
                        !capabilitiesBeforeLoad.hasValidatedInternet ->
                            "Sem internet validada e sem cache local da missão neste dispositivo."
                        !capabilitiesBeforeLoad.hasAuthenticatedServerSession ->
                            "Sessão do servidor indisponível. Faça login novamente para carregar a missão do backend."
                        else ->
                            "Falha ao carregar missão do servidor. Tente novamente quando a conexão estabilizar."
                    }
                }
            }
        }

        setContent {
            RegisterAppTheme {
                val missionState = viewModel.missionState.collectAsStateWithLifecycle().value
                val missionData: ServerMissionDto? = viewModel.mission.collectAsStateWithLifecycle().value
                val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
                val loadError by loadErrorMessage.collectAsStateWithLifecycle()
                val droneState by droneController.droneState.collectAsStateWithLifecycle()
                val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
                val missionOutcome by viewModel.missionOutcome.collectAsStateWithLifecycle()

                androidx.compose.runtime.LaunchedEffect(missionState, missionData?.id) {
                    val mission = missionData ?: return@LaunchedEffect
                    when (missionState) {
                        MissionState.EXECUTING -> {
                            if (!reportStarted) {
                                startMissionReport(
                                    missionId = mission.id,
                                    mission = mission,
                                    telemetry = telemetry
                                )
                            }
                        }

                        MissionState.FINISHED,
                        MissionState.EXECUTION_STOPPED,
                        MissionState.ERROR -> finishMissionReport(
                            telemetry = telemetry,
                            missionOutcomeStatus = missionOutcome?.status?.name,
                            missionOutcomeMessage = missionOutcome?.message,
                            executionMode = missionOutcome?.executionMode?.name
                        )

                        else -> Unit
                    }
                }

                when {
                    missionData != null -> {
                        MissionControlScreen(
                            missionName = missionData.name,
                            missionStatus = missionState.toUiStatus(),
                            droneState = droneState,
                            currentLocation = com.mapbox.geojson.Point.fromLngLat(
                                missionData.poi_longitude,
                                missionData.poi_latitude
                            ),
                            waypoints = missionData.waypoints.inDisplayOrder().mapIndexed { index, waypoint ->
                                Waypoint(
                                    id = index + 1,
                                    latitude = waypoint.latitude,
                                    longitude = waypoint.longitude,
                                    altitude = waypoint.altitude,
                                    speed = missionData.auto_flight_speed
                                )
                            },
                            errorMessage = errorMessage,
                            onBackClick = { finish() },
                            onUploadMission = { viewModel.loadMission(missionData) },
                            onStartMission = { viewModel.startMission() },
                            onPauseMission = { viewModel.pauseMission() },
                            onResumeMission = { viewModel.resumeMission() },
                            onStopMission = { viewModel.stopMission() },
                            onTakeoffClick = { droneController.takeOff() },
                            onLandClick = { droneController.land() },
                            onMoveUpStart = { droneController.moveUp() },
                            onMoveDownStart = { droneController.moveDown() },
                            onMoveStop = { droneController.stopMovement() },
                            onEmergencyStop = { droneController.emergencyStop() },
                            onErrorDismiss = { viewModel.clearError() }
                        )
                    }
                    !loadError.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(loadError ?: "Falha ao carregar missão")
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Carregando missão...")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        finishMissionReport()
        droneController.stop()
        super.onDestroy()
    }

    private fun startMissionReport(
        missionId: Int,
        mission: ServerMissionDto,
        telemetry: DroneTelemetry
    ) {
        if (reportStarted) return
        missionStartedAtMs = System.currentTimeMillis()
        initialBatteryLevel = telemetry.batteryLevel.takeIf { it in 1..100 }
        lifecycleScope.launch {
            val startWeather = safeWeatherSnapshot(stage = "start")
            flightReportManager.startReport(
                missionName = mission.name,
                aircraftName = "Drone",
                startedAtMs = missionStartedAtMs ?: System.currentTimeMillis(),
                extraData = buildMap {
                    val orderedWaypoints = mission.waypoints.inDisplayOrder()
                    put("missionId", missionId.toString())
                    put("missionName", mission.name)
                    put("missionDate", formatTimestamp(missionStartedAtMs))
                    put("missionLocation", formatMissionLocation(mission))
                    put("missionObjective", inferMissionObjective(mission))
                    put("pilotName", "N/A")
                    put("droneModel", "Drone")
                    put("missionType", formatMissionType(mission.flight_path_mode))
                    put("executionMode", "UNKNOWN")
                    put("waypointCount", orderedWaypoints.size.toString())
                    put("coveredArea", "N/A")
                    put("batteryInitial", initialBatteryLevel?.toString() ?: "N/A")
                    put("batteryConsumed", "N/A")
                    put("batteryFinal", "N/A")
                    put("flightDistance", "N/A")
                    put("avgSpeed", "N/A")
                    put("maxSpeed", "N/A")
                    put("maxAltitude", "N/A")
                    put("consumptionPerMinute", "N/A")
                    put("alerts", "N/A")
                    put("windDirection", "N/A")
                    put("aiModel", "N/A")
                    put("aiType", "N/A")
                    put("aiObjectsDetected", "N/A")
                    put("aiPeopleDetected", "N/A")
                    put("aiVehiclesDetected", "N/A")
                    put("aiStructuresDetected", "N/A")
                    put("aiAverageConfidence", "N/A")
                    put("aiFalsePositives", "N/A")
                    put("aiFalseNegatives", "N/A")
                    put("aiProcessingTime", "N/A")
                    put("occurrences", "N/A")
                    put("weather_provider", startWeather?.snapshot?.providerId ?: "N/A")
                    put("weather_start_temperature_c", startWeather?.snapshot?.temperatureC?.toString() ?: "N/A")
                    put("weather_start_wind_ms", startWeather?.snapshot?.windSpeedMs?.toString() ?: "N/A")
                    put("weather_start_rain_mm", startWeather?.snapshot?.rainMm?.toString() ?: "N/A")
                    put("weather_start_lightning_risk", startWeather?.snapshot?.lightningRisk?.toString() ?: "N/A")
                    put("weather_start_safety_level", startWeather?.decision?.level?.name ?: "N/A")
                    put("weather_summary_message", startWeather?.decision?.shortMessage ?: "Clima não disponível")
                    orderedWaypoints.forEachIndexed { index, waypoint ->
                        val waypointNumber = index + 1
                        put("waypoint_${waypointNumber}_lat", waypoint.latitude.toCoordinateLabel())
                        put("waypoint_${waypointNumber}_lon", waypoint.longitude.toCoordinateLabel())
                        put("waypoint_${waypointNumber}_alt", "${waypoint.altitude.toCoordinateLabel()} m")
                        put("waypoint_${waypointNumber}_speed", "${mission.auto_flight_speed.toCoordinateLabel()} m/s")
                        put("waypoint_${waypointNumber}_action", waypoint.actions.toWaypointActionSummary())
                        put("waypoint_${waypointNumber}_dwell", "N/A")
                    }
                }
            )
            reportStarted = true
        }
    }

    private fun finishMissionReport(
        telemetry: DroneTelemetry? = null,
        missionOutcomeStatus: String? = null,
        missionOutcomeMessage: String? = null,
        executionMode: String? = null
    ) {
        if (!reportStarted) return
        reportStarted = false
        lifecycleScope.launch {
            val endWeather = if (connectivityMonitor.isConnectedNow()) {
                safeWeatherSnapshot(stage = "end")
            } else {
                null
            }
            val finalBattery = telemetry?.batteryLevel?.takeIf { it in 0..100 }
            val consumedBattery = if (initialBatteryLevel != null && finalBattery != null) {
                (initialBatteryLevel!! - finalBattery).coerceAtLeast(0)
            } else {
                null
            }
            val durationMinutes = missionStartedAtMs
                ?.let { ((System.currentTimeMillis() - it).coerceAtLeast(0L) / 60000.0).takeIf { minutes -> minutes > 0.0 } }
            val consumptionPerMinute = if (consumedBattery != null && durationMinutes != null) {
                String.format(java.util.Locale.US, "%.1f%%/min", consumedBattery / durationMinutes)
            } else {
                "N/A"
            }

            flightReportManager.updateExtraData(
                buildMap {
                    put("status", missionOutcomeStatus ?: "COMPLETED")
                    put("executionMode", executionMode ?: "UNKNOWN")
                    put("finalObservation", missionOutcomeMessage ?: "N/A")
                    put("batteryFinal", finalBattery?.toString() ?: "N/A")
                    put("batteryConsumed", consumedBattery?.toString() ?: "N/A")
                    put("altitudeAtEnd", telemetry?.altitude?.toDisplayMeters() ?: "N/A")
                    put("speedAtEnd", telemetry?.speed?.toDisplayMs() ?: "N/A")
                    put("flightDistance", telemetry?.distanceFromHome?.toDisplayMeters() ?: "N/A")
                    put("gpsSatellites", telemetry?.gpsSatellites?.toString() ?: "N/A")
                    put("consumptionPerMinute", consumptionPerMinute)
                    put("weather_end_temperature_c", endWeather?.snapshot?.temperatureC?.toString() ?: "N/A")
                    put("weather_end_wind_ms", endWeather?.snapshot?.windSpeedMs?.toString() ?: "N/A")
                    put("weather_end_rain_mm", endWeather?.snapshot?.rainMm?.toString() ?: "N/A")
                    put("weather_end_lightning_risk", endWeather?.snapshot?.lightningRisk?.toString() ?: "N/A")
                    put("weather_end_safety_level", endWeather?.decision?.level?.name ?: "N/A")
                    if (!connectivityMonitor.isConnectedNow()) {
                        put("weather_end_capture_skipped_reason", "offline_no_validated_internet")
                    }
                }
            )
            flightReportManager.finishReport(finalObservation = missionOutcomeMessage)
            missionStartedAtMs = null
            initialBatteryLevel = null
        }
    }

    private suspend fun safeWeatherSnapshot(stage: String) = runCatching {
        val capabilities = runtimeCapabilityResolver.resolve()
        if (!capabilities.canFetchWeather) {
            Log.d(TAG, "Internet validada indisponível; clima ignorado no $stage da missão")
            return@runCatching null
        }
        withTimeoutOrNull(1_500L) {
            val lat = weatherLat ?: return@withTimeoutOrNull null
            val lon = weatherLon ?: return@withTimeoutOrNull null
            withContext(Dispatchers.IO) {
                weatherModule.getWeatherSummaryUseCase(lat, lon, ttlMs = 60_000L)
            }
        }
    }.onFailure { error ->
        Log.w(TAG, "Falha ao obter clima no $stage da missão: ${error.message}", error)
    }.getOrNull()
        ?: run {
            Log.d(TAG, "Clima indisponível no $stage da missão; relatório seguirá sem esse dado")
            null
        }
}

private fun formatTimestamp(timestampMs: Long?): String {
    val safeTimestamp = timestampMs ?: return "N/A"
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
    return formatter.format(java.util.Date(safeTimestamp))
}

private fun formatMissionLocation(mission: ServerMissionDto): String {
    return "${String.format(java.util.Locale.US, "%.6f", mission.poi_latitude)}, " +
        "${String.format(java.util.Locale.US, "%.6f", mission.poi_longitude)}"
}

private fun inferMissionObjective(mission: ServerMissionDto): String {
    val normalized = mission.name.lowercase(java.util.Locale.ROOT)
    return when {
        "inspec" in normalized -> "Inspeção"
        "vigil" in normalized -> "Vigilância"
        "map" in normalized -> "Mapeamento"
        "entrega" in normalized -> "Entrega"
        "monitor" in normalized -> "Monitoramento"
        else -> "N/A"
    }
}

private fun formatMissionType(rawValue: String?): String {
    val normalized = rawValue?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty()
    return when {
        normalized.contains("waypoint") -> "Waypoint"
        normalized.contains("manual") -> "Manual"
        normalized.isBlank() -> "N/A"
        else -> rawValue ?: "N/A"
    }
}

private fun Float.toDisplayMeters(): String = String.format(java.util.Locale.US, "%.1f m", this)

private fun Float.toDisplayMs(): String = String.format(java.util.Locale.US, "%.1f m/s", this)

private fun Double.toCoordinateLabel(): String = String.format(java.util.Locale.US, "%.6f", this)

private fun List<Any>.toWaypointActionSummary(): String {
    if (isEmpty()) return "N/A"
    return joinToString(", ") { action ->
        action.toString().takeIf { it.isNotBlank() } ?: "Ação"
    }
}

fun MissionState.toUiStatus(): MissionStatus {
    return when (this) {
        MissionState.IDLE -> MissionStatus.IDLE

        // Agrupa todos os estados de carregamento/processamento
        MissionState.PREPARING,
        MissionState.DOWNLOADING,
        MissionState.UPLOADING ->
            MissionStatus.LOADING

        // Agrupa estados "prontos"
        MissionState.DOWNLOAD_FINISHED,
        MissionState.READY_TO_EXECUTE ->
            MissionStatus.READY

        MissionState.EXECUTING -> MissionStatus.RUNNING
        MissionState.EXECUTION_PAUSED -> MissionStatus.PAUSED
        MissionState.EXECUTION_STOPPED -> MissionStatus.STOPPED
        MissionState.FINISHED -> MissionStatus.COMPLETED
        MissionState.ERROR -> MissionStatus.ERROR
    }
}
