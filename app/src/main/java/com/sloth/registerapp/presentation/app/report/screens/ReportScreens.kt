package com.sloth.registerapp.presentation.app.report.screens

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.MissionExecutionMode
import com.sloth.registerapp.features.mission.domain.model.MissionOutcomeStatus
import com.sloth.registerapp.features.report.data.manager.FlightReportManager
import com.sloth.registerapp.features.report.data.manager.MissionMediaManager
import com.sloth.registerapp.features.report.domain.model.FlightReport
import com.sloth.registerapp.features.report.domain.model.MissionMedia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.US
import java.util.Locale
import java.io.File
import kotlinx.coroutines.launch
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private val ReportBg        = NeuralColors.Bg
private val ReportSurface   = NeuralColors.Surface
private val ReportSurfaceAlt = NeuralColors.SurfaceAlt
private val ReportBorder    = NeuralColors.Border
private val ReportBorderStrong = NeuralColors.BorderStrong
private val ReportPrimary   = NeuralColors.Primary
private val ReportSecondary = NeuralColors.Secondary
private val ReportMuted     = NeuralColors.Muted
private val ReportSuccess   = NeuralColors.Success
private val ReportWarning   = NeuralColors.Warning
private val ReportDanger    = NeuralColors.Danger

private data class MissionReport(
    val id: String,
    val missionId: String,
    val name: String,
    val status: MissionOutcomeStatus,
    val executionMode: MissionExecutionMode,
    val date: String,
    val duration: String,
    val modelName: String,
    val subtitle: String
)

private data class ReportDetailUiModel(
    val missionName: String,
    val missionId: String,
    val status: MissionOutcomeStatus,
    val statusLabel: String,
    val statusColor: Color,
    val dateTime: String,
    val startedAt: String,
    val endedAt: String,
    val duration: String,
    val missionLocation: String,
    val missionObjective: String,
    val droneModel: String,
    val pilotName: String,
    val temperatureStart: String,
    val temperatureEnd: String,
    val windSpeedStart: String,
    val windSpeedEnd: String,
    val windDirection: String,
    val weatherSummary: String,
    val missionType: String,
    val waypointCount: String,
    val coveredArea: String,
    val totalFlightTime: String,
    val distance: String,
    val averageSpeed: String,
    val maxSpeed: String,
    val maxAltitude: String,
    val batteryUsage: String,
    val batteryConsumptionPerMinute: String,
    val alerts: String,
    val gpsSatellites: String,
    val aiModel: String,
    val aiType: String,
    val aiResults: List<Pair<String, String>>,
    val occurrences: String,
    val weatherProvider: String,
    val finalObservation: String?,
    val waypointSummaries: List<WaypointSummary>
)

private data class WaypointSummary(
    val title: String,
    val latitude: String,
    val longitude: String,
    val altitude: String,
    val speed: String,
    val action: String,
    val dwellTime: String
)

