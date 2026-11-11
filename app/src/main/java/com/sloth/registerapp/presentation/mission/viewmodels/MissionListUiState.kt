package com.sloth.registerapp.presentation.mission.viewmodels

import com.sloth.registerapp.features.mission.domain.model.Mission

sealed class MissionListUiState {
    object Idle : MissionListUiState()
    object Loading : MissionListUiState()
    data class Success(val missions: List<Mission>) : MissionListUiState()
    data class Error(val message: String) : MissionListUiState()
    object Unauthorized : MissionListUiState()
}
