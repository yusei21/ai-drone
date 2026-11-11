package com.sloth.registerapp.features.mission.domain.model

enum class MissionOutcomeStatus {
    COMPLETED,
    ABORTED,
    FAILED
}

data class MissionOutcome(
    val status: MissionOutcomeStatus,
    val message: String,
    val executionMode: MissionExecutionMode = MissionExecutionMode.UNKNOWN,
    val errorCode: Int? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
