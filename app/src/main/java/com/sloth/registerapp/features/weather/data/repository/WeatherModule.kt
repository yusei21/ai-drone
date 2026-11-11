package com.sloth.registerapp.features.weather.data.repository

import android.content.Context
import com.sloth.registerapp.features.weather.data.provider.DefaultWeatherProviderRouter
import com.sloth.registerapp.features.weather.data.provider.OpenMeteoWeatherProvider
import com.sloth.registerapp.features.weather.data.provider.WeatherApiProvider
import com.sloth.registerapp.features.weather.domain.provider.WeatherProvider
import com.sloth.registerapp.features.weather.domain.usecase.GetWeatherDetailUseCase
import com.sloth.registerapp.features.weather.domain.usecase.GetWeatherSummaryUseCase

class WeatherModule private constructor(context: Context) {
    private val providers: Map<String, WeatherProvider> = listOf(
        WeatherApiProvider(),
        OpenMeteoWeatherProvider()
    ).associateBy { it.providerId }

    private val router = DefaultWeatherProviderRouter(context = context.applicationContext, providers = providers)
    private val repository = WeatherRepositoryImpl(router = router)

    val getWeatherSummaryUseCase = GetWeatherSummaryUseCase(repository)
    val getWeatherDetailUseCase = GetWeatherDetailUseCase(repository)

    companion object {
        @Volatile
        private var INSTANCE: WeatherModule? = null

        fun getInstance(context: Context): WeatherModule {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherModule(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