@Composable
fun ReportScreen(
    onMissionClick: (String) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val flightReportManager = remember { FlightReportManager(context) }
    val reports by flightReportManager.reports.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf<MissionOutcomeStatus?>(null) }
    val missions = remember(reports) { reports.map { it.toUiModel() } }
    val normalizedQuery = query.trim().lowercase()
    val filteredMissions = missions.filter { mission ->
        val matchesQuery = normalizedQuery.isBlank() ||
            mission.name.lowercase().contains(normalizedQuery) ||
            mission.date.lowercase().contains(normalizedQuery) ||
            mission.modelName.lowercase().contains(normalizedQuery) ||
            mission.subtitle.lowercase().contains(normalizedQuery)
        val matchesStatus = selectedStatus == null || mission.status == selectedStatus
        matchesQuery && matchesStatus
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                ReportTopBar(
                    title = "Relatórios",
                    subtitle = "OPERAÇÕES  /  HISTÓRICO DE MISSÕES",
                    onBackClick = onBackClick
                )
            }

            item {
                NeuralInput(
                    value = query,
                    onValueChange = { query = it },
                    label = "Buscar missão, data ou drone",
                    leadingIcon = Icons.Default.Search
                )
            }

            item {
                ReportFilterRow(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it }
                )
            }

            if (filteredMissions.isEmpty()) {
                item {
                    EmptyReportCard(
                        message = if (missions.isEmpty()) {
                            "Nenhum relatório disponível ainda."
                        } else {
                            "Nenhum relatório encontrado com os filtros atuais."
                        }
                    )
                }
            } else {
                items(filteredMissions, key = { it.id }) { mission ->
                    MissionReportCard(
                        mission = mission,
                        onClick = { onMissionClick(mission.missionId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    missionId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaManager = remember { MissionMediaManager.getInstance(context) }
    val reportManager = remember { FlightReportManager(context) }
    val droneConnected by DJIConnectionHelper.product.collectAsStateWithLifecycle()
    val reports by reportManager.reports.collectAsStateWithLifecycle(initialValue = emptyList())
    var missionMedia by remember(missionId) { mutableStateOf<List<MissionMedia>>(emptyList()) }

    val report = reports.firstOrNull { it.extraData["missionId"] == missionId || it.id == missionId }
    val detail = remember(report) { report?.toDetailUiModel() }

    fun refreshGallery() {
        scope.launch {
            missionMedia = mediaManager.getMediaByMission(missionId)
        }
    }

    LaunchedEffect(missionId) {
        refreshGallery()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportBg)
    ) {
        ReportGridBackground()

        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Relatório não encontrado",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ID: $missionId",
                    color = ReportMuted,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onBackClick,
                    border = BorderStroke(1.dp, ReportPrimary.copy(alpha = 0.35f))
                ) {
                    Text("Voltar")
                }
            }
            return
        }

        TacticalReportDetailContent(
            detail = detail,
            missionMedia = missionMedia,
            droneConnected = droneConnected != null,
            onBackClick = onBackClick,
            onExportPdf = {
                runCatching {
                    exportReportPdf(context, detail)
                }.onSuccess { file ->
                    openPdfFile(context, file)
                    Toast.makeText(context, "PDF gerado com sucesso.", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Não foi possível gerar o PDF.", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenMedia = { media ->
                runCatching {
                    val localPath = media.localPath ?: return@runCatching
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(localPath.toUri(), "*/*")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    Toast.makeText(context, "Não foi possível abrir a mídia.", Toast.LENGTH_SHORT).show()
                }
            },
            onDownloadMedia = { media ->
                if (droneConnected == null) {
                    Toast.makeText(context, "Conecte o drone para baixar a mídia.", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        mediaManager.markDownloaded(
                            mediaId = media.id,
                            localPath = "content://vantly/local/${media.id}"
                        )
                        refreshGallery()
                        Toast.makeText(context, "Mídia marcada como baixada no telefone.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun TacticalReportDetailContent(
    detail: ReportDetailUiModel,
    missionMedia: List<MissionMedia>,
    droneConnected: Boolean,
    onBackClick: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenMedia: (MissionMedia) -> Unit,
    onDownloadMedia: (MissionMedia) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TacticalTopBar(
                missionName = detail.missionName,
                status = detail.statusLabel,
                statusColor = detail.statusColor,
                onBack = onBackClick
            )
        }
        item {
            ExportPdfButton(onClick = onExportPdf)
        }
        item { TacticalAnimatedSection(revealed, 0) { TacticalGeneralSection(detail) } }
        item { TacticalAnimatedSection(revealed, 1) { TacticalDroneSection(detail) } }
        item { TacticalAnimatedSection(revealed, 2) { TacticalEnvironmentSection(detail) } }
        item { TacticalAnimatedSection(revealed, 3) { TacticalFlightPlanSection(detail) } }
        item { TacticalAnimatedSection(revealed, 4) { TacticalWaypointsSection(detail.waypointSummaries) } }
        item { TacticalAnimatedSection(revealed, 5) { TacticalTelemetrySection(detail) } }
        item { TacticalAnimatedSection(revealed, 6) { TacticalAiSection(detail) } }
        item { TacticalAnimatedSection(revealed, 7) { TacticalOccurrencesSection(detail.occurrences) } }
        item {
            TacticalAnimatedSection(revealed, 8) {
                TacticalGallerySection(
                    missionMedia = missionMedia,
                    droneConnected = droneConnected,
                    onOpenMedia = onOpenMedia,
                    onDownloadMedia = onDownloadMedia
                )
            }
        }
        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

@Composable
private fun TacticalTopBar(
    missionName: String,
    status: String,
    statusColor: Color,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1220), ReportBg)))
            .border(1.dp, ReportBorderStrong, RoundedCornerShape(18.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, ReportPrimary, ReportSecondary, Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = ReportPrimary)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "RELATORIO DE MISSAO",
                    style = TextStyle(
                        color = ReportMuted,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = missionName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = status.uppercase(),
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun TacticalAnimatedSection(
    revealed: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = revealed,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
    ) {
        content()
    }
}

@Composable
private fun TacticalReportCard(
    title: String,
    icon: ImageVector,
    accentColor: Color = ReportPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ReportSurface)
            .border(1.dp, ReportBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Text(
                title.uppercase(),
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.45f), ReportBorder, Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun TacticalInfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = TextStyle(color = ReportMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = TextStyle(color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun TacticalMetricChip(label: String, value: String, color: Color = ReportPrimary) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = TextStyle(
                color = color.copy(alpha = 0.65f),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
private fun TacticalDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ReportBorder)
    )
}

@Composable
private fun TacticalGeneralSection(detail: ReportDetailUiModel) {
    TacticalReportCard("Informações Gerais", Icons.Default.Radar, ReportPrimary) {
        TacticalInfoRow("Nome da missão", detail.missionName)
        TacticalDivider()
        TacticalInfoRow("ID da missão", detail.missionId, ReportMuted)
        TacticalDivider()
        TacticalInfoRow("Data e horário", detail.dateTime)
        TacticalDivider()
        TacticalInfoRow("Localização", detail.missionLocation)
        TacticalDivider()
        TacticalInfoRow("Objetivo", detail.missionObjective)
        TacticalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TacticalMetricChip("Início", detail.startedAt, ReportSuccess)
            TacticalMetricChip("Fim", detail.endedAt, ReportWarning)
            TacticalMetricChip("Duração", detail.duration, ReportPrimary)
        }
    }
}

@Composable
private fun TacticalDroneSection(detail: ReportDetailUiModel) {
    TacticalReportCard("Informações do Drone", Icons.Default.PhoneAndroid, ReportWarning) {
        TacticalInfoRow("Modelo do drone", detail.droneModel)
        TacticalDivider()
        TacticalInfoRow("Piloto responsável", detail.pilotName)
        TacticalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BATERIA", style = TextStyle(color = ReportMuted, fontSize = 9.sp, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace))
                Spacer(modifier = Modifier.height(4.dp))
                TacticalBatteryBar(detail.batteryUsage)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("SATÉLITES GPS", style = TextStyle(color = ReportMuted, fontSize = 9.sp, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace))
                Spacer(modifier = Modifier.height(4.dp))
                Text(detail.gpsSatellites, style = TextStyle(color = ReportSuccess, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace))
            }
        }
        TacticalDivider()
        TacticalInfoRow("Provider do clima", detail.weatherProvider)
        detail.finalObservation?.takeIf { it.isNotBlank() && it != "N/A" }?.let { observation ->
            TacticalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A0A))
                    .border(1.dp, ReportWarning.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(observation, style = TextStyle(color = ReportWarning.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 20.sp))
            }
        }
    }
}

@Composable
private fun TacticalBatteryBar(value: String) {
    val percent = value.substringBefore("%").trim().toFloatOrNull()?.div(100f) ?: 0f
    val color = when {
        percent > 0.5f -> ReportSuccess
        percent > 0.2f -> ReportWarning
        else -> ReportDanger
    }
    Column {
        Text(value, style = TextStyle(color = color, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace))
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ReportBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
            )
        }
    }
}

@Composable
private fun TacticalEnvironmentSection(detail: ReportDetailUiModel) {
    TacticalReportCard("Condições Ambientais", Icons.Default.LocationOn, Color(0xFF64CFFF)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TacticalMetricChip("Temp. Inicial", detail.temperatureStart, Color(0xFF64CFFF))
            TacticalMetricChip("Temp. Final", detail.temperatureEnd, ReportWarning)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TacticalMetricChip("Vento Inicial", detail.windSpeedStart, ReportPrimary)
            TacticalMetricChip("Vento Final", detail.windSpeedEnd, ReportSecondary)
            TacticalMetricChip("Direção", detail.windDirection, ReportMuted)
        }
        if (detail.weatherSummary.isNotBlank() && detail.weatherSummary != "N/A") {
            TacticalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF061622))
                    .border(1.dp, ReportPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(detail.weatherSummary, style = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp))
            }
        }
    }
}

