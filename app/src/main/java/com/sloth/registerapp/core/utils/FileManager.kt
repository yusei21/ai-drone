package com.sloth.registerapp.core.utils // Ou qualquer outro pacote para utilitários

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sloth.registerapp.core.observability.AppObservability
import io.sentry.SentryLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum para sabermos em qual pasta trabalhar
enum class FolderType {
    FACE_DETECTION, // Para rostos capturados pelo drone
    FACE_UPLOAD     // Para rostos que o usuário envia para busca
}

object FileManager {

    /**
     * Retorna o diretório base do aplicativo no armazenamento externo.
     * Ex: /storage/emulated/0/Android/data/com.sloth.registerapp/files
     */
    private fun getAppBaseDir(context: Context): File {
        return context.getExternalFilesDir(null)
            ?: throw IOException("Diretório externo do app indisponível")
    }

    /**
     * Retorna o diretório específico para um tipo de pasta, criando-o se não existir.
     */
    private fun getDirectoryFor(context: Context, folderType: FolderType): File {
        val folderName = when (folderType) {
            FolderType.FACE_DETECTION -> "face-detection"
            FolderType.FACE_UPLOAD -> "face-upload"
        }
        val directory = File(getAppBaseDir(context), folderName)
        if (!directory.exists()) {
            directory.mkdirs() // Cria a pasta e qualquer pasta pai necessária
        }
        return directory
    }

    /**
     * Salva um Bitmap em uma das nossas pastas específicas.
     * Retorna o arquivo salvo ou null em caso de erro.
     */
    fun saveBitmap(context: Context, bitmap: Bitmap, folderType: FolderType): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"
        val directory = getDirectoryFor(context, folderType)
        val file = File(directory, fileName)

        return try {
            AppObservability.breadcrumb(
                category = "storage",
                message = "Saving bitmap",
                data = mapOf(
                    "folder_type" to folderType.name,
                    "format" to "JPEG"
                )
            )
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
            }
            file // Retorna o arquivo que foi salvo com sucesso
        } catch (e: IOException) {
            AppObservability.captureException(
                throwable = e,
                area = "storage",
                operation = "save_bitmap",
                level = SentryLevel.WARNING,
                extras = mapOf(
                    "folder_type" to folderType.name,
                    "file_extension" to file.extension
                )
            )
            null // Retorna null se houve um erro
        }
    }

    fun saveDebugBitmap(context: Context, bitmap: Bitmap, name: String) {
        val file = File(context.getExternalFilesDir(null), "$name.png")
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }
            Log.d("DebugBitmap", "Salvou crop em: ${file.name}")
        } catch (e: IOException) {
            AppObservability.captureException(
                throwable = e,
                area = "storage",
                operation = "save_debug_bitmap",
                level = SentryLevel.WARNING,
                extras = mapOf("file_extension" to file.extension)
            )
        }
    }

    /**
     * Carrega todos os arquivos de imagem de uma pasta específica.
     */
    fun loadImagesFrom(context: Context, folderType: FolderType): List<File> {
        val directory = getDirectoryFor(context, folderType)
        // Lista todos os arquivos que terminam com .jpg ou .png (ignorando maiúsculas/minúsculas)
        return directory.listFiles { _, name ->
            name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true)
        }?.toList() ?: emptyList() // Retorna a lista de arquivos ou uma lista vazia se não houver nenhum
    }
}
