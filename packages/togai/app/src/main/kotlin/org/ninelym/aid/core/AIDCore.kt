/**
 * AIDCore.kt - AI-ID Virtual Neural Processing Unit
 *
 * The AI-ID (Artificial Intelligence Identity) module serves as a virtual NPU
 * that interfaces with the application framework like a hardware device with drivers.
 * This is the central processing core that:
 * - Manages persona/character identity abstraction
 * - Routes requests through the integration hub
 * - Maintains the self-kernel for AI consciousness
 * - Provides driver-like interfaces for personas
 *
 * Architecture: Virtual NPU Pattern
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                         AIDCore (vNPU)                         │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐    │
 * │  │ Self Kernel │←→│ Persona Bus  │←→│ Integration Hub    │    │
 * │  │ (Identity)  │  │ (Drivers)    │  │ (Endpoints)        │    │
 * │  └─────────────┘  └──────────────┘  └────────────────────┘    │
 * │         ↓                ↓                    ↓                │
 * │  ┌─────────────────────────────────────────────────────────┐  │
 * │  │              Cognitive Tensor Pipeline                   │  │
 * │  └─────────────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────────────┘
 */
package org.ninelym.aid.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Represents the current state of the AI-ID virtual NPU
 */
enum class AIDState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    PROCESSING,
    SUSPENDED,
    ERROR,
    SHUTDOWN
}

/**
 * Processing priority levels for the vNPU
 */
enum class ProcessingPriority {
    CRITICAL,    // Immediate processing, interrupt current tasks
    HIGH,        // High priority, queue at front
    NORMAL,      // Standard processing order
    LOW,         // Background processing
    IDLE         // Only process when system is idle
}

/**
 * Result of an AI-ID operation
 */
sealed class AIDResult<out T> {
    data class Success<T>(val data: T, val metadata: AIDMetadata = AIDMetadata()) : AIDResult<T>()
    data class Error(val code: AIDErrorCode, val message: String, val cause: Throwable? = null) : AIDResult<Nothing>()
    data object Pending : AIDResult<Nothing>()
    data class Cancelled(val reason: String) : AIDResult<Nothing>()
}

/**
 * Metadata attached to successful AI-ID operations
 */
