package com.example.diplom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.example.diplom.data.NotificationViewModel

import com.example.diplom.ui.theme.DiplomTheme
import com.example.diplom.ui.navigation.UmamiApp

import com.example.diplom.data.ApiClient
import com.example.diplom.data.TokenManager

// 1. Наследуемся от ComponentActivity (а не AppCompatActivity)
class MainActivity : ComponentActivity() {
    
    // Launcher для запроса разрешений на уведомления (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Разрешение на уведомления получено")
        } else {
            Log.w("MainActivity", "Разрешение на уведомления отклонено")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val tokenManager = TokenManager(this)
        ApiClient.init(tokenManager)
        
        val notificationViewModel = NotificationViewModel()
        
        // Запрос разрешения (для Android 13+)
        askNotificationPermission()
        
        // Получение и регистрация FCM токена
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("MainActivity", "FCM Token: $token")
            notificationViewModel.updateFcmToken(token)
            
            // Subscribe to global broadcast topic
            FirebaseMessaging.getInstance().subscribeToTopic("global_broadcast")
                .addOnCompleteListener { subTask ->
                    if (subTask.isSuccessful) {
                        Log.d("MainActivity", "Subscribed to global_broadcast topic")
                    }
                }
        }

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "umami_notifications"
            val channelName = "Umami Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "System and broadcast notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("MainActivity", "Notification channel created")
        }

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

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is already granted
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}