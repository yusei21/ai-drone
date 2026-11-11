package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.dsl.cameraOptions
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.presentation.mission.components.MapboxMapView
import com.sloth.registerapp.presentation.mission.model.Waypoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private val MissionSurface    = NeuralColors.SurfaceOverlay          // semi-transparente para overlay no mapa
private val MissionSurfaceAlt = NeuralColors.Bg.copy(alpha = 0.95f)
private val MissionPrimary    = NeuralColors.Primary
private val MissionSuccess    = NeuralColors.Success
private val MissionWarning    = NeuralColors.Warning
private val MissionDanger     = NeuralColors.Danger
private val MissionMuted      = NeuralColors.Muted
private val MissionBorder     = NeuralColors.Border

enum class MissionStatus {
    IDLE,
    LOADING,
    READY,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionControlScreen(
    missionName: String = "Missão Alpha",
    missionStatus: MissionStatus = MissionStatus.READY,
    droneState: DroneState = DroneState.ON_GROUND,
    currentLocation: Point = Point.fromLngLat(-44.3025, -2.5307),
    droneLocation: Point? = null,
    waypoints: List<Waypoint> = emptyList(),
    altitude: String = "0m",
    speed: String = "0 m/s",
    battery: Int = 100,
    gpsSignal: Int = 12,
    errorMessage: String? = null,
    onBackClick: () -> Unit = {},
    onUploadMission: () -> Unit = {},
    onStartMission: () -> Unit = {},
    onPauseMission: () -> Unit = {},
    onResumeMission: () -> Unit = {},
    onStopMission: () -> Unit = {},
    onTakeoffClick: () -> Unit = {},
    onLandClick: () -> Unit = {},
    onMoveUpStart: () -> Unit = {},
    onMoveDownStart: () -> Unit = {},
    onMoveStop: () -> Unit = {},
    onEmergencyStop: () -> Unit = {},
    onErrorDismiss: () -> Unit = {}
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            val insetsController = WindowCompat.getInsetsController(it, view)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var selectedWaypoint by rememberSaveable { mutableIntStateOf(0) }
    var configPanelOpen by rememberSaveable { mutableStateOf(false) }
    var waypointPanelOpen by rememberSaveable { mutableStateOf(false) }
    var showStopDialog by rememberSaveable { mutableStateOf(false) }

    val totalDistanceKm = remember(waypoints) { calculateRouteDistanceKm(waypoints) }
    val estimatedMinutes = remember(waypoints, speed) { estimateMissionMinutes(totalDistanceKm, speed, waypoints.size) }
    val mapCenter = remember(waypoints, currentLocation) { calculateMapCenter(waypoints, currentLocation) }
    val selectedWaypointData = waypoints.getOrNull(selectedWaypoint)
    val primaryAction = missionPrimaryAction(missionStatus, onStartMission, onResumeMission)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MapboxMapView(
            modifier = Modifier.fillMaxSize(),
            waypoints = waypoints,
            pointOfInterest = droneLocation ?: currentLocation,
            selectedWaypointIndex = selectedWaypoint.takeIf { it in waypoints.indices },
            primaryColor = MissionPrimary,
            onMapReady = { map ->
                val framingPoints = buildList {
                    add(currentLocation)
                    droneLocation?.let(::add)
                    addAll(waypoints.map { Point.fromLngLat(it.longitude, it.latitude) })
                }.distinctBy { "${it.longitude()}_${it.latitude()}" }

                if (framingPoints.size > 1) {
                    map.cameraForCoordinates(
                        framingPoints,
                        camera = cameraOptions { },
                        coordinatesPadding = EdgeInsets(84.0, 84.0, 84.0, 84.0),
                        maxZoom = 17.2,
                        offset = null
                    ) { fittedCamera ->
                        map.setCamera(fittedCamera)
                    }
                } else {
                    map.setCamera(
                        CameraOptions.Builder()
                            .center(mapCenter)
                            .zoom(16.6)
                            .build()
                    )
                }
            }
        )

        CompactMissionHeader(
            missionName = missionName,
            missionStatus = missionStatus,
            waypointCount = waypoints.size,
            gpsSignal = gpsSignal,
            battery = battery,
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )

        LeftActionRail(
            configPanelOpen = configPanelOpen,
            waypointPanelOpen = waypointPanelOpen,
            onToggleConfig = {
                configPanelOpen = !configPanelOpen
                if (configPanelOpen) waypointPanelOpen = false
            },
            onToggleWaypoint = {
                waypointPanelOpen = !waypointPanelOpen
                if (waypointPanelOpen) configPanelOpen = false
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )

        ManualFlightIconButton(
            icon = Icons.Default.Warning,
            tint = MissionDanger,
            enabled = true,
            onClick = onEmergencyStop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 18.dp)
        )

        BottomMissionDock(
            droneState = droneState,
            missionStatus = missionStatus,
            primaryAction = primaryAction,
            onTakeoffClick = onTakeoffClick,
            onLandClick = onLandClick,
            onMoveUpStart = onMoveUpStart,
            onMoveDownStart = onMoveDownStart,
            onMoveStop = onMoveStop,
            onUploadMission = onUploadMission,
            onPauseMission = onPauseMission,
            onAbortMission = { showStopDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        if (showStopDialog) {
            ConfirmDialog(
                title = "Abortar missão?",
                message = "Tem certeza que deseja abortar a missão atual?",
                confirmText = "Abortar",
                onConfirm = {
                    showStopDialog = false
                    onStopMission()
                },
                onDismiss = { showStopDialog = false },
                isDanger = true
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MissionDanger)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onErrorDismiss) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = configPanelOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CenterModalOverlay(onDismiss = { configPanelOpen = false }) {
                CenterPanel(
                    title = "Configurações DJI",
                    badge = missionStatusLabel(missionStatus),
                    badgeColor = missionStatusColor(missionStatus),
                    onClose = { configPanelOpen = false }
                ) {
                    ConfigPanelContent(
                        totalDistanceKm = totalDistanceKm,
                        estimatedMinutes = estimatedMinutes,
                        selectedWaypoint = selectedWaypointData
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = waypointPanelOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CenterModalOverlay(onDismiss = { waypointPanelOpen = false }) {
                CenterPanel(
                    title = "Waypoints",
                    badge = "${waypoints.size} pontos",
                    badgeColor = MissionPrimary,
                    onClose = { waypointPanelOpen = false }
                ) {
                    WaypointPanelContent(
                        waypoints = waypoints,
                        selectedWaypoint = selectedWaypoint,
                        onSelectWaypoint = {
                            selectedWaypoint = it
                            waypointPanelOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMissionHeader(
    missionName: String,
    missionStatus: MissionStatus,
    waypointCount: Int,
    gpsSignal: Int,
    battery: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 620.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(icon = Icons.Default.ArrowBack, tint = Color.White, onClick = onBackClick)
        Spacer(modifier = Modifier.size(10.dp))
        Surface(
            modifier = Modifier.weight(1f),
            color = MissionSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(missionStatusColor(missionStatus), CircleShape)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(missionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = "$waypointCount WPs · GPS $gpsSignal",
                        color = MissionMuted,
                        fontSize = 9.sp
                    )
                }
                Text(
                    text = "$battery%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun LeftActionRail(
    configPanelOpen: Boolean,
    waypointPanelOpen: Boolean,
    onToggleConfig: () -> Unit,
    onToggleWaypoint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassIconButton(
            icon = Icons.Default.Tune,
            tint = MissionPrimary,
            active = configPanelOpen,
            onClick = onToggleConfig
        )
        GlassIconButton(
            icon = Icons.Default.LocationOn,
            tint = MissionPrimary,
            active = waypointPanelOpen,
            onClick = onToggleWaypoint
        )
    }
}

@Composable
private fun CenterModalOverlay(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x8F1E232B))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.clickable(onClick = {})) {
            content()
        }
    }
}

@Composable
private fun CenterPanel(
    title: String,
    badge: String,
    badgeColor: Color,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 500.dp)
            .heightIn(max = 340.dp)
            .padding(horizontal = 18.dp),
        color = MissionSurfaceAlt,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MissionBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MissionPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MissionMuted, modifier = Modifier.size(16.dp))
                }
            }
            content()
        }
    }
}

@Composable
private fun ConfigPanelContent(
    totalDistanceKm: Double,
    estimatedMinutes: Int,
    selectedWaypoint: Waypoint?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryPill("Distância", "${String.format("%.1f", totalDistanceKm)} km", Modifier.weight(1f))
            SummaryPill("Duração", "~$estimatedMinutes min", Modifier.weight(1f))
        }
        if (selectedWaypoint != null) {
            Surface(
                color = MissionPrimary.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Waypoint selecionado", color = MissionMuted, fontSize = 11.sp)
                    MissionInfoRow("Ponto", "WP ${selectedWaypoint.id}")
                    MissionInfoRow("Altitude", "${selectedWaypoint.altitude.toInt()} m")
                    MissionInfoRow("Velocidade", "${selectedWaypoint.speed} m/s")
                }
            }
        }
    }
}

