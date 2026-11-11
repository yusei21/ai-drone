package com.sloth.registerapp.features.mission.domain.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface OperatorLocationRepository {
    fun locationUpdates(): Flow<Location>
    suspend fun getLastKnownLocation(): Location?
}
