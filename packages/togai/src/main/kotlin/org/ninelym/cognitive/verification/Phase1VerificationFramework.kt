package org.ninelym.cognitive.verification

import org.ninelym.cognitive.CognitiveTensor
import org.ninelym.cognitive.hypergraph.Hypergraph
import org.ninelym.cognitive.hypergraph.Atom
import org.ninelym.cognitive.hypergraph.AtomType
import org.ninelym.cognitive.scheme.MLPrimitive
import org.ninelym.cognitive.scheme.MLAtomSpaceTranslator

/**
 * Comprehensive verification framework for Phase 1
 * 
 * Validates hypergraph consistency, tensor signature correctness,
 * and bidirectional translation integrity between ML primitives
 * and AtomSpace hypergraph patterns
 */
class Phase1VerificationFramework {
    
    private val translator = MLAtomSpaceTranslator()
    
    /**
     * Verify hypergraph consistency
     */
    fun verifyHypergraphConsistency(hypergraph: Hypergraph): ConsistencyReport {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        val stats = hypergraph.getStats()
        
        // Check atom count sanity
        if (stats.atomCount == 0) {
            warnings.add("Hypergraph is empty")
        }
        
        // Verify all atoms have valid truth values
        AtomType.values().forEach { type ->
            val atoms = hypergraph.getAtomsByType(type)
            atoms.forEach { atom ->
                if (!atom.truthValue.isValid()) {
                    issues.add("Atom ${atom.id} has invalid truth value: ${atom.truthValue}")
                }
                if (!atom.attentionValue.isValid()) {
                    issues.add("Atom ${atom.id} has invalid attention value: ${atom.attentionValue}")
                }
            }
        }
        
        // Check for orphaned atoms (no connections)
        val linkedAtoms = mutableSetOf<String>()
        hypergraph.getAtomsByType(AtomType.LINK).forEach { link ->
            // In full implementation, would extract linked atom IDs
        }
        
        // Verify attention value distribution
        val avgAttention = stats.averageAttention
        if (avgAttention < 0.1f) {
            warnings.add("Low average attention across hypergraph: $avgAttention")
        }
        
        return ConsistencyReport(
            isConsistent = issues.isEmpty(),
            atomCount = stats.atomCount,
            linkCount = stats.linkCount,
            issues = issues,
            warnings = warnings,
            averageAttention = avgAttention
        )
    }
    
    /**
     * Validate tensor signature against specification
     * [modality, depth, context, salience, autonomy_index]
     */
    fun validateTensorSignature(tensor: CognitiveTensor): TensorValidationResult {
        val issues = mutableListOf<String>()
        
        // Validate each dimension
        if (tensor.modality !in 0.0f..1.0f) {
            issues.add("Modality ${tensor.modality} out of range [0,1]")
        }
        
        if (tensor.depth < 0.0f) {
            issues.add("Depth ${tensor.depth} must be non-negative")
        }
        
        if (tensor.context !in 0.0f..1.0f) {
            issues.add("Context ${tensor.context} out of range [0,1]")
        }
        
        if (tensor.salience !in 0.0f..1.0f) {
            issues.add("Salience ${tensor.salience} out of range [0,1]")
        }
        
        if (tensor.autonomyIndex !in 0.0f..1.0f) {
            issues.add("Autonomy index ${tensor.autonomyIndex} out of range [0,1]")
        }
        
        // Validate extended dimensions
        if (tensor.valence !in 0.0f..1.0f) {
            issues.add("Valence ${tensor.valence} out of range [0,1]")
        }
        
        if (tensor.arousal !in 0.0f..1.0f) {
            issues.add("Arousal ${tensor.arousal} out of range [0,1]")
        }
        
        if (tensor.confidence !in 0.0f..1.0f) {
            issues.add("Confidence ${tensor.confidence} out of range [0,1]")
        }
        
        if (tensor.complexity < 0.0f) {
            issues.add("Complexity ${tensor.complexity} must be non-negative")
        }
        
        // Compute attention weight
        val attentionWeight = tensor.computeAttentionWeight()
        
        return TensorValidationResult(
            isValid = issues.isEmpty() && tensor.isValid(),
            tensor = tensor,
            attentionWeight = attentionWeight,
            issues = issues
        )
    }
    
    /**
     * Verify bidirectional translation integrity
     * ML Primitive -> AtomSpace -> ML Primitive should preserve information
     */
    fun verifyBidirectionalTranslation(primitive: MLPrimitive): TranslationVerificationResult {
        val issues = mutableListOf<String>()
        
        // Forward translation
        val atoms = translator.mlToAtomSpace(primitive)
        if (atoms.isEmpty()) {
            issues.add("Forward translation produced no atoms")
            return TranslationVerificationResult(
                success = false,
                originalPrimitive = primitive,
                reconstructedPrimitive = null,
                fidelity = 0.0f,
                issues = issues
            )
        }
        
        // Backward translation
        val reconstructed = translator.atomSpaceToML(atoms)
        if (reconstructed == null) {
            issues.add("Backward translation failed")
            return TranslationVerificationResult(
                success = false,
                originalPrimitive = primitive,
                reconstructedPrimitive = null,
                fidelity = 0.0f,
                issues = issues
            )
        }
        
        // Compute fidelity (embedding similarity)
        val fidelity = computeEmbeddingFidelity(
            primitive.embedding,
            reconstructed.embedding
        )
        
        if (fidelity < 0.9f) {
            issues.add("Low translation fidelity: $fidelity")
        }
        
        if (primitive.type != reconstructed.type) {
            issues.add("Type mismatch: ${primitive.type} != ${reconstructed.type}")
        }
        
        return TranslationVerificationResult(
            success = fidelity >= 0.9f && primitive.type == reconstructed.type,
            originalPrimitive = primitive,
            reconstructedPrimitive = reconstructed,
            fidelity = fidelity,
            issues = issues
        )
    }
    
