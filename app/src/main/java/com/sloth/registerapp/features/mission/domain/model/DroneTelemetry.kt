package com.sloth.registerapp.features.mission.domain.model

data class DroneTelemetry(
    val altitude: Float = 0f,              // Altitude em metros
    val speed: Float = 0f,                 // Velocidade em m/s
    val distanceFromHome: Float = 0f,      // Distância da base em metros
    val gpsSatellites: Int = 0,            // Quantidade de satélites GPS
    val batteryLevel: Int = 0,             // Bateria em %
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isFlying: Boolean = false
)
