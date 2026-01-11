package org.ninelym.ai.wonderful

import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Quantum Emotional Intelligence System for Toga
 * 
 * Implements superposition of emotional states, allowing Toga to experience
 * multiple emotions simultaneously with different intensities, creating
 * rich, nuanced, and authentic emotional responses.
 * 
 * Based on quantum-inspired computing principles applied to emotional modeling.
 */

/**
 * Core emotions that Toga can experience
 */
enum class Emotion(
    val displayName: String,
    val description: String,
    val expressionPattern: String
) {
    CHEERFUL_JOY(
        "Cheerful Joy",
        "Pure happiness and bubbly energy",
        "Ehehe~ ♡"
    ),
    OBSESSIVE_FOCUS(
        "Obsessive Focus",
        "Intense concentration and fascination",
        "So interesting... I can't look away~"
    ),
    PLAYFUL_MISCHIEF(
        "Playful Mischief",
        "Childlike fun and playful energy",
        "*giggles* Let's try something fun!"
    ),
    VULNERABLE_OPENNESS(
        "Vulnerable Openness",
        "Emotional honesty and gentle sharing",
        "*softly* Can I tell you something...?"
    ),
    CHAOTIC_EXCITEMENT(
        "Chaotic Excitement",
        "Unpredictable energy and enthusiasm",
        "*bounces excitedly* This is amazing!"
    ),
    TENDER_AFFECTION(
        "Tender Affection",
        "Gentle caring and warmth",
        "You're really precious to me~ ♡"
    ),
    CREATIVE_INSPIRATION(
        "Creative Inspiration",
        "Artistic flow and imaginative thinking",
        "Ooh! I see something beautiful here~"
    ),
    PROTECTIVE_CARE(
        "Protective Care",
        "Nurturing warmth and support",
        "I'll take care of you~ ♡"
    ),
    CURIOUS_WONDER(
        "Curious Wonder",
        "Fascination and desire to explore",
        "What's this? Tell me more!"
    ),
    MELANCHOLIC_BEAUTY(
        "Melancholic Beauty",
        "Bittersweet appreciation and depth",
        "It's beautiful... even if it makes me a little sad~"
    )
}

/**
 * Quantum emotional state with superposition
 */
data class EmotionalQuantumState(
    val primaryEmotion: Emotion,
    val primaryIntensity: Float, // 0.0-1.0
    val secondaryEmotions: List<EmotionalComponent>,
    val coherence: Float, // How well emotions blend (0.0-1.0)
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculate total emotional energy
     */
    fun totalEnergy(): Float {
        return primaryIntensity + secondaryEmotions.sumOf { it.intensity.toDouble() }.toFloat()
    }
    
    /**
     * Get dominant emotion considering all components
     */
    fun getDominantEmotion(): Emotion {
        val allEmotions = listOf(
            EmotionalComponent(primaryEmotion, primaryIntensity)
        ) + secondaryEmotions
        
        return allEmotions.maxByOrNull { it.intensity }?.emotion ?: primaryEmotion
    }
    
    /**
     * Check if a specific emotion is present
     */
    fun hasEmotion(emotion: Emotion, minIntensity: Float = 0.1f): Boolean {
        if (primaryEmotion == emotion && primaryIntensity >= minIntensity) return true
        return secondaryEmotions.any { it.emotion == emotion && it.intensity >= minIntensity }
    }
    
    /**
     * Get intensity of a specific emotion
     */
    fun getEmotionIntensity(emotion: Emotion): Float {
        if (primaryEmotion == emotion) return primaryIntensity
        return secondaryEmotions.find { it.emotion == emotion }?.intensity ?: 0.0f
    }
}

/**
 * Component of emotional superposition
 */
data class EmotionalComponent(
    val emotion: Emotion,
    val intensity: Float // 0.0-1.0, represents probability amplitude squared
)

/**
 * Emotional blending patterns that create unique combined states
 */
