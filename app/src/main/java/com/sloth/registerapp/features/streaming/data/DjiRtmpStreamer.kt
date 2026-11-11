package com.sloth.registerapp.features.streaming.data

import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.streaming.domain.StreamingController
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DjiRtmpStreamer(
    private var rtmpUrl: String
) : StreamingController {

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state: StateFlow<StreamState> = _state

    private val tag = "DjiRtmpStreamer"

    override fun start() {
        if (_state.value is StreamState.Streaming || _state.value is StreamState.Connecting) return
        _state.value = StreamState.Connecting

        try {
            val liveStreamManager = getLiveStreamManager()
            if (liveStreamManager == null) {
                _state.value = StreamState.Error("LiveStreamManager indisponível")
                Log.e(tag, "LiveStreamManager não encontrado")
                AppObservability.captureMessage(
                    message = "DJI LiveStreamManager unavailable",
                    area = "streaming",
                    operation = "dji_rtmp_start"
                )
                return
            }

            if (!setLiveUrl(liveStreamManager, rtmpUrl)) {
                _state.value = StreamState.Error("Falha ao configurar URL RTMP")
                Log.e(tag, "Falha ao configurar URL RTMP")
                AppObservability.captureMessage(
                    message = "DJI RTMP URL configuration failed",
                    area = "streaming",
                    operation = "dji_rtmp_set_url"
                )
                return
            }

            // Desabilita áudio, se o método existir
            setAudioMuted(liveStreamManager, true)

            val started = startStream(liveStreamManager)
            if (started) {
                _state.value = StreamState.Streaming
                Log.d(tag, "RTMP streaming iniciado")
            } else {
                _state.value = StreamState.Error("Falha ao iniciar stream")
                Log.e(tag, "Falha ao iniciar stream")
                AppObservability.captureMessage(
                    message = "DJI RTMP stream start returned false",
                    area = "streaming",
                    operation = "dji_rtmp_start"
                )
            }
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao iniciar stream: ${e.message}")
            Log.e(tag, "Erro ao iniciar stream", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "dji_rtmp_start"
            )
        }
    }

    override fun stop() {
        try {
            val liveStreamManager = getLiveStreamManager() ?: return
            stopStream(liveStreamManager)
            _state.value = StreamState.Idle
            Log.d(tag, "RTMP streaming parado")
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao parar stream: ${e.message}")
            Log.e(tag, "Erro ao parar stream", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "dji_rtmp_stop"
            )
        }
    }

    override fun release() {
        stop()
    }

    fun updateUrl(url: String) {
        rtmpUrl = url
    }

    private fun getLiveStreamManager(): Any? {
        val sdkManager = DJISDKManager.getInstance()
        val method = sdkManager.javaClass.methods.firstOrNull { it.name == "getLiveStreamManager" }
        return method?.invoke(sdkManager)
    }

    private fun setLiveUrl(manager: Any, url: String): Boolean {
        val methods = manager.javaClass.methods
        val candidate = methods.firstOrNull {
            it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == String::class.java &&
                it.name.contains("url", ignoreCase = true)
        }
        return if (candidate != null) {
            candidate.invoke(manager, url)
            true
        } else {
            false
        }
    }

    private fun setAudioMuted(manager: Any, muted: Boolean) {
        val methods = manager.javaClass.methods
        val candidate = methods.firstOrNull {
            it.parameterTypes.size == 1 &&
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType &&
                it.name.contains("audio", ignoreCase = true) &&
                it.name.contains("mute", ignoreCase = true)
        }
        if (candidate != null) {
            candidate.invoke(manager, muted)
        }
    }

    private fun startStream(manager: Any): Boolean {
        val methods = manager.javaClass.methods
        val candidate = methods.firstOrNull { it.name.contains("start", true) && it.name.contains("stream", true) && it.parameterTypes.isEmpty() }
        return if (candidate != null) {
            val result = candidate.invoke(manager)
            result as? Boolean ?: true
        } else {
            false
        }
    }

    private fun stopStream(manager: Any) {
        val methods = manager.javaClass.methods
        val candidate = methods.firstOrNull { it.name.contains("stop", true) && it.name.contains("stream", true) && it.parameterTypes.isEmpty() }
        candidate?.invoke(manager)
    }
}
