package com.sloth.registerapp.features.mission.domain.usecase

import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.MissionOutcome
import kotlinx.coroutines.flow.StateFlow

/**
 * Use Case para execução de missões no drone.
 * Coordena a preparação, upload e execução de missões.
 */
class ExecuteMissionUseCase(
    private val droneMissionManager: DroneMissionManager
) {
    /**
     * Estado atual da missão
     */
    val missionState: StateFlow<MissionState> = droneMissionManager.missionState
    val missionOutcome: StateFlow<MissionOutcome?> = droneMissionManager.missionOutcome

    /**
     * Prepara e faz upload da missão para o drone
     * @param mission Dados da missão a executar
     */
    suspend fun prepareMission(mission: ServerMissionDto) {
        droneMissionManager.prepareAndUploadMission(mission)
    }

    /**
     * Inicia a execução da missão no drone
     */
    suspend fun startMission() {
        droneMissionManager.startMission()
    }

    /**
     * Para a execução da missão
     */
    suspend fun stopMission() {
        droneMissionManager.stopMission()
    }

    /**
     * Pausa a execução da missão
     */
    suspend fun pauseMission() {
        droneMissionManager.pauseMission()
    }

    /**
     * Retoma a execução da missão pausada
     */
    suspend fun resumeMission() {
        droneMissionManager.resumeMission()
    }

    /**
     * Verifica se há uma missão ativa
     */
    fun hasMissionActive(): Boolean {
        return missionState.value != MissionState.IDLE
    }
}
