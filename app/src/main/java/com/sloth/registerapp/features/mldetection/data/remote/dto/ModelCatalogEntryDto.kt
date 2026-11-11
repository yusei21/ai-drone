package com.sloth.registerapp.features.mldetection.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ModelCatalogEntryDto(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String,
    @SerializedName("classes")     val classes: List<String>,
    @SerializedName("size_bytes")  val sizeBytes: Long,
    @SerializedName("description") val description: String = ""
)

data class ModelDownloadUrlDto(
    @SerializedName("url")      val url: String,
    @SerializedName("checksum") val checksum: String = ""
)
