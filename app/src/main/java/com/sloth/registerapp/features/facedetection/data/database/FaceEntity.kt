package com.sloth.registerapp.features.facedetection.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa um rosto cadastrado no banco de dados
 */
@Entity(tableName = "faces")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val embedding: String, // JSON do FloatArray

    val timestamp: Long = System.currentTimeMillis()
)
