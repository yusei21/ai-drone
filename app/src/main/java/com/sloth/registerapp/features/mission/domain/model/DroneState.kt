package com.sloth.registerapp.features.mission.domain.model

// DroneState.kt
enum class DroneState {
    DISCONNECTED,
    ON_GROUND,  // Conectado, no chão, motores desligados
    IN_AIR,     // Voando
    TAKING_OFF, // No processo de decolagem
    EMERGENCY_STOP,     // Parada de emergência
    LANDING,
    GOING_HOME,
    ERROR
}