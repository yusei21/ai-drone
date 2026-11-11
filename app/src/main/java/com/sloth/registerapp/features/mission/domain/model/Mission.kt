package com.sloth.registerapp.features.mission.domain.model

data class Mission(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val pointOfInterestLatitude: Double,
    val pointOfInterestLongitude: Double,
    val previewPoints: List<MissionPoint>,
    val waypointCount: Int,
    val altitude: Float,
    val autoSpeed: Float,
    val maxSpeed: Float
)

data class MissionPoint(
    val latitude: Double,
    val longitude: Double
)
