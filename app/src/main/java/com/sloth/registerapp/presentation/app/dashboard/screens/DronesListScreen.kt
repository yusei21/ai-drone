package com.sloth.registerapp.presentation.app.dashboard.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.presentation.app.theme.NeuralColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DronesListScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val drones = remember { defaultCompatibleDrones() }
    val filteredDrones = remember(query, drones) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) drones
        else drones.filter {
            it.name.lowercase().contains(normalized) ||
                it.description.lowercase().contains(normalized)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Drones Compatíveis",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredDrones.size} modelos",
                            color = NeuralColors.Muted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = NeuralColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuralColors.Bg,
                    scrolledContainerColor = NeuralColors.Surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = NeuralColors.Bg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    placeholder = { Text("Pesquisar drone...", color = NeuralColors.Muted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeuralColors.Primary,
                        unfocusedBorderColor = NeuralColors.Border,
                        focusedContainerColor = NeuralColors.Surface,
                        unfocusedContainerColor = NeuralColors.Surface,
                        cursorColor = NeuralColors.Primary
                    )
                )
            }

            items(filteredDrones, key = { it.name }) { drone ->
                DroneStatusCard(
                    modifier = Modifier.fillMaxWidth(),
                    drone = drone,
                    index = 0,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Mock do site oficial da DJI para ${drone.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
