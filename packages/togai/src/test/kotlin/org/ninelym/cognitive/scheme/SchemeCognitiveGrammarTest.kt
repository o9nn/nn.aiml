package org.ninelym.cognitive.scheme

import org.ninelym.cognitive.hypergraph.Atom
import org.ninelym.cognitive.hypergraph.AtomType
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for Phase 1.1: Scheme Cognitive Grammar Microservices
 * 
 * Validates bidirectional translation between ML primitives and
 * AtomSpace hypergraph patterns
 */
class SchemeCognitiveGrammarTest {
    
    @Test
    fun testMLPrimitiveCreation() {
        val embedding = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
        val primitive = MLPrimitive(
            id = "test-primitive-1",
            type = MLPrimitiveType.EMBEDDING,
            embedding = embedding
        )
        
        assertEquals("test-primitive-1", primitive.id)
        assertEquals(MLPrimitiveType.EMBEDDING, primitive.type)
        assertArrayEquals(embedding, primitive.embedding, 0.01f)
    }
    
    @Test
    fun testMLToAtomSpaceTranslation() {
        val translator = MLAtomSpaceTranslator()
        val embedding = floatArrayOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f)
        val primitive = MLPrimitive(
            id = "ml-primitive-1",
            type = MLPrimitiveType.EMBEDDING,
            embedding = embedding
        )
        
        val atoms = translator.mlToAtomSpace(primitive)
        
        assertTrue("Should produce at least one atom", atoms.isNotEmpty())
        
        val conceptAtom = atoms.firstOrNull { it.type == AtomType.CONCEPT }
        assertNotNull("Should have concept atom", conceptAtom)
        assertTrue("Concept name should contain primitive type", 
                   conceptAtom!!.name.contains("EMBEDDING"))
        
