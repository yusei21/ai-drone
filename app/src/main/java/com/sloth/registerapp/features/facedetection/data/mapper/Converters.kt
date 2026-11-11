package com.sloth.registerapp.features.facedetection.data.mapper

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Conversores de tipos para Room Database
 * Converte FloatArray em String (JSON) para armazenamento
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<FloatArray>() {}.type
        return gson.fromJson(value, type) ?: FloatArray(0)
    }
}

// ========== Extensões Úteis ==========

/**
 * Converte FloatArray para JSON String
 */
fun FloatArray.toJson(): String {
    return Gson().toJson(this)
}

/**
 * Converte JSON String para FloatArray
 */
fun String.toFloatArray(): FloatArray {
    return try {
        Gson().fromJson(this, FloatArray::class.java) ?: FloatArray(0)
    } catch (e: Exception) {
        FloatArray(0)
    }
}
