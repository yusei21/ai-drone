package com.sloth.registerapp.core.network.dto

data class ApiStatusDto(
    val message: String? = null,
    val status: String? = null,
    val running: Boolean? = null
)
