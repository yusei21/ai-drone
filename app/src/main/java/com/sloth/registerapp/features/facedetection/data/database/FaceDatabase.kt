package com.sloth.registerapp.features.facedetection.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sloth.registerapp.features.facedetection.data.mapper.Converters

/**
 * Room Database para armazenar dados de reconhecimento facial
 */
@Database(
    entities = [FaceEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FaceDatabase : RoomDatabase() {

    /**
     * Obtém a interface DAO para operações com faces
     */
    abstract fun faceDao(): FaceDao

    companion object {
        // Previne múltiplas instâncias do banco de dados
        @Volatile
        private var INSTANCE: FaceDatabase? = null

        private const val DATABASE_NAME = "face_recognition_db"

        /**
         * Obtém a instância singleton do banco de dados
         *
         * Implementa o padrão Double-Checked Locking para thread safety
         */
        fun getInstance(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    DATABASE_NAME
                )
                    // Fallback para migração destruitiva (recomendado apenas para versão 1)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

/*
*
*
* // 1. Obter instância do banco
* val database = FaceDatabase.getInstance(context)


* // 2. Obter o DAO
* val faceDao = database.faceDao()


* // 3. Inserir um rosto

* lifecycleScope.launch {
* val face = FaceEntity(
*       name = "João Silva",
*       embedding = floatArrayOf(0.1f, 0.2f, ...).toJson()
*   )
*   val id = faceDao.insert(face)
* }

// 4. Buscar todos
lifecycleScope.launch {
    faceDao.getAllFaces().collect { faces ->
        println("Faces cadastradas: ${faces.size}")
    }
}

// 5. Deletar
lifecycleScope.launch {
    val face = faceDao.getFaceById(1)
    face?.let { faceDao.delete(it) }
}

*
* */
