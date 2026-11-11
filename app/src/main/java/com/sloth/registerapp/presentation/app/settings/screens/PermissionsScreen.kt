package com.sloth.registerapp.presentation.app.settings.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sloth.registerapp.presentation.app.theme.NeuralColors

private data class PermissionUi(
    val label: String,
    val permission: String,
    val granted: Boolean
)

@Composable
fun PermissionsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissions = remember { buildPermissions(context) }
    val grantedCount = permissions.count { it.granted }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Permissões", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("$grantedCount de ${permissions.size} concedidas", fontSize = 11.sp, color = NeuralColors.Muted)
                }
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = NeuralColors.Primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, NeuralColors.Primary.copy(alpha = 0.28f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Security,
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

            // Summary card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = NeuralColors.Surface,
                border = BorderStroke(
                    1.dp,
                    if (grantedCount == permissions.size) NeuralColors.Success.copy(alpha = 0.3f)
                    else NeuralColors.Warning.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (grantedCount == permissions.size) NeuralColors.Success else NeuralColors.Warning,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (grantedCount == permissions.size)
                            "Todas as permissões estão concedidas"
                        else
                            "$grantedCount/${permissions.size} permissões concedidas — algumas funcionalidades podem estar limitadas",
                        color = if (grantedCount == permissions.size) NeuralColors.Success else NeuralColors.Warning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(permissions, key = { it.permission }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = NeuralColors.Surface,
                        border = BorderStroke(
                            1.dp,
                            if (item.granted) NeuralColors.Success.copy(alpha = 0.22f)
                            else NeuralColors.Danger.copy(alpha = 0.22f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (item.granted)
                                    NeuralColors.Success.copy(alpha = 0.10f)
                                else
                                    NeuralColors.Danger.copy(alpha = 0.10f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (item.granted) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (item.granted) NeuralColors.Success else NeuralColors.Danger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (item.granted) "Concedida" else "Não concedida",
                                    fontSize = 12.sp,
                                    color = if (item.granted) NeuralColors.Success else NeuralColors.Danger
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Primary)
            ) {
                Text(
                    text = "Abrir configurações do sistema",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

private fun buildPermissions(context: Context): List<PermissionUi> {
    val list = listOf(
        "Câmera" to Manifest.permission.CAMERA,
        "Localização fina" to Manifest.permission.ACCESS_FINE_LOCATION,
        "Localização aproximada" to Manifest.permission.ACCESS_COARSE_LOCATION,
        "Armazenamento" to Manifest.permission.WRITE_EXTERNAL_STORAGE,
        "Microfone" to Manifest.permission.RECORD_AUDIO,
        "Bluetooth" to Manifest.permission.BLUETOOTH,
        "Bluetooth Admin" to Manifest.permission.BLUETOOTH_ADMIN
    )
    return list.map { (label, permission) ->
        PermissionUi(
            label = label,
            permission = permission,
            granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
}
