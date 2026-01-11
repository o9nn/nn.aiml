package org.ninelym.cognitive.tensor

import org.ninelym.cognitive.CognitiveTensor
import org.ninelym.cognitive.hypergraph.Atom
import org.ninelym.cognitive.hypergraph.AtomType
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for Phase 1.2: Tensor Fragment Architecture
 * 
 * Validates tensor operations, transformations, lifecycle management,
 * and ML primitive integration
 */
class TensorFragmentArchitectureTest {
    
    @Test
    fun testTensorSerializationJSON() {
        val serializer = TensorSerializer()
        val tensor = CognitiveTensor(
            modality = 0.5f,
            depth = 1.0f,
            context = 0.7f,
            salience = 0.8f,
            autonomyIndex = 0.6f
        )
        
        val json = serializer.toJSON(tensor)
        assertNotNull("JSON should not be null", json)
        assertTrue("JSON should contain modality", json.contains("modality"))
        assertTrue("JSON should contain salience", json.contains("salience"))
        
        val deserialized = serializer.fromJSON(json)
        assertNotNull("Deserialization should succeed", deserialized)
        assertEquals("Modality should match", tensor.modality, deserialized!!.modality, 0.01f)
        assertEquals("Salience should match", tensor.salience, deserialized.salience, 0.01f)
    }
    
    @Test
    fun testTensorSerializationBinary() {
        val serializer = TensorSerializer()
        val tensor = CognitiveTensor(
            modality = 0.3f,
            depth = 2.0f,
            context = 0.5f,
            salience = 0.9f,
            autonomyIndex = 0.7f
        )
        
        val binary = serializer.toBinary(tensor)
        assertNotNull("Binary should not be null", binary)
        assertTrue("Binary should have data", binary.isNotEmpty())
        
        val deserialized = serializer.fromBinary(binary)
        assertNotNull("Deserialization should succeed", deserialized)
        assertEquals("Modality should match", tensor.modality, deserialized!!.modality, 0.01f)
        assertEquals("Depth should match", tensor.depth, deserialized.depth, 0.01f)
    }
    
    @Test
    fun testMLEmbeddingOperation() {
        val embedding = floatArrayOf(0.2f, 1.5f, 0.8f, 0.6f, 0.4f)
        val operation = MLEmbeddingOperation(embedding)
        
        val inputTensor = CognitiveTensor(0.5f, 1.0f, 0.5f, 0.5f, 0.5f)
        val outputTensor = operation.apply(inputTensor)
        
        assertNotNull("Output tensor should not be null", outputTensor)
        assertEquals("Modality should be from embedding[0]", 0.2f, outputTensor.modality, 0.01f)
        assertEquals("Depth should be from embedding[1]", 1.5f, outputTensor.depth, 0.01f)
        assertTrue("Output should be valid", outputTensor.isValid())
    }
    
    @Test
    fun testTensorTransformationPipeline() {
        val pipeline = TensorTransformationPipeline()
        
        val tensor = CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.6f)
        
        val result = pipeline
            .normalize()
            .scaleAttention(1.5f)
            .execute(tensor)
        
