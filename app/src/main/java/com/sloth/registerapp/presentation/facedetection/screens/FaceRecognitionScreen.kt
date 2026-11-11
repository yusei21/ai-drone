package com.sloth.registerapp.presentation.facedetection.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sloth.registerapp.features.facedetection.domain.usecase.RegisterFaceUseCase
import com.sloth.registerapp.features.facedetection.data.ml.FaceAnalysisResult
import com.sloth.registerapp.features.facedetection.data.ml.FaceAnalyzer
import com.sloth.registerapp.features.facedetection.data.ml.FaceAnalyzerConfig
import com.sloth.registerapp.features.facedetection.data.ml.FaceAnalyzerListener
import com.sloth.registerapp.core.utils.imageProxyToBitmap
import com.sloth.registerapp.presentation.facedetection.viewmodels.FaceRegistrationUiState
import com.sloth.registerapp.presentation.facedetection.viewmodels.FaceRegistrationViewModel
import com.sloth.registerapp.presentation.facedetection.viewmodels.FaceRegistrationViewModelFactory
import java.util.concurrent.Executors

/**
 * Tela principal de registro facial
 */
@Composable
fun FaceRegistrationScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val faceService = remember {
        RegisterFaceUseCase.getInstance(context)
    }

    val viewModel: FaceRegistrationViewModel = viewModel(
        factory = FaceRegistrationViewModelFactory(faceService)
    )

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        FaceRegistrationContent(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack
        )
    } else {
        PermissionRequestScreen(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * Conteúdo principal da tela com gerenciamento de estados
 */
@Composable
fun FaceRegistrationContent(
    viewModel: FaceRegistrationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        when (val state = uiState) {
            is FaceRegistrationUiState.Scanning -> {
                CameraPreviewScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onOpenRegistered = {
                        viewModel.faceService.openRegisteredFacesScreen(context)
                    }
                )
            }
            is FaceRegistrationUiState.Processing -> {
                ProcessingScreen(message = "Processando imagem...")
            }
            is FaceRegistrationUiState.Saving -> {
                ProcessingScreen(message = "Salvando cadastro...")
            }
            is FaceRegistrationUiState.Success -> {
                ResultScreen(
                    bitmap = state.bitmap,
                    embedding = state.embedding,
                    isDuplicate = state.isDuplicate,
                    duplicateName = state.existingFace?.name,
                    onSave = { name ->
                        viewModel.saveFace(name, state.embedding)
                    },
                    onReset = { viewModel.resetState() }
                )
            }
            is FaceRegistrationUiState.Saved -> {
                SavedScreen(
                    name = state.name,
                    onContinue = { viewModel.resetState() },
                    onOpenRegistered = {
                        viewModel.faceService.openRegisteredFacesScreen(context)
                    }
                )
            }
            is FaceRegistrationUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.resetState() }
                )
            }
        }
    }
}

/**
 * Tela de preview da câmera com análise em tempo real
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(
    viewModel: FaceRegistrationViewModel,
    onNavigateBack: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisResult by viewModel.analysisResult.collectAsState()

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val canCapture = remember(analysisResult) {
        analysisResult is FaceAnalysisResult.AdvancedResult &&
                (analysisResult as FaceAnalysisResult.AdvancedResult).isStable
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        val config = FaceAnalyzerConfig(advancedAnalysis = true)
                        it.setAnalyzer(
                            cameraExecutor,
                            FaceAnalyzer(
                                listener = object : FaceAnalyzerListener {
                                    override fun onResult(result: FaceAnalysisResult) {
                                        viewModel.updateAnalysisResult(result)
                                    }
                                },
                                config = config
                            )
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("CameraPreviewScreen", "Erro ao iniciar câmera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Scaffold(
        topBar = {
            CameraTopBar(
                onNavigateBack = onNavigateBack,
                onOpenRegistered = onOpenRegistered
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview da câmera
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay escuro nas bordas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
            )

            // Overlay de guia facial
            FaceGuideOverlay(analysisResult = analysisResult)

            // Card de instruções
            InstructionsCard(
                analysisResult = analysisResult,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // Botão de captura
            CaptureButton(
                enabled = canCapture,
                onClick = {
                    capturePhoto(context, imageCapture, viewModel)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

/**
 * TopBar da tela de câmera com navegação e ações
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTopBar(
    onNavigateBack: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Cadastro Facial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Botão para ver rostos cadastrados
            FilledTonalButton(
                onClick = onOpenRegistered,
                modifier = Modifier.padding(end = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ver Cadastros",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
        )
    )
}

/**
 * Overlay com guia de posicionamento do rosto
 */
