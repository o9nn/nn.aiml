/**
 * PersonaDriver.kt - Persona Driver Abstraction Layer
 *
 * The Persona Driver system provides a hardware-driver-like abstraction for
 * AI personas and characters. Just as device drivers abstract hardware interfaces,
 * Persona Drivers abstract character personalities and behaviors.
 *
 * This allows any AI persona to be implemented by:
 * 1. Defining personality traits (like hardware specifications)
 * 2. Implementing the driver interface (like device drivers)
 * 3. Registering with the Persona Bus (like plug-and-play)
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      Persona Bus                                │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │              Driver Manager                              │   │
 * │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
 * │  │  │Driver A │ │Driver B │ │Driver C │ │Driver N │ ...   │   │
 * │  │  │(Layla)  │ │(Aria)   │ │(Marcus) │ │(Custom) │       │   │
 * │  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘       │   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * │         ↓                                                      │
 * │  ┌─────────────────────────────────────────────────────────┐   │
 * │  │              Persona Interface Layer                     │   │
 * │  │  ┌──────────────────────────────────────────────────┐   │   │
 * │  │  │  PersonaSpec → PersonaState → PersonaOutput      │   │   │
 * │  │  └──────────────────────────────────────────────────┘   │   │
 * │  └─────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────┘
 */
package org.ninelym.aid.driver

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ninelym.aid.core.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

// ============= Persona Specification (like hardware specs) =============

/**
 * Trait category for organizing personality dimensions
 */
enum class TraitCategory {
    CORE,           // Fundamental personality traits
    EMOTIONAL,      // Emotional response patterns
    COGNITIVE,      // Thinking style and approach
    SOCIAL,         // Social interaction patterns
    CREATIVE,       // Creative expression
    ETHICAL,        // Ethical constraints and values
    BEHAVIORAL      // Behavior patterns and habits
}

/**
 * A personality trait with its value and metadata
 */
data class PersonalityTrait(
    val name: String,
    val category: TraitCategory,
    val value: Float,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val mutable: Boolean = true,
    val description: String = "",
    val modificationHistory: List<TraitModification> = emptyList()
) {
    /**
     * Clamp value to valid range
     */
    fun clampedValue(): Float = value.coerceIn(minValue, maxValue)

    /**
     * Create modified copy with history tracking
     */
    fun modify(newValue: Float, reason: String = ""): PersonalityTrait {
        if (!mutable) return this
        val modification = TraitModification(
            oldValue = value,
            newValue = newValue.coerceIn(minValue, maxValue),
            timestamp = System.currentTimeMillis(),
            reason = reason
        )
        return copy(
            value = newValue.coerceIn(minValue, maxValue),
            modificationHistory = modificationHistory + modification
        )
    }
}

data class TraitModification(
    val oldValue: Float,
    val newValue: Float,
    val timestamp: Long,
    val reason: String
)

/**
 * Speech pattern configuration
 */
data class SpeechPattern(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prefixes: List<String> = emptyList(),
    val suffixes: List<String> = emptyList(),
    val fillers: List<String> = emptyList(),
    val emoticons: List<String> = emptyList(),
    val vocabularyStyle: VocabularyStyle = VocabularyStyle.NEUTRAL,
    val sentenceStructure: SentenceStructure = SentenceStructure.STANDARD,
    val emphasisPatterns: List<String> = emptyList(),
    val triggerWords: Map<String, List<String>> = emptyMap(), // word -> alternatives
    val probability: Float = 0.5f // How often to apply the pattern
)

enum class VocabularyStyle {
    FORMAL,
    CASUAL,
    PLAYFUL,
    TECHNICAL,
    POETIC,
    NEUTRAL
}

enum class SentenceStructure {
    STANDARD,
    EXCLAMATORY,
    QUESTIONING,
    FRAGMENTED,
    FLOWING,
    CONCISE
}

/**
 * Complete persona specification - the "hardware specs" for a persona
 */
