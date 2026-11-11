package com.sloth.registerapp.features.report.domain.model

enum class MissionMediaType {
    PHOTO,
    VIDEO
}

enum class MissionMediaSource {
    DRONE_SD,
    PHONE_LOCAL
}

data class MissionMedia(
    val id: String,
    val missionId: String,
    val mediaType: MissionMediaType,
    val source: MissionMediaSource,
    val dronePath: String? = null,
    val localPath: String? = null,
    val createdAtMs: Long,
    val sizeBytes: Long? = null,
    val isDownloaded: Boolean = false
)
