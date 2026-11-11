package com.sloth.registerapp.core.constants

object CameraConstants {
    // Resolução
    const val CAMERA_WIDTH = 1920
    const val CAMERA_HEIGHT = 1080
    const val CAMERA_FPS = 30

    // Face Detection
    const val MIN_FACE_SIZE = 0.15f // 15% da imagem
    const val FACE_DETECTION_CONFIDENCE = 0.8f
    const val FACE_TRACKING_TIMEOUT_MS = 3000L

    // Permissões
    const val CAMERA_PERMISSION = android.Manifest.permission.CAMERA
    const val STORAGE_PERMISSION = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
}