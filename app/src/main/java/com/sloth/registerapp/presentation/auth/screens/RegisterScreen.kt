package com.sloth.registerapp.presentation.auth.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.google.gson.Gson
import com.sloth.registerapp.R
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterResponseDto
import com.sloth.registerapp.presentation.app.components.NeuralAnimatedBackdrop
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.Bg)
    ) {
        NeuralAnimatedBackdrop(
            baseColor = NeuralColors.Bg,
            primaryColor = NeuralColors.Primary,
            secondaryColor = NeuralColors.Secondary
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
                    RegisterIdentity()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = NeuralColors.Surface,
                        border = BorderStroke(1.dp, NeuralColors.BorderStrong)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            RegisterSectionTitle("Dados da Conta")

                            NeuralRegisterField(
                                value = username,
                                onValueChange = { username = it; errorMessage = "" },
                                label = "Nome de Usuário",
                                placeholder = "Digite seu username",
                                leadingIcon = Icons.Default.Person,
                                keyboardType = KeyboardType.Text
                            )

                            NeuralRegisterField(
                                value = email,
                                onValueChange = { email = it; errorMessage = "" },
                                label = "Email",
                                placeholder = "seu@email.com",
                                leadingIcon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email
                            )

                            NeuralRegisterField(
                                value = password,
                                onValueChange = { password = it; errorMessage = "" },
                                label = "Senha",
                                placeholder = "Mínimo 6 caracteres",
                                leadingIcon = Icons.Default.Lock,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                            tint = NeuralColors.Muted
                                        )
                                    }
                                },
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                            )

                            NeuralRegisterField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = "" },
                                label = "Confirmar Senha",
                                placeholder = "Repita a senha",
                                leadingIcon = Icons.Default.Lock,
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                            tint = NeuralColors.Muted
                                        )
                                    }
                                },
                                keyboardType = KeyboardType.Password,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                isError = confirmPassword.isNotEmpty() && password != confirmPassword
                            )

                            AnimatedVisibility(
                                visible = confirmPassword.isNotEmpty() && password != confirmPassword,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = NeuralColors.Danger,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "As senhas não coincidem",
                                        color = NeuralColors.Danger,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = errorMessage.isNotEmpty(),
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NeuralColors.Danger.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, NeuralColors.Danger.copy(alpha = 0.30f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = NeuralColors.Danger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(text = errorMessage, color = NeuralColors.Danger, fontSize = 13.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    when {
                                        username.isBlank() -> errorMessage = "Por favor, digite um nome de usuário"
                                        email.isBlank() -> errorMessage = "Por favor, digite um email"
                                        !email.contains("@") -> errorMessage = "Email inválido"
                                        password.isBlank() -> errorMessage = "Por favor, digite uma senha"
                                        password.length < 6 -> errorMessage = "A senha deve ter no mínimo 6 caracteres"
                                        password != confirmPassword -> errorMessage = "As senhas não coincidem"
                                        else -> {
                                            isLoading = true
                                            scope.launch {
                                                try {
                                                    val apiService = RetrofitClient.getInstance(context)
                                                    apiService.register(
                                                        RegisterRequestDto(
                                                            username = username.trim(),
                                                            email = email.trim(),
                                                            password = password
                                                        )
                                                    )
                                                    onRegisterSuccess()
                                                } catch (e: HttpException) {
                                                    errorMessage = if (e.code() == 400) {
                                                        try {
                                                            val body = e.response()?.errorBody()?.string()
                                                            Gson().fromJson(body, RegisterResponseDto::class.java).message
                                                        } catch (_: Exception) {
                                                            "Ocorreu um erro ao registrar."
                                                        }
                                                    } else {
                                                        "Erro ${e.code()}: Não foi possível registrar."
                                                    }
                                                } catch (e: Exception) {
                                                    errorMessage = "Falha na conexão. Verifique sua internet."
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
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NeuralColors.Primary, NeuralColors.Secondary)
                                            )
                                        ),
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
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Criar Conta",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    RegisterFooter(onNavigateToLogin = onNavigateToLogin)
                }
            }
        }
    }
}

@Composable
private fun RegisterIdentity() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = RoundedCornerShape(24.dp),
            color = NeuralColors.Surface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, NeuralColors.BorderStrong)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = NeuralColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text(
            text = "Vently Neural",
            style = TextStyle(
                brush = Brush.horizontalGradient(listOf(NeuralColors.Primary, NeuralColors.Secondary)),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = "Crie sua conta para acessar o sistema",
            color = Color.White.copy(alpha = 0.84f),
            fontSize = 14.sp
        )
        Text(
            text = "Controle de drones, missões e inteligência artificial",
            color = NeuralColors.Muted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RegisterSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeuralColors.Primary.copy(alpha = 0.10f)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = NeuralColors.Primary,
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
                .background(
                    Brush.horizontalGradient(listOf(NeuralColors.BorderStrong, Color.Transparent))
                )
        )
    }
}

@Composable
private fun NeuralRegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = NeuralColors.Muted) },
        placeholder = { Text(placeholder, color = NeuralColors.Muted.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isError) NeuralColors.Danger else NeuralColors.Primary
            )
        },
        trailingIcon = trailingIcon,
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = NeuralColors.Primary,
            unfocusedBorderColor = NeuralColors.BorderStrong,
            errorBorderColor = NeuralColors.Danger,
            focusedContainerColor = Color.Black.copy(alpha = 0.18f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
            errorContainerColor = NeuralColors.Danger.copy(alpha = 0.06f),
            cursorColor = NeuralColors.Primary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        singleLine = true
    )
}

@Composable
private fun RegisterFooter(onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuralColors.Surface, RoundedCornerShape(24.dp))
            .border(1.dp, NeuralColors.Border, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Já tem uma conta?",
                color = NeuralColors.Muted,
                fontSize = 14.sp
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Fazer Login",
                    color = NeuralColors.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
