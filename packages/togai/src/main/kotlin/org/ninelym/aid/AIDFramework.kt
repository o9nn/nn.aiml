/**
 * AIDFramework.kt - AI-ID Framework Main Entry Point
 *
 * The AID (Artificial Intelligence Identity) Framework is a generalized,
 * character-agnostic AI assistant framework. It provides:
 *
 * - Virtual NPU architecture for AI processing
 * - Persona driver system (like hardware drivers for AI personalities)
 * - Integration hub with service endpoints
 * - Self kernel for AI consciousness abstraction
 * - Complete persona factory for creating any AI character
 *
 * Architecture Overview:
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                           AID Framework                                 │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  ┌───────────────────────────────────────────────────────────────────┐ │
 * │  │                        AIDCore (vNPU)                              │ │
 * │  │  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐   │ │
 * │  │  │ Self Kernel │  │ Persona Bus  │  │ Integration Hub        │   │ │
 * │  │  │ (Identity)  │  │ (Drivers)    │  │ (Endpoints)            │   │ │
 * │  │  └──────┬──────┘  └──────┬───────┘  └───────────┬────────────┘   │ │
 * │  │         │                │                      │                │ │
 * │  │         └────────────────┼──────────────────────┘                │ │
 * │  │                          ↓                                       │ │
 * │  │  ┌───────────────────────────────────────────────────────────┐  │ │
 * │  │  │              Cognitive Tensor Pipeline                     │  │ │
 * │  │  └───────────────────────────────────────────────────────────┘  │ │
 * │  └───────────────────────────────────────────────────────────────────┘ │
 * │                                                                         │
 * │  ┌───────────────────────────────────────────────────────────────────┐ │
 * │  │                      Service Layer                                 │ │
 * │  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐          │ │
 * │  │  │Inference│ │Voice   │ │Image   │ │Document│ │Memory  │   ...   │ │
 * │  │  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘          │ │
 * │  └───────────────────────────────────────────────────────────────────┘ │
 * │                                                                         │
 * │  ┌───────────────────────────────────────────────────────────────────┐ │
 * │  │                      Persona Layer                                 │ │
 * │  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                      │ │
 * │  │  │Helper  │ │Creative│ │Analyst │ │Custom  │   [Any Persona]     │ │
 * │  │  └────────┘ └────────┘ └────────┘ └────────┘                      │ │
 * │  └───────────────────────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Usage:
 * ```kotlin
 * // Initialize the framework
 * val framework = AIDFramework.create()
 * framework.initialize()
 *
 * // Create and register a persona
 * val myPersona = PersonaFactory.create("assistant") {
 *     archetype(PersonaArchetype.HELPER)
 *     coreTrait("helpfulness", 0.9f)
 * }
 * framework.registerPersona(myPersona)
 * framework.setActivePersona("assistant")
 *
 * // Process input through the framework
 * val result = framework.process("Hello, how can you help me?")
 * ```
 */
package org.ninelym.aid

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.ninelym.aid.core.*
import org.ninelym.aid.driver.*
import org.ninelym.aid.endpoint.*
import org.ninelym.aid.hub.*
import org.ninelym.aid.kernel.*
import org.ninelym.aid.persona.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Framework state
 */
enum class FrameworkState {
    CREATED,
    INITIALIZING,
    READY,
    PROCESSING,
    ERROR,
    SHUTDOWN
}

/**
 * Framework event
 */
sealed class FrameworkEvent {
    data class StateChanged(val oldState: FrameworkState, val newState: FrameworkState) : FrameworkEvent()
    data class PersonaRegistered(val personaId: String) : FrameworkEvent()
    data class PersonaActivated(val personaId: String) : FrameworkEvent()
    data class EndpointRegistered(val endpointName: String) : FrameworkEvent()
    data class Error(val message: String, val cause: Throwable? = null) : FrameworkEvent()
    data class ProcessingComplete(val requestId: String, val success: Boolean) : FrameworkEvent()
}

/**
 * Framework configuration
 */
data class FrameworkConfiguration(
    val aidConfiguration: AIDConfiguration = AIDConfiguration(),
    val autoRegisterDefaultPersonas: Boolean = true,
    val autoRegisterDefaultEndpoints: Boolean = true,
    val enableCognitiveIntegration: Boolean = true,
    val debugMode: Boolean = false
)

/**
 * Main entry point for the AID Framework
 */
