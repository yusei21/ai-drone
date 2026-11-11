package com.sloth.registerapp.features.mission.domain.model

sealed class FlightCommandResult {
    data class Accepted(val command: FlightCommandType) : FlightCommandResult()
    data class Rejected(
        val command: FlightCommandType,
        val reason: FlightCommandRejectionReason
    ) : FlightCommandResult()
    data class Failed(
        val command: FlightCommandType,
        val error: FlightCommandError
    ) : FlightCommandResult()
}

enum class FlightCommandType {
    TAKE_OFF,
    LAND,
    RETURN_TO_HOME
}

enum class FlightCommandRejectionReason {
    INVALID_STATE,
    NOT_CONNECTED
}

sealed class FlightCommandError {
    data object Timeout : FlightCommandError()
    data class Sdk(val description: String) : FlightCommandError()
}
