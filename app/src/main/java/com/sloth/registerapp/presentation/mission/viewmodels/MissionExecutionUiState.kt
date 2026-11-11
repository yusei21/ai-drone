package com.sloth.registerapp.presentation.mission.viewmodels

/**
 * Estados da UI para execução de missões no drone
 * 
 * Diferente de MissionListUiState que gerencia a listagem
 */
sealed class MissionUiState {
    object Idle : MissionUiState()
    object Preparing : MissionUiState()
    object Downloading : MissionUiState()
    object DownloadFinished : MissionUiState()
    object Uploading : MissionUiState()
    object ReadyToExecute : MissionUiState()
    object Executing : MissionUiState()
    object Paused : MissionUiState()
    object Stopped : MissionUiState()
    object Finished : MissionUiState()
    data class Error(val message: String = "") : MissionUiState()
}
