package com.sloth.registerapp.presentation.app.welcome.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.R
import com.sloth.registerapp.presentation.app.components.NeuralAnimatedBackdrop
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private val Bg          = NeuralColors.Bg
private val CardSurface = NeuralColors.Surface
private val Border      = NeuralColors.BorderStrong
private val BorderSub   = NeuralColors.Border
private val Primary     = NeuralColors.Primary
private val Secondary   = NeuralColors.Secondary
private val Success     = NeuralColors.Success
private val Muted       = NeuralColors.Muted

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "enter_alpha"
    )
    val enterOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 0.06f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "enter_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // Tela de entrada limpa: sem particulas ou luzes animadas sobre o conteúdo.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    alpha = enterAlpha
                    translationY = size.height * enterOffset
                }
        ) {
            Spacer(modifier = Modifier.weight(1f))
            AppHeader()
            Spacer(modifier = Modifier.height(24.dp))
            InfoCard()
            Spacer(modifier = Modifier.weight(1f))
            BottomSection(onEnterClick = onLoginClick)
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "ai-drone",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append("ai-drone") }
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun InfoCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "Operações aéreas com visão computacional",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Acesse vídeo DJI, detecção de pessoas e missões com os controles essenciais em destaque.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 22.sp
        )

    }
}

@Composable
private fun FeatureItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun StatusBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(
                color = Primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(1.dp, Border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(Success, CircleShape)
        )
        Text(
            text = "Sistema pronto",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BottomSection(onEnterClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        GradientButton(onClick = onEnterClick)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ai-drone  ·  v${BuildConfig.VERSION_NAME}",
            color = Muted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GradientButton(onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "btn_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                Brush.horizontalGradient(listOf(Primary, Primary)),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        val released = tryAwaitRelease()
                        pressed = false
                        if (released) onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Entrar",
                color = Bg,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Bg,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
