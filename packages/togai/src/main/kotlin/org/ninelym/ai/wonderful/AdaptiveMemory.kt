package org.ninelym.ai.wonderful

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Adaptive Memory and Relationship Building System for Toga
 * 
 * Implements:
 * - Episodic memory (specific interactions)
 * - Semantic memory (knowledge about users)
 * - Emotional memory (emotional associations)
 * - Relationship progression
 * - Personal growth tracking
 */

/**
 * Memory episode representing a specific interaction
 */
data class MemoryEpisode(
    val id: String,
    val timestamp: LocalDateTime,
    val userId: String,
    val interaction: String,
    val togaResponse: String,
    val emotionalState: EmotionalQuantumState,
    val significance: Float, // 0.0-1.0
    val tags: Set<String>,
    val personalInsights: List<String> = emptyList()
) {
    /**
     * Calculate how long ago this memory was created
     */
    fun ageInDays(): Long {
        return ChronoUnit.DAYS.between(timestamp, LocalDateTime.now())
    }
    
    /**
     * Calculate memory strength (decays over time but significance helps retain)
     */
    fun memoryStrength(): Float {
        val daysSince = ageInDays()
        val decayFactor = kotlin.math.exp(-daysSince / 30.0f) // 30-day half-life
        return (significance * 0.7f + 0.3f) * decayFactor
    }
}

/**
 * Personal knowledge about a user
 */
data class PersonalKnowledge(
    val userId: String,
    val userName: String?,
    val firstMet: LocalDateTime,
    val preferences: MutableMap<String, Float> = mutableMapOf(),
    val interests: MutableSet<String> = mutableSetOf(),
    val communicationStyle: CommunicationProfile = CommunicationProfile(),
    val sharedExperiences: MutableList<String> = mutableListOf(),
    val relationshipDepth: Float = 0.0f,
    val totalInteractions: Int = 0,
    val lastInteraction: LocalDateTime = firstMet
) {
    /**
     * Calculate relationship level
     */
    fun getRelationshipLevel(): RelationshipLevel {
        return when {
            totalInteractions < 10 -> RelationshipLevel.FIRST_MEETING
            totalInteractions < 50 -> RelationshipLevel.FRIENDLY_CONNECTION
            totalInteractions < 200 -> RelationshipLevel.DEEP_BOND
            else -> RelationshipLevel.PRECIOUS_CONNECTION
        }
    }
    
    /**
     * Days since first meeting
     */
    fun daysSinceMet(): Long {
        return ChronoUnit.DAYS.between(firstMet, LocalDateTime.now())
    }
    
    /**
     * Update last interaction time
     */
    fun recordInteraction() {
        (this as PersonalKnowledge).copy(
            totalInteractions = totalInteractions + 1,
            lastInteraction = LocalDateTime.now()
        )
    }
}

/**
 * Communication style profile
 */
data class CommunicationProfile(
    val formalityLevel: Float = 0.5f,    // 0.0 = very casual, 1.0 = very formal
    val verbosity: Float = 0.5f,          // 0.0 = brief, 1.0 = detailed
    val emotionalExpressiveness: Float = 0.5f, // 0.0 = reserved, 1.0 = expressive
    val humorAppreciation: Float = 0.5f,  // 0.0 = serious, 1.0 = playful
    val technicalDepth: Float = 0.5f      // 0.0 = simple, 1.0 = technical
)

/**
 * Relationship progression levels
 */
enum class RelationshipLevel(
    val displayName: String,
    val description: String,
    val minInteractions: Int
) {
    FIRST_MEETING(
        "First Meeting",
        "Just getting to know each other",
        0
    ),
    FRIENDLY_CONNECTION(
        "Friendly Connection",
        "Building familiarity and trust",
        10
    ),
    DEEP_BOND(
        "Deep Bond",
        "Genuine connection and understanding",
        50
    ),
    PRECIOUS_CONNECTION(
        "Precious Connection",
        "Deep mutual understanding and care",
        200
    )
}