@Composable
private fun TacticalFlightPlanSection(detail: ReportDetailUiModel) {
    TacticalReportCard("Planejamento de Voo", Icons.Default.Radar, ReportSuccess) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TacticalMetricChip("Tipo", detail.missionType, ReportSuccess)
            TacticalMetricChip("Waypoints", detail.waypointCount, ReportPrimary)
            TacticalMetricChip("Área", detail.coveredArea, ReportWarning)
        }
    }
}

@Composable
private fun TacticalWaypointsSection(waypoints: List<WaypointSummary>) {
    TacticalReportCard("Dados dos Waypoints", Icons.Default.LocationOn, Color(0xFFB06EFF)) {
        if (waypoints.isEmpty()) {
            Text("Nenhum waypoint registrado.", style = TextStyle(color = ReportMuted, fontSize = 13.sp))
        } else {
            waypoints.forEachIndexed { index, wp ->
                TacticalWaypointRow(index, wp)
                if (index < waypoints.lastIndex) TacticalDivider()
            }
        }
    }
}

@Composable
private fun TacticalWaypointRow(index: Int, waypoint: WaypointSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A0D2E))
                .border(1.dp, Color(0xFFB06EFF).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", style = TextStyle(color = Color(0xFFB06EFF), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TacticalWaypointDetail("LAT", waypoint.latitude)
            TacticalWaypointDetail("LNG", waypoint.longitude)
            TacticalWaypointDetail("ALT", waypoint.altitude)
            TacticalWaypointDetail("VEL", waypoint.speed)
            TacticalWaypointDetail("DWELL", waypoint.dwellTime)
            TacticalWaypointDetail("AÇÃO", waypoint.action)
        }
    }
}

@Composable
private fun TacticalWaypointDetail(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = TextStyle(color = ReportMuted, fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace))
        Text(value, style = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun TacticalTelemetrySection(detail: ReportDetailUiModel) {
    TacticalReportCard("Telemetria e Performance", Icons.Default.CheckCircle, ReportPrimary) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TacticalMetricChip("Tempo Voo", detail.totalFlightTime, ReportPrimary)
                TacticalMetricChip("Vel. Média", detail.averageSpeed, ReportSuccess)
                TacticalMetricChip("Consumo/min", detail.batteryConsumptionPerMinute, ReportWarning)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TacticalMetricChip("Distância", detail.distance, Color(0xFF64CFFF))
                TacticalMetricChip("Vel. Máx.", detail.maxSpeed, ReportDanger)
                TacticalMetricChip("Alt. Máx.", detail.maxAltitude, Color(0xFFB06EFF))
            }
        }
        val alerts = detail.alerts.toAlertList()
        if (alerts.isNotEmpty()) {
            TacticalDivider()
            Text("ALERTAS", style = TextStyle(color = ReportDanger, fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace))
            alerts.forEach { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ReportDanger.copy(alpha = 0.08f))
                        .border(1.dp, ReportDanger.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ReportDanger, modifier = Modifier.size(14.dp))
                    Text(alert, style = TextStyle(color = ReportDanger.copy(alpha = 0.85f), fontSize = 12.sp))
                }
            }
        }
    }
}

@Composable
private fun TacticalAiSection(detail: ReportDetailUiModel) {
    TacticalReportCard("Inteligência Artificial", Icons.Default.Error, Color(0xFFB06EFF)) {
        TacticalInfoRow("Modelo utilizado", detail.aiModel)
        TacticalDivider()
        TacticalInfoRow("Tipo", detail.aiType)
        TacticalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            detail.aiResults.take(4).forEach { (label, value) ->
                TacticalMetricChip(label, value, ReportPrimary)
            }
        }
        detail.aiResults.drop(4).forEach { (label, value) ->
            TacticalDivider()
            TacticalInfoRow(label, value)
        }
    }
}

@Composable
private fun TacticalOccurrencesSection(occurrences: String) {
    TacticalReportCard("Ocorrências e Eventos", Icons.Default.Warning, ReportWarning) {
        if (occurrences.isBlank() || occurrences == "N/A") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ReportSuccess, modifier = Modifier.size(16.dp))
                Text("Nenhuma ocorrência registrada.", style = TextStyle(color = ReportSuccess.copy(alpha = 0.8f), fontSize = 13.sp))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1100))
                    .border(1.dp, ReportWarning.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(occurrences, style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 22.sp))
            }
        }
    }
}

@Composable
private fun TacticalGallerySection(
    missionMedia: List<MissionMedia>,
    droneConnected: Boolean,
    onOpenMedia: (MissionMedia) -> Unit,
    onDownloadMedia: (MissionMedia) -> Unit
) {
    TacticalReportCard("Galeria da Missão", Icons.Default.PhoneAndroid, ReportSuccess) {
        when {
            missionMedia.isEmpty() && !droneConnected -> {
                Text("Conecte o drone para ver as mídias.", style = TextStyle(color = ReportMuted, fontSize = 13.sp))
            }
            missionMedia.isEmpty() -> {
                Text("Nenhuma mídia disponível.", style = TextStyle(color = ReportMuted, fontSize = 13.sp))
            }
            else -> {
                Text("${missionMedia.size} arquivos", style = TextStyle(color = ReportMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(missionMedia) { media ->
                        TacticalMediaThumbnailCard(
                            media = media,
                            onClick = onOpenMedia,
                            onDownload = onDownloadMedia
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalMediaThumbnailCard(
    media: MissionMedia,
    onClick: (MissionMedia) -> Unit,
    onDownload: (MissionMedia) -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 108.dp, height = 84.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ReportSurfaceAlt)
            .border(1.dp, ReportBorder, RoundedCornerShape(10.dp))
            .clickable { onClick(media) }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (media.mediaType.name == "VIDEO") Icons.Default.PhoneAndroid else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ReportMuted,
                modifier = Modifier.size(26.dp)
            )
        }
        if (!media.isDownloaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ReportBg.copy(alpha = 0.85f))
                    .border(1.dp, ReportPrimary.copy(alpha = 0.5f), CircleShape)
                    .clickable { onDownload(media) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Baixar", tint = ReportPrimary, modifier = Modifier.size(14.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ReportSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Baixado", tint = ReportSuccess, modifier = Modifier.size(12.dp))
            }
        }
    }
}

private fun String.toAlertList(): List<String> {
    if (isBlank() || this == "N/A") return emptyList()
    return split(";", "•", "\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

@Composable
private fun ReportTopBar(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = ReportSurface,
            border = BorderStroke(1.dp, ReportBorderStrong)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = ReportMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.31f),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = accent, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExportPdfButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, ReportPrimary.copy(alpha = 0.35f))
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = ReportPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Gerar PDF", color = Color.White)
    }
}

@Composable
private fun NeuralInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = ReportPrimary)
        },
        label = { Text(label, color = ReportMuted) },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun ReportFilterRow(
    selectedStatus: MissionOutcomeStatus?,
    onStatusSelected: (MissionOutcomeStatus?) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportFilterChip("Todos", selectedStatus == null) { onStatusSelected(null) }
        ReportFilterChip("Concluídas", selectedStatus == MissionOutcomeStatus.COMPLETED) {
            onStatusSelected(MissionOutcomeStatus.COMPLETED)
        }
        ReportFilterChip("Falhas", selectedStatus == MissionOutcomeStatus.FAILED) {
            onStatusSelected(MissionOutcomeStatus.FAILED)
        }
        ReportFilterChip("Abortadas", selectedStatus == MissionOutcomeStatus.ABORTED) {
            onStatusSelected(MissionOutcomeStatus.ABORTED)
        }
    }
}

