package com.sloth.registerapp.presentation.app.dashboard.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.R
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private const val DASHBOARD_TAG = "DashboardScreen"
private val NeuralBg        = NeuralColors.Bg
private val NeuralSurface   = NeuralColors.Surface
private val NeuralSurfaceAlt = NeuralColors.SurfaceAlt
private val NeuralBorder    = NeuralColors.Border
private val NeuralBorderStrong = NeuralColors.BorderStrong
private val NeuralPrimary   = NeuralColors.Primary
private val NeuralSecondary = NeuralColors.Secondary
private val NeuralMuted     = NeuralColors.Muted
private val NeuralSuccess   = NeuralColors.Success
private val NeuralWarning   = NeuralColors.Warning
private val NeuralDanger    = NeuralColors.Danger
private val NeuralCardGlow  = NeuralColors.CardGlow
private val DroneCatalogCardHeight = 330.dp
private val SupportCardHeight = 84.dp

data class CompatibleDrone(
    val name: String,
    val description: String,
    val imageRes: Int,
    val officialUrl: String,
    val accentColor: Color
)

@Composable
fun DashboardScreen(
    droneStatus: String = "Desconectado",
    userName: String = "Usuario",
    isLoggedIn: Boolean = true,
    onShowAllDronesClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRefreshStatusClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val drones = remember { featuredCompatibleDrones() }
    val statusUi = remember(droneStatus) { dashboardStatus(droneStatus) }
    var visible by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "dashboard_pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralBg)
    ) {
        DashboardGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            DashboardTopBar(
                onSettingsClick = onSettingsClick,
                pulseAlpha = pulseAlpha
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(700))
            ) {
                Column {
                    HeroBanner(
                        userName = userName,
                        isLoggedIn = isLoggedIn
                    )

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ConnectionStatusCard(
                            statusUi = statusUi,
                            isRefreshing = false,
                            onRefreshStatusClick = onRefreshStatusClick
                        )

                        SectionTitle(
                            title = "Drones Compativeis",
                            icon = Icons.Default.AirplanemodeActive,
                            actionLabel = "Lista completa",
                            onActionClick = onShowAllDronesClick
                        )
                        DroneCarousel(
                            drones = drones,
                            onDroneClick = { drone ->
                                Log.d(DASHBOARD_TAG, "Mock do site oficial para ${drone.name}: ${drone.officialUrl}")
                                Toast.makeText(
                                    context,
                                    "Mock do site oficial da DJI para ${drone.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        SectionTitle(
                            title = "Apoio Institucional",
                            icon = Icons.Default.Business
                        )
                        InstitutionalSupportGrid()

                        SectionTitle(
                            title = "Sobre o Projeto",
                            icon = Icons.Default.Info,
                            actionLabel = "Clima",
                            onActionClick = onWeatherClick
                        )
                        ProjectDescriptionCard()

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    onSettingsClick: () -> Unit,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeuralPrimary, NeuralSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vantly Neural",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Visao computacional e rotas",
                color = NeuralMuted,
                fontSize = 10.sp
            )
        }

        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = NeuralSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong.copy(alpha = 0.9f))
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuracoes",
                    tint = NeuralPrimary,
                    modifier = Modifier.alpha(0.95f + pulseAlpha * 0.2f)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(
    userName: String,
    isLoggedIn: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralBg,
                            Color(0xFF0A2050),
                            NeuralBg
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NeuralPrimary.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeuralPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SISTEMA ATIVO",
                            color = NeuralPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Bem-vindo de volta,\n${if (isLoggedIn) userName else "Piloto"}.",
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Gerencie rotas e monitore seus drones com inteligencia artificial em tempo real.",
                    color = NeuralMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeuralPrimary,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

private data class DashboardStatusUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private fun dashboardStatus(raw: String): DashboardStatusUi {
    val normalized = raw.ifBlank { "Status indisponivel" }
    return when {
        raw.contains("conect", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Link ativo com o drone",
                icon = Icons.Default.Wifi,
                accent = NeuralPrimary
            )
        raw.contains("pronto", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Aguardando conexao com o drone",
                icon = Icons.Default.Wifi,
                accent = NeuralWarning
            )
        raw.contains("registr", ignoreCase = true) || raw.contains("falha", ignoreCase = true) || raw.contains("erro", ignoreCase = true) ->
            DashboardStatusUi(
                title = normalized,
                subtitle = "Verifique SDK, permissao ou rede",
                icon = Icons.Default.WifiOff,
                accent = NeuralDanger
            )
        else -> DashboardStatusUi(
            title = normalized,
            subtitle = "Estado atual informado pelo SDK",
            icon = Icons.Default.WifiOff,
            accent = NeuralDanger
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    statusUi: DashboardStatusUi,
    isRefreshing: Boolean,
    onRefreshStatusClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, statusUi.accent.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurface,
                            if (statusUi.accent == NeuralDanger) Color(0xFF1A0820) else Color(0xFF002040)
                        )
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = statusUi.accent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, statusUi.accent.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = statusUi.accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = statusUi.icon,
                            contentDescription = null,
                            tint = statusUi.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusUi.title,
                    color = if (statusUi.accent == NeuralDanger) NeuralDanger else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusUi.subtitle,
                    color = NeuralMuted,
                    fontSize = 11.sp
                )
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = NeuralPrimary.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeuralPrimary.copy(alpha = 0.28f))
            ) {
                IconButton(onClick = onRefreshStatusClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar status",
                        tint = NeuralPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeuralPrimary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeuralPrimary,
                modifier = Modifier.padding(6.dp).size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(NeuralBorderStrong, Color.Transparent)
                    )
                )
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, color = NeuralPrimary)
            }
        }
    }
}