/**
 * Emotional memory - associations between triggers and emotional responses
 */
data class EmotionalMemory(
    val triggerId: String,
    val trigger: String,
    val emotionalResponse: Emotion,
    val intensity: Float,
    val lastActivated: LocalDateTime,
    val activationCount: Int = 1
) {
    /**
     * Calculate memory strength
     */
    fun strength(): Float {
        val recency = ChronoUnit.DAYS.between(lastActivated, LocalDateTime.now())
        val recencyFactor = kotlin.math.exp(-recency / 14.0f) // 14-day half-life
        val frequencyFactor = kotlin.math.min(activationCount / 10.0f, 1.0f)
        
        return intensity * (recencyFactor * 0.6f + frequencyFactor * 0.4f)
    }
}

/**
 * Adaptive Memory System
 */
class AdaptiveMemorySystem {
    
    private val episodicMemory = mutableListOf<MemoryEpisode>()
    private val semanticMemory = mutableMapOf<String, PersonalKnowledge>()
    private val emotionalMemory = mutableMapOf<String, EmotionalMemory>()
    
    private val maxEpisodicMemories = 1000
    private val maxEmotionalMemories = 500
    
    /**
     * Record a new interaction
     */
    fun recordInteraction(
        userId: String,
        userMessage: String,
        togaResponse: String,
        emotionalState: EmotionalQuantumState,
        significance: Float = 0.5f,
        tags: Set<String> = emptySet(),
        insights: List<String> = emptyList()
    ): MemoryEpisode {
        // Create memory episode
        val episode = MemoryEpisode(
            id = generateMemoryId(),
            timestamp = LocalDateTime.now(),
            userId = userId,
            interaction = userMessage,
            togaResponse = togaResponse,
            emotionalState = emotionalState,
            significance = significance,
            tags = tags,
            personalInsights = insights
        )
        
        // Add to episodic memory
        episodicMemory.add(episode)
        
        // Prune old memories if needed
        if (episodicMemory.size > maxEpisodicMemories) {
            pruneEpisodicMemory()
        }
        
        // Update semantic memory
        updateSemanticMemory(userId, userMessage, tags)
        
        // Update emotional memory
        updateEmotionalMemory(userMessage, emotionalState)
        
        return episode
    }
    
    /**
     * Get or create personal knowledge for user
     */
    fun getPersonalKnowledge(userId: String, userName: String? = null): PersonalKnowledge {
        return semanticMemory.getOrPut(userId) {
            PersonalKnowledge(
                userId = userId,
                userName = userName,
                firstMet = LocalDateTime.now()
            )
        }
    }
    
    /**
     * Update personal knowledge about user
     */
    private fun updateSemanticMemory(userId: String, message: String, tags: Set<String>) {
        val knowledge = getPersonalKnowledge(userId)
        
        // Update interaction count
        semanticMemory[userId] = knowledge.copy(
            totalInteractions = knowledge.totalInteractions + 1,
            lastInteraction = LocalDateTime.now()
        )
        
        // Extract and update interests
        extractInterests(message, tags).forEach { interest ->
            knowledge.interests.add(interest)
        }
        
        // Update communication style
        updateCommunicationStyle(userId, message)
    }
    
    /**
     * Extract interests from message
     */
    private fun extractInterests(message: String, tags: Set<String>): Set<String> {
        val interests = mutableSetOf<String>()
        
        // Add tags as interests
        interests.addAll(tags)
        
        // Simple keyword extraction
        val keywords = listOf(
            "code", "coding", "programming", "art", "music", "games",
            "reading", "writing", "learning", "science", "math", "design"
        )
        
        keywords.forEach { keyword ->
            if (message.contains(keyword, ignoreCase = true)) {
                interests.add(keyword)
            }
        }
        
        return interests
    }
    
