package com.sloth.registerapp.presentation.facedetection.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Tela de Registro Facial
 * 
 * Interface para capturar e registrar novos rostos no banco de dados
 */
@Composable
fun FaceRegistrationScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Face Registration",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Coming soon...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
