package com.sloth.registerapp.presentation.facedetection.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.features.facedetection.domain.usecase.RegisterFaceUseCase
import com.sloth.registerapp.features.facedetection.data.local.FaceEntity
import com.sloth.registerapp.features.facedetection.domain.model.FaceRegistrationResult
import com.sloth.registerapp.features.facedetection.domain.usecase.SaveFaceUseCase
import com.sloth.registerapp.features.facedetection.data.ml.FaceAnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar o estado da tela de registro facial
 */
class FaceRegistrationViewModel(
    val faceService: RegisterFaceUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "FaceRegistrationViewModel"
    }

    private val _uiState = MutableStateFlow<FaceRegistrationUiState>(FaceRegistrationUiState.Scanning)
    val uiState: StateFlow<FaceRegistrationUiState> = _uiState.asStateFlow()

    private val _analysisResult = MutableStateFlow<FaceAnalysisResult>(FaceAnalysisResult.NoFace)
    val analysisResult: StateFlow<FaceAnalysisResult> = _analysisResult.asStateFlow()

    // Estado temporário durante a captura
    private var currentBitmap: Bitmap? = null
    private var currentEmbedding: FloatArray? = null
    private var isDuplicate: Boolean = false
    private var duplicateFace: FaceEntity? = null

    // ========== Métodos Públicos ==========

    fun updateAnalysisResult(result: FaceAnalysisResult) {
        _analysisResult.value = result
    }

    /**
     * Processa a captura da foto
     * Apenas gera embedding e verifica duplicata, SEM salvar ainda
     */
    fun processCapture(bitmap: Bitmap) {
        Log.d(TAG, "📸 Processando captura...")
        viewModelScope.launch {
            _uiState.value = FaceRegistrationUiState.Processing

            try {
                // 1. Gera embedding
                Log.d(TAG, "🧠 Gerando embedding...")
                val embedding = faceService.embeddingEngine.generateEmbedding(bitmap)

                if (embedding == null) {
                    Log.e(TAG, "❌ Falha ao gerar embedding")
                    _uiState.value = FaceRegistrationUiState.Error("Falha ao processar rosto")
                    return@launch
                }

                Log.d(TAG, "✅ Embedding gerado")

                // 2. Verifica duplicata
                Log.d(TAG, "🔍 Verificando duplicatas...")
                val (isDup, dupFace) = faceService.repository.findSimilarFace(
                    embedding
                )

                // 3. Salva estado para depois
                currentBitmap = bitmap
                currentEmbedding = embedding
                isDuplicate = isDup
                duplicateFace = dupFace

                if (isDuplicate && dupFace != null) {
                    Log.w(TAG, "⚠️ Duplicata encontrada: ${dupFace.name}")
                }

                // 4. Vai para tela de resultado
                _uiState.value = FaceRegistrationUiState.Success(
                    bitmap = bitmap,
                    embedding = embedding,
                    isDuplicate = isDuplicate,
                    existingFace = duplicateFace
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao processar captura: ${e.message}", e)
                _uiState.value = FaceRegistrationUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Salva o rosto capturado com o nome fornecido
     * Só chama isso DEPOIS de processCapture
     */
    fun saveFace(name: String, embedding: FloatArray) {
        Log.d(TAG, "💾 Salvando rosto: $name")

        // Validação
        if (name.isBlank()) {
            Log.w(TAG, "⚠️ Nome vazio")
            _uiState.value = FaceRegistrationUiState.Error("Nome não pode estar vazio")
            return
        }

        if (isDuplicate) {
            Log.w(TAG, "⚠️ Tentando salvar duplicata")
            _uiState.value = FaceRegistrationUiState.Error(
                "Este rosto já foi cadastrado como '${duplicateFace?.name}'"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = FaceRegistrationUiState.Saving

            try {
                // Salva usando SaveFaceUseCase (sem verificar duplicata novamente)
                val saveFaceUseCase = SaveFaceUseCase(faceService.repository)
                val result = saveFaceUseCase(name, embedding)

                // Limpa estado temporário
                currentBitmap = null
                currentEmbedding = null
                isDuplicate = false
                duplicateFace = null

                when (result) {
                    is FaceRegistrationResult.Success -> {
                        Log.d(TAG, "✅ Rosto salvo com ID: ${result.id}")
                        _uiState.value = FaceRegistrationUiState.Saved(name)
                    }
                    is FaceRegistrationResult.Error -> {
                        Log.e(TAG, "❌ Erro ao salvar: ${result.message}")
                        _uiState.value = FaceRegistrationUiState.Error(result.message)
                    }
                    is FaceRegistrationResult.Duplicate -> {
                        Log.w(TAG, "⚠️ Detectada duplicata: ${result.existingFace.name}")
                        _uiState.value = FaceRegistrationUiState.Error(
                            "Rosto já cadastrado como '${result.existingFace.name}'"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exceção ao salvar: ${e.message}", e)
                _uiState.value = FaceRegistrationUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    /**
     * Reseta o estado para início
     */
    fun resetState() {
        Log.d(TAG, "🔄 Resetando estado")
        currentBitmap = null
        currentEmbedding = null
        isDuplicate = false
        duplicateFace = null
        _uiState.value = FaceRegistrationUiState.Scanning
        _analysisResult.value = FaceAnalysisResult.NoFace
    }
}

// ========== Estados da UI ==========

sealed class FaceRegistrationUiState {
    object Scanning : FaceRegistrationUiState()
    object Processing : FaceRegistrationUiState()
    object Saving : FaceRegistrationUiState()

    data class Success(
        val bitmap: Bitmap,
        val embedding: FloatArray,
        val isDuplicate: Boolean = false,
        val existingFace: FaceEntity? = null
    ) : FaceRegistrationUiState()

    data class Saved(val name: String) : FaceRegistrationUiState()
    data class Error(val message: String) : FaceRegistrationUiState()
}

// ========== Factory ==========

class FaceRegistrationViewModelFactory(
    private val faceService: RegisterFaceUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceRegistrationViewModel::class.java)) {
            return FaceRegistrationViewModel(faceService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

