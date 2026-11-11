package com.sloth.registerapp.presentation.mission.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.mission.domain.model.Mission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val TAG = "MissionListViewModel"

class MissionListViewModel(application: Application) : AndroidViewModel(application) {

    private val missionRepository = MissionRepositoryImpl(
        application,
        RetrofitClient.getInstance(application),
        com.sloth.registerapp.core.auth.TokenRepository.getInstance(application)
    )

    private val _uiState = MutableStateFlow<MissionListUiState>(MissionListUiState.Idle)
    val uiState: StateFlow<MissionListUiState> = _uiState

    fun fetchMissions() {
        viewModelScope.launch {
            _uiState.value = MissionListUiState.Loading
            val result = missionRepository.getMissions()
            result.onSuccess { missions ->
                _uiState.value = MissionListUiState.Success(missions)
                Log.d(TAG, "Missions fetched successfully: ${missions.size} missions")
            }.onFailure { error ->
                if (error is HttpException && error.code() == 401) {
                    _uiState.value = MissionListUiState.Unauthorized
                    Log.w(TAG, "Unauthorized after retry/refresh flow")
                } else {
                    _uiState.value = MissionListUiState.Error(error.message ?: "Unknown error")
                    Log.e(TAG, "Error fetching missions", error)
                }
            }
        }
    }

    fun deleteMission(missionId: Int) {
        viewModelScope.launch {
            val result = missionRepository.deleteMission(missionId)
            result.onSuccess {
                val currentState = _uiState.value
                if (currentState is MissionListUiState.Success) {
                    _uiState.value = currentState.copy(
                        missions = currentState.missions.filterNot { it.id == missionId }
                    )
                } else {
                    fetchMissions()
                }
            }.onFailure { error ->
                _uiState.value = MissionListUiState.Error(error.message ?: "Unknown error")
                Log.e(TAG, "Error deleting mission", error)
            }
        }
    }
}
