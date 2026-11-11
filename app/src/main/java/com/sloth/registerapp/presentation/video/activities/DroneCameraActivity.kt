package com.sloth.registerapp.presentation.video.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sloth.registerapp.presentation.video.screens.CellCameraScreen
import com.sloth.registerapp.presentation.video.screens.DroneCameraScreen

class DroneCameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Recupera o tema do SettingsScreen (pode ser via ViewModel, DataStore, etc. - aqui exemplo simples)
            var theme by remember { mutableStateOf("Escuro") }
            val isDarkTheme = when (theme) {
                "Claro" -> false
                "Escuro" -> true
                else -> isSystemInDarkTheme()
            }
            com.sloth.registerapp.presentation.app.theme.AppTheme(darkTheme = isDarkTheme) {
                var showCellCamera by remember { mutableStateOf(false) }
                if (showCellCamera) {
                    CellCameraScreen(onBackClick = { showCellCamera = false })
                } else {
                    DroneCameraScreen(
                        droneController = remember { com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager() },
                        onCellCameraClick = { showCellCamera = true },
                        onSurfaceTextureAvailable = { _, _, _ -> /* TODO: bind texture to DJI feed */ },
                        onSurfaceTextureDestroyed = { false },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
