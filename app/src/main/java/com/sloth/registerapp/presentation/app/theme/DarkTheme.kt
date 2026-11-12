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
    onPrimary         = NeuralColors.Bg,
    primaryContainer  = NeuralColors.SurfaceAlt,
    onPrimaryContainer = NeuralColors.TextPrimary,

    // Secundária (verde — sucesso)
    secondary         = NeuralColors.Success,
    onSecondary       = NeuralColors.Bg,
    secondaryContainer = NeuralColors.SurfaceAlt,
    onSecondaryContainer = NeuralColors.TextPrimary,

    // Terciária (âmbar — aviso)
    tertiary          = NeuralColors.Warning,
    onTertiary        = NeuralColors.Bg,
    tertiaryContainer = NeuralColors.SurfaceAlt,
    onTertiaryContainer = NeuralColors.TextPrimary,

    // Erro / Perigo
    error             = NeuralColors.Danger,
    onError           = Color.White,
    errorContainer    = NeuralColors.SurfaceAlt,
    onErrorContainer  = NeuralColors.TextPrimary,

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
