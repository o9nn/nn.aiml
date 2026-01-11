package org.ninelym.cognitive.tensor

import org.ninelym.cognitive.CognitiveTensor
import org.ninelym.cognitive.hypergraph.Atom

/**
 * Tensor Fragment Architecture for cognitive computation
 * 
 * Manages tensor operations and transformations for cognitive primitives,
 * enabling efficient processing of cognitive tensors with signature
 * [modality, depth, context, salience, autonomy_index]
 */
class TensorFragmentProcessor {
    
    private val fragments = mutableMapOf<String, TensorFragment>()
    
    /**
     * Create a tensor fragment from cognitive tensor
     */
    fun createFragment(id: String, tensor: CognitiveTensor): TensorFragment {
        val fragment = TensorFragment(
            id = id,
            tensor = tensor,
            timestamp = System.currentTimeMillis()
        )
        fragments[id] = fragment
        return fragment
    }
    
    /**
     * Process tensor through transformation pipeline
     */
    fun processTensor(tensor: CognitiveTensor, operations: List<TensorOperation>): CognitiveTensor {
        return operations.fold(tensor) { acc, operation ->
            operation.apply(acc)
        }
    }
    
    /**
     * Combine multiple tensor fragments
     */
    fun combineFragments(fragmentIds: List<String>, combiner: TensorCombiner): CognitiveTensor? {
        val tensors = fragmentIds.mapNotNull { fragments[it]?.tensor }
        if (tensors.isEmpty()) return null
        
        return combiner.combine(tensors)
    }
    
    /**
     * Get tensor fragments by attention threshold
     */
    fun getActiveFragments(minAttention: Float = 0.3f): List<TensorFragment> {
        return fragments.values.filter { 
            it.tensor.computeAttentionWeight() >= minAttention 
        }
    }
    
    /**
     * Convert atom to tensor fragment
     */
    fun atomToFragment(atom: Atom): TensorFragment {
        return TensorFragment(
            id = atom.id,
            tensor = atom.toCognitiveTensor(),
            timestamp = System.currentTimeMillis(),
            sourceAtom = atom
        )
    }
    
    /**
     * Cleanup old fragments based on age and attention
     */
    fun cleanup(maxAge: Long = 3600000, minAttention: Float = 0.1f) {
        val currentTime = System.currentTimeMillis()
        fragments.entries.removeAll { (_, fragment) ->
            val age = currentTime - fragment.timestamp
            age > maxAge && fragment.tensor.computeAttentionWeight() < minAttention
        }
    }
}

/**
 * Individual tensor fragment with metadata
 */
data class TensorFragment(
    val id: String,
    val tensor: CognitiveTensor,
    val timestamp: Long,
    val sourceAtom: Atom? = null
) {
    /**
     * Check if fragment is still active based on age and attention
     */
    fun isActive(maxAge: Long = 1800000, minAttention: Float = 0.2f): Boolean {
        val age = System.currentTimeMillis() - timestamp
        return age <= maxAge && tensor.computeAttentionWeight() >= minAttention
    }
}

/**
 * Interface for tensor operations
 */
interface TensorOperation {
    fun apply(tensor: CognitiveTensor): CognitiveTensor
}

/**
 * Normalize tensor values to [0,1] range
 */
class NormalizationOperation : TensorOperation {
    override fun apply(tensor: CognitiveTensor): CognitiveTensor {
        val values = tensor.toArray()
        val max = values.maxOrNull() ?: 1.0f
        val min = values.minOrNull() ?: 0.0f
        val range = max - min
        
        if (range == 0.0f) return tensor
        
        val normalized = values.map { (it - min) / range }.toFloatArray()
        return CognitiveTensor.fromArray(normalized)
    }
}

/**
 * Apply attention-based scaling
 */
class AttentionScalingOperation(private val scale: Float = 1.5f) : TensorOperation {
    override fun apply(tensor: CognitiveTensor): CognitiveTensor {
        val attentionWeight = tensor.computeAttentionWeight()
        return CognitiveTensor(
            modality = tensor.modality,
            depth = tensor.depth,
            context = tensor.context,
            salience = tensor.salience * (1.0f + attentionWeight * scale),
            autonomyIndex = tensor.autonomyIndex
        )
    }
}

