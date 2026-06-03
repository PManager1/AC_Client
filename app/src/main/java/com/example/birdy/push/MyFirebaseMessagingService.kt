package com.example.birdy.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.birdy.MainActivity
import com.example.birdy.R
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "birdy_push_channel"
        private const val CHANNEL_NAME = "U-DO Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "📱 FCM token received: ${token.take(20)}...")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "🔔 Push notification received!")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "U-DO"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""

        Log.d(TAG, "🔔 Title: $title")
        Log.d(TAG, "🔔 Body: $body")
        Log.d(TAG, "🔔 Data: ${remoteMessage.data}")

        showNotification(title, body, remoteMessage.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // HIGH = shows as heads-up notification
            ).apply {
                description = "Push notifications from U-DO"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open MainActivity when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For Android 7 and below
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "✅ Notification displayed: $title")
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${Config.API_BASE_URL}/saveDeviceToken")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Platform", "android")

                // Attach auth token if available
                val authToken = AuthManager.getToken(this@MyFirebaseMessagingService)
                if (!authToken.isNullOrEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                    Log.d(TAG, "➡️ Auth header attached")
                } else {
                    Log.w(TAG, "⚠️ No auth token — cannot send device token yet")
                    return@launch
                }

                val payload = """{"token":"$token","platform":"android"}"""
                connection.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    Log.d(TAG, "✅ FCM token sent to server successfully")
                } else {
                    Log.e(TAG, "❌ Failed to send token: HTTP $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending FCM token: ${e.message}")
            }
        }
    }
}