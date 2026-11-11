package com.sloth.registerapp.presentation.mission.model

data class Waypoint(
    var id: Int = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Double = 0.0
)