@Composable
private fun DroneCarousel(
    drones: List<CompatibleDrone>,
    onDroneClick: (CompatibleDrone) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        drones.forEachIndexed { index, drone ->
            DroneStatusCard(
                modifier = Modifier.width(248.dp),
                drone = drone,
                index = index,
                onClick = { onDroneClick(drone) }
            )
        }
    }
}

@Composable
internal fun DroneStatusCard(
    modifier: Modifier = Modifier,
    drone: CompatibleDrone,
    index: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.height(DroneCatalogCardHeight),
        shape = RoundedCornerShape(20.dp),
        color = NeuralSurface,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, drone.accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                drone.accentColor.copy(alpha = 0.20f),
                                NeuralCardGlow.copy(alpha = 0.92f)
                            )
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = drone.imageRes),
                    contentDescription = drone.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = drone.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = drone.description,
                        color = NeuralMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    drone.accentColor.copy(alpha = 0.82f),
                                    drone.accentColor
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            Log.d(DASHBOARD_TAG, "Mock do site oficial para ${drone.name}: ${drone.officialUrl}")
                            Toast.makeText(
                                context,
                                "Mock do site oficial da DJI para ${drone.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IR PARA DJI",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DroneCardSpec(
    icon: ImageVector,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.82f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun compatibleDroneFamily(name: String): String {
    return when {
        "matrice" in name.lowercase() -> "Linha Matrice"
        "phantom" in name.lowercase() -> "Linha Phantom"
        "mini" in name.lowercase() -> "Linha Mini"
        "air" in name.lowercase() -> "Linha Air"
        "spark" in name.lowercase() -> "Linha Spark"
        "enterprise" in name.lowercase() -> "Enterprise"
        "thermal" in name.lowercase() -> "Thermal"
        "multispectral" in name.lowercase() -> "Multispectral"
        else -> "Linha Mavic"
    }
}

private fun compatibleDroneModelTag(name: String): String {
    return compatibleDroneFamily(name)
        .replace("Linha ", "")
        .uppercase()
}

private fun compatibleDroneFamilyLabel(name: String): String {
    return when {
        "matrice" in name.lowercase() -> "Matrice"
        "phantom" in name.lowercase() -> "Phantom"
        "mini" in name.lowercase() -> "Mini"
        "air" in name.lowercase() -> "Air"
        "spark" in name.lowercase() -> "Spark"
        else -> "Mavic"
    }
}

private fun compatibleDroneProfile(name: String): String {
    val normalized = name.lowercase()
    return when {
        "thermal" in normalized -> "Termico"
        "multispectral" in normalized -> "Mapeamento"
        "enterprise" in normalized -> "Industrial"
        "rtk" in normalized -> "Precisao"
        "mini" in normalized || "air" in normalized || "spark" in normalized -> "Portatil"
        else -> "Aereo"
    }
}

@Composable
private fun InstitutionalSupportGrid() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportCard(
            modifier = Modifier.weight(1f),
            name = "IFMA",
            subtitle = "Instituto Federal",
            imageRes = R.drawable.ifma_logo
        )
        SupportCard(
            modifier = Modifier.weight(1f),
            name = "FAPEMA",
            subtitle = "Pesquisa e inovacao",
            icon = Icons.Default.Science
        )
    }
}

