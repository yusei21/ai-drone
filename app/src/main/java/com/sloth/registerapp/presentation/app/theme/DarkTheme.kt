package com.sloth.registerapp.presentation.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquema de cores escuro alinhado com a paleta Neural.
 * Agora o MaterialTheme.colorScheme reflete as mesmas cores usadas nos screens.
 */
val DarkColorScheme = darkColorScheme(
    // Base
    background        = NeuralColors.Bg,
    onBackground      = NeuralColors.TextPrimary,

    // Primária (ciano neural)
    primary           = NeuralColors.Primary,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFF003A50),
    onPrimaryContainer = NeuralColors.TextPrimary,

    // Secundária (verde — sucesso)
    secondary         = NeuralColors.Success,
    onSecondary       = NeuralColors.Bg,
    secondaryContainer = Color(0xFF003D28),
    onSecondaryContainer = Color(0xFFDCFCE7),

    // Terciária (âmbar — aviso)
    tertiary          = NeuralColors.Warning,
    onTertiary        = NeuralColors.Bg,
    tertiaryContainer = Color(0xFF4A3000),
    onTertiaryContainer = Color(0xFFFFEDD5),

    // Erro / Perigo
    error             = NeuralColors.Danger,
    onError           = Color.White,
    errorContainer    = Color(0xFF4A001A),
    onErrorContainer  = Color(0xFFFFCDD8),

    // Superfícies
    surface           = NeuralColors.Surface,
    surfaceVariant    = NeuralColors.SurfaceAlt,
    onSurface         = NeuralColors.TextPrimary,
    onSurfaceVariant  = NeuralColors.Muted,

    // Bordas e contornos
    outline           = NeuralColors.Border,
    outlineVariant    = NeuralColors.BorderStrong,

    // Inversas (usadas em tooltips/snackbars claros)
    inverseSurface    = Color(0xFFE5E7EB),
    inverseOnSurface  = NeuralColors.Bg,
    inversePrimary    = NeuralColors.Secondary,

    // Extras
    surfaceTint       = NeuralColors.Primary,
    scrim             = Color(0x99000000),
)
