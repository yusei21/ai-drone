package com.sloth.registerapp.features.facedetection.domain.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.sloth.registerapp.features.facedetection.data.local.FaceDatabase
import com.sloth.registerapp.features.facedetection.data.local.FaceEntity
import com.sloth.registerapp.features.facedetection.data.repository.FaceRepositoryImpl
import com.sloth.registerapp.features.facedetection.domain.model.FaceCaptureState
import com.sloth.registerapp.features.facedetection.domain.model.FaceRegistrationResult
import com.sloth.registerapp.features.facedetection.domain.usecase.CaptureFaceUseCase
import com.sloth.registerapp.features.facedetection.domain.usecase.SaveFaceUseCase
import com.sloth.registerapp.features.facedetection.domain.usecase.GenerateEmbeddingUseCase
import com.sloth.registerapp.presentation.facedetection.activities.FaceRegistrationActivity
import com.sloth.registerapp.presentation.facedetection.activities.RegisteredFacesActivity
import kotlinx.coroutines.flow.Flow

/**
 * UseCase para Registro Completo de Rosto
 *
 * Orquestra o fluxo completo de registro facial:
 * - Captura de imagem
 * - Geração de embedding
 * - Verificação de duplicatas
 * - Salvamento em banco de dados
 *
 * API pública para registrar rostos no banco de dados
 *
 * Uso:
 * ```
 * val registerFaceUseCase = RegisterFaceUseCase.getInstance(context)
 * registerFaceUseCase.registerFace(bitmap, "João Silva")
 * ```
 *
 * Benefícios:
 * ✅ Desacoplado - Não depende de nada externo
 * ✅ Thread-safe - Singleton com Double-Checked Locking
 * ✅ Reativo - Usa Flow para observar mudanças
 * ✅ Completo - Captura, verifica duplicatas e salva
 */
class RegisterFaceUseCase private constructor(context: Context) {

    private val appContext = context.applicationContext

    // Inicializa componentes internos
    // Exponha os componentes para o ViewModel
    internal val embeddingEngine = GenerateEmbeddingUseCase(appContext)
    private val database = FaceDatabase.getInstance(appContext)
    internal val repository = FaceRepositoryImpl(database.faceDao(), embeddingEngine)

    // Use cases
    private val captureFaceUseCase = CaptureFaceUseCase(embeddingEngine, repository)
    private val saveFaceUseCase = SaveFaceUseCase(repository)

