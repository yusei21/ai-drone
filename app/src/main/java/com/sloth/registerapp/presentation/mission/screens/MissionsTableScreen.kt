package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private val MissionBg       = NeuralColors.Bg
private val MissionSurface  = NeuralColors.Surface
private val MissionSurfaceAlt = NeuralColors.SurfaceAlt
private val MissionBorder   = NeuralColors.Border
private val MissionPrimary  = NeuralColors.Primary
private val MissionSecondary = NeuralColors.Secondary
private val MissionMuted    = NeuralColors.Muted
private val MissionDanger   = NeuralColors.Danger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsTableScreen(
    missions: List<Mission> = emptyList(),
    isLoading: Boolean = false,
    canCreateMission: Boolean = true,
    createBlockedMessage: String? = null,
    onCreateMissionClick: () -> Unit = {},
    onViewMissionClick: (Int) -> Unit = {},
    onEditMissionClick: (Int) -> Unit = {},
    onDeleteMissionClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var missionToDelete by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MissionBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Missões",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OPERAÇÕES  /  ROTAS E EXECUÇÃO",
                            color = MissionMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MissionBg)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MissionBg)
        ) {
            when {
                isLoading -> LoadingMissionsState()
                missions.isEmpty() -> EmptyMissionsState(
                    canCreateMission = canCreateMission,
                    createBlockedMessage = createBlockedMessage,
                    onCreateMissionClick = onCreateMissionClick
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 16.dp,
                        end = 20.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MissionCreateBanner(
                            canCreateMission = canCreateMission,
                            createBlockedMessage = createBlockedMessage,
                            onCreateMissionClick = onCreateMissionClick
                        )
                    }

                    items(missions, key = { it.id }) { mission ->
                        MissionListCard(
                            mission = mission,
                            onOpenClick = { onViewMissionClick(mission.id) },
                            onEditClick = { onEditMissionClick(mission.id) },
                            onDeleteClick = { missionToDelete = mission.id }
                        )
                    }
                }
            }
        }
    }

    if (missionToDelete != null) {
        AlertDialog(
            onDismissRequest = { missionToDelete = null },
            containerColor = MissionSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MissionDanger)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir missão", color = Color.White)
                }
            },
            text = {
                Text(
                    "Deseja realmente excluir esta missão?",
                    color = MissionMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        missionToDelete?.let(onDeleteMissionClick)
                        missionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MissionDanger)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { missionToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LoadingMissionsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MissionPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Carregando missoes...", color = MissionMuted)
        }
    }
}

@Composable
private fun MissionCreateBanner(
    canCreateMission: Boolean,
    createBlockedMessage: String?,
    onCreateMissionClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MissionSurface,
        border = BorderStroke(
            1.dp,
            if (canCreateMission) MissionPrimary.copy(alpha = 0.22f) else MissionBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MissionPrimary.copy(alpha = if (canCreateMission) 0.10f else 0.04f),
                            MissionSurface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Nova missao",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = createBlockedMessage ?: "Crie uma nova missao sem cobrir as acoes dos cards.",
                    color = if (canCreateMission) MissionMuted else MissionPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onCreateMissionClick,
                enabled = canCreateMission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canCreateMission) MissionPrimary else MissionSurfaceAlt,
                    contentColor = if (canCreateMission) MissionBg else MissionMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Criar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyMissionsState(
    canCreateMission: Boolean,
    createBlockedMessage: String?,
    onCreateMissionClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nenhuma missao encontrada", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = createBlockedMessage ?: "Crie sua primeira missao para comecar.",
                color = MissionMuted
            )
            Button(
                onClick = onCreateMissionClick,
                enabled = canCreateMission,
                colors = ButtonDefaults.buttonColors(containerColor = MissionPrimary)
            ) {
                Text("Criar missao")
            }
        }
    }
}

@Composable
private fun MissionListCard(
    mission: Mission,
    onOpenClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MissionPrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MissionSurface,
                            MissionSurfaceAlt
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MissionPreviewPane(mission = mission)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = mission.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoMetric(label = "Velocidade", value = "${mission.autoSpeed} m/s")
                    InfoMetric(label = "Altura", value = "${mission.altitude.toInt()} m")
                    InfoMetric(label = "Waypoints", value = "${mission.waypointCount}")
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ActionMiniButton(
                                modifier = Modifier.fillMaxWidth(),
                                label = "Abrir missao",
                                icon = Icons.Default.OpenInNew,
                                tint = MissionPrimary,
                                onClick = onOpenClick
                            )
                        }
                        ActionIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Editar missao",
                            tint = MissionPrimary,
                            onClick = onEditClick
                        )
                        ActionIconButton(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir missao",
                            tint = MissionDanger,
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MissionMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionMiniButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MissionPreviewPane(mission: Mission) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f),
        shape = RoundedCornerShape(16.dp),
        color = MissionSurfaceAlt,
        border = BorderStroke(1.dp, MissionBorder)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF061018))

            val gridStep = 32.dp.toPx()
            var gx = 0f
            while (gx <= size.width) {
                drawLine(Color(0x1400C2FF), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = 0f
            while (gy <= size.height) {
                drawLine(Color(0x1400C2FF), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += gridStep
            }

            val waypoints = mission.previewPoints
            if (waypoints.isEmpty()) return@Canvas

            val allLngs = waypoints.map { it.longitude } + mission.pointOfInterestLongitude
            val allLats = waypoints.map { it.latitude } + mission.pointOfInterestLatitude
            val minLng = allLngs.min()
            val maxLng = allLngs.max()
            val minLat = allLats.min()
            val maxLat = allLats.max()
            val spanX = maxOf(0.0001, maxLng - minLng)
            val spanY = maxOf(0.0001, maxLat - minLat)
            val padding = 26.dp.toPx()

            fun toScreen(lat: Double, lng: Double): Offset {
                val px = ((lng - minLng) / spanX).toFloat() * (size.width - padding * 2) + padding
                val py = ((maxLat - lat) / spanY).toFloat() * (size.height - padding * 2) + padding
                return Offset(px, py)
            }

            val screenPoints = waypoints.map { toScreen(it.latitude, it.longitude) }

            if (screenPoints.size > 1) {
                val path = Path()
                screenPoints.forEachIndexed { i, o ->
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path = path,
                    color = Color(0x8800C2FF),
                    style = Stroke(width = 4f, join = StrokeJoin.Round, cap = StrokeCap.Round)
                )
            }

            screenPoints.forEachIndexed { index, offset ->
                drawCircle(Color(0x3300C2FF), radius = if (index == 0) 10f else 7f, center = offset)
                drawCircle(Color(0xFF00C2FF), radius = if (index == 0) 5f else 3.5f, center = offset)
            }

            val poiOffset = toScreen(mission.pointOfInterestLatitude, mission.pointOfInterestLongitude)
            drawCircle(Color(0xFF00E5A0), radius = 4.5f, center = poiOffset)
        }
    }
}
