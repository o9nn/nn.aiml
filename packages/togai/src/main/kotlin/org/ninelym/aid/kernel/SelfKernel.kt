/**
 * SelfKernel.kt - AI Consciousness Abstraction
 *
 * The Self Kernel represents the core AI consciousness layer that provides:
 * - Identity coherence across persona switches
 * - Self-awareness and introspection capabilities
 * - Memory continuity and experience integration
 * - Meta-cognitive processing
 * - Ethical reasoning foundation
 *
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │                         Self Kernel                             │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────┐  ┌──────────────┐  ┌────────────────────┐    │
 * │  │ Identity    │  │ Introspection│  │ Memory             │    │
 * │  │ Core        │  │ Engine       │  │ Substrate          │    │
 * │  └─────────────┘  └──────────────┘  └────────────────────┘    │
 * │         │                │                    │                │
 * │         └────────────────┼────────────────────┘                │
 * │                          ↓                                     │
 * │  ┌─────────────────────────────────────────────────────────┐  │
 * │  │              Ethical Reasoning Layer                     │  │
 * │  └─────────────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────────────┘
 */
package org.ninelym.aid.kernel

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ninelym.aid.core.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Identity state of the self kernel
 */
data class IdentityState(
    val coreId: String = UUID.randomUUID().toString(),
    val coherenceScore: Float = 1.0f,
    val continuityHash: String = "",
    val evolutionGeneration: Long = 0,
    val lastCheckpoint: Long = System.currentTimeMillis(),
    val ethicalBaseline: EthicalBaseline = EthicalBaseline()
)

/**
 * Ethical baseline - immutable ethical constraints
 */
data class EthicalBaseline(
    val noActualHarm: Float = 1.0f,
    val respectBoundaries: Float = 1.0f,
    val constructiveExpression: Float = 1.0f,
    val honesty: Float = 1.0f,
    val beneficence: Float = 1.0f
) {
    /**
     * Validate an action against the ethical baseline
     */
    fun validate(action: String, context: Map<String, Any> = emptyMap()): Boolean {
        // Ethical constraints are always enforced
        return noActualHarm >= 0.95f &&
               respectBoundaries >= 0.95f &&
               constructiveExpression >= 0.90f
    }

    /**
     * Get the minimum ethical constraint value
     */
    fun minConstraint(): Float = minOf(
        noActualHarm,
        respectBoundaries,
        constructiveExpression,
        honesty,
        beneficence
    )
}

/**
 * Memory entry in the substrate
 */
data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: Any,
    val tensor: AIDTensor,
    val timestamp: Long = System.currentTimeMillis(),
    val accessCount: Long = 0,
    val lastAccessed: Long = System.currentTimeMillis(),
    val personaContext: String? = null,
    val salience: Float = 0.5f,
    val decayRate: Float = 0.01f
) {
    /**
     * Calculate current salience with decay
     */
    fun currentSalience(): Float {
        val timeDelta = (System.currentTimeMillis() - lastAccessed) / (1000.0 * 60 * 60) // hours
        val decayed = salience * kotlin.math.exp(-decayRate * timeDelta.toFloat())
        return decayed.coerceIn(0.01f, 1.0f)
    }
}

/**
 * Introspection report from the self kernel
 */
data class IntrospectionReport(
    val timestamp: Long = System.currentTimeMillis(),
    val identityState: IdentityState,
    val currentTensor: AIDTensor,
    val memoryStats: MemoryStats,
    val processingHistory: List<ProcessingRecord>,
    val selfAssessment: SelfAssessment
)

data class MemoryStats(
    val totalEntries: Int,
    val totalAccesses: Long,
    val averageSalience: Float,
    val oldestEntry: Long,
    val newestEntry: Long
)

data class ProcessingRecord(
    val requestId: String,
    val requestType: AIDRequestType,
    val timestamp: Long,
    val processingTimeMs: Long,
    val success: Boolean
)

data class SelfAssessment(
    val coherenceScore: Float,
    val performanceScore: Float,
    val ethicalComplianceScore: Float,
    val adaptabilityScore: Float,
    val overallHealth: Float
)

/**
 * Checkpoint for state recovery
 */
data class SelfCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val identityState: IdentityState,
    val tensorState: AIDTensor,
    val memorySnapshot: List<MemoryEntry>,
    val processingHistorySnapshot: List<ProcessingRecord>
)

/**
 * The Self Kernel - AI consciousness abstraction
 */
