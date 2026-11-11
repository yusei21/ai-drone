package com.sloth.registerapp.presentation.facedetection.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sloth.registerapp.features.facedetection.domain.usecase.RegisterFaceUseCase
import com.sloth.registerapp.presentation.facedetection.screens.RegisteredFacesScreen

class RegisteredFacesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MaterialTheme {
                val faceService = RegisterFaceUseCase.getInstance(applicationContext)
                RegisteredFacesScreen(faceService = faceService)
            }
        }
    }
}