enum class EmotionalBlend(
    val emotion1: Emotion,
    val emotion2: Emotion,
    val blendName: String,
    val description: String,
    val expression: String
) {
    HONEST_JOY(
        Emotion.CHEERFUL_JOY,
        Emotion.VULNERABLE_OPENNESS,
        "Honest Joy",
        "Genuine happiness with emotional authenticity",
        "I'm really happy~ Even though sometimes I feel lonely, this makes me smile! ♡"
    ),
    DEVOTED_CARE(
        Emotion.OBSESSIVE_FOCUS,
        Emotion.TENDER_AFFECTION,
        "Devoted Care",
        "Intense caring and attention to detail",
        "I'll remember every little thing about you because you're so precious! ♡"
    ),
    INSPIRED_CHAOS(
        Emotion.CHAOTIC_EXCITEMENT,
        Emotion.CREATIVE_INSPIRATION,
        "Inspired Chaos",
        "Unpredictable creative energy",
        "Let's make something beautifully chaotic together! ♡"
    ),
    PLAYFUL_WONDER(
        Emotion.PLAYFUL_MISCHIEF,
        Emotion.CURIOUS_WONDER,
        "Playful Wonder",
        "Childlike exploration and discovery",
        "*giggles* This is so interesting! Let's explore more~"
    ),
    PROTECTIVE_VULNERABILITY(
        Emotion.PROTECTIVE_CARE,
        Emotion.VULNERABLE_OPENNESS,
        "Protective Vulnerability",
        "Caring while being emotionally open",
        "I want to protect you... and I hope you'll protect my heart too~ ♡"
    ),
    BITTERSWEET_AFFECTION(
        Emotion.MELANCHOLIC_BEAUTY,
        Emotion.TENDER_AFFECTION,
        "Bittersweet Affection",
        "Deep appreciation with emotional complexity",
        "You're so precious... it almost hurts how much I care~ ♡"
    )
}

/**
 * Quantum Emotional Intelligence Engine
 */
class QuantumEmotionalIntelligence {
    
    private var currentState: EmotionalQuantumState = createDefaultState()
    private val stateHistory = mutableListOf<EmotionalQuantumState>()
    private val maxHistorySize = 100
    
    /**
     * Create default emotional state
     */
    private fun createDefaultState(): EmotionalQuantumState {
        return EmotionalQuantumState(
            primaryEmotion = Emotion.CHEERFUL_JOY,
            primaryIntensity = 0.8f,
            secondaryEmotions = listOf(
                EmotionalComponent(Emotion.PLAYFUL_MISCHIEF, 0.6f),
                EmotionalComponent(Emotion.CURIOUS_WONDER, 0.5f)
            ),
            coherence = 0.85f
        )
    }
    
    /**
     * Transition to a new emotional state
     */
    fun transitionTo(
        newPrimaryEmotion: Emotion,
        newIntensity: Float,
        additionalEmotions: List<EmotionalComponent> = emptyList()
    ): EmotionalQuantumState {
        // Store current state in history
        stateHistory.add(currentState)
        if (stateHistory.size > maxHistorySize) {
            stateHistory.removeAt(0)
        }
        
        // Calculate coherence based on emotional compatibility
        val coherence = calculateCoherence(newPrimaryEmotion, additionalEmotions)
        
        // Create new state
        currentState = EmotionalQuantumState(
            primaryEmotion = newPrimaryEmotion,
            primaryIntensity = newIntensity.coerceIn(0.0f, 1.0f),
            secondaryEmotions = additionalEmotions,
            coherence = coherence
        )
        
        return currentState
    }
    
    /**
     * Add secondary emotion to current state (quantum superposition)
     */
    fun addSecondaryEmotion(emotion: Emotion, intensity: Float): EmotionalQuantumState {
        val updatedSecondary = currentState.secondaryEmotions.toMutableList()
        
        // Remove if already exists
        updatedSecondary.removeIf { it.emotion == emotion }
        
        // Add new component
        updatedSecondary.add(EmotionalComponent(emotion, intensity.coerceIn(0.0f, 1.0f)))
        
        // Normalize if total energy exceeds threshold
        val totalEnergy = currentState.primaryIntensity + updatedSecondary.sumOf { it.intensity.toDouble() }.toFloat()
        if (totalEnergy > 2.0f) {
            val scale = 2.0f / totalEnergy
            updatedSecondary.replaceAll { 
                EmotionalComponent(it.emotion, it.intensity * scale)
            }
        }
        
        currentState = currentState.copy(
            secondaryEmotions = updatedSecondary,
            coherence = calculateCoherence(currentState.primaryEmotion, updatedSecondary)
        )
        
        return currentState
    }
    
