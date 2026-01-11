package org.ninelym.cognitive.verification

import org.ninelym.cognitive.CognitiveTensor
import org.ninelym.cognitive.hypergraph.*
import org.ninelym.cognitive.scheme.MLPrimitive
import org.ninelym.cognitive.scheme.MLPrimitiveType
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for Phase 1.3: Verification & Visualization
 * 
 * Validates hypergraph consistency checks, tensor signature validation,
 * and bidirectional translation verification
 */
class Phase1VerificationTest {
    
    @Test
    fun testHypergraphConsistencyVerification() {
        val framework = Phase1VerificationFramework()
        val hypergraph = Hypergraph()
        
        // Add valid atoms
        hypergraph.addAtom(Atom("a1", AtomType.CONCEPT, "concept1"))
        hypergraph.addAtom(Atom("a2", AtomType.PREDICATE, "predicate1"))
        hypergraph.addAtom(Atom("a3", AtomType.LINK, "link1"))
        
        val report = framework.verifyHypergraphConsistency(hypergraph)
        
        assertTrue("Hypergraph should be consistent", report.isConsistent)
        assertEquals("Should have 3 atoms", 3, report.atomCount)
        assertTrue("Should have no issues", report.issues.isEmpty())
    }
    
    @Test
    fun testHypergraphConsistencyWithInvalidAtoms() {
        val framework = Phase1VerificationFramework()
        val hypergraph = Hypergraph()
        
        // Add atom with invalid truth value
        val invalidAtom = Atom(
            id = "invalid-1",
            type = AtomType.CONCEPT,
            name = "invalid-concept",
            truthValue = TruthValue(1.5f, 0.5f)  // Invalid strength > 1.0
        )
        hypergraph.addAtom(invalidAtom)
        
        val report = framework.verifyHypergraphConsistency(hypergraph)
        
        assertFalse("Hypergraph should not be consistent", report.isConsistent)
        assertFalse("Should have issues", report.issues.isEmpty())
    }
    
    @Test
    fun testTensorSignatureValidation() {
        val framework = Phase1VerificationFramework()
        
        // Valid tensor
        val validTensor = CognitiveTensor(
            modality = 0.5f,
            depth = 1.0f,
            context = 0.7f,
            salience = 0.8f,
            autonomyIndex = 0.6f
        )
        
        val result = framework.validateTensorSignature(validTensor)
        
        assertTrue("Valid tensor should pass", result.isValid)
        assertTrue("Should have no issues", result.issues.isEmpty())
        assertEquals("Tensor should match", validTensor, result.tensor)
        assertTrue("Attention weight should be positive", result.attentionWeight > 0)
    }
    
    @Test
    fun testTensorSignatureValidationWithInvalidValues() {
        val framework = Phase1VerificationFramework()
        
        // Invalid tensor - modality out of range
        val invalidTensor = CognitiveTensor(
            modality = 1.5f,  // Invalid > 1.0
            depth = 1.0f,
            context = 0.7f,
            salience = 0.8f,
            autonomyIndex = 0.6f
        )
        
        val result = framework.validateTensorSignature(invalidTensor)
        
        assertFalse("Invalid tensor should fail", result.isValid)
        assertFalse("Should have issues", result.issues.isEmpty())
        assertTrue("Should report modality issue", 
                   result.issues.any { it.contains("Modality") })
    }
    
    @Test
    fun testBidirectionalTranslationVerification() {
        val framework = Phase1VerificationFramework()
        
        val primitive = MLPrimitive(
            id = "test-primitive",
            type = MLPrimitiveType.EMBEDDING,
            embedding = floatArrayOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f)
        )
        
        val result = framework.verifyBidirectionalTranslation(primitive)
        