data class PersonaSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val displayName: String = name,
    val description: String = "",
    val version: String = "1.0.0",
    val traits: Map<String, PersonalityTrait>,
    val speechPatterns: List<SpeechPattern> = emptyList(),
    val defaultTensor: AIDTensor = AIDTensor.neutral(),
    val avatarConfig: Map<String, Any> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Get trait value by name
     */
    fun getTraitValue(name: String): Float? = traits[name]?.clampedValue()

    /**
     * Get all traits in a category
     */
    fun getTraitsByCategory(category: TraitCategory): List<PersonalityTrait> =
        traits.values.filter { it.category == category }

    /**
     * Calculate composite trait value from multiple traits
     */
    fun calculateComposite(traitNames: List<String>, weights: List<Float>? = null): Float {
        if (traitNames.isEmpty()) return 0.5f
        val w = weights ?: List(traitNames.size) { 1f / traitNames.size }
        return traitNames.zip(w).sumOf { (name, weight) ->
            (getTraitValue(name) ?: 0.5f).toDouble() * weight
        }.toFloat()
    }

    /**
     * Convert to AIDTensor based on current traits
     */
    fun toTensor(): AIDTensor {
        val emotionalTraits = getTraitsByCategory(TraitCategory.EMOTIONAL)
        val cognitiveTraits = getTraitsByCategory(TraitCategory.COGNITIVE)
        val creativeTraits = getTraitsByCategory(TraitCategory.CREATIVE)
        val ethicalTraits = getTraitsByCategory(TraitCategory.ETHICAL)

        return AIDTensor(
            modality = defaultTensor.modality,
            depth = cognitiveTraits.map { it.clampedValue() }.average().toFloat().takeIf { !it.isNaN() } ?: 0.5f,
            context = defaultTensor.context,
            salience = emotionalTraits.map { it.clampedValue() }.average().toFloat().takeIf { !it.isNaN() } ?: 0.5f,
            autonomyIndex = defaultTensor.autonomyIndex,
            identity = 1.0f, // Persona identity coherence
            emotionalValence = (emotionalTraits.find { it.name.contains("positivity", ignoreCase = true) }?.clampedValue() ?: 0.5f),
            creativityFactor = creativeTraits.map { it.clampedValue() }.average().toFloat().takeIf { !it.isNaN() } ?: 0.5f,
            ethicalConstraint = ethicalTraits.map { it.clampedValue() }.average().toFloat().takeIf { !it.isNaN() } ?: 1.0f
        )
    }

    companion object {
        /**
         * Create a basic persona spec with minimal configuration
         */
        fun basic(name: String, description: String = ""): PersonaSpec {
            return PersonaSpec(
                name = name,
                description = description,
                traits = mapOf(
                    "helpfulness" to PersonalityTrait("helpfulness", TraitCategory.CORE, 0.8f),
                    "friendliness" to PersonalityTrait("friendliness", TraitCategory.SOCIAL, 0.7f),
                    "creativity" to PersonalityTrait("creativity", TraitCategory.CREATIVE, 0.6f),
                    "empathy" to PersonalityTrait("empathy", TraitCategory.EMOTIONAL, 0.7f),
                    "ethics" to PersonalityTrait("ethics", TraitCategory.ETHICAL, 1.0f, mutable = false)
                )
            )
        }
    }
}

// ============= Persona State (runtime state) =============

/**
 * Emotional state of a persona at runtime
 */
