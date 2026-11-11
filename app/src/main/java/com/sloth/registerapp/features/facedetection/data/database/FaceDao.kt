package com.sloth.registerapp.features.facedetection.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para operações com a tabela de faces
 */
@Dao
interface FaceDao {

    /**
     * Insere um novo rosto no banco de dados
     * @return ID do rosto inserido
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(face: FaceEntity): Long

    /**
     * Atualiza um rosto existente
     */
    @Update
    suspend fun update(face: FaceEntity)

    /**
     * Deleta um rosto específico
     */
    @Delete
    suspend fun delete(face: FaceEntity)

    /**
     * Retorna todas as faces ordenadas por data (mais recentes primeiro)
     * Flow para observar mudanças em tempo real
     */
    @Query("SELECT * FROM faces ORDER BY timestamp DESC")
    fun getAllFaces(): Flow<List<FaceEntity>>

    /**
     * Retorna todas as faces (versão síncrona)
     */
    @Query("SELECT * FROM faces ORDER BY timestamp DESC")
    suspend fun getAllFacesSync(): List<FaceEntity>

    /**
     * Busca um rosto pelo ID
     */
    @Query("SELECT * FROM faces WHERE id = :id")
    suspend fun getFaceById(id: Long): FaceEntity?

    /**
     * Busca um rosto pelo nome
     */
    @Query("SELECT * FROM faces WHERE name LIKE :name LIMIT 1")
    suspend fun getFaceByName(name: String): FaceEntity?

    /**
     * Deleta todas as faces
     */
    @Query("DELETE FROM faces")
    suspend fun deleteAll()

    /**
     * Retorna o número total de faces cadastradas
     */
    @Query("SELECT COUNT(*) FROM faces")
    suspend fun getCount(): Int
}
