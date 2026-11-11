package com.sloth.registerapp.presentation.facedetection.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.features.facedetection.domain.usecase.RegisterFaceUseCase
import com.sloth.registerapp.features.facedetection.data.local.FaceEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredFacesScreen(
    faceService: RegisterFaceUseCase,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val faces by faceService.getAllFaces().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var faceToDelete by remember { mutableStateOf<FaceEntity?>(null) }

    // Filtrar rostos baseado na pesquisa
    val filteredFaces = remember(faces, searchQuery) {
        if (searchQuery.isBlank()) {
            faces
        } else {
            faces.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pessoas Cadastradas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${faces.size} ${if (faces.size == 1) "pessoa" else "pessoas"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
        ) {
            if (faces.isEmpty()) {
                // Estado vazio
                EmptyStateContent()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Barra de pesquisa
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Contador de resultados
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "${filteredFaces.size} ${if (filteredFaces.size == 1) "resultado encontrado" else "resultados encontrados"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Lista de rostos
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredFaces.isEmpty() && searchQuery.isNotBlank()) {
                            item {
                                NoResultsContent(searchQuery = searchQuery)
                            }
                        } else {
                            items(
                                items = filteredFaces,
                                key = { it.id }
                            ) { face ->
                                FaceListItem(
                                    face = face,
                                    onDelete = { faceToDelete = face }
                                )
                            }
                        }
                    }

                    // Botão de limpar todos (apenas se houver rostos)
                    if (faces.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = colorScheme.surface
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteAllDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colorScheme.error
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(colorScheme.error, colorScheme.error))
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Limpar Todos os Cadastros",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmação para deletar uma pessoa
    if (faceToDelete != null) {
        DeleteConfirmationDialog(
            title = "Deletar Cadastro",
            message = "Deseja realmente deletar o cadastro de ${faceToDelete?.name}?",
            onConfirm = {
                scope.launch {
                    faceToDelete?.let { faceService.deleteFace(it) }
                    faceToDelete = null
                }
            },
            onDismiss = { faceToDelete = null }
        )
    }

    // Diálogo de confirmação para deletar todos
    if (showDeleteAllDialog) {
        DeleteConfirmationDialog(
            title = "Limpar Todos os Cadastros",
            message = "Esta ação irá deletar TODOS os ${faces.size} cadastros permanentemente. Deseja continuar?",
            onConfirm = {
                scope.launch {
                    faceService.deleteAllFaces()
                    showDeleteAllDialog = false
                }
            },
            onDismiss = { showDeleteAllDialog = false },
            isDestructive = true
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text("Pesquisar por nome...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpar pesquisa"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface
        )
    )
}

@Composable
fun FaceListItem(
    face: FaceEntity,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 4.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Avatar com inicial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colorScheme.primary,
                                colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = face.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nome
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = face.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${face.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // Botão deletar
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Deletar ${face.name}",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent() {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
            Icon(
            imageVector = Icons.Default.PersonOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colorScheme.primary
        )
    }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Nenhuma pessoa cadastrada",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Comece cadastrando o primeiro rosto para utilizar o sistema",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoResultsContent(searchQuery: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nenhum resultado encontrado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Não encontramos ninguém com \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val dangerColor = colorScheme.error
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.Delete,
                contentDescription = null,
                tint = dangerColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dangerColor
                )
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
