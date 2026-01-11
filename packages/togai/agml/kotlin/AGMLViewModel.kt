/**
 * AGML ViewModel - UI integration for AGML meta-cognitive system
 * 
 * Provides reactive state management for Android UI components
 * integrating with Togai's character system and chat interface.
 */
package com.togai.agml

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI State for AGML integration
 */
data class AGMLUiState(
    val isInitialized: Boolean = false,
    val isProcessing: Boolean = false,
    val currentResponse: AGMLResponse? = null,
    val emotionalState: EmotionalState = EmotionalState(0f, 0.3f, 0.5f, "neutral"),
    val selfImage: Map<String, SelfImageComponent> = emptyMap(),
    val conversationHistory: List<ConversationEntry> = emptyList(),
    val metaCognitiveLayer: MetaCognitiveLayer = MetaCognitiveLayer.BASE_PROCESSING,
    val error: String? = null
)

/**
 * Conversation entry for UI display
 */
data class ConversationEntry(
    val id: Long = System.currentTimeMillis(),
    val userInput: String,
    val response: String,
    val emotionalState: EmotionalState?,
    val metaCognitiveLayer: MetaCognitiveLayer,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * UI Events from user interaction
 */
sealed class AGMLEvent {
    data class SendMessage(val text: String) : AGMLEvent()
    data class SetTopic(val topic: String) : AGMLEvent()
    data class SetPredicate(val name: String, val value: String) : AGMLEvent()
    object ClearHistory : AGMLEvent()
    object RequestSelfReflection : AGMLEvent()
}

/**
 * ViewModel for AGML UI integration
 */
class AGMLViewModel(
    private val agmlService: AGMLService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AGMLUiState())
    val uiState: StateFlow<AGMLUiState> = _uiState.asStateFlow()
    
    // Derived states for specific UI components
    val emotionalState: StateFlow<EmotionalState> = _uiState
        .map { it.emotionalState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmotionalState(0f, 0.3f, 0.5f, "neutral"))
    
    val isProcessing: StateFlow<Boolean> = _uiState
        .map { it.isProcessing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val currentLayer: StateFlow<MetaCognitiveLayer> = _uiState
        .map { it.metaCognitiveLayer }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MetaCognitiveLayer.BASE_PROCESSING)
    
    init {
        // Observe service state flows
        viewModelScope.launch {
            agmlService.getEmotionalStateFlow().collect { state ->
                _uiState.update { it.copy(emotionalState = state) }
            }
        }
        
        viewModelScope.launch {
            agmlService.getSelfImageFlow().collect { selfImage ->
                _uiState.update { it.copy(selfImage = selfImage) }
            }
        }
        
        _uiState.update { it.copy(isInitialized = true) }
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: AGMLEvent) {
        when (event) {
            is AGMLEvent.SendMessage -> sendMessage(event.text)
            is AGMLEvent.SetTopic -> agmlService.setTopic(event.topic)
            is AGMLEvent.SetPredicate -> agmlService.setPredicate(event.name, event.value)
            is AGMLEvent.ClearHistory -> clearHistory()
            is AGMLEvent.RequestSelfReflection -> requestSelfReflection()
        }
    }
    
    private fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            try {
                agmlService.processInputFlow(text).collect { response ->
                    val entry = ConversationEntry(
                        userInput = text,
                        response = response.text,
                        emotionalState = response.emotionalState,
                        metaCognitiveLayer = response.metaCognitiveLayer
                    )
                    
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            currentResponse = response,
                            metaCognitiveLayer = response.metaCognitiveLayer,
                            conversationHistory = state.conversationHistory + entry
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
    
    private fun clearHistory() {
        _uiState.update { it.copy(conversationHistory = emptyList()) }
    }
    
    private fun requestSelfReflection() {
        viewModelScope.launch {
            sendMessage("Tell me about yourself and how you think")
        }
    }
    
    /**
     * Get Live2D expression based on current emotional state
     */
    fun getLive2DExpression(): String {
        return _uiState.value.emotionalState.toLive2DExpression()
    }
    
    /**
     * Get voice parameters for TTS
     */
    fun getVoiceParams(): Map<String, Any> {
        return _uiState.value.currentResponse?.toVoiceParams() ?: mapOf(
            "text" to "",
            "pitch" to 1.0f,
            "speed" to 1.0f,
            "emotion" to "neutral"
        )
    }
    
    /**
     * Get meta-cognitive layer description for UI
     */
    fun getLayerDescription(): String {
        return _uiState.value.metaCognitiveLayer.description
    }
    
    /**
     * Get self-image summary for UI
     */
    fun getSelfImageSummary(): String {
        val selfImage = _uiState.value.selfImage
        val identity = selfImage["identity"]?.value ?: "AI Assistant"
        val purpose = selfImage["purpose"]?.value ?: "Conversation"
        val interactions = selfImage["interaction_count"]?.value ?: "0"
        
        return "$identity | Purpose: $purpose | Interactions: $interactions"
    }
}

/**
 * Factory for creating AGMLViewModel
 */
class AGMLViewModelFactory(
    private val agmlService: AGMLService
) {
    fun create(): AGMLViewModel {
        return AGMLViewModel(agmlService)
    }
}
