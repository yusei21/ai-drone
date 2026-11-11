package com.sloth.registerapp.features.facedetection.domain.usecase

import android.util.Log
import com.sloth.registerapp.features.facedetection.domain.repository.FaceRepository
import com.sloth.registerapp.features.facedetection.domain.model.FaceRegistrationResult

/**
 * Use Case para salvar um rosto no banco de dados
 *
 * Responsabilidades:
 * - Validar dados de entrada
 * - Verificar duplicatas
 * - Salvar no banco de dados
 * - Retornar resultado operação
 */
class SaveFaceUseCase(
    private val repository: FaceRepository
) {
    companion object {
        private const val TAG = "SaveFaceUseCase"
    }

    /**
     * Executa o use case
     *
     * @param name Nome da pessoa
     * @param embedding Embedding do rosto
     * @return FaceRegistrationResult com o resultado da operação
     */
    suspend operator fun invoke(name: String, embedding: FloatArray): FaceRegistrationResult {
        return try {
            // 1. Valida entrada
            if (name.isBlank()) {
                Log.w(TAG, "⚠️ Nome inválido")
                return FaceRegistrationResult.Error("Nome não pode estar vazio")
            }

            if (embedding.isEmpty()) {
                Log.w(TAG, "⚠️ Embedding vazio")
                return FaceRegistrationResult.Error("Embedding inválido")
            }

            Log.d(TAG, "💾 Salvando rosto: $name")

            // 2. Salva no banco usando repository
            val result = repository.saveFace(name, embedding)

            // 3. Mapeia resultado para FaceRegistrationResult
            result.fold(
                onSuccess = { id ->
                    Log.d(TAG, "✅ Rosto salvo com sucesso! ID: $id")
                    FaceRegistrationResult.Success(
                        id = id,
                        name = name,
                        embedding = embedding
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Erro ao salvar: ${error.message}")
                    FaceRegistrationResult.Error(
                        message = error.message ?: "Erro desconhecido ao salvar",
                        exception = error
                    )
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exceção: ${e.message}", e)
            FaceRegistrationResult.Error(
                message = e.message ?: "Erro desconhecido",
                exception = e
            )
        }
    }
}
