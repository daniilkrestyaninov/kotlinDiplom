package com.example.diplom.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.diplom.MainActivity
import com.example.diplom.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UmamiMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Umami", it.body ?: "")
        } ?: run {
            // Check if message contains a data payload.
            if (remoteMessage.data.isNotEmpty()) {
                Log.d("FCM", "Message data payload: ${remoteMessage.data}")
                val title = remoteMessage.data["title"] ?: "Umami"
                val body = remoteMessage.data["body"] ?: ""
                sendNotification(title, body)
            }
        }
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait a bit to ensure TokenManager/ApiClient is initialized if called during startup
                val service = ApiClient.notificationService
                val response = service.registerDevice(DeviceTokenRequest(token))
                if (response.isSuccessful) {
                    Log.d("FCM", "Token successfully registered on server")
                } else {
                    Log.e("FCM", "Failed to register token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error registering token: ${e.message}")
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "umami_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Umami Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