@Composable
private fun WaypointPanelContent(
    waypoints: List<Waypoint>,
    selectedWaypoint: Int,
    onSelectWaypoint: (Int) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(waypoints) { index, waypoint ->
            val isSelected = index == selectedWaypoint
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectWaypoint(index) },
                color = if (isSelected) MissionPrimary.copy(alpha = 0.10f) else Color(0xFF0A1628),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MissionPrimary.copy(alpha = 0.35f) else MissionBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(22.dp),
                            color = if (isSelected) MissionPrimary else Color(0xFF0D2040),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isSelected) Color.White else MissionMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "WP ${index + 1}",
                            color = if (isSelected) Color.White else Color(0xFF6A8AA8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    MissionWaypointDetail(Icons.Default.Height, "Alt", "${waypoint.altitude.toInt()} m", MissionSuccess)
                    MissionWaypointDetail(Icons.Default.Speed, "Vel", "${waypoint.speed} m/s", MissionPrimary)
                    MissionWaypointDetail(Icons.Default.Explore, "Coord", waypoint.latitude.formatShortCoord(), MissionWarning)
                }
            }
        }
    }
}

@Composable
private fun BottomMissionDock(
    droneState: DroneState,
    missionStatus: MissionStatus,
    primaryAction: MissionPrimaryAction,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit,
    onMoveUpStart: () -> Unit,
    onMoveDownStart: () -> Unit,
    onMoveStop: () -> Unit,
    onUploadMission: () -> Unit,
    onPauseMission: () -> Unit,
    onAbortMission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LeftFlightStack(
            droneState = droneState,
            onTakeoffClick = onTakeoffClick,
            onLandClick = onLandClick
        )

        Surface(
            modifier = Modifier
                .widthIn(max = 430.dp),
            color = Color.Black.copy(alpha = 0.82f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MissionBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MissionActionIcon(
                    icon = Icons.Default.Upload,
                    tint = MissionSuccess,
                    enabled = missionStatus == MissionStatus.READY || missionStatus == MissionStatus.ERROR,
                    onClick = onUploadMission
                )
                Button(
                    onClick = primaryAction.onClick,
                    enabled = primaryAction.enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAction.color,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(primaryAction.icon, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(primaryAction.label, fontWeight = FontWeight.Bold)
                }
                MissionActionIcon(
                    icon = Icons.Default.Pause,
                    tint = MissionWarning,
                    enabled = missionStatus == MissionStatus.RUNNING,
                    onClick = onPauseMission
                )
                MissionActionIcon(
                    icon = Icons.Default.Stop,
                    tint = MissionDanger,
                    enabled = missionStatus == MissionStatus.RUNNING || missionStatus == MissionStatus.PAUSED,
                    onClick = onAbortMission
                )
            }
        }

        RightMovementStack(
            droneState = droneState,
            onMoveUpStart = onMoveUpStart,
            onMoveDownStart = onMoveDownStart,
            onMoveStop = onMoveStop
        )
    }
}

