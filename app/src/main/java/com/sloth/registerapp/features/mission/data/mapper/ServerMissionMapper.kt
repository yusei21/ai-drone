package com.sloth.registerapp.features.mission.data.mapper

import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.data.remote.dto.WaypointDto
import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.domain.model.MissionPoint

/**
 * Mapper para converter ServerMissionDto (modelo de servidor) para Mission (modelo de UI).
 * Separa responsabilidades e facilita testes unitários.
 */
object ServerMissionMapper {
    fun toDomain(serverMission: ServerMissionDto): Mission {
        val orderedWaypoints = serverMission.waypoints.inDisplayOrder()
        return Mission(
            id = serverMission.id,
            name = serverMission.name,
            latitude = serverMission.poi_latitude,
            longitude = serverMission.poi_longitude,
            pointOfInterestLatitude = serverMission.poi_latitude,
            pointOfInterestLongitude = serverMission.poi_longitude,
            previewPoints = orderedWaypoints.map { waypoint ->
                MissionPoint(
                    latitude = waypoint.latitude,
                    longitude = waypoint.longitude
                )
            },
            waypointCount = orderedWaypoints.size,
            altitude = orderedWaypoints.firstOrNull()?.altitude?.toFloat() ?: 0f,
            autoSpeed = serverMission.auto_flight_speed.toFloat(),
            maxSpeed = serverMission.max_flight_speed.toFloat()
        )
    }

    fun toDomainList(serverMissions: List<ServerMissionDto>): List<Mission> {
        return serverMissions.map { toDomain(it) }
    }
}

fun List<WaypointDto>.inDisplayOrder(): List<WaypointDto> {
    // The server returns waypoints in the correct traversal order via the JSON array.
    // We only sort when the server explicitly provides a dedicated `sequence` field
    // (mapped strictly from the JSON key "sequence", not from alternate names like
    // "order", "index", or "position" which could be DB row IDs, not traversal indices).
    // Preserve server array order in all other cases.
    val hasExplicitSequence = all { it.sequence != null }
    return if (hasExplicitSequence) sortedBy { it.sequence } else this
}
