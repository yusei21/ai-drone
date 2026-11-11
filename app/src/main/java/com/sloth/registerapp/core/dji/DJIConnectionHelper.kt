package com.sloth.registerapp.core.dji

import android.content.Context
import android.util.Log
import android.widget.Toast
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object DJIConnectionHelper {

    private const val TAG = "ApplicationDJI"

    // StateFlow para emitir o status da conexão para a UI
    private val _connectionStatus = MutableStateFlow("Aguardando inicialização...")
    val connectionStatus = _connectionStatus.asStateFlow()

    private var isRegistered = false
    private var isRegistering = false
    private var appContext: Context? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // StateFlow para emitir o produto (drone) quando ele se conecta
    private val _product = MutableStateFlow<BaseProduct?>(null)
    val product = _product.asStateFlow()

    // Função para obter a instância do produto de forma síncrona
    fun getProductInstance(): BaseProduct? = _product.value

    // Função principal que a MainActivity chamará
    fun registerApp(context: Context) {
        appContext = context.applicationContext
        if (isRegistered || isRegistering) {
            refreshConnectionStatus()
            return
        }

        isRegistering = true
        _connectionStatus.value = "Registrando aplicativo..."
        Log.d(TAG, "Iniciando o registro do SDK DJI...")

        DJISDKManager.getInstance().registerApp(context, object : DJISDKManager.SDKManagerCallback {
            override fun onRegister(error: DJIError?) {
                isRegistering = false
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.d(TAG, "Registro do SDK bem-sucedido!")
                    isRegistered = true
                    _connectionStatus.value = "Pronto para Conexão"
                    // Não precisa de runOnUiThread aqui, o Toast já faz isso se necessário
                    Toast.makeText(context, "Registro do SDK bem-sucedido!", Toast.LENGTH_SHORT).show()
                    DJISDKManager.getInstance().startConnectionToProduct()
                } else {
                    Log.d(TAG, "Falha no registro do SDK: ${error?.description}")
                    _connectionStatus.value = "Falha no registro"
                    Toast.makeText(context, "Falha no registro: ${error?.description}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onProductDisconnect() {
                Log.d(TAG, "Produto DJI desconectado.")
                _connectionStatus.value = if (isRegistered) {
                    "Pronto para Conexão"
                } else {
                    "Produto Desconectado"
                }
                _product.value = null
            }

            override fun onProductConnect(product: BaseProduct?) {
                val modelName = product?.model?.displayName ?: "Modelo Desconhecido"
                Log.d(TAG, "Produto DJI conectado: $modelName")
                _connectionStatus.value = "Conectado a: $modelName"
                _product.value = product
            }

            override fun onProductChanged(product: BaseProduct?) {
                _product.value = product
                refreshConnectionStatus()
            }

            override fun onComponentChange(key: BaseProduct.ComponentKey?, oldC: BaseComponent?, newC: BaseComponent?) {}
            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    fun refreshConnectionStatus() {
        val currentProduct = DJISDKManager.getInstance().product
        _product.value = currentProduct
        val modelName = currentProduct?.model?.displayName ?: "Modelo Desconhecido"
        _connectionStatus.value = when {
            currentProduct == null && isRegistered -> "Pronto para Conexão"
            currentProduct == null -> "Produto Desconectado"
            currentProduct.isConnected -> "Conectado a: $modelName"
            else -> "Conectando..."
        }
    }

    fun tryReconnect(context: Context? = null) {
        context?.let { appContext = it.applicationContext }
        val safeContext = appContext

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionStatus.value = "Sincronizando..."

            // Se não registrou ainda, força o registro antes da reconexão.
            if (!isRegistered && !isRegistering) {
                if (safeContext != null) {
                    registerApp(safeContext)
                } else {
                    _connectionStatus.value = "Contexto indisponível para reconexão"
                    return@launch
                }
            }

            // Tenta reconectar algumas vezes para não depender de restart do app.
            repeat(4) { attempt ->
                DJISDKManager.getInstance().startConnectionToProduct()
                delay(1500)
                refreshConnectionStatus()
                if (_product.value != null) {
                    Log.d(TAG, "Reconexão concluída na tentativa ${attempt + 1}")
                    return@launch
                }
            }

            // Fallback: força novo ciclo de registro se ainda não conectou.
            if (_product.value == null && safeContext != null && !isRegistering) {
                Log.w(TAG, "Reconexão sem sucesso. Forçando novo registro do SDK.")
                isRegistered = false
                registerApp(safeContext)
            }
            refreshConnectionStatus()
        }
    }
}
