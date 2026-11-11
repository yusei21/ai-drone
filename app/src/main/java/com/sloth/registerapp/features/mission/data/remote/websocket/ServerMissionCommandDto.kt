package com.sloth.registerapp.features.mission.data.remote.websocket

/**
 * Representa os comandos de missão que o servidor pode enviar para o aplicativo.
 */
enum class ServerMissionCommandDto(val code: Int) {
    UPLOAD_MISSION(0x10),
    START_MISSION(0x11),
    STOP_MISSION(0x12),
    PAUSE_MISSION(0x16),
    RESUME_MISSION(0x17);

    companion object {
        fun fromCode(code: Int) = values().find { it.code == code }
    }
}
