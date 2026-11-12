package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.core.settings.MediaStorageSettingsRepository
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.features.mission.data.manager.MissionStorageManager
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Usuário",
    userEmail: String = "usuario@email.com",
    isLoggedIn: Boolean = false,
    selectedTheme: String = "Padrão do Sistema",
    onBackClick: () -> Unit = {},
    onChangeProfilePhoto: () -> Unit = {},
    onChangeUsername: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onRecentLogins: () -> Unit = {},
    onManagePermissions: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf(true) }
    var streamQuality by remember { mutableStateOf("Automática") }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rtmpRepo = remember(context) { RtmpSettingsRepository.getInstance(context) }
    val mediaStorageRepo = remember(context) { MediaStorageSettingsRepository.getInstance(context) }
    val measurementRepo = remember(context) { MeasurementSettingsRepository.getInstance(context) }
    val weatherProviderRepo = remember(context) { WeatherProviderSettingsRepository.getInstance(context) }
    val missionStorageManager = remember(context) { MissionStorageManager.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsState(initial = RtmpSettingsRepository.DEFAULT_URL)
    val mediaStorageTarget by mediaStorageRepo.mediaStorageTarget.collectAsState(initial = MediaStorageSettingsRepository.TARGET_PHONE)
    val measurementSystem by measurementRepo.measurementSystem.collectAsState(initial = MeasurementSettingsRepository.SYSTEM_METRIC)
    val weatherPrimary by weatherProviderRepo.primaryProvider.collectAsState(initial = WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
    val weatherFallback by weatherProviderRepo.fallbackProvider.collectAsState(initial = WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
    var rtmpUrlInput by remember { mutableStateOf(rtmpUrl) }
    val hasWeatherApiKey = remember { BuildConfig.WEATHER_API_KEY.isNotBlank() }

    LaunchedEffect(rtmpUrl) {
        if (rtmpUrlInput != rtmpUrl) rtmpUrlInput = rtmpUrl
    }
    LaunchedEffect(Unit) {
        cacheSizeBytes = missionStorageManager.getMissionCacheSizeBytes()
    }
    LaunchedEffect(hasWeatherApiKey, weatherPrimary, weatherFallback) {
        if (!hasWeatherApiKey) {
            if (weatherPrimary == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
                weatherProviderRepo.setPrimaryProvider(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
            if (weatherFallback == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
                weatherProviderRepo.setFallbackProvider(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.Bg)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Surface,
                    border = BorderStroke(1.dp, NeuralColors.Border)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Configurações",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Personalize o sistema",
                        fontSize = 11.sp,
                        color = NeuralColors.Muted
                    )
                }

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.28f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Thin separator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, NeuralColors.BorderStrong, Color.Transparent)
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                if (isLoggedIn) {
                    item {
                        NeuralProfileCard(
                            userName = userName,
                            userEmail = userEmail,
                            onChangeProfilePhoto = onChangeProfilePhoto
                        )
                    }

                    item {
                        ExpandableSection(
                            title = "Conta e Segurança",
                            icon = Icons.Default.Security,
                            isExpanded = expandedSection == "account",
                            onToggle = { expandedSection = if (expandedSection == "account") null else "account" }
                        ) {
                            SettingsItem(Icons.Default.Person, "Alterar Nome de Usuário", userName, onChangeUsername)
                            SettingsItem(Icons.Default.Lock, "Alterar Senha", "••••••••", onChangePassword)
                            SettingsItem(Icons.Default.History, "Últimos Logins", "Histórico de acessos", onRecentLogins)
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = "Armazenamento e Mídia",
                        icon = Icons.Default.Storage,
                        isExpanded = expandedSection == "storage",
                        onToggle = { expandedSection = if (expandedSection == "storage") null else "storage" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.Folder,
                            title = "Local de Salvamento",
                            options = listOf("Celular", "SD do Drone"),
                            selectedOption = mediaStorageTarget.toStorageLabel(),
                            onOptionSelected = { label ->
                                scope.launch { mediaStorageRepo.setMediaStorageTarget(label.toStorageTarget()) }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CleaningServices,
                            title = "Limpar missões no dispositivo",
                            subtitle = "Uso atual: ${cacheSizeBytes.toReadableBytes()}",
                            onClick = { showClearCacheDialog = true }
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Configurações Gerais",
                        icon = Icons.Default.Tune,
                        isExpanded = expandedSection == "general",
                        onToggle = { expandedSection = if (expandedSection == "general") null else "general" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.Palette,
                            title = "Tema do Aplicativo",
                            options = listOf("Claro", "Escuro", "Padrão do Sistema"),
                            selectedOption = selectedTheme,
                            onOptionSelected = onThemeChange
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.Speed,
                            title = "Unidades de Medida",
                            options = listOf("Métrico (m, km/h)", "Imperial (ft, mph)"),
                            selectedOption = measurementSystem.toMeasurementLabel(),
                            onOptionSelected = { label ->
                                scope.launch { measurementRepo.setMeasurementSystem(label.toMeasurementSystem()) }
                            }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.Notifications,
                            title = "Notificações",
                            subtitle = if (notifications) "Ativadas" else "Desativadas",
                            checked = notifications,
                            onCheckedChange = { notifications = it }
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Configurações Climáticas",
                        icon = Icons.Default.Cloud,
                        isExpanded = expandedSection == "weather",
                        onToggle = { expandedSection = if (expandedSection == "weather") null else "weather" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.FilterDrama,
                            title = "Provider Principal",
                            options = weatherProviderOptions(hasWeatherApiKey),
                            selectedOption = weatherPrimary.toWeatherProviderLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    val selected = label.toWeatherProviderId()
                                    weatherProviderRepo.setPrimaryProvider(selected)
                                    if (selected == weatherFallback)
                                        weatherProviderRepo.setFallbackProvider(selected.alternativeWeatherProvider())
                                }
                            }
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.SwapHoriz,
                            title = "Provider Fallback",
                            options = weatherProviderOptions(hasWeatherApiKey),
                            selectedOption = weatherFallback.toWeatherProviderLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    val selected = label.toWeatherProviderId()
                                    weatherProviderRepo.setFallbackProvider(selected)
                                    if (selected == weatherPrimary)
                                        weatherProviderRepo.setPrimaryProvider(selected.alternativeWeatherProvider())
                                }
                            }
                        )
                        NeuralInfoText(
                            if (!hasWeatherApiKey)
                                "WeatherAPI desativada: chave não configurada. Usando Open-Meteo."
                            else
                                "Chave WeatherAPI detectada. Fallback ativado em caso de erro da WeatherAPI."
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Configurações da Câmera",
                        icon = Icons.Default.Videocam,
                        isExpanded = expandedSection == "camera",
                        onToggle = { expandedSection = if (expandedSection == "camera") null else "camera" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.VideoSettings,
                            title = "Qualidade da Transmissão",
                            options = listOf("Automática", "Alta Definição (HD)", "Fluida"),
                            selectedOption = streamQuality,
                            onOptionSelected = { streamQuality = it }
                        )
                    }
                }

                item {
                    ExpandableSection(
                        title = "Streaming RTMP",
                        icon = Icons.Default.Cast,
                        isExpanded = expandedSection == "rtmp",
                        onToggle = { expandedSection = if (expandedSection == "rtmp") null else "rtmp" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = rtmpUrlInput,
                                onValueChange = { rtmpUrlInput = it },
                                label = { Text("URL RTMP", color = NeuralColors.Muted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeuralColors.Primary,
                                    unfocusedBorderColor = NeuralColors.Border,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.18f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
                                    cursorColor = NeuralColors.Primary
                                )
                            )
                            Button(
                                onClick = { scope.launch { rtmpRepo.setRtmpUrl(rtmpUrlInput.trim()) } },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Primary)
                            ) {
                                Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            NeuralInfoText("Atual: $rtmpUrl")
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = "Permissões e Sobre",
                        icon = Icons.Default.Info,
                        isExpanded = expandedSection == "about",
                        onToggle = { expandedSection = if (expandedSection == "about") null else "about" }
                    ) {
                        SettingsItem(Icons.Default.Security, "Gerenciar Permissões", "Configurações do sistema", onManagePermissions)
                        SettingsItem(Icons.Default.Info, "Sobre o Aplicativo", "Versão ${BuildConfig.VERSION_NAME}", onAbout)
                        SettingsItem(Icons.Default.Policy, "Política de Privacidade", "Termos e condições", onPrivacyPolicy)
                    }
                }

                item {
                    Button(
                        onClick = { if (isLoggedIn) onLogout() else onLoginClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        if (isLoggedIn)
                                            listOf(NeuralColors.Danger, Color(0xFF8B0000))
                                        else
                                            listOf(NeuralColors.Primary, NeuralColors.Secondary)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isLoggedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isLoggedIn) "Sair da Conta" else "Realizar Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = NeuralColors.Surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeuralColors.Warning)
                    Spacer(Modifier.width(8.dp))
                    Text("Limpar missões locais", color = Color.White)
                }
            },
            text = {
                Text(
                    "Remove as missões salvas neste dispositivo (${cacheSizeBytes.toReadableBytes()}). Esta ação não pode ser desfeita.",
                    color = NeuralColors.Muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            missionStorageManager.clearMissionCache()
                            cacheSizeBytes = missionStorageManager.getMissionCacheSizeBytes()
                            showClearCacheDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Danger)
                ) { Text("Limpar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancelar", color = NeuralColors.Muted)
                }
            }
        )
    }
}

@Composable
private fun NeuralProfileCard(
    userName: String,
    userEmail: String,
    onChangeProfilePhoto: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(NeuralColors.Primary.copy(alpha = 0.08f), NeuralColors.Surface)
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onChangeProfilePhoto,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = NeuralColors.Primary.copy(alpha = 0.15f),
                border = BorderStroke(2.dp, NeuralColors.Primary.copy(alpha = 0.50f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeuralColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(text = userEmail, fontSize = 13.sp, color = NeuralColors.Muted)
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = NeuralColors.Success.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NeuralColors.Success.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeuralColors.Success, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Autenticado",
                            color = NeuralColors.Success,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(
            1.dp,
            if (isExpanded) NeuralColors.Primary.copy(alpha = 0.35f) else NeuralColors.Border
        )
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuralColors.Primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.20f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = NeuralColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeuralColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, NeuralColors.BorderStrong, Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.SurfaceAlt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.Primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeuralColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = subtitle, fontSize = 12.sp, color = NeuralColors.Muted)
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeuralColors.Muted.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.SurfaceAlt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.Primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeuralColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = subtitle, fontSize = 12.sp, color = NeuralColors.Muted)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeuralColors.Primary,
                    uncheckedThumbColor = NeuralColors.Muted,
                    uncheckedTrackColor = NeuralColors.Border
                )
            )
        }
    }
}

