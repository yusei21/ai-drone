package com.sloth.registerapp.core.dji

/**
 * Exception customizada para erros do SDK DJI
 */
class DJIException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String? = null
) : Exception(message, cause)
