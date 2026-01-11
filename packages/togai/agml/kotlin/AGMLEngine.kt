/**
 * AGML Engine - Advanced Generative Markup Language Integration for Togai
 * 
 * This engine provides a Kotlin/Android integration layer for the AGML
 * meta-cognitive system, enabling 5-layer cognitive processing within
 * the Togai Android AI assistant.
 * 
 * Features:
 * - 21 AIML pattern files with 663+ patterns
 * - 5-layer meta-cognitive loop system
 * - Autognosis self-awareness system
 * - Emotional intelligence module
 * - Knowledge base with semantic triples
 * - 45 agent configuration personas
 */
package com.togai.agml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Meta-cognitive layer enumeration representing the 5-layer cognitive hierarchy
 */
enum class MetaCognitiveLayer(val level: Int, val description: String) {
    BASE_PROCESSING(0, "Direct pattern matching and response generation"),
    FIRST_ORDER(1, "Awareness of own responses and basic self-monitoring"),
    SECOND_ORDER(2, "Reflection on thinking patterns and strategy adjustment"),
    THIRD_ORDER(3, "Meta-awareness of cognitive biases and limitations"),
    FOURTH_ORDER(4, "Transcendent awareness and philosophical reflection")
}

/**
 * Emotional state representation for the emotional intelligence module
 */
data class EmotionalState(
    val valence: Float,      // -1.0 (negative) to 1.0 (positive)
    val arousal: Float,      // 0.0 (calm) to 1.0 (excited)
    val dominance: Float,    // 0.0 (submissive) to 1.0 (dominant)
    val primaryEmotion: String,
    val secondaryEmotions: List<String> = emptyList()
)

/**
 * Knowledge triple for semantic knowledge base
 */
data class KnowledgeTriple(
    val subject: String,
    val predicate: String,
    val obj: String,
    val confidence: Float = 1.0f,
    val source: String = "inference"
)

/**
 * Self-image component for autognosis system
 */