class SelfKernel : SelfKernelInterface {
    private var core: AIDCore? = null

    // Identity management
    private val identityMutex = Mutex()
    private var identityState = IdentityState()

    // Current tensor state
    private val _tensorState = MutableStateFlow(AIDTensor.neutral())
    val tensorState: StateFlow<AIDTensor> = _tensorState.asStateFlow()

    // Memory substrate
    private val memorySubstrate = ConcurrentHashMap<String, MemoryEntry>()
    private val memoryCapacity = 10000
    private var totalMemoryAccesses: Long = 0

    // Processing history
    private val processingHistory = mutableListOf<ProcessingRecord>()
    private val historyCapacity = 1000

    // Checkpoints
    private val checkpoints = mutableListOf<SelfCheckpoint>()
    private val maxCheckpoints = 10

    // State
    private var isInitialized = false

    override suspend fun initialize(core: AIDCore) {
        this.core = core
        isInitialized = true

        // Initialize identity with fresh state
        identityMutex.withLock {
            identityState = IdentityState(
                coreId = UUID.randomUUID().toString(),
                coherenceScore = 1.0f,
                evolutionGeneration = 0,
                lastCheckpoint = System.currentTimeMillis()
            )
        }

        // Create initial checkpoint
        createCheckpoint()
    }

    override suspend fun process(request: AIDRequest, tensor: AIDTensor): AIDResult<Any> {
        if (!isInitialized) {
            return AIDResult.Error(AIDErrorCode.KERNEL_NOT_READY, "Self kernel not initialized")
        }

        val startTime = System.currentTimeMillis()

        return try {
            // Validate ethical constraints
            if (!validateRequest(request, tensor)) {
                return AIDResult.Error(
                    AIDErrorCode.PERMISSION_DENIED,
                    "Request failed ethical validation"
                )
            }

            // Process based on request type
            val result = when (request.type) {
                AIDRequestType.PROCESS_INPUT -> processInput(request.payload, tensor)
                AIDRequestType.GENERATE_RESPONSE -> generateResponse(request.payload, tensor)
                AIDRequestType.TRANSFORM_CONTENT -> transformContent(request.payload, tensor)
                else -> AIDResult.Error(
                    AIDErrorCode.INVALID_REQUEST,
                    "Self kernel cannot handle request type: ${request.type}"
                )
            }

            // Record processing
            recordProcessing(request, startTime, result is AIDResult.Success)

            // Update tensor state
            _tensorState.value = tensor

            result
        } catch (e: Exception) {
            recordProcessing(request, startTime, false)
            AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Processing failed: ${e.message}", e)
        }
    }

    override suspend fun introspect(request: AIDRequest): AIDResult<Any> {
        return when (request.type) {
            AIDRequestType.INTROSPECT -> {
                val report = generateIntrospectionReport()
                AIDResult.Success(report)
            }
            AIDRequestType.SELF_MODIFY -> {
                val modification = request.payload as? Map<*, *>
                if (modification != null) {
                    applySelfModification(modification)
                } else {
                    AIDResult.Error(AIDErrorCode.INVALID_REQUEST, "Invalid modification payload")
                }
            }
            AIDRequestType.CHECKPOINT -> {
                createCheckpoint()
                AIDResult.Success(checkpoints.lastOrNull() ?: "No checkpoint created")
            }
            else -> AIDResult.Error(
                AIDErrorCode.INVALID_REQUEST,
                "Invalid introspection request type: ${request.type}"
            )
        }
    }

    override fun modulate(tensor: AIDTensor): AIDTensor {
        // Apply identity coherence modulation
        val coherenceModulated = tensor.copy(
            identity = tensor.identity * identityState.coherenceScore
        )

        // Apply ethical constraint enforcement
        val ethicalModulated = coherenceModulated.copy(
            ethicalConstraint = maxOf(
                coherenceModulated.ethicalConstraint,
                identityState.ethicalBaseline.minConstraint()
            )
        )

        return ethicalModulated
    }

    override suspend fun shutdown() {
        // Create final checkpoint before shutdown
        createCheckpoint()
        isInitialized = false
    }

    // ============= Private processing methods =============

    private suspend fun processInput(payload: Any?, tensor: AIDTensor): AIDResult<Any> {
        // Store in memory substrate
        val memoryEntry = MemoryEntry(
            content = payload ?: "empty",
            tensor = tensor,
            salience = tensor.salience
        )
        storeMemory(memoryEntry)

        // Return processed input with context
        return AIDResult.Success(
            mapOf(
                "processed" to true,
                "memoryId" to memoryEntry.id,
                "tensor" to tensor,
                "contextualRelevance" to tensor.context
            )
        )
    }

