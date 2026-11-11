package com.sloth.registerapp.features.weather.data.provider

import android.content.Context
import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.core.settings.WeatherQuotaSettingsRepository
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot
import com.sloth.registerapp.features.weather.domain.provider.WeatherProvider
import com.sloth.registerapp.features.weather.domain.provider.WeatherProviderRouter
import com.sloth.registerapp.features.weather.domain.provider.WeatherQuotaController
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException
import io.sentry.SentryLevel

class DefaultWeatherProviderRouter(
    context: Context,
    private val providers: Map<String, WeatherProvider>,
    private val quotaController: WeatherQuotaController = DefaultWeatherQuotaController(context)
) : WeatherProviderRouter {
    private val tag = "WeatherProviderRouter"

    private val settings = WeatherProviderSettingsRepository.getInstance(context.applicationContext)
    private val quotaSettings = WeatherQuotaSettingsRepository.getInstance(context.applicationContext)

    override suspend fun getCurrentWithFallback(lat: Double, lon: Double): WeatherSnapshot {
        val primary = settings.primaryProviderValue()
        val fallback = settings.fallbackProviderValue()
        return executeWithFallback(primary, fallback) { provider -> provider.getCurrent(lat, lon) }
    }

    override suspend fun getHourlyWithFallback(lat: Double, lon: Double): List<WeatherSnapshot> {
        val primary = settings.primaryProviderValue()
        val fallback = settings.fallbackProviderValue()
        return executeWithFallback(primary, fallback) { provider -> provider.getHourly(lat, lon) }
    }

    private suspend fun <T> executeWithFallback(
        primaryId: String,
        fallbackId: String,
        call: suspend (WeatherProvider) -> T
    ): T {
        val primary = providers[primaryId] ?: error("Provider principal não encontrado: $primaryId")
        val fallback = providers[fallbackId] ?: error("Provider fallback não encontrado: $fallbackId")

        return runCatching {
            guardedCall(primary, call)
        }.getOrElse { firstError ->
            Log.w(
                tag,
                "Provider principal falhou (${primary.providerId}): ${firstError.toLogReason()}. Tentando fallback ${fallback.providerId}."
            )
            AppObservability.breadcrumb(
                category = "weather",
                message = "Primary weather provider failed; trying fallback",
                level = SentryLevel.WARNING,
                data = mapOf(
                    "primary_provider" to primary.providerId,
                    "fallback_provider" to fallback.providerId,
                    "error_type" to firstError::class.java.simpleName,
                    "http_status_code" to (firstError as? HttpException)?.code()
                )
            )
            runCatching {
                guardedCall(fallback, call)
            }.getOrElse { fallbackError ->
                Log.e(
                    tag,
                    "Fallback também falhou (${fallback.providerId}): ${fallbackError.toLogReason()}",
                    fallbackError
                )
                AppObservability.captureException(
                    throwable = fallbackError,
                    area = "weather",
                    operation = "get_weather_with_fallback",
                    tags = mapOf(
                        "primary_provider" to primary.providerId,
                        "fallback_provider" to fallback.providerId
                    ),
                    extras = mapOf(
                        "primary_error_type" to firstError::class.java.simpleName,
                        "primary_http_status_code" to (firstError as? HttpException)?.code(),
                        "fallback_http_status_code" to (fallbackError as? HttpException)?.code()
                    )
                )
                throw firstError
            }
        }
    }

    private suspend fun <T> guardedCall(provider: WeatherProvider, call: suspend (WeatherProvider) -> T): T {
        val policy = quotaSettings.policy.first()
        if (policy.prototypeMode && provider.providerId !in ALLOWED_PROTOTYPE_PROVIDERS) {
            error("Provider não permitido em modo protótipo: ${provider.providerId}")
        }
        check(quotaController.canCall(provider.providerId)) {
            "Cota excedida para provider ${provider.providerId}"
        }
        return try {
            AppObservability.breadcrumb(
                category = "weather",
                message = "Weather provider call started",
                data = mapOf("provider" to provider.providerId)
            )
            call(provider).also { quotaController.recordCall(provider.providerId) }
        } catch (e: HttpException) {
            if (e.code() == 429) {
                quotaController.recordRateLimit(provider.providerId)
            }
            Log.w(tag, "Erro HTTP no provider ${provider.providerId}: ${e.code()} ${e.message()}")
            throw e
        } catch (e: Throwable) {
            Log.w(tag, "Erro não HTTP no provider ${provider.providerId}: ${e.toLogReason()}")
            throw e
        }
    }

    private suspend fun WeatherProviderSettingsRepository.primaryProviderValue(): String {
        return primaryProvider.firstOrDefault(WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
    }

    private suspend fun WeatherProviderSettingsRepository.fallbackProviderValue(): String {
        return fallbackProvider.firstOrDefault(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
    }

    private suspend fun kotlinx.coroutines.flow.Flow<String>.firstOrDefault(default: String): String {
        return runCatching { this.first() }.getOrDefault(default)
    }

    companion object {
        private val ALLOWED_PROTOTYPE_PROVIDERS = setOf(
            WeatherProviderSettingsRepository.PROVIDER_WEATHER_API,
            WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO
        )
    }
}

private fun Throwable.toLogReason(): String {
    return when (this) {
        is HttpException -> "HTTP ${code()} (${message()})"
        is SocketTimeoutException -> "timeout (${message ?: "sem mensagem"})"
        is IOException -> "erro de rede (${message ?: "sem mensagem"})"
        else -> "${this::class.java.simpleName} (${message ?: "sem mensagem"})"
    }
}
