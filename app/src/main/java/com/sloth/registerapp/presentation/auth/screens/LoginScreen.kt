package com.sloth.registerapp.presentation.auth.screens

import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sloth.registerapp.R
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.core.observability.AppObservability
import com.sloth.registerapp.features.auth.data.manager.LoginHistoryManager
import com.sloth.registerapp.features.auth.data.remote.dto.LoginRequestDto
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import com.sloth.registerapp.presentation.app.components.NeuralAnimatedBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import io.sentry.SentryLevel
import retrofit2.HttpException

private val NeuralBg         = NeuralColors.Bg
private val NeuralSurface    = NeuralColors.Surface
private val NeuralBorder     = NeuralColors.Border
private val NeuralBorderStrong = NeuralColors.BorderStrong
private val NeuralPrimary    = NeuralColors.Primary
private val NeuralSecondary  = NeuralColors.Secondary
private val NeuralMuted      = NeuralColors.Muted
private val NeuralSuccess    = NeuralColors.Success
private val NeuralWarning    = NeuralColors.Warning
private val NeuralDanger     = NeuralColors.Danger

@Composable
fun LoginScreen(
    navController: NavController,
    onRegisterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isApiOnline by remember { mutableStateOf<Boolean?>(null) }
    var visible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val loginHistoryManager = remember(context) { LoginHistoryManager.getInstance(context) }
    val tag = "LoginScreen"

    LaunchedEffect(Unit) {
        visible = true
        val apiService = RetrofitClient.getInstance(context)
        while (isActive) {
            isApiOnline = runCatching {
                val health = apiService.getApiStatus()
                val text = "${health.status.orEmpty()} ${health.message.orEmpty()}".lowercase()
                health.running == true ||
                    text.contains("running") ||
                    text.contains("rodando") ||
                    text.contains("online") ||
                    text.contains("ok")
            }.getOrElse { false }
            delay(12000)
        }
    }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralBg)
    ) {
        NeuralAnimatedBackdrop(
            baseColor = NeuralBg,
            primaryColor = NeuralPrimary,
            secondaryColor = NeuralSecondary
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(tween(600)) + slideInVertically(
                    animationSpec = tween(700),
                    initialOffsetY = { it / 6 }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    LoginIdentity()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = NeuralSurface,
                        border = BorderStroke(1.dp, NeuralBorderStrong)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LoginSectionTitle("Credenciais de Acesso")

                            NeuralTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = "Usuário",
                                placeholder = "Digite seu usuário",
                                leadingIcon = Icons.Default.AccountCircle,
                                singleLine = true,
                                keyboardType = KeyboardType.Text
                            )

                            NeuralTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Senha",
                                placeholder = "Digite sua senha",
                                leadingIcon = Icons.Default.Lock,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                            tint = NeuralMuted
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                }
                            )

                            TextButton(
                                onClick = { showError("Recuperação de senha ainda não implementada.") },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Esqueceu a senha?",
                                    color = NeuralPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    when {
                                        username.isBlank() -> showError("Por favor, digite seu nome de usuário")
                                        password.isBlank() -> showError("Por favor, digite sua senha")
                                        else -> {
                                            isLoading = true
                                            scope.launch {
                                                try {
                                                    AppObservability.breadcrumb(
                                                        category = "auth",
                                                        message = "Login started",
                                                        data = mapOf("method" to "password")
                                                    )
                                                    val apiService = RetrofitClient.getInstance(context)
                                                    val tokenRepository = TokenRepository.getInstance(context)
                                                    val sessionManager = SessionManager.getInstance(context)
                                                    val localSessionManager = LocalSessionManager.getInstance(context)

                                                    val response = try {
                                                        Log.d(tag, "Tentando login via body")
                                                        apiService.loginWithBody(LoginRequestDto(username, password))
                                                    } catch (e: HttpException) {
                                                        if (e.code() == 401) {
                                                            Log.d(tag, "Login via body falhou (401). Tentando Basic Auth")
                                                            val credentials = "$username:$password"
                                                            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                                                            apiService.login(basicAuth)
                                                        } else {
                                                            throw e
                                                        }
                                                    }

                                                    val accessToken = response.resolvedAccessToken()
                                                    if (accessToken.isNullOrBlank()) {
                                                        throw IllegalStateException("Login sem access_token no payload")
                                                    }

                                                    var effectiveUserId = response.resolvedUserId()
                                                    var effectiveUsername = username
                                                    var effectiveEmail = ""

                                                    try {
                                                        val userMe = apiService.getMe("Bearer $accessToken")
                                                        if (effectiveUserId.isBlank()) effectiveUserId = userMe.id
                                                        effectiveUsername = userMe.username.ifBlank { effectiveUsername }
                                                        effectiveEmail = userMe.email.orEmpty()
                                                    } catch (e: Exception) {
                                                        Log.w(tag, "auth/me falhou apos login, mantendo sessao minima.", e)
                                                        AppObservability.breadcrumb(
                                                            category = "auth",
                                                            message = "User profile lookup failed after login",
                                                            level = SentryLevel.WARNING,
                                                            data = mapOf("error_type" to e::class.java.simpleName)
                                                        )
                                                    }

                                                    if (effectiveUserId.isBlank()) {
                                                        effectiveUserId = "local:$username"
                                                    }

                                                    sessionManager.createSession(
                                                        token = accessToken,
                                                        userId = effectiveUserId,
                                                        username = effectiveUsername,
                                                        email = effectiveEmail,
                                                        expiryDays = 7L
                                                    )

                                                    tokenRepository.saveTokens(
                                                        accessToken = accessToken,
                                                        refreshToken = response.refreshToken
                                                    )

                                                    localSessionManager.loginLocal(
                                                        userId = effectiveUserId,
                                                        username = effectiveUsername,
                                                        email = effectiveEmail
                                                    )
                                                    localSessionManager.setServerAuthState(ServerAuthState.SERVER_AUTH_OK)
                                                    AppObservability.breadcrumb(
                                                        category = "auth",
                                                        message = "Login succeeded"
                                                    )
                                                    loginHistoryManager.recordLoginAttempt(
                                                        username = effectiveUsername.ifBlank { username },
                                                        status = LoginAttemptStatus.SUCCESS,
                                                        ownerUserId = effectiveUserId
                                                    )

                                                    navController.navigate("dashboard") {
                                                        popUpTo("welcome") { inclusive = true }
                                                    }
                                                } catch (e: HttpException) {
                                                    Log.e(tag, "Login HTTP ${e.code()}: ${e.message()}")
                                                    AppObservability.breadcrumb(
                                                        category = "auth",
                                                        message = "Login HTTP failure",
                                                        level = if (e.code() == 401) SentryLevel.INFO else SentryLevel.WARNING,
                                                        data = mapOf("status_code" to e.code())
                                                    )
                                                    val message = if (e.code() == 401) {
                                                        "Usuário ou senha inválidos."
                                                    } else {
                                                        "Erro ${e.code()}: ${e.message()}"
                                                    }
                                                    loginHistoryManager.recordLoginAttempt(
                                                        username = username,
                                                        status = LoginAttemptStatus.FAILED
                                                    )
                                                    showError(message)
                                                } catch (e: Exception) {
                                                    Log.e(tag, "Login error: ${e.message}", e)
                                                    AppObservability.captureException(
                                                        throwable = e,
                                                        area = "auth",
                                                        operation = "login",
                                                        level = SentryLevel.WARNING
                                                    )
                                                    loginHistoryManager.recordLoginAttempt(
                                                        username = username,
                                                        status = LoginAttemptStatus.FAILED
                                                    )
                                                    showError("Falha na conexão. Verifique sua internet.")
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                enabled = !isLoading,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.horizontalGradient(listOf(NeuralPrimary, NeuralSecondary))),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Entrar no sistema",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LoginFooter(
                        isApiOnline = isApiOnline,
                        onRegisterClick = onRegisterClick,
                        onSkipClick = onSkipClick
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )

        IconButton(
            onClick = { onSkipClick() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun LoginIdentity() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = RoundedCornerShape(24.dp),
            color = NeuralSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, NeuralBorderStrong)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Logo Vently Neural",
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Text(
            text = "Vently Neural",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(NeuralPrimary, NeuralSecondary)),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
private fun LoginSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeuralPrimary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = NeuralPrimary,
                modifier = Modifier
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(NeuralBorderStrong, Color.Transparent)))
        )
    }
}

@Composable
private fun NeuralTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NeuralMuted) },
        placeholder = { Text(placeholder, color = NeuralMuted.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = NeuralPrimary
            )
        },
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = NeuralPrimary,
            unfocusedBorderColor = NeuralBorderStrong,
            focusedContainerColor = Color.Black.copy(alpha = 0.18f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
            cursorColor = NeuralPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = singleLine
    )
}

@Composable
private fun LoginFooter(
    isApiOnline: Boolean?,
    onRegisterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val statusText = when (isApiOnline) {
        true -> "Sistema on-line"
        false -> "Sistema off-line"
        null -> "Verificando sistema..."
    }
    val statusColor = when (isApiOnline) {
        true -> NeuralSuccess
        false -> NeuralDanger
        null -> NeuralWarning
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuralSurface, RoundedCornerShape(24.dp))
            .border(1.dp, NeuralBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Não tem uma conta?",
                color = NeuralMuted,
                fontSize = 14.sp
            )
            TextButton(onClick = onRegisterClick) {
                Text(
                    text = "Registre-se",
                    color = NeuralPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = statusColor.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.26f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

    }
}