@Composable
private fun MissionActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        color = if (enabled) tint.copy(alpha = 0.14f) else Color(0xFF0A1628),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) tint.copy(alpha = 0.35f) else MissionBorder),
        onClick = { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else MissionMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LeftFlightStack(
    droneState: DroneState,
    onTakeoffClick: () -> Unit,
    onLandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ManualFlightIconButton(
            icon = Icons.Default.FlightTakeoff,
            tint = MissionSuccess,
            enabled = droneState == DroneState.ON_GROUND,
            onClick = onTakeoffClick
        )
        ManualFlightIconButton(
            icon = Icons.Default.FlightLand,
            tint = MissionWarning,
            enabled = droneState == DroneState.IN_AIR,
            onClick = onLandClick
        )
    }
}

@Composable
private fun RightMovementStack(
    droneState: DroneState,
    onMoveUpStart: () -> Unit,
    onMoveDownStart: () -> Unit,
    onMoveStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HoldFlightIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            enabled = droneState == DroneState.IN_AIR,
            tint = MissionPrimary,
            onPressStart = onMoveUpStart,
            onPressEnd = onMoveStop
        )
        HoldFlightIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            enabled = droneState == DroneState.IN_AIR,
            tint = MissionPrimary,
            onPressStart = onMoveDownStart,
            onPressEnd = onMoveStop
        )
    }
}

