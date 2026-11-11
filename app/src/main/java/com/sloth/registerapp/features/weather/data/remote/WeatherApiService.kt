package com.sloth.registerapp.features.weather.data.remote

import com.sloth.registerapp.features.weather.data.remote.dto.WeatherApiCurrentResponseDto
import com.sloth.registerapp.features.weather.data.remote.dto.WeatherApiForecastResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/current.json")
    suspend fun getCurrent(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("aqi") aqi: String = "no"
    ): WeatherApiCurrentResponseDto

    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("days") days: Int = 1,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherApiForecastResponseDto
}