data class EmotionalState(
    val valence: Float = 0.5f,          // Positive (1) to negative (0)
    val arousal: Float = 0.5f,          // High energy (1) to low energy (0)
    val dominance: Float = 0.5f,        // In control (1) to submissive (0)
    val mood: String = "neutral",
    val intensity: Float = 0.5f,
    val stability: Float = 0.8f,        // How stable the emotional state is
    val lastUpdate: Long = System.currentTimeMillis()
) {
    /**
     * Blend with another emotional state
     */
    fun blend(other: EmotionalState, weight: Float = 0.3f): EmotionalState {
        val w = weight.coerceIn(0f, 1f)
        val iw = 1f - w
        return EmotionalState(
            valence = valence * iw + other.valence * w,
            arousal = arousal * iw + other.arousal * w,
            dominance = dominance * iw + other.dominance * w,
            mood = if (w > 0.5f) other.mood else mood,
            intensity = intensity * iw + other.intensity * w,
            stability = stability * 0.95f, // Transitions reduce stability
            lastUpdate = System.currentTimeMillis()
        )
    }

    /**
     * Apply decay toward neutral
     */
    fun decay(rate: Float = 0.1f): EmotionalState {
        val decayToward = 0.5f
        return copy(
            valence = valence + (decayToward - valence) * rate,
            arousal = arousal + (decayToward - arousal) * rate,
            dominance = dominance + (decayToward - dominance) * rate,
            intensity = intensity * (1f - rate),
            stability = minOf(stability + rate * 0.5f, 1f),
            lastUpdate = System.currentTimeMillis()
        )
    }
}

/**
 * Memory context for the persona
 */
data class PersonaMemoryContext(
    val recentTopics: List<String> = emptyList(),
    val userPreferences: Map<String, Any> = emptyMap(),
    val conversationLength: Int = 0,
    val lastInteractionTime: Long = System.currentTimeMillis(),
    val interactionHistory: List<InteractionSummary> = emptyList()
)

data class InteractionSummary(
    val timestamp: Long,
    val sentiment: Float,
    val topicTags: List<String>,
    val outcomePositive: Boolean
)

/**
 * Complete runtime state of a persona
 */
data class PersonaState(
    val specId: String,
    val emotionalState: EmotionalState = EmotionalState(),
    val memoryContext: PersonaMemoryContext = PersonaMemoryContext(),
    val currentTensor: AIDTensor = AIDTensor.neutral(),
    val activeModifiers: List<StateModifier> = emptyList(),
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionStart: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis()
)

data class StateModifier(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tensorModification: AIDTensor,
    val duration: Long = Long.MAX_VALUE,
    val startTime: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > startTime + duration
}

// ============= Persona Output =============

/**
 * Output from persona processing
 */
data class PersonaOutput(
    val content: String,
    val tensor: AIDTensor,
    val emotionalState: EmotionalState,
    val metadata: Map<String, Any> = emptyMap(),
    val speechPatternApplied: String? = null,
    val processingTimeMs: Long = 0
)

// ============= Persona Driver Interface =============

/**
 * Interface that all persona drivers must implement
 * Similar to a hardware device driver interface
 */
interface PersonaDriver {
    /**
     * Get the persona specification this driver implements
     */
    val spec: PersonaSpec

    /**
     * Get current state
     */
    val state: PersonaState

    /**
     * Initialize the driver
     */
    suspend fun initialize(): AIDResult<Unit>

    /**
     * Process input through this persona
     */
    suspend fun process(
        input: String,
        inputTensor: AIDTensor,
        context: Map<String, Any> = emptyMap()
    ): AIDResult<PersonaOutput>

    /**
     * Modulate a tensor according to this persona's characteristics
     */
    fun modulate(tensor: AIDTensor): AIDTensor

    /**
     * Apply an emotional stimulus
     */
    suspend fun applyStimulus(
        stimulus: String,
        intensity: Float = 0.5f,
        category: TraitCategory = TraitCategory.EMOTIONAL
    ): EmotionalState

    /**
     * Update the persona's memory context
     */
    suspend fun updateMemory(context: PersonaMemoryContext)

    /**
     * Get driver capabilities
     */
    fun getCapabilities(): Set<DriverCapability>

    /**
     * Shutdown the driver
     */
    suspend fun shutdown()
}

