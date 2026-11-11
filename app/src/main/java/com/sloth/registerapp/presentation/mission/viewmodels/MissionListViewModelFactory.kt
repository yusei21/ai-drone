package com.sloth.registerapp.presentation.mission.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MissionListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MissionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MissionListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
