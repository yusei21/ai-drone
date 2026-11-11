package com.sloth.registerapp.core.network

import com.sloth.registerapp.BuildConfig

object AuthRoutePolicy {
    private val authRoutes = setOf(
        "auth/login",
        "auth/register",
        "auth/refresh"
    )

    private val publicFlightRoutes = setOf(
        "mission/status",
        "mission/start",
        "mission/stop"
    )

    fun isAuthRoute(encodedPath: String): Boolean {
        val normalized = normalizePath(encodedPath)
        return authRoutes.any { normalized.startsWith(it) }
    }

    fun requiresAccessToken(encodedPath: String): Boolean {
        val normalized = normalizePath(encodedPath)
        if (authRoutes.any { normalized.startsWith(it) }) return false
        if (BuildConfig.ALLOW_PUBLIC_FLIGHT_OPS && publicFlightRoutes.any { normalized.startsWith(it) }) return false
        return true
    }

    private fun normalizePath(path: String): String {
        return path.removePrefix("/").removePrefix("api/")
    }
}
