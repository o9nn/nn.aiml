package org.ninelym.togai.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.ninelym.togai.R
import org.ninelym.togai.TogaiApplication
import org.ninelym.evolution.MultimodalRequest

/**
 * Foreground service for AI inference operations
 * 
 * Handles long-running AI tasks in the background
 */
class AIInferenceService : Service() {
    
    private val TAG = "AIInferenceService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "ai_inference_channel"
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private lateinit var app: TogaiApplication
    
    inner class LocalBinder : Binder() {
        fun getService(): AIInferenceService = this@AIInferenceService
    }
    
    override fun onCreate() {
        super.onCreate()
        app = TogaiApplication.getInstance(application)
        createNotificationChannel()
        Log.i(TAG, "AI Inference Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("AI Service Running")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    /**
     * Process AI inference request
     */
    fun processInference(
        textInput: String?,
        imagePrompt: String? = null,
        audioInput: ByteArray? = null,
        quantumProcessing: Boolean = false,
        callback: (Boolean, String) -> Unit
    ) {
        serviceScope.launch {
            try {
                updateNotification("Processing AI request...")
                
                val request = MultimodalRequest(
                    textInput = textInput,
                    imagePrompt = imagePrompt,
                    audioInput = audioInput,
                    quantumProcessing = quantumProcessing
                )
                
                val response = app.multimodalOrchestrator.processMultimodal(request)
                
                if (response.success) {
                    val result = buildString {
                        appendLine("Processing successful")
                        appendLine("Modalities: ${response.results.size}")
                        if (response.fusedResult != null) {
                            appendLine("Confidence: ${response.fusedResult.overallConfidence}")
                        }
                    }
                    updateNotification("Processing complete")
                    callback(true, result)
                } else {
                    updateNotification("Processing failed")
                    callback(false, response.error ?: "Unknown error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Inference error", e)
                updateNotification("Error occurred")
                callback(false, e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Inference",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AI processing notifications"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Create notification
     */
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Togai AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Update notification
     */
    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "AI Inference Service destroyed")
    }
}
