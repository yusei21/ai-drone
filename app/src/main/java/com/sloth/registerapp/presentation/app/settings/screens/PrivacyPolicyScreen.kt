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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.presentation.app.theme.NeuralColors

@Composable
fun PrivacyPolicyScreen(
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
                    Text("Política de Privacidade", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Termos e condições de uso", fontSize = 11.sp, color = NeuralColors.Muted)
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.28f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = NeuralColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color.Transparent, NeuralColors.BorderStrong, Color.Transparent))
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeuralColors.Surface,
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.20f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = NeuralColors.Primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Documento Simulado — Versão 1.0",
                            color = NeuralColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                PolicySection(
                    number = "1",
                    title = "Coleta de Dados",
                    body = "Coletamos dados de autenticação, telemetria operacional e mídias de missão para funcionamento do aplicativo."
                )
                PolicySection(
                    number = "2",
                    title = "Uso dos Dados",
                    body = "Os dados são utilizados para execução de missões, geração de relatórios e melhoria da experiência operacional."
                )
                PolicySection(
                    number = "3",
                    title = "Armazenamento e Retenção",
                    body = "Informações podem ser armazenadas localmente no dispositivo e, quando configurado, sincronizadas com backend autorizado."
                )
                PolicySection(
                    number = "4",
                    title = "Compartilhamento",
                    body = "Não compartilhamos dados com terceiros fora do escopo operacional e institucional do projeto."
                )
                PolicySection(
                    number = "5",
                    title = "Contato",
                    body = "Para dúvidas sobre privacidade, entre em contato com a equipe responsável pelo projeto."
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PolicySection(number: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, NeuralColors.Border)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(8.dp),
                color = NeuralColors.Primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number, color = NeuralColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = NeuralColors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}
