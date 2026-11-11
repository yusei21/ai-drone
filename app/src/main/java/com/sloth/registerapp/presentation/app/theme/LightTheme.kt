package com.sloth.registerapp.presentation.app.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquema de cores claro — mantém a identidade visual neural mas com fundo claro.
 */
val LightColorScheme = lightColorScheme(
    // Base
    background        = Color(0xFFF2F5FA),
    onBackground      = Color(0xFF0B1220),

    // Primária (ciano neural — funciona bem em fundo claro também)
    primary           = Color(0xFF0099CC),
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFD0F0FF),
    onPrimaryContainer = Color(0xFF003A50),

    // Secundária (verde)
    secondary         = Color(0xFF1AA06E),
    onSecondary       = Color.White,
    secondaryContainer = Color(0xFFCDFAE6),
    onSecondaryContainer = Color(0xFF003D28),

    // Terciária (âmbar)
    tertiary          = Color(0xFFB07A00),
    onTertiary        = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF4A3000),

    // Erro / Perigo
    error             = Color(0xFFCC2244),
    onError           = Color.White,
    errorContainer    = Color(0xFFFFCDD8),
    onErrorContainer  = Color(0xFF4A001A),

    // Superfícies
    surface           = Color.White,
    surfaceVariant    = Color(0xFFECF0F7),
    onSurface         = Color(0xFF0B1220),
    onSurfaceVariant  = Color(0xFF4A6080),

    // Bordas
    outline           = Color(0xFFCBD5E1),
    outlineVariant    = Color(0xFFE2E8F0),

    // Inversas
    inverseSurface    = Color(0xFF0A1628),
    inverseOnSurface  = Color.White,
    inversePrimary    = NeuralColors.Primary,

    // Extras
    surfaceTint       = Color(0xFF0099CC),
    scrim             = Color(0x99000000),
)
