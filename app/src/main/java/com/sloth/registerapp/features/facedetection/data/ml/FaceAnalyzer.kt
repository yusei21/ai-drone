package com.sloth.registerapp.features.facedetection.data.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sloth.registerapp.core.utils.imageProxyToBitmap

/**
 * Analisador de Rostos usando ML Kit
 * 
 * Responsabilidades:
 * - Detectar rostos em frames da câmera
 * - Analisar qualidade (centralização, iluminação)
 * - Garantir estabilidade antes de captura
 * 
 * Camada: Data/ML (infraestrutura de Machine Learning)
 */
class FaceAnalyzer(
    private val listener: FaceAnalyzerListener,
    private val config: FaceAnalyzerConfig = FaceAnalyzerConfig()
) : ImageAnalysis.Analyzer {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(config.performanceMode)
            .setLandmarkMode(config.landmarkMode)
            .setClassificationMode(config.classificationMode)
            .setMinFaceSize(config.minFaceSize)
            .apply { if (config.isTrackingEnabled) enableTracking() }
            .build()

        FaceDetection.getClient(options)
    }

    private var stableFramesCount = 0
    private var lastWasGood = false
    private var isProcessing = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing && config.skipAnalysisWhenBusy) {
            imageProxy.close()
            return
        }
        isProcessing = true

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val bitmap = imageProxyToBitmap(imageProxy)
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { faces ->
                    val result = analyzeFaces(
                        faces,
                        imageProxy.width,
                        imageProxy.height,
                        imageProxy,
                        bitmap
                    )
                    listener.onResult(result)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erro na detecção: ${e.message}")
                    resetStability()
                    listener.onResult(FaceAnalysisResult.Error(e.message ?: "Erro desconhecido"))
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }

    fun analyze(bitmap: Bitmap, rotationDegrees: Int) {
        if (isProcessing && config.skipAnalysisWhenBusy) {
            return
        }
        isProcessing = true

        val image = InputImage.fromBitmap(bitmap, rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val result = analyzeFaces(
                    faces,
                    bitmap.width,
                    bitmap.height,
                    null,
                    bitmap
                )
                listener.onResult(result)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro na detecção: ${e.message}")
                resetStability()
                listener.onResult(FaceAnalysisResult.Error(e.message ?: "Erro desconhecido"))
            }
            .addOnCompleteListener {
                isProcessing = false
            }
    }

    private fun analyzeFaces(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        imageProxy: ImageProxy?,
        bitmap: Bitmap?
    ): FaceAnalysisResult {
        return when {
            faces.isEmpty() -> {
                resetStability()
                FaceAnalysisResult.NoFace
            }
            faces.size > 1 && config.disallowMultipleFaces -> {
                resetStability()
                FaceAnalysisResult.MultipleFaces
            }
            else -> {
                val face = faces[0]
                if (config.advancedAnalysis && imageProxy != null) {
                    analyzeAdvanced(face, imageWidth, imageHeight, imageProxy, bitmap)
                } else {
                    FaceAnalysisResult.FaceDetected(face, bitmap)
                }
            }
        }
    }

    private fun analyzeAdvanced(face: Face, imageWidth: Int, imageHeight: Int, imageProxy: ImageProxy, bitmap: Bitmap?): FaceAnalysisResult {
        val isCentered = isFaceCentered(face.boundingBox, imageWidth, imageHeight)
        val brightness = calculateFaceBrightness(face, imageProxy)
        val isWellLit = brightness >= config.minBrightness

        val isCurrentlyGood = isCentered && isWellLit
        if (isCurrentlyGood) {
            if (lastWasGood) {
                stableFramesCount++
            } else {
                stableFramesCount = 1
            }
        } else {
            resetStability()
        }
        lastWasGood = isCurrentlyGood

        val isStable = stableFramesCount >= config.requiredStableFrames

        Log.d(TAG, "🎯 Centralizado: $isCentered | 💡 Iluminado: $isWellLit | ⏱️ Frames estáveis: $stableFramesCount/${config.requiredStableFrames}")

        return FaceAnalysisResult.AdvancedResult(
            face = face,
            isCentered = isCentered,
            isWellLit = isWellLit,
            brightness = brightness,
            isStable = isStable,
            bitmap = bitmap
        )
    }

    private fun resetStability() {
        stableFramesCount = 0
        lastWasGood = false
    }

    private fun isFaceCentered(boundingBox: Rect, imageWidth: Int, imageHeight: Int): Boolean {
        val faceCenterX = boundingBox.centerX().toFloat() / imageWidth
        val faceCenterY = boundingBox.centerY().toFloat() / imageHeight
        val tolerance = config.centerTolerance
        return faceCenterX in (0.5f - tolerance)..(0.5f + tolerance) &&
               faceCenterY in (0.5f - tolerance)..(0.5f + tolerance)
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun calculateFaceBrightness(face: Face, imageProxy: ImageProxy): Float {
        try {
            val image = imageProxy.image ?: return 50f

            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val yRowStride = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride

            val faceBounds = face.boundingBox
            val faceWidth = faceBounds.width().toFloat()
            val faceHeight = faceBounds.height().toFloat()

            val centerReduction = 0.3f

            val left = (faceBounds.left + faceWidth * centerReduction).toInt()
                .coerceIn(0, imageProxy.width - 1)
            val top = (faceBounds.top + faceHeight * centerReduction).toInt()
                .coerceIn(0, imageProxy.height - 1)
            val right = (faceBounds.right - faceWidth * centerReduction).toInt()
                .coerceIn(left + 1, imageProxy.width)
            val bottom = (faceBounds.bottom - faceHeight * centerReduction).toInt()
                .coerceIn(top + 1, imageProxy.height)

            var totalLuminance = 0L
            var pixelCount = 0

            for (y in top until bottom step 2) {
                for (x in left until right step 2) {
                    try {
                        val index = y * yRowStride + x * yPixelStride
                        if (index >= 0 && index < yBuffer.capacity()) {
                            val luminance = yBuffer.get(index).toInt() and 0xFF
                            totalLuminance += luminance
                            pixelCount++
                        }
                    } catch (e: Exception) {
                        // Ignora
                    }
                }
            }

            if (pixelCount == 0) return 50f

            return (totalLuminance / pixelCount).toFloat()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao calcular luminosidade: ${e.message}", e)
            return 50f
        }
    }


    fun release() {
        detector.close()
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
    }
}