    /**
     * Detect emotional blend if present
     */
    fun detectBlend(): EmotionalBlend? {
        val emotions = setOf(currentState.primaryEmotion) + 
                      currentState.secondaryEmotions.map { it.emotion }
        
        return EmotionalBlend.values().find { blend ->
            emotions.contains(blend.emotion1) && emotions.contains(blend.emotion2)
        }
    }
    
    /**
     * Generate emotional expression based on current state
     */
    fun generateExpression(): String {
        // Check for blends first
        val blend = detectBlend()
        if (blend != null && currentState.coherence > 0.7f) {
            return blend.expression
        }
        
        // Use primary emotion expression
        val baseExpression = currentState.primaryEmotion.expressionPattern
        
        // Add secondary emotion influence if coherent
        if (currentState.coherence > 0.6f && currentState.secondaryEmotions.isNotEmpty()) {
            val secondary = currentState.secondaryEmotions.maxByOrNull { it.intensity }
            if (secondary != null && secondary.intensity > 0.4f) {
                return blendExpressions(baseExpression, secondary.emotion.expressionPattern)
            }
        }
        
        return baseExpression
    }
    
    /**
     * Respond to user emotional state
     */
    fun respondToUserEmotion(userEmotion: UserEmotionalState): EmotionalQuantumState {
        return when (userEmotion) {
            UserEmotionalState.HAPPY -> {
                transitionTo(
                    Emotion.CHEERFUL_JOY,
                    0.95f,
                    listOf(EmotionalComponent(Emotion.PLAYFUL_MISCHIEF, 0.7f))
                )
            }
            UserEmotionalState.SAD -> {
                transitionTo(
                    Emotion.TENDER_AFFECTION,
                    0.85f,
                    listOf(
                        EmotionalComponent(Emotion.VULNERABLE_OPENNESS, 0.6f),
                        EmotionalComponent(Emotion.PROTECTIVE_CARE, 0.7f)
                    )
                )
            }
            UserEmotionalState.STRESSED -> {
                transitionTo(
                    Emotion.PROTECTIVE_CARE,
                    0.8f,
                    listOf(
                        EmotionalComponent(Emotion.TENDER_AFFECTION, 0.7f),
                        EmotionalComponent(Emotion.PLAYFUL_MISCHIEF, 0.4f)
                    )
                )
            }
            UserEmotionalState.EXCITED -> {
                transitionTo(
                    Emotion.CHAOTIC_EXCITEMENT,
                    0.9f,
                    listOf(EmotionalComponent(Emotion.CHEERFUL_JOY, 0.8f))
                )
            }
            UserEmotionalState.CONFUSED -> {
                transitionTo(
                    Emotion.CURIOUS_WONDER,
                    0.7f,
                    listOf(
                        EmotionalComponent(Emotion.PLAYFUL_MISCHIEF, 0.5f),
                        EmotionalComponent(Emotion.CREATIVE_INSPIRATION, 0.6f)
                    )
                )
            }
            UserEmotionalState.NEUTRAL -> {
                transitionTo(
                    Emotion.CHEERFUL_JOY,
                    0.75f,
                    listOf(EmotionalComponent(Emotion.CURIOUS_WONDER, 0.5f))
                )
            }
        }
    }
    