enum class DriverCapability {
    TEXT_PROCESSING,
    EMOTIONAL_MODULATION,
    SPEECH_PATTERN_APPLICATION,
    MEMORY_MANAGEMENT,
    TRAIT_EVOLUTION,
    MULTI_LANGUAGE,
    AVATAR_CONTROL
}

// ============= Base Persona Driver Implementation =============

/**
 * Base implementation of PersonaDriver that can be extended for specific personas
 */
open class BasePersonaDriver(
    override val spec: PersonaSpec
) : PersonaDriver {
    protected val stateMutex = Mutex()
    protected var _state = PersonaState(specId = spec.id, currentTensor = spec.toTensor())

    override val state: PersonaState
        get() = _state

    override suspend fun initialize(): AIDResult<Unit> {
        stateMutex.withLock {
            _state = PersonaState(
                specId = spec.id,
                currentTensor = spec.toTensor(),
                sessionStart = System.currentTimeMillis()
            )
        }
        return AIDResult.Success(Unit)
    }

    override suspend fun process(
        input: String,
        inputTensor: AIDTensor,
        context: Map<String, Any>
    ): AIDResult<PersonaOutput> {
        val startTime = System.currentTimeMillis()

        return try {
            stateMutex.withLock {
                _state = _state.copy(lastActivity = System.currentTimeMillis())
            }

            // Modulate input tensor with persona characteristics
            val modulatedTensor = modulate(inputTensor)

            // Apply speech pattern if applicable
            val processedContent = applySpeechPattern(input, context)

            // Update emotional state based on input
            val newEmotionalState = processEmotionalResponse(input, inputTensor)

            stateMutex.withLock {
                _state = _state.copy(
                    emotionalState = newEmotionalState,
                    currentTensor = modulatedTensor
                )
            }

            val output = PersonaOutput(
                content = processedContent,
                tensor = modulatedTensor,
                emotionalState = newEmotionalState,
                speechPatternApplied = spec.speechPatterns.firstOrNull()?.name,
                processingTimeMs = System.currentTimeMillis() - startTime
            )

            AIDResult.Success(output)
        } catch (e: Exception) {
            AIDResult.Error(AIDErrorCode.COGNITIVE_PIPELINE_ERROR, "Persona processing failed: ${e.message}", e)
        }
    }

    override fun modulate(tensor: AIDTensor): AIDTensor {
        val personaTensor = spec.toTensor()
        return tensor.blend(personaTensor, 0.4f).copy(
            identity = personaTensor.identity,
            ethicalConstraint = maxOf(tensor.ethicalConstraint, personaTensor.ethicalConstraint)
        )
    }

    override suspend fun applyStimulus(
        stimulus: String,
        intensity: Float,
        category: TraitCategory
    ): EmotionalState {
        return stateMutex.withLock {
            val currentEmotion = _state.emotionalState

            // Simple stimulus processing
            val valenceShift = when {
                stimulus.contains("happy", ignoreCase = true) ||
                stimulus.contains("good", ignoreCase = true) ||
                stimulus.contains("love", ignoreCase = true) -> intensity * 0.3f
                stimulus.contains("sad", ignoreCase = true) ||
                stimulus.contains("bad", ignoreCase = true) ||
                stimulus.contains("hate", ignoreCase = true) -> -intensity * 0.3f
                else -> 0f
            }

            val arousalShift = when {
                stimulus.contains("exciting", ignoreCase = true) ||
                stimulus.contains("amazing", ignoreCase = true) -> intensity * 0.2f
                stimulus.contains("calm", ignoreCase = true) ||
                stimulus.contains("relaxed", ignoreCase = true) -> -intensity * 0.2f
                else -> 0f
            }

            val newState = currentEmotion.copy(
                valence = (currentEmotion.valence + valenceShift).coerceIn(0f, 1f),
                arousal = (currentEmotion.arousal + arousalShift).coerceIn(0f, 1f),
                intensity = intensity,
                lastUpdate = System.currentTimeMillis()
            )

            _state = _state.copy(emotionalState = newState)
            newState
        }
    }

    override suspend fun updateMemory(context: PersonaMemoryContext) {
        stateMutex.withLock {
            _state = _state.copy(memoryContext = context)
        }
    }

    override fun getCapabilities(): Set<DriverCapability> = setOf(
        DriverCapability.TEXT_PROCESSING,
        DriverCapability.EMOTIONAL_MODULATION,
        DriverCapability.SPEECH_PATTERN_APPLICATION,
        DriverCapability.MEMORY_MANAGEMENT
    )

    override suspend fun shutdown() {
        // Cleanup if needed
    }

    // ============= Protected helper methods =============

    protected open fun applySpeechPattern(content: String, context: Map<String, Any>): String {
        if (spec.speechPatterns.isEmpty()) return content

        val pattern = spec.speechPatterns.first()
        var result = content

        // Apply prefix with probability
        if (pattern.prefixes.isNotEmpty() && Math.random() < pattern.probability) {
            result = "${pattern.prefixes.random()} $result"
        }

        // Apply suffix with probability
        if (pattern.suffixes.isNotEmpty() && Math.random() < pattern.probability) {
            result = "$result ${pattern.suffixes.random()}"
        }

        return result
    }

    protected open fun processEmotionalResponse(input: String, tensor: AIDTensor): EmotionalState {
        val current = _state.emotionalState

        // Simple keyword-based emotional processing
        val positiveKeywords = listOf("happy", "good", "great", "love", "wonderful", "amazing", "cute")
        val negativeKeywords = listOf("sad", "bad", "hate", "terrible", "awful", "wrong")

        val positiveCount = positiveKeywords.count { input.contains(it, ignoreCase = true) }
        val negativeCount = negativeKeywords.count { input.contains(it, ignoreCase = true) }

        val valenceShift = (positiveCount - negativeCount) * 0.1f

        return current.copy(
            valence = (current.valence + valenceShift).coerceIn(0f, 1f),
            arousal = (current.arousal + tensor.salience * 0.1f).coerceIn(0f, 1f),
            lastUpdate = System.currentTimeMillis()
        )
    }
}