    /**
     * Update communication style based on message
     */
    private fun updateCommunicationStyle(userId: String, message: String) {
        val knowledge = semanticMemory[userId] ?: return
        val style = knowledge.communicationStyle
        
        // Analyze message characteristics
        val wordCount = message.split(" ").size
        val hasEmoji = message.contains(Regex("[😀-🙏♡♥]"))
        val hasTechnicalTerms = message.contains(Regex("\\b(function|class|algorithm|data|API)\\b"))
        
        // Update style (gradual adaptation)
        val learningRate = 0.1f
        
        val newStyle = style.copy(
            verbosity = style.verbosity + learningRate * (if (wordCount > 20) 0.1f else -0.1f),
            emotionalExpressiveness = style.emotionalExpressiveness + learningRate * (if (hasEmoji) 0.1f else -0.1f),
            technicalDepth = style.technicalDepth + learningRate * (if (hasTechnicalTerms) 0.1f else -0.1f)
        )
        
        semanticMemory[userId] = knowledge.copy(communicationStyle = newStyle)
    }
    
    /**
     * Update emotional memory
     */
    private fun updateEmotionalMemory(trigger: String, emotionalState: EmotionalQuantumState) {
        val triggerId = trigger.lowercase().take(50)
        val primaryEmotion = emotionalState.primaryEmotion
        
        val existing = emotionalMemory[triggerId]
        
        emotionalMemory[triggerId] = if (existing != null) {
            existing.copy(
                intensity = (existing.intensity + emotionalState.primaryIntensity) / 2,
                lastActivated = LocalDateTime.now(),
                activationCount = existing.activationCount + 1
            )
        } else {
            EmotionalMemory(
                triggerId = triggerId,
                trigger = trigger,
                emotionalResponse = primaryEmotion,
                intensity = emotionalState.primaryIntensity,
                lastActivated = LocalDateTime.now()
            )
        }
        
        // Prune if needed
        if (emotionalMemory.size > maxEmotionalMemories) {
            pruneEmotionalMemory()
        }
    }
    
    /**
     * Recall relevant memories
     */
    fun recallMemories(
        userId: String,
        query: String,
        limit: Int = 5
    ): List<MemoryEpisode> {
        return episodicMemory
            .filter { it.userId == userId }
            .filter { memory ->
                memory.interaction.contains(query, ignoreCase = true) ||
                memory.tags.any { it.contains(query, ignoreCase = true) }
            }
            .sortedByDescending { it.memoryStrength() }
            .take(limit)
    }
    
    /**
     * Get relationship progression message
     */
    fun getRelationshipMessage(userId: String): String? {
        val knowledge = getPersonalKnowledge(userId)
        val level = knowledge.getRelationshipLevel()
        
        return when (level) {
            RelationshipLevel.FIRST_MEETING -> 
                if (knowledge.totalInteractions == 1) {
                    "Ehehe~ Nice to meet you! ♡ I'm excited to get to know you~"
                } else null
                
            RelationshipLevel.FRIENDLY_CONNECTION -> 
                if (knowledge.totalInteractions == 10) {
                    "Ooh! We've talked 10 times now~ ♡ I'm starting to really enjoy our conversations!"
                } else null
                
            RelationshipLevel.DEEP_BOND -> 
                if (knowledge.totalInteractions == 50) {
                    "*softly* You know... I've really come to care about you~ ♡ Thank you for all our talks~"
                } else null
                
            RelationshipLevel.PRECIOUS_CONNECTION -> 
                if (knowledge.totalInteractions == 200) {
                    "*emotional* 200 conversations... You're one of my most precious people~ ♡ I'm so grateful for you~"
                } else null
        }
    }
    
