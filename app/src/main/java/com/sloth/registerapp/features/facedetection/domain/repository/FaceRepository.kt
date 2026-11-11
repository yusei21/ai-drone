package com.sloth.registerapp.features.facedetection.domain.repository

import com.sloth.registerapp.features.facedetection.data.local.FaceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface do Repository para gerenciar dados de faces
 *
 * Define o contrato que a camada de dados deve implementar
 * Responsável por:
 * - Operações CRUD de faces
 * - Comparação de embeddings
 * - Verificação de duplicatas
 */
interface FaceRepository {

    /**
     * Flow reativo com todas as faces cadastradas
     * Use para observar mudanças em tempo real
     */
    val allFaces: Flow<List<FaceEntity>>

    // ========== Operações de Leitura ==========

    /**
     * Busca um rosto pelo ID
     *
     * @param id ID do rosto
     * @return FaceEntity ou null se não encontrado
     */
    suspend fun getFaceById(id: Long): FaceEntity?

    /**
     * Retorna todas as faces cadastradas (versão síncrona)
     *
     * @return Lista de todas as faces
     */
    suspend fun getAllFacesSync(): List<FaceEntity>

    /**
     * Retorna o número total de faces cadastradas
     *
     * @return Quantidade de faces
     */
    suspend fun getCount(): Int

    // ========== Operações de Escrita ==========

    /**
     * Salva um novo rosto no banco de dados
     *
     * @param name Nome da pessoa
     * @param embedding Embedding do rosto
     * @return Result<Long> com o ID do rosto salvo
     */
    suspend fun saveFace(name: String, embedding: FloatArray): Result<Long>

    /**
     * Deleta um rosto específico
     *
     * @param face FaceEntity a ser deletado
     * @return Número de linhas afetadas
     */
    suspend fun deleteFace(face: FaceEntity): Int

    /**
     * Deleta todas as faces do banco
     *
     * @return Número de linhas afetadas
     */
    suspend fun deleteAll(): Int

    // ========== Operações de Comparação ==========

    /**
     * Verifica se já existe um rosto similar no banco
     *
     * Usa similaridade de cosseno com threshold configurável
     *
     * @param embedding Embedding a comparar
     * @param similarityThreshold Threshold de similaridade (0.0 a 1.0)
     * @return Pair<Boolean, FaceEntity?> - (encontrou similar?, face encontrada)
     */
    suspend fun findSimilarFace(
        embedding: FloatArray,
        similarityThreshold: Float = 0.87f
    ): Pair<Boolean, FaceEntity?>
}
