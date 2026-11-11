package com.sloth.registerapp.features.facedetection.domain.model

import com.sloth.registerapp.features.facedetection.data.local.FaceEntity

/**
 * Resultado da operação de registro de rosto
 * 
 * Representa o resultado de salvar um rosto no banco de dados.
 * Usa sealed class para type-safe handling.
 */
sealed class FaceRegistrationResult {

    /**
     * Rosto registrado com sucesso
     */
    data class Success(
        val id: Long,           // ID do rosto no banco
        val name: String,       // Nome da pessoa
        val embedding: FloatArray  // Embedding gerado
    ) : FaceRegistrationResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (id != other.id) return false
            if (name != other.name) return false
            if (!embedding.contentEquals(other.embedding)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    /**
     * Rosto já estava cadastrado (duplicata)
     */
    data class Duplicate(
        val existingFace: FaceEntity  // Face encontrada no banco
    ) : FaceRegistrationResult()

    /**
     * Erro durante a operação
     */
    data class Error(
        val message: String,           // Mensagem de erro
        val exception: Throwable? = null  // Exceção original (opcional)
    ) : FaceRegistrationResult()
}
