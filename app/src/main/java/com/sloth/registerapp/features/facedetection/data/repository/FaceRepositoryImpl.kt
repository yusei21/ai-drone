package com.sloth.registerapp.features.facedetection.data.repository

import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.facedetection.data.local.FaceDao
import com.sloth.registerapp.features.facedetection.data.local.FaceEntity
import com.sloth.registerapp.features.facedetection.data.mapper.toFloatArray
import com.sloth.registerapp.features.facedetection.data.mapper.toJson
import com.sloth.registerapp.features.facedetection.domain.usecase.GenerateEmbeddingUseCase
import com.sloth.registerapp.features.facedetection.domain.repository.FaceRepository
import kotlinx.coroutines.flow.Flow
import io.sentry.SentryLevel

/**
 * Implementação do Repository para gerenciar dados de faces
 *
 * Camada intermediária entre o banco de dados e a lógica de negócio
 * Responsável por:
 * - Operações CRUD de faces
 * - Comparação de embeddings
 * - Verificação de duplicatas
 */
class FaceRepositoryImpl(
    private val faceDao: FaceDao,
    private val embeddingEngine: GenerateEmbeddingUseCase
) : FaceRepository {
    companion object {
        private const val TAG = "FaceRepositoryImpl"
        private const val SIMILARITY_THRESHOLD = 0.87f // 75% de similaridade
    }

    /**
     * Retorna todas as faces cadastradas (Flow reativo)
     * Use para observar mudanças em tempo real
     */
    override val allFaces: Flow<List<FaceEntity>> = faceDao.getAllFaces()

    // ========== Operações de Leitura ==========

    /**
     * Busca um rosto pelo ID
     *
     * @param id ID do rosto
     * @return FaceEntity ou null se não encontrado
     */
    override suspend fun getFaceById(id: Long): FaceEntity? {
        return try {
            Log.d(TAG, "🔍 Buscando rosto com ID: $id")
            val face = faceDao.getFaceById(id)

            if (face != null) {
                Log.d(TAG, "✅ Rosto encontrado: ${face.name}")
            } else {
                Log.d(TAG, "⚠️ Rosto não encontrado")
            }

            face
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar rosto: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "get_face_by_id",
                level = SentryLevel.WARNING,
                extras = mapOf("face_id" to id)
            )
            null
        }
    }

    /**
     * Retorna todas as faces cadastradas (versão síncrona)
     *
     * @return Lista de todas as faces
     */
    override suspend fun getAllFacesSync(): List<FaceEntity> {
        return try {
            Log.d(TAG, "📋 Buscando todas as faces...")
            val faces = faceDao.getAllFacesSync()
            Log.d(TAG, "✅ ${faces.size} faces encontradas")
            faces
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar faces: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "get_all_faces",
                level = SentryLevel.WARNING
            )
            emptyList()
        }
    }

    /**
     * Retorna o número total de faces cadastradas
     *
     * @return Quantidade de faces
     */
    override suspend fun getCount(): Int {
        return try {
            val count = faceDao.getCount()
            Log.d(TAG, "📊 Total de faces: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao contar faces: ${e.message}", e)
            0
        }
    }

    // ========== Operações de Escrita ==========

    /**
     * Salva um novo rosto no banco de dados
     *
     * @param name Nome da pessoa
     * @param embedding Embedding do rosto
     * @return Result<Long> com o ID do rosto salvo
     */
    override suspend fun saveFace(name: String, embedding: FloatArray): Result<Long> {
        return try {
            Log.d(TAG, "💾 Salvando rosto: $name")

            // 1. Verifica se já existe um rosto similar
            val (isDuplicate, existingFace) = findSimilarFace(embedding, SIMILARITY_THRESHOLD)

            if (isDuplicate && existingFace != null) {
                Log.w(TAG, "⚠️ Rosto duplicado! Já cadastrado como: ${existingFace.name}")
                return Result.failure(
                    DuplicateFaceException("Rosto já cadastrado como '${existingFace.name}'")
                )
            }

            // 2. Cria a entidade
            val faceEntity = FaceEntity(
                name = name,
                embedding = embedding.toJson()
            )

            // 3. Insere no banco
            val id = faceDao.insert(faceEntity)
            Log.d(TAG, "✅ Rosto salvo com sucesso! ID: $id")

            Result.success(id)

        } catch (e: DuplicateFaceException) {
            Log.w(TAG, "⚠️ Duplicata detectada: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar rosto: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "save_face",
                extras = mapOf("embedding_size" to embedding.size)
            )
            Result.failure(e)
        }
    }

    /**
     * Deleta um rosto específico
     *
     * @param face FaceEntity a ser deletado
     */
    override suspend fun deleteFace(face: FaceEntity): Int {
        return try {
            Log.d(TAG, "🗑️ Deletando rosto: ${face.name}")
            faceDao.delete(face)
            Log.d(TAG, "✅ Rosto deletado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao deletar rosto: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "delete_face",
                level = SentryLevel.WARNING,
                extras = mapOf("face_id" to face.id)
            )
            0
        }
    }

    /**
     * Deleta todas as faces do banco
     */
    override suspend fun deleteAll(): Int {
        return try {
            Log.d(TAG, "🗑️ Deletando TODOS os rostos...")
            faceDao.deleteAll()
            Log.d(TAG, "✅ Todos os rostos foram deletados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao deletar todos: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "delete_all_faces"
            )
            0
        }
    }

    // ========== Operações de Comparação ==========

    /**
     * Verifica se já existe um rosto similar no banco
     *
     * Usa similaridade de cosseno com threshold de 75%
     *
     * @param embedding Embedding a comparar
     * @return Pair<Boolean, FaceEntity?> - (encontrou similar?, face encontrada)
     */
    override suspend fun findSimilarFace(embedding: FloatArray, similarityThreshold: Float): Pair<Boolean, FaceEntity?> {
        return try {
            Log.d(TAG, "🔍 Procurando rosto similar...")

            val allFaces = getAllFacesSync()

            if (allFaces.isEmpty()) {
                Log.d(TAG, "ℹ️ Nenhum rosto cadastrado para comparar")
                return Pair(false, null)
            }

            Log.d(TAG, "📊 Comparando com ${allFaces.size} rosto(s) cadastrado(s)...")

            // Compara com cada rosto cadastrado
            for (face in allFaces) {
                try {
                    val storedEmbedding = face.embedding.toFloatArray()

                    // Calcula similaridade de cosseno
                    val cosineSimilarity = embeddingEngine.compareEmbeddings(
                        embedding,
                        storedEmbedding
                    )

                    // Normaliza de [-1, 1] para [0, 1]
                    val normalizedSimilarity = (cosineSimilarity + 1f) / 2f

                    Log.d(
                        TAG,
                        "📈 ${face.name}: ${String.format("%.2f", normalizedSimilarity * 100)}%"
                    )

                    // Se passou no threshold, encontrou uma duplicata
                    if (normalizedSimilarity >= similarityThreshold) {
                        Log.d(TAG, "✅ Rosto similar encontrado: ${face.name} (${String.format("%.2f", normalizedSimilarity * 100)}%)")
                        return Pair(true, face)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Erro ao comparar com ${face.name}: ${e.message}")
                    AppObservability.breadcrumb(
                        category = "face_detection",
                        message = "Face comparison failed for one stored face",
                        level = SentryLevel.WARNING,
                        data = mapOf(
                            "face_id" to face.id,
                            "error_type" to e::class.java.simpleName
                        )
                    )
                }
            }

            Log.d(TAG, "✅ Nenhum rosto similar encontrado")
            Pair(false, null)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao procurar rosto similar: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "find_similar_face",
                extras = mapOf("embedding_size" to embedding.size)
            )
            Pair(false, null)
        }
    }
}

/**
 * Exception customizada para quando um rosto duplicado é encontrado
 */
class DuplicateFaceException(message: String) : Exception(message)
