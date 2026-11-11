package com.sloth.registerapp.core.auth.model

sealed class LocalSessionState {
    data object LOCAL_LOGGED_OUT : LocalSessionState()
    data class LOCAL_LOGGED_IN(val userId: String) : LocalSessionState()
}

enum class NetworkState {
    ONLINE,
    OFFLINE
}

enum class ServerAuthState {
    SERVER_AUTH_OK,
    SERVER_ACCESS_EXPIRED,
    SERVER_AUTH_REQUIRED
}
