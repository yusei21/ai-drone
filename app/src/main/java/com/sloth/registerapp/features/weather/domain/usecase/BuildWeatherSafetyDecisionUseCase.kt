package com.sloth.registerapp.features.weather.domain.usecase

import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyDecision
import com.sloth.registerapp.features.weather.domain.model.WeatherSafetyLevel
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot

class BuildWeatherSafetyDecisionUseCase {

    operator fun invoke(snapshot: WeatherSnapshot): WeatherSafetyDecision {
        val reasons = mutableListOf<String>()

        val gust = snapshot.windGustMs ?: 0.0
        val wind = snapshot.windSpeedMs ?: 0.0
        val rain = snapshot.rainMm ?: 0.0
        val rainProbability = snapshot.precipitationProbabilityPercent ?: 0.0
        val lightning = snapshot.lightningRisk ?: 0.0

        val noFly = gust >= 14.0 || rain >= 2.0 || lightning >= 0.7 || rainProbability >= 85.0
        val caution = wind >= 10.0 || rain >= 0.5 || rainProbability >= 60.0

        if (gust >= 14.0) reasons += "Rajada de vento muito alta"
        if (rain >= 2.0) reasons += "Chuva intensa"
        if (lightning >= 0.7) reasons += "Risco de raio"
        if (rainProbability >= 85.0) reasons += "Alta probabilidade de chuva iminente"
        if (!noFly && wind >= 10.0) reasons += "Vento moderado/forte"
        if (!noFly && rain >= 0.5) reasons += "Chance de chuva"
        if (!noFly && rainProbability >= 60.0) reasons += "Probabilidade de chuva em breve"

        return when {
            noFly -> WeatherSafetyDecision(
                level = WeatherSafetyLevel.NO_FLY,
                shortMessage = "Evite voo por risco climático",
                reasons = reasons
            )
            caution -> WeatherSafetyDecision(
                level = WeatherSafetyLevel.CAUTION,
                shortMessage = "Atenção: condições climáticas moderadas",
                reasons = reasons.ifEmpty { listOf("Condições variáveis") }
            )
            else -> WeatherSafetyDecision(
                level = WeatherSafetyLevel.SAFE,
                shortMessage = "Clima favorável para voo",
                reasons = listOf("Condições dentro dos limites")
            )
        }
    }
}