@Composable
fun FaceGuideOverlay(analysisResult: FaceAnalysisResult) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val color = when (analysisResult) {
        is FaceAnalysisResult.AdvancedResult -> {
            when {
                analysisResult.isStable -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            }
        }
        is FaceAnalysisResult.FaceDetected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scale(if (analysisResult is FaceAnalysisResult.AdvancedResult && analysisResult.isStable) scale else 1f)
    ) {
        val ovalWidth = size.width * 0.65f
        val ovalHeight = size.height * 0.45f
        val left = (size.width - ovalWidth) / 2
        val top = (size.height - ovalHeight) / 2

        // Oval principal
        drawOval(
            color = color,
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 6f)
        )

        // Cantos decorativos
        val cornerLength = 40f
        val cornerOffset = 20f

        // Canto superior esquerdo
        drawLine(
            color = color,
            start = Offset(left - cornerOffset, top),
            end = Offset(left - cornerOffset, top - cornerLength),
            strokeWidth = 6f
        )
        drawLine(
            color = color,
            start = Offset(left, top - cornerOffset),
            end = Offset(left - cornerLength, top - cornerOffset),
            strokeWidth = 6f
        )
    }
}

/**
 * Card com instruções para o usuário
 */
@Composable
fun InstructionsCard(
    analysisResult: FaceAnalysisResult,
    modifier: Modifier = Modifier
) {
    val (message, icon, color) = when (analysisResult) {
        is FaceAnalysisResult.NoFace ->
            Triple("Posicione seu rosto no centro", Icons.Default.FaceRetouchingNatural, MaterialTheme.colorScheme.tertiary)
        is FaceAnalysisResult.MultipleFaces ->
            Triple("Apenas uma pessoa por vez", Icons.Default.Groups, MaterialTheme.colorScheme.error)
        is FaceAnalysisResult.AdvancedResult -> {
            when {
                !analysisResult.isCentered ->
                    Triple("Centralize seu rosto", Icons.Default.CenterFocusWeak, MaterialTheme.colorScheme.tertiary)
                !analysisResult.isWellLit ->
                    Triple("Melhore a iluminação", Icons.Default.WbSunny, MaterialTheme.colorScheme.tertiary)
                !analysisResult.isStable ->
                    Triple("Mantenha a posição...", Icons.Default.Timer, MaterialTheme.colorScheme.primary)
                else ->
                    Triple("Pronto para capturar!", Icons.Default.CheckCircle, MaterialTheme.colorScheme.secondary)
            }
        }
        is FaceAnalysisResult.Error ->
            Triple("Erro na análise", Icons.Default.Error, MaterialTheme.colorScheme.error)
        else -> Triple("Aguardando análise", Icons.Default.HourglassEmpty, MaterialTheme.colorScheme.onSurface)
    }

    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Botão flutuante para capturar foto
 */
@Composable
fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Anel externo pulsante quando pronto
        if (enabled) {
            val infiniteTransition = rememberInfiniteTransition(label = "ring")
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ring"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(ringScale)
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // Botão principal
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            containerColor = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capturar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * Tela de processamento com loading
 */
@Composable
fun ProcessingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Tela de resultado com opção de salvar
 */
@Composable
fun ResultScreen(
    bitmap: Bitmap,
    embedding: FloatArray,
    isDuplicate: Boolean,
    duplicateName: String?,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showEmbedding by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Text(
                text = if (isDuplicate) "Rosto Já Cadastrado" else "Rosto Capturado!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Imagem capturada
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Rosto capturado",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alerta de duplicação
            if (isDuplicate && duplicateName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Rosto já registrado",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Cadastrado como: $duplicateName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Campo de nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da pessoa") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDuplicate,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDuplicate) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card de embedding (expansível)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Embedding Gerado",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { showEmbedding = !showEmbedding }) {
                            Icon(
                                imageVector = if (showEmbedding) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showEmbedding) "Ocultar" else "Expandir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Dimensões: ${embedding.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = showEmbedding) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Primeiros 10 valores:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = embedding.take(10).joinToString(", ") { "%.4f".format(it) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botões de ação
            if (!isDuplicate) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Salvar Cadastro",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rosto já cadastrado")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tirar Nova Foto")
            }
        }
    }
}

/**
 * Tela de sucesso ao salvar
 */
@Composable
fun SavedScreen(
    name: String,
    onContinue: () -> Unit,
    onOpenRegistered: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Ícone animado
            val scale by rememberInfiniteTransition(label = "success").animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cadastro Realizado!",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$name foi cadastrado com sucesso",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botões de ação
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadastrar Novo Rosto",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenRegistered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurface))
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ver Todos os Cadastros",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Tela de erro
 */
@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.error
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Erro no Processo",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tentar Novamente",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tela solicitando permissão de câmera
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Botão de voltar
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissão de Câmera",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Para cadastrar rostos, precisamos acessar sua câmera. Garantimos que suas imagens são processadas com segurança.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permitir Acesso à Câmera",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ========== Helpers ==========

/**
 * Captura foto usando CameraX
 */
@androidx.camera.core.ExperimentalGetImage
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: FaceRegistrationViewModel
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()

                if (bitmap != null) {
                    viewModel.processCapture(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CapturePhoto", "Erro ao capturar: ${exception.message}", exception)
            }
        }
    )
}
