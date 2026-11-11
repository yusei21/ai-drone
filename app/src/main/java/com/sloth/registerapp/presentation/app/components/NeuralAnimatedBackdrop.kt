package com.sloth.registerapp.presentation.app.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun NeuralAnimatedBackdrop(
    modifier: Modifier = Modifier,
    baseColor: Color,
    primaryColor: Color,
    secondaryColor: Color
) {
    val transition = rememberInfiniteTransition(label = "neural_backdrop")
    val gridOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing)
        ),
        label = "grid_offset"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing)
        ),
        label = "beam_sweep"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor,
                    baseColor.copy(alpha = 0.98f),
                    Color(0xFF020814)
                )
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = pulse), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.16f),
                radius = size.minDimension * 0.44f
            ),
            radius = size.minDimension * 0.44f,
            center = Offset(size.width * 0.18f, size.height * 0.16f),
            blendMode = BlendMode.Screen
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = pulse * 0.9f), Color.Transparent),
                center = Offset(size.width * 0.84f, size.height * 0.78f),
                radius = size.minDimension * 0.40f
            ),
            radius = size.minDimension * 0.40f,
            center = Offset(size.width * 0.84f, size.height * 0.78f),
            blendMode = BlendMode.Screen
        )

        val step = 38.dp.toPx()
        var x = -step + (gridOffset % step)
        while (x < size.width + step) {
            drawLine(
                color = primaryColor.copy(alpha = 0.045f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }

        var y = -(gridOffset % step)
        while (y < size.height + step) {
            drawLine(
                color = secondaryColor.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }

        rotate(degrees = -18f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            val beamX = size.width * sweep
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.05f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(beamX - size.width * 0.34f, -size.height * 0.2f),
                size = Size(
                    width = size.width * 0.42f,
                    height = size.height * 1.5f
                ),
                blendMode = BlendMode.Screen
            )
        }
    }
}
