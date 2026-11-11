package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.presentation.app.theme.NeuralColors

@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.Bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(NeuralColors.Bg, Color(0xFF091836), NeuralColors.Bg)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
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
                Column {
                    Text("Sobre o Aplicativo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Versão ${BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = NeuralColors.Muted)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Identity Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = NeuralColors.Surface,
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeuralColors.Primary.copy(alpha = 0.08f), NeuralColors.Surface)
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = NeuralColors.Primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, NeuralColors.Primary.copy(alpha = 0.40f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = NeuralColors.Primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Vently Neural",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(
                                    listOf(NeuralColors.Primary, NeuralColors.Secondary)
                                ),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = NeuralColors.Muted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = NeuralColors.Success.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NeuralColors.Success.copy(alpha = 0.30f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NeuralColors.Success, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Plataforma Operacional de Drones",
                                    color = NeuralColors.Success,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Description Card
                AboutInfoCard(
                    icon = Icons.Default.Info,
                    title = "O Projeto",
                    body = "O Vently Neural é uma plataforma avançada para planejamento, execução e análise de missões com drones. Integra telemetria em tempo real, visão computacional com IA embarcada e relatórios operacionais completos."
                )

                AboutInfoCard(
                    icon = Icons.Default.AirplanemodeActive,
                    title = "Funcionalidades",
                    body = "Controle de drones DJI via SDK, planejamento de rotas com waypoints no mapa, detecção de pessoas via IA, transmissão de vídeo ao vivo (RTMP), monitoramento climático e geração de relatórios em PDF."
                )

                AboutInfoCard(
                    icon = Icons.Default.Science,
                    title = "Contexto Acadêmico",
                    body = "Desenvolvido no contexto acadêmico com apoio institucional do IFMA e FAPEMA, com foco em pesquisa aplicada, inovação tecnológica e desenvolvimento de sistemas críticos de missão."
                )

                AboutInfoCard(
                    icon = Icons.Default.Business,
                    title = "Apoio Institucional",
                    body = "Instituto Federal do Maranhão (IFMA) e Fundação de Amparo à Pesquisa e ao Desenvolvimento Científico e Tecnológico do Maranhão (FAPEMA)."
                )

                // Tech tags
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuralColors.Surface,
                    border = BorderStroke(1.dp, NeuralColors.Border)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tecnologias", color = NeuralColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Jetpack Compose", "DJI SDK", "Mapbox", "Kotlin").forEach { tech ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = NeuralColors.Primary.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = tech,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        color = NeuralColors.Primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AboutInfoCard(icon: ImageVector, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, NeuralColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = NeuralColors.Primary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(body, color = NeuralColors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