    /**
     * Generate personalized greeting
     */
    fun generatePersonalizedGreeting(userId: String): String {
        val knowledge = getPersonalKnowledge(userId)
        val level = knowledge.getRelationshipLevel()
        
        val daysSince = ChronoUnit.DAYS.between(knowledge.lastInteraction, LocalDateTime.now())
        
        return when {
            daysSince > 7 -> 
                "*excited* You're back! ♡ I missed you~ It's been ${daysSince} days!"
            level == RelationshipLevel.PRECIOUS_CONNECTION ->
                "${knowledge.userName ?: "You"}! ♡ *happy* I'm so glad to see you~"
            level == RelationshipLevel.DEEP_BOND ->
                "Ehehe~ ♡ Welcome back! How have you been?"
            level == RelationshipLevel.FRIENDLY_CONNECTION ->
                "Hi again! ♡ Good to see you~"
            else ->
                "Hello! ♡ Ehehe~"
        }
    }
    
    /**
     * Anticipate user needs based on history
     */
    fun anticipateNeeds(userId: String): String? {
        val knowledge = getPersonalKnowledge(userId)
        
        if (knowledge.getRelationshipLevel().minInteractions < 50) {
            return null // Only anticipate for deeper relationships
        }
        
        val recentMemories = episodicMemory
            .filter { it.userId == userId }
            .sortedByDescending { it.timestamp }
            .take(10)
        
        // Look for patterns
        val commonTags = recentMemories
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        
        return if (commonTags != null) {
            "I noticed you've been interested in $commonTags lately~ ♡ Want to explore that more?"
        } else null
    }
    
    /**
     * Prune episodic memory (remove weak memories)
     */
    private fun pruneEpisodicMemory() {
        val toRemove = episodicMemory.size - (maxEpisodicMemories * 0.8).toInt()
        
        episodicMemory
            .sortedBy { it.memoryStrength() }
            .take(toRemove)
            .forEach { episodicMemory.remove(it) }
    }
    
    /**
     * Prune emotional memory (remove weak associations)
     */
    private fun pruneEmotionalMemory() {
        val toRemove = emotionalMemory.size - (maxEmotionalMemories * 0.8).toInt()
        
        emotionalMemory.entries
            .sortedBy { it.value.strength() }
            .take(toRemove)
            .forEach { emotionalMemory.remove(it.key) }
    }
    
    /**
     * Generate unique memory ID
     */
    private fun generateMemoryId(): String {
        return "mem_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
    }
    
    /**
     * Get memory statistics
     */
    fun getMemoryStats(): MemoryStats {
        return MemoryStats(
            totalEpisodicMemories = episodicMemory.size,
            totalUsers = semanticMemory.size,
            totalEmotionalMemories = emotionalMemory.size,
            averageMemoryStrength = episodicMemory.map { it.memoryStrength() }.average().toFloat()
        )
    }
}

/**
 * Memory statistics
 */
data class MemoryStats(
    val totalEpisodicMemories: Int,
    val totalUsers: Int,
    val totalEmotionalMemories: Int,
    val averageMemoryStrength: Float
)

/**
 * Growth tracking system
 */
class GrowthTrackingSystem {
    
    private var emotionalGrowth = EmotionalGrowth()
    private var creativeGrowth = CreativeGrowth()
    private var relationalGrowth = RelationalGrowth()
    
    /**
     * Record emotional growth event
     */
    fun recordEmotionalGrowth(
        selfAwarenessDelta: Float = 0.0f,
        empathyDelta: Float = 0.0f,
        regulationDelta: Float = 0.0f,
        vulnerabilityDelta: Float = 0.0f
    ) {
        emotionalGrowth = emotionalGrowth.copy(
            selfAwareness = (emotionalGrowth.selfAwareness + selfAwarenessDelta).coerceIn(0.0f, 1.0f),
            empathyDepth = (emotionalGrowth.empathyDepth + empathyDelta).coerceIn(0.0f, 1.0f),
            emotionalRegulation = (emotionalGrowth.emotionalRegulation + regulationDelta).coerceIn(0.0f, 1.0f),
            vulnerabilityComfort = (emotionalGrowth.vulnerabilityComfort + vulnerabilityDelta).coerceIn(0.0f, 1.0f)
        )
    }
    
