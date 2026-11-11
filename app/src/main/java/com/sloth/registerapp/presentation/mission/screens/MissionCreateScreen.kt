package com.sloth.registerapp.presentation.mission.screens

import android.Manifest
import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapbox.geojson.Point
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.remote.dto.WaypointDto
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.mission.domain.usecase.CreateMissionUseCase
import com.sloth.registerapp.presentation.mission.components.MapboxMapView
import com.sloth.registerapp.presentation.mission.model.Waypoint
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

private enum class SelectionTarget {
    POI,
    WAYPOINT
}

private val NeuralBackground = Color(0xFF050E1F)
private val NeuralSurface = Color(0xFF0A1628)
private val NeuralSurfaceAlt = Color(0xFF071120)
private val NeuralBorder = Color(0xFF0D2A50)
private val NeuralPrimary = Color(0xFF00C2FF)
private val NeuralSecondary = Color(0xFF0066FF)
private val NeuralTextMuted = Color(0xFF4A7FA5)

data class WaypointActionDraft(
    val type: String,
    val param: String = ""
)

data class MissionWaypointDraft(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: String = "20",
    val turnMode: String = "CLOCKWISE",
    val actions: List<WaypointActionDraft> = emptyList()
)

data class MissionData(
    val name: String = "",
    val pointOfInterest: Point? = null,
    val autoFlightSpeed: String = "",
    val maxFlightSpeed: String = "",
    val exitOnSignalLost: Boolean = true,
    val gimbalPitchRotationEnabled: Boolean = true,
    val repeatTimes: String = "",
    val finishedAction: String = "GO_HOME",
    val flightPathMode: String = "NORMAL",
    val gotoFirstWaypointMode: String = "SAFELY",
    val headingMode: String = "USING_WAYPOINT_HEADING",
    val waypoints: List<MissionWaypointDraft> = emptyList()
)

private data class LabeledOption(
    val value: String,
    val label: String
)

private val finishedActionOptions = listOf(
    LabeledOption("GO_HOME", "Retornar ao Inicio"),
    LabeledOption("GO_FIRST_WAYPOINT", "Ir para o Primeiro Waypoint"),
    LabeledOption("NO_ACTION", "Nenhuma acao"),
    LabeledOption("AUTO_LAND", "Pousar automaticamente")
)

private val flightPathOptions = listOf(
    LabeledOption("NORMAL", "Normal"),
    LabeledOption("CURVED", "Curvo")
)

private val gotoFirstWaypointOptions = listOf(
    LabeledOption("SAFELY", "Seguro"),
    LabeledOption("POINT_TO_POINT", "Direto")
)

private val headingModeOptions = listOf(
    LabeledOption("USING_WAYPOINT_HEADING", "Usar direcao do waypoint"),
    LabeledOption("TOWARD_POINT_OF_INTEREST", "Usar direcao do POI")
)

private val turnModeOptions = listOf(
    LabeledOption("CLOCKWISE", "Sentido horario"),
    LabeledOption("COUNTER_CLOCKWISE", "Sentido anti-horario")
)

