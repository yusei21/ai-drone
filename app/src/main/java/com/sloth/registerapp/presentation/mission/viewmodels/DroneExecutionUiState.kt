package com.sloth.registerapp.presentation.mission.viewmodels

sealed class DroneExecutionUiState {
    object Idle : DroneExecutionUiState()
    object Preparing : DroneExecutionUiState()
    object Downloading : DroneExecutionUiState()
    object DownloadFinished : DroneExecutionUiState()
    object Uploading : DroneExecutionUiState()
    object ReadyToExecute : DroneExecutionUiState()
    object Executing : DroneExecutionUiState()
    object Paused : DroneExecutionUiState()
    object Stopped : DroneExecutionUiState()
    object Finished : DroneExecutionUiState()
    object Error : DroneExecutionUiState()

    fun isLoading(): Boolean = this is Preparing || this is Uploading || this is Downloading

    fun isExecuting(): Boolean = this is Executing || this is Paused

    fun isReadyToStart(): Boolean = this is ReadyToExecute
}
