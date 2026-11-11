package com.sloth.registerapp.presentation.video.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.streaming.domain.StreamState

@Composable
fun StreamingControl(
    state: StreamState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val (label, enabled, action) = when (state) {
        StreamState.Idle -> Triple("Iniciar Transmissão", true, onStart)
        StreamState.Connecting -> Triple("Conectando...", false, {})
        StreamState.Streaming -> Triple("Parar Transmissão", true, onStop)
        is StreamState.Error -> Triple("Erro RTMP", true, onStart)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Button(
            onClick = action,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary.copy(alpha = 0.2f),
                contentColor = colorScheme.onSurface
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}
