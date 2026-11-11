package com.sloth.registerapp.presentation.mission.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.utils.MeasurementConverter
import com.sloth.registerapp.features.mission.data.drone.manager.DroneCommandManager

@Composable
fun DroneControlScreen(onMissionsClick: () -> Unit) {
    // TODO: Integrate real drone telemetry from DroneCommandManager
    // TODO: Bind DJI video feed surface texture from camera component
    // TODO: Implement emergency stop button with haptic feedback
    
    val droneController = remember { DroneCommandManager() }
    val context = LocalContext.current
    val measurementRepo = remember { MeasurementSettingsRepository.getInstance(context) }
    val telemetry by droneController.telemetry.collectAsStateWithLifecycle()
    val measurementSystem by measurementRepo.measurementSystem.collectAsStateWithLifecycle(
        initialValue = MeasurementSettingsRepository.SYSTEM_METRIC
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Telemetry summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Alt: ${MeasurementConverter.formatAltitude(telemetry.altitude, measurementSystem)}")
            Text("Vel: ${MeasurementConverter.formatSpeed(telemetry.speed, measurementSystem)}")
            Text("Bateria: ${telemetry.batteryLevel}%")
        }

        // Control Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { droneController.takeOff() }) {
                Text("Take Off")
            }
            Button(onClick = { droneController.land() }) {
                Text("Land")
            }
            Button(onClick = { droneController.returnToHome() }) {
                Text("Return Home")
            }
            Button(onClick = onMissionsClick) {
                Text("Missions")
            }
        }
    }
}
