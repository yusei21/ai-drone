package com.sloth.registerapp.features.weather.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoCurrentResponseDto(
    @SerializedName("current") val current: OpenMeteoCurrentDto?
)

data class OpenMeteoForecastResponseDto(
    @SerializedName("hourly") val hourly: OpenMeteoHourlyDto?
)

data class OpenMeteoHourlyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("temperature_2m") val temperature2m: List<Double>?,
    @SerializedName("precipitation") val precipitation: List<Double>?,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int>?,
    @SerializedName("wind_speed_10m") val windSpeed10m: List<Double>?,
    @SerializedName("wind_speed_80m") val windSpeed80m: List<Double>?,
    @SerializedName("wind_speed_120m") val windSpeed120m: List<Double>?,
    @SerializedName("wind_speed_180m") val windSpeed180m: List<Double>?,
    @SerializedName("wind_gusts_10m") val windGusts10m: List<Double>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?
)

data class OpenMeteoCurrentDto(
    @SerializedName("time") val time: String?,
    @SerializedName("temperature_2m") val temperature2m: Double?,
    @SerializedName("precipitation") val precipitation: Double?,
    @SerializedName("precipitation_probability") val precipitationProbability: Int?,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double?,
    @SerializedName("wind_speed_80m") val windSpeed80m: Double?,
    @SerializedName("wind_speed_120m") val windSpeed120m: Double?,
    @SerializedName("wind_speed_180m") val windSpeed180m: Double?,
    @SerializedName("wind_gusts_10m") val windGusts10m: Double?,
    @SerializedName("weather_code") val weatherCode: Int?
)