@Composable
private fun SupportCard(
    modifier: Modifier = Modifier,
    name: String,
    subtitle: String,
    icon: ImageVector? = null,
    imageRes: Int? = null
) {
    Surface(
        modifier = modifier.height(SupportCardHeight),
        shape = RoundedCornerShape(16.dp),
        color = NeuralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = NeuralPrimary.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        imageRes != null -> Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = name,
                            modifier = Modifier.padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                        icon != null -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeuralPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NeuralMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProjectDescriptionCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NeuralSurface,
                            NeuralSurfaceAlt
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Vantly Neural e uma plataforma avancada de gerenciamento de drones autonomos com processamento de imagem em tempo real via inteligencia artificial embarcada.",
                color = Color(0xFF8AB4CE),
                fontSize = 13.sp,
                lineHeight = 21.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProjectStat(value = "98.7%", label = "Precisao IA")
                VerticalDivider()
                ProjectStat(value = "< 20ms", label = "Latencia")
                VerticalDivider()
                ProjectStat(value = "50km2", label = "Cobertura")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TechTag("IA Embarcada")
                TechTag("Tempo Real")
                TechTag("5G Ready")
            }
        }
    }
}

@Composable
private fun ProjectStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TextStyle(
                brush = Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary)),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = label,
            color = NeuralMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(NeuralBorderStrong)
    )
}

@Composable
private fun TechTag(label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NeuralPrimary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuralPrimary.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = NeuralPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DashboardGridBackground() {
    val transition = rememberInfiniteTransition(label = "dashboard_grid")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "dashboard_grid_offset"
    )
    val glow by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboard_grid_glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        var x = -step + offset
        while (x < size.width + step) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.04f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = NeuralPrimary.copy(alpha = 0.035f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }
        drawCircle(
            color = NeuralPrimary.copy(alpha = glow),
            radius = size.minDimension * 0.18f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f)
        )
    }
}

fun featuredCompatibleDrones(): List<CompatibleDrone> = listOf(
    compatibleDrone(
        name = "DJI Mavic Air",
        description = "Drone compacto da linha Air, focado em mobilidade e captura aérea leve.",
        accentColor = NeuralPrimary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2",
        description = "Modelo Air com foco em autonomia, estabilidade e uso geral de campo.",
        accentColor = NeuralSecondary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2S",
        description = "Versão Air com sensor superior e perfil mais forte para imagem aérea.",
        accentColor = NeuralSuccess
    ),
    compatibleDrone(
        name = "DJI Air 3",
        description = "Air de nova geração com câmera dupla e operação mais versátil.",
        accentColor = NeuralWarning
    ),
    compatibleDrone(
        name = "DJI Mavic Mini",
        description = "Mini ultraleve para voo recreativo e missões visuais simples.",
        accentColor = NeuralPrimary
    )
)

