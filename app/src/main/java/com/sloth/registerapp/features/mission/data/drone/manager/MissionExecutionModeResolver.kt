package com.sloth.registerapp.features.mission.data.drone.manager

import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.MissionExecutionMode
import dji.sdk.products.Aircraft

internal object MissionExecutionModeResolver {

    fun resolve(connectionHelper: DJIConnectionHelper): MissionExecutionMode {
        val product = connectionHelper.getProductInstance() as? Aircraft ?: return MissionExecutionMode.UNKNOWN
        val flightController = product.flightController ?: return MissionExecutionMode.UNKNOWN

        // Fallback robusto por reflexão para evitar acoplamento com variantes do SDK.
        val simulator = invokeMethod(flightController, "getSimulator") ?: return MissionExecutionMode.REAL
        val isActive = invokeBoolean(simulator, "isSimulatorActive")
            ?: invokeBoolean(simulator, "getIsSimulatorActive")
            ?: invokeBoolean(simulator, "isSimulationActive")

        return when (isActive) {
            true -> MissionExecutionMode.SIMULATED
            false -> MissionExecutionMode.REAL
            null -> MissionExecutionMode.REAL
        }
    }

    private fun invokeMethod(target: Any, methodName: String): Any? {
        return try {
            target.javaClass.getMethod(methodName).invoke(target)
        } catch (_: Exception) {
            null
        }
    }

    private fun invokeBoolean(target: Any, methodName: String): Boolean? {
        val result = invokeMethod(target, methodName) ?: return null
        return result as? Boolean
    }
}

