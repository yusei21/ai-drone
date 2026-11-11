package com.sloth.registerapp.features.mldetection.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.sloth.registerapp.features.mldetection.domain.model.DetectionResult
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.domain.model.MLModelSource
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class YoloInferenceEngine private constructor(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    val model: MLModel
) : Closeable {

    private val inputSize: Int
    private val isNchw: Boolean
    private val predictionCount: Int
    private val outputDim: Int

    init {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        require(inputShape.size == 4 && inputShape[0] == 1) {
            "Entrada TFLite deve ter formato de lote unico: ${inputShape.contentToString()}"
        }
        isNchw = inputShape[1] == 3 && inputShape[2] == inputShape[3]
        val isNhwc = inputShape[3] == 3 && inputShape[1] == inputShape[2]
        require(isNchw || isNhwc) {
            "Formato de entrada nao suportado: ${inputShape.contentToString()} (esperado NCHW ou NHWC RGB quadrado)"
        }
        inputSize = if (isNchw) inputShape[2] else inputShape[1]
        require(interpreter.getInputTensor(0).dataType() == org.tensorflow.lite.DataType.FLOAT32) {
            "Entrada TFLite precisa ser FLOAT32 (exportar sem quantizacao)"
        }
        require(interpreter.getOutputTensor(0).dataType() == org.tensorflow.lite.DataType.FLOAT32) {
            "Saida TFLite precisa ser FLOAT32 (exportar sem quantizacao)"
        }
        require(outputShape.size == 3 && outputShape[0] == 1 && outputShape[1] in 5..100) {
            "Formato de saida nao suportado: ${outputShape.contentToString()} (esperado [1, 4+classes, N])"
        }
        predictionCount = outputShape[2]
        outputDim = outputShape[1]
        Log.d(TAG, "Engine loaded: ${model.name} input=${inputShape.contentToString()} output=${outputShape.contentToString()}")
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = bitmapToInputBuffer(scaledBitmap)
        if (scaledBitmap != bitmap) scaledBitmap.recycle()

        val output = Array(1) { Array(outputDim) { FloatArray(predictionCount) } }
        interpreter.run(inputBuffer, output)

        // LiteRT-Torch YOLO26 exports normalized xywh values in [0, 1].
        // Other YOLO TFLite exports may use pixel coordinates in [0, inputSize].
        // Determine the format once per frame from the center coordinates.
        val raw = output[0]
        val pixelCoordinateCount = (0 until predictionCount).count { index ->
            raw[0][index] > 2f || raw[1][index] > 2f
        }
        val coordinateScale = if (pixelCoordinateCount > predictionCount / 10) {
            inputSize.toFloat()
        } else {
            1f
        }

        return if (outputDim == 5) {
            decodeSingleClass(raw, coordinateScale)
        } else {
            decodeMultiClass(raw, coordinateScale)
        }
    }

    override fun close() {
        runCatching { interpreter.close() }
        runCatching { gpuDelegate?.close() }
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        if (isNchw) {
            // LiteRT-Torch exports YOLO26 with [1,3,H,W] (channel-first).
            // Channels must be contiguous: all red pixels, then green, then blue.
            for (shift in intArrayOf(16, 8, 0)) {
                for (pixel in pixels) {
                    buffer.putFloat(((pixel shr shift) and 0xFF) / 255f)
                }
            }
        } else {
            // Standard TensorFlow Lite exports often use [1,H,W,3].
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                buffer.putFloat((pixel and 0xFF) / 255f)
            }
        }
        buffer.rewind()
        return buffer
    }

    // [1][5][8400]: row 4 = objectness confidence, single class
    private fun decodeSingleClass(output: Array<FloatArray>, coordinateScale: Float): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()
        val label = model.classes.firstOrNull() ?: "Objeto"
        for (index in 0 until predictionCount) {
            val confidence = output[4][index]
            if (!confidence.isFinite() || confidence < CONFIDENCE_THRESHOLD) continue
            val rect = extractRect(output, index, coordinateScale) ?: continue
            detections += DetectionResult(label = label, confidence = confidence, normalizedRect = rect)
        }
        return nonMaximumSuppression(detections)
    }

    // [1][4+N][8400]: rows 4..4+N are class scores (YOLOv8 style)
    private fun decodeMultiClass(output: Array<FloatArray>, coordinateScale: Float): List<DetectionResult> {
        val numClasses = outputDim - 4
        val detections = mutableListOf<DetectionResult>()
        for (index in 0 until predictionCount) {
            var bestScore = 0f
            var bestClass = 0
            for (c in 0 until numClasses) {
                val score = output[4 + c][index]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }
            if (!bestScore.isFinite() || bestScore < CONFIDENCE_THRESHOLD) continue
            val rect = extractRect(output, index) ?: continue
            val label = model.classes.getOrElse(bestClass) { "Classe_$bestClass" }
            detections += DetectionResult(label = label, confidence = bestScore, normalizedRect = rect)
        }
        return nonMaximumSuppression(detections)
    }

    private fun extractRect(output: Array<FloatArray>, index: Int, coordinateScale: Float): RectF? {
        val centerX = output[0][index] / coordinateScale
        val centerY = output[1][index] / coordinateScale
        val width = output[2][index] / coordinateScale
        val height = output[3][index] / coordinateScale
        if (!centerX.isFinite() || !centerY.isFinite() || !width.isFinite() || !height.isFinite()) return null
        if (width <= MIN_BOX_SIZE || height <= MIN_BOX_SIZE) return null
        return RectF(
            (centerX - width / 2f).coerceIn(0f, 1f),
            (centerY - height / 2f).coerceIn(0f, 1f),
            (centerX + width / 2f).coerceIn(0f, 1f),
            (centerY + height / 2f).coerceIn(0f, 1f)
        )
    }

    private fun nonMaximumSuppression(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val candidate = sorted.removeAt(0)
            selected += candidate
            sorted.removeAll { iou(candidate.normalizedRect, it.normalizedRect) > IOU_THRESHOLD }
        }
        return selected.take(MAX_DETECTIONS)
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        val interW = (right - left).coerceAtLeast(0f)
        val interH = (bottom - top).coerceAtLeast(0f)
        val inter = interW * interH
        if (inter <= 0f) return 0f
        return inter / (a.width() * a.height() + b.width() * b.height() - inter)
    }

    companion object {
        private const val TAG = "YoloInferenceEngine"
        private const val CONFIDENCE_THRESHOLD = 0.27f
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 20
        private const val MIN_BOX_SIZE = 0.005f

        fun create(context: Context, model: MLModel): YoloInferenceEngine {
            val modelBuffer = when (model.source) {
                MLModelSource.ASSET -> loadFromAssets(context, model.filePath)
                MLModelSource.LOCAL_FILE -> loadFromFile(model.filePath)
            }

            var gpuDelegate: GpuDelegate? = null
            val options = Interpreter.Options().apply {
                runCatching {
                    gpuDelegate = GpuDelegate()
                    addDelegate(gpuDelegate!!)
                    Log.d(TAG, "GPU delegate enabled for ${model.name}")
                }.onFailure {
                    gpuDelegate = null
                    Log.w(TAG, "GPU delegate unavailable, using CPU: ${it.message}")
                }
                numThreads = 4
            }

            // A GPU pode aceitar o delegate, mas falhar na criacao do Interpreter
            // por operadores nao suportados. Neste caso, tentar CPU.
            return try {
                YoloInferenceEngine(Interpreter(modelBuffer, options), gpuDelegate, model)
            } catch (gpuError: Exception) {
                Log.w(TAG, "GPU model initialization failed, retrying on CPU: ${gpuError.message}")
                runCatching { gpuDelegate?.close() }
                val cpuOptions = Interpreter.Options().apply { numThreads = 4 }
                YoloInferenceEngine(Interpreter(modelBuffer, cpuOptions), null, model)
            }
        }

        private fun loadFromAssets(context: Context, assetPath: String): ByteBuffer {
            val fd = context.assets.openFd(assetPath)
            return fd.createInputStream().channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset,
                fd.declaredLength
            )
        }

        private fun loadFromFile(filePath: String): ByteBuffer {
            val file = File(filePath)
            return file.inputStream().channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                file.length()
            )
        }
    }
}