// ============= Persona Bus =============

/**
 * The Persona Bus manages all registered persona drivers
 * Similar to a hardware device bus (USB, PCI, etc.)
 */
class PersonaBus : PersonaBusInterface {
    private var core: AIDCore? = null
    private var isInitialized = false

    // Registered drivers
    private val drivers = ConcurrentHashMap<String, PersonaDriver>()
    private val specs = ConcurrentHashMap<String, PersonaSpec>()

    // Active driver
    private var activeDriverId: String? = null

    // Events
    private val _events = MutableSharedFlow<PersonaBusEvent>(replay = 16)
    val events: SharedFlow<PersonaBusEvent> = _events.asSharedFlow()

    override suspend fun initialize(core: AIDCore) {
        this.core = core
        isInitialized = true
    }

    override suspend fun handleRequest(request: AIDRequest, tensor: AIDTensor): AIDResult<Any> {
        if (!isInitialized) {
            return AIDResult.Error(AIDErrorCode.DRIVER_INITIALIZATION_FAILED, "Persona bus not initialized")
        }

        return when (request.type) {
            AIDRequestType.LOAD_PERSONA -> loadPersonaFromRequest(request)
            AIDRequestType.SWITCH_PERSONA -> switchPersona(request)
            AIDRequestType.UPDATE_PERSONA_STATE -> updatePersonaState(request, tensor)
            AIDRequestType.GET_PERSONA_INFO -> getPersonaInfo(request)
            else -> AIDResult.Error(
                AIDErrorCode.INVALID_REQUEST,
                "Persona bus cannot handle request type: ${request.type}"
            )
        }
    }

