package com.sloth.registerapp.presentation.mission.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.mission.data.location.OperatorLocationEstimate
import com.sloth.registerapp.features.mission.data.location.OperatorLocationRepositoryImpl
import com.sloth.registerapp.features.mission.data.location.OperatorPositionStabilizer
import com.sloth.registerapp.features.mission.domain.repository.OperatorLocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.sentry.SentryLevel

data class OperatorLocationUiState(
    val location: Point? = null,
    val accuracyMeters: Float? = null,
    val isStationary: Boolean = false,
    val errorMessage: String? = null,
    val hasPermission: Boolean = false
)

class OperatorLocationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: OperatorLocationRepository = OperatorLocationRepositoryImpl(application)
    private val positionStabilizer = OperatorPositionStabilizer()

    private val _uiState = MutableStateFlow(OperatorLocationUiState())
    val uiState: StateFlow<OperatorLocationUiState> = _uiState.asStateFlow()

    private var updatesJob: Job? = null

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        AppObservability.breadcrumb(
            category = "permissions",
            message = if (granted) "Location permission granted" else "Location permission denied",
            level = if (granted) SentryLevel.INFO else SentryLevel.WARNING,
            data = mapOf("permission" to "location")
        )

        if (!granted) {
            updatesJob?.cancel()
            updatesJob = null
            positionStabilizer.reset()
            _uiState.update {
                it.copy(
                    location = null,
                    accuracyMeters = null,
                    isStationary = false,
                    errorMessage = "Permissão de GPS não concedida"
                )
            }
            return
        }

        if (updatesJob == null) {
            startUpdates()
        }
    }

    private fun startUpdates() {
        updatesJob?.cancel()
        updatesJob = viewModelScope.launch {
            try {
                val last = repository.getLastKnownLocation()
                last?.let(positionStabilizer::update)?.let(::publishEstimate)
            } catch (e: Exception) {
                AppObservability.captureException(
                    throwable = e,
                    area = "location",
                    operation = "load_last_known_location",
                    level = SentryLevel.WARNING
                )
                _uiState.update { it.copy(errorMessage = "Falha ao obter GPS") }
            }

            repository.locationUpdates()
                .catch { error ->
                    AppObservability.captureException(
                        throwable = error,
                        area = "location",
                        operation = "observe_location_updates",
                        level = SentryLevel.WARNING
                    )
                    _uiState.update { it.copy(errorMessage = "Falha ao obter GPS") }
                }
                .collect { location ->
                    positionStabilizer.update(location)?.let(::publishEstimate)
                }
        }
    }

    private fun publishEstimate(estimate: OperatorLocationEstimate) {
        _uiState.update {
            it.copy(
                location = estimate.point,
                accuracyMeters = estimate.accuracyMeters,
                isStationary = estimate.isStationary,
                errorMessage = null
            )
        }
    }
}