fun defaultCompatibleDrones(): List<CompatibleDrone> = listOf(
    compatibleDrone(
        name = "DJI Mavic Air",
        description = "Drone compacto da linha Air, focado em mobilidade e captura aérea leve.",
        accentColor = NeuralPrimary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2",
        description = "Modelo Air com foco em autonomia, estabilidade e uso geral de campo.",
        accentColor = NeuralSecondary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2S",
        description = "Versão Air com sensor superior e perfil mais forte para imagem aérea.",
        accentColor = NeuralSuccess
    ),
    compatibleDrone("DJI Air 3", "Air de nova geração com câmera dupla e operação mais versátil.", NeuralPrimary),
    compatibleDrone("DJI Mavic Mini", "Mini ultraleve para voo recreativo e missões visuais simples.", NeuralSecondary),
    compatibleDrone("DJI Mini 2", "Mini portátil com melhor estabilidade e alcance para uso leve.", NeuralSuccess),
    compatibleDrone("DJI Mini 3", "Mini com câmera otimizada e foco em operação portátil.", NeuralPrimary),
    compatibleDrone("DJI Mini 3 Pro", "Mini avançado com recursos premium de imagem e segurança.", NeuralSecondary),
    compatibleDrone("DJI Mini 4 Pro", "Mini topo de linha para operação compacta com mais sensores.", NeuralSuccess),
    compatibleDrone("DJI MAPIR", "Plataforma voltada para mapeamento e captura especializada.", NeuralPrimary),
    compatibleDrone("DJI Mavic Pro", "Clássico dobrável da linha Mavic para operação geral.", NeuralSecondary),
    compatibleDrone("DJI Mavic 2 Pro", "Mavic com foco em imagem de maior qualidade e uso profissional.", NeuralSuccess),
    compatibleDrone("DJI Mavic 2 Zoom", "Versão com câmera zoom para inspeção e observação.", NeuralPrimary),
    compatibleDrone("DJI Mavic 2 Enterprise Dual", "Enterprise com câmera dual para operação técnica.", NeuralSecondary),
    compatibleDrone("DJI Mavic 2 Enterprise Advanced", "Enterprise avançado com foco em inspeção e resposta.", NeuralSuccess),
    compatibleDrone("DJI Mavic 3 / DJI Mavic 3 Pro", "Linha Mavic 3 para captura avançada e missões premium.", NeuralPrimary),
    compatibleDrone("DJI Mavic 3 Multispectral", "Modelo voltado para agricultura e análise multiespectral.", NeuralSecondary),
    compatibleDrone("DJI Mavic 3 Enterprise - 3E", "Enterprise com foco em mapeamento e inspeção corporativa.", NeuralSuccess),
    compatibleDrone("DJI Mavic 3 Thermal - 3T", "Versão térmica para segurança, busca e inspeção.", NeuralPrimary),
    compatibleDrone("DJI Phantom 3", "Plataforma clássica Phantom para captura aérea e treino.", NeuralSecondary),
    compatibleDrone("DJI Phantom 3 Advanced", "Phantom 3 com pacote avançado para operação estável.", NeuralSuccess),
    compatibleDrone("DJI Phantom 3 Pro", "Versão Pro da linha Phantom 3 com perfil mais robusto.", NeuralPrimary),
    compatibleDrone("DJI Phantom 4", "Plataforma amplamente usada em mapeamento e inspeção.", NeuralSecondary),
    compatibleDrone("DJI Phantom 4 Pro", "Phantom 4 com câmera melhorada para uso profissional.", NeuralSuccess),
    compatibleDrone("DJI Phantom 4 Pro V2", "Refinamento da linha Phantom 4 Pro para uso técnico.", NeuralPrimary),
    compatibleDrone("DJI Phantom 4 RTK", "Modelo RTK para mapeamento preciso e georreferenciamento.", NeuralSecondary),
    compatibleDrone("DJI Phantom 4 Multispectral", "Plataforma Phantom voltada para agricultura de precisão.", NeuralSuccess),
    compatibleDrone("DJI Spark", "Drone compacto e leve para captura rápida e uso recreativo.", NeuralPrimary),
    compatibleDrone("DJI Matrice 100", "Plataforma Matrice modular para pesquisa e desenvolvimento.", NeuralSecondary),
    compatibleDrone("DJI Matrice 200", "Matrice industrial para inspeção e missões corporativas.", NeuralSuccess),
    compatibleDrone("DJI Matrice 200 V2", "Revisão V2 com foco em confiabilidade operacional.", NeuralPrimary),
    compatibleDrone("DJI Matrice 210", "Versão com suporte ampliado a cargas e sensores.", NeuralSecondary),
    compatibleDrone("DJI Matrice 210 RTK", "Matrice com RTK para missões que exigem precisão.", NeuralSuccess),
    compatibleDrone("DJI Matrice 210 V2", "Atualização da linha 210 com foco em estabilidade industrial.", NeuralPrimary),
    compatibleDrone("DJI Matrice 210 V2 RTK", "Variação RTK da linha 210 V2 para mapeamento técnico.", NeuralSecondary),
    compatibleDrone("DJI Matrice 300 RTK", "Plataforma enterprise robusta para inspeção avançada.", NeuralSuccess),
    compatibleDrone("DJI Matrice 30T", "Drone compacto enterprise com sensor térmico integrado.", NeuralPrimary),
    compatibleDrone("DJI Matrice 600", "Hexacóptero para cargas maiores e aplicações especializadas.", NeuralSecondary),
    compatibleDrone("DJI Matrice 4 / 4T", "Linha enterprise recente para inspeção e operações técnicas.", NeuralSuccess)
)

private fun compatibleDrone(
    name: String,
    description: String,
    accentColor: Color
): CompatibleDrone {
    return CompatibleDrone(
        name = name,
        description = description,
        imageRes = compatibleDroneImageRes(name),
        officialUrl = "https://www.dji.com/br",
        accentColor = accentColor
    )
}

private fun compatibleDroneImageRes(name: String): Int {
    val normalized = name.lowercase()
    return when {
        normalized == "dji air 3" -> R.drawable.drone_dji_air_3
        normalized == "dji mavic air" -> R.drawable.drone_dji_mavic_air
        normalized == "dji mavic air 2" -> R.drawable.drone_dji_mavic_air_2
        normalized == "dji mavic air 2s" -> R.drawable.drone_dji_mavic_air_2s
        normalized == "dji mavic mini" -> R.drawable.drone_dji_mavic_mini
        "matrice" in normalized -> R.drawable.ic_catalog_drone_matrice
        "phantom" in normalized -> R.drawable.ic_catalog_drone_phantom
        "air" in normalized || "mini" in normalized || "spark" in normalized -> R.drawable.ic_catalog_drone_air
        else -> R.drawable.ic_catalog_drone_mavic
    }
}