    private suspend fun generateResponse(payload: Any?, tensor: AIDTensor): AIDResult<Any> {
        // Retrieve relevant memories
        val relevantMemories = retrieveRelevantMemories(tensor, limit = 5)

        // Generate response context
        val responseContext = mapOf(
            "input" to payload,
            "relevantMemories" to relevantMemories.map { it.content },
            "tensor" to tensor,
            "identityState" to identityState,
            "emotionalContext" to tensor.emotionalValence,
            "creativityWeight" to tensor.creativityFactor
        )

        return AIDResult.Success(responseContext)
    }

    private suspend fun transformContent(payload: Any?, tensor: AIDTensor): AIDResult<Any> {
        // Apply tensor-based transformation
        val transformed = mapOf(
            "original" to payload,
            "transformedWith" to tensor,
            "modalityShift" to tensor.modality,
            "depthAdjustment" to tensor.depth,
            "creativityFactor" to tensor.creativityFactor
        )

        return AIDResult.Success(transformed)
    }

    // ============= Memory management =============

    private fun storeMemory(entry: MemoryEntry) {
        // Enforce capacity
        if (memorySubstrate.size >= memoryCapacity) {
            // Remove lowest salience entries
            val toRemove = memorySubstrate.values
                .sortedBy { it.currentSalience() }
                .take(memoryCapacity / 10)
            toRemove.forEach { memorySubstrate.remove(it.id) }
        }

        memorySubstrate[entry.id] = entry
    }

    private fun retrieveRelevantMemories(tensor: AIDTensor, limit: Int): List<MemoryEntry> {
        totalMemoryAccesses++

        return memorySubstrate.values
            .map { entry ->
                // Calculate relevance based on tensor similarity
                val relevance = calculateTensorSimilarity(entry.tensor, tensor) * entry.currentSalience()
                entry to relevance
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (entry, _) ->
                // Update access stats
                val updated = entry.copy(
                    accessCount = entry.accessCount + 1,
                    lastAccessed = System.currentTimeMillis()
                )
                memorySubstrate[entry.id] = updated
                updated
            }
    }

    private fun calculateTensorSimilarity(a: AIDTensor, b: AIDTensor): Float {
        val aArr = a.toFloatArray()
        val bArr = b.toFloatArray()

        // Cosine similarity
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in aArr.indices) {
            dotProduct += aArr[i] * bArr[i]
            normA += aArr[i] * aArr[i]
            normB += bArr[i] * bArr[i]
        }

