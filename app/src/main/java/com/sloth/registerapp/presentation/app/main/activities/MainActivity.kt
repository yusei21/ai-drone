package com.sloth.registerapp.presentation.app.main.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.network.ConnectivityMonitor
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.core.settings.AppThemeSettingsRepository
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.presentation.mission.screens.MissionCreateScreen
import com.sloth.registerapp.presentation.mission.screens.MissionsTableScreen
import com.sloth.registerapp.presentation.app.dashboard.screens.DashboardScreen
import com.sloth.registerapp.presentation.app.dashboard.screens.DronesListScreen
import com.sloth.registerapp.presentation.app.welcome.screens.WelcomeScreen
import com.sloth.registerapp.presentation.mission.screens.DroneControlScreen
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModel
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListViewModelFactory
import com.sloth.registerapp.presentation.mission.viewmodels.MissionListUiState
import com.sloth.registerapp.presentation.app.components.BottomNavBar
import com.sloth.registerapp.presentation.auth.screens.LoginScreen
import com.sloth.registerapp.presentation.auth.screens.RegisterScreen
import com.sloth.registerapp.presentation.mission.activities.MissionControlActivity
import com.sloth.registerapp.presentation.app.report.screens.ReportDetailScreen
import com.sloth.registerapp.presentation.app.report.screens.ReportScreen
import com.sloth.registerapp.presentation.app.settings.screens.AboutScreen
import com.sloth.registerapp.presentation.app.settings.screens.PermissionsScreen
import com.sloth.registerapp.presentation.app.settings.screens.PrivacyPolicyScreen
import com.sloth.registerapp.presentation.app.settings.screens.RecentLoginsScreen
import com.sloth.registerapp.presentation.app.settings.screens.SettingsScreen
import com.sloth.registerapp.presentation.app.weather.screens.WeatherOverviewScreen
import com.sloth.registerapp.presentation.video.activities.DroneCameraActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppObservability.screenOpened("MainActivity")

        // Inicializa o SDK da DJI imediatamente
        runCatching {
            DJIConnectionHelper.registerApp(applicationContext)
        }.onSuccess {
            AppObservability.breadcrumb(
                category = "drone",
                message = "DJI SDK registration requested"
            )
        }.onFailure { error ->
            AppObservability.captureException(
                throwable = error,
                area = "drone",
                operation = "register_dji_sdk"
            )
            throw error
        }

        PermissionHelper.initializeLaunchers(this) {}
        PermissionHelper.checkAndRequestAllPermissions(this) {}
        AppObservability.breadcrumb(
            category = "permissions",
            message = "Initial permission check requested"
        )

        setContent {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            val context = LocalContext.current
            val themeRepository = remember(context) { AppThemeSettingsRepository.getInstance(context) }
            val selectedTheme by themeRepository.appTheme.collectAsState(initial = AppThemeSettingsRepository.DEFAULT_THEME)
            val isDarkTheme = when (selectedTheme) {
                AppThemeSettingsRepository.THEME_LIGHT -> false
                AppThemeSettingsRepository.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            com.sloth.registerapp.presentation.app.theme.AppTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val localSessionManager = remember { LocalSessionManager.getInstance(context) }
                val sessionManager = remember { com.sloth.registerapp.core.auth.SessionManager.getInstance(context) }
                val tokenRepository = remember { com.sloth.registerapp.core.auth.TokenRepository.getInstance(context) }
                val scope = rememberCoroutineScope()
                val currentUserId by localSessionManager.currentUserId.collectAsState(initial = null)
                val guestModeEnabled by localSessionManager.isGuestModeEnabled.collectAsState(initial = false)
                val serverAuthState by localSessionManager.serverAuthState.collectAsState(initial = ServerAuthState.SERVER_AUTH_REQUIRED)
                val userName by localSessionManager.currentUsername.collectAsState(initial = "Usuário")
                val userEmail by localSessionManager.currentEmail.collectAsState(initial = "usuario@labubu.com")
                val accessToken by tokenRepository.accessToken.collectAsState(initial = null)
                val isSessionActive by sessionManager.isSessionActive.collectAsState(initial = true)
                val startDestination = if (!currentUserId.isNullOrBlank() || !accessToken.isNullOrBlank() || guestModeEnabled) "dashboard" else "welcome"
                val isLoggedIn = !currentUserId.isNullOrBlank() && !guestModeEnabled

                LaunchedEffect(accessToken, isSessionActive, currentUserId, guestModeEnabled) {
                    if (!guestModeEnabled &&
                        !accessToken.isNullOrBlank() &&
                        !isSessionActive &&
                        currentUserId.isNullOrBlank()
                    ) {
                        tokenRepository.clearTokens()
                    }
                }

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    destination.route?.let { route ->
                        AppObservability.screenOpened(route)
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("welcome") {
                        WelcomeScreen(
                            onLoginClick = { navController.navigate("login") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            onRegisterClick = { navController.navigate("register") },
                            onSkipClick = {
                                scope.launch {
                                    localSessionManager.setGuestModeEnabled(true)
                                    navController.navigate("dashboard") {
                                        popUpTo("welcome") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("login") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("drone_control") {
                        DroneControlScreen(onMissionsClick = {
                            // TODO: Navigate to missions screen
                        })
                    }
                    composable("dashboard") {
                        val droneStatus by DJIConnectionHelper.connectionStatus.collectAsState()
                        ScreenWithBottomBar(navController = navController) {
                            DashboardScreen(
                                droneStatus = droneStatus,
                                userName = userName ?: "Usuário",
                                isLoggedIn = isLoggedIn,
                                onShowAllDronesClick = { navController.navigate("drones_list") },
                                onWeatherClick = { navController.navigate("weather_overview") },
                                onRefreshStatusClick = {
                                    AppObservability.breadcrumb(
                                        category = "drone",
                                        message = "Manual drone reconnect requested",
                                        data = mapOf("screen" to "dashboard")
                                    )
                                    DJIConnectionHelper.tryReconnect(context)
                                },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                    }
                    composable("drones_list") {
                        DronesListScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("weather_overview") {
                        WeatherOverviewScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            userName = userName ?: "Usuário",
                            userEmail = userEmail ?: "usuario@labubu.com",
                            isLoggedIn = isLoggedIn,
                            selectedTheme = selectedTheme,
                            onBackClick = { navController.popBackStack() },
                            onRecentLogins = { navController.navigate("recent_logins") },
                            onManagePermissions = { navController.navigate("permissions") },
                            onAbout = { navController.navigate("about") },
                            onPrivacyPolicy = { navController.navigate("privacy_policy") },
                            onThemeChange = { theme ->
                                scope.launch {
                                    themeRepository.setAppTheme(theme)
                                }
                            },
                            onLoginClick = {
                                navController.navigate("login") {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = {
                                scope.launch {
                                    tokenRepository.clearTokens()
                                    sessionManager.clearSession()
                                    localSessionManager.logoutLocal()
                                    navController.navigate("welcome") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("recent_logins") {
                        RecentLoginsScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("permissions") {
                        PermissionsScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("about") {
                        AboutScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("privacy_policy") {
                        PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("mission") {
                        val viewModel: MissionListViewModel = viewModel(factory = MissionListViewModelFactory(application))
                        val uiState by viewModel.uiState.collectAsState()
                        val canCreateMission = !currentUserId.isNullOrBlank()
                        val isConnected by remember { ConnectivityMonitor.getInstance(context) }
                            .isConnected
                            .collectAsState(initial = true)
                        val createBlockedMessage = when {
                            !canCreateMission -> "Operação local disponível. Faça login para sincronizar com servidor."
                            !isConnected -> "Sem internet, trabalhando localmente."
                            uiState is MissionListUiState.Unauthorized || serverAuthState == ServerAuthState.SERVER_AUTH_REQUIRED ->
                                "Faça login para sincronizar com servidor."
                            else -> null
                        }

                        // Buscar missões quando entrar na tela
                        LaunchedEffect(Unit) {
                            viewModel.fetchMissions()
                        }

                        val (missions, isLoading) = remember(uiState) {
                            when (uiState) {
                                is MissionListUiState.Idle -> emptyList<Mission>() to false
                                is MissionListUiState.Loading -> emptyList<Mission>() to true
                                is MissionListUiState.Success -> {
                                    val data = uiState as MissionListUiState.Success
                                    data.missions to false
                                }
                                is MissionListUiState.Error -> emptyList<Mission>() to false
                                is MissionListUiState.Unauthorized -> emptyList<Mission>() to false
                            }
                        }

                        ScreenWithBottomBar(navController = navController) {
                            MissionsTableScreen(
                                missions = missions,
                                isLoading = isLoading,
                                canCreateMission = canCreateMission,
                                createBlockedMessage = createBlockedMessage,
                                onBackClick = { navController.popBackStack() },
                                onCreateMissionClick = {
                                    if (canCreateMission) {
                                        navController.navigate("mission-create")
                                    } else {
                                        navController.navigate("login") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onViewMissionClick = { missionId ->
                                    AppObservability.breadcrumb(
                                        category = "mission",
                                        message = "Mission control opened",
                                        data = mapOf("mission_id" to missionId)
                                    )
                                    val intent = Intent(context, MissionControlActivity::class.java).apply {
                                        putExtra("MISSION_ID", missionId)
                                    }
                                    context.startActivity(intent)
                                },
                                onDeleteMissionClick = { missionId ->
                                    viewModel.deleteMission(missionId)
                                }
                            )
                        }
                    }
                    composable("mission-create") {
                        MissionCreateScreen(onBackClick = { navController.popBackStack() })
                    }
                    composable("report") {
                        ScreenWithBottomBar(navController = navController) {
                            ReportScreen(
                                onBackClick = { navController.popBackStack() },
                                onMissionClick = { missionId ->
                                    navController.navigate("report_detail/$missionId")
                                }
                            )
                        }
                    }
                    composable("report_detail/{missionId}") { backStackEntry ->
                        val missionId = backStackEntry.arguments?.getString("missionId") ?: "unknown"
                        ScreenWithBottomBar(navController = navController) {
                            ReportDetailScreen(
                                missionId = missionId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenWithBottomBar(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onLiveFeedClick = {
                    AppObservability.breadcrumb(
                        category = "video",
                        message = "Drone live feed opened"
                    )
                    val intent = Intent(context, DroneCameraActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