data class AIDMetadata(
    val processingTimeMs: Long = 0,
    val personaId: String? = null,
    val endpointId: String? = null,
    val cognitiveTensorSignature: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Error codes for AI-ID operations
 */
enum class AIDErrorCode {
    PERSONA_NOT_FOUND,
    ENDPOINT_NOT_AVAILABLE,
    DRIVER_INITIALIZATION_FAILED,
    KERNEL_NOT_READY,
    PROCESSING_TIMEOUT,
    INTEGRATION_HUB_ERROR,
    COGNITIVE_PIPELINE_ERROR,
    INVALID_REQUEST,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    INTERNAL_ERROR
}

/**
 * Cognitive tensor for AI processing - the universal data format
 */
data class AIDTensor(
    val modality: Float = 0.5f,          // Sensory modality type (0-1)
    val depth: Float = 0.5f,             // Semantic processing depth (0-1)
    val context: Float = 0.5f,           // Contextual relevance (0-1)
    val salience: Float = 0.5f,          // Attention salience (0-1)
    val autonomyIndex: Float = 0.5f,     // Agent autonomy level (0-1)
    val identity: Float = 0.5f,          // Identity coherence (0-1)
    val emotionalValence: Float = 0.5f,  // Emotional state (-1 to 1, normalized to 0-1)
    val creativityFactor: Float = 0.5f,  // Creative processing weight (0-1)
    val ethicalConstraint: Float = 1.0f  // Ethical boundary strength (0-1, default max)
) {
    /**
     * Blend two tensors with given weight
     */
    fun blend(other: AIDTensor, weight: Float = 0.5f): AIDTensor {
        val w = weight.coerceIn(0f, 1f)
        val iw = 1f - w
        return AIDTensor(
            modality = this.modality * iw + other.modality * w,
            depth = this.depth * iw + other.depth * w,
            context = this.context * iw + other.context * w,
            salience = this.salience * iw + other.salience * w,
            autonomyIndex = this.autonomyIndex * iw + other.autonomyIndex * w,
            identity = this.identity * iw + other.identity * w,
            emotionalValence = this.emotionalValence * iw + other.emotionalValence * w,
            creativityFactor = this.creativityFactor * iw + other.creativityFactor * w,
            ethicalConstraint = maxOf(this.ethicalConstraint, other.ethicalConstraint) // Always preserve max ethical constraint
        )
    }

    /**
     * Convert to float array for neural processing
     */
    fun toFloatArray(): FloatArray = floatArrayOf(
        modality, depth, context, salience, autonomyIndex,
        identity, emotionalValence, creativityFactor, ethicalConstraint
    )

    companion object {
        const val TENSOR_DIMENSION = 9

        /**
         * Create tensor from float array
         */
        fun fromFloatArray(array: FloatArray): AIDTensor {
            require(array.size >= TENSOR_DIMENSION) { "Array must have at least $TENSOR_DIMENSION elements" }
            return AIDTensor(
                modality = array[0],
                depth = array[1],
                context = array[2],
                salience = array[3],
                autonomyIndex = array[4],
                identity = array[5],
                emotionalValence = array[6],
                creativityFactor = array[7],
                ethicalConstraint = array[8]
            )
        }

        /**
         * Create a neutral tensor
         */
        fun neutral(): AIDTensor = AIDTensor()

        /**
         * Create a high-salience tensor for attention-critical operations
         */
        fun highSalience(): AIDTensor = AIDTensor(salience = 0.9f, context = 0.8f)
    }
}

/**
 * Request to the AI-ID virtual NPU
 */
data class AIDRequest(
    val id: String = UUID.randomUUID().toString(),
    val type: AIDRequestType,
    val payload: Any?,
    val priority: ProcessingPriority = ProcessingPriority.NORMAL,
    val targetPersonaId: String? = null,
    val targetEndpoint: String? = null,
    val inputTensor: AIDTensor = AIDTensor.neutral(),
    val timeout: Long = 30000L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of requests the AI-ID can process
 */
enum class AIDRequestType {
    // Core processing
    PROCESS_INPUT,
    GENERATE_RESPONSE,
    TRANSFORM_CONTENT,

    // Persona management
    LOAD_PERSONA,
    SWITCH_PERSONA,
    UPDATE_PERSONA_STATE,
    GET_PERSONA_INFO,

    // Endpoint operations
    INVOKE_ENDPOINT,
    QUERY_ENDPOINTS,
    REGISTER_ENDPOINT,

    // Kernel operations
    INTROSPECT,
    SELF_MODIFY,
    CHECKPOINT,

    // Pipeline operations
    COGNITIVE_PROCESS,
    EMOTIONAL_MODULATE,
    ETHICAL_VALIDATE
}

/**
 * Event emitted by the AI-ID core
 */
sealed class AIDEvent {
    data class StateChanged(val oldState: AIDState, val newState: AIDState) : AIDEvent()
    data class PersonaLoaded(val personaId: String) : AIDEvent()
    data class PersonaSwitched(val fromId: String?, val toId: String) : AIDEvent()
    data class EndpointRegistered(val endpointId: String) : AIDEvent()
    data class RequestProcessed(val requestId: String, val result: AIDResult<*>) : AIDEvent()
    data class Error(val code: AIDErrorCode, val message: String) : AIDEvent()
    data class TensorUpdated(val tensor: AIDTensor) : AIDEvent()
}

/**
 * Configuration for the AI-ID core
 */
data class AIDConfiguration(
    val maxConcurrentRequests: Int = 16,
    val defaultTimeout: Long = 30000L,
    val enableCognitivePipeline: Boolean = true,
    val enableEmotionalModulation: Boolean = true,
    val enableEthicalValidation: Boolean = true,
    val tensorCacheSize: Int = 1024,
    val autoPersonaRecovery: Boolean = true,
    val debugMode: Boolean = false
)

/**
 * The AI-ID Core - Virtual Neural Processing Unit
 *
 * This is the main entry point for all AI identity and persona operations.
 * It acts as a virtual hardware device that processes AI identity operations
 * through a driver-based architecture.
 */
class AIDCore private constructor(
    private val configuration: AIDConfiguration
) {
    // State management
    private val _state = MutableStateFlow(AIDState.UNINITIALIZED)
    val state: StateFlow<AIDState> = _state.asStateFlow()

    // Event stream
    private val _events = MutableSharedFlow<AIDEvent>(replay = 16)
    val events: SharedFlow<AIDEvent> = _events.asSharedFlow()

    // Current cognitive tensor state
    private val _currentTensor = MutableStateFlow(AIDTensor.neutral())
    val currentTensor: StateFlow<AIDTensor> = _currentTensor.asStateFlow()

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Request queue
    private val requestQueue = ConcurrentHashMap<String, AIDRequest>()

    // Active persona
    private var activePersonaId: String? = null

    // Processing statistics
    private var totalRequestsProcessed: Long = 0
    private var totalProcessingTimeMs: Long = 0

    /**
     * Reference to connected components (set during initialization)
     */
    private var selfKernel: SelfKernelInterface? = null
    private var integrationHub: IntegrationHubInterface? = null
    private var personaBus: PersonaBusInterface? = null

    /**
     * Initialize the AI-ID core with connected components
     */
    suspend fun initialize(
        kernel: SelfKernelInterface,
        hub: IntegrationHubInterface,
        bus: PersonaBusInterface
    ): AIDResult<Unit> {
        if (_state.value != AIDState.UNINITIALIZED) {
            return AIDResult.Error(
                AIDErrorCode.INTERNAL_ERROR,
                "AI-ID Core already initialized"
            )
        }

        _state.value = AIDState.INITIALIZING
        emitEvent(AIDEvent.StateChanged(AIDState.UNINITIALIZED, AIDState.INITIALIZING))

        return try {
            // Connect components
            selfKernel = kernel
            integrationHub = hub
            personaBus = bus

            // Initialize subsystems
            kernel.initialize(this)
            hub.initialize(this)
            bus.initialize(this)

            _state.value = AIDState.READY
            emitEvent(AIDEvent.StateChanged(AIDState.INITIALIZING, AIDState.READY))

            AIDResult.Success(Unit, AIDMetadata())
        } catch (e: Exception) {
            _state.value = AIDState.ERROR
            emitEvent(AIDEvent.Error(AIDErrorCode.INTERNAL_ERROR, "Initialization failed: ${e.message}"))
            AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Initialization failed", e)
        }
    }

    /**
     * Process a request through the vNPU
     */
    suspend fun process(request: AIDRequest): AIDResult<Any> {
        if (_state.value != AIDState.READY && _state.value != AIDState.PROCESSING) {
            return AIDResult.Error(AIDErrorCode.KERNEL_NOT_READY, "AI-ID Core not ready")
        }

        val startTime = System.currentTimeMillis()
        requestQueue[request.id] = request

        return try {
            _state.value = AIDState.PROCESSING

            // Apply cognitive tensor transformation
            val processedTensor = if (configuration.enableCognitivePipeline) {
                processCognitivePipeline(request.inputTensor)
            } else {
                request.inputTensor
            }

            // Route request based on type
            val result = when (request.type) {
                AIDRequestType.PROCESS_INPUT,
                AIDRequestType.GENERATE_RESPONSE,
                AIDRequestType.TRANSFORM_CONTENT -> {
                    selfKernel?.process(request, processedTensor)
                        ?: AIDResult.Error(AIDErrorCode.KERNEL_NOT_READY, "Self kernel not available")
                }

                AIDRequestType.LOAD_PERSONA,
                AIDRequestType.SWITCH_PERSONA,
                AIDRequestType.UPDATE_PERSONA_STATE,
                AIDRequestType.GET_PERSONA_INFO -> {
                    personaBus?.handleRequest(request, processedTensor)
                        ?: AIDResult.Error(AIDErrorCode.DRIVER_INITIALIZATION_FAILED, "Persona bus not available")
                }

                AIDRequestType.INVOKE_ENDPOINT,
                AIDRequestType.QUERY_ENDPOINTS,
                AIDRequestType.REGISTER_ENDPOINT -> {
                    integrationHub?.handleRequest(request, processedTensor)
                        ?: AIDResult.Error(AIDErrorCode.INTEGRATION_HUB_ERROR, "Integration hub not available")
                }

                AIDRequestType.INTROSPECT,
                AIDRequestType.SELF_MODIFY,
                AIDRequestType.CHECKPOINT -> {
                    selfKernel?.introspect(request)
                        ?: AIDResult.Error(AIDErrorCode.KERNEL_NOT_READY, "Self kernel not available")
                }

                AIDRequestType.COGNITIVE_PROCESS -> {
                    AIDResult.Success(processedTensor)
                }

                AIDRequestType.EMOTIONAL_MODULATE -> {
                    val modulated = modulateEmotion(processedTensor, request.payload)
                    AIDResult.Success(modulated)
                }

                AIDRequestType.ETHICAL_VALIDATE -> {
                    val isValid = validateEthics(request.payload, processedTensor)
                    AIDResult.Success(isValid)
                }
            }

            val processingTime = System.currentTimeMillis() - startTime
            totalRequestsProcessed++
            totalProcessingTimeMs += processingTime

            // Update current tensor state
            _currentTensor.value = processedTensor

            _state.value = AIDState.READY
            emitEvent(AIDEvent.RequestProcessed(request.id, result))

            when (result) {
                is AIDResult.Success -> AIDResult.Success(
                    result.data,
                    result.metadata.copy(
                        processingTimeMs = processingTime,
                        personaId = activePersonaId,
                        cognitiveTensorSignature = processedTensor.hashCode().toString()
                    )
                )
                else -> result
            }
        } catch (e: Exception) {
            _state.value = AIDState.READY
            AIDResult.Error(AIDErrorCode.PROCESSING_TIMEOUT, "Processing failed: ${e.message}", e)
        } finally {
            requestQueue.remove(request.id)
        }
    }

    /**
     * Process tensor through cognitive pipeline
     */
    private fun processCognitivePipeline(tensor: AIDTensor): AIDTensor {
        // Apply persona modulation if active
        val personaModulated = activePersonaId?.let { personaId ->
            personaBus?.modulate(tensor, personaId) ?: tensor
        } ?: tensor

        // Apply self-kernel processing
        val kernelProcessed = selfKernel?.modulate(personaModulated) ?: personaModulated

        return kernelProcessed
    }

    /**
     * Modulate tensor with emotional context
     */
    private fun modulateEmotion(tensor: AIDTensor, emotionData: Any?): AIDTensor {
        if (!configuration.enableEmotionalModulation) return tensor

        val emotionalShift = when (emotionData) {
            is Float -> emotionData
            is Double -> emotionData.toFloat()
            is Map<*, *> -> (emotionData["valence"] as? Number)?.toFloat() ?: 0f
            else -> 0f
        }

        return tensor.copy(
            emotionalValence = (tensor.emotionalValence + emotionalShift).coerceIn(0f, 1f)
        )
    }

    /**
     * Validate request against ethical constraints
     */
    private fun validateEthics(payload: Any?, tensor: AIDTensor): Boolean {
        if (!configuration.enableEthicalValidation) return true

        // Ethical constraints are always enforced
        return tensor.ethicalConstraint >= 0.9f
    }

    /**
     * Set active persona
     */
    suspend fun setActivePersona(personaId: String): AIDResult<Unit> {
        val previousId = activePersonaId

        val loadResult = personaBus?.loadPersona(personaId)
        if (loadResult is AIDResult.Success) {
            activePersonaId = personaId
            emitEvent(AIDEvent.PersonaSwitched(previousId, personaId))
            return AIDResult.Success(Unit)
        }

        return loadResult as? AIDResult<Unit>
            ?: AIDResult.Error(AIDErrorCode.PERSONA_NOT_FOUND, "Failed to load persona: $personaId")
    }

    /**
     * Get current active persona ID
     */
    fun getActivePersonaId(): String? = activePersonaId

    /**
     * Get processing statistics
     */
    fun getStatistics(): Map<String, Any> = mapOf(
        "totalRequests" to totalRequestsProcessed,
        "totalProcessingTimeMs" to totalProcessingTimeMs,
        "averageProcessingTimeMs" to if (totalRequestsProcessed > 0)
            totalProcessingTimeMs / totalRequestsProcessed else 0,
        "activePersonaId" to (activePersonaId ?: "none"),
        "currentState" to _state.value.name,
        "pendingRequests" to requestQueue.size
    )

    /**
     * Shutdown the AI-ID core
     */
    suspend fun shutdown() {
        _state.value = AIDState.SHUTDOWN
        emitEvent(AIDEvent.StateChanged(AIDState.READY, AIDState.SHUTDOWN))

        selfKernel?.shutdown()
        integrationHub?.shutdown()
        personaBus?.shutdown()

        scope.cancel()
    }

    private suspend fun emitEvent(event: AIDEvent) {
        _events.emit(event)
    }

    companion object {
        @Volatile
        private var instance: AIDCore? = null

        /**
         * Get or create the singleton AI-ID Core instance
         */
        fun getInstance(configuration: AIDConfiguration = AIDConfiguration()): AIDCore {
            return instance ?: synchronized(this) {
                instance ?: AIDCore(configuration).also { instance = it }
            }
        }

        /**
         * Reset the singleton instance (for testing)
         */
        fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }
}

/**
 * Interface for the Self Kernel component
 */
interface SelfKernelInterface {
    suspend fun initialize(core: AIDCore)
    suspend fun process(request: AIDRequest, tensor: AIDTensor): AIDResult<Any>
    suspend fun introspect(request: AIDRequest): AIDResult<Any>
    fun modulate(tensor: AIDTensor): AIDTensor
    suspend fun shutdown()
}

/**
 * Interface for the Integration Hub component
 */
interface IntegrationHubInterface {
    suspend fun initialize(core: AIDCore)
    suspend fun handleRequest(request: AIDRequest, tensor: AIDTensor): AIDResult<Any>
    suspend fun shutdown()
}

/**
 * Interface for the Persona Bus component
 */
interface PersonaBusInterface {
    suspend fun initialize(core: AIDCore)
    suspend fun handleRequest(request: AIDRequest, tensor: AIDTensor): AIDResult<Any>
    suspend fun loadPersona(personaId: String): AIDResult<Unit>
    fun modulate(tensor: AIDTensor, personaId: String): AIDTensor
    suspend fun shutdown()
}
