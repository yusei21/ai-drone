package com.sloth.registerapp.presentation.app.weather.screens

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.features.weather.data.repository.WeatherModule
import com.sloth.registerapp.features.weather.domain.model.WindProfileLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSummary
import com.sloth.registerapp.presentation.mission.viewmodels.OperatorLocationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import com.sloth.registerapp.presentation.app.theme.NeuralColors

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherOverviewScreen(
    onBackClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val weatherModule = remember(context) { WeatherModule.getInstance(context) }
    val providerSettings = remember(context) { WeatherProviderSettingsRepository.getInstance(context) }
    val selectedPrimaryProvider by providerSettings.primaryProvider.collectAsStateWithLifecycle(
        initialValue = WeatherProviderSettingsRepository.PROVIDER_WEATHER_API
    )

    val locationViewModel: OperatorLocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var summary by remember { mutableStateOf<WeatherSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var lastWeatherRequestMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        locationViewModel.setPermissionGranted(locationPermissions.allPermissionsGranted)
    }

    LaunchedEffect(locationUiState.location) {
        val location = locationUiState.location ?: run {
            isLoading = false
            return@LaunchedEffect
        }

        val lat = location.latitude()
        val lon = location.longitude()
        val now = System.currentTimeMillis()
        if (now - lastWeatherRequestMs < WEATHER_MIN_REFRESH_MS) {
            return@LaunchedEffect
        }

        val firstLoad = summary == null
        isLoading = firstLoad
        isRefreshing = !firstLoad
        errorText = null

        val latestSummary = runCatching {
            weatherModule.getWeatherSummaryUseCase(
                lat = lat,
                lon = lon,
                ttlMs = WEATHER_OVERVIEW_TTL_MS
            )
        }.onFailure {
            errorText = "Falha ao carregar dados climáticos"
        }.getOrNull()

        if (latestSummary != null) {
            summary = latestSummary
            lastWeatherRequestMs = now
        }

        isLoading = false
        isRefreshing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralBg)
    ) {
        WeatherGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            WeatherTopBar(onBackClick = onBackClick)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    when {
                        isLoading -> WeatherStateCard(
                            title = "Carregando condições",
                            body = "Buscando dados meteorológicos para apoiar a operação do drone.",
                            trailing = {
                                CircularProgressIndicator(
                                    color = NeuralPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        )

                        errorText != null -> WeatherStateCard(
                            title = "Falha na leitura climática",
                            body = errorText!!,
                            accent = NeuralDanger
                        )

                        summary == null -> WeatherStateCard(
                            title = "Aguardando localização",
                            body = "Permita a localização do operador para liberar as condições climáticas atuais.",
                            accent = NeuralWarning
                        )

                        else -> {
                            val data = summary!!
                            val snapshot = data.snapshot
                            val windProfiles = snapshot.windProfiles.filter { it.speedMs != null }
                            val lightningPercent = (snapshot.lightningRisk ?: 0.0) * 100.0

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (isRefreshing) {
                                    WeatherInlineLabel("Atualizando clima...")
                                }

                                WeatherHeroCard(summary = data)

                                WeatherSectionTitle("Panorama atual")
                                CurrentFlightMetricsRow(
                                    windKmh = snapshot.windSpeedMs.toKmh(),
                                    rainNowMm = snapshot.rainMm ?: 0.0,
                                    lightningPercent = lightningPercent
                                )

                                WeatherInfoCard(
                                    title = "Leitura operacional",
                                    rows = listOf(
                                        "Temperatura" to snapshot.temperatureC.toTemperatureLabel(),
                                        "Vento base" to snapshot.windSpeedMs.toKmhLabel(defaultLabel = "N/D"),
                                        "Rajadas" to snapshot.windGustMs.toKmhLabel(defaultLabel = "N/D"),
                                        "Chuva agora" to snapshot.rainMm.toRainLabel(),
                                        "Risco de raio" to "${lightningPercent.roundToInt()}%",
                                        "Fonte" to snapshot.providerId
                                    )
                                )

                                if (windProfiles.size > 1) {
                                    WeatherSectionTitle("Vento por altitude")
                                    WindProfileCard(windProfiles = windProfiles)
                                }

                                WeatherInlineLabel(
                                    "Atualizado às ${snapshot.timestampMs.toHourLabel()}",
                                    highlight = "provider ${snapshot.providerId.lowercase()}"
                                )

                                if (selectedPrimaryProvider != snapshot.providerId) {
                                    WeatherProviderFallbackAlert(
                                        selectedPrimaryProvider = selectedPrimaryProvider,
                                        currentProvider = snapshot.providerId
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
private fun WeatherTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary))),
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
                text = "Condições Climáticas",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(NeuralPrimary, NeuralSecondary)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Leitura operacional para voo",
                color = NeuralMuted,
                fontSize = 10.sp
            )
        }

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = NeuralSurface,
            border = BorderStroke(1.dp, NeuralBorder)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = NeuralMuted
                )
            }
        }
    }
}

