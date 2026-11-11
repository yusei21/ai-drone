package com.sloth.registerapp.features.streaming.domain

import kotlinx.coroutines.flow.StateFlow

interface StreamingController {
    val state: StateFlow<StreamState>
    fun start()
    fun stop()
    fun release()
}
