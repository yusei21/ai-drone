package com.sloth.registerapp.features.mldetection.data.download

import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.mldetection.data.registry.ModelRegistry
import io.sentry.SentryLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * Downloads a YOLO TFLite model from a URL into [ModelRegistry.modelsDir].
 *
 * Usage:
 * ```
 * val manager = ModelDownloadManager(registry)
 * manager.download(
 *     url = "https://example.com/models/vehicle_detector.tflite",
 *     fileName = "vehicle_detector",
 *     expectedChecksum = "abc123...",
 *     classes = listOf("Carro", "Moto", "Caminhao"),
 *     onProgress = { progress -> ... }
 * )
 * ```
 * After completion, [ModelRegistry] picks up the new file automatically via FileObserver.
 */
class ModelDownloadManager(
    private val registry: ModelRegistry,
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    sealed class DownloadResult {
        data class Success(val file: File) : DownloadResult()
        data class Failure(val reason: String) : DownloadResult()
    }

    suspend fun download(
        url: String,
        fileName: String,
        expectedChecksum: String = "",
        classes: List<String> = emptyList(),
        onProgress: (Float) -> Unit = {}
    ): DownloadResult = withContext(Dispatchers.IO) {
        val destFile = File(registry.modelsDir, "$fileName.tflite")
        val tempFile = File(registry.modelsDir, "$fileName.tmp")

        runCatching {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            response.use {
                if (!response.isSuccessful) {
                    AppObservability.captureMessage(
                        message = "ML model binary download returned HTTP error",
                        area = "ml",
                        operation = "download_model_binary",
                        level = SentryLevel.WARNING,
                        tags = mapOf("http.status_code" to response.code.toString()),
                        extras = mapOf("file_name" to fileName)
                    )
                    return@withContext DownloadResult.Failure("HTTP ${response.code}")
                }

                val body = response.body ?: return@withContext DownloadResult.Failure("Empty response body")
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
                var downloadedBytes = 0L

                AppObservability.breadcrumb(
                    category = "ml",
                    message = "ML model binary download started",
                    data = mapOf(
                        "file_name" to fileName,
                        "content_length" to totalBytes
                    )
                )

                tempFile.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(8192)
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            out.write(buf, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }

                if (expectedChecksum.isNotBlank()) {
                    val actualChecksum = sha256(tempFile)
                    if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                        tempFile.delete()
                        AppObservability.captureMessage(
                            message = "ML model checksum mismatch",
                            area = "ml",
                            operation = "download_model_binary",
                            extras = mapOf("file_name" to fileName)
                        )
                        return@withContext DownloadResult.Failure("Checksum mismatch")
                    }
                }

                // Write class labels if provided
                if (classes.isNotEmpty()) {
                    File(registry.modelsDir, "$fileName.txt").writeText(classes.joinToString("\n"))
                }

                tempFile.renameTo(destFile)
                Log.d(TAG, "Model downloaded: ${destFile.name}")
                DownloadResult.Success(destFile)
            }
        }.getOrElse { e ->
            tempFile.delete()
            Log.e(TAG, "Download failed: ${e.message}", e)
            AppObservability.captureException(
                throwable = e,
                area = "ml",
                operation = "download_model_binary",
                extras = mapOf("file_name" to fileName)
            )
            DownloadResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "ModelDownloadManager"
    }
}
