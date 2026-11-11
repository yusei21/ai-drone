package com.sloth.registerapp.features.mission.domain.usecase

import com.sloth.registerapp.features.mission.domain.model.Mission
import com.sloth.registerapp.features.mission.domain.repository.MissionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use Case para sincronização de missões com o servidor.
 * Coordena atualizações em tempo real via WebSocket.
 */
class SyncMissionUseCase(
    private val repository: MissionRepository
) {
    /**
     * Inicia sincronização em tempo real de missões
     * @return Flow de missões atualizadas
     */
    fun startSync(): Flow<Mission> {
        return repository.listenMissionUpdates()
    }

    /**
     * Sincroniza estado local com o servidor
     * Busca missões do servidor e compara com estado local
     */
    suspend fun syncWithServer(): Result<List<Mission>> {
        return try {
            val serverMissions = repository.getMissions()
            
            serverMissions.onSuccess { missions ->
                // TODO: Comparar com missões locais (se houver cache)
                // TODO: Resolver conflitos
                // Por enquanto apenas retorna as missões do servidor
            }
            
            serverMissions
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Força uma sincronização completa
     * Útil para resolver inconsistências
     */
    suspend fun forceSync(): Result<List<Mission>> {
        // TODO: Limpar cache local se existir
        return syncWithServer()
    }
}
