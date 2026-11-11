package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.features.auth.data.manager.LoginHistoryManager
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import com.sloth.registerapp.features.auth.domain.model.LoginHistory
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentLoginsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember(context) { LoginHistoryManager.getInstance(context) }
    var logs by remember { mutableStateOf<List<LoginHistory>>(emptyList()) }

    LaunchedEffect(Unit) {
        logs = manager.getRecentLogins(limit = 30)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.Bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(NeuralColors.Bg, Color(0xFF091836), NeuralColors.Bg))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBackClick,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Surface,
                    border = BorderStroke(1.dp, NeuralColors.Border)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Últimos Logins", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${logs.size} registros", fontSize = 11.sp, color = NeuralColors.Muted)
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.28f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = NeuralColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, NeuralColors.BorderStrong, Color.Transparent)
                        )
                    )
            )

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = NeuralColors.Surface,
                            border = BorderStroke(1.dp, NeuralColors.Border)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = NeuralColors.Muted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Nenhum login registrado ainda.", color = NeuralColors.Muted, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { item ->
                        LoginHistoryCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginHistoryCard(item: LoginHistory) {
    val success = item.status == LoginAttemptStatus.SUCCESS
    val statusColor = if (success) NeuralColors.Success else NeuralColors.Danger

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (success) "Login realizado" else "Falha de login",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text("Usuário: ${item.usernameSnapshot}", color = Color.White, fontSize = 13.sp)
                Text(
                    text = "Dispositivo: ${item.deviceLabel ?: "Não informado"}",
                    color = NeuralColors.Muted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Data: ${item.createdAtMs.toDateTimeLabel()}",
                    color = NeuralColors.Muted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!item.ipOrNetwork.isNullOrBlank()) {
                    Text(
                        text = "Rede: ${item.ipOrNetwork}",
                        color = NeuralColors.Primary.copy(alpha = 0.70f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun Long.toDateTimeLabel(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    return formatter.format(Date(this))
}
