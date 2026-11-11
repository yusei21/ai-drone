package com.sloth.registerapp.features.mission.domain.usecase

import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.repository.MissionRepository

/**
 * Use Case para criação de novas missões.
 * Contém validações de negócio antes de enviar ao servidor.
 */
class CreateMissionUseCase(
    private val repository: MissionRepository
) {
    /**
     * Cria uma nova missão após validações
     * @param mission Dados da missão a criar
     * @return Result com a missão criada ou erro
     */
    suspend operator fun invoke(mission: ServerMissionDto): Result<ServerMissionDto> {
        // Validações de negócio
        val validationError = validateMission(mission)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        // Enviar para o servidor
        return repository.uploadMission(mission)
    }

    /**
     * Valida os dados da missão antes de criar
     * @return Mensagem de erro ou null se válido
     */
    private fun validateMission(mission: ServerMissionDto): String? {
        return when {
            mission.name.isBlank() -> "Nome da missão não pode estar vazio"
            mission.waypoints.size < 2 -> "Missão deve ter pelo menos 2 waypoints"
            mission.auto_flight_speed <= 0 -> "Velocidade automática deve ser maior que zero"
            mission.max_flight_speed <= 0 -> "Velocidade máxima deve ser maior que zero"
            mission.auto_flight_speed > mission.max_flight_speed -> "Velocidade automática não pode ser maior que a máxima"
            else -> null
        }
    }
}
