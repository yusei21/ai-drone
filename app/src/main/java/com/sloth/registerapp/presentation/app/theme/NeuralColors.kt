package com.sloth.registerapp.presentation.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de cores centralizada do tema Neural.
 * Todos os screens devem importar daqui em vez de definir cores localmente.
 */
object NeuralColors {
    // Fundos
    val Bg            = Color(0xFF050E1F)
    val Surface       = Color(0xFF0A1628)
    val SurfaceAlt    = Color(0xFF071120)
    val SurfaceOverlay = Color(0xCC0A1628)  // para painéis sobrepostos ao mapa

    // Bordas
    val Border        = Color(0xFF0D2040)
    val BorderStrong  = Color(0xFF0D2A50)

    // Principais
    val Primary       = Color(0xFF00C2FF)   // ciano neural
    val Secondary     = Color(0xFF0066FF)   // azul neural

    // Semânticas
    val Success       = Color(0xFF00E5A0)
    val Warning       = Color(0xFFFFB800)
    val Danger        = Color(0xFFFF3B6E)

    // Texto
    val TextPrimary   = Color(0xEBFFFFFF)   // branco 92%
    val Muted         = Color(0xFF4A7FA5)   // azul-cinza muted

    // Extras
    val CardGlow      = Color(0xFF112A4A)
    val GridLine      = Color(0x0A4EA1FF)
}
