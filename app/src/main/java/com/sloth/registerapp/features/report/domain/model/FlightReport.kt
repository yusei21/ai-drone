package com.sloth.registerapp.features.report.domain.model

data class FlightReport(
    val id: String,
    val missionName: String,
    val aircraftName: String,
    val createdAtMs: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationMs: Long,
    val finalObservation: String?,
    val extraData: Map<String, String> = emptyMap()
)

data class FlightReportSession(
    val id: String,
    val missionName: String,
    val aircraftName: String,
    val createdAtMs: Long,
    val startedAtMs: Long,
    val elapsedMs: Long = 0L,
    val extraData: Map<String, String> = emptyMap()
)

