package com.sloth.registerapp.features.facedetection.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.sloth.registerapp.features.facedetection.domain.repository.FaceRepository

/**
 * UseCase para Reconhecer uma Pessoa Cadastrada
 * 
 * Responsabilidades:
 * - Gerar embedding do rosto
 * - Comparar com rostos cadastrados
 * - Retornar nome da pessoa ou "Desconhecido"
 */
class RecognizePersonUseCase(
    private val embeddingEngine: GenerateEmbeddingUseCase,
    private val repository: FaceRepository,
    private val similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD
) {
    
    companion object {
        private const val TAG = "RecognizePersonUseCase"
        private const val DEFAULT_SIMILARITY_THRESHOLD = 0.80f
    }
    
    /**
     * Reconhece uma pessoa a partir de um Bitmap
     * 
     * @param faceBitmap Bitmap do rosto a ser reconhecido
     * @return Nome da pessoa cadastrada ou "Desconhecido"
     */
    suspend operator fun invoke(faceBitmap: Bitmap): String {
        return try {
            Log.d(TAG, "🔍 Iniciando reconhecimento...")
            
            // Gera embedding
            val embedding = embeddingEngine.generateEmbedding(faceBitmap)
            
            if (embedding == null) {
                Log.e(TAG, "❌ Falha ao gerar embedding")
                return "Erro"
            }
            
            // Busca rosto similar
            val (isDuplicate, existingFace) = repository.findSimilarFace(
                embedding,
                similarityThreshold
            )
            
            return if (isDuplicate && existingFace != null) {
                Log.d(TAG, "✅ Reconhecido: ${existingFace.name}")
                existingFace.name
            } else {
                Log.d(TAG, "❓ Desconhecido")
                "Desconhecido"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no reconhecimento: ${e.message}", e)
            "Erro"
        }
    }
}
