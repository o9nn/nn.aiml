/**
 * AGML Service - Android Service wrapper for AGML Engine
 * 
 * Provides background processing and integration with Togai's
 * existing AI infrastructure (LLaMA, Stable Diffusion, Live2D).
 */
package com.togai.agml

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Callback interface for AGML responses
 */
interface AGMLCallback {
    fun onResponse(response: AGMLResponse)
    fun onEmotionalStateChanged(state: EmotionalState)
    fun onSelfImageUpdated(selfImage: Map<String, SelfImageComponent>)
    fun onError(error: Throwable)
}

/**
 * Android Service providing AGML capabilities
 */
class AGMLService : Service() {
    
    companion object {
        private const val TAG = "AGMLService"
        
        // Intent actions
        const val ACTION_PROCESS_INPUT = "com.togai.agml.PROCESS_INPUT"
        const val ACTION_QUERY_KNOWLEDGE = "com.togai.agml.QUERY_KNOWLEDGE"
        const val ACTION_GET_SELF_IMAGE = "com.togai.agml.GET_SELF_IMAGE"
        
        // Intent extras
        const val EXTRA_INPUT_TEXT = "input_text"
        const val EXTRA_SUBJECT = "subject"
    }
    
    private lateinit var engine: AGMLEngine
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val callbacks = mutableListOf<AGMLCallback>()
    
    // Binder for local binding
    private val binder = AGMLBinder()
    
    inner class AGMLBinder : Binder() {
        fun getService(): AGMLService = this@AGMLService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AGML Service created")
        
        engine = AGMLEngine(applicationContext)
        
        // Initialize engine
        scope.launch {
            engine.initialize().onSuccess {
                Log.i(TAG, "AGML Engine initialized successfully")
            }.onFailure { error ->
                Log.e(TAG, "Failed to initialize AGML Engine", error)
                callbacks.forEach { it.onError(error) }
            }
        }
        
        // Observe emotional state changes
        scope.launch {
            engine.emotionalStateFlow.collect { state ->
                callbacks.forEach { it.onEmotionalStateChanged(state) }
            }
        }
        
        // Observe self-image updates
        scope.launch {
            engine.selfImageFlow.collect { selfImage ->
                callbacks.forEach { it.onSelfImageUpdated(selfImage) }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PROCESS_INPUT -> {
                val input = intent.getStringExtra(EXTRA_INPUT_TEXT) ?: return
                processInput(input)
            }
            ACTION_QUERY_KNOWLEDGE -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: return
                queryKnowledge(subject)
            }
            ACTION_GET_SELF_IMAGE -> {
                val selfImage = engine.getSelfImage()
                callbacks.forEach { it.onSelfImageUpdated(selfImage) }
            }
        }
    }
    
    /**
     * Process input through AGML engine
     */
    fun processInput(input: String, callback: ((AGMLResponse) -> Unit)? = null) {
        scope.launch {
            try {
                val response = engine.processInput(input)
                callback?.invoke(response)
                callbacks.forEach { it.onResponse(response) }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing input", e)
                callbacks.forEach { it.onError(e) }
            }
        }
    }
    
    /**
     * Process input and return Flow for reactive handling
     */
    fun processInputFlow(input: String): Flow<AGMLResponse> = flow {
        emit(engine.processInput(input))
    }.flowOn(Dispatchers.Default)
    
    /**
     * Query knowledge base
     */
    fun queryKnowledge(subject: String): List<KnowledgeTriple> {
        return engine.queryKnowledge(subject)
    }
    
    /**
     * Get current self-image
     */
    fun getSelfImage(): Map<String, SelfImageComponent> {
        return engine.getSelfImage()
    }
    
    /**
     * Get conversation history
     */
    fun getConversationHistory(): List<Pair<String, String>> {
        return engine.getConversationHistory()
    }
    
    /**
     * Set user predicate
     */
    fun setPredicate(name: String, value: String) {
        engine.setPredicate(name, value)
    }
    
    /**
     * Get user predicate
     */
    fun getPredicate(name: String): String? {
        return engine.getPredicate(name)
    }
    
    /**
     * Set conversation topic
     */
    fun setTopic(topic: String) {
        engine.setTopic(topic)
    }
    
    /**
     * Register callback for AGML events
     */
    fun registerCallback(callback: AGMLCallback) {
        callbacks.add(callback)
    }
    
    /**
     * Unregister callback
     */
    fun unregisterCallback(callback: AGMLCallback) {
        callbacks.remove(callback)
    }
    
    /**
     * Get emotional state flow for reactive UI
     */
    fun getEmotionalStateFlow(): StateFlow<EmotionalState> {
        return engine.emotionalStateFlow
    }
    
    /**
     * Get self-image flow for reactive UI
     */
    fun getSelfImageFlow(): StateFlow<Map<String, SelfImageComponent>> {
        return engine.selfImageFlow
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        engine.shutdown()
        Log.i(TAG, "AGML Service destroyed")
    }
}

/**
 * Extension functions for easy integration with Togai
 */

/**
 * Convert AGML emotional state to Live2D expression
 */
fun EmotionalState.toLive2DExpression(): String {
    return when (primaryEmotion) {
        "excited" -> "happy_high"
        "content" -> "happy_low"
        "angry" -> "angry"
        "sad" -> "sad"
        "alert" -> "surprised"
        else -> "neutral"
    }
}

/**
 * Convert AGML response to voice synthesis parameters
 */
fun AGMLResponse.toVoiceParams(): Map<String, Any> {
    val emotion = emotionalState ?: EmotionalState(0f, 0.3f, 0.5f, "neutral")
    
    return mapOf(
        "text" to text,
        "pitch" to (1.0f + emotion.valence * 0.2f),
        "speed" to (1.0f + emotion.arousal * 0.3f),
        "emotion" to emotion.primaryEmotion
    )
}

/**
 * Integration helper for combining AGML with LLaMA responses
 */
class AGMLLLaMAIntegration(
    private val agmlService: AGMLService
) {
    /**
     * Enhance LLaMA response with AGML meta-cognitive processing
     */
    suspend fun enhanceResponse(
        userInput: String,
        llamaResponse: String
    ): EnhancedResponse {
        // Process through AGML for meta-cognitive analysis
        val agmlResponse = agmlService.processInputFlow(userInput).first()
        
        // Combine insights
        return EnhancedResponse(
            primaryResponse = llamaResponse,
            metaCognitiveInsight = agmlResponse.selfReflection,
            emotionalContext = agmlResponse.emotionalState,
            suggestedTone = determineTone(agmlResponse),
            knowledgeGained = agmlResponse.knowledgeUpdates
        )
    }
    
    private fun determineTone(response: AGMLResponse): String {
        return when (response.metaCognitiveLayer) {
            MetaCognitiveLayer.FOURTH_ORDER -> "philosophical"
            MetaCognitiveLayer.THIRD_ORDER -> "analytical"
            MetaCognitiveLayer.SECOND_ORDER -> "reflective"
            MetaCognitiveLayer.FIRST_ORDER -> "attentive"
            MetaCognitiveLayer.BASE_PROCESSING -> "conversational"
        }
    }
}

/**
 * Enhanced response combining LLaMA and AGML
 */
data class EnhancedResponse(
    val primaryResponse: String,
    val metaCognitiveInsight: String?,
    val emotionalContext: EmotionalState?,
    val suggestedTone: String,
    val knowledgeGained: List<KnowledgeTriple>
)
