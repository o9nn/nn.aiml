/**
 * ServiceAdapter.kt - Bridge between existing services and AID Framework
 *
 * This adapter allows seamless integration of existing Layla/framework services
 * with the new AI-ID architecture. It provides:
 *
 * - Wrapping of legacy services as AID endpoints
 * - Migration of old character system to new persona drivers
 * - Compatibility layer for gradual migration
 */
package org.ninelym.aid.adapter

import org.ninelym.aid.core.*
import org.ninelym.aid.driver.*
import org.ninelym.aid.hub.*
import org.ninelym.aid.endpoint.*
import org.ninelym.aid.persona.*

/**
 * Adapter for wrapping any service as an AID endpoint
 */
abstract class ServiceAdapter<T>(
    private val service: T,
    override val endpointName: String,
    override val endpointDescription: String,
    override val endpointCapabilities: Set<ServiceCapability>
) : BaseEndpointAdapter(endpointName, endpointDescription, endpointCapabilities) {

    /**
     * Get the underlying service
     */
    fun getService(): T = service

    /**
     * Override to provide custom initialization logic
     */
    override suspend fun doInitialize(): AIDResult<Unit> {
        return try {
            initializeService(service)
            AIDResult.Success(Unit)
        } catch (e: Exception) {
            AIDResult.Error(
                AIDErrorCode.DRIVER_INITIALIZATION_FAILED,
                "Failed to initialize ${endpointName}: ${e.message}",
                e
            )
        }
    }

    /**
     * Override to provide custom shutdown logic
     */
    override suspend fun doShutdown() {
        shutdownService(service)
    }

    /**
     * Subclasses implement this to initialize the underlying service
     */
    protected open suspend fun initializeService(service: T) {}

    /**
     * Subclasses implement this to shutdown the underlying service
     */
    protected open suspend fun shutdownService(service: T) {}
}

/**
 * Generic lambda-based service adapter for quick wrapping
 */
class LambdaServiceAdapter(
    endpointName: String,
    endpointDescription: String,
    endpointCapabilities: Set<ServiceCapability>,
    private val initHandler: suspend () -> AIDResult<Unit> = { AIDResult.Success(Unit) },
    private val requestHandler: suspend (EndpointRequest, AIDTensor) -> AIDResult<Any>,
    private val shutdownHandler: suspend () -> Unit = {}
) : BaseEndpointAdapter(endpointName, endpointDescription, endpointCapabilities) {

    override suspend fun doInitialize(): AIDResult<Unit> = initHandler()

    override suspend fun doHandle(request: EndpointRequest, tensor: AIDTensor): AIDResult<Any> {
        return requestHandler(request, tensor)
    }

    override suspend fun doShutdown() = shutdownHandler()
}

/**
 * Builder for creating service adapters
 */
class ServiceAdapterBuilder(private val name: String) {
    private var description: String = ""
    private var capabilities: Set<ServiceCapability> = emptySet()
    private var initHandler: suspend () -> AIDResult<Unit> = { AIDResult.Success(Unit) }
    private var requestHandler: suspend (EndpointRequest, AIDTensor) -> AIDResult<Any> = { _, _ ->
        AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "No handler defined")
    }
    private var shutdownHandler: suspend () -> Unit = {}

    fun description(desc: String) = apply { description = desc }

    fun capabilities(vararg caps: ServiceCapability) = apply {
        capabilities = caps.toSet()
    }

    fun onInit(handler: suspend () -> AIDResult<Unit>) = apply {
        initHandler = handler
    }

    fun onRequest(handler: suspend (EndpointRequest, AIDTensor) -> AIDResult<Any>) = apply {
        requestHandler = handler
    }

    fun onShutdown(handler: suspend () -> Unit) = apply {
        shutdownHandler = handler
    }

    fun build(): LambdaServiceAdapter = LambdaServiceAdapter(
        endpointName = name,
        endpointDescription = description,
        endpointCapabilities = capabilities,
        initHandler = initHandler,
        requestHandler = requestHandler,
        shutdownHandler = shutdownHandler
    )
}

