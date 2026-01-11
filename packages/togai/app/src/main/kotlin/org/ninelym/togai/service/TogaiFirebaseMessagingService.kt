package org.ninelym.togai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.ninelym.togai.R

/**
 * Firebase Cloud Messaging service
 * 
 * Handles push notifications and remote messages
 */
class TogaiFirebaseMessagingService : FirebaseMessagingService() {
    
    private val TAG = "TogaiFCM"
    private val CHANNEL_ID = "togai_notifications"
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token: $token")
        
        // Send token to server
        sendTokenToServer(token)
    }
    
    private fun sendTokenToServer(token: String) {
        // Store token locally
        getSharedPreferences("togai_prefs", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
        
        // TODO: Implement server sync when backend is ready
        // This should send the token to your backend server
        // Example:
        // CoroutineScope(Dispatchers.IO).launch {
        //     try {
        //         apiService.updateFcmToken(token)
        //         Log.i(TAG, "Token sent to server successfully")
        //     } catch (e: Exception) {
        //         Log.e(TAG, "Failed to send token to server", e)
        //     }
        // }
        
        Log.i(TAG, "Token stored locally, server sync pending backend implementation")
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.i(TAG, "Message received from: ${message.from}")
        
        message.notification?.let { notification ->
            showNotification(
                notification.title ?: "Togai",
                notification.body ?: "New message"
            )
        }
        
        message.data.let { data ->
            Log.i(TAG, "Message data: $data")
            // Handle data payload
        }
    }
    
    private fun showNotification(title: String, body: String) {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Togai Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General notifications from Togai"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