@Composable
private fun ReportFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
private fun EmptyReportCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ReportSurface,
        border = BorderStroke(1.dp, ReportBorder)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = ReportMuted,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun MissionReportCard(
    mission: MissionReport,
    onClick: () -> Unit
) {
    val statusColor = mission.statusColor()
    val statusLabel = mission.statusLabel()
    val statusIcon = mission.statusIcon()

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = ReportSurface,
        border = BorderStroke(1.dp, ReportBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(74.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ReportPrimary.copy(alpha = 0.24f), ReportSecondary.copy(alpha = 0.14f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚁", fontSize = 30.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = mission.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mission.modelName,
                    color = ReportMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = mission.subtitle,
                    color = ReportMuted,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(statusLabel, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                    )
                    Text(
                        text = "${mission.date} • ${mission.duration}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionSummaryHero(detail: ReportDetailUiModel) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, detail.statusColor.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(ReportSurfaceAlt, ReportSurface, detail.statusColor.copy(alpha = 0.16f))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.missionName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Missão ${detail.missionId}",
                        color = ReportMuted,
                        fontSize = 13.sp
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(detail.statusLabel) },
                    border = BorderStroke(1.dp, detail.statusColor.copy(alpha = 0.4f))
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroMetric("Início", detail.startedAt, ReportPrimary)
                HeroMetric("Fim", detail.endedAt, detail.statusColor)
                HeroMetric("Duração", detail.duration, ReportWarning)
            }
        }
    }
}

@Composable
private fun ReportSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ReportSurface,
        border = BorderStroke(1.dp, ReportBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ReportPrimary.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = ReportPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value) ->
                    MetricCard(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = ReportSurfaceAlt,
        border = BorderStroke(1.dp, ReportBorderStrong.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = ReportMuted, fontSize = 11.sp)
            Text(
                text = value.ifBlank { "N/A" },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun NeuralInfoText(text: String) {
    Text(
        text = text,
        color = ReportMuted,
        fontSize = 13.sp
    )
}

@Composable
private fun WaypointCard(waypoint: WaypointSummary) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ReportSurfaceAlt,
        border = BorderStroke(1.dp, ReportBorderStrong.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = waypoint.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            MetricGrid(
                items = listOf(
                    "Latitude" to waypoint.latitude,
                    "Longitude" to waypoint.longitude,
                    "Altura" to waypoint.altitude,
                    "Velocidade" to waypoint.speed,
                    "Ação" to waypoint.action,
                    "Tempo de permanência" to waypoint.dwellTime
                )
            )
        }
    }
}