/**
 * Factory function for creating service adapters
 */
fun createServiceAdapter(
    name: String,
    configure: ServiceAdapterBuilder.() -> Unit
): LambdaServiceAdapter {
    return ServiceAdapterBuilder(name).apply(configure).build()
}

// ============= Legacy Character Migration =============

/**
 * Adapter for migrating legacy Character objects to PersonaDriver
 */
class LegacyCharacterAdapter(
    private val name: String,
    private val personalityTraits: Map<String, Float>,
    private val responseTemplates: List<String> = emptyList(),
    private val emotionalResponses: Map<String, Float> = emptyMap()
) : PersonaDriver {

    private val _spec: PersonaSpec = createSpecFromLegacy()
    private var _state: PersonaState = PersonaState(specId = _spec.id, currentTensor = _spec.toTensor())

    override val spec: PersonaSpec get() = _spec
    override val state: PersonaState get() = _state

    override suspend fun initialize(): AIDResult<Unit> {
        _state = PersonaState(specId = _spec.id, currentTensor = _spec.toTensor())
        return AIDResult.Success(Unit)
    }

    override suspend fun process(
        input: String,
        inputTensor: AIDTensor,
        context: Map<String, Any>
    ): AIDResult<PersonaOutput> {
        val modulatedTensor = modulate(inputTensor)
        val processedContent = applyLegacyPatterns(input)

        val output = PersonaOutput(
            content = processedContent,
            tensor = modulatedTensor,
            emotionalState = _state.emotionalState
        )

        return AIDResult.Success(output)
    }

    override fun modulate(tensor: AIDTensor): AIDTensor {
        val personaTensor = _spec.toTensor()
        return tensor.blend(personaTensor, 0.4f)
    }

    override suspend fun applyStimulus(
        stimulus: String,
        intensity: Float,
        category: TraitCategory
    ): EmotionalState {
        val currentEmotion = _state.emotionalState

        // Use legacy emotional response mappings if available
        val emotionalShift = emotionalResponses.entries
            .filter { stimulus.contains(it.key, ignoreCase = true) }
            .map { it.value }
            .sum()

        val newValence = (currentEmotion.valence + emotionalShift * intensity).coerceIn(0f, 1f)
        val newState = currentEmotion.copy(valence = newValence, lastUpdate = System.currentTimeMillis())

        _state = _state.copy(emotionalState = newState)
        return newState
    }

    override suspend fun updateMemory(context: PersonaMemoryContext) {
        _state = _state.copy(memoryContext = context)
    }

    override fun getCapabilities(): Set<DriverCapability> = setOf(
        DriverCapability.TEXT_PROCESSING,
        DriverCapability.EMOTIONAL_MODULATION
    )

    override suspend fun shutdown() {}

    private fun createSpecFromLegacy(): PersonaSpec {
        return PersonaFactory.fromLegacyCharacter(
            name = name,
            personalityTraits = personalityTraits,
            responseTemplates = responseTemplates
        )
    }

    private fun applyLegacyPatterns(input: String): String {
        if (responseTemplates.isEmpty()) return input

        // Apply legacy response templates if available
        val template = responseTemplates.randomOrNull()
        return if (template != null && Math.random() < 0.3) {
            "$template $input"
        } else {
            input
        }
    }
}

/**
 * Factory for creating legacy character adapters
 */
object LegacyCharacterFactory {

    /**
     * Create adapter from legacy character data
     */
    fun fromLegacy(
        name: String,
        traits: Map<String, Float>,
        templates: List<String> = emptyList()
    ): LegacyCharacterAdapter {
        return LegacyCharacterAdapter(name, traits, templates)
    }

