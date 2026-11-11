package com.sloth.registerapp.features.streaming.data

import android.util.Log
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.streaming.domain.StreamState
import com.sloth.registerapp.features.streaming.domain.StreamingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PhoneRtmpStreamer(
    private val openGlView: OpenGlView,
    private var rtmpUrl: String,
    private val config: VideoConfig = VideoConfig()
) : StreamingController {

    data class VideoConfig(
        val enableAudio: Boolean = false
    )

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state: StateFlow<StreamState> = _state

    private val tag = "PhoneRtmpStreamer"

    private val connectChecker = object : ConnectChecker {
        override fun onConnectionStarted(url: String) {
            _state.value = StreamState.Connecting
            Log.d(tag, "RTMP conectando")
            AppObservability.breadcrumb(
                category = "streaming",
                message = "Phone RTMP connection started",
                data = mapOf("source" to "phone_camera")
            )
        }

        override fun onConnectionSuccess() {
            _state.value = StreamState.Streaming
            Log.d(tag, "RTMP conectado")
        }

        override fun onConnectionFailed(reason: String) {
            Log.e(tag, "Falha RTMP: $reason")
            AppObservability.captureMessage(
                message = "Phone RTMP connection failed",
                area = "streaming",
                operation = "phone_rtmp_connection",
                extras = mapOf("reason" to reason)
            )
            _state.value = StreamState.Error("Falha RTMP: $reason")
            if (::rtmpCamera.isInitialized && rtmpCamera.isStreaming) {
                rtmpCamera.stopStream()
            }
        }

        override fun onDisconnect() {
            _state.value = StreamState.Idle
            Log.d(tag, "RTMP desconectado")
        }

        override fun onAuthError() {
            _state.value = StreamState.Error("Erro de autenticação RTMP")
            Log.e(tag, "Erro de autenticação RTMP")
            AppObservability.captureMessage(
                message = "Phone RTMP authentication failed",
                area = "streaming",
                operation = "phone_rtmp_auth"
            )
        }

        override fun onAuthSuccess() {
            Log.d(tag, "Autenticação RTMP OK")
        }
    }

    private lateinit var rtmpCamera: RtmpCamera2

    init {
        rtmpCamera = RtmpCamera2(openGlView, connectChecker)
    }

    override fun start() {
        if (_state.value is StreamState.Streaming || _state.value is StreamState.Connecting) return
        _state.value = StreamState.Connecting

        try {
            val videoPrepared = rtmpCamera.prepareVideo()
            val audioPrepared = if (config.enableAudio) {
                rtmpCamera.enableAudio()
                rtmpCamera.prepareAudio()
            } else {
                rtmpCamera.disableAudio()
                true
            }

            if (!videoPrepared || !audioPrepared) {
                _state.value = StreamState.Error("Falha ao preparar stream")
                Log.e(tag, "Falha ao preparar stream")
                AppObservability.captureMessage(
                    message = "Phone RTMP stream preparation failed",
                    area = "streaming",
                    operation = "phone_rtmp_prepare",
                    extras = mapOf(
                        "video_prepared" to videoPrepared,
                        "audio_prepared" to audioPrepared,
                        "audio_enabled" to config.enableAudio
                    )
                )
                return
            }

            rtmpCamera.startStream(rtmpUrl)
            Log.d(tag, "RTMP startStream chamado")
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao iniciar stream: ${e.message}")
            Log.e(tag, "Erro ao iniciar stream", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "phone_rtmp_start",
                extras = mapOf("audio_enabled" to config.enableAudio)
            )
        }
    }

    override fun stop() {
        try {
            if (rtmpCamera.isStreaming) {
                rtmpCamera.stopStream()
            }
            _state.value = StreamState.Idle
            Log.d(tag, "RTMP streaming parado")
        } catch (e: Exception) {
            _state.value = StreamState.Error("Erro ao parar stream: ${e.message}")
            Log.e(tag, "Erro ao parar stream", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "phone_rtmp_stop"
            )
        }
    }

    override fun release() {
        stop()
        stopPreview()
        rtmpCamera.stopCamera()
    }

    fun updateUrl(url: String) {
        rtmpUrl = url
    }

    fun startPreview() {
        try {
            if (!rtmpCamera.isOnPreview) {
                rtmpCamera.startPreview()
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao iniciar preview", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "phone_preview_start"
            )
        }
    }

    fun stopPreview() {
        try {
            if (rtmpCamera.isOnPreview) {
                rtmpCamera.stopPreview()
            }
        } catch (e: Exception) {
            Log.e(tag, "Erro ao parar preview", e)
            AppObservability.captureException(
                throwable = e,
                area = "streaming",
                operation = "phone_preview_stop"
            )
        }
    }
}