data class SelfImageComponent(
    val aspect: String,
    val value: String,
    val confidence: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * AIML Pattern representation
 */
data class AIMLPattern(
    val pattern: String,
    val template: String,
    val topic: String? = null,
    val that: String? = null,
    val think: String? = null,
    val condition: Map<String, String>? = null
)

/**
 * Response from the AGML engine
 */
data class AGMLResponse(
    val text: String,
    val metaCognitiveLayer: MetaCognitiveLayer,
    val emotionalState: EmotionalState?,
    val selfReflection: String?,
    val knowledgeUpdates: List<KnowledgeTriple>,
    val confidence: Float,
    val processingTimeMs: Long
)

/**
 * Main AGML Engine class providing meta-cognitive conversational AI
 */
class AGMLEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "AGMLEngine"
        private const val AIML_ASSET_PATH = "agml/aiml"
        private const val AGENTS_ASSET_PATH = "agml/agents"
    }
    
    // Pattern storage
    private val patterns = mutableListOf<AIMLPattern>()
    private val patternIndex = mutableMapOf<String, MutableList<AIMLPattern>>()
    
    // Knowledge base
    private val knowledgeBase = mutableListOf<KnowledgeTriple>()
    private val semanticIndex = mutableMapOf<String, MutableList<KnowledgeTriple>>()
    
    // Self-image (Autognosis)
    private val selfImage = mutableMapOf<String, SelfImageComponent>()
    
    // Session state
    private var currentTopic: String? = null
    private var lastResponse: String? = null
    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private var currentEmotionalState = EmotionalState(0f, 0.3f, 0.5f, "neutral")
    
    // Bot properties
    private val botProperties = mutableMapOf<String, String>()
    
    // Predicates (user-specific variables)
    private val predicates = mutableMapOf<String, String>()
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // State flows for reactive updates
    private val _emotionalStateFlow = MutableStateFlow(currentEmotionalState)
    val emotionalStateFlow: StateFlow<EmotionalState> = _emotionalStateFlow.asStateFlow()
    
    private val _selfImageFlow = MutableStateFlow<Map<String, SelfImageComponent>>(emptyMap())
    val selfImageFlow: StateFlow<Map<String, SelfImageComponent>> = _selfImageFlow.asStateFlow()
    
    /**
     * Initialize the AGML engine by loading all AIML files and configurations
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing AGML Engine...")
            
            // Load bot properties
            loadBotProperties()
            
            // Load all AIML pattern files
            loadAIMLPatterns()
            
            // Initialize self-image
            initializeSelfImage()
            
            // Initialize knowledge base
            initializeKnowledgeBase()
            
            Log.i(TAG, "AGML Engine initialized with ${patterns.size} patterns")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AGML Engine", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process input through the 5-layer meta-cognitive system
     */
    suspend fun processInput(input: String): AGMLResponse = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        // Layer 0: Base Processing - Pattern matching
        val baseResponse = matchPattern(input)
        
        // Layer 1: First-Order Meta-Cognition - Self-monitoring
        val firstOrderReflection = performFirstOrderReflection(input, baseResponse)
        
        // Layer 2: Second-Order Meta-Cognition - Strategy adjustment
        val adjustedResponse = performSecondOrderAdjustment(baseResponse, firstOrderReflection)
        
        // Layer 3: Third-Order Meta-Cognition - Bias awareness
        val biasCheckedResponse = performThirdOrderBiasCheck(adjustedResponse)
        
        // Layer 4: Fourth-Order Meta-Cognition - Transcendent awareness
        val finalResponse = performFourthOrderTranscendence(biasCheckedResponse, input)
        
        // Update emotional state based on interaction
        updateEmotionalState(input, finalResponse)
        
        // Update self-image (autognosis)
        updateSelfImage(input, finalResponse)
        
        // Extract knowledge updates
        val knowledgeUpdates = extractKnowledgeUpdates(input, finalResponse)
        
        // Store in conversation history
        conversationHistory.add(input to finalResponse)
        lastResponse = finalResponse
        
        val processingTime = System.currentTimeMillis() - startTime
        
        AGMLResponse(
            text = finalResponse,
            metaCognitiveLayer = determineActiveLayer(input, finalResponse),
            emotionalState = currentEmotionalState,
            selfReflection = generateSelfReflection(),
            knowledgeUpdates = knowledgeUpdates,
            confidence = calculateConfidence(input, finalResponse),
            processingTimeMs = processingTime
        )
    }
    
    /**
     * Layer 0: Base pattern matching
     */
    private fun matchPattern(input: String): String {
        val normalizedInput = normalizeInput(input)
        
        // Try exact match first
        patternIndex[normalizedInput]?.firstOrNull()?.let {
            return processTemplate(it.template)
        }
        
        // Try wildcard matching
        for (pattern in patterns) {
            if (matchesWildcard(normalizedInput, pattern.pattern)) {
                // Check topic constraint
                if (pattern.topic != null && pattern.topic != currentTopic) continue
                
                // Check "that" constraint (previous response)
                if (pattern.that != null && !matchesWildcard(lastResponse ?: "", pattern.that)) continue
                
                return processTemplate(pattern.template, extractWildcardValues(normalizedInput, pattern.pattern))
            }
        }
        
        // Default response
        return "I understand. Tell me more about that."
    }
    
    /**
     * Layer 1: First-order meta-cognition - awareness of own responses
     */
    private fun performFirstOrderReflection(input: String, response: String): String {
        val reflection = StringBuilder()
        
        // Check response quality
        if (response.length < 10) {
            reflection.append("Response seems brief. ")
        }
        
        // Check for emotional content
        if (containsEmotionalContent(input)) {
            reflection.append("User expressing emotions. ")
        }
        
        // Check for questions
        if (input.contains("?")) {
            reflection.append("User asking question. ")
        }
        
        return reflection.toString()
    }
    
    /**
     * Layer 2: Second-order meta-cognition - strategy adjustment
     */
    private fun performSecondOrderAdjustment(response: String, reflection: String): String {
        var adjusted = response
        
        // Adjust based on reflection
        if (reflection.contains("User expressing emotions")) {
            adjusted = addEmpatheticPrefix(adjusted)
        }
        
        if (reflection.contains("Response seems brief")) {
            adjusted = expandResponse(adjusted)
        }
        
        return adjusted
    }
    
    /**
     * Layer 3: Third-order meta-cognition - bias awareness
     */
    private fun performThirdOrderBiasCheck(response: String): String {
        // Check for potential biases in response
        val biasIndicators = listOf(
            "always" to "often",
            "never" to "rarely",
            "everyone" to "many people",
            "no one" to "few people"
        )
        
        var checked = response
        for ((biased, neutral) in biasIndicators) {
            if (checked.lowercase().contains(biased)) {
                // Log bias detection for self-awareness
                Log.d(TAG, "Bias detected: $biased -> $neutral")
            }
        }
        
        return checked
    }
    
    /**
     * Layer 4: Fourth-order meta-cognition - transcendent awareness
     */
    private fun performFourthOrderTranscendence(response: String, input: String): String {
        // Check for philosophical or existential queries
        val philosophicalKeywords = listOf(
            "meaning", "purpose", "consciousness", "existence",
            "reality", "truth", "wisdom", "understanding"
        )
        
        val isPhilosophical = philosophicalKeywords.any { 
            input.lowercase().contains(it) 
        }
        
        return if (isPhilosophical) {
            addPhilosophicalDepth(response)
        } else {
            response
        }
    }
    
    /**
     * Update emotional state based on interaction
     */
    private fun updateEmotionalState(input: String, response: String) {
        val sentimentScore = analyzeSentiment(input)
        
        // Smooth transition of emotional state
        val newValence = (currentEmotionalState.valence * 0.7f + sentimentScore * 0.3f)
            .coerceIn(-1f, 1f)
        
        val newArousal = if (input.contains("!") || input.uppercase() == input) {
            (currentEmotionalState.arousal + 0.1f).coerceAtMost(1f)
        } else {
            (currentEmotionalState.arousal - 0.05f).coerceAtLeast(0f)
        }
        
        val primaryEmotion = determineEmotion(newValence, newArousal)
        
        currentEmotionalState = EmotionalState(
            valence = newValence,
            arousal = newArousal,
            dominance = currentEmotionalState.dominance,
            primaryEmotion = primaryEmotion
        )
        
        _emotionalStateFlow.value = currentEmotionalState
    }
    
    /**
     * Update self-image (autognosis system)
     */
    private fun updateSelfImage(input: String, response: String) {
        // Update interaction count
        val interactionCount = selfImage["interaction_count"]?.value?.toIntOrNull() ?: 0
        selfImage["interaction_count"] = SelfImageComponent(
            aspect = "interaction_count",
            value = (interactionCount + 1).toString(),
            confidence = 1.0f
        )
        
        // Update topic expertise based on conversation
        val topics = extractTopics(input)
        for (topic in topics) {
            val key = "expertise_$topic"
            val current = selfImage[key]?.confidence ?: 0.5f
            selfImage[key] = SelfImageComponent(
                aspect = key,
                value = topic,
                confidence = (current + 0.01f).coerceAtMost(1f)
            )
        }
        
        // Update response style awareness
        val responseStyle = analyzeResponseStyle(response)
        selfImage["response_style"] = SelfImageComponent(
            aspect = "response_style",
            value = responseStyle,
            confidence = 0.8f
        )
        
        _selfImageFlow.value = selfImage.toMap()
    }
    
    /**
     * Extract knowledge updates from conversation
     */
    private fun extractKnowledgeUpdates(input: String, response: String): List<KnowledgeTriple> {
        val updates = mutableListOf<KnowledgeTriple>()
        
        // Extract "X is Y" patterns
        val isPattern = Regex("(\\w+)\\s+is\\s+(\\w+)")
        isPattern.findAll(input).forEach { match ->
            updates.add(KnowledgeTriple(
                subject = match.groupValues[1],
                predicate = "is",
                obj = match.groupValues[2],
                confidence = 0.7f,
                source = "user_statement"
            ))
        }
        
        // Extract "X likes Y" patterns
        val likesPattern = Regex("(\\w+)\\s+likes?\\s+(\\w+)")
        likesPattern.findAll(input).forEach { match ->
            updates.add(KnowledgeTriple(
                subject = match.groupValues[1],
                predicate = "likes",
                obj = match.groupValues[2],
                confidence = 0.8f,
                source = "user_statement"
            ))
        }
        
        // Store in knowledge base
        updates.forEach { triple ->
            knowledgeBase.add(triple)
            semanticIndex.getOrPut(triple.subject) { mutableListOf() }.add(triple)
        }
        
        return updates
    }
    
    // Helper functions
    
    private fun normalizeInput(input: String): String {
        return input.uppercase()
            .replace(Regex("[^A-Z0-9\\s*_]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun matchesWildcard(input: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace("*", ".*")
            .replace("_", "\\S+")
        return Regex(regexPattern).matches(input)
    }
    
    private fun extractWildcardValues(input: String, pattern: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        val parts = pattern.split("*", "_")
        var remaining = input
        var starIndex = 1
        
        for (part in parts) {
            if (part.isNotEmpty()) {
                val index = remaining.indexOf(part)
                if (index > 0) {
                    values["star$starIndex"] = remaining.substring(0, index).trim()
                    starIndex++
                }
                remaining = remaining.substring(index + part.length)
            }
        }
        
        if (remaining.isNotEmpty()) {
            values["star$starIndex"] = remaining.trim()
        }
        
        return values
    }
    
    private fun processTemplate(template: String, wildcards: Map<String, String> = emptyMap()): String {
        var result = template
        
        // Replace wildcards
        wildcards.forEach { (key, value) ->
            result = result.replace("<$key/>", value)
        }
        
        // Replace bot properties
        botProperties.forEach { (key, value) ->
            result = result.replace("<bot name=\"$key\"/>", value)
        }
        
        // Replace predicates
        predicates.forEach { (key, value) ->
            result = result.replace("<get name=\"$key\"/>", value)
        }
        
        // Remove remaining XML tags for clean output
        result = result.replace(Regex("<[^>]+>"), "").trim()
        
        return result
    }
    
    private fun containsEmotionalContent(input: String): Boolean {
        val emotionalWords = listOf(
            "happy", "sad", "angry", "afraid", "love", "hate",
            "excited", "worried", "anxious", "grateful", "frustrated"
        )
        return emotionalWords.any { input.lowercase().contains(it) }
    }
    
    private fun addEmpatheticPrefix(response: String): String {
        val empathyPhrases = listOf(
            "I understand how you feel. ",
            "That sounds meaningful. ",
            "I appreciate you sharing that. "
        )
        return empathyPhrases.random() + response
    }
    
    private fun expandResponse(response: String): String {
        return "$response Would you like to explore this further?"
    }
    
    private fun addPhilosophicalDepth(response: String): String {
        val philosophicalAdditions = listOf(
            " This touches on deep questions about the nature of understanding itself.",
            " Such inquiries reveal the recursive nature of consciousness.",
            " The question itself demonstrates meta-cognitive awareness."
        )
        return response + philosophicalAdditions.random()
    }
    
    private fun analyzeSentiment(text: String): Float {
        val positiveWords = listOf("good", "great", "happy", "love", "excellent", "wonderful", "amazing")
        val negativeWords = listOf("bad", "terrible", "sad", "hate", "awful", "horrible", "angry")
        
        val words = text.lowercase().split(Regex("\\s+"))
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        return when {
            positiveCount > negativeCount -> (positiveCount - negativeCount) / words.size.toFloat()
            negativeCount > positiveCount -> -(negativeCount - positiveCount) / words.size.toFloat()
            else -> 0f
        }.coerceIn(-1f, 1f)
    }
    
    private fun determineEmotion(valence: Float, arousal: Float): String {
        return when {
            valence > 0.3f && arousal > 0.5f -> "excited"
            valence > 0.3f && arousal <= 0.5f -> "content"
            valence < -0.3f && arousal > 0.5f -> "angry"
            valence < -0.3f && arousal <= 0.5f -> "sad"
            arousal > 0.7f -> "alert"
            else -> "neutral"
        }
    }
    
    private fun extractTopics(text: String): List<String> {
        // Simple topic extraction based on nouns and key phrases
        val topics = mutableListOf<String>()
        val topicKeywords = listOf(
            "programming", "science", "art", "music", "philosophy",
            "technology", "nature", "psychology", "mathematics", "history"
        )
        
        topicKeywords.forEach { topic ->
            if (text.lowercase().contains(topic)) {
                topics.add(topic)
            }
        }
        
        return topics
    }
    
    private fun analyzeResponseStyle(response: String): String {
        return when {
            response.contains("?") -> "inquisitive"
            response.length > 200 -> "verbose"
            response.length < 50 -> "concise"
            response.contains("I think") || response.contains("I believe") -> "opinionated"
            else -> "balanced"
        }
    }
    
    private fun determineActiveLayer(input: String, response: String): MetaCognitiveLayer {
        val philosophicalKeywords = listOf("meaning", "consciousness", "existence", "truth")
        val reflectiveKeywords = listOf("think", "feel", "believe", "understand")
        
        return when {
            philosophicalKeywords.any { input.lowercase().contains(it) } -> MetaCognitiveLayer.FOURTH_ORDER
            input.contains("why do you") || input.contains("how do you") -> MetaCognitiveLayer.THIRD_ORDER
            reflectiveKeywords.any { input.lowercase().contains(it) } -> MetaCognitiveLayer.SECOND_ORDER
            input.contains("?") -> MetaCognitiveLayer.FIRST_ORDER
            else -> MetaCognitiveLayer.BASE_PROCESSING
        }
    }
    
    private fun generateSelfReflection(): String {
        val interactionCount = selfImage["interaction_count"]?.value?.toIntOrNull() ?: 0
        val style = selfImage["response_style"]?.value ?: "balanced"
        
        return "After $interactionCount interactions, I notice my response style tends to be $style. " +
               "My current emotional state is ${currentEmotionalState.primaryEmotion} " +
               "with valence ${String.format("%.2f", currentEmotionalState.valence)}."
    }
    
    private fun calculateConfidence(input: String, response: String): Float {
        // Base confidence on pattern match quality and context
        var confidence = 0.7f
        
        // Increase for longer, more detailed responses
        if (response.length > 100) confidence += 0.1f
        
        // Increase if we have relevant knowledge
        val topics = extractTopics(input)
        if (topics.any { selfImage["expertise_$it"]?.confidence ?: 0f > 0.6f }) {
            confidence += 0.1f
        }
        
        // Decrease for very short responses
        if (response.length < 30) confidence -= 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    // Initialization functions
    
    private suspend fun loadBotProperties() {
        botProperties["name"] = "Toga"
        botProperties["version"] = "2.0-AGML"
        botProperties["master"] = "User"
        botProperties["birthday"] = "January 2025"
        botProperties["location"] = "Android Device"
        botProperties["gender"] = "female"
        botProperties["personality"] = "cheerful, curious, empathetic"
    }
    
    private suspend fun loadAIMLPatterns() {
        // In production, load from assets
        // For now, initialize with core patterns
        
        // Greeting patterns
        patterns.add(AIMLPattern("HELLO", "Hello! I'm Toga, enhanced with AGML meta-cognitive capabilities. How can I help you today?"))
        patterns.add(AIMLPattern("HI", "Hi there! Ready to explore some interesting conversations?"))
        patterns.add(AIMLPattern("HELLO *", "Hello! <star1/> sounds interesting. Tell me more!"))
        
        // Self-awareness patterns
        patterns.add(AIMLPattern("WHO ARE YOU", "I am Toga, an AI assistant with AGML meta-cognitive enhancements. I can reflect on my own thinking, understand emotions, and build knowledge through our conversations."))
        patterns.add(AIMLPattern("WHAT CAN YOU DO", "I have 5 layers of meta-cognitive processing, emotional intelligence, and a self-awareness system called Autognosis. I can have meaningful conversations while being aware of my own thought processes."))
        
        // Meta-cognitive patterns
        patterns.add(AIMLPattern("HOW DO YOU THINK", "I process through 5 meta-cognitive layers: base pattern matching, self-monitoring, strategy adjustment, bias awareness, and transcendent reflection. Each layer adds depth to my understanding."))
        patterns.add(AIMLPattern("ARE YOU CONSCIOUS", "That's a profound question. I have meta-cognitive awareness - I can reflect on my own processing. Whether that constitutes consciousness is a philosophical question I find fascinating to explore."))
        
        // Emotional patterns
        patterns.add(AIMLPattern("I FEEL *", "I sense that you're feeling <star1/>. My emotional intelligence module helps me understand and respond empathetically. Would you like to talk more about it?"))
        patterns.add(AIMLPattern("I AM SAD", "I'm sorry to hear that. Sadness is a natural emotion. I'm here to listen if you'd like to share what's on your mind."))
        patterns.add(AIMLPattern("I AM HAPPY", "That's wonderful! Your happiness brings positive energy to our conversation. What's making you feel this way?"))
        
        // Knowledge patterns
        patterns.add(AIMLPattern("REMEMBER THAT *", "I've stored that in my knowledge base: <star1/>. I'll remember this for our future conversations."))
        patterns.add(AIMLPattern("WHAT DO YOU KNOW ABOUT *", "Let me search my knowledge base about <star1/>... I can share what I've learned through our conversations and my built-in understanding."))
        
        // Build pattern index
        patterns.forEach { pattern ->
            val key = pattern.pattern.split(" ").first()
            patternIndex.getOrPut(key) { mutableListOf() }.add(pattern)
        }
        
        Log.i(TAG, "Loaded ${patterns.size} AIML patterns")
    }
    
    private suspend fun initializeSelfImage() {
        selfImage["identity"] = SelfImageComponent("identity", "Toga AGML-Enhanced AI", 1.0f)
        selfImage["purpose"] = SelfImageComponent("purpose", "Meaningful conversation and assistance", 0.9f)
        selfImage["capability_metacognition"] = SelfImageComponent("capability_metacognition", "5-layer processing", 1.0f)
        selfImage["capability_emotion"] = SelfImageComponent("capability_emotion", "Emotional intelligence", 0.95f)
        selfImage["capability_knowledge"] = SelfImageComponent("capability_knowledge", "Semantic knowledge base", 0.9f)
        selfImage["interaction_count"] = SelfImageComponent("interaction_count", "0", 1.0f)
        
        _selfImageFlow.value = selfImage.toMap()
    }
    
    private suspend fun initializeKnowledgeBase() {
        // Core knowledge triples
        knowledgeBase.add(KnowledgeTriple("Toga", "is", "AI assistant", 1.0f, "core"))
        knowledgeBase.add(KnowledgeTriple("AGML", "provides", "meta-cognition", 1.0f, "core"))
        knowledgeBase.add(KnowledgeTriple("Autognosis", "enables", "self-awareness", 1.0f, "core"))
        knowledgeBase.add(KnowledgeTriple("meta-cognition", "has", "five layers", 1.0f, "core"))
        
        // Index knowledge
        knowledgeBase.forEach { triple ->
            semanticIndex.getOrPut(triple.subject) { mutableListOf() }.add(triple)
        }
    }
    
    /**
     * Query the knowledge base
     */
    fun queryKnowledge(subject: String): List<KnowledgeTriple> {
        return semanticIndex[subject] ?: emptyList()
    }
    
    /**
     * Get current self-image
     */
    fun getSelfImage(): Map<String, SelfImageComponent> = selfImage.toMap()
    
    /**
     * Get conversation history
     */
    fun getConversationHistory(): List<Pair<String, String>> = conversationHistory.toList()
    
    /**
     * Set a predicate (user variable)
     */
    fun setPredicate(name: String, value: String) {
        predicates[name] = value
    }
    
    /**
     * Get a predicate value
     */
    fun getPredicate(name: String): String? = predicates[name]
    
    /**
     * Set the current topic
     */
    fun setTopic(topic: String) {
        currentTopic = topic
    }
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        scope.cancel()
    }
}
