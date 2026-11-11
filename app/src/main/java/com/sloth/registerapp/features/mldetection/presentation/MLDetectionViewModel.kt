package com.sloth.registerapp.features.mldetection.presentation

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.network.httpStatusCodeOrNull
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.mldetection.data.download.ModelDownloadManager
import com.sloth.registerapp.features.mldetection.data.ml.YoloInferenceEngine
import com.sloth.registerapp.features.mldetection.data.remote.dto.ModelCatalogEntryDto
import com.sloth.registerapp.features.mldetection.data.registry.ModelRegistry
import com.sloth.registerapp.features.mldetection.domain.model.DetectionResult
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.sentry.SentryLevel

class MLDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getInstance(application)
    private val registry = ModelRegistry.getInstance(application)
    val downloadManager = ModelDownloadManager(registry)
    private val inferenceMutex = Mutex()

    // Available models (live, reacts to drop-in changes)
    val availableModels: StateFlow<List<MLModel>> = registry.models
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId

    private val _detectionBoxes = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionBoxes: StateFlow<List<DetectionResult>> = _detectionBoxes

    private val _lastInferenceMs = MutableStateFlow<Long?>(null)
    val lastInferenceMs: StateFlow<Long?> = _lastInferenceMs

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _remoteCatalog = MutableStateFlow<List<ModelCatalogEntryDto>>(emptyList())
    val remoteCatalog: StateFlow<List<ModelCatalogEntryDto>> = _remoteCatalog

    private val _remoteCatalogLoading = MutableStateFlow(false)
    val remoteCatalogLoading: StateFlow<Boolean> = _remoteCatalogLoading

    private var activeEngine: YoloInferenceEngine? = null
    private var lastInferenceFailureReportAt = 0L

    init {
        // Auto-select first available model when catalog loads
        viewModelScope.launch {
            availableModels.collect { models ->
                val currentId = _activeModelId.value
                if (currentId == null || models.none { it.id == currentId }) {
                    val firstId = models.firstOrNull()?.id
                    if (firstId != null) {
                        selectModel(firstId)
                    }
                }
            }
        }

        refreshRemoteCatalog()
    }

    fun selectModel(modelId: String) {
        if (_activeModelId.value == modelId) return
        _activeModelId.value = modelId
        AppObservability.breadcrumb(
            category = "ml",
            message = "ML model selected",
            data = mapOf("model_id" to modelId)
        )
        viewModelScope.launch(Dispatchers.IO) {
            loadEngine(modelId)
        }
    }

    fun clearDetections() {
        _detectionBoxes.value = emptyList()
        _lastError.value = null
    }

    /**
     * Runs YOLO inference on the provided bitmap (on IO dispatcher).
     * Recycles [bitmap] after use — do not access it after calling this.
     */
    fun processFrame(bitmap: Bitmap) {
        val engine = activeEngine ?: run {
            bitmap.recycle()
            return
        }
        if (inferenceMutex.isLocked) {
            bitmap.recycle()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            inferenceMutex.withLock {
                val start = System.currentTimeMillis()
                runCatching {
                    engine.detect(bitmap)
                }.onSuccess { results ->
                    _detectionBoxes.value = results
                    _lastInferenceMs.value = System.currentTimeMillis() - start
                    _lastError.value = null
                }.onFailure { e ->
                    Log.e(TAG, "Inference failed", e)
                    reportInferenceFailureIfNeeded(e, engine.model, bitmap.width, bitmap.height)
                    _detectionBoxes.value = emptyList()
                    _lastError.value = e.message ?: "Falha na inferência"
                }
                runCatching { bitmap.recycle() }
            }
        }
    }

    fun refreshRemoteCatalog() {
        viewModelScope.launch {
            _remoteCatalogLoading.value = true
            AppObservability.breadcrumb(
                category = "ml",
                message = "Remote model catalog requested"
            )
            runCatching {
                apiService.getMlModelCatalog()
            }.onSuccess { catalog ->
                _remoteCatalog.value = catalog
            }.onFailure { e ->
                Log.w(TAG, "Falha ao carregar catálogo remoto de modelos", e)
                AppObservability.captureException(
                    throwable = e,
                    area = "ml",
                    operation = "refresh_remote_catalog",
                    level = SentryLevel.WARNING,
                    tags = e.httpStatusCodeOrNull()?.let { mapOf("http.status_code" to it.toString()) } ?: emptyMap()
                )
                _lastError.value = e.message ?: "Falha ao carregar catálogo remoto"
            }
            _remoteCatalogLoading.value = false
        }
    }

    fun downloadRemoteModel(modelId: String) {
        val catalogEntry = _remoteCatalog.value.firstOrNull { it.id == modelId } ?: run {
            _lastError.value = "Modelo remoto não encontrado"
            return
        }
        val localFileName = sanitizeModelFileName(catalogEntry.id)
        viewModelScope.launch {
            _downloadProgress.value = 0f
            AppObservability.breadcrumb(
                category = "ml",
                message = "Remote model download URL requested",
                data = mapOf("model_id" to modelId)
            )
            runCatching {
                apiService.getMlModelDownloadUrl(modelId)
            }.onSuccess { downloadDto ->
                val result = downloadManager.download(
                    url = downloadDto.url,
                    fileName = localFileName,
                    classes = catalogEntry.classes,
                    expectedChecksum = downloadDto.checksum,
                    onProgress = { _downloadProgress.value = it }
                )
                _downloadProgress.value = null
                when (result) {
                    is ModelDownloadManager.DownloadResult.Success -> {
                        registry.reload()
                        selectModel(localModelId(localFileName))
                    }
                    is ModelDownloadManager.DownloadResult.Failure -> {
                        _lastError.value = "Download falhou: ${result.reason}"
                        AppObservability.captureMessage(
                            message = "ML model download failed",
                            area = "ml",
                            operation = "download_remote_model",
                            extras = mapOf(
                                "model_id" to modelId,
                                "reason" to result.reason
                            )
                        )
                    }
                }
            }.onFailure { e ->
                _downloadProgress.value = null
                AppObservability.captureException(
                    throwable = e,
                    area = "ml",
                    operation = "get_model_download_url",
                    level = SentryLevel.WARNING,
                    extras = mapOf("model_id" to modelId),
                    tags = e.httpStatusCodeOrNull()?.let { mapOf("http.status_code" to it.toString()) } ?: emptyMap()
                )
                _lastError.value = "Falha ao obter URL de download: ${e.message}"
            }
        }
    }

    fun downloadModel(
        url: String,
        fileName: String,
        classes: List<String>,
        expectedChecksum: String = ""
    ) {
        viewModelScope.launch {
            _downloadProgress.value = 0f
            val result = downloadManager.download(
                url = url,
                fileName = fileName,
                classes = classes,
                expectedChecksum = expectedChecksum,
                onProgress = { _downloadProgress.value = it }
            )
            _downloadProgress.value = null
            when (result) {
                is ModelDownloadManager.DownloadResult.Success -> {
                    registry.reload()
                    Log.d(TAG, "Download complete: ${result.file.name}")
                }
                is ModelDownloadManager.DownloadResult.Failure -> {
                    _lastError.value = "Download falhou: ${result.reason}"
                    Log.e(TAG, "Download failed: ${result.reason}")
                    AppObservability.captureMessage(
                        message = "Manual ML model download failed",
                        area = "ml",
                        operation = "download_model",
                        extras = mapOf(
                            "file_name" to sanitizeModelFileName(fileName),
                            "reason" to result.reason
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadEngine(modelId: String) {
        val model = availableModels.value.firstOrNull { it.id == modelId } ?: return
        withContext(Dispatchers.IO) {
            val old = activeEngine
            activeEngine = null
            runCatching { old?.close() }

            runCatching {
                AppObservability.breadcrumb(
                    category = "ml",
                    message = "Loading inference engine",
                    data = mapOf(
                        "model_id" to model.id,
                        "model_source" to model.source.name
                    )
                )
                YoloInferenceEngine.create(getApplication(), model)
            }.onSuccess { engine ->
                activeEngine = engine
                Log.d(TAG, "Engine loaded: ${model.name}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to load engine for ${model.name}", e)
                AppObservability.captureException(
                    throwable = e,
                    area = "ml",
                    operation = "load_inference_engine",
                    extras = mapOf(
                        "model_id" to model.id,
                        "model_source" to model.source.name,
                        "classes_count" to model.classes.size
                    )
                )
                _lastError.value = "Falha ao carregar modelo: ${e.message}"
            }
        }
    }

    private fun reportInferenceFailureIfNeeded(
        error: Throwable,
        model: MLModel,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        val now = System.currentTimeMillis()
        if (now - lastInferenceFailureReportAt < INFERENCE_FAILURE_REPORT_INTERVAL_MS) return
        lastInferenceFailureReportAt = now

        AppObservability.captureException(
            throwable = error,
            area = "ml",
            operation = "run_inference",
            extras = mapOf(
                "model_id" to model.id,
                "model_source" to model.source.name,
                "bitmap_width" to bitmapWidth,
                "bitmap_height" to bitmapHeight
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { activeEngine?.close() }
        registry.stop()
    }

    companion object {
        private const val TAG = "MLDetectionViewModel"
        private const val INFERENCE_FAILURE_REPORT_INTERVAL_MS = 60_000L

        fun sanitizeModelFileName(value: String): String {
            return value
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9._-]+"), "_")
                .trim('_')
                .ifBlank { "ml_model" }
        }

        fun localModelId(fileName: String): String = "local-$fileName"
    }
}