/**
 * Interface for combining multiple tensors
 */
interface TensorCombiner {
    fun combine(tensors: List<CognitiveTensor>): CognitiveTensor
}

/**
 * Average-based tensor combination
 */
class AverageCombiner : TensorCombiner {
    override fun combine(tensors: List<CognitiveTensor>): CognitiveTensor {
        if (tensors.isEmpty()) {
            return CognitiveTensor(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        }
        
        val count = tensors.size.toFloat()
        val sumModality = tensors.sumOf { it.modality.toDouble() }.toFloat()
        val sumDepth = tensors.sumOf { it.depth.toDouble() }.toFloat()
        val sumContext = tensors.sumOf { it.context.toDouble() }.toFloat()
        val sumSalience = tensors.sumOf { it.salience.toDouble() }.toFloat()
        val sumAutonomy = tensors.sumOf { it.autonomyIndex.toDouble() }.toFloat()
        
        return CognitiveTensor(
            modality = sumModality / count,
            depth = sumDepth / count,
            context = sumContext / count,
            salience = sumSalience / count,
            autonomyIndex = sumAutonomy / count
        )
    }
}

/**
 * Attention-weighted tensor combination
 */
class AttentionWeightedCombiner : TensorCombiner {
    override fun combine(tensors: List<CognitiveTensor>): CognitiveTensor {
        if (tensors.isEmpty()) {
            return CognitiveTensor(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        }
        
        val weights = tensors.map { it.computeAttentionWeight() }
        val totalWeight = weights.sum()
        
        if (totalWeight == 0.0f) {
            return AverageCombiner().combine(tensors)
        }
        
        val weightedSum = tensors.zip(weights) { tensor, weight ->
            val factor = weight / totalWeight
            CognitiveTensor(
                modality = tensor.modality * factor,
                depth = tensor.depth * factor,
                context = tensor.context * factor,
                salience = tensor.salience * factor,
                autonomyIndex = tensor.autonomyIndex * factor
            )
        }
        
        return AverageCombiner().combine(weightedSum)
    }
}

/**
 * ML Primitive Tensor Operation - Convert ML embeddings to cognitive tensors
 */
class MLEmbeddingOperation(private val embedding: FloatArray) : TensorOperation {
    override fun apply(tensor: CognitiveTensor): CognitiveTensor {
        // Use embedding values to modulate tensor dimensions
        val embeddingNorm = if (embedding.isNotEmpty()) {
            kotlin.math.sqrt(embedding.map { it * it }.sum()).coerceIn(0f, 1f)
        } else 0.5f
        
        return CognitiveTensor(
            modality = if (embedding.isNotEmpty()) embedding[0].coerceIn(0f, 1f) else tensor.modality,
            depth = if (embedding.size > 1) embedding[1].coerceIn(0f, 4f) else tensor.depth,
            context = if (embedding.size > 2) embedding[2].coerceIn(0f, 1f) else tensor.context,
            salience = embeddingNorm,
            autonomyIndex = if (embedding.size > 3) embedding[3].coerceIn(0f, 1f) else tensor.autonomyIndex
        )
    }
}

/**
 * Tensor Serialization for ML model interoperability
 */
class TensorSerializer {
    
    /**
     * Serialize cognitive tensor to JSON format
     */
    fun toJSON(tensor: CognitiveTensor): String {
        return buildString {
            append("{")
            append("\"modality\":${tensor.modality},")
            append("\"depth\":${tensor.depth},")
            append("\"context\":${tensor.context},")
            append("\"salience\":${tensor.salience},")
            append("\"autonomy_index\":${tensor.autonomyIndex},")
            append("\"valence\":${tensor.valence},")
            append("\"arousal\":${tensor.arousal},")
            append("\"confidence\":${tensor.confidence},")
            append("\"complexity\":${tensor.complexity}")
            append("}")
        }
    }
    
