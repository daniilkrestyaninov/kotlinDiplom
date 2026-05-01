package com.example.diplom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

import com.example.diplom.ui.theme.DiplomTheme
import com.example.diplom.ui.navigation.UmamiApp

import com.example.diplom.data.ApiClient
import com.example.diplom.data.TokenManager

// 1. Наследуемся от ComponentActivity (а не AppCompatActivity)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tokenManager = TokenManager(this)
        ApiClient.init(tokenManager)
        
        // We instantiate AuthViewModel here or inside UmamiApp using a ViewModelProvider factory.
        // For simplicity in Compose, we can just pass the tokenManager to UmamiApp and let it instantiate.

        // 2. Вместо setContentView используем setContent
        setContent {
            // Базовая тема Material Design
            DiplomTheme(darkTheme = false) {
                // Поверхность, занимающая весь экран
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Вызываем наш UI-компонент (мобильный экран УМАМИ)
                    UmamiApp(tokenManager)
                }
            }
        }
    }
}