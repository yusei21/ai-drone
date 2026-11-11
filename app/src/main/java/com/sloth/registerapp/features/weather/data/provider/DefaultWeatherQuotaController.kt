package com.sloth.registerapp.features.weather.data.provider

import android.content.Context
import com.sloth.registerapp.core.settings.WeatherQuotaPolicy
import com.sloth.registerapp.core.settings.WeatherQuotaSettingsRepository
import com.sloth.registerapp.core.settings.WeatherQuotaUsageRepository
import com.sloth.registerapp.features.weather.domain.provider.WeatherQuotaController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultWeatherQuotaController(
    context: Context
) : WeatherQuotaController {
    private val settings = WeatherQuotaSettingsRepository.getInstance(context.applicationContext)
    private val usageRepository = WeatherQuotaUsageRepository.getInstance(context.applicationContext)
    private val usageMutex = Mutex()

    override suspend fun canCall(providerId: String): Boolean {
        return usageMutex.withLock {
            val now = System.currentTimeMillis()
            val policy = settings.policy.first()
            val usage = normalizedUsage(providerId, now)
            if (usage.blockedUntilMs > now) return@withLock false

            usage.minuteCount < policy.maxCallsPerMinute &&
                usage.hourCount < policy.maxCallsPerHour &&
                usage.dayCount < policy.maxCallsPerDay
        }
    }

    override suspend fun recordCall(providerId: String) {
        usageMutex.withLock {
            val now = System.currentTimeMillis()
            val usage = normalizedUsage(providerId, now)
            usageRepository.saveUsage(
                providerId,
                usage.copy(
                    minuteCount = usage.minuteCount + 1,
                    hourCount = usage.hourCount + 1,
                    dayCount = usage.dayCount + 1
                )
            )
        }
    }

    override suspend fun recordRateLimit(providerId: String) {
        usageMutex.withLock {
            val now = System.currentTimeMillis()
            val policy: WeatherQuotaPolicy = settings.policy.first()
            val usage = normalizedUsage(providerId, now)
            usageRepository.saveUsage(
                providerId,
                usage.copy(
                    blockedUntilMs = now + policy.cooldownMinutesOn429 * MINUTE_MS
                )
            )
        }
    }

    private suspend fun normalizedUsage(providerId: String, now: Long): com.sloth.registerapp.core.settings.WeatherProviderQuotaUsage {
        val usage = usageRepository.getUsage(providerId)
        val normalized = usage.copy(
            minuteWindowStartMs = if (now - usage.minuteWindowStartMs >= MINUTE_MS) now else usage.minuteWindowStartMs,
            minuteCount = if (now - usage.minuteWindowStartMs >= MINUTE_MS) 0 else usage.minuteCount,
            hourWindowStartMs = if (now - usage.hourWindowStartMs >= HOUR_MS) now else usage.hourWindowStartMs,
            hourCount = if (now - usage.hourWindowStartMs >= HOUR_MS) 0 else usage.hourCount,
            dayWindowStartMs = if (now - usage.dayWindowStartMs >= DAY_MS) now else usage.dayWindowStartMs,
            dayCount = if (now - usage.dayWindowStartMs >= DAY_MS) 0 else usage.dayCount
        )
        if (normalized != usage) {
            usageRepository.saveUsage(providerId, normalized)
        }
        return normalized
    }

    companion object {
        private const val MINUTE_MS = 60_000L
        private const val HOUR_MS = 60 * MINUTE_MS
        private const val DAY_MS = 24 * HOUR_MS
    }
}