    /**
     * Deserialize JSON to cognitive tensor
     */
    fun fromJSON(json: String): CognitiveTensor? {
        return try {
            // Simple JSON parsing (in production, use proper JSON library)
            val values = json
                .removeSurrounding("{", "}")
                .split(",")
                .associate { 
                    val parts = it.split(":")
                    parts[0].trim().removeSurrounding("\"") to parts[1].trim().toFloat()
                }
            
            CognitiveTensor(
                modality = values["modality"] ?: 0.5f,
                depth = values["depth"] ?: 1.0f,
                context = values["context"] ?: 0.5f,
                salience = values["salience"] ?: 0.5f,
                autonomyIndex = values["autonomy_index"] ?: 0.5f,
                valence = values["valence"] ?: 0.5f,
                arousal = values["arousal"] ?: 0.5f,
                confidence = values["confidence"] ?: 0.5f,
                complexity = values["complexity"] ?: 0.5f
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Serialize to binary format for efficient transmission
     */
    fun toBinary(tensor: CognitiveTensor): ByteArray {
        val array = tensor.toArray()
        val buffer = java.nio.ByteBuffer.allocate(array.size * 4)
        array.forEach { buffer.putFloat(it) }
        return buffer.array()
    }
    
    /**
     * Deserialize from binary format
     */
    fun fromBinary(bytes: ByteArray): CognitiveTensor? {
        return try {
            val buffer = java.nio.ByteBuffer.wrap(bytes)
            val array = FloatArray(bytes.size / 4) { buffer.getFloat() }
            CognitiveTensor.fromArray(array)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Enhanced tensor transformation pipeline for ML model inference
 */
class TensorTransformationPipeline {
    private val operations = mutableListOf<TensorOperation>()
    
    fun addOperation(operation: TensorOperation): TensorTransformationPipeline {
        operations.add(operation)
        return this
    }
    
    fun normalize(): TensorTransformationPipeline {
        operations.add(NormalizationOperation())
        return this
    }
    
    fun scaleAttention(scale: Float = 1.5f): TensorTransformationPipeline {
        operations.add(AttentionScalingOperation(scale))
        return this
    }
    
    fun applyMLEmbedding(embedding: FloatArray): TensorTransformationPipeline {
        operations.add(MLEmbeddingOperation(embedding))
        return this
    }
    
    fun execute(tensor: CognitiveTensor): CognitiveTensor {
        return operations.fold(tensor) { acc, operation ->
            operation.apply(acc)
        }
    }
    
    fun executeBatch(tensors: List<CognitiveTensor>): List<CognitiveTensor> {
        return tensors.map { execute(it) }
    }
    
    fun clear() {
        operations.clear()
    }
}

/**
 * Fragment lifecycle manager for automatic garbage collection
 */
class FragmentLifecycleManager(
    private val processor: TensorFragmentProcessor,
    private val maxAge: Long = 3600000, // 1 hour
    private val minAttention: Float = 0.1f,
    private val maxFragments: Int = 10000
) {
    
    private var lastCleanup = System.currentTimeMillis()
    private val cleanupInterval = 300000L // 5 minutes
    
    /**
     * Perform lifecycle management checks
     */
    fun performMaintenance() {
        val currentTime = System.currentTimeMillis()
        
        // Periodic cleanup
        if (currentTime - lastCleanup > cleanupInterval) {
            processor.cleanup(maxAge, minAttention)
            lastCleanup = currentTime
        }
        
        // Emergency cleanup if too many fragments
        val activeFragments = processor.getActiveFragments(0.0f)
        if (activeFragments.size > maxFragments) {
            // Keep only highest attention fragments
            val sortedFragments = activeFragments.sortedByDescending { 
                it.tensor.computeAttentionWeight() 
            }
            val toRemove = sortedFragments.drop(maxFragments)
            // Note: In full implementation, would remove these fragments
        }
    }
    
    /**
     * Get lifecycle statistics
     */
    fun getStats(): LifecycleStats {
        val activeFragments = processor.getActiveFragments(0.0f)
        val oldFragments = activeFragments.count { 
            System.currentTimeMillis() - it.timestamp > maxAge 
        }
        val lowAttentionFragments = activeFragments.count {
            it.tensor.computeAttentionWeight() < minAttention
        }
        
        return LifecycleStats(
            totalFragments = activeFragments.size,
            oldFragments = oldFragments,
            lowAttentionFragments = lowAttentionFragments,
            lastCleanup = lastCleanup
        )
    }
}

/**
 * Lifecycle statistics
 */
data class LifecycleStats(
    val totalFragments: Int,
    val oldFragments: Int,
    val lowAttentionFragments: Int,
    val lastCleanup: Long
)