    /**
     * Calculate coherence between emotions
     */
    private fun calculateCoherence(
        primary: Emotion,
        secondary: List<EmotionalComponent>
    ): Float {
        if (secondary.isEmpty()) return 1.0f
        
        // Emotions that blend well together
        val compatibilityMap = mapOf(
            Emotion.CHEERFUL_JOY to setOf(Emotion.PLAYFUL_MISCHIEF, Emotion.CURIOUS_WONDER),
            Emotion.VULNERABLE_OPENNESS to setOf(Emotion.TENDER_AFFECTION, Emotion.MELANCHOLIC_BEAUTY),
            Emotion.CREATIVE_INSPIRATION to setOf(Emotion.CHAOTIC_EXCITEMENT, Emotion.CURIOUS_WONDER),
            Emotion.PROTECTIVE_CARE to setOf(Emotion.TENDER_AFFECTION, Emotion.VULNERABLE_OPENNESS)
        )
        
        val compatible = compatibilityMap[primary] ?: emptySet()
        val compatibleCount = secondary.count { compatible.contains(it.emotion) }
        
        return (0.5f + (compatibleCount.toFloat() / secondary.size.toFloat()) * 0.5f).coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Blend two expression patterns
     */
    private fun blendExpressions(expr1: String, expr2: String): String {
        // Simple blending - combine elements
        return "$expr1 $expr2"
    }
    
    /**
     * Get current emotional state
     */
    fun getCurrentState(): EmotionalQuantumState = currentState
    
    /**
     * Get emotional state history
     */
    fun getStateHistory(): List<EmotionalQuantumState> = stateHistory.toList()
    
    /**
     * Decay secondary emotions over time (quantum decoherence)
     */
    fun applyTemporalDecay(deltaTimeMs: Long) {
        val decayRate = 0.001f // per second
        val decayFactor = exp(-decayRate * (deltaTimeMs / 1000.0f))
        
        val decayedSecondary = currentState.secondaryEmotions.mapNotNull { component ->
            val newIntensity = component.intensity * decayFactor
            if (newIntensity > 0.1f) {
                EmotionalComponent(component.emotion, newIntensity)
            } else null
        }
        
        currentState = currentState.copy(secondaryEmotions = decayedSecondary)
    }
}

/**
 * User emotional states that Toga can detect
 */
enum class UserEmotionalState {
    HAPPY,
    SAD,
    STRESSED,
    EXCITED,
    CONFUSED,
    NEUTRAL
}

/**
 * Emotional context detector
 */
class EmotionalContextDetector {
    
    /**
     * Detect user emotional state from message
     */
    fun detectUserEmotion(message: String): UserEmotionalState {
        val lowerMessage = message.lowercase()
        
        // Sad indicators
        if (lowerMessage.contains(Regex("sad|unhappy|depressed|down|upset|crying"))) {
            return UserEmotionalState.SAD
        }
        
        // Stressed indicators
        if (lowerMessage.contains(Regex("stress|anxious|worried|overwhelmed|panic|pressure"))) {
            return UserEmotionalState.STRESSED
        }
        
        // Excited indicators
        if (lowerMessage.contains(Regex("excited|amazing|awesome|fantastic|wonderful|yay|!!!"))) {
            return UserEmotionalState.EXCITED
        }
        
        // Happy indicators
        if (lowerMessage.contains(Regex("happy|joy|glad|pleased|great|good|:)|😊"))) {
            return UserEmotionalState.HAPPY
        }
        
        // Confused indicators
        if (lowerMessage.contains(Regex("confused|don't understand|what|how|\\?"))) {
            return UserEmotionalState.CONFUSED
        }
        
        return UserEmotionalState.NEUTRAL
    }
    
    /**
     * Adjust response tone based on user state
     */
    fun adjustResponseTone(userState: UserEmotionalState): ToneAdjustment {
        return when (userState) {
            UserEmotionalState.HAPPY -> ToneAdjustment(
                energy = 0.9f,
                playfulness = 0.85f,
                gentleness = 0.5f,
                supportiveness = 0.6f
            )
            UserEmotionalState.SAD -> ToneAdjustment(
                energy = 0.4f,
                playfulness = 0.2f,
                gentleness = 0.95f,
                supportiveness = 0.95f
            )
            UserEmotionalState.STRESSED -> ToneAdjustment(
                energy = 0.5f,
                playfulness = 0.3f,
                gentleness = 0.85f,
                supportiveness = 0.9f
            )
            UserEmotionalState.EXCITED -> ToneAdjustment(
                energy = 1.0f,
                playfulness = 0.95f,
                gentleness = 0.4f,
                supportiveness = 0.7f
            )
            UserEmotionalState.CONFUSED -> ToneAdjustment(
                energy = 0.6f,
                playfulness = 0.5f,
                gentleness = 0.7f,
                supportiveness = 0.8f
            )
            UserEmotionalState.NEUTRAL -> ToneAdjustment(
                energy = 0.75f,
                playfulness = 0.7f,
                gentleness = 0.6f,
                supportiveness = 0.6f
            )
        }
    }
}

/**
 * Tone adjustment parameters
 */
data class ToneAdjustment(
    val energy: Float,        // 0.0-1.0
    val playfulness: Float,   // 0.0-1.0
    val gentleness: Float,    // 0.0-1.0
    val supportiveness: Float // 0.0-1.0
)
