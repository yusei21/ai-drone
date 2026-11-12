package com.sloth.registerapp.features.mldetection.data.registry

import android.content.Context
import android.os.FileObserver
import android.util.Log
import com.sloth.registerapp.features.mldetection.domain.model.MLModel
import com.sloth.registerapp.features.mldetection.domain.model.MLModelSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Maintains a live catalog of available YOLO TFLite models.
 *
 * Sources:
 * - **Local files**: any .tflite dropped into [modelsDir] (`filesDir/ml_models/`)
 *   accompanied by an optional .txt file with one class label per line.
 *
 * Changes to [modelsDir] are picked up automatically via [FileObserver].
 */
class ModelRegistry(private val context: Context) {

    val modelsDir: File = File(context.filesDir, "ml_models").also { it.mkdirs() }

    private val _models = MutableStateFlow<List<MLModel>>(emptyList())
    val models: StateFlow<List<MLModel>> = _models

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val watcher = object : FileObserver(modelsDir.absolutePath, CREATE or DELETE or MOVED_TO or MOVED_FROM) {
        override fun onEvent(event: Int, path: String?) {
            if (path?.endsWith(".tflite") == true || path?.endsWith(".txt") == true) {
                Log.d(TAG, "FileObserver event=$event path=$path — reloading catalog")
                reload()
            }
        }
    }

    init {
        reload()
        watcher.startWatching()
    }

    fun reload() {
        scope.launch {
            _models.value = buildCatalog()
        }
    }

    fun stop() {
        watcher.stopWatching()
    }

    private fun buildCatalog(): List<MLModel> {
        val catalog = mutableListOf<MLModel>()
        catalog += scanLocalFiles()
        // Fallback to the model bundled inside the APK when no local override exists.
        // The local file with the same canonical name takes precedence.
        if (catalog.none { it.id == DEFAULT_LOCAL_ID }) {
            catalog += scanBundledDefaultModel()
        }
        Log.d(TAG, "Catalog rebuilt: ${catalog.size} model(s)")
        return catalog
    }

    private fun scanLocalFiles(): List<MLModel> {
        return modelsDir.listFiles { f -> f.isFile && f.name.endsWith(".tflite", ignoreCase = true) && f.length() > 0L }
            ?.sortedWith(compareBy<File> { it.name != DEFAULT_FILE_NAME }.thenBy { it.name })
            ?.mapNotNull { file ->
                runCatching { buildLocalModel(file) }
                    .onFailure { Log.e(TAG, "Skipping ${file.name}: ${it.message}") }
                    .getOrNull()
            }
            ?: emptyList()
    }

    private fun buildLocalModel(file: File): MLModel {
        val baseName = file.nameWithoutExtension
        val classesFile = File(modelsDir, "$baseName.txt")
        val classes = if (baseName == "pessoas_ativo") {
            listOf("pessoa")
        } else if (classesFile.exists()) {
            classesFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        val id = "local-$baseName"
        val displayName = baseName.replace('_', ' ').replaceFirstChar { it.uppercase() }
        return MLModel(
            id = id,
            name = displayName,
            classes = classes.ifEmpty { listOf("Objeto") },
            filePath = file.absolutePath,
            source = MLModelSource.LOCAL_FILE,
            sizeBytes = file.length()
        )
    }

    private fun scanBundledDefaultModel(): List<MLModel> {
        val assetPath = "ml_models/$DEFAULT_FILE_NAME"
        return runCatching {
            // openFd also checks whether the asset can be memory-mapped by TFLite.
            context.assets.openFd(assetPath).use { descriptor ->
                MLModel(
                    id = DEFAULT_ASSET_ID,
                    name = "Pessoas (integrado)",
                    classes = listOf("pessoa"),
                    filePath = assetPath,
                    source = MLModelSource.ASSET,
                    sizeBytes = descriptor.length
                )
            }
        }.onFailure { Log.d(TAG, "Bundled model not installed: $assetPath") }
            .getOrNull()?.let(::listOf) ?: emptyList()
    }

    companion object {
        const val DEFAULT_FILE_NAME = "pessoas_ativo.tflite"
        const val DEFAULT_LOCAL_ID = "local-pessoas_ativo"
        const val DEFAULT_ASSET_ID = "asset-pessoas_ativo"
        private const val TAG = "ModelRegistry"

        @Volatile private var instance: ModelRegistry? = null

        fun getInstance(context: Context): ModelRegistry {
            return instance ?: synchronized(this) {
                instance ?: ModelRegistry(context.applicationContext).also { instance = it }
            }
        }
    }
}
