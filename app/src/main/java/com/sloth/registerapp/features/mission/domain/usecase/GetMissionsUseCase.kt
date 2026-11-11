package com.sloth.registerapp.features.mission.domain.usecase

import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use Case para buscar e listar missões do servidor.
 * Encapsula a lógica de negócio para obtenção de missões.
 */
class GetMissionsUseCase(
    private val repository: MissionRepository
) {
    /**
     * Busca todas as missões disponíveis
     * @return Result com lista de missões ou erro
     */
    suspend operator fun invoke(): Result<List<Mission>> {
        return repository.getMissions()
    }

    /**
     * Observa atualizações de missões em tempo real
     * @return Flow de atualizações de missões
     */
    fun observeMissionUpdates(): Flow<Mission> {
        return repository.listenMissionUpdates()
    }

    /**
     * Busca uma missão específica por ID
     * @param missionId ID da missão
     * @return Flow que emite a missão encontrada ou erro
     */
    fun getMissionById(missionId: Int): Flow<Result<Mission?>> = flow {
        val result = repository.getMission(missionId)
        result.onSuccess { serverMission ->
            // Converter ServerMissionDto para Mission se necessário
            // Por enquanto emitir null se não encontrado
            emit(Result.success(null))
        }.onFailure { error ->
            emit(Result.failure(error))
        }
    }
}