@Composable
private fun WeatherHeroCard(summary: WeatherSummary) {
    val snapshot = summary.snapshot
    val (icon, accent, statusTag) = when (summary.decision.level) {
        WeatherSafetyLevel.SAFE -> Triple(Icons.Default.WbSunny, NeuralSuccess, "VOO LIBERADO")
        WeatherSafetyLevel.CAUTION -> Triple(Icons.Default.Cloud, NeuralWarning, "ATENCAO")
        WeatherSafetyLevel.NO_FLY -> Triple(Icons.Default.Thunderstorm, NeuralDanger, "NAO DECOLAR")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(NeuralSurfaceAlt, accent.copy(alpha = 0.18f), NeuralSurface)
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .border(1.dp, accent.copy(alpha = 0.24f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusTag,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = summary.decision.shortMessage,
                        color = Color.White,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = summary.decision.reasons.joinToString(" • "),
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "${snapshot.temperatureC.toTemperatureLabel()} • ${snapshot.windSpeedMs.toKmhLabel()}",
                        color = NeuralMuted,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = accent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(18.dp).size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentFlightMetricsRow(
    windKmh: Double,
    rainNowMm: Double,
    lightningPercent: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WeatherPulseMetricCard(
            modifier = Modifier.weight(1f),
            title = "Vento",
            value = "${windKmh.roundToInt()} km/h",
            accent = if (windKmh >= 36.0) NeuralDanger else NeuralPrimary
        )
        WeatherPulseMetricCard(
            modifier = Modifier.weight(1f),
            title = "Chuva",
            value = "${"%.1f".format(rainNowMm)} mm",
            accent = if (rainNowMm >= 0.5) NeuralWarning else NeuralPrimary
        )
        WeatherPulseMetricCard(
            modifier = Modifier.weight(1f),
            title = "Raio",
            value = "${lightningPercent.roundToInt()}%",
            accent = if (lightningPercent >= 70.0) NeuralDanger else NeuralPrimary
        )
    }
}

@Composable
private fun WeatherPulseMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = NeuralSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = NeuralMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.15f))),
                        androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun WeatherInfoCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = NeuralSurface,
        border = BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WeatherSectionTitle(title)
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index != rows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeuralBorderStrong, Color.Transparent)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WindProfileCard(windProfiles: List<WindProfileLevel>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = NeuralSurface,
        border = BorderStroke(1.dp, NeuralBorderStrong)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Perfis verticais disponíveis na API atual. Se o provider ativo não oferecer múltiplas alturas, este bloco fica oculto.",
                color = NeuralMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            windProfiles.forEach { level ->
                val speedKmh = (level.speedMs ?: 0.0) * 3.6
                val accent = when {
                    speedKmh >= 45.0 -> NeuralDanger
                    speedKmh >= 30.0 -> NeuralWarning
                    else -> NeuralPrimary
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = accent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AirplanemodeActive,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${level.heightMeters}m",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = level.speedMs.toKmhLabel(defaultLabel = "N/D"),
                            color = accent,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherProviderFallbackAlert(
    selectedPrimaryProvider: String,
    currentProvider: String
) {
    WeatherStateCard(
        title = "Fallback ativo",
        body = "O provider configurado $selectedPrimaryProvider falhou e a leitura atual veio de $currentProvider.",
        accent = NeuralWarning
    )
}

@Composable
private fun WeatherStateCard(
    title: String,
    body: String,
    accent: Color = NeuralPrimary,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = NeuralSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun WeatherSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = NeuralPrimary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
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
                .background(Brush.horizontalGradient(listOf(NeuralBorderStrong, Color.Transparent)))
        )
    }
}

@Composable
private fun WeatherInlineLabel(
    text: String,
    highlight: String? = null
) {
    val displayText = if (highlight == null) text else "$text • $highlight"
    Text(
        text = displayText,
        color = NeuralMuted,
        fontSize = 12.sp
    )
}

@Composable
private fun WeatherGridBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeuralBg,
                        NeuralSurfaceAlt.copy(alpha = 0.94f),
                        NeuralBg
                    )
                )
            )
    )
}

private fun Double?.toKmh(): Double = (this ?: 0.0) * 3.6

private fun Double?.toKmhLabel(defaultLabel: String = "0 km/h"): String {
    val value = this ?: return defaultLabel
    return "${value.times(3.6).roundToInt()} km/h"
}

private fun Double?.toTemperatureLabel(): String {
    val value = this ?: return "N/D"
    return "${value.roundToInt()}°C"
}

private fun Double?.toRainLabel(): String {
    val value = this ?: return "N/D"
    return "${"%.1f".format(value)} mm"
}

private fun Long.toHourLabel(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

private const val WEATHER_MIN_REFRESH_MS = 5 * 60_000L
private const val WEATHER_OVERVIEW_TTL_MS = 5 * 60_000L
