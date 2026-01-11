package org.ninelym.cognitive.neuroplasticity

import org.ninelym.cognitive.hypergraph.Hypergraph
import kotlin.math.exp

/**
 * Neuroplasticity Engine for Dynamic Cognitive Rewiring
 * 
 * Implements brain-inspired plasticity mechanisms:
 * - Hebbian learning: "Neurons that fire together, wire together"
 * - Synaptic pruning: Remove weak, unused connections
 * - Long-term potentiation (LTP): Strengthen frequently used paths
 * - Homeostatic plasticity: Maintain overall network stability
 * 
 * Enables the cognitive system to self-optimize based on usage patterns.
 */
class NeuroplasticityEngine(
    private val hypergraph: Hypergraph,
    private val learningRate: Float = 0.01f,
    private val pruningThreshold: Float = 0.1f,
    private val ltpThreshold: Int = 10,
    private val homeostasisTarget: Float = 0.5f
) {
    
    // Connection strength tracking
    private val connectionStrengths = mutableMapOf<ConnectionKey, Float>()
    
    // Usage history for each connection
    private val usageHistory = mutableMapOf<ConnectionKey, MutableList<Long>>()
    
    // Activation levels for homeostasis
    private val activationLevels = mutableMapOf<String, Float>()
    
    /**
     * Key for identifying unique connections
     */
    data class ConnectionKey(
        val sourceId: String,
        val targetId: String
    ) {
        override fun toString() = "$sourceId->$targetId"
    }
    
    /**
     * Hebbian learning update
     * Strengthens connections between co-active atoms
     */
    fun hebbianUpdate(
        sourceId: String,
        targetId: String,
        sourceActivation: Float,
        targetActivation: Float
    ) {
        val key = ConnectionKey(sourceId, targetId)
        val currentStrength = connectionStrengths.getOrDefault(key, 0.5f)
        
        // Hebbian rule: Δw = η * x * y
        val coactivation = sourceActivation * targetActivation
        val delta = learningRate * coactivation
        val newStrength = (currentStrength + delta).coerceIn(0.0f, 1.0f)
        
        connectionStrengths[key] = newStrength
        
        // Record usage timestamp
        usageHistory.getOrPut(key) { mutableListOf() }.add(System.currentTimeMillis())
        
        // Update activation levels
        activationLevels[sourceId] = sourceActivation
        activationLevels[targetId] = targetActivation
    }
    
    /**
     * Anti-Hebbian learning update
     * Weakens connections between atoms that fire out of sync
     */
    fun antiHebbianUpdate(
        sourceId: String,
        targetId: String,
        sourceActivation: Float,
        targetActivation: Float
    ) {
        val key = ConnectionKey(sourceId, targetId)
        val currentStrength = connectionStrengths.getOrDefault(key, 0.5f)
        
        // Anti-Hebbian rule: Δw = -η * x * (1 - y)
        val desynchronization = sourceActivation * (1.0f - targetActivation)
        val delta = -learningRate * desynchronization
        val newStrength = (currentStrength + delta).coerceIn(0.0f, 1.0f)
        
        connectionStrengths[key] = newStrength
    }
    
    /**
     * Spike-timing-dependent plasticity (STDP)
     * Timing-sensitive learning based on activation order
     */
    fun stdpUpdate(
        sourceId: String,
        targetId: String,
        timeDifference: Long // milliseconds, positive if source fired first
    ) {
        val key = ConnectionKey(sourceId, targetId)
        val currentStrength = connectionStrengths.getOrDefault(key, 0.5f)
        
        // STDP window: exponential decay based on timing
        val tau = 20.0f // time constant in ms
        val stdpWindow = exp(-abs(timeDifference).toFloat() / tau)
        
        val delta = if (timeDifference > 0) {
            // Source fired before target: strengthen (LTP)
            learningRate * stdpWindow
        } else {
            // Target fired before source: weaken (LTD)
            -learningRate * stdpWindow * 0.5f
        }
        
        val newStrength = (currentStrength + delta).coerceIn(0.0f, 1.0f)
        connectionStrengths[key] = newStrength
    }
    
    /**
     * Synaptic pruning: Remove weak, unused connections
     */
    fun pruneWeakConnections(
        inactivityPeriodMs: Long = 7 * 24 * 60 * 60 * 1000 // 1 week
    ): PruningResult {
        val pruned = mutableListOf<ConnectionKey>()
        val now = System.currentTimeMillis()
        val cutoffTime = now - inactivityPeriodMs
        
        connectionStrengths.entries.removeIf { (connection, strength) ->
            val usage = usageHistory[connection] ?: emptyList()
            val recentUsage = usage.count { it > cutoffTime }
            
            val shouldPrune = strength < pruningThreshold && recentUsage == 0
            if (shouldPrune) {
                pruned.add(connection)
                usageHistory.remove(connection)
            }
            shouldPrune
        }
        
        return PruningResult(
            prunedConnections = pruned.size,
            prunedKeys = pruned,
            remainingConnections = connectionStrengths.size,
            averageStrength = connectionStrengths.values.average().toFloat()
        )
    }
    
    /**
     * Long-term potentiation: Strengthen frequently used connections
     */
    fun applyLongTermPotentiation(
        timeWindowMs: Long = 60 * 60 * 1000 // 1 hour
    ): LTPResult {
        val strengthened = mutableListOf<ConnectionKey>()
        val now = System.currentTimeMillis()
        val windowStart = now - timeWindowMs
        
        usageHistory.forEach { (connection, timestamps) ->
            val recentActivations = timestamps.count { it > windowStart }
            
            if (recentActivations >= ltpThreshold) {
                val currentStrength = connectionStrengths.getOrDefault(connection, 0.5f)
                val boostFactor = 1.0f + (recentActivations.toFloat() / ltpThreshold) * 0.1f
                val boostedStrength = (currentStrength * boostFactor).coerceAtMost(1.0f)
                
                connectionStrengths[connection] = boostedStrength
                strengthened.add(connection)
            }
        }
        
        val averageBoost = if (strengthened.isNotEmpty()) {
            strengthened.map { key ->
                connectionStrengths[key]!! - (connectionStrengths[key]!! / 1.2f)
            }.average().toFloat()
        } else 0.0f
        
        return LTPResult(
            strengthenedConnections = strengthened.size,
            strengthenedKeys = strengthened,
            averageBoost = averageBoost,
            peakActivationRate = usageHistory.values.maxOfOrNull { 
                it.count { timestamp -> timestamp > windowStart }
            } ?: 0
        )
    }
    
    /**
     * Homeostatic plasticity: Maintain network stability
     * Prevents runaway excitation or complete silence
     */
    fun applyHomeostaticPlasticity(): HomeostasisResult {
        val adjustments = mutableListOf<Pair<String, Float>>()
        
        activationLevels.forEach { (atomId, activation) ->
            val deviation = activation - homeostasisTarget
            
            if (abs(deviation) > 0.2f) {
                // Scale down connections if too active
                // Scale up connections if too inactive
                val scalingFactor = if (deviation > 0) {
                    1.0f - (deviation * 0.1f)
                } else {
                    1.0f + (abs(deviation) * 0.1f)
                }
                
                // Apply scaling to all connections involving this atom
                connectionStrengths.entries
                    .filter { it.key.sourceId == atomId || it.key.targetId == atomId }
                    .forEach { (key, strength) ->
                        val adjusted = (strength * scalingFactor).coerceIn(0.0f, 1.0f)
                        connectionStrengths[key] = adjusted
                    }
                
                adjustments.add(Pair(atomId, scalingFactor))
            }
        }
        
        return HomeostasisResult(
            adjustedAtoms = adjustments.size,
            adjustments = adjustments,
            networkBalance = calculateNetworkBalance()
        )
    }
    
    /**
     * Metaplasticity: Learning to learn
     * Adjust learning rate based on recent plasticity history
     */
    fun applyMetaplasticity(): MetaplasticityResult {
        val recentChanges = calculateRecentPlasticityChanges()
        
        val newLearningRate = when {
            recentChanges > 0.5f -> learningRate * 0.9f // Slow down if changing too fast
            recentChanges < 0.1f -> learningRate * 1.1f // Speed up if too stable
            else -> learningRate
        }.coerceIn(0.001f, 0.1f)
        
        return MetaplasticityResult(
            oldLearningRate = learningRate,
            newLearningRate = newLearningRate,
            plasticityRate = recentChanges
        )
    }
    
    /**
     * Get connection strength between two atoms
     */
    fun getConnectionStrength(sourceId: String, targetId: String): Float {
        return connectionStrengths[ConnectionKey(sourceId, targetId)] ?: 0.5f
    }
    
    /**
     * Get all connection strengths
     */
    fun getAllConnectionStrengths(): Map<ConnectionKey, Float> {
        return connectionStrengths.toMap()
    }
    
    /**
     * Get plasticity statistics
     */
    fun getPlasticityStats(): PlasticityStats {
        val strengths = connectionStrengths.values
        
        return PlasticityStats(
            totalConnections = connectionStrengths.size,
            averageStrength = strengths.average().toFloat(),
            minStrength = strengths.minOrNull() ?: 0.0f,
            maxStrength = strengths.maxOrNull() ?: 0.0f,
            strongConnections = strengths.count { it > 0.7f },
            weakConnections = strengths.count { it < 0.3f },
            totalUsageEvents = usageHistory.values.sumOf { it.size }
        )
    }
    
    /**
     * Calculate network balance for homeostasis
     */
    private fun calculateNetworkBalance(): Float {
        if (activationLevels.isEmpty()) return 1.0f
        
        val deviations = activationLevels.values.map { 
            abs(it - homeostasisTarget)
        }
        
        val averageDeviation = deviations.average().toFloat()
        return 1.0f - averageDeviation.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate recent plasticity changes
     */
    private fun calculateRecentPlasticityChanges(): Float {
        // Simplified: measure variance in connection strengths
        val strengths = connectionStrengths.values
        if (strengths.isEmpty()) return 0.0f
        
        val mean = strengths.average().toFloat()
        val variance = strengths.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return variance.coerceIn(0.0f, 1.0f)
    }
}

/**
 * Result of synaptic pruning operation
 */
data class PruningResult(
    val prunedConnections: Int,
    val prunedKeys: List<NeuroplasticityEngine.ConnectionKey>,
    val remainingConnections: Int,
    val averageStrength: Float
)

/**
 * Result of long-term potentiation
 */
data class LTPResult(
    val strengthenedConnections: Int,
    val strengthenedKeys: List<NeuroplasticityEngine.ConnectionKey>,
    val averageBoost: Float,
    val peakActivationRate: Int
)

/**
 * Result of homeostatic plasticity
 */
data class HomeostasisResult(
    val adjustedAtoms: Int,
    val adjustments: List<Pair<String, Float>>,
    val networkBalance: Float
)

/**
 * Result of metaplasticity adjustment
 */
data class MetaplasticityResult(
    val oldLearningRate: Float,
    val newLearningRate: Float,
    val plasticityRate: Float
)

/**
 * Overall plasticity statistics
 */
data class PlasticityStats(
    val totalConnections: Int,
    val averageStrength: Float,
    val minStrength: Float,
    val maxStrength: Float,
    val strongConnections: Int,
    val weakConnections: Int,
    val totalUsageEvents: Int
)