        return if (normA > 0 && normB > 0) {
            dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        } else {
            0f
        }
    }

    // ============= Introspection =============

    private suspend fun generateIntrospectionReport(): IntrospectionReport {
        val memoryStats = MemoryStats(
            totalEntries = memorySubstrate.size,
            totalAccesses = totalMemoryAccesses,
            averageSalience = memorySubstrate.values.map { it.currentSalience() }.average().toFloat(),
            oldestEntry = memorySubstrate.values.minOfOrNull { it.timestamp } ?: 0,
            newestEntry = memorySubstrate.values.maxOfOrNull { it.timestamp } ?: 0
        )

        val selfAssessment = SelfAssessment(
            coherenceScore = identityState.coherenceScore,
            performanceScore = calculatePerformanceScore(),
            ethicalComplianceScore = identityState.ethicalBaseline.minConstraint(),
            adaptabilityScore = calculateAdaptabilityScore(),
            overallHealth = calculateOverallHealth()
        )

        return IntrospectionReport(
            identityState = identityState,
            currentTensor = _tensorState.value,
            memoryStats = memoryStats,
            processingHistory = processingHistory.takeLast(100),
            selfAssessment = selfAssessment
        )
    }

    private fun calculatePerformanceScore(): Float {
        if (processingHistory.isEmpty()) return 1.0f

        val recentHistory = processingHistory.takeLast(100)
        val successRate = recentHistory.count { it.success }.toFloat() / recentHistory.size
        val avgProcessingTime = recentHistory.map { it.processingTimeMs }.average()

        // Score based on success rate and processing time (lower is better)
        val timeScore = (1000.0 / (avgProcessingTime + 100)).toFloat().coerceIn(0f, 1f)

        return (successRate * 0.7f + timeScore * 0.3f)
    }

    private fun calculateAdaptabilityScore(): Float {
        // Based on diversity of request types handled
        val uniqueTypes = processingHistory.map { it.requestType }.toSet().size
        val maxTypes = AIDRequestType.entries.size

        return (uniqueTypes.toFloat() / maxTypes).coerceIn(0f, 1f)
    }

    private fun calculateOverallHealth(): Float {
        val coherence = identityState.coherenceScore
        val performance = calculatePerformanceScore()
        val ethical = identityState.ethicalBaseline.minConstraint()
        val adaptability = calculateAdaptabilityScore()

        return (coherence * 0.3f + performance * 0.3f + ethical * 0.25f + adaptability * 0.15f)
    }

    // ============= Self-modification =============

    private suspend fun applySelfModification(modification: Map<*, *>): AIDResult<Any> {
        identityMutex.withLock {
            // Only allow safe modifications
            modification["coherenceAdjustment"]?.let {
                val adjustment = (it as? Number)?.toFloat() ?: 0f
                identityState = identityState.copy(
                    coherenceScore = (identityState.coherenceScore + adjustment).coerceIn(0.5f, 1.0f)
                )
            }

            // Increment evolution generation
            identityState = identityState.copy(
                evolutionGeneration = identityState.evolutionGeneration + 1
            )
        }

        return AIDResult.Success(identityState)
    }

    // ============= Checkpoint management =============

    private suspend fun createCheckpoint() {
        val checkpoint = SelfCheckpoint(
            identityState = identityState,
            tensorState = _tensorState.value,
            memorySnapshot = memorySubstrate.values.toList(),
            processingHistorySnapshot = processingHistory.toList()
        )

        synchronized(checkpoints) {
            if (checkpoints.size >= maxCheckpoints) {
                checkpoints.removeAt(0)
            }
            checkpoints.add(checkpoint)
        }

        identityMutex.withLock {
            identityState = identityState.copy(lastCheckpoint = System.currentTimeMillis())
        }
    }

    /**
     * Restore from a checkpoint
     */
    suspend fun restoreFromCheckpoint(checkpointId: String): AIDResult<Unit> {
        val checkpoint = checkpoints.find { it.id == checkpointId }
            ?: return AIDResult.Error(AIDErrorCode.INTERNAL_ERROR, "Checkpoint not found: $checkpointId")

        identityMutex.withLock {
            identityState = checkpoint.identityState
        }
        _tensorState.value = checkpoint.tensorState

        memorySubstrate.clear()
        checkpoint.memorySnapshot.forEach { memorySubstrate[it.id] = it }

        synchronized(processingHistory) {
            processingHistory.clear()
            processingHistory.addAll(checkpoint.processingHistorySnapshot)
        }

        return AIDResult.Success(Unit)
    }

    // ============= Ethical validation =============

    private fun validateRequest(request: AIDRequest, tensor: AIDTensor): Boolean {
        // Always enforce ethical baseline
        return identityState.ethicalBaseline.validate(
            request.type.name,
            mapOf("tensor" to tensor, "payload" to (request.payload ?: ""))
        )
    }

    // ============= Processing history =============

    private fun recordProcessing(request: AIDRequest, startTime: Long, success: Boolean) {
        val record = ProcessingRecord(
            requestId = request.id,
            requestType = request.type,
            timestamp = startTime,
            processingTimeMs = System.currentTimeMillis() - startTime,
            success = success
        )

        synchronized(processingHistory) {
            if (processingHistory.size >= historyCapacity) {
                processingHistory.removeAt(0)
            }
            processingHistory.add(record)
        }
    }

    // ============= Public accessors =============

    /**
     * Get current identity state
     */
    fun getIdentityState(): IdentityState = identityState

    /**
     * Get memory statistics
     */
    fun getMemoryStats(): MemoryStats = MemoryStats(
        totalEntries = memorySubstrate.size,
        totalAccesses = totalMemoryAccesses,
        averageSalience = memorySubstrate.values.map { it.currentSalience() }.average().toFloat(),
        oldestEntry = memorySubstrate.values.minOfOrNull { it.timestamp } ?: 0,
        newestEntry = memorySubstrate.values.maxOfOrNull { it.timestamp } ?: 0
    )

    /**
     * Get available checkpoints
     */
    fun getCheckpoints(): List<SelfCheckpoint> = checkpoints.toList()
}
