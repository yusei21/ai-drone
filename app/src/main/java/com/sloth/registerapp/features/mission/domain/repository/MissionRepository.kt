package com.sloth.registerapp.features.mission.domain.repository

import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.Mission
import kotlinx.coroutines.flow.Flow

/**
 * Interface de abstração para acesso a dados de missões.
 * Define o contrato que a camada de dados deve implementar.
 * Agnostic de fonte (API, WebSocket, BD local, etc)
 */
interface MissionRepository {
    
    /**
     * Obtém todas as missões do servidor
     * @return Result com lista de missões ou erro
     */
    suspend fun getMissions(): Result<List<Mission>>
    
    /**
     * Obtém uma missão específica pelo ID
     * @param id ID da missão
     * @return Result com a missão ou null se não encontrada
     */
    suspend fun getMission(id: Int): Result<ServerMissionDto?>
    
    /**
     * Observa atualizações de missões em tempo real (WebSocket)
     * @return Flow de atualizações de missão
     */
    fun listenMissionUpdates(): Flow<Mission>
    
    /**
     * Envia uma missão para o servidor
     * @param mission Dados da missão a enviar
     * @return Result com a resposta do servidor
     */
    suspend fun uploadMission(mission: ServerMissionDto): Result<ServerMissionDto>

    /**
     * Remove uma missão do cache local e dos artefatos associados.
     * Quando disponível no futuro, a sincronização remota pode ser adicionada aqui.
     */
    suspend fun deleteMission(id: Int): Result<Unit>
}
