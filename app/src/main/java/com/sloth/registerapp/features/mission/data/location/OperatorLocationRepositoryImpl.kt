package com.sloth.registerapp.features.mission.data.location

import android.location.Location
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.mission.domain.repository.OperatorLocationRepository
import io.sentry.SentryLevel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OperatorLocationRepositoryImpl(
    context: Context
) : OperatorLocationRepository {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(Location(location))
            }
        }

        try {
            AppObservability.breadcrumb(
                category = "location",
                message = "Operator location updates requested",
                data = mapOf("priority" to "high_accuracy")
            )
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (security: SecurityException) {
            AppObservability.captureException(
                throwable = security,
                area = "location",
                operation = "request_location_updates",
                level = SentryLevel.WARNING,
                tags = mapOf("permission" to "missing")
            )
            close(security)
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }.distinctUntilChanged()

    override suspend fun getLastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        AppObservability.breadcrumb(
                            category = "location",
                            message = "Last known location unavailable",
                            level = SentryLevel.WARNING
                        )
                    }
                    if (location == null) {
                        continuation.resume(null)
                    } else {
                        continuation.resume(Location(location))
                    }
                }
                .addOnFailureListener { error ->
                    AppObservability.captureException(
                        throwable = error,
                        area = "location",
                        operation = "get_last_known_location",
                        level = SentryLevel.WARNING
                    )
                    continuation.resumeWithException(error)
                }
        }
}
