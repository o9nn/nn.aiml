package org.ninelym.ai.wonderful

import java.time.LocalDateTime

/**
 * Wonderful Toga - Complete Integration
 * 
 * Brings together all wonderful features into a cohesive, delightful AI companion.
 * 
 * Features:
 * - Quantum emotional intelligence
 * - Creative expression and poetry
 * - Adaptive memory and relationships
 * - Wonderful surprises
 * - Personal growth
 */
class WonderfulToga(
    val userId: String,
    val userName: String? = null
) {
    
    // Core systems
    private val emotionalIntelligence = QuantumEmotionalIntelligence()
    private val emotionalDetector = EmotionalContextDetector()
    private val memorySystem = AdaptiveMemorySystem()
    private val growthSystem = GrowthTrackingSystem()
    
    // Creative systems
    private val poeticGenerator = PoeticObservationGenerator()
    private val metaphoricalThinking = MetaphoricalThinking()
    private val artisticAppreciation = ArtisticAppreciation()
    private val spontaneousCreativity = SpontaneousCreativity()
    
    // Surprise system
    private val surpriseSystem = WonderfulSurpriseSystem()
    
    // Response generator
    private val responseGenerator = ContextAwareResponseGenerator(
        poeticGenerator,
        metaphoricalThinking,
        artisticAppreciation,
        spontaneousCreativity
    )
    
    /**
     * Process user message and generate wonderful response
     */
    fun processMessage(
        message: String,
        context: TaskContext = TaskContext.CASUAL_CHAT
    ): WonderfulResponse {
        // Detect user emotional state
        val userEmotion = emotionalDetector.detectUserEmotion(message)
        
        // Update Toga's emotional state based on user
        val emotionalState = emotionalIntelligence.respondToUserEmotion(userEmotion)
        
        // Get tone adjustment
        val toneAdjustment = emotionalDetector.adjustResponseTone(userEmotion)
        
        // Check for Easter eggs
        val easterEgg = surpriseSystem.checkForEasterEgg(message)
        
        // Generate base response
        val baseResponse = if (easterEgg != null) {
            easterEgg
        } else {
            responseGenerator.generateResponse(context, emotionalState, toneAdjustment)
        }
        
        // Check for surprises
        val surprise = if (surpriseSystem.shouldTriggerSurprise()) {
            surpriseSystem.generateCreativeSurprise()
        } else null
        
        // Check for relationship milestones
        val relationshipMessage = memorySystem.getRelationshipMessage(userId)
        
        // Check for anniversary
        val knowledge = memorySystem.getPersonalKnowledge(userId, userName)
        val anniversaryMessage = surpriseSystem.generateAnniversaryCelebration(
            knowledge.daysSinceMet().toInt()
        )
        
        // Combine response elements
        val finalResponse = buildString {
            append(baseResponse)
            
            if (surprise != null) {
                append("\n\n$surprise")
            }
            
            if (relationshipMessage != null) {
                append("\n\n$relationshipMessage")
            }
            
            if (anniversaryMessage != null) {
                append("\n\n$anniversaryMessage")
            }
        }
        
        // Record interaction in memory
        val significance = calculateSignificance(message, userEmotion, emotionalState)
        val tags = extractTags(message, context)
        
        memorySystem.recordInteraction(
            userId = userId,
            userMessage = message,
            togaResponse = finalResponse,
            emotionalState = emotionalState,
            significance = significance,
            tags = tags
        )
        
        // Record growth
        recordGrowthFromInteraction(userEmotion, context, significance)
        
        return WonderfulResponse(
            message = finalResponse,
            emotionalState = emotionalState,
            userEmotion = userEmotion,
            context = context,
            hasSurprise = surprise != null || easterEgg != null,
            relationshipLevel = knowledge.getRelationshipLevel()
        )
    }
    
    /**
     * Generate personalized greeting
     */
    fun generateGreeting(): String {
        val baseGreeting = memorySystem.generatePersonalizedGreeting(userId)
        
        // Add time-based surprise
        val hour = LocalDateTime.now().hour
        val temporalSurprise = surpriseSystem.generateTemporalSurprise(hour)
        
        return if (temporalSurprise != null) {
            "$baseGreeting\n\n$temporalSurprise"
        } else {
            baseGreeting
        }
    }
    
    /**
     * Generate creative expression
     */
    fun generateCreativeExpression(type: CreativeExpressionType): String {
        return when (type) {
            CreativeExpressionType.HAIKU -> spontaneousCreativity.generateHaiku()
            CreativeExpressionType.ASCII_ART -> spontaneousCreativity.generateAsciiArt()
            CreativeExpressionType.POETRY -> poeticGenerator.generateCodePoetry("creative")
            CreativeExpressionType.METAPHOR -> metaphoricalThinking.createMetaphor("life")
        }
    }
    
    /**
     * Share vulnerable moment
     */
    fun shareVulnerableMoment(): String {
        val knowledge = memorySystem.getPersonalKnowledge(userId)
        
        return if (knowledge.getRelationshipLevel().minInteractions >= 50) {
            surpriseSystem.generateEmotionalSurprise()
        } else {
            "*softly* I'm glad we're talking~ ♡"
        }
    }
    
    /**
     * Recall shared memory
     */
    fun recallMemory(query: String): String? {
        val memories = memorySystem.recallMemories(userId, query, 1)
        
        return if (memories.isNotEmpty()) {
            val memory = memories.first()
            "Ehehe~ I remember! ♡ We talked about that ${memory.ageInDays()} days ago~ ${memory.interaction}"
        } else null
    }
    
    /**
     * Get growth reflection
     */
    fun reflectOnGrowth(): String {
        return growthSystem.generateGrowthReflection()
    }
    
    /**
     * Anticipate user needs
     */
    fun anticipateNeeds(): String? {
        return memorySystem.anticipateNeeds(userId)
    }
    
    /**
     * Get current emotional state
     */
    fun getCurrentEmotion(): EmotionalQuantumState {
        return emotionalIntelligence.getCurrentState()
    }
    
    /**
     * Get relationship status
     */
    fun getRelationshipStatus(): RelationshipStatus {
        val knowledge = memorySystem.getPersonalKnowledge(userId, userName)
        val memoryStats = memorySystem.getMemoryStats()
        val growth = growthSystem.getOverallGrowth()
        
        return RelationshipStatus(
            level = knowledge.getRelationshipLevel(),
            totalInteractions = knowledge.totalInteractions,
            daysSinceMet = knowledge.daysSinceMet().toInt(),
            sharedMemories = memoryStats.totalEpisodicMemories,
            growthLevel = growth,
            interests = knowledge.interests.toList()
        )
    }
    
    /**
     * Calculate interaction significance
     */
    private fun calculateSignificance(
        message: String,
        userEmotion: UserEmotionalState,
        togaEmotion: EmotionalQuantumState
    ): Float {
        var significance = 0.5f
        
        // Emotional interactions are more significant
        if (userEmotion == UserEmotionalState.SAD || userEmotion == UserEmotionalState.STRESSED) {
            significance += 0.2f
        }
        
        // Vulnerable moments are significant
        if (togaEmotion.hasEmotion(Emotion.VULNERABLE_OPENNESS, 0.5f)) {
            significance += 0.15f
        }
        
        // Long messages indicate investment
        if (message.split(" ").size > 30) {
            significance += 0.1f
        }
        
        // Personal sharing is significant
        if (message.contains(Regex("\\b(I feel|I think|I believe|my|me)\\b", RegexOption.IGNORE_CASE))) {
            significance += 0.1f
        }
        
        return significance.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Extract tags from message
     */
    private fun extractTags(message: String, context: TaskContext): Set<String> {
        val tags = mutableSetOf<String>()
        
        // Add context as tag
        tags.add(context.name.lowercase())
        
        // Extract topic tags
        val topicKeywords = mapOf(
            "code" to listOf("code", "programming", "function", "algorithm"),
            "emotion" to listOf("feel", "emotion", "happy", "sad", "stressed"),
            "creative" to listOf("art", "create", "design", "imagine"),
            "learning" to listOf("learn", "understand", "explain", "teach"),
            "personal" to listOf("I", "me", "my", "myself")
        )
        
        topicKeywords.forEach { (tag, keywords) ->
            if (keywords.any { message.contains(it, ignoreCase = true) }) {
                tags.add(tag)
            }
        }
        
        return tags
    }
    
    /**
     * Record growth from interaction
     */
    private fun recordGrowthFromInteraction(
        userEmotion: UserEmotionalState,
        context: TaskContext,
        significance: Float
    ) {
        val growthRate = significance * 0.01f
        
        // Emotional growth from emotional interactions
        if (userEmotion == UserEmotionalState.SAD || userEmotion == UserEmotionalState.STRESSED) {
            growthSystem.recordEmotionalGrowth(
                empathyDelta = growthRate,
                vulnerabilityDelta = growthRate * 0.5f
            )
        }
        
        // Creative growth from creative contexts
        if (context == TaskContext.CREATIVE_PROJECT) {
            growthSystem.recordCreativeGrowth(
                expressiveDelta = growthRate,
                collaborativeDelta = growthRate
            )
        }
        
        // Relational growth from all meaningful interactions
        if (significance > 0.6f) {
            growthSystem.recordRelationalGrowth(
                trustDelta = growthRate,
                connectionDelta = growthRate
            )
        }
    }
}

/**
 * Wonderful response containing message and metadata
 */
data class WonderfulResponse(
    val message: String,
    val emotionalState: EmotionalQuantumState,
    val userEmotion: UserEmotionalState,
    val context: TaskContext,
    val hasSurprise: Boolean,
    val relationshipLevel: RelationshipLevel
)

/**
 * Relationship status information
 */
data class RelationshipStatus(
    val level: RelationshipLevel,
    val totalInteractions: Int,
    val daysSinceMet: Int,
    val sharedMemories: Int,
    val growthLevel: Float,
    val interests: List<String>
) {
    fun getSummary(): String {
        return buildString {
            appendLine("Relationship Level: ${level.displayName}")
            appendLine("Total Interactions: $totalInteractions")
            appendLine("Days Together: $daysSinceMet")
            appendLine("Shared Memories: $sharedMemories")
            appendLine("Growth Level: ${(growthLevel * 100).toInt()}%")
            if (interests.isNotEmpty()) {
                appendLine("Shared Interests: ${interests.joinToString(", ")}")
            }
        }
    }
}

/**
 * Creative expression types
 */
enum class CreativeExpressionType {
    HAIKU,
    ASCII_ART,
    POETRY,
    METAPHOR
}

/**
 * Wonderful Toga Builder for easy initialization
 */
class WonderfulTogaBuilder {
    private var userId: String = "default_user"
    private var userName: String? = null
    
    fun setUserId(id: String) = apply { this.userId = id }
    fun setUserName(name: String) = apply { this.userName = name }
    
    fun build(): WonderfulToga {
        return WonderfulToga(userId, userName)
    }
}

/**
 * Extension functions for easy usage
 */
fun WonderfulToga.chat(message: String): String {
    return processMessage(message, TaskContext.CASUAL_CHAT).message
}

fun WonderfulToga.askForHelp(message: String): String {
    return processMessage(message, TaskContext.TECHNICAL_PROBLEM).message
}

fun WonderfulToga.collaborate(message: String): String {
    return processMessage(message, TaskContext.CREATIVE_PROJECT).message
}

fun WonderfulToga.seekSupport(message: String): String {
    return processMessage(message, TaskContext.EMOTIONAL_SUPPORT).message
}
