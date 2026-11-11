package com.sloth.registerapp.features.mldetection.domain.model

data class MLModel(
    val id: String,
    val name: String,
    val classes: List<String>,
    val filePath: String,
    val source: MLModelSource,
    val sizeBytes: Long = 0L
)

enum class MLModelSource {
    ASSET,
    LOCAL_FILE
}
