/**
 * IntegrationHub.kt - Central Service Endpoint Registry
 *
 * The Integration Hub acts as the central nervous system connecting all
 * application services to the AI-ID core. It provides:
 * - Service endpoint registration and discovery
 * - Request routing and load balancing
 * - Capability negotiation
 * - Service health monitoring
 * - Tensor-aware request transformation
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                      Integration Hub                            │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐    │
 * │  │ Endpoint    │  │ Router       │  │ Health             │    │
 * │  │ Registry    │  │ Engine       │  │ Monitor            │    │
 * │  └─────────────┘  └──────────────┘  └────────────────────┘    │
 * │         ↓                ↓                    ↓                │
 * │  ┌─────────────────────────────────────────────────────────┐  │
 * │  │              Service Endpoint Layer                      │  │
 * │  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │  │
 * │  │  │Inference│ │Voice   │ │Image   │ │Document│   ...     │  │
 * │  │  └────────┘ └────────┘ └────────┘ └────────┘            │  │
 * │  └─────────────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────────────┘
 */
package org.ninelym.aid.hub

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ninelym.aid.core.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Capability categories for services
 */
enum class ServiceCapability {
    // Input/Output
    TEXT_INPUT,
    TEXT_OUTPUT,
    VOICE_INPUT,
    VOICE_OUTPUT,
    IMAGE_INPUT,
    IMAGE_OUTPUT,
    DOCUMENT_INPUT,

    // Processing
    INFERENCE,
    GENERATION,
    TRANSFORMATION,
    ANALYSIS,

    // Storage
    MEMORY,
    PERSISTENCE,
    CACHING,

    // Integration
    CALENDAR,
    NOTIFICATIONS,
    AUTOMATION,
    CLOUD_SYNC,

    // Security
    AUTHENTICATION,
    PRIVACY,
    ENCRYPTION,

    // Monitoring
    ANALYTICS,
    PERFORMANCE,
    HEALTH_CHECK
}

/**
 * Endpoint state
 */
enum class EndpointState {
    REGISTERED,
    INITIALIZING,
    READY,
    BUSY,
    DEGRADED,
    ERROR,
    OFFLINE
}

/**
 * Service endpoint definition
 */
data class ServiceEndpoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val capabilities: Set<ServiceCapability>,
    val state: EndpointState = EndpointState.REGISTERED,
    val priority: Int = 50, // 0-100, higher = more priority
    val metadata: Map<String, Any> = emptyMap(),
    val registeredAt: Long = System.currentTimeMillis(),
    val lastHealthCheck: Long = System.currentTimeMillis(),
    val healthScore: Float = 1.0f,
    val invocationCount: Long = 0,
    val errorCount: Long = 0
) {
    /**
     * Calculate endpoint reliability score
     */
    fun reliabilityScore(): Float {
        if (invocationCount == 0L) return 1.0f
        val successRate = 1.0f - (errorCount.toFloat() / invocationCount)
        return successRate * healthScore
    }
}

/**
 * Endpoint handler interface - implemented by services to handle requests
 */
interface EndpointHandler {
    /**
     * Initialize the handler
     */
    suspend fun initialize(): AIDResult<Unit>

    /**
     * Handle a request
     */
    suspend fun handle(
        request: EndpointRequest,
        tensor: AIDTensor
    ): AIDResult<Any>

    /**
     * Get current health status
     */
    fun getHealth(): EndpointHealth

    /**
     * Shutdown the handler
     */
    suspend fun shutdown()
}

/**
 * Request to an endpoint
 */
