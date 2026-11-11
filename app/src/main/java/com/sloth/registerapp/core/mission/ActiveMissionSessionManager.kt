package com.sloth.registerapp.core.mission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ActiveMissionStatus {
    IDLE,
    LOADING,
    READY,
    EXECUTING,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

data class ActiveMissionSession(
    val missionId: String? = null,
    val missionName: String? = null,
    val status: ActiveMissionStatus = ActiveMissionStatus.IDLE
) {
    val hasMission: Boolean
        get() = !missionId.isNullOrBlank()
}

object ActiveMissionSessionManager {
    private val _session = MutableStateFlow(ActiveMissionSession())
    val session: StateFlow<ActiveMissionSession> = _session.asStateFlow()
    private val _activeMissionId = MutableStateFlow<String?>(null)
    val activeMissionId: StateFlow<String?> = _activeMissionId.asStateFlow()

    fun startMissionSession(missionId: String, missionName: String? = null) {
        _activeMissionId.value = missionId
        _session.value = ActiveMissionSession(
            missionId = missionId,
            missionName = missionName,
            status = ActiveMissionStatus.LOADING
        )
    }

    fun updateMissionDetails(missionId: String, missionName: String?) {
        _activeMissionId.value = missionId
        _session.update {
            it.copy(
                missionId = missionId,
                missionName = missionName ?: it.missionName
            )
        }
    }

    fun updateMissionStatus(status: ActiveMissionStatus) {
        _session.update { session ->
            if (!session.hasMission && status != ActiveMissionStatus.IDLE) {
                session
            } else {
                session.copy(status = status)
            }
        }
    }

    fun clearMissionSession() {
        _activeMissionId.value = null
        _session.value = ActiveMissionSession()
    }
}