/**
 * Listener para resultados da análise facial
 */
interface FaceAnalyzerListener {
    fun onResult(result: FaceAnalysisResult)
}

/**
 * Resultado da análise facial
 */
sealed class FaceAnalysisResult {
    object NoFace : FaceAnalysisResult()
    object MultipleFaces : FaceAnalysisResult()
    data class FaceDetected(val face: Face, val bitmap: Bitmap?) : FaceAnalysisResult()
    data class AdvancedResult(
        val face: Face,
        val isCentered: Boolean,
        val isWellLit: Boolean,
        val brightness: Float,
        val isStable: Boolean,
        val bitmap: Bitmap?
    ) : FaceAnalysisResult()
    data class Error(val message: String) : FaceAnalysisResult()
}

/**
 * Configuração do FaceAnalyzer
 */
data class FaceAnalyzerConfig(
    val performanceMode: Int = FaceDetectorOptions.PERFORMANCE_MODE_FAST,
    val landmarkMode: Int = FaceDetectorOptions.LANDMARK_MODE_NONE,
    val classificationMode: Int = FaceDetectorOptions.CLASSIFICATION_MODE_NONE,
    val minFaceSize: Float = 0.15f,
    val isTrackingEnabled: Boolean = false,
    val skipAnalysisWhenBusy: Boolean = true,
    val disallowMultipleFaces: Boolean = true,
    val advancedAnalysis: Boolean = false,
    val centerTolerance: Float = 0.20f,
    val minBrightness: Float = 45f,
    val requiredStableFrames: Int = 8
)
