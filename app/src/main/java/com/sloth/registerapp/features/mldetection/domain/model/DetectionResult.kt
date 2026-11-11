package com.sloth.registerapp.features.mldetection.domain.model

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val normalizedRect: RectF
)
