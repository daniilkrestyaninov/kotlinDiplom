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

// 1. Наследуемся от ComponentActivity (а не AppCompatActivity)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    UmamiMobileScreen()
                }
            }
        }
    }
}