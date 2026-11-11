package com.sloth.registerapp.features.mission.data.remote.websocket

import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.google.gson.Gson
import io.sentry.SentryLevel
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "MissionWebSocket"

class MissionWebSocketListener(private val onMissionReceived: (ServerMissionDto) -> Unit) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "WebSocket connection opened")
        AppObservability.breadcrumb(
            category = "mission.websocket",
            message = "Mission WebSocket opened",
            data = mapOf("status_code" to response.code)
        )
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "Receiving: $text")
        try {
            val mission = Gson().fromJson(text, ServerMissionDto::class.java)
            AppObservability.breadcrumb(
                category = "mission.websocket",
                message = "Mission update received",
                data = mapOf(
                    "mission_id" to mission.id,
                    "waypoints_count" to mission.waypoints.size
                )
            )
            onMissionReceived(mission)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing mission from WebSocket message", e)
            AppObservability.captureException(
                throwable = e,
                area = "mission",
                operation = "parse_websocket_mission",
                level = SentryLevel.WARNING,
                extras = mapOf("payload_size" to text.length)
            )
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        Log.d(TAG, "Closing: $code / $reason")
        AppObservability.breadcrumb(
            category = "mission.websocket",
            message = "Mission WebSocket closing",
            data = mapOf("code" to code)
        )
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Error: " + t.message, t)
        AppObservability.captureException(
            throwable = t,
            area = "mission",
            operation = "websocket_failure",
            level = SentryLevel.WARNING,
            extras = mapOf("status_code" to response?.code)
        )
    }
}
