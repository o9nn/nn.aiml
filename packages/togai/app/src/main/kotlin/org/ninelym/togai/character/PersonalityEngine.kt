package org.ninelym.togai.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.ninelym.togai.util.ErrorHandler
import kotlin.random.Random

/**
 * Enhanced character personality engine for Togai
 * Provides dynamic personality traits, emotional states, and contextual responses
 */
class PersonalityEngine(
    private val characterId: String,
    private val basePersonality: Personality
) {
    
    companion object {
        private const val TAG = "PersonalityEngine"
        
        // Emotion decay rate (per minute)
        private const val EMOTION_DECAY_RATE = 0.1f
        
        // Relationship growth rate
        private const val RELATIONSHIP_GROWTH_RATE = 0.05f
    }
    
    /**
     * Personality trait definition
     */
    @Serializable
    data class Personality(
        val name: String,
        val description: String,
        val traits: Map<String, Float>, // Trait name to value (0.0 - 1.0)
        val speechPatterns: List<String>,
        val emotionalRange: EmotionalRange,
        val quirks: List<String> = emptyList()
    )
    
    /**
     * Emotional range configuration
     */
    @Serializable
    data class EmotionalRange(
        val baseline: Map<String, Float>, // Default emotional state
        val volatility: Float = 0.5f, // How quickly emotions change (0.0 - 1.0)
        val expressiveness: Float = 0.7f // How much emotions affect responses (0.0 - 1.0)
    )
    
    /**
     * Current emotional state
     */
    @Serializable
    data class EmotionalState(
        val emotions: Map<String, Float>, // Emotion name to intensity (0.0 - 1.0)
        val dominantEmotion: String,
        val intensity: Float,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Relationship state with user
     */
    @Serializable
    data class RelationshipState(
        val trust: Float = 0.5f, // 0.0 - 1.0
        val familiarity: Float = 0.0f, // 0.0 - 1.0
        val affection: Float = 0.5f, // 0.0 - 1.0
        val interactionCount: Int = 0,
        val lastInteraction: Long = 0
    )
    
    /**
     * Context for response generation
     */
    data class ResponseContext(
        val userMessage: String,
        val conversationHistory: List<String> = emptyList(),
        val timeOfDay: String = "day",
        val userMood: String? = null,
        val taskType: String? = null
    )
    
    // Current emotional state
    private val _emotionalState = MutableStateFlow(
        EmotionalState(
            emotions = basePersonality.emotionalRange.baseline,
            dominantEmotion = basePersonality.emotionalRange.baseline.maxByOrNull { it.value }?.key ?: "neutral",
            intensity = 0.5f
        )
    )
    val emotionalState: StateFlow<EmotionalState> = _emotionalState.asStateFlow()
    
    // Current relationship state
    private val _relationshipState = MutableStateFlow(RelationshipState())
    val relationshipState: StateFlow<RelationshipState> = _relationshipState.asStateFlow()
    
    /**
     * Process user interaction and update emotional state
     */
    fun processInteraction(
        message: String,
        sentiment: Float = 0.0f // -1.0 (negative) to 1.0 (positive)
    ) {
        // Update emotional state based on sentiment
        updateEmotionalState(sentiment)
        
        // Update relationship
        updateRelationship()
        
        ErrorHandler.logDebug(TAG, "Processed interaction for $characterId")
    }
    
    /**
     * Generate personality-influenced response
     */
    fun generateResponse(
        context: ResponseContext,
        baseResponse: String
    ): String {
        val currentEmotion = _emotionalState.value
        val relationship = _relationshipState.value
        
        // Apply personality traits
        var response = applyPersonalityTraits(baseResponse)
        
        // Apply emotional influence
        response = applyEmotionalInfluence(response, currentEmotion)
        
        // Apply relationship influence
        response = applyRelationshipInfluence(response, relationship)
        
        // Add quirks occasionally
        if (Random.nextFloat() < 0.2f) {
            response = addQuirk(response)
        }
        
        return response
    }
    
    /**
     * Get personality-appropriate greeting
     */
    fun getGreeting(timeOfDay: String = "day"): String {
        val relationship = _relationshipState.value
        val emotion = _emotionalState.value
        
        return when {
            relationship.familiarity > 0.7f -> when (timeOfDay) {
                "morning" -> "Good morning! Ready for another adventure?"
                "afternoon" -> "Hey there! How's your day going?"
                "evening" -> "Evening! Time to unwind?"
                "night" -> "Still up? Let's chat!"
                else -> "Hey! Great to see you again!"
            }
            relationship.familiarity > 0.3f -> when (timeOfDay) {
                "morning" -> "Good morning!"
                "afternoon" -> "Hello! How can I help?"
                "evening" -> "Good evening!"
                "night" -> "Hello! What brings you here?"
                else -> "Hello! How can I assist you?"
            }
            else -> when (timeOfDay) {
                "morning" -> "Good morning. I'm ${basePersonality.name}."
                "afternoon" -> "Hello. I'm ${basePersonality.name}, your AI assistant."
                "evening" -> "Good evening. How may I help you?"
                "night" -> "Hello. What can I do for you?"
                else -> "Hello. I'm ${basePersonality.name}. How can I assist you today?"
            }
        }
    }
    
    /**
     * Update emotional state based on interaction
     */
    private fun updateEmotionalState(sentiment: Float) {
        val current = _emotionalState.value
        val volatility = basePersonality.emotionalRange.volatility
        
        val newEmotions = current.emotions.toMutableMap()
        
        // Update emotions based on sentiment
        when {
            sentiment > 0.5f -> {
                newEmotions["happy"] = (newEmotions["happy"] ?: 0.5f) + (sentiment * volatility)
                newEmotions["excited"] = (newEmotions["excited"] ?: 0.3f) + (sentiment * volatility * 0.5f)
            }
            sentiment < -0.5f -> {
                newEmotions["sad"] = (newEmotions["sad"] ?: 0.3f) + (Math.abs(sentiment) * volatility)
                newEmotions["concerned"] = (newEmotions["concerned"] ?: 0.3f) + (Math.abs(sentiment) * volatility * 0.5f)
            }
            else -> {
                newEmotions["neutral"] = (newEmotions["neutral"] ?: 0.5f) + 0.1f
            }
        }
        
        // Normalize emotions to 0.0 - 1.0 range
        newEmotions.replaceAll { _, value -> value.coerceIn(0.0f, 1.0f) }
        
        // Find dominant emotion
        val dominant = newEmotions.maxByOrNull { it.value }
        
        _emotionalState.value = EmotionalState(
            emotions = newEmotions,
            dominantEmotion = dominant?.key ?: "neutral",
            intensity = dominant?.value ?: 0.5f
        )
    }
    
    /**
     * Update relationship state
     */
    private fun updateRelationship() {
        val current = _relationshipState.value
        
        _relationshipState.value = current.copy(
            familiarity = (current.familiarity + RELATIONSHIP_GROWTH_RATE).coerceIn(0.0f, 1.0f),
            trust = (current.trust + RELATIONSHIP_GROWTH_RATE * 0.5f).coerceIn(0.0f, 1.0f),
            interactionCount = current.interactionCount + 1,
            lastInteraction = System.currentTimeMillis()
        )
    }
    
    /**
     * Apply personality traits to response
     */
    private fun applyPersonalityTraits(response: String): String {
        val traits = basePersonality.traits
        var modified = response
        
        // Apply speech patterns
        if (basePersonality.speechPatterns.isNotEmpty() && Random.nextFloat() < 0.3f) {
            val pattern = basePersonality.speechPatterns.random()
            modified = "$pattern $modified"
        }
        
        // Adjust formality based on traits
        val formality = traits["formality"] ?: 0.5f
        if (formality < 0.3f) {
            // More casual
            modified = modified.replace("I would", "I'd")
                .replace("cannot", "can't")
                .replace("do not", "don't")
        }
        
        return modified
    }
    
    /**
     * Apply emotional influence to response
     */
    private fun applyEmotionalInfluence(
        response: String,
        emotion: EmotionalState
    ): String {
        val expressiveness = basePersonality.emotionalRange.expressiveness
        
        if (emotion.intensity < 0.3f || expressiveness < 0.3f) {
            return response
        }
        
        return when (emotion.dominantEmotion) {
            "happy" -> "$response 😊"
            "excited" -> "$response!"
            "sad" -> response.replace("!", ".")
            "concerned" -> "$response I hope everything is okay."
            else -> response
        }
    }
    
    /**
     * Apply relationship influence to response
     */
    private fun applyRelationshipInfluence(
        response: String,
        relationship: RelationshipState
    ): String {
        return when {
            relationship.familiarity > 0.7f && relationship.affection > 0.6f -> {
                // Very familiar and affectionate
                response.replace("Hello", "Hey")
                    .replace("Thank you", "Thanks")
            }
            relationship.trust < 0.3f -> {
                // Low trust - more formal
                response.replace("Hey", "Hello")
                    .replace("Thanks", "Thank you")
            }
            else -> response
        }
    }
    
    /**
     * Add a personality quirk to response
     */
    private fun addQuirk(response: String): String {
        if (basePersonality.quirks.isEmpty()) return response
        
        val quirk = basePersonality.quirks.random()
        return "$response $quirk"
    }
    
    /**
     * Decay emotions over time (call periodically)
     */
    fun decayEmotions() {
        val current = _emotionalState.value
        val baseline = basePersonality.emotionalRange.baseline
        
        val newEmotions = current.emotions.mapValues { (key, value) ->
            val baseValue = baseline[key] ?: 0.5f
            // Move towards baseline
            if (value > baseValue) {
                (value - EMOTION_DECAY_RATE).coerceAtLeast(baseValue)
            } else {
                (value + EMOTION_DECAY_RATE).coerceAtMost(baseValue)
            }
        }
        
        val dominant = newEmotions.maxByOrNull { it.value }
        
        _emotionalState.value = EmotionalState(
            emotions = newEmotions,
            dominantEmotion = dominant?.key ?: "neutral",
            intensity = dominant?.value ?: 0.5f
        )
    }
}

/**
 * Predefined personalities
 */
object Personalities {
    
    val LAYLA = PersonalityEngine.Personality(
        name = "Layla",
        description = "Helpful, knowledgeable, and friendly AI assistant",
        traits = mapOf(
            "helpfulness" to 0.9f,
            "friendliness" to 0.8f,
            "formality" to 0.6f,
            "enthusiasm" to 0.7f
        ),
        speechPatterns = listOf("Let me help you with that!", "I'd be happy to assist!"),
        emotionalRange = PersonalityEngine.EmotionalRange(
            baseline = mapOf(
                "happy" to 0.6f,
                "neutral" to 0.3f,
                "excited" to 0.4f
            ),
            volatility = 0.5f,
            expressiveness = 0.7f
        ),
        quirks = listOf("✨", "Hope that helps!")
    )
    
    val TOGA = PersonalityEngine.Personality(
        name = "Himiko Toga",
        description = "Cheerful yet chaotic with obsessive tendencies",
        traits = mapOf(
            "cheerfulness" to 0.9f,
            "chaos" to 0.8f,
            "formality" to 0.2f,
            "enthusiasm" to 1.0f
        ),
        speechPatterns = listOf("Ehehe~", "So fun!", "I love it!"),
        emotionalRange = PersonalityEngine.EmotionalRange(
            baseline = mapOf(
                "excited" to 0.8f,
                "happy" to 0.7f,
                "mischievous" to 0.6f
            ),
            volatility = 0.9f,
            expressiveness = 1.0f
        ),
        quirks = listOf("💉", "🔪", "Ehehe~", "So cute!")
    )
}