@Composable
private fun ManualFlightIconButton(
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(44.dp),
        color = if (enabled) tint.copy(alpha = 0.20f) else Color(0x330A1628),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (enabled) tint.copy(alpha = 0.50f) else MissionBorder),
        onClick = { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else MissionMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HoldFlightIconButton(
    icon: ImageVector,
    enabled: Boolean,
    tint: Color,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
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
        color = if (enabled) tint.copy(alpha = 0.20f) else Color(0x330A1628),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (enabled) tint.copy(alpha = 0.50f) else MissionBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (enabled) tint else MissionMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0A1628),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MissionBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = MissionMuted, fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MissionInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MissionMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MissionWaypointDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(label, color = MissionMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(42.dp),
        color = if (active) tint.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (active) tint.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)
        ),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MissionSurfaceAlt,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = MissionMuted) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) MissionDanger else MissionPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MissionMuted)
            }
        }
    )
}

private data class MissionPrimaryAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val enabled: Boolean,
    val onClick: () -> Unit
)

private fun missionPrimaryAction(
    missionStatus: MissionStatus,
    onStartMission: () -> Unit,
    onResumeMission: () -> Unit
): MissionPrimaryAction {
    return when (missionStatus) {
        MissionStatus.IDLE,
        MissionStatus.READY -> MissionPrimaryAction("Iniciar missão", Icons.Default.PlayArrow, MissionPrimary, true, onStartMission)
        MissionStatus.LOADING -> MissionPrimaryAction("Carregando", Icons.Default.HourglassEmpty, MissionWarning, false, {})
        MissionStatus.PAUSED -> MissionPrimaryAction("Retomar missão", Icons.Default.PlayArrow, MissionPrimary, true, onResumeMission)
        MissionStatus.RUNNING -> MissionPrimaryAction("Em execução", Icons.Default.PlayArrow, MissionSuccess, false, {})
        MissionStatus.STOPPED,
        MissionStatus.ERROR -> MissionPrimaryAction("Tentar de novo", Icons.Default.Refresh, MissionPrimary, true, onStartMission)
        MissionStatus.COMPLETED -> MissionPrimaryAction("Concluída", Icons.Default.CheckCircle, MissionSuccess, false, {})
    }
}

private fun missionStatusColor(status: MissionStatus): Color {
    return when (status) {
        MissionStatus.IDLE -> Color.White.copy(alpha = 0.6f)
        MissionStatus.LOADING -> MissionWarning
        MissionStatus.READY -> MissionPrimary
        MissionStatus.RUNNING -> MissionSuccess
        MissionStatus.PAUSED -> MissionWarning
        MissionStatus.STOPPED -> MissionDanger
        MissionStatus.COMPLETED -> MissionSuccess
        MissionStatus.ERROR -> MissionDanger
    }
}

private fun missionStatusLabel(status: MissionStatus): String {
    return when (status) {
        MissionStatus.IDLE -> "IDLE"
        MissionStatus.LOADING -> "UPLOADING"
        MissionStatus.READY -> "READY"
        MissionStatus.RUNNING -> "EXECUTING"
        MissionStatus.PAUSED -> "PAUSED"
        MissionStatus.STOPPED -> "STOPPED"
        MissionStatus.COMPLETED -> "FINISHED"
        MissionStatus.ERROR -> "ERROR"
    }
}

private fun calculateMapCenter(waypoints: List<Waypoint>, currentLocation: Point): Point {
    if (waypoints.isEmpty()) return currentLocation
    return Point.fromLngLat(
        waypoints.map { it.longitude }.average(),
        waypoints.map { it.latitude }.average()
    )
}

private fun calculateRouteDistanceKm(waypoints: List<Waypoint>): Double {
    if (waypoints.size < 2) return 0.0
    var distanceMeters = 0.0
    for (index in 1 until waypoints.size) {
        val current = waypoints[index]
        val previous = waypoints[index - 1]
        val dx = (current.longitude - previous.longitude) * 111000 * cos(current.latitude * PI / 180)
        val dy = (current.latitude - previous.latitude) * 111000
        distanceMeters += sqrt(dx * dx + dy * dy)
    }
    return distanceMeters / 1000
}

private fun estimateMissionMinutes(distanceKm: Double, speed: String, waypointCount: Int): Int {
    val normalized = speed.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
    val speedMs = normalized.toDoubleOrNull()?.takeIf { it > 0 } ?: 8.0
    val travelMinutes = ((distanceKm * 1000) / speedMs) / 60
    return (travelMinutes + waypointCount * 0.35).toInt().coerceAtLeast(1)
}

private fun Double.formatShortCoord(): String = String.format("%.4f", this)
