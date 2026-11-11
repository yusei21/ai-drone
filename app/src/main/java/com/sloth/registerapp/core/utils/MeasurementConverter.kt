package com.sloth.registerapp.core.utils

import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import java.util.Locale

object MeasurementConverter {

    fun formatAltitude(valueMeters: Float, system: String): String {
        return if (system == MeasurementSettingsRepository.SYSTEM_IMPERIAL) {
            val feet = valueMeters * 3.28084f
            String.format(Locale.US, "%.1fft", feet)
        } else {
            String.format(Locale.US, "%.1fm", valueMeters)
        }
    }

    fun formatDistance(valueMeters: Float, system: String): String {
        return if (system == MeasurementSettingsRepository.SYSTEM_IMPERIAL) {
            val feet = valueMeters * 3.28084f
            String.format(Locale.US, "%.0fft", feet)
        } else {
            String.format(Locale.US, "%.0fm", valueMeters)
        }
    }

    fun formatSpeed(valueMetersPerSecond: Float, system: String): String {
        return if (system == MeasurementSettingsRepository.SYSTEM_IMPERIAL) {
            val mph = valueMetersPerSecond * 2.23694f
            String.format(Locale.US, "%.1fmph", mph)
        } else {
            val kmh = valueMetersPerSecond * 3.6f
            String.format(Locale.US, "%.1fkm/h", kmh)
        }
    }
}