    override suspend fun loadPersona(personaId: String): AIDResult<Unit> {
        val driver = drivers[personaId]
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Persona not found: $personaId")

        val initResult = driver.initialize()
        if (initResult is AIDResult.Error) {
            return initResult
        }

        activeDriverId = personaId
        _events.emit(PersonaBusEvent.PersonaLoaded(personaId, driver.spec))

        return AIDResult.Success(Unit)
    }

    override fun modulate(tensor: AIDTensor, personaId: String): AIDTensor {
        val driver = drivers[personaId] ?: return tensor
        return driver.modulate(tensor)
    }

    override suspend fun shutdown() {
        drivers.values.forEach { it.shutdown() }
        drivers.clear()
        specs.clear()
        isInitialized = false
    }

    // ============= Driver Registration =============

    /**
     * Register a persona driver
     */
    suspend fun registerDriver(driver: PersonaDriver): AIDResult<Unit> {
        return try {
            val initResult = driver.initialize()
            if (initResult is AIDResult.Error) {
                return AIDResult.Error(
                    AIDErrorCode.DRIVER_INITIALIZATION_FAILED,
                    "Driver initialization failed: ${initResult.message}"
                )
            }

            drivers[driver.spec.id] = driver
            specs[driver.spec.id] = driver.spec

            _events.emit(PersonaBusEvent.DriverRegistered(driver.spec.id, driver.spec))

            AIDResult.Success(Unit)
        } catch (e: Exception) {
            AIDResult.Error(AIDErrorCode.DRIVER_INITIALIZATION_FAILED, "Failed to register driver: ${e.message}", e)
        }
    }

    /**
     * Register a persona from a specification (creates default driver)
     */
    suspend fun registerPersona(spec: PersonaSpec): AIDResult<Unit> {
        val driver = BasePersonaDriver(spec)
        return registerDriver(driver)
    }

    /**
     * Unregister a driver
     */
    suspend fun unregisterDriver(personaId: String): AIDResult<Unit> {
        val driver = drivers.remove(personaId)
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Driver not found: $personaId")

        specs.remove(personaId)
        driver.shutdown()

        if (activeDriverId == personaId) {
            activeDriverId = null
        }

        _events.emit(PersonaBusEvent.DriverUnregistered(personaId))

        return AIDResult.Success(Unit)
    }

    // ============= Request Handlers =============

    private suspend fun loadPersonaFromRequest(request: AIDRequest): AIDResult<Any> {
        val personaId = request.targetPersonaId
            ?: (request.payload as? Map<*, *>)?.get("personaId") as? String
            ?: return AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "Persona ID required")

