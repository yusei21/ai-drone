package com.sloth.registerapp.features.facedetection.domain.usecase

import android.graphics.Bitmap
import com.sloth.registerapp.features.facedetection.domain.repository.FaceRepository
import com.sloth.registerapp.features.facedetection.domain.model.FaceCaptureState

/**
 * UseCase para capturar rostos da câmera
 * 
 * Responsabilidades:
 * - Obter frame da câmera
 * - Executar análise facial
 * - Gerar embedding facial
 */
class CaptureFaceUseCase(
    private val embeddingEngine: GenerateEmbeddingUseCase,
    private val repository: FaceRepository
) {
    
    /**
     * Captura e analisa um rosto de uma imagem
     * @param bitmap Imagem a ser analisada
     * @return Estado da captura com embedding
     */
    suspend operator fun invoke(bitmap: Bitmap): FaceCaptureState {
        return try {
            // Gera embedding facial
            val embedding = embeddingEngine.generateEmbedding(bitmap)
            
            if (embedding != null) {
                FaceCaptureState.Success(
                    embedding = embedding,
                    bitmap = bitmap
                )
            } else {
                FaceCaptureState.Error("Failed to generate embedding")
            }
        } catch (e: Exception) {
            FaceCaptureState.Error(e.message ?: "Unknown error during capture")
        }
    }
}

