package com.sloth.registerapp.features.facedetection.domain.model

import android.graphics.Bitmap
import com.sloth.registerapp.features.facedetection.data.local.FaceEntity

/**
 * Estados durante o processo de captura de rosto
 * 
 * Representa os diferentes estados do fluxo de captura facial.
 * Usa sealed class para type-safe state management.
 */
sealed class FaceCaptureState {

    /**
     * Estado inicial - pronto para iniciar captura
     */
    object Idle : FaceCaptureState()

    /**
     * Escaneando rosto em tempo real
     */
    object Scanning : FaceCaptureState()

    /**
     * Processando a captura (gerando embedding)
     */
    object Processing : FaceCaptureState()

    /**
     * Captura bem-sucedida
     */
    data class Success(
        val bitmap: Bitmap,                 // Imagem capturada
        val embedding: FloatArray,          // Embedding gerado
        val isDuplicate: Boolean = false,   // Se é uma duplicata
        val existingFace: FaceEntity? = null  // Face duplicada encontrada
    ) : FaceCaptureState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (bitmap != other.bitmap) return false
            if (!embedding.contentEquals(other.embedding)) return false
            if (isDuplicate != other.isDuplicate) return false
            if (existingFace != other.existingFace) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bitmap.hashCode()
            result = 31 * result + embedding.contentHashCode()
            result = 31 * result + isDuplicate.hashCode()
            result = 31 * result + (existingFace?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * Erro durante a captura
     */
    data class Error(val message: String) : FaceCaptureState()
}
