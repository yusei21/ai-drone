package com.sloth.registerapp.core.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    private const val TAG = "PermissionHelper"

    // Lista de todas as permissões relevantes para o app
    private val PERMISSIONS = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    )

    // Descrições amigáveis para cada permissão
    private val permissionDescriptions = mapOf(
        Manifest.permission.CAMERA to PermissionInfo("Câmera", "Necessária para capturar fotos e vídeos."),
        Manifest.permission.VIBRATE to PermissionInfo("Vibração", "Usada para fornecer feedback tátil."),
        Manifest.permission.INTERNET to PermissionInfo("Acesso à Internet", "Essencial para comunicação com a rede."),
        Manifest.permission.ACCESS_WIFI_STATE to PermissionInfo("Status do Wi-Fi", "Verifica o estado da conexão Wi-Fi."),
        Manifest.permission.WAKE_LOCK to PermissionInfo("Manter Ativo", "Impede o dispositivo de dormir durante operações críticas."),
        Manifest.permission.ACCESS_COARSE_LOCATION to PermissionInfo("Localização Aproximada", "Acessa sua localização de forma aproximada."),
        Manifest.permission.ACCESS_FINE_LOCATION to PermissionInfo("Localização Precisa", "Acessa sua localização exata via GPS."),
        Manifest.permission.BLUETOOTH to PermissionInfo("Bluetooth (Legado)", "Para dispositivos Bluetooth mais antigos."),
        Manifest.permission.BLUETOOTH_ADMIN to PermissionInfo("Admin Bluetooth (Legado)", "Para gerenciar dispositivos Bluetooth antigos."),
        Manifest.permission.BLUETOOTH_CONNECT to PermissionInfo("Conexão Bluetooth", "Necessária para se conectar a dispositivos Bluetooth."),
        Manifest.permission.BLUETOOTH_SCAN to PermissionInfo("Busca Bluetooth", "Necessária para encontrar dispositivos Bluetooth próximos."),
        Manifest.permission.BLUETOOTH_ADVERTISE to PermissionInfo("Anúncio Bluetooth", "Permite que o app seja visível para outros dispositivos."),
        Manifest.permission.READ_MEDIA_IMAGES to PermissionInfo("Ler Imagens", "Acessa as imagens armazenadas no dispositivo."),
        Manifest.permission.READ_MEDIA_VIDEO to PermissionInfo("Ler Vídeos", "Acessa os vídeos armazenados no dispositivo."),
        Manifest.permission.READ_MEDIA_AUDIO to PermissionInfo("Ler Áudio", "Acessa os áudios armazenados no dispositivo."),
        Manifest.permission.READ_PHONE_STATE to PermissionInfo("Status do Telefone", "Lê informações do estado do telefone."),
        Manifest.permission.RECORD_AUDIO to PermissionInfo("Gravar Áudio", "Necessária para gravar áudio com o microfone.")
    )

    data class PermissionInfo(
        val name: String,
        val description: String
    )

    // Métodos para a UI: obter nomes, descrições e status de todas as permissões
    fun getPermissionList(): List<String> = PERMISSIONS

    fun getDescription(permission: String): String =
        permissionDescriptions[permission]?.description ?: "Permissão não documentada."

    fun getName(permission: String): String =
        permissionDescriptions[permission]?.name ?: permission

    fun getStatus(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun getAllStatuses(context: Context): List<Triple<String, String, Boolean>> =
        PERMISSIONS.map { Triple(getName(it), getDescription(it), getStatus(context, it)) }


    // ============ Função moderna de requisição de permissões (Jetpack) =============

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var systemAlertWindowLauncher: ActivityResultLauncher<Intent>
    private lateinit var manageExternalStorageLauncher: ActivityResultLauncher<Intent>

    // Inicializa os launchers (chame no onCreate da MainActivity/PermissionActivity)
    fun initializeLaunchers(activity: ComponentActivity, onAllGranted: () -> Unit) {
        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (permissionsMap.all { it.value }) {
                Log.d(TAG, "Todas as permissões de runtime concedidas.")
                checkSpecialPermissions(activity, onAllGranted)
            } else {
                val deniedPermissions = permissionsMap.filter { !it.value }.keys
                Log.d(TAG, "Permissões negadas: ${deniedPermissions.joinToString()}")
                Toast.makeText(activity, "Permissões de runtime necessárias negadas.", Toast.LENGTH_LONG).show()
            }
        }

        systemAlertWindowLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissions(activity, onAllGranted)
        }

        manageExternalStorageLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkSpecialPermissions(activity, onAllGranted)
        }
    }

    fun checkAndRequestAllPermissions(activity: ComponentActivity, onAllGranted: () -> Unit) {
        val missing = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            Log.d(TAG, "Todas as permissões já concedidas.")
            checkSpecialPermissions(activity, onAllGranted)
        }
    }

    private fun checkSpecialPermissions(activity: ComponentActivity, onAllGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))
            systemAlertWindowLauncher.launch(intent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${activity.packageName}"))
            manageExternalStorageLauncher.launch(intent)
            return
        }
        Log.d(TAG, "Todas as permissões (normais e especiais) concedidas.")
        onAllGranted()
    }

    // ============ (Opcional) Função compatível com Activity clássica para pedir permissões =============

    fun checkAndRequestPermissions(activity: Activity): Boolean {
        val permissionsNeeded = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

}