    companion object {
        private const val TAG = "RegisterFaceUseCase"

        // Singleton
        @Volatile
        private var INSTANCE: RegisterFaceUseCase? = null

        // Constantes para Intent extras
        const val REQUEST_CODE_FACE_CAPTURE = 1001
        const val EXTRA_FACE_NAME = "face_name"
        const val EXTRA_FACE_ID = "face_id"
        const val EXTRA_IS_DUPLICATE = "is_duplicate"

        /**
         * Obtém a instância singleton do use case
         *
         * @param context Contexto da aplicação
         * @return Instância do RegisterFaceUseCase
         */
        fun getInstance(context: Context): RegisterFaceUseCase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "🚀 Inicializando RegisterFaceUseCase")
                val instance = RegisterFaceUseCase(context)
                INSTANCE = instance
                instance
            }
        }
    }

    // ========== Métodos Públicos - Captura ==========

    /**
     * Abre a tela de captura de rosto
     *
     * Exemplo de uso com Activity:
     * ```
     * faceService.openCaptureScreen(this)
     * ```
     *
     * @param context Contexto para iniciar a Activity
     */
    fun openCaptureScreen(context: Context) {
        Log.d(TAG, "📱 Abrindo tela de captura...")
        val intent = createCaptureIntent(context)
        context.startActivity(intent)
    }

    fun openRegisteredFacesScreen(context: Context) {
        val intent = Intent(context, RegisteredFacesActivity::class.java)
        context.startActivity(intent)
    }

    /**
     * Cria um Intent para abrir a tela de captura
     *
     * Use com registerForActivityResult:
     * ```
     * val launcher = registerForActivityResult(
     *     ActivityResultContracts.StartActivityForResult()
     * ) { result ->
     *     if (result.resultCode == RESULT_OK) {
     *         val faceId = result.data?.getLongExtra(EXTRA_FACE_ID, -1)
     *         val name = result.data?.getStringExtra(EXTRA_FACE_NAME)
     *     }
     * }
     * launcher.launch(faceService.createCaptureIntent(this))
     * ```
     *
     * @param context Contexto
     * @return Intent para iniciar a captura
     */
    fun createCaptureIntent(context: Context): Intent {
        return Intent(context, FaceRegistrationActivity::class.java)
    }

    fun createRegisteredIntent(context: Context): Intent{
        return Intent(context, RegisteredFacesActivity::class.java)
    }

    /**
     * Gera o embedding de um rosto a partir de um Bitmap.
     *
     * Este método é um atalho para acessar a funcionalidade do GenerateEmbeddingUseCase.
     *
     * @param bitmap O bitmap contendo o rosto.
     * @return Um FloatArray representando o embedding, ou null se a geração falhar.
     */
    fun generateEmbedding(bitmap: Bitmap): FloatArray? {
        Log.d(TAG, "🧠 Gerando embedding facial via serviço...")
        return embeddingEngine.generateEmbedding(bitmap)
    }

    // ========== Métodos Públicos - Processamento ==========

    /**
     * Captura um rosto de um Bitmap e salva no banco de dados
     *
     * Exemplo:
     * ```
     * val result = faceService.registerFace(bitmap, "João Silva")
     * when (result) {
     *     is FaceRegistrationResult.Success -> Log.d("TAG", "Salvo: ${result.id}")
     *     is FaceRegistrationResult.Duplicate -> Log.w("TAG", "Já existe")
     *     is FaceRegistrationResult.Error -> Log.e("TAG", result.message)
     * }
     * ```
     *
     * @param bitmap Imagem contendo o rosto
     * @param name Nome da pessoa
     * @return FaceRegistrationResult com o resultado da operação
     */
    suspend fun registerFace(bitmap: Bitmap, name: String): FaceRegistrationResult {
        Log.d(TAG, "📸 Registrando novo rosto: $name")

        // 1. Captura e gera embedding
        val captureResult = captureFaceUseCase(bitmap)

        // 2. Processa resultado da captura
        return when (captureResult) {
            is FaceCaptureState.Success -> {
                // Se for duplicata, retorna duplicata
                if (captureResult.isDuplicate && captureResult.existingFace != null) {
                    Log.w(TAG, "⚠️ Rosto duplicado detectado")
                    FaceRegistrationResult.Duplicate(captureResult.existingFace)
                } else {
                    // Salva o rosto
                    saveFaceUseCase(name, captureResult.embedding)
                }
            }
            is FaceCaptureState.Error -> {
                Log.e(TAG, "❌ Erro na captura: ${captureResult.message}")
                FaceRegistrationResult.Error(captureResult.message)
            }
            else -> {
                FaceRegistrationResult.Error("Estado inválido")
            }
        }
    }

    /**
     * Verifica se um rosto já está cadastrado (sem salvar)
     *
     * Exemplo:
     * ```
     * val (isDuplicate, existingFace) = faceService.checkDuplicate(bitmap)
     * if (isDuplicate) {
     *     Toast.makeText(this, "Já cadastrado: ${existingFace?.name}", LENGTH_SHORT).show()
     * }
     * ```
     *
     * @param bitmap Imagem para verificar
     * @return Pair<Boolean, FaceEntity?> - (é duplicata?, face encontrada)
     */
    suspend fun checkDuplicate(bitmap: Bitmap, similarityThreshold: Float? = null): Pair<Boolean, FaceEntity?> {
        Log.d(TAG, "🔍 Verificando se é duplicata...")

        val embedding = embeddingEngine.generateEmbedding(bitmap) ?: return Pair(false, null)

        return if (similarityThreshold != null) {
            repository.findSimilarFace(embedding, similarityThreshold)
        } else {
            // usa o valor padrão da função findSimilarFace (que tem parâmetro default)
            repository.findSimilarFace(embedding)
        }
    }

    // ========== Métodos Públicos - Leitura ==========

    /**
     * Retorna todas as faces cadastradas (Flow reativo)
     *
     * Use para observar mudanças em tempo real:
     * ```
     * lifecycleScope.launch {
     *     faceService.getAllFaces().collect { faces ->
     *         adapter.submitList(faces)
     *     }
     * }
     * ```
     *
     * @return Flow<List<FaceEntity>>
     */
    fun getAllFaces(): Flow<List<FaceEntity>> {
        Log.d(TAG, "📋 Retornando todas as faces (Flow)")
        return repository.allFaces
    }

    /**
     * Retorna todas as faces cadastradas (versão síncrona)
     *
     * @return Lista de todas as faces
     */
    suspend fun getAllFacesSync(): List<FaceEntity> {
        Log.d(TAG, "📋 Retornando todas as faces (Sync)")
        return repository.getAllFacesSync()
    }

    /**
     * Busca uma face pelo ID
     *
     * @param id ID da face
     * @return FaceEntity ou null
     */
    suspend fun getFaceById(id: Long): FaceEntity? {
        return repository.getFaceById(id)
    }

    /**
     * Retorna o número total de faces cadastradas
     *
     * @return Quantidade de faces
     */
    suspend fun getFaceCount(): Int {
        return repository.getCount()
    }

    // ========== Métodos Públicos - Deleção ==========

    /**
     * Deleta uma face específica
     *
     * @param face FaceEntity a deletar
     */
    suspend fun deleteFace(face: FaceEntity) {
        Log.d(TAG, "🗑️ Deletando rosto: ${face.name}")
        repository.deleteFace(face)
    }

    /**
     * Deleta todas as faces cadastradas
     *
     * ⚠️ Operação irreversível!
     */
    suspend fun deleteAllFaces() {
        Log.d(TAG, "🗑️ Deletando TODAS as faces!")
        repository.deleteAll()
    }

    // ========== Métodos Públicos - Cleanup ==========

    /**
     * Libera recursos do serviço
     *
     * Chame no onDestroy da sua Application:
     * ```
     * override fun onTerminate() {
     *     registerFaceUseCase.release()
     *     super.onTerminate()
     * }
     * ```
     */
    fun release() {
        Log.d(TAG, "🛑 Liberando recursos do RegisterFaceUseCase")
        embeddingEngine.close()
        INSTANCE = null
    }
}
