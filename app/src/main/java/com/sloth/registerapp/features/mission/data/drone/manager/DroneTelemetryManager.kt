package com.sloth.registerapp.features.mission.data.drone.manager

import android.util.Log
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.features.mission.domain.model.DroneState
import com.sloth.registerapp.features.mission.domain.model.DroneTelemetry
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.battery.Battery
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DroneTelemetryManager(
    private val scope: CoroutineScope
) {
    private val _telemetry = MutableStateFlow(DroneTelemetry())
    val telemetry: StateFlow<DroneTelemetry> = _telemetry.asStateFlow()

    private val _droneState = MutableStateFlow(DroneState.DISCONNECTED)
    val droneState: StateFlow<DroneState> = _droneState.asStateFlow()

    private var activeFlightController: FlightController? = null
    private var activeBattery: Battery? = null
    private var telemetryJob: Job? = null

    init {
        observeTelemetry()
    }

    private fun observeTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            DJIConnectionHelper.product.collect { product ->
                val flightController = (product as? Aircraft)?.flightController
                val battery = (product as? Aircraft)?.battery
                if (flightController == activeFlightController && battery == activeBattery) return@collect

                try {
                    activeFlightController?.setStateCallback(null)
                } catch (_: Exception) {
                }
                try {
                    activeBattery?.setStateCallback(null)
                } catch (_: Exception) {
                }

                activeFlightController = flightController
                activeBattery = battery
                if (flightController == null) {
                    _droneState.value = DroneState.DISCONNECTED
                    return@collect
                }

                try {
                    flightController.setStateCallback { state ->
                        updateTelemetryFromState(state)
                        updateDroneStateFromState(state)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Não foi possível registrar callback de telemetria: ${e.message}")
                }

                try {
                    battery?.setStateCallback { state ->
                        _telemetry.value = _telemetry.value.copy(
                            batteryLevel = state.chargeRemainingInPercent
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Não foi possível registrar callback de bateria: ${e.message}")
                }
            }
        }
    }

    fun updateTelemetry(
        altitude: Float? = null,
        speed: Float? = null,
        distance: Float? = null,
        gps: Int? = null,
        battery: Int? = null
    ) {
        _telemetry.value = _telemetry.value.copy(
            altitude = altitude ?: _telemetry.value.altitude,
            speed = speed ?: _telemetry.value.speed,
            distanceFromHome = distance ?: _telemetry.value.distanceFromHome,
            gpsSatellites = gps ?: _telemetry.value.gpsSatellites,
            batteryLevel = battery ?: _telemetry.value.batteryLevel
        )
    }

    fun stop() {
        telemetryJob?.cancel()
        telemetryJob = null
        try {
            activeFlightController?.setStateCallback(null)
        } catch (_: Exception) {
        }
        try {
            activeBattery?.setStateCallback(null)
        } catch (_: Exception) {
        }
        activeFlightController = null
        activeBattery = null
        _droneState.value = DroneState.DISCONNECTED
    }

    private fun updateTelemetryFromState(state: FlightControllerState) {
        val altitude = extractAltitude(state)
        val speed = extractSpeed(state)
        val gps = extractGpsSatellites(state)
        val battery = extractBattery(state)
        val aircraftLocation = extractAircraftLocation(state)
        val homeLocation = extractHomeLocation(state)
        val distance = if (aircraftLocation != null && homeLocation != null) {
            haversineMeters(aircraftLocation.first, aircraftLocation.second, homeLocation.first, homeLocation.second)
        } else {
            null
        }
        val isFlying = extractIsFlying(state)

        _telemetry.value = _telemetry.value.copy(
            altitude = altitude ?: _telemetry.value.altitude,
            speed = speed ?: _telemetry.value.speed,
            distanceFromHome = distance ?: _telemetry.value.distanceFromHome,
            gpsSatellites = gps ?: _telemetry.value.gpsSatellites,
            batteryLevel = battery ?: _telemetry.value.batteryLevel,
            latitude = aircraftLocation?.first ?: _telemetry.value.latitude,
            longitude = aircraftLocation?.second ?: _telemetry.value.longitude,
            isFlying = isFlying ?: _telemetry.value.isFlying
        )
    }

    private fun updateDroneStateFromState(state: FlightControllerState) {
        val isFlying = extractIsFlying(state)
        val isLanding = getBoolean(state, "isLanding")
            ?: getBoolean(state, "getIsLanding")
        val isTakingOff = getBoolean(state, "isTakingOff")
            ?: getBoolean(state, "getIsTakingOff")
        val isGoingHome = getBoolean(state, "isGoingHome")
            ?: getBoolean(state, "getIsGoingHome")
        val motorsOn = getBoolean(state, "areMotorsOn")
            ?: getBoolean(state, "getAreMotorsOn")

        val derived = when {
            isLanding == true -> DroneState.LANDING
            isTakingOff == true -> DroneState.TAKING_OFF
            isGoingHome == true -> DroneState.GOING_HOME
            isFlying == true -> DroneState.IN_AIR
            motorsOn == true -> DroneState.ON_GROUND
            else -> DroneState.ON_GROUND
        }

        _droneState.value = derived
    }

    private fun extractAltitude(state: FlightControllerState): Float? {
        val direct = runCatching { state.aircraftLocation?.altitude }.getOrNull()
            ?: getFloat(state, "getAltitude")
            ?: getFloat(state, "getAltitudeInMeters")
        if (direct != null) return direct
        val aircraftLocation = state.aircraftLocation ?: getObject(state, "getAircraftLocation") ?: return null
        return getFloat(aircraftLocation, "getAltitude") ?: getFloat(aircraftLocation, "getHeight")
    }

    private fun extractSpeed(state: FlightControllerState): Float? {
        val vx = runCatching { state.velocityX }.getOrNull() ?: getFloat(state, "getVelocityX")
        val vy = runCatching { state.velocityY }.getOrNull() ?: getFloat(state, "getVelocityY")
        val vz = runCatching { state.velocityZ }.getOrNull() ?: getFloat(state, "getVelocityZ")
        return if (vx != null && vy != null && vz != null) {
            sqrt(vx * vx + vy * vy + vz * vz)
        } else {
            getFloat(state, "getVelocity") ?: getFloat(state, "getSpeed")
        }
    }

    private fun extractGpsSatellites(state: FlightControllerState): Int? {
        return runCatching { state.satelliteCount }.getOrNull()
            ?: getInt(state, "getSatelliteCount")
            ?: getInt(state, "getGPSSatelliteCount")
            ?: getInt(state, "getGpsSatelliteCount")
    }

    private fun extractBattery(state: FlightControllerState): Int? {
        return getInt(state, "getBatteryPercentage")
            ?: getInt(state, "getBatteryPercent")
    }

    private fun extractIsFlying(state: FlightControllerState): Boolean? {
        return runCatching { state.isFlying }.getOrNull()
            ?: getBoolean(state, "isFlying")
            ?: getBoolean(state, "getIsFlying")
    }

    private fun extractAircraftLocation(state: FlightControllerState): Pair<Double, Double>? {
        return extractLocation(state.aircraftLocation ?: getObject(state, "getAircraftLocation"))
    }

    private fun extractHomeLocation(state: FlightControllerState): Pair<Double, Double>? {
        return extractLocation(state.homeLocation ?: getObject(state, "getHomeLocation"))
    }

    private fun extractLocation(location: Any?): Pair<Double, Double>? {
        location ?: return null
        val lat = getDouble(location, "getLatitude") ?: return null
        val lon = getDouble(location, "getLongitude") ?: return null
        if (lat == 0.0 && lon == 0.0) return null
        return Pair(lat, lon)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    private fun getObject(target: Any?, methodName: String): Any? {
        if (target == null) return null
        return try {
            target.javaClass.getMethod(methodName).invoke(target)
        } catch (_: Exception) {
            null
        }
    }

    private fun getFloat(target: Any?, methodName: String): Float? {
        val value = getObject(target, methodName) ?: return null
        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Number -> value.toFloat()
            else -> null
        }
    }

    private fun getDouble(target: Any?, methodName: String): Double? {
        val value = getObject(target, methodName) ?: return null
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Number -> value.toDouble()
            else -> null
        }
    }

    private fun getInt(target: Any?, methodName: String): Int? {
        val value = getObject(target, methodName) ?: return null
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun getBoolean(target: Any?, methodName: String): Boolean? {
        val value = getObject(target, methodName) ?: return null
        return value as? Boolean
    }

    companion object {
        private const val TAG = "DroneTelemetry"
    }
}