data class EndpointRequest(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val payload: Any?,
    val parameters: Map<String, Any> = emptyMap(),
    val timeout: Long = 30000L,
    val priority: ProcessingPriority = ProcessingPriority.NORMAL,
    val requireCapabilities: Set<ServiceCapability> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Health status of an endpoint
 */
data class EndpointHealth(
    val healthy: Boolean,
    val state: EndpointState,
    val latencyMs: Long = 0,
    val lastError: String? = null,
    val uptime: Long = 0,
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * Hub event for monitoring
 */
sealed class HubEvent {
    data class EndpointRegistered(val endpoint: ServiceEndpoint) : HubEvent()
    data class EndpointUnregistered(val endpointId: String) : HubEvent()
    data class EndpointStateChanged(val endpointId: String, val oldState: EndpointState, val newState: EndpointState) : HubEvent()
    data class RequestRouted(val requestId: String, val endpointId: String) : HubEvent()
    data class RequestCompleted(val requestId: String, val success: Boolean, val durationMs: Long) : HubEvent()
    data class HealthCheckCompleted(val healthyCount: Int, val totalCount: Int) : HubEvent()
}

/**
 * Hub statistics
 */
data class HubStatistics(
    val totalEndpoints: Int,
    val activeEndpoints: Int,
    val totalRequests: Long,
    val successfulRequests: Long,
    val averageLatencyMs: Long,
    val capabilityCoverage: Map<ServiceCapability, Int>,
    val uptime: Long
)

/**
 * The Integration Hub - Central service endpoint registry and router
 */
class IntegrationHub : IntegrationHubInterface {
    private var core: AIDCore? = null
    private var isInitialized = false
    private val startTime = System.currentTimeMillis()

    // Endpoint registry
    private val endpoints = ConcurrentHashMap<String, ServiceEndpoint>()
    private val handlers = ConcurrentHashMap<String, EndpointHandler>()

    // Capability index for fast lookups
    private val capabilityIndex = ConcurrentHashMap<ServiceCapability, MutableSet<String>>()

    // Event stream
    private val _events = MutableSharedFlow<HubEvent>(replay = 32)
    val events: SharedFlow<HubEvent> = _events.asSharedFlow()

    // Statistics
    private var totalRequests: Long = 0
    private var successfulRequests: Long = 0
    private var totalLatency: Long = 0

    // Health check
    private val healthCheckMutex = Mutex()
    private var healthCheckJob: Job? = null

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun initialize(core: AIDCore) {
        this.core = core
        isInitialized = true

        // Start periodic health checks
        startHealthChecks()
    }

    override suspend fun handleRequest(request: AIDRequest, tensor: AIDTensor): AIDResult<Any> {
        if (!isInitialized) {
            return AIDResult.Error(AIDErrorCode.INTEGRATION_HUB_ERROR, "Integration hub not initialized")
        }

        return when (request.type) {
            AIDRequestType.INVOKE_ENDPOINT -> invokeEndpoint(request, tensor)
            AIDRequestType.QUERY_ENDPOINTS -> queryEndpoints(request)
            AIDRequestType.REGISTER_ENDPOINT -> registerEndpointFromRequest(request)
            else -> AIDResult.Error(
                AIDErrorCode.INVALID_REQUEST,
                "Integration hub cannot handle request type: ${request.type}"
            )
        }
    }

    override suspend fun shutdown() {
        healthCheckJob?.cancel()
        scope.cancel()

        // Shutdown all handlers
        handlers.values.forEach { handler ->
            try {
                handler.shutdown()
            } catch (e: Exception) {
                // Log but don't fail shutdown
            }
        }

        endpoints.clear()
        handlers.clear()
        capabilityIndex.clear()
        isInitialized = false
    }

    // ============= Endpoint Registration =============

    /**
     * Register a service endpoint with its handler
     */
    suspend fun registerEndpoint(
        name: String,
        description: String,
        capabilities: Set<ServiceCapability>,
        handler: EndpointHandler,
        priority: Int = 50,
        metadata: Map<String, Any> = emptyMap()
    ): AIDResult<ServiceEndpoint> {
        val endpoint = ServiceEndpoint(
            name = name,
            description = description,
            capabilities = capabilities,
            priority = priority,
            metadata = metadata,
            state = EndpointState.INITIALIZING
        )

        return try {
            // Initialize the handler
            val initResult = handler.initialize()
            if (initResult is AIDResult.Error) {
                return AIDResult.Error(
                    AIDErrorCode.DRIVER_INITIALIZATION_FAILED,
                    "Handler initialization failed: ${initResult.message}"
                )
            }

            // Register endpoint and handler
            val readyEndpoint = endpoint.copy(state = EndpointState.READY)
            endpoints[endpoint.id] = readyEndpoint
            handlers[endpoint.id] = handler

            // Update capability index
            capabilities.forEach { cap ->
                capabilityIndex.getOrPut(cap) { mutableSetOf() }.add(endpoint.id)
            }

            scope.launch { _events.emit(HubEvent.EndpointRegistered(readyEndpoint)) }

            AIDResult.Success(readyEndpoint)
        } catch (e: Exception) {
            AIDResult.Error(
                AIDErrorCode.DRIVER_INITIALIZATION_FAILED,
                "Failed to register endpoint: ${e.message}",
                e
            )
        }
    }

    /**
     * Unregister an endpoint
     */
    suspend fun unregisterEndpoint(endpointId: String): AIDResult<Unit> {
        val endpoint = endpoints.remove(endpointId)
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "Endpoint not found: $endpointId")

        val handler = handlers.remove(endpointId)
        handler?.shutdown()

        // Update capability index
        endpoint.capabilities.forEach { cap ->
            capabilityIndex[cap]?.remove(endpointId)
        }

        scope.launch { _events.emit(HubEvent.EndpointUnregistered(endpointId)) }

        return AIDResult.Success(Unit)
    }

    private suspend fun registerEndpointFromRequest(request: AIDRequest): AIDResult<Any> {
        val payload = request.payload as? Map<*, *>
            ?: return AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "Invalid endpoint registration payload")

        val name = payload["name"] as? String
            ?: return AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "Endpoint name required")
        val description = payload["description"] as? String ?: ""
        val capabilities = (payload["capabilities"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.mapNotNull { name ->
                ServiceCapability.entries.find { it.name.equals(name, ignoreCase = true) }
            }
            ?.toSet()
            ?: emptySet()

        // Create a simple proxy handler for dynamic registration
        val handler = object : EndpointHandler {
            override suspend fun initialize() = AIDResult.Success(Unit)
            override suspend fun handle(request: EndpointRequest, tensor: AIDTensor): AIDResult<Any> {
                return AIDResult.Success(mapOf("proxied" to true, "request" to request))
            }
            override fun getHealth() = EndpointHealth(true, EndpointState.READY)
            override suspend fun shutdown() {}
        }

        return registerEndpoint(name, description, capabilities, handler)
    }

    // ============= Endpoint Invocation =============

    private suspend fun invokeEndpoint(request: AIDRequest, tensor: AIDTensor): AIDResult<Any> {
        val startTime = System.currentTimeMillis()
        totalRequests++

        // Determine target endpoint
        val endpointId = request.targetEndpoint
            ?: findBestEndpoint(request, tensor)
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "No suitable endpoint found")

        val endpoint = endpoints[endpointId]
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "Endpoint not found: $endpointId")

        val handler = handlers[endpointId]
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "Handler not found for endpoint: $endpointId")

        // Check endpoint state
        if (endpoint.state != EndpointState.READY && endpoint.state != EndpointState.BUSY) {
            return AIDResult.Error(
                AIDErrorCode.ENDPOINT_NOT_AVAILABLE,
                "Endpoint not available: ${endpoint.state}"
            )
        }

        // Create endpoint request
        val endpointRequest = EndpointRequest(
            action = (request.payload as? Map<*, *>)?.get("action") as? String ?: "default",
            payload = request.payload,
            parameters = (request.payload as? Map<*, *>)
                ?.filterKeys { it is String }
                ?.mapKeys { it.key as String }
                ?.filterValues { it != null }
                ?.mapValues { it.value!! }
                ?: emptyMap(),
            timeout = request.timeout,
            priority = request.priority
        )

        // Update endpoint state
        updateEndpointState(endpointId, EndpointState.BUSY)

        return try {
            scope.launch { _events.emit(HubEvent.RequestRouted(request.id, endpointId)) }

            val result = handler.handle(endpointRequest, tensor)

            val duration = System.currentTimeMillis() - startTime
            totalLatency += duration

            // Update endpoint stats
            val updatedEndpoint = endpoint.copy(
                invocationCount = endpoint.invocationCount + 1,
                errorCount = if (result is AIDResult.Error) endpoint.errorCount + 1 else endpoint.errorCount,
                lastHealthCheck = System.currentTimeMillis()
            )
            endpoints[endpointId] = updatedEndpoint

            if (result is AIDResult.Success) {
                successfulRequests++
            }

            scope.launch {
                _events.emit(HubEvent.RequestCompleted(request.id, result is AIDResult.Success, duration))
            }

            updateEndpointState(endpointId, EndpointState.READY)

            result
        } catch (e: Exception) {
            updateEndpointState(endpointId, EndpointState.ERROR)
            AIDResult.Error(AIDErrorCode.INTEGRATION_HUB_ERROR, "Endpoint invocation failed: ${e.message}", e)
        }
    }

    private fun findBestEndpoint(request: AIDRequest, tensor: AIDTensor): String? {
        // Get payload requirements
        val payload = request.payload as? Map<*, *>
        val requiredCapabilities = (payload?.get("capabilities") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.mapNotNull { name ->
                ServiceCapability.entries.find { it.name.equals(name, ignoreCase = true) }
            }
            ?.toSet()
            ?: emptySet()

        // Find endpoints with required capabilities
        val candidateIds = if (requiredCapabilities.isEmpty()) {
            endpoints.keys.toSet()
        } else {
            requiredCapabilities.flatMap { cap ->
                capabilityIndex[cap] ?: emptySet()
            }.toSet()
        }

        // Filter to ready endpoints and score them
        return candidateIds
            .mapNotNull { endpoints[it] }
            .filter { it.state == EndpointState.READY || it.state == EndpointState.BUSY }
            .maxByOrNull { endpoint ->
                // Score based on priority, reliability, and tensor alignment
                val priorityScore = endpoint.priority / 100f
                val reliabilityScore = endpoint.reliabilityScore()
                val alignmentScore = calculateTensorAlignment(endpoint, tensor)

                priorityScore * 0.4f + reliabilityScore * 0.4f + alignmentScore * 0.2f
            }?.id
    }

    private fun calculateTensorAlignment(endpoint: ServiceEndpoint, tensor: AIDTensor): Float {
        // Calculate how well the endpoint's capabilities align with tensor characteristics
        var score = 0.5f

        // High creativity tensor prefers generation capabilities
        if (tensor.creativityFactor > 0.7f && endpoint.capabilities.contains(ServiceCapability.GENERATION)) {
            score += 0.2f
        }

        // High salience prefers fast/inference capabilities
        if (tensor.salience > 0.7f && endpoint.capabilities.contains(ServiceCapability.INFERENCE)) {
            score += 0.15f
        }

        // Deep processing prefers analysis capabilities
        if (tensor.depth > 0.7f && endpoint.capabilities.contains(ServiceCapability.ANALYSIS)) {
            score += 0.15f
        }

        return score.coerceIn(0f, 1f)
    }

    // ============= Endpoint Query =============

    private suspend fun queryEndpoints(request: AIDRequest): AIDResult<Any> {
        val payload = request.payload as? Map<*, *>

        // Filter by capabilities if specified
        val requiredCapabilities = (payload?.get("capabilities") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.mapNotNull { name ->
                ServiceCapability.entries.find { it.name.equals(name, ignoreCase = true) }
            }
            ?.toSet()

        // Filter by state if specified
        val requiredState = (payload?.get("state") as? String)?.let { stateName ->
            EndpointState.entries.find { it.name.equals(stateName, ignoreCase = true) }
        }

        val filteredEndpoints = endpoints.values.filter { endpoint ->
            val capMatch = requiredCapabilities?.all { endpoint.capabilities.contains(it) } ?: true
            val stateMatch = requiredState?.let { endpoint.state == it } ?: true
            capMatch && stateMatch
        }

        return AIDResult.Success(filteredEndpoints)
    }

    // ============= Health Monitoring =============

    private fun startHealthChecks() {
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(30000) // Check every 30 seconds
                performHealthCheck()
            }
        }
    }

    private suspend fun performHealthCheck() {
        healthCheckMutex.withLock {
            var healthyCount = 0
            val totalCount = endpoints.size

            endpoints.forEach { (id, endpoint) ->
                val handler = handlers[id]
                if (handler != null) {
                    try {
                        val health = handler.getHealth()
                        val newState = if (health.healthy) EndpointState.READY else EndpointState.DEGRADED
                        val newHealth = if (health.healthy) 1.0f else 0.5f

                        if (health.healthy) healthyCount++

                        val updatedEndpoint = endpoint.copy(
                            state = newState,
                            healthScore = newHealth,
                            lastHealthCheck = System.currentTimeMillis()
                        )
                        endpoints[id] = updatedEndpoint

                        if (endpoint.state != newState) {
                            _events.emit(HubEvent.EndpointStateChanged(id, endpoint.state, newState))
                        }
                    } catch (e: Exception) {
                        updateEndpointState(id, EndpointState.ERROR)
                    }
                }
            }

            _events.emit(HubEvent.HealthCheckCompleted(healthyCount, totalCount))
        }
    }

    private fun updateEndpointState(endpointId: String, newState: EndpointState) {
        endpoints[endpointId]?.let { endpoint ->
            if (endpoint.state != newState) {
                endpoints[endpointId] = endpoint.copy(state = newState)
                scope.launch {
                    _events.emit(HubEvent.EndpointStateChanged(endpointId, endpoint.state, newState))
                }
            }
        }
    }

    // ============= Public API =============

    /**
     * Get all registered endpoints
     */
    fun getEndpoints(): List<ServiceEndpoint> = endpoints.values.toList()

    /**
     * Get endpoint by ID
     */
    fun getEndpoint(id: String): ServiceEndpoint? = endpoints[id]

    /**
     * Get endpoints by capability
     */
    fun getEndpointsByCapability(capability: ServiceCapability): List<ServiceEndpoint> {
        return capabilityIndex[capability]
            ?.mapNotNull { endpoints[it] }
            ?: emptyList()
    }

    /**
     * Get hub statistics
     */
    fun getStatistics(): HubStatistics {
        val capCoverage = ServiceCapability.entries.associateWith { cap ->
            capabilityIndex[cap]?.size ?: 0
        }

        return HubStatistics(
            totalEndpoints = endpoints.size,
            activeEndpoints = endpoints.values.count { it.state == EndpointState.READY },
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            averageLatencyMs = if (totalRequests > 0) totalLatency / totalRequests else 0,
            capabilityCoverage = capCoverage,
            uptime = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Direct endpoint invocation (convenience method)
     */
    suspend fun invoke(
        endpointId: String,
        action: String,
        payload: Any? = null,
        tensor: AIDTensor = AIDTensor.neutral()
    ): AIDResult<Any> {
        val handler = handlers[endpointId]
            ?: return AIDResult.Error(AIDErrorCode.ENDPOINT_NOT_AVAILABLE, "Endpoint not found: $endpointId")

        val request = EndpointRequest(action = action, payload = payload)
        return handler.handle(request, tensor)
    }
}
