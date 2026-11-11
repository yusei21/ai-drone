package com.sloth.registerapp.features.facedetection.domain.model

/**
 * Modelo de domínio para Face
 * 
 * Representa um rosto no contexto de negócio (domain).
 * Este é o modelo limpo, sem dependências de banco de dados.
 */
data class Face(
    val id: Long,
    val name: String,
    val embedding: FloatArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Face) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (createdAt != other.createdAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