        return loadPersona(personaId)
    }

    private suspend fun switchPersona(request: AIDRequest): AIDResult<Any> {
        val targetId = request.targetPersonaId
            ?: (request.payload as? Map<*, *>)?.get("personaId") as? String
            ?: return AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "Target persona ID required")

        val previousId = activeDriverId

        val loadResult = loadPersona(targetId)
        if (loadResult is AIDResult.Error) {
            return loadResult
        }

        _events.emit(PersonaBusEvent.PersonaSwitched(previousId, targetId))

        return AIDResult.Success(mapOf(
            "previousPersona" to previousId,
            "currentPersona" to targetId
        ))
    }

    private suspend fun updatePersonaState(request: AIDRequest, tensor: AIDTensor): AIDResult<Any> {
        val personaId = request.targetPersonaId ?: activeDriverId
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "No active persona")

        val driver = drivers[personaId]
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Persona not found: $personaId")

        val payload = request.payload as? Map<*, *> ?: emptyMap<String, Any>()

        // Apply stimulus if provided
        val stimulus = payload["stimulus"] as? String
        if (stimulus != null) {
            val intensity = (payload["intensity"] as? Number)?.toFloat() ?: 0.5f
            driver.applyStimulus(stimulus, intensity)
        }

        // Update memory if provided
        val memoryUpdate = payload["memory"] as? PersonaMemoryContext
        if (memoryUpdate != null) {
            driver.updateMemory(memoryUpdate)
        }

        return AIDResult.Success(driver.state)
    }

    private fun getPersonaInfo(request: AIDRequest): AIDResult<Any> {
        val personaId = request.targetPersonaId
            ?: (request.payload as? Map<*, *>)?.get("personaId") as? String

        return if (personaId != null) {
            val spec = specs[personaId]
                ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Persona not found: $personaId")
            val driver = drivers[personaId]
            AIDResult.Success(mapOf(
                "spec" to spec,
                "state" to driver?.state,
                "capabilities" to driver?.getCapabilities()
            ))
        } else {
            // Return all personas
            AIDResult.Success(mapOf(
                "personas" to specs.values.toList(),
                "activePersonaId" to activeDriverId
            ))
        }
    }

    // ============= Public API =============

    /**
     * Get active driver
     */
    fun getActiveDriver(): PersonaDriver? = activeDriverId?.let { drivers[it] }

    /**
     * Get driver by ID
     */
    fun getDriver(personaId: String): PersonaDriver? = drivers[personaId]

    /**
     * Get all registered personas
     */
    fun getAllPersonas(): List<PersonaSpec> = specs.values.toList()

    /**
     * Process input through active persona
     */
    suspend fun process(
        input: String,
        tensor: AIDTensor = AIDTensor.neutral(),
        context: Map<String, Any> = emptyMap()
    ): AIDResult<PersonaOutput> {
        val driver = getActiveDriver()
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "No active persona")

        return driver.process(input, tensor, context)
    }

    /**
     * Create a custom persona dynamically
     */
    suspend fun createCustomPersona(
        name: String,
        traits: Map<String, Float>,
        speechPatterns: List<SpeechPattern> = emptyList()
    ): AIDResult<PersonaSpec> {
        val personalityTraits = traits.mapValues { (traitName, value) ->
            PersonalityTrait(
                name = traitName,
                category = inferCategory(traitName),
                value = value
            )
        }

        val spec = PersonaSpec(
            name = name,
            traits = personalityTraits,
            speechPatterns = speechPatterns
        )

        val registerResult = registerPersona(spec)
        if (registerResult is AIDResult.Error) {
            return AIDResult.Error(registerResult.code, registerResult.message)
        }

        return AIDResult.Success(spec)
    }

    private fun inferCategory(traitName: String): TraitCategory {
        return when {
            traitName.contains("emotion", ignoreCase = true) ||
            traitName.contains("happy", ignoreCase = true) ||
            traitName.contains("sad", ignoreCase = true) -> TraitCategory.EMOTIONAL

            traitName.contains("creative", ignoreCase = true) ||
            traitName.contains("imagination", ignoreCase = true) -> TraitCategory.CREATIVE

            traitName.contains("logic", ignoreCase = true) ||
            traitName.contains("analytical", ignoreCase = true) -> TraitCategory.COGNITIVE

            traitName.contains("social", ignoreCase = true) ||
            traitName.contains("friendly", ignoreCase = true) -> TraitCategory.SOCIAL

            traitName.contains("ethic", ignoreCase = true) ||
            traitName.contains("moral", ignoreCase = true) -> TraitCategory.ETHICAL

            else -> TraitCategory.CORE
        }
    }
}

/**
 * Events emitted by the Persona Bus
 */
sealed class PersonaBusEvent {
    data class DriverRegistered(val personaId: String, val spec: PersonaSpec) : PersonaBusEvent()
    data class DriverUnregistered(val personaId: String) : PersonaBusEvent()
    data class PersonaLoaded(val personaId: String, val spec: PersonaSpec) : PersonaBusEvent()
    data class PersonaSwitched(val fromId: String?, val toId: String) : PersonaBusEvent()
    data class StateUpdated(val personaId: String, val state: PersonaState) : PersonaBusEvent()
}