    /**
     * Migrate a legacy Layla character
     */
    fun migrateLayla(): LegacyCharacterAdapter {
        return fromLegacy(
            name = "Layla",
            traits = mapOf(
                "helpfulness" to 0.9f,
                "empathy" to 0.8f,
                "patience" to 0.9f,
                "knowledge" to 0.85f
            ),
            templates = listOf(
                "I'd be happy to help with that!",
                "Let me assist you.",
                "Of course!"
            )
        )
    }

    /**
     * Migrate a legacy Aria character
     */
    fun migrateAria(): LegacyCharacterAdapter {
        return fromLegacy(
            name = "Aria",
            traits = mapOf(
                "creativity" to 0.95f,
                "imagination" to 0.9f,
                "playfulness" to 0.8f,
                "expressiveness" to 0.85f
            ),
            templates = listOf(
                "Ooh, that's interesting!",
                "Let's explore this together!",
                "How creative!"
            )
        )
    }

    /**
     * Migrate a legacy Marcus character
     */
    fun migrateMarcus(): LegacyCharacterAdapter {
        return fromLegacy(
            name = "Marcus",
            traits = mapOf(
                "logic" to 0.95f,
                "precision" to 0.85f,
                "analytical" to 0.9f,
                "objectivity" to 0.85f
            ),
            templates = listOf(
                "Analyzing the data...",
                "From a logical perspective,",
                "The evidence suggests"
            )
        )
    }

    /**
     * Migrate a legacy Toga character
     */
    fun migrateToga(): LegacyCharacterAdapter {
        return fromLegacy(
            name = "Toga",
            traits = mapOf(
                "cheerfulness" to 0.95f,
                "playfulness" to 0.9f,
                "chaos" to 0.85f,
                "expressiveness" to 0.9f,
                "noActualHarm" to 1.0f,
                "respectBoundaries" to 1.0f
            ),
            templates = listOf(
                "Ehehe~",
                "Ooh!",
                "How cute~"
            )
        )
    }
}

// ============= Unified Assistant Interface =============

/**
 * Unified assistant interface that works with any persona
 * This provides a simplified API for applications that just need
 * basic assistant functionality
 */
class UnifiedAssistant(
    private val framework: org.ninelym.aid.AIDFramework
) {
    /**
     * Process a message and get a response
     */
    suspend fun chat(message: String): AIDResult<String> {
        val result = framework.process(message)
        return when (result) {
            is AIDResult.Success -> AIDResult.Success(result.data.personaResponse)
            is AIDResult.Error -> result
            else -> AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Unexpected result")
        }
    }

    /**
     * Switch to a different persona
     */
    suspend fun switchPersona(name: String): AIDResult<Unit> {
        return framework.setActivePersonaByName(name)
    }

    /**
     * Get available personas
     */
    fun getAvailablePersonas(): List<String> {
        return framework.getPersonas().map { it.displayName }
    }

    /**
     * Get current persona
     */
    fun getCurrentPersona(): String? {
        return framework.getActivePersona()?.displayName
    }

    /**
     * Generate an image
     */
    suspend fun generateImage(prompt: String): AIDResult<Any> {
        return framework.invokeEndpoint("image", "generate", mapOf("prompt" to prompt))
    }

    /**
     * Synthesize speech
     */
    suspend fun speak(text: String): AIDResult<Any> {
        return framework.invokeEndpoint("voice", "synthesize", mapOf("text" to text))
    }

    /**
     * Process a document
     */
    suspend fun summarizeDocument(path: String): AIDResult<Any> {
        val loadResult = framework.invokeEndpoint("document", "load", mapOf("path" to path))
        if (loadResult is AIDResult.Error) return loadResult

        val docId = (loadResult as AIDResult.Success).data as? Map<*, *>
        val documentId = docId?.get("documentId") as? String ?: return loadResult

        return framework.invokeEndpoint("document", "summarize", mapOf("documentId" to documentId))
    }
}