    /**
     * Compute embedding similarity (cosine similarity)
     */
    private fun computeEmbeddingFidelity(emb1: FloatArray, emb2: FloatArray): Float {
        if (emb1.isEmpty() || emb2.isEmpty()) return 0.0f
        
        val minSize = minOf(emb1.size, emb2.size)
        if (minSize == 0) return 0.0f
        
        val e1 = emb1.take(minSize).toFloatArray()
        val e2 = emb2.take(minSize).toFloatArray()
        
        val dotProduct = e1.zip(e2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
        val norm1 = kotlin.math.sqrt(e1.sumOf { (it * it).toDouble() }.toFloat())
        val norm2 = kotlin.math.sqrt(e2.sumOf { (it * it).toDouble() }.toFloat())
        
        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (norm1 * norm2)
        } else 0.0f
    }
    
    /**
     * Comprehensive Phase 1 verification
     */
    fun runComprehensiveVerification(
        hypergraph: Hypergraph,
        tensors: List<CognitiveTensor>,
        primitives: List<MLPrimitive>
    ): Phase1VerificationReport {
        val consistencyReport = verifyHypergraphConsistency(hypergraph)
        
        val tensorValidations = tensors.map { validateTensorSignature(it) }
        val validTensors = tensorValidations.count { it.isValid }
        
        val translationVerifications = primitives.map { verifyBidirectionalTranslation(it) }
        val successfulTranslations = translationVerifications.count { it.success }
        
        val avgFidelity = if (translationVerifications.isNotEmpty()) {
            translationVerifications.map { it.fidelity }.average().toFloat()
        } else 0.0f
        
        return Phase1VerificationReport(
            timestamp = System.currentTimeMillis(),
            consistencyReport = consistencyReport,
            tensorValidationRate = if (tensors.isNotEmpty()) validTensors.toFloat() / tensors.size else 1.0f,
            translationSuccessRate = if (primitives.isNotEmpty()) successfulTranslations.toFloat() / primitives.size else 1.0f,
            averageTranslationFidelity = avgFidelity,
            totalIssues = consistencyReport.issues.size + 
                          tensorValidations.sumOf { it.issues.size } +
                          translationVerifications.sumOf { it.issues.size },
            passed = consistencyReport.isConsistent &&
                    (validTensors == tensors.size || tensors.isEmpty()) &&
                    (avgFidelity >= 0.9f || primitives.isEmpty())
        )
    }
}

/**
 * Hypergraph consistency report
 */
data class ConsistencyReport(
    val isConsistent: Boolean,
    val atomCount: Int,
    val linkCount: Int,
    val issues: List<String>,
    val warnings: List<String>,
    val averageAttention: Float
)

/**
 * Tensor validation result
 */
data class TensorValidationResult(
    val isValid: Boolean,
    val tensor: CognitiveTensor,
    val attentionWeight: Float,
    val issues: List<String>
)

/**
 * Translation verification result
 */
data class TranslationVerificationResult(
    val success: Boolean,
    val originalPrimitive: MLPrimitive,
    val reconstructedPrimitive: MLPrimitive?,
    val fidelity: Float,
    val issues: List<String>
)

/**
 * Complete Phase 1 verification report
 */
data class Phase1VerificationReport(
    val timestamp: Long,
    val consistencyReport: ConsistencyReport,
    val tensorValidationRate: Float,
    val translationSuccessRate: Float,
    val averageTranslationFidelity: Float,
    val totalIssues: Int,
    val passed: Boolean
) {
    override fun toString(): String {
        return buildString {
            appendLine("═══════════════════════════════════════════════════")
            appendLine("  PHASE 1 VERIFICATION REPORT")
            appendLine("═══════════════════════════════════════════════════")
            appendLine()
            appendLine("Timestamp: ${java.time.Instant.ofEpochMilli(timestamp)}")
            appendLine("Status: ${if (passed) "✓ PASSED" else "✗ FAILED"}")
            appendLine()
            appendLine("HYPERGRAPH CONSISTENCY:")
            appendLine("  Atoms: ${consistencyReport.atomCount}")
            appendLine("  Links: ${consistencyReport.linkCount}")
            appendLine("  Avg Attention: ${"%.3f".format(consistencyReport.averageAttention)}")
            appendLine("  Issues: ${consistencyReport.issues.size}")
            appendLine("  Warnings: ${consistencyReport.warnings.size}")
            appendLine()
            appendLine("TENSOR VALIDATION:")
            appendLine("  Validation Rate: ${"%.1f".format(tensorValidationRate * 100)}%")
            appendLine()
            appendLine("BIDIRECTIONAL TRANSLATION:")
            appendLine("  Success Rate: ${"%.1f".format(translationSuccessRate * 100)}%")
            appendLine("  Avg Fidelity: ${"%.3f".format(averageTranslationFidelity)}")
            appendLine()
            appendLine("TOTAL ISSUES: $totalIssues")
            
            if (consistencyReport.issues.isNotEmpty()) {
                appendLine()
                appendLine("CRITICAL ISSUES:")
                consistencyReport.issues.forEach { appendLine("  - $it") }
            }
            
            if (consistencyReport.warnings.isNotEmpty()) {
                appendLine()
                appendLine("WARNINGS:")
                consistencyReport.warnings.forEach { appendLine("  - $it") }
            }
            
            appendLine()
            appendLine("═══════════════════════════════════════════════════")
        }
    }
}
