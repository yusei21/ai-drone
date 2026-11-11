package com.sloth.registerapp.features.weather.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherApiCurrentResponseDto(
    @SerializedName("current") val current: WeatherApiCurrentDto?
)

data class WeatherApiForecastResponseDto(
    @SerializedName("forecast") val forecast: WeatherApiForecastDto?
)

data class WeatherApiForecastDto(
    @SerializedName("forecastday") val forecastDays: List<WeatherApiForecastDayDto>?
)

data class WeatherApiForecastDayDto(
    @SerializedName("hour") val hourly: List<WeatherApiHourlyDto>?
)

data class WeatherApiHourlyDto(
    @SerializedName("time_epoch") val timeEpochSeconds: Long?,
    @SerializedName("temp_c") val tempC: Double?,
    @SerializedName("precip_mm") val precipMm: Double?,
    @SerializedName("chance_of_rain") val chanceOfRainPercent: Int?,
    @SerializedName("wind_kph") val windKph: Double?,
    @SerializedName("gust_kph") val gustKph: Double?,
    @SerializedName("condition") val condition: WeatherApiConditionDto?
)

data class WeatherApiCurrentDto(
    @SerializedName("temp_c") val tempC: Double?,
    @SerializedName("precip_mm") val precipMm: Double?,
    @SerializedName("wind_kph") val windKph: Double?,
    @SerializedName("gust_kph") val gustKph: Double?,
    @SerializedName("condition") val condition: WeatherApiConditionDto?
)

data class WeatherApiConditionDto(
    @SerializedName("code") val code: Int?
)
