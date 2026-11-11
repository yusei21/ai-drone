package com.sloth.registerapp.features.mission.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ServerMissionDto(
    val auto_flight_speed: Double,
    val exit_on_signal_lost: Boolean,
    val finished_action: String,
    val flight_path_mode: String,
    val gimbal_pitch_rotation_enabled: Boolean,
    val goto_first_waypoint_mode: String,
    val heading_mode: String,
    val id: Int,
    val max_flight_speed: Double,
    val name: String,
    val poi_latitude: Double,
    val poi_longitude: Double,
    val repeat_times: Int,
    val waypoints: List<WaypointDto>
)

data class WaypointDto(
    @SerializedName(value = "id", alternate = ["waypoint_id"])
    val waypointId: Int? = null,
    @SerializedName(value = "waypoint_actions", alternate = ["actions"])
    val actions: List<Any>,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val turn_mode: String,
    @SerializedName("sequence")
    val sequence: Int? = null
)

data class MissionCreateRequestDto(
    val auto_flight_speed: Double,
    val exit_on_signal_lost: Boolean,
    val finished_action: Int,
    val flight_path_mode: Int,
    val gimbal_pitch_rotation_enabled: Boolean,
    val goto_first_waypoint_mode: Int,
    val heading_mode: Int,
    val id: Int,
    val max_flight_speed: Double,
    val name: String,
    val poi_latitude: Double,
    val poi_longitude: Double,
    val repeat_times: Int,
    val waypoints: List<WaypointCreateRequestDto>
)

data class WaypointCreateRequestDto(
    @SerializedName("waypoint_actions")
    val actions: List<WaypointActionCreateRequestDto>,
    val altitude: Double,
    val latitude: Double,
    val longitude: Double,
    val turn_mode: Int
)

data class WaypointActionCreateRequestDto(
    val action_type: Int,
    val action_param: Int
)

data class MissionCreateResponseDto(
    val mission_id: Int
)
