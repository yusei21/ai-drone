package com.sloth.registerapp.features.mission.data.location

import android.location.Location
import com.mapbox.geojson.Point
import kotlin.math.max
import kotlin.math.min

data class OperatorLocationEstimate(
    val point: Point,
    val accuracyMeters: Float,
    val isStationary: Boolean
)

class OperatorPositionStabilizer(
    private val config: Config = Config()
) {
    data class Config(
        val maxAcceptedAccuracyMeters: Float = 35f,
        val goodAccuracyMeters: Float = 12f,
        val minMovementMeters: Float = 3.5f,
        val stationaryMinMovementMeters: Float = 6f,
        val stationaryEnterAfterMs: Long = 6_000L,
        val stationaryClusterRadiusMeters: Float = 8f,
        val stationarySpeedThresholdMps: Float = 0.8f,
        val stationaryExitDistanceMeters: Float = 12f,
        val stationaryExitSpeedMps: Float = 1.8f,
        val outlierDistanceMeters: Float = 28f,
        val outlierAccuracyMultiplier: Float = 1.35f,
        val movingAlpha: Float = 0.34f,
        val stationaryAlpha: Float = 0.18f,
        val highConfidenceAlphaBoost: Float = 0.10f,
        val largeMoveAlphaBoost: Float = 0.22f
    )

    private var filteredLocation: Location? = null
    private var lastRawLocation: Location? = null
    private var lastAcceptedRawLocation: Location? = null
    private var stationaryCandidateSinceMs: Long? = null
    private var isStationary = false

    fun update(rawLocation: Location): OperatorLocationEstimate? {
        if (!rawLocation.hasAccuracy() || rawLocation.accuracy <= 0f) return currentEstimate()
        if (rawLocation.accuracy > config.maxAcceptedAccuracyMeters) {
            lastRawLocation = Location(rawLocation)
            return currentEstimate()
        }

        val sample = Location(rawLocation)
        val currentFiltered = filteredLocation
        if (currentFiltered == null) {
            filteredLocation = sample
            lastRawLocation = Location(sample)
            lastAcceptedRawLocation = Location(sample)
            stationaryCandidateSinceMs = sample.time
            isStationary = false
            return sample.toEstimate()
        }

        val distanceFromFiltered = currentFiltered.distanceTo(sample)
        val speedMps = resolveSpeedMetersPerSecond(sample)
        updateStationaryState(
            sample = sample,
            distanceFromFilteredMeters = distanceFromFiltered,
            speedMetersPerSecond = speedMps
        )

        if (shouldRejectAsOutlier(sample, distanceFromFiltered, speedMps)) {
            lastRawLocation = Location(sample)
            return currentEstimate()
        }

        val effectiveMovementThreshold = max(
            if (isStationary) config.stationaryMinMovementMeters else config.minMovementMeters,
            sample.accuracy * if (isStationary) 0.75f else 0.45f
        )

        if (distanceFromFiltered < effectiveMovementThreshold && speedMps < config.stationaryExitSpeedMps) {
            lastRawLocation = Location(sample)
            return currentEstimate()
        }

        val alpha = computeAlpha(
            accuracyMeters = sample.accuracy,
            distanceFromFilteredMeters = distanceFromFiltered,
            speedMetersPerSecond = speedMps
        )
        val blended = blend(currentFiltered, sample, alpha)
        filteredLocation = blended
        lastRawLocation = Location(sample)
        lastAcceptedRawLocation = Location(sample)
        return blended.toEstimate()
    }

    fun reset() {
        filteredLocation = null
        lastRawLocation = null
        lastAcceptedRawLocation = null
        stationaryCandidateSinceMs = null
        isStationary = false
    }

    private fun currentEstimate(): OperatorLocationEstimate? = filteredLocation?.toEstimate()

    private fun resolveSpeedMetersPerSecond(sample: Location): Float {
        if (sample.hasSpeed() && sample.speed >= 0f) return sample.speed
        val previous = lastRawLocation ?: return 0f
        val elapsedMs = (sample.time - previous.time).coerceAtLeast(1L)
        return previous.distanceTo(sample) / (elapsedMs / 1000f)
    }

    private fun updateStationaryState(
        sample: Location,
        distanceFromFilteredMeters: Float,
        speedMetersPerSecond: Float
    ) {
        val mostlyStill =
            speedMetersPerSecond <= config.stationarySpeedThresholdMps &&
                distanceFromFilteredMeters <= config.stationaryClusterRadiusMeters

        if (mostlyStill) {
            if (stationaryCandidateSinceMs == null) {
                stationaryCandidateSinceMs = sample.time
            }
            if (sample.time - (stationaryCandidateSinceMs ?: sample.time) >= config.stationaryEnterAfterMs) {
                isStationary = true
            }
            return
        }

        val definiteMovement =
            speedMetersPerSecond >= config.stationaryExitSpeedMps ||
                distanceFromFilteredMeters >= config.stationaryExitDistanceMeters

        if (definiteMovement) {
            stationaryCandidateSinceMs = null
            isStationary = false
        }
    }

    private fun shouldRejectAsOutlier(
        sample: Location,
        distanceFromFilteredMeters: Float,
        speedMetersPerSecond: Float
    ): Boolean {
        if (!isStationary) return false
        if (distanceFromFilteredMeters < config.outlierDistanceMeters) return false

        val distanceFromAccepted = lastAcceptedRawLocation?.distanceTo(sample) ?: distanceFromFilteredMeters
        val tolerance = max(
            sample.accuracy * config.outlierAccuracyMultiplier,
            config.outlierDistanceMeters
        )

        return distanceFromAccepted > tolerance &&
            speedMetersPerSecond < config.stationaryExitSpeedMps &&
            sample.accuracy > config.goodAccuracyMeters
    }

    private fun computeAlpha(
        accuracyMeters: Float,
        distanceFromFilteredMeters: Float,
        speedMetersPerSecond: Float
    ): Float {
        var alpha = if (isStationary) config.stationaryAlpha else config.movingAlpha
        if (accuracyMeters <= config.goodAccuracyMeters) {
            alpha += config.highConfidenceAlphaBoost
        }
        if (distanceFromFilteredMeters >= config.stationaryExitDistanceMeters || speedMetersPerSecond >= config.stationaryExitSpeedMps) {
            alpha += config.largeMoveAlphaBoost
        }
        return min(alpha, 0.72f)
    }

    private fun blend(current: Location, target: Location, alpha: Float): Location {
        val blended = Location(target)
        blended.latitude = current.latitude + (target.latitude - current.latitude) * alpha
        blended.longitude = current.longitude + (target.longitude - current.longitude) * alpha
        blended.accuracy = current.accuracy + (target.accuracy - current.accuracy) * alpha
        return blended
    }

    private fun Location.toEstimate(): OperatorLocationEstimate {
        return OperatorLocationEstimate(
            point = Point.fromLngLat(longitude, latitude),
            accuracyMeters = accuracy,
            isStationary = isStationary
        )
    }
}