@Composable
fun SettingsDropdownItem(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.SurfaceAlt
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(text = selectedOption, fontSize = 12.sp, color = NeuralColors.Primary)
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = NeuralColors.Muted,
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(NeuralColors.Surface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedOption) NeuralColors.Primary else Color.White
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeuralInfoText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = NeuralColors.Muted,
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuralColors.Bg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

private fun Long.toReadableBytes(): String {
    if (this <= 0L) return "0 B"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
}

private fun weatherProviderOptions(hasWeatherApiKey: Boolean): List<String> {
    return if (hasWeatherApiKey) {
        listOf(
            WeatherProviderSettingsRepository.PROVIDER_WEATHER_API.toWeatherProviderLabel(),
            WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO.toWeatherProviderLabel()
        )
    } else {
        listOf(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO.toWeatherProviderLabel())
    }
}

private fun String.toWeatherProviderLabel(): String = when (this) {
    WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO -> "Open-Meteo (grátis)"
    else -> "WeatherAPI (principal)"
}

private fun String.toWeatherProviderId(): String = when {
    startsWith("Open-Meteo") -> WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO
    else -> WeatherProviderSettingsRepository.PROVIDER_WEATHER_API
}

private fun String.alternativeWeatherProvider(): String =
    if (this == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
        WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO
    else WeatherProviderSettingsRepository.PROVIDER_WEATHER_API

private fun String.toStorageLabel(): String = when (this) {
    MediaStorageSettingsRepository.TARGET_DRONE_SD -> "SD do Drone"
    else -> "Celular"
}

private fun String.toStorageTarget(): String =
    if (this == "SD do Drone") MediaStorageSettingsRepository.TARGET_DRONE_SD
    else MediaStorageSettingsRepository.TARGET_PHONE

private fun String.toMeasurementLabel(): String =
    if (this == MeasurementSettingsRepository.SYSTEM_IMPERIAL) "Imperial (ft, mph)" else "Métrico (m, km/h)"

private fun String.toMeasurementSystem(): String =
    if (startsWith("Imperial")) MeasurementSettingsRepository.SYSTEM_IMPERIAL
    else MeasurementSettingsRepository.SYSTEM_METRIC