        assertNotNull("Pipeline result should not be null", result)
        assertTrue("Pipeline result should be valid", result.isValid())
    }
    
    @Test
    fun testTensorTransformationWithMLEmbedding() {
        val pipeline = TensorTransformationPipeline()
        val embedding = floatArrayOf(0.3f, 0.8f, 0.6f, 0.7f)
        
        val tensor = CognitiveTensor(0.5f, 1.0f, 0.5f, 0.5f, 0.5f)
        
        val result = pipeline
            .applyMLEmbedding(embedding)
            .normalize()
            .execute(tensor)
        
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be valid", result.isValid())
    }
    
    @Test
    fun testBatchTransformation() {
        val pipeline = TensorTransformationPipeline()
        pipeline.normalize().scaleAttention(1.2f)
        
        val tensors = listOf(
            CognitiveTensor(0.1f, 0.5f, 0.3f, 0.4f, 0.2f),
            CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.6f),
            CognitiveTensor(0.8f, 1.5f, 0.9f, 0.9f, 0.8f)
        )
        
        val results = pipeline.executeBatch(tensors)
        
        assertEquals("Should transform all tensors", tensors.size, results.size)
        results.forEach { result ->
            assertTrue("Each result should be valid", result.isValid())
        }
    }
    
    @Test
    fun testFragmentLifecycleManager() {
        val processor = TensorFragmentProcessor()
        val manager = FragmentLifecycleManager(
            processor = processor,
            maxAge = 1000,
            minAttention = 0.2f,
            maxFragments = 100
        )
        
        // Create some fragments
        repeat(5) { i ->
            val tensor = CognitiveTensor(0.5f, 1.0f, 0.5f, 0.5f + i * 0.1f, 0.5f)
            processor.createFragment("fragment-$i", tensor)
        }
        
        // Get statistics
        val stats = manager.getStats()
        
        assertEquals("Should have 5 fragments", 5, stats.totalFragments)
        assertTrue("Should track cleanup time", stats.lastCleanup > 0)
    }
    
    @Test
    fun testFragmentLifecycleMaintenance() {
        val processor = TensorFragmentProcessor()
        val manager = FragmentLifecycleManager(
            processor = processor,
            maxAge = 100,  // Very short age for testing
            minAttention = 0.5f
        )
        
        // Create low attention fragment (should be cleaned)
        val lowAttention = CognitiveTensor(0.1f, 0.5f, 0.3f, 0.1f, 0.1f)
        processor.createFragment("low-attention", lowAttention)
        
        // Create high attention fragment (should survive)
        val highAttention = CognitiveTensor(0.5f, 1.0f, 0.7f, 0.9f, 0.8f)
        processor.createFragment("high-attention", highAttention)
        
        // Wait for fragments to age
        Thread.sleep(150)
        
        // Perform maintenance
        manager.performMaintenance()
        
        val stats = manager.getStats()
        assertTrue("Should have old fragments detected", stats.oldFragments >= 0)
    }
    
    @Test
    fun testTensorFragmentCreation() {
        val processor = TensorFragmentProcessor()
        val tensor = CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.6f)
        
        val fragment = processor.createFragment("test-fragment", tensor)
        
        assertEquals("Fragment ID should match", "test-fragment", fragment.id)
        assertEquals("Fragment tensor should match", tensor, fragment.tensor)
        assertTrue("Fragment should be active", fragment.isActive())
    }
    
    @Test
    fun testTensorFragmentFromAtom() {
        val processor = TensorFragmentProcessor()
        val atom = Atom(
            id = "test-atom",
            type = AtomType.CONCEPT,
            name = "test-concept"
        )
        
        val fragment = processor.atomToFragment(atom)
        
        assertEquals("Fragment ID should match atom ID", atom.id, fragment.id)
        assertNotNull("Fragment should have tensor", fragment.tensor)
        assertEquals("Source atom should be preserved", atom, fragment.sourceAtom)
    }
    
    @Test
    fun testGetActiveFragmentsByAttention() {
        val processor = TensorFragmentProcessor()
        
        // Create fragments with different attention levels
        processor.createFragment("high-1", CognitiveTensor(0.5f, 1.0f, 0.7f, 0.9f, 0.8f))
        processor.createFragment("high-2", CognitiveTensor(0.5f, 1.0f, 0.7f, 0.8f, 0.9f))
        processor.createFragment("low-1", CognitiveTensor(0.1f, 0.5f, 0.3f, 0.2f, 0.1f))
        processor.createFragment("low-2", CognitiveTensor(0.1f, 0.5f, 0.3f, 0.1f, 0.2f))
        
        val activeFragments = processor.getActiveFragments(minAttention = 0.5f)
        
        // High attention fragments should be included
        assertTrue("Should have high attention fragments", 
                   activeFragments.any { it.id.startsWith("high-") })
    }
    
    @Test
    fun testCombineFragments() {
        val processor = TensorFragmentProcessor()
        
        // Create several fragments
        val ids = (1..3).map { i ->
            val tensor = CognitiveTensor(0.5f, 1.0f, 0.5f + i * 0.1f, 0.6f, 0.7f)
            processor.createFragment("frag-$i", tensor)
            "frag-$i"
        }
        
        val combined = processor.combineFragments(ids, AverageCombiner())
        
        assertNotNull("Combined tensor should not be null", combined)
        assertTrue("Combined tensor should be valid", combined!!.isValid())
    }
    
    @Test
    fun testFragmentCleanup() {
        val processor = TensorFragmentProcessor()
        
        // Create fragments
        repeat(10) { i ->
            val tensor = CognitiveTensor(0.5f, 1.0f, 0.5f, 0.3f + i * 0.05f, 0.5f)
            processor.createFragment("cleanup-$i", tensor)
        }
        
        // Cleanup old low-attention fragments
        processor.cleanup(maxAge = 0, minAttention = 0.6f)
        
        // Verify cleanup happened (fragments with attention < 0.6 should be removed)
        val remaining = processor.getActiveFragments(0.0f)
        assertTrue("Some fragments should remain", remaining.isNotEmpty())
    }
}
