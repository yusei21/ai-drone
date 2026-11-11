package com.sloth.registerapp.features.weather.data.remote

import com.sloth.registerapp.features.weather.data.remote.dto.OpenMeteoCurrentResponseDto
import com.sloth.registerapp.features.weather.data.remote.dto.OpenMeteoForecastResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun getCurrent(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,precipitation,precipitation_probability,wind_speed_10m,wind_speed_80m,wind_speed_120m,wind_speed_180m,wind_gusts_10m,weather_code",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
        @Query("precipitation_unit") precipitationUnit: String = "mm",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoCurrentResponseDto

    @GET("v1/forecast")
    suspend fun getHourly(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") hourly: String = "temperature_2m,precipitation,precipitation_probability,wind_speed_10m,wind_speed_80m,wind_speed_120m,wind_speed_180m,wind_gusts_10m,weather_code",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("wind_speed_unit") windSpeedUnit: String = "ms",
        @Query("precipitation_unit") precipitationUnit: String = "mm",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponseDto
}
