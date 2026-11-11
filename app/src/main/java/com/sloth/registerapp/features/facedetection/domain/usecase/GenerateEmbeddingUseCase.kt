package com.sloth.registerapp.features.facedetection.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * UseCase para geração e comparação de embeddings faciais
 * Usa LiteRT (Google AI Edge) em vez de TensorFlow Lite
 * 
 * Responsabilidades:
 * - Carregar modelo de embedding facial
 * - Gerar embeddings de imagens faciais
 * - Comparar similaridade entre embeddings
 */
class GenerateEmbeddingUseCase(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var lastEmbeddingFailureReportAt = 0L
    private val inputSize = 112 // MobileFaceNet usa 112x112
    private val embeddingSize = 192 // Tamanho do embedding do MobileFaceNet

    companion object {
        private const val TAG = "GenerateEmbeddingUseCase"
        private const val MODEL_FILE = "mobile_face_net.tflite"
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
        private const val EMBEDDING_FAILURE_REPORT_INTERVAL_MS = 60_000L
    }

    init {
        loadModel()
    }

    /**
     * Carrega o modelo TFLite do assets
     */
    private fun loadModel() {
        try {
            val modelFile = loadModelFile(context, MODEL_FILE)

            // LiteRT não precisa configurar GPU explicitamente
            // Ele detecta automaticamente
            interpreter = Interpreter(modelFile)
            Log.d(TAG, "✅ Modelo LiteRT carregado com sucesso")
            AppObservability.breadcrumb(
                category = "face_detection",
                message = "Face embedding model loaded",
                data = mapOf("model_file" to MODEL_FILE)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar modelo: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "face_detection",
                operation = "load_embedding_model",
                extras = mapOf("model_file" to MODEL_FILE)
            )
        }
    }

    /**
     * Carrega arquivo do modelo dos assets
     */
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        try {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar arquivo de modelo: ${e.message}", e)
            throw e
        }
    }

    /**
     * Gera embedding de uma face detectada
     * @param faceBitmap Bitmap contendo a face
     * @return FloatArray com o embedding (192 dimensões)
     */
    fun generateEmbedding(faceBitmap: Bitmap): FloatArray? {
        try {
            // Pré-processa a imagem
            val processedBitmap = preprocessImage(faceBitmap)
            val inputBuffer = bitmapToByteBuffer(processedBitmap)

            // Prepara output
            val output = Array(1) { FloatArray(embeddingSize) }

            // Executa inferência
            interpreter?.run(inputBuffer, output)

            // Normaliza o embedding
            val embedding = output[0]
            return normalizeEmbedding(embedding)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao gerar embedding: ${e.message}", e)
            reportEmbeddingFailureIfNeeded(e, faceBitmap.width, faceBitmap.height)
            return null
        }
    }

    private fun reportEmbeddingFailureIfNeeded(error: Throwable, bitmapWidth: Int, bitmapHeight: Int) {
        val now = System.currentTimeMillis()
        if (now - lastEmbeddingFailureReportAt < EMBEDDING_FAILURE_REPORT_INTERVAL_MS) return
        lastEmbeddingFailureReportAt = now

        AppObservability.captureException(
            throwable = error,
            area = "face_detection",
            operation = "generate_embedding",
            extras = mapOf(
                "bitmap_width" to bitmapWidth,
                "bitmap_height" to bitmapHeight,
                "model_file" to MODEL_FILE
            )
        )
    }

    /**
     * Pré-processa a imagem para o formato esperado pelo modelo
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Redimensiona para 112x112
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Garante formato ARGB_8888
        return if (resizedBitmap.config != Bitmap.Config.ARGB_8888) {
            val convertedBitmap = Bitmap.createBitmap(
                inputSize,
                inputSize,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(convertedBitmap)
            canvas.drawBitmap(resizedBitmap, 0f, 0f, null)
            resizedBitmap.recycle()
            convertedBitmap
        } else {
            resizedBitmap
        }
    }

    /**
     * Converte Bitmap para ByteBuffer normalizado
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]

                // Extrai RGB e normaliza
                val r = ((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD
                val g = ((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD
                val b = ((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        return byteBuffer
    }

    /**
     * Normaliza o embedding (L2 normalization)
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sumSquares = 0f
        for (value in embedding) {
            sumSquares += value * value
        }
        val norm = sqrt(sumSquares)

        if (norm == 0f) {
            Log.w(TAG, "⚠️ Norma é zero!")
            return embedding
        }

        return FloatArray(embedding.size) { i ->
            embedding[i] / norm
        }
    }

    /**
     * Compara dois embeddings usando similaridade de cosseno
     * @return Valor entre -1 e 1 (quanto maior, mais similar)
     */
    fun compareEmbeddings(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            Log.e(TAG, "❌ Embeddings com tamanhos diferentes: ${embedding1.size} vs ${embedding2.size}")
            return -1f
        }

        // Como os embeddings já estão normalizados,
        // a similaridade de cosseno é simplesmente o produto escalar
        var similarity = 0f
        for (i in embedding1.indices) {
            similarity += embedding1[i] * embedding2[i]
        }

        return similarity
    }

    /**
     * Compara dois bitmaps gerando seus embeddings
     * @return Similaridade entre as faces (0 a 1)
     */
    fun compareFaces(face1: Bitmap, face2: Bitmap): Float? {
        val embedding1 = generateEmbedding(face1) ?: return null
        val embedding2 = generateEmbedding(face2) ?: return null

        val similarity = compareEmbeddings(embedding1, embedding2)

        // Converte de [-1, 1] para [0, 1]
        return (similarity + 1f) / 2f
    }

    /**
     * Verifica se duas faces são da mesma pessoa
     * @param threshold Limiar de similaridade (padrão 0.75)
     */
    fun areSamePerson(face1: Bitmap, face2: Bitmap, threshold: Float = 0.75f): Boolean {
        val similarity = compareFaces(face1, face2) ?: return false
        return similarity >= threshold
    }

    /**
     * Calcula distância euclidiana entre embeddings
     */
    fun euclideanDistance(embedding1: FloatArray, embedding2: FloatArray): Float {
        if (embedding1.size != embedding2.size) {
            return Float.MAX_VALUE
        }

        var sum = 0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    /**
     * Libera recursos
     */
    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            Log.d(TAG, "✅ Recursos liberados")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Erro ao liberar recursos: ${e.message}")
        }
    }
}