class AIDFramework private constructor(
    private val configuration: FrameworkConfiguration
) {
    // Core components
    private lateinit var aidCore: AIDCore
    private lateinit var selfKernel: SelfKernel
    private lateinit var integrationHub: IntegrationHub
    private lateinit var personaBus: PersonaBus

    // State management
    private val _state = MutableStateFlow(FrameworkState.CREATED)
    val state: StateFlow<FrameworkState> = _state.asStateFlow()

    // Events
    private val _events = MutableSharedFlow<FrameworkEvent>(replay = 32)
    val events: SharedFlow<FrameworkEvent> = _events.asSharedFlow()

    // Initialization flag
    private val isInitialized = AtomicBoolean(false)

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Initialize the framework
     */
    suspend fun initialize(): AIDResult<Unit> {
        if (isInitialized.get()) {
            return AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Framework already initialized")
        }

        _state.value = FrameworkState.INITIALIZING
        emitEvent(FrameworkEvent.StateChanged(FrameworkState.CREATED, FrameworkState.INITIALIZING))

        return try {
            // Create core components
            aidCore = AIDCore.getInstance(configuration.aidConfiguration)
            selfKernel = SelfKernel()
            integrationHub = IntegrationHub()
            personaBus = PersonaBus()

            // Initialize the AI-ID core with all components
            val coreResult = aidCore.initialize(selfKernel, integrationHub, personaBus)
            if (coreResult is AIDResult.Error) {
                throw Exception("Core initialization failed: ${coreResult.message}")
            }

            // Register default endpoints if configured
            if (configuration.autoRegisterDefaultEndpoints) {
                registerDefaultEndpoints()
            }

            // Register default personas if configured
            if (configuration.autoRegisterDefaultPersonas) {
                registerDefaultPersonas()
            }

            isInitialized.set(true)
            _state.value = FrameworkState.READY
            emitEvent(FrameworkEvent.StateChanged(FrameworkState.INITIALIZING, FrameworkState.READY))

            AIDResult.Success(Unit)
        } catch (e: Exception) {
            _state.value = FrameworkState.ERROR
            emitEvent(FrameworkEvent.Error("Initialization failed: ${e.message}", e))
            AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Framework initialization failed: ${e.message}", e)
        }
    }

    /**
     * Process input through the framework
     */
    suspend fun process(
        input: String,
        context: Map<String, Any> = emptyMap(),
        priority: ProcessingPriority = ProcessingPriority.NORMAL
    ): AIDResult<ProcessingOutput> {
        ensureInitialized()

        _state.value = FrameworkState.PROCESSING

        return try {
            // Get current tensor state
            val inputTensor = aidCore.currentTensor.value

            // Process through persona if active
            val personaOutput = personaBus.process(input, inputTensor, context)

            // Get the output tensor
            val outputTensor = when (personaOutput) {
                is AIDResult.Success -> personaOutput.data.tensor
                else -> inputTensor
            }

            // Create processing request
            val request = AIDRequest(
                type = AIDRequestType.PROCESS_INPUT,
                payload = mapOf(
                    "input" to input,
                    "context" to context,
                    "personaOutput" to personaOutput
                ),
                priority = priority,
                inputTensor = outputTensor
            )

            // Process through the core
            val coreResult = aidCore.process(request)

            _state.value = FrameworkState.READY

            when (coreResult) {
                is AIDResult.Success -> {
                    val output = ProcessingOutput(
                        input = input,
                        personaResponse = (personaOutput as? AIDResult.Success)?.data?.content ?: input,
                        tensor = outputTensor,
                        emotionalState = (personaOutput as? AIDResult.Success)?.data?.emotionalState,
                        metadata = coreResult.metadata,
                        processingResult = coreResult.data
                    )
                    emitEvent(FrameworkEvent.ProcessingComplete(request.id, true))
                    AIDResult.Success(output)
                }
                is AIDResult.Error -> {
                    emitEvent(FrameworkEvent.ProcessingComplete(request.id, false))
                    coreResult
                }
                else -> {
                    AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Unexpected processing result")
                }
            }
        } catch (e: Exception) {
            _state.value = FrameworkState.READY
            AIDResult.Error(AIDErrorCode.PROCESSING_TIMEOUT, "Processing failed: ${e.message}", e)
        }
    }

    /**
     * Invoke an endpoint directly
     */
    suspend fun invokeEndpoint(
        endpointName: String,
        action: String,
        parameters: Map<String, Any> = emptyMap()
    ): AIDResult<Any> {
        ensureInitialized()

        val endpoint = integrationHub.getEndpoints().find { it.name == endpointName }
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "Endpoint not found: $endpointName")

        val tensor = personaBus.getActiveDriver()?.let {
            it.modulate(aidCore.currentTensor.value)
        } ?: aidCore.currentTensor.value

        return integrationHub.invoke(endpoint.id, action, parameters, tensor)
    }

    // ============= Persona Management =============

    /**
     * Register a persona specification
     */
    suspend fun registerPersona(spec: PersonaSpec): AIDResult<Unit> {
        ensureInitialized()
        val result = personaBus.registerPersona(spec)
        if (result is AIDResult.Success) {
            emitEvent(FrameworkEvent.PersonaRegistered(spec.id))
        }
        return result
    }

    /**
     * Register a persona driver
     */
    suspend fun registerPersonaDriver(driver: PersonaDriver): AIDResult<Unit> {
        ensureInitialized()
        val result = personaBus.registerDriver(driver)
        if (result is AIDResult.Success) {
            emitEvent(FrameworkEvent.PersonaRegistered(driver.spec.id))
        }
        return result
    }

    /**
     * Set active persona by ID
     */
    suspend fun setActivePersona(personaId: String): AIDResult<Unit> {
        ensureInitialized()
        val result = aidCore.setActivePersona(personaId)
        if (result is AIDResult.Success) {
            emitEvent(FrameworkEvent.PersonaActivated(personaId))
        }
        return result
    }

    /**
     * Set active persona by name
     */
    suspend fun setActivePersonaByName(name: String): AIDResult<Unit> {
        ensureInitialized()
        val persona = personaBus.getAllPersonas().find { it.name.equals(name, ignoreCase = true) }
            ?: return AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Persona not found: $name")
        return setActivePersona(persona.id)
    }

    /**
     * Get all registered personas
     */
    fun getPersonas(): List<PersonaSpec> {
        return if (isInitialized.get()) personaBus.getAllPersonas() else emptyList()
    }

    /**
     * Get active persona
     */
    fun getActivePersona(): PersonaSpec? {
        return if (isInitialized.get()) personaBus.getActiveDriver()?.spec else null
    }

    /**
     * Create and register a custom persona
     */
    suspend fun createPersona(
        name: String,
        configure: PersonaBuilder.() -> Unit
    ): AIDResult<PersonaSpec> {
        ensureInitialized()
        val spec = PersonaFactory.create(name, configure)
        val result = registerPersona(spec)
        return when (result) {
            is AIDResult.Success -> AIDResult.Success(spec)
            is AIDResult.Error -> AIDResult.Error(result.code, result.message)
            else -> AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Unexpected result")
        }
    }

    // ============= Endpoint Management =============

    /**
     * Register a custom endpoint
     */
    suspend fun registerEndpoint(
        name: String,
        description: String,
        capabilities: Set<ServiceCapability>,
        handler: EndpointHandler
    ): AIDResult<ServiceEndpoint> {
        ensureInitialized()
        val result = integrationHub.registerEndpoint(name, description, capabilities, handler)
        if (result is AIDResult.Success) {
            emitEvent(FrameworkEvent.EndpointRegistered(name))
        }
        return result
    }

    /**
     * Get all registered endpoints
     */
    fun getEndpoints(): List<ServiceEndpoint> {
        return if (isInitialized.get()) integrationHub.getEndpoints() else emptyList()
    }

    /**
     * Get endpoints by capability
     */
    fun getEndpointsByCapability(capability: ServiceCapability): List<ServiceEndpoint> {
        return if (isInitialized.get()) integrationHub.getEndpointsByCapability(capability) else emptyList()
    }

    // ============= Self Kernel Access =============

    /**
     * Introspect the self kernel
     */
    suspend fun introspect(): AIDResult<IntrospectionReport> {
        ensureInitialized()
        val request = AIDRequest(
            type = AIDRequestType.INTROSPECT,
            payload = null
        )
        val result = aidCore.process(request)
        return when (result) {
            is AIDResult.Success -> {
                val report = result.data as? IntrospectionReport
                if (report != null) AIDResult.Success(report)
                else AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Invalid introspection result")
            }
            is AIDResult.Error -> result
            else -> AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Unexpected result")
        }
    }

    /**
     * Get current identity state
     */
    fun getIdentityState(): IdentityState? {
        return if (isInitialized.get()) selfKernel.getIdentityState() else null
    }

    /**
     * Get current cognitive tensor
     */
    fun getCurrentTensor(): AIDTensor? {
        return if (isInitialized.get()) aidCore.currentTensor.value else null
    }

    // ============= Statistics & Monitoring =============

    /**
     * Get framework statistics
     */
    fun getStatistics(): FrameworkStatistics {
        if (!isInitialized.get()) {
            return FrameworkStatistics.empty()
        }

        return FrameworkStatistics(
            coreStats = aidCore.getStatistics(),
            hubStats = integrationHub.getStatistics(),
            kernelStats = selfKernel.getMemoryStats(),
            personaCount = personaBus.getAllPersonas().size,
            activePersonaId = aidCore.getActivePersonaId(),
            frameworkState = _state.value
        )
    }

    // ============= Shutdown =============

    /**
     * Shutdown the framework
     */
    suspend fun shutdown() {
        if (!isInitialized.get()) return

        _state.value = FrameworkState.SHUTDOWN
        emitEvent(FrameworkEvent.StateChanged(FrameworkState.READY, FrameworkState.SHUTDOWN))

        aidCore.shutdown()
        scope.cancel()
        isInitialized.set(false)
    }

    // ============= Private helpers =============

    private fun ensureInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("Framework not initialized. Call initialize() first.")
        }
    }

    private suspend fun registerDefaultEndpoints() {
        EndpointFactory.createAllEndpoints().forEach { endpoint ->
            val result = integrationHub.registerEndpoint(
                name = endpoint.endpointName,
                description = endpoint.endpointDescription,
                capabilities = endpoint.endpointCapabilities,
                handler = endpoint
            )
            if (result is AIDResult.Success) {
                emitEvent(FrameworkEvent.EndpointRegistered(endpoint.endpointName))
            }
        }
    }

    private suspend fun registerDefaultPersonas() {
        // Register preset personas
        listOf(
            PersonaFactory.createLayla(),
            PersonaFactory.createAria(),
            PersonaFactory.createMarcus()
        ).forEach { spec ->
            registerPersona(spec)
        }
    }

    private suspend fun emitEvent(event: FrameworkEvent) {
        _events.emit(event)
    }

    companion object {
        /**
         * Create a new AID Framework instance
         */
        fun create(configuration: FrameworkConfiguration = FrameworkConfiguration()): AIDFramework {
            return AIDFramework(configuration)
        }

        /**
         * Create with default configuration
         */
        fun createDefault(): AIDFramework = create()

        /**
         * Create with custom configuration via builder
         */
        fun create(configure: FrameworkConfigurationBuilder.() -> Unit): AIDFramework {
            val builder = FrameworkConfigurationBuilder()
            builder.configure()
            return create(builder.build())
        }
    }
}