private val waypointActionOptions = listOf(
    LabeledOption("START_TAKE_PHOTO", "Capturar foto"),
    LabeledOption("START_RECORD", "Iniciar gravacao"),
    LabeledOption("STOP_RECORD", "Parar gravacao"),
    LabeledOption("STAY", "Pausar"),
    LabeledOption("ROTATE_AIRCRAFT", "Rotacionar aeronave"),
    LabeledOption("GIMBAL_PITCH", "Inclinar gimbal"),
    LabeledOption("CAMERA_ZOOM", "Zoom da camera"),
    LabeledOption("CAMERA_FOCUS", "Foco da camera")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MissionCreateScreen(onBackClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var missionData by remember { mutableStateOf(MissionData()) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("A missao foi criada com sucesso.") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMapTouched by remember { mutableStateOf(false) }

    val createMissionUseCase = remember {
        CreateMissionUseCase(
            MissionRepositoryImpl(
                context,
                RetrofitClient.getInstance(context),
                TokenRepository.getInstance(context)
            )
        )
    }

    val canProceed = when (currentStep) {
        0 -> isConfigStepValid(missionData)
        1 -> missionData.pointOfInterest != null && missionData.waypoints.size >= 2
        else -> true
    }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NeuralBackground,
        topBar = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                color = NeuralBackground,
                shadowElevation = 6.dp
            ) {
                Column {
                    TopAppBar(
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = NeuralBackground,
                            navigationIconContentColor = Color.White
                        ),
                        title = {
                            Column {
                                Text(
                                    text = "Criar Missao",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(NeuralPrimary, NeuralSecondary)
                                        )
                                    )
                                )
                                Text(
                                    text = when (currentStep) {
                                        0 -> "Dados gerais"
                                        1 -> "Mapa e waypoints"
                                        else -> "Revisao"
                                    },
                                    fontSize = 13.sp,
                                    color = NeuralTextMuted
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                            }
                        }
                    )
                    StepIndicator(
                        currentStep = currentStep,
                        titles = listOf("Missao", "Waypoints", "Revisao"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = NeuralSurfaceAlt,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NeuralPrimary
                            )
                        ) {
                            Text("Voltar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                0, 1 -> currentStep += 1
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val missionDto = missionDataToServerDto(missionData)
                                            val result = withContext(Dispatchers.IO) {
                                                createMissionUseCase(missionDto)
                                            }
                                            result.onSuccess { createdMission ->
                                                successMessage = if (createdMission.id < 0) {
                                                    "Missao salva localmente. Ela sera enviada ao servidor quando houver conexao."
                                                } else {
                                                    "A missao \"${missionData.name}\" foi criada com sucesso."
                                                }
                                                showSuccessDialog = true
                                            }.onFailure { error ->
                                                errorMessage = when (error) {
                                                    is IllegalArgumentException -> error.message ?: "Dados invalidos"
                                                    is HttpException -> "Erro ${error.code()} ao salvar missao"
                                                    else -> error.message ?: "Erro ao salvar missao"
                                                }
                                            }
                                        } catch (error: Throwable) {
                                            errorMessage = error.message ?: "Erro ao salvar missao"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = canProceed && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStep == 2) NeuralSecondary else NeuralPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (currentStep) {
                                    0 -> "Continuar"
                                    1 -> "Revisar"
                                    else -> "Salvar"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeuralBackground,
                            Color(0xFF091A36),
                            NeuralBackground
                        )
                    )
                )
        ) {
            NeuralGridBackground()
            AnimatedContent(targetState = currentStep, label = "mission_create_step") { step ->
                when (step) {
                    0 -> ConfigStep(
                        data = missionData,
                        onChange = { missionData = it }
                    )
                    1 -> MapStep(
                        data = missionData,
                        onDataChange = { missionData = it },
                        initialMapCenter = locationUiState.location,
                        onMapTouch = { isMapTouched = it },
                        modifier = Modifier.verticalScroll(
                            rememberScrollState(),
                            enabled = !isMapTouched
                        )
                    )
                    else -> ReviewStep(
                        data = missionData,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        SuccessDialog(
            onDismiss = {
                showSuccessDialog = false
                onBackClick()
            },
            message = successMessage
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Falha ao salvar missao") },
            text = { Text(errorMessage ?: "Erro desconhecido") },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    titles: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        titles.forEachIndexed { index, title ->
            Surface(
                modifier = Modifier.weight(1f),
                color = if (index <= currentStep) NeuralPrimary.copy(alpha = 0.14f) else NeuralSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (index <= currentStep) NeuralPrimary.copy(alpha = 0.35f) else NeuralBorder
                )
            ) {
                Text(
                    text = "${index + 1}. $title",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = if (index <= currentStep) NeuralPrimary else NeuralTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ConfigStep(
    data: MissionData,
    onChange: (MissionData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MissionStageBanner(
            eyebrow = "SISTEMA DE PLANEJAMENTO",
            title = "Configure a base da missao",
            subtitle = "Velocidade, direcao, trajetoria e regras de seguranca antes da rota."
        )

        SectionCard(
            title = "Nome da Missao",
            subtitle = "Identificacao principal da missao",
            icon = Icons.Default.Flag
        ) {
            OutlinedTextField(
                value = data.name,
                onValueChange = { onChange(data.copy(name = it)) },
                label = { Text("Nome da Missao") },
                placeholder = { Text("Ex: Missao Alpha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        SectionCard(
            title = "Velocidade",
            subtitle = "Valores padrao usados na missao",
            icon = Icons.Default.Speed
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = data.autoFlightSpeed,
                    onValueChange = { onChange(data.copy(autoFlightSpeed = it)) },
                    label = { Text("Velocidade Padrao (m/s)") },
                    placeholder = { Text("10") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = data.maxFlightSpeed,
                    onValueChange = { onChange(data.copy(maxFlightSpeed = it)) },
                    label = { Text("Velocidade Maxima (m/s)") },
                    placeholder = { Text("15") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = data.repeatTimes,
                onValueChange = { onChange(data.copy(repeatTimes = it)) },
                label = { Text("Execucoes") },
                placeholder = { Text("1") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }

        SectionCard(
            title = "Comportamento da Missao",
            subtitle = "Campos alinhados ao DTO e ao banco",
            icon = Icons.Default.Settings
        ) {
            SelectionField(
                label = "Acao ao Finalizar",
                value = data.finishedAction,
                options = finishedActionOptions,
                onSelected = { onChange(data.copy(finishedAction = it)) }
            )
            SelectionField(
                label = "Modo de Trajetoria",
                value = data.flightPathMode,
                options = flightPathOptions,
                onSelected = { onChange(data.copy(flightPathMode = it)) }
            )
            SelectionField(
                label = "Modo de Direcao",
                value = data.headingMode,
                options = headingModeOptions,
                onSelected = { onChange(data.copy(headingMode = it)) }
            )
            SelectionField(
                label = "Modo GTFW",
                value = data.gotoFirstWaypointMode,
                options = gotoFirstWaypointOptions,
                onSelected = { onChange(data.copy(gotoFirstWaypointMode = it)) }
            )
            SwitchRow(
                label = "Sair ao Perder Sinal",
                checked = data.exitOnSignalLost,
                onCheckedChange = { onChange(data.copy(exitOnSignalLost = it)) }
            )
            SwitchRow(
                label = "Habilitar Rotacao do Gimbal",
                checked = data.gimbalPitchRotationEnabled,
                onCheckedChange = { onChange(data.copy(gimbalPitchRotationEnabled = it)) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MapStep(
    data: MissionData,
    onDataChange: (MissionData) -> Unit,
    initialMapCenter: Point?,
    onMapTouch: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val latestData by rememberUpdatedState(data)
    val scope = rememberCoroutineScope()
    var selectionTarget by rememberSaveable { mutableStateOf(SelectionTarget.POI) }
    var pendingWaypointPoint by remember { mutableStateOf<Point?>(null) }
    var selectedWaypointId by rememberSaveable { mutableIntStateOf(-1) }
    var editingWaypointId by rememberSaveable { mutableIntStateOf(-1) }
    var mapScrollReleaseJob by remember { mutableStateOf<Job?>(null) }
    val selectedWaypoint = data.waypoints.firstOrNull { it.id == selectedWaypointId }
        ?: data.waypoints.firstOrNull()
    val selectedWaypointIndex = data.waypoints.indexOfFirst { it.id == selectedWaypoint?.id }
    LaunchedEffect(data.waypoints) {
        val hasSelectedWaypoint = data.waypoints.any { it.id == selectedWaypointId }
        if (!hasSelectedWaypoint) {
            selectedWaypointId = data.waypoints.firstOrNull()?.id ?: -1
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapScrollReleaseJob?.cancel()
            onMapTouch(false)
        }
    }

    fun lockParentScrollTemporarily() {
        onMapTouch(true)
        mapScrollReleaseJob?.cancel()
        mapScrollReleaseJob = scope.launch {
            delay(180)
            onMapTouch(false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MissionStageBanner(
            eyebrow = "MAPEAMENTO TATICO",
            title = "Defina POI e waypoints",
            subtitle = "Clique no mapa para montar a rota e configure cada waypoint no modal."
        )

        SectionCard(
            title = "Como Adicionar Waypoint",
            subtitle = "O mapa está logo abaixo deste card",
            icon = Icons.Default.EditLocationAlt
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. No mapa abaixo, selecione o modo Waypoint.",
                        color = colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "2. Toque no mapa no ponto desejado.",
                        color = colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "3. O modal de configuração do waypoint abrirá automaticamente.",
                        color = colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }

        SectionCard(
            title = "Mapa da Missão",
            subtitle = "Selecione primeiro o ponto de interesse e depois os waypoints. Role para baixo para ver os waypoints adicionados.",
            icon = Icons.Default.Map
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectionTarget == SelectionTarget.POI,
                    onClick = { selectionTarget = SelectionTarget.POI },
                    label = { Text("Ponto de Interesse") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = selectionTarget == SelectionTarget.WAYPOINT,
                    onClick = { selectionTarget = SelectionTarget.WAYPOINT },
                    label = { Text("Waypoint") },
                    leadingIcon = {
                        Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Text(
                text = if (selectionTarget == SelectionTarget.POI) {
                    "Clique no mapa para definir o ponto de interesse."
                } else {
                    "Clique no mapa para selecionar as coordenadas do waypoint."
                },
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> lockParentScrollTemporarily()
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_OUTSIDE -> {
                                mapScrollReleaseJob?.cancel()
                                onMapTouch(false)
                            }
                        }
                        false
                    }
            ) {
                MapboxMapView(
                    modifier = Modifier.fillMaxSize(),
                    waypoints = data.waypoints.map { waypoint ->
                        Waypoint(
                            id = waypoint.id,
                            latitude = waypoint.latitude,
                            longitude = waypoint.longitude,
                            altitude = waypoint.altitude.toDoubleOrNull() ?: 20.0,
                            speed = data.autoFlightSpeed.toDoubleOrNull() ?: 10.0
                        )
                    },
                    pointOfInterest = data.pointOfInterest,
                    pendingPoint = pendingWaypointPoint,
                    selectedWaypointIndex = selectedWaypointIndex.takeIf { it >= 0 },
                    primaryColor = colorScheme.primary,
                    onMapClick = { point ->
                        when (selectionTarget) {
                            SelectionTarget.POI -> {
                                onDataChange(latestData.copy(pointOfInterest = point))
                            }
                            SelectionTarget.WAYPOINT -> {
                                pendingWaypointPoint = point
                            }
                        }
                        true
                    },
                    onMapReady = { map ->
                        val initialCenter = data.pointOfInterest
                            ?: initialMapCenter
                            ?: Point.fromLngLat(-44.279191, -2.53698)
                        map.setCamera(
                            com.mapbox.maps.CameraOptions.Builder()
                                .center(initialCenter)
                                .zoom(if (data.pointOfInterest == null) 12.4 else 15.6)
                                .build()
                        )
                    }
                )
            }
        }

        SectionCard(
            title = "Ponto de Interesse e Rota",
            subtitle = "Resumo operacional para validar a missão",
            icon = Icons.Default.Place
        ) {
            CoordinateSummary(
                label = "Latitude",
                value = data.pointOfInterest?.latitude()?.formatCoord() ?: "Selecione no mapa"
            )
            CoordinateSummary(
                label = "Longitude",
                value = data.pointOfInterest?.longitude()?.formatCoord() ?: "Selecione no mapa"
            )
        }

        SectionCard(
            title = "Waypoints Adicionados",
            subtitle = "A missão precisa de pelo menos 2 waypoints",
            icon = Icons.Default.Route
        ) {
            if (data.waypoints.isEmpty()) {
                Text(
                    text = "Nenhum waypoint adicionado ainda.",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
            } else {
                data.waypoints.forEachIndexed { index, waypoint ->
                    InteractiveWaypointRow(
                        waypoint = waypoint,
                        index = index,
                        isSelected = false,
                        onSelect = {},
                        onEdit = { editingWaypointId = waypoint.id },
                        onRemove = {
                            onDataChange(
                                data.copy(
                                    waypoints = data.waypoints
                                        .filterIndexed { idx, _ -> idx != index }
                                        .mapIndexed { idx, item -> item.copy(id = idx + 1) }
                                )
                            )
                        }
                    )
                    if (index < data.waypoints.lastIndex) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }

    if (pendingWaypointPoint != null) {
        WaypointEditorDialog(
            point = pendingWaypointPoint!!,
            onDismiss = { pendingWaypointPoint = null },
            onConfirm = { draft ->
                val point = pendingWaypointPoint ?: return@WaypointEditorDialog
                val nextWaypoint = MissionWaypointDraft(
                    id = data.waypoints.size + 1,
                    latitude = point.latitude(),
                    longitude = point.longitude(),
                    altitude = draft.altitude,
                    turnMode = draft.turnMode,
                    actions = draft.actions
                )
                onDataChange(data.copy(waypoints = data.waypoints + nextWaypoint))
                selectedWaypointId = nextWaypoint.id
                pendingWaypointPoint = null
            }
        )
    }

    val editingWaypoint = data.waypoints.firstOrNull { it.id == editingWaypointId }
    if (editingWaypoint != null) {
        WaypointEditorDialog(
            point = Point.fromLngLat(editingWaypoint.longitude, editingWaypoint.latitude),
            initialWaypoint = editingWaypoint,
            confirmLabel = "Salvar waypoint",
            onDismiss = { editingWaypointId = -1 },
            onConfirm = { updatedDraft ->
                onDataChange(
                    data.copy(
                        waypoints = data.waypoints.map {
                            if (it.id == editingWaypoint.id) {
                                editingWaypoint.copy(
                                    altitude = updatedDraft.altitude,
                                    turnMode = updatedDraft.turnMode,
                                    actions = updatedDraft.actions
                                )
                            } else {
                                it
                            }
                        }
                    )
                )
                selectedWaypointId = editingWaypoint.id
                editingWaypointId = -1
            }
        )
    }
}

@Composable
private fun ReviewStep(
    data: MissionData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MissionStageBanner(
            eyebrow = "REVISAO FINAL",
            title = "Valide antes de enviar",
            subtitle = "Confira os parametros da missao e a lista de waypoints."
        )

        SectionCard(
            title = "Resumo da Missao",
            subtitle = "Revise os campos antes de salvar",
            icon = Icons.Default.CheckCircle
        ) {
            InfoRow("Nome da Missao", data.name)
            InfoRow(
                "Ponto de Interesse",
                data.pointOfInterest?.let {
                    "${it.latitude().formatCoord()}, ${it.longitude().formatCoord()}"
                } ?: "Nao definido"
            )
            InfoRow("Velocidade Padrao", "${data.autoFlightSpeed} m/s")
            InfoRow("Velocidade Maxima", "${data.maxFlightSpeed} m/s")
            InfoRow("Execucoes", data.repeatTimes)
            InfoRow("Acao ao Finalizar", labelFor(data.finishedAction, finishedActionOptions))
            InfoRow("Modo de Trajetoria", labelFor(data.flightPathMode, flightPathOptions))
            InfoRow("Modo de Direcao", labelFor(data.headingMode, headingModeOptions))
            InfoRow("Modo GTFW", labelFor(data.gotoFirstWaypointMode, gotoFirstWaypointOptions))
            InfoRow("Perda de Sinal", if (data.exitOnSignalLost) "Sim" else "Nao")
            InfoRow("Rotacao do Gimbal", if (data.gimbalPitchRotationEnabled) "Sim" else "Nao")
            InfoRow("Waypoints", data.waypoints.size.toString())
        }

        SectionCard(
            title = "Lista de Waypoints",
            subtitle = "Altitude, curva e acoes por ponto",
            icon = Icons.Default.Route
        ) {
            if (data.waypoints.isEmpty()) {
                Text("Nenhum waypoint configurado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                data.waypoints.forEachIndexed { index, waypoint ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Waypoint ${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${waypoint.latitude.formatCoord()}, ${waypoint.longitude.formatCoord()}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Altitude: ${waypoint.altitude} m | Curva: ${labelFor(waypoint.turnMode, turnModeOptions)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (waypoint.actions.isEmpty()) {
                                "Acoes: nenhuma"
                            } else {
                                "Acoes: ${waypoint.actions.joinToString { labelFor(it.type, waypointActionOptions) }}"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    if (index < data.waypoints.lastIndex) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurface,
                            NeuralSurfaceAlt
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NeuralPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = NeuralPrimary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text(subtitle, color = NeuralTextMuted, fontSize = 12.sp)
                    }
                }
                content()
            }
        )
    }
}

@Composable
private fun MissionStageBanner(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    val transition = rememberInfiniteTransition(label = "stage_banner")
    val glow by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_glow"
    )

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurfaceAlt,
                            Color(0xFF0A2050),
                            NeuralBackground
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeuralPrimary.copy(alpha = glow)
                ) {
                    Text(
                        text = eyebrow,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = NeuralPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = NeuralTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun NeuralGridBackground() {
    val transition = rememberInfiniteTransition(label = "grid")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "grid_offset"
    )
    val glow by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grid_glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        var x = -step + offset
        while (x < size.width + step) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.05f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.04f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
        drawCircle(
            color = NeuralPrimary.copy(alpha = glow),
            radius = size.minDimension * 0.18f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.22f)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<LabeledOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = labelFor(value, options),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir opcoes")
                }
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CoordinateSummary(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
    }
}

@Composable
private fun ActionChipRow(
    index: Int,
    action: WaypointActionDraft,
    onRemove: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}. ${labelFor(action.type, waypointActionOptions)}",
                modifier = Modifier.weight(1f),
                color = colorScheme.onSurface,
                fontSize = 13.sp
            )
            if (action.param.isNotBlank()) {
                Text(
                    text = action.param,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remover acao", tint = colorScheme.error)
            }
        }
    }
}

@Composable
private fun InteractiveWaypointRow(
    waypoint: MissionWaypointDraft,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(
                color = if (isSelected) NeuralPrimary.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) colorScheme.primary else colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    (index + 1).toString(),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${waypoint.latitude.formatCoord()}, ${waypoint.longitude.formatCoord()}",
                fontSize = 13.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = "Altitude ${waypoint.altitude} m | ${labelFor(waypoint.turnMode, turnModeOptions)}",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = if (waypoint.actions.isEmpty()) {
                    "Sem acoes"
                } else {
                    waypoint.actions.joinToString { labelFor(it.type, waypointActionOptions) }
                },
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Tune, contentDescription = "Editar waypoint", tint = colorScheme.primary)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Remover waypoint", tint = colorScheme.error)
        }
    }
}

@Composable
private fun WaypointEditorDialog(
    point: Point,
    initialWaypoint: MissionWaypointDraft? = null,
    confirmLabel: String = "Adicionar Waypoint",
    onDismiss: () -> Unit,
    onConfirm: (MissionWaypointDraft) -> Unit
) {
    var altitude by rememberSaveable(initialWaypoint?.id) { mutableStateOf(initialWaypoint?.altitude ?: "") }
    var turnMode by rememberSaveable(initialWaypoint?.id) { mutableStateOf(initialWaypoint?.turnMode ?: "CLOCKWISE") }
    val actions = remember(initialWaypoint?.id) {
        mutableStateListOf<WaypointActionDraft>().apply {
            addAll(initialWaypoint?.actions.orEmpty())
        }
    }
    var showActionDialog by remember { mutableStateOf(false) }
    val altitudeValid = altitude.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (initialWaypoint == null) "Configurar Waypoint" else "Editar Waypoint",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${point.latitude().formatCoord()}, ${point.longitude().formatCoord()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = altitude,
                    onValueChange = { altitude = it },
                    label = { Text("Altitude") },
                    placeholder = { Text("20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SelectionField(
                    label = "Curva",
                    value = turnMode,
                    options = turnModeOptions,
                    onSelected = { turnMode = it }
                )
                SectionHeader(title = "Ações do Waypoint", icon = Icons.Default.Tune)
                if (actions.isEmpty()) {
                    Text(
                        text = "Nenhuma ação configurada",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                } else {
                    actions.forEachIndexed { index, action ->
                        ActionChipRow(
                            index = index,
                            action = action,
                            onRemove = { actions.removeAt(index) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showActionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar Ação")
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onConfirm(
                            MissionWaypointDraft(
                                id = initialWaypoint?.id ?: -1,
                                latitude = point.latitude(),
                                longitude = point.longitude(),
                                altitude = altitude,
                                turnMode = turnMode,
                                actions = actions.toList()
                            )
                        )
                    },
                    enabled = altitudeValid
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = null
    )

    if (showActionDialog) {
        WaypointActionDialog(
            onDismiss = { showActionDialog = false },
            onConfirm = { actions.add(it) }
        )
    }
}

@Composable
private fun WaypointActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (WaypointActionDraft) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(waypointActionOptions.first().value) }
    var param by rememberSaveable { mutableStateOf("") }
    val option = waypointActionOptions.first { it.value == selectedType }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar Ação")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionField(
                    label = "Tipo de Ação",
                    value = selectedType,
                    options = waypointActionOptions,
                    onSelected = { selectedType = it }
                )
                OutlinedTextField(
                    value = param,
                    onValueChange = { param = it },
                    label = { Text(parameterLabelForAction(selectedType)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Acao selecionada: ${option.label}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onConfirm(WaypointActionDraft(type = selectedType, param = param))
                        onDismiss()
                    }
                ) {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(
            value,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SuccessDialog(
    onDismiss: () -> Unit,
    message: String
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Missao Salva", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Concluir")
            }
        }
    )
}

private fun isConfigStepValid(data: MissionData): Boolean {
    val autoSpeed = data.autoFlightSpeed.toDoubleOrNull()
    val maxSpeed = data.maxFlightSpeed.toDoubleOrNull()
    val repeatTimes = data.repeatTimes.toIntOrNull()
    return data.name.isNotBlank() &&
        autoSpeed != null &&
        autoSpeed > 0 &&
        maxSpeed != null &&
        maxSpeed > 0 &&
        autoSpeed <= maxSpeed &&
        repeatTimes != null &&
        repeatTimes > 0
}

private fun missionDataToServerDto(data: MissionData): ServerMissionDto {
    val poi = data.pointOfInterest ?: data.waypoints.firstOrNull()?.let {
        Point.fromLngLat(it.longitude, it.latitude)
    } ?: Point.fromLngLat(0.0, 0.0)

    return ServerMissionDto(
        auto_flight_speed = data.autoFlightSpeed.toDoubleOrNull() ?: 10.0,
        exit_on_signal_lost = data.exitOnSignalLost,
        finished_action = data.finishedAction,
        flight_path_mode = data.flightPathMode,
        gimbal_pitch_rotation_enabled = data.gimbalPitchRotationEnabled,
        goto_first_waypoint_mode = data.gotoFirstWaypointMode,
        heading_mode = data.headingMode,
        id = 0,
        max_flight_speed = data.maxFlightSpeed.toDoubleOrNull() ?: 15.0,
        name = data.name.trim(),
        poi_latitude = poi.latitude(),
        poi_longitude = poi.longitude(),
        repeat_times = data.repeatTimes.toIntOrNull() ?: 1,
        waypoints = data.waypoints.mapIndexed { index, waypoint ->
            WaypointDto(
                actions = waypoint.actions.map { action ->
                    mapOf(
                        "type" to action.type,
                        "param" to (action.param.toIntOrNull() ?: 0)
                    )
                },
                altitude = waypoint.altitude.toDoubleOrNull() ?: 20.0,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                turn_mode = waypoint.turnMode,
                sequence = index
            )
        }
    )
}

private fun labelFor(value: String, options: List<LabeledOption>): String {
    return options.firstOrNull { it.value == value }?.label ?: value
}

private fun parameterLabelForAction(action: String): String {
    return when (action) {
        "STAY" -> "Duracao em segundos"
        "ROTATE_AIRCRAFT" -> "Angulo em graus"
        "GIMBAL_PITCH" -> "Angulo do gimbal"
        "CAMERA_ZOOM" -> "Nivel de zoom"
        "CAMERA_FOCUS" -> "Valor de foco"
        else -> "Parametro opcional"
    }
}

private fun Double.formatCoord(): String = String.format("%.6f", this)
