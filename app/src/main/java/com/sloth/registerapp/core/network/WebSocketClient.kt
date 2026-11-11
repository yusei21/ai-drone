package com.sloth.registerapp.core.network

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val url: String,
    private val listener: WebSocketListener
) {

    private var webSocket: WebSocket? = null

    fun connect() {
        if (webSocket != null) return

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "Canceled by user")
        webSocket = null
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }
}
