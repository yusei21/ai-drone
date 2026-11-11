package com.sloth.registerapp.presentation.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

data class SyncStatus(
    val isOnline: Boolean,
    val pendingOperations: Int,
    val failedOperations: Int,
    val lastSyncTimestamp: Long
)

@Composable
fun SyncStatusBar(syncStatus: SyncStatus) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !syncStatus.isOnline -> colorScheme.tertiary
            syncStatus.failedOperations > 0 -> colorScheme.error
            syncStatus.pendingOperations > 0 -> colorScheme.primary
            else -> colorScheme.secondary
        },
        animationSpec = tween(500), label = ""
    )
    val contentColor = when {
        !syncStatus.isOnline -> colorScheme.onTertiary
        syncStatus.failedOperations > 0 -> colorScheme.onError
        syncStatus.pendingOperations > 0 -> colorScheme.onPrimary
        else -> colorScheme.onSecondary
    }

    val icon: ImageVector
    val text: String

    when {
        !syncStatus.isOnline -> {
            icon = Icons.Default.CloudOff
            text = "OFFLINE"
        }
        syncStatus.failedOperations > 0 -> {
            icon = Icons.Default.Error
            text = "${syncStatus.failedOperations} operações falhadas"
        }
        syncStatus.pendingOperations > 0 -> {
            icon = Icons.Default.Sync
            text = "${syncStatus.pendingOperations} operações pendentes"
        }
        else -> {
            icon = Icons.Default.CheckCircle
            text = "Tudo sincronizado"
        }
    }

    AnimatedVisibility(visible = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = formatLastSync(syncStatus.lastSyncTimestamp),
                color = contentColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun formatLastSync(timestamp: Long): String {
    if (timestamp == 0L) return "Nunca"
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Agora mesmo"
        minutes < 60 -> "Sincronizado há $minutes min"
        hours < 24 -> "Sincronizado há $hours h"
        else -> "Sincronizado há $days dias"
    }
}
