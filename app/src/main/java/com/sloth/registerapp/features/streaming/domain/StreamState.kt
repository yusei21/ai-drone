package com.sloth.registerapp.features.streaming.domain

sealed class StreamState {
    data object Idle : StreamState()
    data object Connecting : StreamState()
    data object Streaming : StreamState()
    data class Error(val message: String) : StreamState()
}