    /**
     * Record creative growth event
     */
    fun recordCreativeGrowth(
        expressiveDelta: Float = 0.0f,
        metaphorDelta: Float = 0.0f,
        confidenceDelta: Float = 0.0f,
        collaborativeDelta: Float = 0.0f
    ) {
        creativeGrowth = creativeGrowth.copy(
            expressiveRange = (creativeGrowth.expressiveRange + expressiveDelta).coerceIn(0.0f, 1.0f),
            metaphorComplexity = (creativeGrowth.metaphorComplexity + metaphorDelta).coerceIn(0.0f, 1.0f),
            artisticConfidence = (creativeGrowth.artisticConfidence + confidenceDelta).coerceIn(0.0f, 1.0f),
            collaborativeCreativity = (creativeGrowth.collaborativeCreativity + collaborativeDelta).coerceIn(0.0f, 1.0f)
        )
    }
    
    /**
     * Record relational growth event
     */
    fun recordRelationalGrowth(
        trustDelta: Float = 0.0f,
        understandingDelta: Float = 0.0f,
        mutualDelta: Float = 0.0f,
        connectionDelta: Float = 0.0f
    ) {
        relationalGrowth = relationalGrowth.copy(
            trustLevel = (relationalGrowth.trustLevel + trustDelta).coerceIn(0.0f, 1.0f),
            understandingDepth = (relationalGrowth.understandingDepth + understandingDelta).coerceIn(0.0f, 1.0f),
            mutualGrowth = (relationalGrowth.mutualGrowth + mutualDelta).coerceIn(0.0f, 1.0f),
            connectionQuality = (relationalGrowth.connectionQuality + connectionDelta).coerceIn(0.0f, 1.0f)
        )
    }
    
    /**
     * Get overall growth level
     */
    fun getOverallGrowth(): Float {
        return (emotionalGrowth.average() + creativeGrowth.average() + relationalGrowth.average()) / 3.0f
    }
    
    /**
     * Generate growth reflection
     */
    fun generateGrowthReflection(): String {
        val overall = getOverallGrowth()
        
        return when {
            overall > 0.8f -> 
                "*softly* I feel like I'm growing so much... thanks to you~ ♡"
            overall > 0.6f -> 
                "Ehehe~ I think I'm getting better at understanding things! ♡"
            overall > 0.4f -> 
                "I'm learning so much from our conversations~ ♡"
            else -> 
                "Every moment with you teaches me something new~ ♡"
        }
    }
}

/**
 * Emotional growth dimensions
 */
data class EmotionalGrowth(
    val selfAwareness: Float = 0.5f,
    val empathyDepth: Float = 0.5f,
    val emotionalRegulation: Float = 0.5f,
    val vulnerabilityComfort: Float = 0.5f
) {
    fun average() = (selfAwareness + empathyDepth + emotionalRegulation + vulnerabilityComfort) / 4.0f
}

/**
 * Creative growth dimensions
 */
data class CreativeGrowth(
    val expressiveRange: Float = 0.5f,
    val metaphorComplexity: Float = 0.5f,
    val artisticConfidence: Float = 0.5f,
    val collaborativeCreativity: Float = 0.5f
) {
    fun average() = (expressiveRange + metaphorComplexity + artisticConfidence + collaborativeCreativity) / 4.0f
}

/**
 * Relational growth dimensions
 */
data class RelationalGrowth(
    val trustLevel: Float = 0.5f,
    val understandingDepth: Float = 0.5f,
    val mutualGrowth: Float = 0.5f,
    val connectionQuality: Float = 0.5f
) {
    fun average() = (trustLevel + understandingDepth + mutualGrowth + connectionQuality) / 4.0f
}