@Composable
private fun MissionMediaCard(
    media: MissionMedia,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ReportSurfaceAlt,
        border = BorderStroke(1.dp, ReportBorderStrong.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (media.mediaType.name == "PHOTO") "Foto" else "Vídeo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = media.dronePath ?: media.localPath ?: "Mídia sem caminho",
                        color = ReportMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(if (media.isDownloaded) "No telefone" else "No drone", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (media.isDownloaded && !media.localPath.isNullOrBlank()) {
                    Button(onClick = onOpen) {
                        Text("Abrir")
                    }
                } else {
                    OutlinedButton(onClick = onDownload) {
                        Text("Baixar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportGridBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(10) { index ->
            Box(
                modifier = Modifier
                    .padding(start = (18 + index * 34).dp, top = (24 + index * 52).dp)
                    .size((120 + (index % 3) * 20).dp)
                    .alpha(0.05f)
                    .background(
                        color = if (index % 2 == 0) ReportPrimary else ReportSecondary,
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun FlightReport.toUiModel(): MissionReport {
    val status = extraData["status"]
        ?.let { runCatching { MissionOutcomeStatus.valueOf(it) }.getOrNull() }
        ?: MissionOutcomeStatus.COMPLETED
    val executionMode = extraData["executionMode"]
        ?.let { runCatching { MissionExecutionMode.valueOf(it) }.getOrNull() }
        ?: MissionExecutionMode.UNKNOWN
    val missionId = extraData["missionId"] ?: id
    val subtitle = buildString {
        append(extraData["missionType"] ?: executionMode.toDisplayLabel())
        val location = extraData["missionLocation"]
        if (!location.isNullOrBlank()) {
            append(" • ")
            append(location)
        }
    }

    return MissionReport(
        id = id,
        missionId = missionId,
        name = missionName,
        status = status,
        executionMode = executionMode,
        date = createdAtMs.toShortDate(),
        duration = durationMs.toDurationLabel(),
        modelName = extraData["droneModel"] ?: aircraftName,
        subtitle = subtitle
    )
}

private fun createMockReport(reportManager: FlightReportManager) {
    val now = System.currentTimeMillis()
    val startedAt = now - (27 * 60 * 1000L)
    val endedAt = now - (9 * 60 * 1000L)

    reportManager.startReport(
        missionName = "Inspeção Demo - Linha de Transmissão",
        aircraftName = "DJI Matrice 300 RTK",
        startedAtMs = startedAt,
        extraData = mapOf(
            "missionId" to "demo-report-001",
            "missionDate" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(startedAt)),
            "status" to MissionOutcomeStatus.COMPLETED.name,
            "executionMode" to MissionExecutionMode.REAL.name,
            "missionLocation" to "São Luís - MA",
            "missionObjective" to "Verificar estruturas, vegetação de risco e pontos de aquecimento.",
            "droneModel" to "DJI Matrice 300 RTK",
            "pilotName" to "Operador Demo",
            "weather_start_temperature_c" to "29.4",
            "weather_end_temperature_c" to "30.1",
            "weather_start_wind_ms" to "3.8",
            "weather_end_wind_ms" to "4.5",
            "windDirection" to "Nordeste",
            "weather_summary_message" to "Céu parcialmente nublado, vento moderado e boa visibilidade.",
            "weather_provider" to "WeatherAPI",
            "missionType" to "Inspeção visual automatizada",
            "waypointCount" to "4",
            "coveredArea" to "1.8 km²",
            "flightDistance" to "2.9 km",
            "avgSpeed" to "6.2 m/s",
            "maxSpeed" to "11.4 m/s",
            "maxAltitude" to "78 m",
            "altitudeAtEnd" to "62 m",
            "batteryInitial" to "98",
            "batteryFinal" to "62",
            "batteryConsumed" to "36",
            "consumptionPerMinute" to "1.9%/min",
            "alerts" to "Sem alertas críticos. Sinal estável durante toda a missão.",
            "gpsSatellites" to "18",
            "aiModel" to "Vantly Vision v2",
            "aiType" to "Detecção e classificação",
            "aiObjectsDetected" to "26",
            "aiPeopleDetected" to "3",
            "aiVehiclesDetected" to "5",
            "aiStructuresDetected" to "18",
            "aiAverageConfidence" to "92%",
            "aiFalsePositives" to "1",
            "aiFalseNegatives" to "0",
            "aiProcessingTime" to "14.2 s",
            "occurrences" to "Missão concluída com sucesso. Um waypoint exigiu ajuste manual de heading por rajada lateral.",
            "waypoint_1_lat" to "-2.5301",
            "waypoint_1_lon" to "-44.2981",
            "waypoint_1_alt" to "65 m",
            "waypoint_1_speed" to "5.0 m/s",
            "waypoint_1_action" to "START_TAKE_PHOTO",
            "waypoint_1_dwell" to "2 s",
            "waypoint_2_lat" to "-2.5310",
            "waypoint_2_lon" to "-44.2966",
            "waypoint_2_alt" to "72 m",
            "waypoint_2_speed" to "6.0 m/s",
            "waypoint_2_action" to "START_RECORD",
            "waypoint_2_dwell" to "3 s",
            "waypoint_3_lat" to "-2.5323",
            "waypoint_3_lon" to "-44.2951",
            "waypoint_3_alt" to "78 m",
            "waypoint_3_speed" to "6.5 m/s",
            "waypoint_3_action" to "ROTATE_AIRCRAFT",
            "waypoint_3_dwell" to "2 s",
            "waypoint_4_lat" to "-2.5334",
            "waypoint_4_lon" to "-44.2937",
            "waypoint_4_alt" to "70 m",
            "waypoint_4_speed" to "5.8 m/s",
            "waypoint_4_action" to "STOP_RECORD",
            "waypoint_4_dwell" to "2 s"
        )
    )
    reportManager.finishReport(
        finalObservation = "Relatório demo para validar o design da tela e a exportação em PDF.",
        endedAtMs = endedAt
    )
}

private object PdfColors {
    val Primary = android.graphics.Color.parseColor("#17305D")
    val PrimaryLight = android.graphics.Color.parseColor("#264A8C")
    val Accent = android.graphics.Color.parseColor("#00A8E8")
    val AccentSoft = android.graphics.Color.parseColor("#EAF5FF")
    val Success = android.graphics.Color.parseColor("#1F9D63")
    val Warning = android.graphics.Color.parseColor("#D98A10")
    val Danger = android.graphics.Color.parseColor("#D64541")
    val Surface = android.graphics.Color.parseColor("#F7F9FC")
    val SurfaceAlt = android.graphics.Color.parseColor("#EEF3FA")
    val Divider = android.graphics.Color.parseColor("#D8E1EE")
    val TextPrimary = android.graphics.Color.parseColor("#1E2B45")
    val TextSecondary = android.graphics.Color.parseColor("#5C6985")
    val TextMuted = android.graphics.Color.parseColor("#7B88A4")
    val TextOnDark = android.graphics.Color.WHITE
}

private data class PdfSectionRow(
    val key: String,
    val value: String
)

private data class PdfReportSection(
    val title: String,
    val rows: List<PdfSectionRow>
)

private fun pdfPaint(block: Paint.() -> Unit): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply(block)

private fun exportReportPdf(context: android.content.Context, detail: ReportDetailUiModel): File {
    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        "reports"
    ).apply { mkdirs() }
    val safeMissionId = detail.missionId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val file = File(directory, "relatorio_$safeMissionId.pdf")

    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f
    val topContentStart = 196f
    val footerBaseline = pageHeight - 16f
    val contentBottom = footerBaseline - 18f
    val usableWidth = pageWidth - (margin * 2)

    val titlePaint = pdfPaint {
        textSize = 20f
        isFakeBoldText = true
        color = PdfColors.TextOnDark
    }
    val subtitlePaint = pdfPaint {
        textSize = 10f
        color = android.graphics.Color.parseColor("#C7D7FF")
    }
    val sectionTitlePaint = pdfPaint {
        textSize = 12f
        isFakeBoldText = true
        color = PdfColors.TextOnDark
    }
    val labelPaint = pdfPaint {
        textSize = 10f
        isFakeBoldText = true
        color = PdfColors.TextPrimary
    }
    val valuePaint = pdfPaint {
        textSize = 10f
        color = PdfColors.TextSecondary
    }
    val cardValuePaint = pdfPaint {
        textSize = 12f
        isFakeBoldText = true
        color = PdfColors.TextPrimary
    }
    val cardLabelPaint = pdfPaint {
        textSize = 8.5f
        isFakeBoldText = true
        color = PdfColors.TextMuted
    }
    val badgePaint = pdfPaint {
        textSize = 9f
        isFakeBoldText = true
        color = PdfColors.TextOnDark
    }
    val footerPaint = pdfPaint {
        textSize = 8f
        color = PdfColors.TextSecondary
    }

    val sections = buildReportPdfSections(detail)
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = topContentStart

    fun drawFooter() {
        canvas.drawLine(
            margin,
            footerBaseline - 8f,
            pageWidth - margin,
            footerBaseline - 8f,
            pdfPaint {
                color = PdfColors.Divider
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }
        )
        canvas.drawText("Relatório de Missão", margin, footerBaseline, footerPaint)
        val missionLabel = "ID ${detail.missionId}"
        canvas.drawText(missionLabel, margin + 98f, footerBaseline, footerPaint)
        val pageLabel = "Pág. $pageNumber"
        canvas.drawText(pageLabel, pageWidth - margin - footerPaint.measureText(pageLabel), footerBaseline, footerPaint)
    }

    fun startNewPage() {
        drawFooter()
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin + 12f
    }

    fun ensureRoom(requiredHeight: Float) {
        if (y + requiredHeight > contentBottom) {
            startNewPage()
        }
    }

    fun String.wrap(paint: Paint, maxWidth: Float): List<String> {
        if (isBlank()) return listOf("")
        val result = mutableListOf<String>()
        split('\n').forEach { paragraph ->
            if (paragraph.isBlank()) {
                result += ""
                return@forEach
            }
            var line = ""
            paragraph.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) result += line
                    line = word
                }
            }
            if (line.isNotEmpty()) result += line
        }
        return result
    }

    fun drawWrappedText(
        text: String,
        paint: Paint,
        x: Float,
        startY: Float,
        maxWidth: Float,
        lineGap: Float = 4f
    ): Float {
        var currentY = startY
        text.wrap(paint, maxWidth).forEach { line ->
            canvas.drawText(line, x, currentY, paint)
            currentY += paint.textSize + lineGap
        }
        return currentY - startY
    }

    fun statusColorFor(label: String): Int {
        return when {
            label.contains("conclu", ignoreCase = true) -> PdfColors.Success
            label.contains("erro", ignoreCase = true) || label.contains("falha", ignoreCase = true) -> PdfColors.Danger
            else -> PdfColors.Warning
        }
    }

    fun drawHeader() {
        val headerHeight = 112f
        canvas.drawRect(
            0f,
            0f,
            pageWidth.toFloat(),
            headerHeight,
            pdfPaint { color = PdfColors.Primary; style = Paint.Style.FILL }
        )
        canvas.drawRect(
            0f,
            0f,
            6f,
            headerHeight,
            pdfPaint { color = PdfColors.Accent; style = Paint.Style.FILL }
        )
        canvas.drawCircle(
            pageWidth + 28f,
            -12f,
            96f,
            pdfPaint {
                color = PdfColors.PrimaryLight
                style = Paint.Style.FILL
                alpha = 120
            }
        )

        val headerTextX = margin + 14f
        var titleY = 38f
        detail.missionName.wrap(titlePaint, usableWidth - 160f).forEach { line ->
            canvas.drawText(line, headerTextX, titleY, titlePaint)
            titleY += titlePaint.textSize + 4f
        }
        canvas.drawText("Relatório detalhado de missão", headerTextX, titleY + 4f, subtitlePaint)
        canvas.drawText(detail.dateTime, headerTextX, titleY + 20f, subtitlePaint)

        val badgeText = "  ${detail.statusLabel.uppercase()}  "
        val badgeWidth = badgePaint.measureText(badgeText) + 10f
        val badgeLeft = pageWidth - margin - badgeWidth
        val badgeRect = android.graphics.RectF(badgeLeft, 22f, badgeLeft + badgeWidth, 40f)
        canvas.drawRoundRect(
            badgeRect,
            8f,
            8f,
            pdfPaint {
                color = statusColorFor(detail.statusLabel)
                style = Paint.Style.FILL
            }
        )
        canvas.drawText(badgeText, badgeLeft + 5f, 34f, badgePaint)
        canvas.drawText("Missão ${detail.missionId}", badgeLeft, 55f, subtitlePaint)
    }

    fun drawSummaryCards() {
        val gap = 10f
        val cardWidth = (usableWidth - gap * 2) / 3f
        val cardTop = 126f
        val cardHeight = 54f
        val summaryCards = listOf(
            "Drone" to detail.droneModel,
            "Duração" to detail.duration,
            "Cobertura" to detail.coveredArea
        )

        summaryCards.forEachIndexed { index, (label, value) ->
            val left = margin + index * (cardWidth + gap)
            val rect = android.graphics.RectF(left, cardTop, left + cardWidth, cardTop + cardHeight)
            canvas.drawRoundRect(
                rect,
                10f,
                10f,
                pdfPaint { color = PdfColors.Surface; style = Paint.Style.FILL }
            )
            canvas.drawRoundRect(
                rect,
                10f,
                10f,
                pdfPaint {
                    color = PdfColors.Divider
                    style = Paint.Style.STROKE
                    strokeWidth = 0.9f
                }
            )
            canvas.drawRect(
                left,
                cardTop,
                left + 4f,
                cardTop + cardHeight,
                pdfPaint { color = PdfColors.Accent; style = Paint.Style.FILL }
            )
            canvas.drawText(label.uppercase(), left + 12f, cardTop + 18f, cardLabelPaint)
            drawWrappedText(value, cardValuePaint, left + 12f, cardTop + 34f, cardWidth - 22f, lineGap = 2f)
        }
    }

    fun drawSectionHeader(title: String) {
        val headerRect = android.graphics.RectF(margin, y, margin + usableWidth, y + 26f)
        canvas.drawRoundRect(
            headerRect,
            7f,
            7f,
            pdfPaint { color = PdfColors.PrimaryLight; style = Paint.Style.FILL }
        )
        canvas.drawRect(
            margin,
            y,
            margin + 4f,
            y + 26f,
            pdfPaint { color = PdfColors.Accent; style = Paint.Style.FILL }
        )
        canvas.drawText(title, margin + 12f, y + 17f, sectionTitlePaint)
        y += 30f
    }

    fun measureRowHeight(row: PdfSectionRow, keyWidth: Float, valueWidth: Float): Float {
        val keyLines = row.key.wrap(labelPaint, keyWidth - 16f)
        val valueLines = row.value.wrap(valuePaint, valueWidth - 16f)
        val lines = maxOf(keyLines.size, valueLines.size).coerceAtLeast(1)
        return 10f + (lines * (valuePaint.textSize + 4f)) + 8f
    }

    fun drawRow(row: PdfSectionRow, alternate: Boolean) {
        val keyWidth = usableWidth * 0.34f
        val valueWidth = usableWidth - keyWidth
        val rowHeight = measureRowHeight(row, keyWidth, valueWidth)
        ensureRoom(rowHeight + 2f)

        val rowRect = android.graphics.RectF(margin, y, margin + usableWidth, y + rowHeight)
        canvas.drawRoundRect(
            rowRect,
            0f,
            0f,
            pdfPaint {
                color = if (alternate) PdfColors.SurfaceAlt else PdfColors.Surface
                style = Paint.Style.FILL
            }
        )
        canvas.drawLine(
            margin + keyWidth,
            y + 7f,
            margin + keyWidth,
            y + rowHeight - 7f,
            pdfPaint {
                color = PdfColors.Divider
                strokeWidth = 0.7f
                style = Paint.Style.STROKE
            }
        )

        val keyTop = y + 16f
        drawWrappedText(row.key, labelPaint, margin + 8f, keyTop, keyWidth - 16f, lineGap = 2f)
        drawWrappedText(row.value.ifBlank { "—" }, valuePaint, margin + keyWidth + 8f, keyTop, valueWidth - 16f, lineGap = 2f)
        y += rowHeight
    }

    fun drawSection(section: PdfReportSection) {
        var rowIndex = 0
        var continuation = false
        while (rowIndex < section.rows.size) {
            ensureRoom(32f)
            drawSectionHeader(
                if (continuation) "${section.title} (continuação)" else section.title
            )
            while (rowIndex < section.rows.size) {
                val row = section.rows[rowIndex]
                val rowHeight = measureRowHeight(row, usableWidth * 0.34f, usableWidth * 0.66f)
                if (y + rowHeight > contentBottom) {
                    startNewPage()
                    continuation = true
                    break
                }
                drawRow(row, alternate = rowIndex % 2 == 0)
                rowIndex += 1
            }
            if (rowIndex >= section.rows.size) {
                y += 14f
            }
        }
    }

    drawHeader()
    drawSummaryCards()

    sections.forEach { section ->
        drawSection(section)
    }

    drawFooter()
    document.finishPage(page)
    file.outputStream().use(document::writeTo)
    document.close()
    return file
}

private fun buildReportPdfSections(detail: ReportDetailUiModel): List<PdfReportSection> {
    val sections = mutableListOf<PdfReportSection>()

    sections += PdfReportSection(
        title = "Resumo Executivo",
        rows = listOf(
            PdfSectionRow("Missão", detail.missionName),
            PdfSectionRow("ID da missão", detail.missionId),
            PdfSectionRow("Status", detail.statusLabel),
            PdfSectionRow("Data e horário", detail.dateTime),
            PdfSectionRow("Início", detail.startedAt),
            PdfSectionRow("Fim", detail.endedAt),
            PdfSectionRow("Duração", detail.duration)
        )
    )

    sections += PdfReportSection(
        title = "Informações Gerais",
        rows = listOf(
            PdfSectionRow("Localização", detail.missionLocation),
            PdfSectionRow("Objetivo", detail.missionObjective),
            PdfSectionRow("Tipo de missão", detail.missionType),
            PdfSectionRow("Área coberta", detail.coveredArea),
            PdfSectionRow("Total de waypoints", detail.waypointCount)
        )
    )

    sections += PdfReportSection(
        title = "Drone e Operação",
        rows = listOf(
            PdfSectionRow("Modelo do drone", detail.droneModel),
            PdfSectionRow("Piloto responsável", detail.pilotName),
            PdfSectionRow("Uso de bateria", detail.batteryUsage),
            PdfSectionRow("Consumo por minuto", detail.batteryConsumptionPerMinute),
            PdfSectionRow("Satélites GPS", detail.gpsSatellites),
            PdfSectionRow("Alertas", detail.alerts),
            PdfSectionRow("Observação final", detail.finalObservation ?: "N/A")
        )
    )

    sections += PdfReportSection(
        title = "Clima",
        rows = listOf(
            PdfSectionRow("Temperatura inicial", detail.temperatureStart),
            PdfSectionRow("Temperatura final", detail.temperatureEnd),
            PdfSectionRow("Vento inicial", detail.windSpeedStart),
            PdfSectionRow("Vento final", detail.windSpeedEnd),
            PdfSectionRow("Direção do vento", detail.windDirection),
            PdfSectionRow("Resumo climático", detail.weatherSummary),
            PdfSectionRow("Provider do clima", detail.weatherProvider)
        )
    )

    sections += PdfReportSection(
        title = "Telemetria e Performance",
        rows = listOf(
            PdfSectionRow("Tempo total de voo", detail.totalFlightTime),
            PdfSectionRow("Distância percorrida", detail.distance),
            PdfSectionRow("Velocidade média", detail.averageSpeed),
            PdfSectionRow("Velocidade máxima", detail.maxSpeed),
            PdfSectionRow("Altitude máxima", detail.maxAltitude)
        )
    )

    sections += PdfReportSection(
        title = "Inteligência Artificial",
        rows = buildList {
            add(PdfSectionRow("Modelo utilizado", detail.aiModel))
            add(PdfSectionRow("Tipo", detail.aiType))
            detail.aiResults.forEach { (label, value) ->
                add(PdfSectionRow(label, value))
            }
        }
    )

    sections += PdfReportSection(
        title = "Ocorrências e Eventos",
        rows = listOf(PdfSectionRow("Registro", detail.occurrences))
    )

    if (detail.waypointSummaries.isEmpty()) {
        sections += PdfReportSection(
            title = "Waypoints",
            rows = listOf(PdfSectionRow("Informação", "Nenhum waypoint disponível nesta missão."))
        )
    } else {
        detail.waypointSummaries.forEachIndexed { index, waypoint ->
            sections += PdfReportSection(
                title = "Waypoint ${index + 1} · ${waypoint.title}",
                rows = listOf(
                    PdfSectionRow("Latitude", waypoint.latitude),
                    PdfSectionRow("Longitude", waypoint.longitude),
                    PdfSectionRow("Altitude", waypoint.altitude),
                    PdfSectionRow("Velocidade", waypoint.speed),
                    PdfSectionRow("Ação", waypoint.action),
                    PdfSectionRow("Permanência", waypoint.dwellTime)
                )
            )
        }
    }

    return sections
}

private fun openPdfFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
}

private fun FlightReport.toDetailUiModel(): ReportDetailUiModel {
    val status = extraData["status"]
        ?.let { runCatching { MissionOutcomeStatus.valueOf(it) }.getOrNull() }
        ?: MissionOutcomeStatus.COMPLETED

    return ReportDetailUiModel(
        missionName = missionName,
        missionId = extraData["missionId"] ?: id,
        status = status,
        statusLabel = status.statusLabel(),
        statusColor = status.statusColor(),
        dateTime = extraData["missionDate"] ?: startedAtMs.toDateTimeLabel(),
        startedAt = startedAtMs.toHourLabel(),
        endedAt = endedAtMs.toHourLabel(),
        duration = durationMs.toDurationLabel(),
        missionLocation = extraData["missionLocation"] ?: "N/A",
        missionObjective = extraData["missionObjective"] ?: "N/A",
        droneModel = extraData["droneModel"] ?: aircraftName,
        pilotName = extraData["pilotName"] ?: "N/A",
        temperatureStart = extraData["weather_start_temperature_c"].withUnit("°C"),
        temperatureEnd = extraData["weather_end_temperature_c"].withUnit("°C"),
        windSpeedStart = extraData["weather_start_wind_ms"].toKmhLabel(),
        windSpeedEnd = extraData["weather_end_wind_ms"].toKmhLabel(),
        windDirection = extraData["windDirection"] ?: "N/A",
        weatherSummary = extraData["weather_summary_message"] ?: "N/A",
        missionType = extraData["missionType"] ?: "N/A",
        waypointCount = extraData["waypointCount"] ?: "N/A",
        coveredArea = extraData["coveredArea"] ?: "N/A",
        totalFlightTime = durationMs.toDurationLabel(),
        distance = extraData["flightDistance"] ?: "N/A",
        averageSpeed = extraData["avgSpeed"] ?: "N/A",
        maxSpeed = extraData["maxSpeed"] ?: "N/A",
        maxAltitude = extraData["maxAltitude"] ?: (extraData["altitudeAtEnd"] ?: "N/A"),
        batteryUsage = buildBatteryUsage(extraData),
        batteryConsumptionPerMinute = extraData["consumptionPerMinute"] ?: "N/A",
        alerts = extraData["alerts"] ?: "N/A",
        gpsSatellites = extraData["gpsSatellites"] ?: "N/A",
        aiModel = extraData["aiModel"] ?: "N/A",
        aiType = extraData["aiType"] ?: "N/A",
        aiResults = listOf(
            "Objetos detectados" to (extraData["aiObjectsDetected"] ?: "N/A"),
            "Pessoas" to (extraData["aiPeopleDetected"] ?: "N/A"),
            "Veículos" to (extraData["aiVehiclesDetected"] ?: "N/A"),
            "Estruturas" to (extraData["aiStructuresDetected"] ?: "N/A"),
            "Confiança média" to (extraData["aiAverageConfidence"] ?: "N/A"),
            "Falsos positivos" to (extraData["aiFalsePositives"] ?: "N/A"),
            "Falsos negativos" to (extraData["aiFalseNegatives"] ?: "N/A"),
            "Tempo de processamento" to (extraData["aiProcessingTime"] ?: "N/A")
        ),
        occurrences = extraData["occurrences"] ?: "N/A",
        weatherProvider = extraData["weather_provider"] ?: "N/A",
        finalObservation = finalObservation,
        waypointSummaries = extraData.toWaypointSummaries()
    )
}

private fun Map<String, String>.toWaypointSummaries(): List<WaypointSummary> {
    val waypointCount = this["waypointCount"]?.toIntOrNull() ?: return emptyList()
    return (1..waypointCount).map { index ->
        WaypointSummary(
            title = "Waypoint $index",
            latitude = this["waypoint_${index}_lat"] ?: "N/A",
            longitude = this["waypoint_${index}_lon"] ?: "N/A",
            altitude = this["waypoint_${index}_alt"] ?: "N/A",
            speed = this["waypoint_${index}_speed"] ?: "N/A",
            action = this["waypoint_${index}_action"] ?: "N/A",
            dwellTime = this["waypoint_${index}_dwell"] ?: "N/A"
        )
    }
}

private fun buildBatteryUsage(extraData: Map<String, String>): String {
    val initial = extraData["batteryInitial"] ?: "N/A"
    val final = extraData["batteryFinal"] ?: "N/A"
    val consumed = extraData["batteryConsumed"] ?: "N/A"
    return "$initial% -> $final% (gasto: $consumed%)"
}

private fun MissionReport.statusColor(): Color = status.statusColor()

private fun MissionOutcomeStatus.statusColor(): Color = when (this) {
    MissionOutcomeStatus.COMPLETED -> ReportSuccess
    MissionOutcomeStatus.ABORTED -> ReportWarning
    MissionOutcomeStatus.FAILED -> ReportDanger
}

private fun MissionReport.statusLabel(): String = status.statusLabel()

private fun MissionOutcomeStatus.statusLabel(): String = when (this) {
    MissionOutcomeStatus.COMPLETED -> "Concluída"
    MissionOutcomeStatus.ABORTED -> "Abortada"
    MissionOutcomeStatus.FAILED -> "Falha"
}

private fun MissionReport.statusIcon(): ImageVector = when (status) {
    MissionOutcomeStatus.COMPLETED -> Icons.Default.CheckCircle
    MissionOutcomeStatus.ABORTED -> Icons.Default.Warning
    MissionOutcomeStatus.FAILED -> Icons.Default.Error
}

private fun MissionExecutionMode.toDisplayLabel(): String = when (this) {
    MissionExecutionMode.REAL -> "Missão real"
    MissionExecutionMode.SIMULATED -> "Simulada"
    MissionExecutionMode.UNKNOWN -> "Modo não informado"
}

private fun String?.toKmhLabel(): String {
    val ms = this?.toDoubleOrNull() ?: return "N/A"
    return "${String.format(Locale.US, "%.1f", ms * 3.6)} km/h"
}

private fun String?.withUnit(unit: String): String {
    val value = this ?: return "N/A"
    return if (value == "N/A") value else "$value$unit"
}

private fun Long.toShortDate(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale("pt", "BR"))
    return formatter.format(Date(this))
}

private fun Long.toDateTimeLabel(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(this))
}

private fun Long.toHourLabel(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(this))
}

private fun Long.toDurationLabel(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