        assertTrue("Translation should succeed", result.success)
        assertEquals("Original primitive should match", primitive, result.originalPrimitive)
        assertNotNull("Reconstructed primitive should exist", result.reconstructedPrimitive)
        assertTrue("Fidelity should be high", result.fidelity > 0.5f)
    }
    
    @Test
    fun testBidirectionalTranslationWithDifferentTypes() {
        val framework = Phase1VerificationFramework()
        
        MLPrimitiveType.values().forEach { type ->
            val primitive = MLPrimitive(
                id = "test-${type.name}",
                type = type,
                embedding = floatArrayOf(0.3f, 0.5f, 0.7f)
            )
            
            val result = framework.verifyBidirectionalTranslation(primitive)
            
            assertNotNull("Should handle type ${type.name}", result.reconstructedPrimitive)
            assertTrue("Should have some fidelity for ${type.name}", result.fidelity >= 0.0f)
        }
    }
    
    @Test
    fun testComprehensivePhase1Verification() {
        val framework = Phase1VerificationFramework()
        
        // Create test hypergraph
        val hypergraph = Hypergraph()
        hypergraph.addAtom(Atom("a1", AtomType.CONCEPT, "concept1"))
        hypergraph.addAtom(Atom("a2", AtomType.PREDICATE, "predicate1"))
        
        // Create test tensors
        val tensors = listOf(
            CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.6f),
            CognitiveTensor(0.3f, 0.8f, 0.6f, 0.9f, 0.7f)
        )
        
        // Create test primitives
        val primitives = listOf(
            MLPrimitive("p1", MLPrimitiveType.EMBEDDING, floatArrayOf(0.5f, 0.6f, 0.7f)),
            MLPrimitive("p2", MLPrimitiveType.ACTIVATION, floatArrayOf(0.3f, 0.4f, 0.5f))
        )
        
        val report = framework.runComprehensiveVerification(hypergraph, tensors, primitives)
        
        assertNotNull("Report should not be null", report)
        assertTrue("Report should pass", report.passed)
        assertEquals("Should validate all tensors", 1.0f, report.tensorValidationRate, 0.01f)
        assertTrue("Should have high translation success", report.translationSuccessRate > 0.0f)
        assertTrue("Report timestamp should be set", report.timestamp > 0)
    }
    
    @Test
    fun testComprehensiveVerificationWithIssues() {
        val framework = Phase1VerificationFramework()
        
        // Create hypergraph with issues
        val hypergraph = Hypergraph()
        val invalidAtom = Atom(
            id = "invalid",
            type = AtomType.CONCEPT,
            name = "invalid",
            truthValue = TruthValue(2.0f, 0.5f)  // Invalid
        )
        hypergraph.addAtom(invalidAtom)
        
        // Create invalid tensors
        val tensors = listOf(
            CognitiveTensor(1.5f, 1.0f, 0.7f, 0.8f, 0.6f)  // Invalid modality
        )
        
        val primitives = emptyList<MLPrimitive>()
        
        val report = framework.runComprehensiveVerification(hypergraph, tensors, primitives)
        
        assertFalse("Report should fail with issues", report.passed)
        assertTrue("Should have issues", report.totalIssues > 0)
    }
    
    @Test
    fun testVerificationReportToString() {
        val framework = Phase1VerificationFramework()
        
        val hypergraph = Hypergraph()
        hypergraph.addAtom(Atom("a1", AtomType.CONCEPT, "concept1"))
        
        val tensors = listOf(CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.6f))
        val primitives = emptyList<MLPrimitive>()
        
        val report = framework.runComprehensiveVerification(hypergraph, tensors, primitives)
        val reportString = report.toString()
        
        assertNotNull("Report string should not be null", reportString)
        assertTrue("Should contain 'VERIFICATION REPORT'", reportString.contains("VERIFICATION REPORT"))
        assertTrue("Should contain status", reportString.contains("PASSED") || reportString.contains("FAILED"))
        assertTrue("Should contain hypergraph info", reportString.contains("HYPERGRAPH"))
        assertTrue("Should contain tensor info", reportString.contains("TENSOR"))
    }
    
    @Test
    fun testConsistencyReportWithWarnings() {
        val framework = Phase1VerificationFramework()
        val hypergraph = Hypergraph()
        
        // Empty hypergraph should generate warnings
        val report = framework.verifyHypergraphConsistency(hypergraph)
        
        assertTrue("Empty hypergraph should still be consistent", report.isConsistent)
        assertFalse("Should have warnings", report.warnings.isEmpty())
        assertTrue("Should warn about empty hypergraph", 
                   report.warnings.any { it.contains("empty") })
    }
    
    @Test
    fun testTensorValidationAllDimensions() {
        val framework = Phase1VerificationFramework()
        
        // Test each dimension separately
        val testCases = listOf(
            CognitiveTensor(-0.1f, 1.0f, 0.5f, 0.5f, 0.5f),  // Invalid modality
            CognitiveTensor(0.5f, -1.0f, 0.5f, 0.5f, 0.5f),  // Invalid depth
            CognitiveTensor(0.5f, 1.0f, 1.5f, 0.5f, 0.5f),   // Invalid context
            CognitiveTensor(0.5f, 1.0f, 0.5f, -0.1f, 0.5f),  // Invalid salience
            CognitiveTensor(0.5f, 1.0f, 0.5f, 0.5f, 1.5f)    // Invalid autonomy
        )
        
        testCases.forEach { tensor ->
            val result = framework.validateTensorSignature(tensor)
            assertFalse("Invalid tensor should fail validation", result.isValid)
            assertFalse("Should report specific issue", result.issues.isEmpty())
        }
    }
    
    @Test
    fun testTranslationFidelityMetrics() {
        val framework = Phase1VerificationFramework()
        
        // Test with high-quality embedding
        val highQualityPrimitive = MLPrimitive(
            id = "high-quality",
            type = MLPrimitiveType.EMBEDDING,
            embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f)
        )
        
        val result = framework.verifyBidirectionalTranslation(highQualityPrimitive)
        
        assertTrue("High quality translation should succeed", result.success)
        assertTrue("Fidelity should be reasonable", result.fidelity >= 0.0f && result.fidelity <= 1.0f)
    }
    
    @Test
    fun testMultiplePrimitiveVerification() {
        val framework = Phase1VerificationFramework()
        
        val primitives = (1..10).map { i ->
            MLPrimitive(
                id = "primitive-$i",
                type = MLPrimitiveType.EMBEDDING,
                embedding = FloatArray(5) { (it + i) / 15.0f }
            )
        }
        
        val results = primitives.map { framework.verifyBidirectionalTranslation(it) }
        
        assertTrue("Most translations should succeed", 
                   results.count { it.success } > primitives.size / 2)
        
        results.forEach { result ->
            assertNotNull("Each should have reconstructed primitive", result.reconstructedPrimitive)
        }
    }
}
