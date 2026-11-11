package com.sloth.registerapp.presentation.mission.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.core.mission.ActiveMissionSessionManager
import com.sloth.registerapp.core.mission.ActiveMissionStatus
import com.sloth.registerapp.features.mission.data.drone.manager.DroneMissionManager
import com.sloth.registerapp.features.mission.data.drone.manager.MissionState
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import com.sloth.registerapp.features.mission.domain.model.MissionOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar a execução de missões no drone
 */
class DroneExecutionViewModel(
    private val missionManager: DroneMissionManager
) : ViewModel() {

    companion object {
        private const val TAG = "DroneExecutionViewModel"
    }

    // Estado da missão (MissionState da DroneMissionManager)
    private val _missionState = MutableStateFlow<MissionState>(MissionState.IDLE)
    val missionState = _missionState.asStateFlow()
    val missionOutcome = missionManager.missionOutcome

    // Dados da missão
    private val _mission = MutableStateFlow<ServerMissionDto?>(null)
    val mission = _mission.asStateFlow()

    // Mensagem de erro
    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage = _errorMessage.asStateFlow()

    // Estados da UI
    private val _uiState = MutableStateFlow<MissionUiState>(MissionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Eventos para toast/snackbar
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeMissionState()
    }

    /**
     * Observa mudanças no estado da missão
     */
    private fun observeMissionState() {
        viewModelScope.launch {
            missionManager.missionState.collect { state ->
                _missionState.value = state
                val missionLoaded = _mission.value != null
                val newUiState = when (state) {
                    MissionState.IDLE -> MissionUiState.Idle
                    MissionState.PREPARING -> MissionUiState.Preparing
                    MissionState.DOWNLOADING -> MissionUiState.Downloading
                    MissionState.DOWNLOAD_FINISHED -> MissionUiState.DownloadFinished
                    MissionState.UPLOADING -> MissionUiState.Uploading
                    MissionState.READY_TO_EXECUTE -> MissionUiState.ReadyToExecute
                    MissionState.EXECUTING -> MissionUiState.Executing
                    MissionState.EXECUTION_PAUSED -> MissionUiState.Paused
                    MissionState.EXECUTION_STOPPED -> MissionUiState.Stopped
                    MissionState.FINISHED -> MissionUiState.Finished
                    MissionState.ERROR -> MissionUiState.Error("")
                }
                _uiState.value = newUiState
                if (missionLoaded) {
                    ActiveMissionSessionManager.updateMissionStatus(state.toActiveSessionStatus())
                }
            }
        }
    }

    fun loadMission(mission: ServerMissionDto) {
        Log.i(TAG, "Carregando missão: ${mission.name}")
        _mission.value = mission
        viewModelScope.launch {
            try {
                ActiveMissionSessionManager.startMissionSession(
                    missionId = mission.id.toString(),
                    missionName = mission.name
                )
                missionManager.prepareAndUploadMission(mission)
                Log.i(TAG, "✅ Missão carregada!")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao carregar: ${e.message}")
                ActiveMissionSessionManager.updateMissionStatus(ActiveMissionStatus.ERROR)
                _errorMessage.value = "Erro: ${e.message}"
                _uiEvent.emit(UiEvent.ShowError("Erro: ${e.message}"))
            }
        }
    }

    fun startMission() {
        viewModelScope.launch {
            try {
                missionManager.startMission()
                Log.i(TAG, "✅ Missão iniciada!")
                _uiEvent.emit(UiEvent.ShowMessage("Missão iniciada"))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao iniciar: ${e.message}")
                _errorMessage.value = "Erro: ${e.message}"
                _uiEvent.emit(UiEvent.ShowError("Erro: ${e.message}"))
            }
        }
    }

    fun pauseMission() {
        viewModelScope.launch {
            try {
                missionManager.pauseMission()
                _uiEvent.emit(UiEvent.ShowMessage("Missão pausada"))
            } catch (e: Exception) {
                _errorMessage.value = "Erro: ${e.message}"
                _uiEvent.emit(UiEvent.ShowError("Erro: ${e.message}"))
            }
        }
    }

    fun resumeMission() {
        viewModelScope.launch {
            try {
                missionManager.resumeMission()
                _uiEvent.emit(UiEvent.ShowMessage("Missão retomada"))
            } catch (e: Exception) {
                _errorMessage.value = "Erro: ${e.message}"
                _uiEvent.emit(UiEvent.ShowError("Erro: ${e.message}"))
            }
        }
    }

    fun stopMission() {
        viewModelScope.launch {
            try {
                missionManager.stopMission()
                _uiEvent.emit(UiEvent.ShowMessage("Missão parada"))
            } catch (e: Exception) {
                _errorMessage.value = "Erro: ${e.message}"
                _uiEvent.emit(UiEvent.ShowError("Erro: ${e.message}"))
            }
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                missionManager.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao destruir manager", e)
            }
        }
    }
}

private fun MissionState.toActiveSessionStatus(): ActiveMissionStatus {
    return when (this) {
        MissionState.IDLE -> ActiveMissionStatus.IDLE
        MissionState.PREPARING,
        MissionState.DOWNLOADING,
        MissionState.UPLOADING -> ActiveMissionStatus.LOADING
        MissionState.DOWNLOAD_FINISHED,
        MissionState.READY_TO_EXECUTE -> ActiveMissionStatus.READY
        MissionState.EXECUTING -> ActiveMissionStatus.EXECUTING
        MissionState.EXECUTION_PAUSED -> ActiveMissionStatus.PAUSED
        MissionState.EXECUTION_STOPPED -> ActiveMissionStatus.STOPPED
        MissionState.FINISHED -> ActiveMissionStatus.COMPLETED
        MissionState.ERROR -> ActiveMissionStatus.ERROR
    }
}

/**
 * Factory para DroneExecutionViewModel
 */
class DroneExecutionViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DroneExecutionViewModel::class.java)) {
            val droneMissionManager = DroneMissionManager(com.sloth.registerapp.core.dji.DJIConnectionHelper)
            @Suppress("UNCHECKED_CAST")
            return DroneExecutionViewModel(droneMissionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Eventos que disparam ações na UI (Toasts, Snackbars, etc)
 */
sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
}