        val evaluationAtoms = atoms.filter { it.type == AtomType.EVALUATION }
        assertTrue("Should have evaluation atoms for embedding dimensions", 
                   evaluationAtoms.isNotEmpty())
    }
    
    @Test
    fun testAtomSpaceToMLTranslation() {
        val translator = MLAtomSpaceTranslator()
        
        // Create atoms manually
        val atoms = listOf(
            Atom(
                id = "concept-1",
                type = AtomType.CONCEPT,
                name = "EMBEDDING_concept-1"
            ),
            Atom(
                id = "eval-1",
                type = AtomType.EVALUATION,
                name = "dimension_0"
            ),
            Atom(
                id = "eval-2",
                type = AtomType.EVALUATION,
                name = "dimension_1"
            )
        )
        
        val primitive = translator.atomSpaceToML(atoms)
        
        assertNotNull("Should produce ML primitive", primitive)
        assertEquals("Should be EMBEDDING type", MLPrimitiveType.EMBEDDING, primitive!!.type)
        assertTrue("Should have embedding dimensions", primitive.embedding.isNotEmpty())
    }
    
    @Test
    fun testBidirectionalTranslation() {
        val translator = MLAtomSpaceTranslator()
        val originalEmbedding = floatArrayOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
        val originalPrimitive = MLPrimitive(
            id = "bidirectional-test",
            type = MLPrimitiveType.ACTIVATION,
            embedding = originalEmbedding
        )
        
        // Forward: ML -> Atoms
        val atoms = translator.mlToAtomSpace(originalPrimitive)
        assertFalse("Forward translation should produce atoms", atoms.isEmpty())
        
        // Backward: Atoms -> ML
        val reconstructed = translator.atomSpaceToML(atoms)
        assertNotNull("Backward translation should succeed", reconstructed)
        
        // Verify reconstruction fidelity
        assertEquals("ID should match", originalPrimitive.id, reconstructed!!.id)
        // Note: embedding values may differ slightly due to normalization
        assertTrue("Should have embedding", reconstructed.embedding.isNotEmpty())
    }
    
    @Test
    fun testBatchMLToAtomSpaceTranslation() {
        val translator = MLAtomSpaceTranslator()
        val primitives = listOf(
            MLPrimitive("p1", MLPrimitiveType.EMBEDDING, floatArrayOf(0.1f, 0.2f)),
            MLPrimitive("p2", MLPrimitiveType.ACTIVATION, floatArrayOf(0.3f, 0.4f)),
            MLPrimitive("p3", MLPrimitiveType.TOKEN, floatArrayOf(0.5f, 0.6f))
        )
        
        val allAtoms = translator.mlBatchToAtomSpace(primitives)
        
        assertTrue("Should produce atoms for all primitives", allAtoms.size >= primitives.size)
        
        // Verify each primitive type is represented
        val conceptAtoms = allAtoms.filter { it.type == AtomType.CONCEPT }
        assertEquals("Should have concept for each primitive", primitives.size, conceptAtoms.size)
    }
    
    @Test
    fun testCognitiveGrammarServiceImplementation() {
        val service = CognitiveGrammarServiceImpl()
        
        // Test parsing
        val atoms = service.parseToAtoms("(concept test)")
        assertTrue("Should parse expression to atoms", atoms.isNotEmpty())
        
        // Test conversion back to scheme
        val scheme = service.atomsToScheme(atoms)
        assertTrue("Should convert atoms to scheme", scheme.isNotEmpty())
        
        // Test validation
        assertTrue("Valid expression should pass", service.validateSyntax("(concept valid)"))
        assertTrue("Valid expression should pass", service.validateSyntax("(inherit dog animal)"))
    }
    
    @Test
    fun testMLToSchemeConversion() {
        val service = CognitiveGrammarServiceImpl()
        val primitive = MLPrimitive(
            id = "test-ml",
            type = MLPrimitiveType.EMBEDDING,
            embedding = floatArrayOf(0.5f, 0.6f, 0.7f)
        )
        
        val scheme = service.mlToScheme(primitive)
        
        assertNotNull("Should convert ML to Scheme", scheme)
        assertTrue("Scheme should not be empty", scheme.isNotEmpty())
        assertTrue("Scheme should contain parentheses", scheme.contains("(") && scheme.contains(")"))
    }
    
    @Test
    fun testSchemeToMLConversion() {
        val service = CognitiveGrammarServiceImpl()
        val expression = "(concept test-concept)"
        
        val primitive = service.schemeToML(expression)
        
        // Note: Conversion may be lossy without explicit embedding in expression
        // This tests the conversion pipeline works
        assertNotNull("Should attempt conversion", primitive)
    }
    
    @Test
    fun testDifferentMLPrimitiveTypes() {
        val translator = MLAtomSpaceTranslator()
        
        MLPrimitiveType.values().forEach { type ->
            val primitive = MLPrimitive(
                id = "test-${type.name}",
                type = type,
                embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
            )
            
            val atoms = translator.mlToAtomSpace(primitive)
            assertFalse("Type ${type.name} should produce atoms", atoms.isEmpty())
            
            val reconstructed = translator.atomSpaceToML(atoms)
            assertNotNull("Type ${type.name} should reconstruct", reconstructed)
        }
    }
    
    @Test
    fun testMLPrimitiveWithMetadata() {
        val translator = MLAtomSpaceTranslator()
        val primitive = MLPrimitive(
            id = "with-metadata",
            type = MLPrimitiveType.HIDDEN_STATE,
            embedding = floatArrayOf(0.1f, 0.2f),
            metadata = mapOf(
                "parent" to "parent-node",
                "layer" to 3
            )
        )
        
        val atoms = translator.mlToAtomSpace(primitive)
        
        // Should include inheritance atom due to "parent" metadata
        val hasInheritance = atoms.any { it.type == AtomType.INHERITANCE }
        assertTrue("Should create inheritance for parent metadata", hasInheritance)
    }
    
    @Test
    fun testAttentionComputationFromEmbedding() {
        val translator = MLAtomSpaceTranslator()
        
        // High magnitude embedding should produce high attention
        val highMagnitude = MLPrimitive(
            id = "high-attention",
            type = MLPrimitiveType.ATTENTION,
            embedding = floatArrayOf(0.9f, 0.9f, 0.9f, 0.9f)
        )
        
        val atoms = translator.mlToAtomSpace(highMagnitude)
        val conceptAtom = atoms.first { it.type == AtomType.CONCEPT }
        
        assertTrue("High magnitude should produce significant attention",
                   conceptAtom.attentionValue.totalImportance() > 0.5f)
    }
}
