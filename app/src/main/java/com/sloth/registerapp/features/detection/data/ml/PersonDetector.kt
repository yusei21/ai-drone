package com.sloth.registerapp.features.detection.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DetectionBox(
    val label: String,
    val confidence: Float,
    val normalizedRect: RectF
)

class PersonDetector(
    context: Context
) : Closeable {

    private val appContext = context.applicationContext
    private val interpreter: Interpreter
    private val inputSize: Int
    private val predictionCount: Int

    init {
        val modelBuffer = loadModelFile(appContext, MODEL_FILE)
        interpreter = Interpreter(modelBuffer)

        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        inputSize = inputShape.getOrNull(2) ?: DEFAULT_INPUT_SIZE
        predictionCount = outputShape.getOrNull(2) ?: DEFAULT_PREDICTION_COUNT

        Log.d(TAG, "PersonDetector carregado. input=${inputShape.contentToString()} output=${outputShape.contentToString()}")
    }

    fun detect(bitmap: Bitmap): List<DetectionBox> {
        if (bitmap.width <= 0 || bitmap.height <= 0) return emptyList()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = bitmapToInputBuffer(scaledBitmap)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        val output = Array(1) { Array(OUTPUT_FEATURE_COUNT) { FloatArray(predictionCount) } }
        interpreter.run(inputBuffer, output)

        return decode(output[0])
    }

    override fun close() {
        interpreter.close()
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }

        buffer.rewind()
        return buffer
    }

    private fun decode(
        output: Array<FloatArray>
    ): List<DetectionBox> {
        val detections = mutableListOf<DetectionBox>()

        for (index in 0 until predictionCount) {
            val confidence = output[4][index]
            if (confidence < CONFIDENCE_THRESHOLD) continue

            val centerX = output[0][index] / inputSize.toFloat()
            val centerY = output[1][index] / inputSize.toFloat()
            val width = output[2][index] / inputSize.toFloat()
            val height = output[3][index] / inputSize.toFloat()

            val left = (centerX - width / 2f).coerceIn(0f, 1f)
            val top = (centerY - height / 2f).coerceIn(0f, 1f)
            val right = (centerX + width / 2f).coerceIn(0f, 1f)
            val bottom = (centerY + height / 2f).coerceIn(0f, 1f)

            if (width <= MIN_BOX_SIZE_NORMALIZED || height <= MIN_BOX_SIZE_NORMALIZED) continue

            detections += DetectionBox(
                label = PERSON_LABEL,
                confidence = confidence,
                normalizedRect = RectF(left, top, right, bottom)
            )
        }

        return nonMaximumSuppression(detections)
    }

    private fun nonMaximumSuppression(
        detections: List<DetectionBox>
    ): List<DetectionBox> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionBox>()

        while (sorted.isNotEmpty()) {
            val candidate = sorted.removeAt(0)
            selected += candidate
            sorted.removeAll { current ->
                intersectionOverUnion(candidate.normalizedRect, current.normalizedRect) > IOU_THRESHOLD
            }
        }

        return selected.take(MAX_DETECTIONS)
    }

    private fun intersectionOverUnion(first: RectF, second: RectF): Float {
        val left = maxOf(first.left, second.left)
        val top = maxOf(first.top, second.top)
        val right = minOf(first.right, second.right)
        val bottom = minOf(first.bottom, second.bottom)

        val intersectionWidth = (right - left).coerceAtLeast(0f)
        val intersectionHeight = (bottom - top).coerceAtLeast(0f)
        val intersectionArea = intersectionWidth * intersectionHeight
        if (intersectionArea <= 0f) return 0f

        val firstArea = first.width() * first.height()
        val secondArea = second.width() * second.height()
        return intersectionArea / (firstArea + secondArea - intersectionArea)
    }

    companion object {
        private const val TAG = "PersonDetector"
        const val MODEL_FILE = "person_detector.tflite"
        private const val PERSON_LABEL = "Pessoa"
        private const val DEFAULT_INPUT_SIZE = 640
        private const val DEFAULT_PREDICTION_COUNT = 8400
        private const val OUTPUT_FEATURE_COUNT = 5
        private const val CONFIDENCE_THRESHOLD = 0.12f
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 8
        private const val MIN_BOX_SIZE_NORMALIZED = 0.005f

        fun isModelAvailable(context: Context): Boolean {
            return runCatching {
                context.assets.open(MODEL_FILE).close()
                true
            }.getOrDefault(false)
        }

        private fun loadModelFile(context: Context, assetName: String): ByteBuffer {
            val fileDescriptor = context.assets.openFd(assetName)
            return fileDescriptor.createInputStream().channel.map(
                java.nio.channels.FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }
}