/**
 * Builder for framework configuration
 */
class FrameworkConfigurationBuilder {
    private var autoRegisterDefaultPersonas = true
    private var autoRegisterDefaultEndpoints = true
    private var enableCognitiveIntegration = true
    private var debugMode = false
    private var maxConcurrentRequests = 16
    private var defaultTimeout = 30000L

    fun autoRegisterDefaultPersonas(value: Boolean) = apply { autoRegisterDefaultPersonas = value }
    fun autoRegisterDefaultEndpoints(value: Boolean) = apply { autoRegisterDefaultEndpoints = value }
    fun enableCognitiveIntegration(value: Boolean) = apply { enableCognitiveIntegration = value }
    fun debugMode(value: Boolean) = apply { debugMode = value }
    fun maxConcurrentRequests(value: Int) = apply { maxConcurrentRequests = value }
    fun defaultTimeout(value: Long) = apply { defaultTimeout = value }

    fun build(): FrameworkConfiguration = FrameworkConfiguration(
        aidConfiguration = AIDConfiguration(
            maxConcurrentRequests = maxConcurrentRequests,
            defaultTimeout = defaultTimeout,
            debugMode = debugMode
        ),
        autoRegisterDefaultPersonas = autoRegisterDefaultPersonas,
        autoRegisterDefaultEndpoints = autoRegisterDefaultEndpoints,
        enableCognitiveIntegration = enableCognitiveIntegration,
        debugMode = debugMode
    )
}

