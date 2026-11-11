package com.sloth.registerapp.features.mission.data.drone.manager

import android.util.Log
import com.sloth.registerapp.features.mission.domain.model.FlightCommandError
import com.sloth.registerapp.features.mission.domain.model.FlightCommandResult
import com.sloth.registerapp.features.mission.domain.model.FlightCommandType
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.flightcontroller.FlightController
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class FlightCommandExecutor(
    private val flightControllerProvider: () -> FlightController?,
    private val logTag: String,
    private val timeoutMs: Long
) {
    suspend fun execute(
        command: FlightCommandType,
        action: FlightController.(CommonCallbacks.CompletionCallback<DJIError>) -> Unit
    ): FlightCommandResult {
        val flightController = flightControllerProvider()
            ?: return FlightCommandResult.Failed(
                command = command,
                error = FlightCommandError.Sdk("FlightController não disponível")
            )

        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    flightController.action(object : CommonCallbacks.CompletionCallback<DJIError> {
                        override fun onResult(error: DJIError?) {
                            if (!continuation.isActive) return

                            val result = if (error == null) {
                                Log.d(logTag, "✅ Comando $command aceito pelo SDK")
                                FlightCommandResult.Accepted(command)
                            } else {
                                Log.e(logTag, "❌ Erro em $command: ${error.description}")
                                FlightCommandResult.Failed(
                                    command = command,
                                    error = FlightCommandError.Sdk(error.description)
                                )
                            }
                            continuation.resume(result)
                        }
                    })
                }
            }
        } catch (_: TimeoutCancellationException) {
            Log.e(logTag, "❌ Timeout aguardando callback para $command")
            FlightCommandResult.Failed(
                command = command,
                error = FlightCommandError.Timeout
            )
        }
    }
}
