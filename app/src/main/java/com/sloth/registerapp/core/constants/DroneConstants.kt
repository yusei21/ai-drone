package com.sloth.registerapp.core.constants

object DroneConstants {
    // Limites de bateria
    const val MIN_BATTERY_LEVEL = 20
    const val LOW_BATTERY_LEVEL = 30
    const val CRITICAL_BATTERY_LEVEL = 15

    // Timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L // 30 segundos
    const val COMMAND_TIMEOUT_MS = 5000L     // 5 segundos
    const val TELEMETRY_UPDATE_INTERVAL_MS = 100L // 100ms

    // Valores padrão
    const val DEFAULT_FLIGHT_ALTITUDE = 10f
    const val DEFAULT_FLIGHT_SPEED = 5f
    const val EMERGENCY_LAND_ALTITUDE = 2f

    // Limites de altitude (em metros)
    const val MIN_ALTITUDE = 0.5f           // Altitude mínima
    const val MAX_ALTITUDE = 60f           // Altitude máxima legal (400 pés = ~122m)
    const val SAFE_ALTITUDE = 10f           // Altitude segura padrão
    const val ALTITUDE_WARNING_THRESHOLD = 100f // Aviso de altitude alta
    const val GROUND_CLEARANCE = 0.3f       // Clearance mínimo do solo

    // Limites de velocidade (em m/s)
    const val MIN_SPEED = 0.5f              // Velocidade mínima
    const val MAX_SPEED = 15f               // Velocidade máxima
    const val SAFE_SPEED = 5f               // Velocidade segura padrão
    const val LANDING_SPEED = 0.5f          // Velocidade de pouso
    const val RTH_SPEED = 8f                // Velocidade de retorno para casa
    const val EMERGENCY_DESCENT_SPEED = 1.5f // Velocidade de descida de emergência

    // Limites de distância (em metros)
    const val MAX_DISTANCE_FROM_HOME = 500f // Distância máxima permitida da base
    const val DISTANCE_WARNING_THRESHOLD = 400f // Aviso de distância
    const val MIN_OBSTACLE_DISTANCE = 2f    // Distância mínima de obstáculos
    const val GEOFENCE_RADIUS = 1000f       // Raio da geofence

    // Limites de inclinação/rotação (em graus)
    const val MAX_PITCH_ANGLE = 30f         // Inclinação máxima para frente/trás
    const val MAX_ROLL_ANGLE = 30f          // Inclinação máxima lateral
    const val MAX_YAW_RATE = 90f            // Taxa máxima de rotação (graus/s)
    const val CRITICAL_TILT_ANGLE = 45f     // Ângulo crítico de inclinação

    // Limites de vento
    const val MAX_WIND_SPEED = 10f          // Velocidade máxima de vento (m/s)
    const val WARNING_WIND_SPEED = 8f       // Aviso de vento forte

    // Condições ambientais
    const val MIN_TEMPERATURE_C = -10f      // Temperatura mínima de operação
    const val MAX_TEMPERATURE_C = 40f       // Temperatura máxima de operação
    const val MIN_GPS_SATELLITES = 6        // Mínimo de satélites GPS
    const val GOOD_GPS_SATELLITES = 10      // Número bom de satélites
    const val MIN_GPS_ACCURACY = 5f         // Precisão mínima GPS (metros)

    // Timeouts de segurança
    const val MAX_FLIGHT_TIME_MS = 1200000L // 20 minutos de voo máximo
    const val RTH_TIMEOUT_MS = 120000L      // 2 minutos para RTH
    const val LANDING_TIMEOUT_MS = 30000L   // 30 segundos para pouso
    const val EMERGENCY_RESPONSE_MS = 500L  // Tempo de resposta de emergência

    // Zonas de segurança
    const val NO_FLY_ZONE_BUFFER = 50f      // Buffer de zona proibida (metros)
    const val AIRPORT_EXCLUSION_RADIUS = 8000f // 8km de aeroportos

    // Limites de carga útil
    const val MAX_PAYLOAD_KG = 2f           // Carga máxima
    const val WEIGHT_WARNING_THRESHOLD = 1.5f // Aviso de peso

    // Ações de emergência
    const val EMERGENCY_HOVER_TIME_MS = 5000L // 5 segundos de hover em emergência
    const val AUTO_LAND_DELAY_MS = 10000L   // 10 segundos antes de pouso automático

    // Sensores
    const val OBSTACLE_DETECTION_RANGE = 10f // Alcance de detecção (metros)
    const val COLLISION_WARNING_TIME_S = 2f  // Tempo de aviso de colisão
    const val MIN_SIGNAL_STRENGTH = -90     // Força mínima de sinal (dBm)

    // Checklist pré-voo
    const val PREFLIGHT_CHECK_TIMEOUT_MS = 60000L // 1 minuto para checklist

    // Estados de segurança
    enum class SafetyLevel {
        SAFE,           // Todas as condições normais
        WARNING,        // Condições de aviso
        CRITICAL,       // Condições críticas
        EMERGENCY       // Emergência - ação imediata necessária
    }

    // Códigos de erro de segurança
    object SafetyError {
        const val LOW_BATTERY = 1001
        const val LOST_GPS = 1002
        const val HIGH_WIND = 1003
        const val CONNECTION_LOST = 1004
        const val GEOFENCE_BREACH = 1005
        const val ALTITUDE_LIMIT = 1006
        const val TEMPERATURE_EXTREME = 1007
        const val OBSTACLE_DETECTED = 1008
        const val MOTOR_FAILURE = 1009
        const val SENSOR_MALFUNCTION = 1010
    }
}