/**
 * Output from framework processing
 */
data class ProcessingOutput(
    val input: String,
    val personaResponse: String,
    val tensor: AIDTensor,
    val emotionalState: EmotionalState?,
    val metadata: AIDMetadata,
    val processingResult: Any?
)

/**
 * Framework statistics
 */
data class FrameworkStatistics(
    val coreStats: Map<String, Any>,
    val hubStats: HubStatistics,
    val kernelStats: MemoryStats,
    val personaCount: Int,
    val activePersonaId: String?,
    val frameworkState: FrameworkState
) {
    companion object {
        fun empty() = FrameworkStatistics(
            coreStats = emptyMap(),
            hubStats = HubStatistics(0, 0, 0, 0, 0, emptyMap(), 0),
            kernelStats = MemoryStats(0, 0, 0f, 0, 0),
            personaCount = 0,
            activePersonaId = null,
            frameworkState = FrameworkState.CREATED
        )
    }
}

// ============= Convenience Extensions =============

/**
 * Quick initialization and setup
 */
suspend fun AIDFramework.quickSetup(
    defaultPersonaName: String = "layla",
    additionalPersonas: List<PersonaSpec> = emptyList()
): AIDResult<Unit> {
    val initResult = initialize()
    if (initResult is AIDResult.Error) return initResult

    additionalPersonas.forEach { registerPersona(it) }

    return setActivePersonaByName(defaultPersonaName)
}
