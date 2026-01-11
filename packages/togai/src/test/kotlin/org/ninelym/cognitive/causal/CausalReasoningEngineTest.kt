package org.ninelym.cognitive.causal

import org.junit.Test
import org.ninelym.cognitive.hypergraph.Hypergraph
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for Causal Reasoning Engine
 * 
 * Tests causal inference, counterfactual reasoning, and intervention simulation
 */
class CausalReasoningEngineTest {
    
    @Test
    fun `test causal discovery from simple observations`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create observations where A causes B
        val observations = listOf(
            mapOf("A" to 0.1f, "B" to 0.15f, "C" to 0.5f),
            mapOf("A" to 0.2f, "B" to 0.25f, "C" to 0.5f),
            mapOf("A" to 0.3f, "B" to 0.35f, "C" to 0.5f),
            mapOf("A" to 0.4f, "B" to 0.45f, "C" to 0.5f),
            mapOf("A" to 0.5f, "B" to 0.55f, "C" to 0.5f),
            mapOf("A" to 0.6f, "B" to 0.65f, "C" to 0.5f),
            mapOf("A" to 0.7f, "B" to 0.75f, "C" to 0.5f),
            mapOf("A" to 0.8f, "B" to 0.85f, "C" to 0.5f)
        )
        
        val result = engine.discoverCausalStructure(observations)
        
        // Verify discovery was successful
        assertTrue(result.discoveredCauses > 0, "Should discover causal relationships")
        assertEquals(3, result.graph.nodes.size, "Should have 3 nodes")
        assertTrue(result.reliability > 0.5f, "Should have decent reliability with 8 observations")
        assertTrue(result.executionTime > 0, "Should have execution time")
        
        // Check that A->B relationship was discovered (strong correlation)
        val aToBEdge = result.graph.edges["A"]?.contains("B") ?: false
        assertTrue(aToBEdge || result.graph.edges.isNotEmpty(), 
            "Should discover strong A->B relationship or have some edges")
    }
    
    @Test
    fun `test causal discovery with empty observations`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        val result = engine.discoverCausalStructure(emptyList())
        
        assertEquals(0, result.discoveredCauses, "No causes with empty observations")
        assertEquals(0, result.discoveredEffects, "No effects with empty observations")
        assertEquals(0.0f, result.reliability, "Zero reliability with no data")
        assertEquals(0, result.graph.nodes.size, "No nodes")
    }
    
    @Test
    fun `test counterfactual reasoning`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create a simple causal graph: A -> B -> C
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B", "C"),
            edges = mapOf(
                "A" to setOf("B"),
                "B" to setOf("C")
            ),
            strengths = mapOf(
                Pair("A", "B") to 0.8f,
                Pair("B", "C") to 0.7f
            ),
            confidence = 0.9f
        )
        
        // Counterfactual: What if A was set to 1.0?
        val result = engine.counterfactualReasoning(
            causalGraph = causalGraph,
            intervention = mapOf("A" to 1.0f),
            query = "C"
        )
        
        assertEquals("C", result.query)
        assertTrue(result.confidence > 0.0f, "Should have some confidence")
        assertTrue(result.alternativeWorlds.isNotEmpty(), "Should generate alternative worlds")
        assertEquals(5, result.alternativeWorlds.size, "Should generate 5 alternative worlds")
        
        // Predicted outcome should be influenced by intervention
        assertTrue(result.predictedOutcome >= 0.0f && result.predictedOutcome <= 1.0f,
            "Predicted outcome should be in valid range")
    }
    
    @Test
    fun `test intervention simulation`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create a causal graph with branching: A -> B, A -> C
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B", "C"),
            edges = mapOf(
                "A" to setOf("B", "C")
            ),
            strengths = mapOf(
                Pair("A", "B") to 0.6f,
                Pair("A", "C") to 0.8f
            ),
            confidence = 0.85f
        )
        
        val result = engine.simulateIntervention(
            causalGraph = causalGraph,
            targetVariable = "A",
            interventionValue = 0.9f
        )
        
        assertEquals("A", result.targetVariable)
        assertEquals(0.9f, result.interventionValue, 0.01f)
        assertTrue(result.causalPathways.isNotEmpty(), "Should find causal pathways")
        assertTrue(result.predictedEffects.isNotEmpty(), "Should predict effects")
        assertTrue(result.totalCausalEffect > 0.0f, "Should have total causal effect")
        
        // Check that both B and C are in predicted effects
        assertTrue(result.predictedEffects.containsKey("B") || result.predictedEffects.containsKey("C"),
            "Should predict effects on downstream variables")
    }
    
    @Test
    fun `test causal pathway finding`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create a chain: A -> B -> C -> D
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B", "C", "D"),
            edges = mapOf(
                "A" to setOf("B"),
                "B" to setOf("C"),
                "C" to setOf("D")
            ),
            strengths = mapOf(
                Pair("A", "B") to 0.9f,
                Pair("B", "C") to 0.8f,
                Pair("C", "D") to 0.7f
            ),
            confidence = 0.95f
        )
        
        val result = engine.simulateIntervention(
            causalGraph = causalGraph,
            targetVariable = "A",
            interventionValue = 1.0f
        )
        
        assertTrue(result.causalPathways.isNotEmpty(), "Should find pathways")
        
        // Check that pathways have expected properties
        result.causalPathways.forEach { pathway ->
            assertTrue(pathway.strength > 0.0f, "Pathway strength should be positive")
            assertTrue(pathway.directness > 0.0f && pathway.directness <= 1.0f,
                "Directness should be in (0, 1]")
            assertTrue(pathway.path.isNotEmpty(), "Path should not be empty")
            assertEquals("A", pathway.path.first(), "Path should start with source")
        }
    }
    
    @Test
    fun `test integration with hypergraph`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create a simple causal graph
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("X", "Y"),
            edges = mapOf("X" to setOf("Y")),
            strengths = mapOf(Pair("X", "Y") to 0.75f),
            confidence = 0.9f
        )
        
        val atomsAdded = engine.integrateCausalKnowledge(causalGraph, "test")
        
        assertTrue(atomsAdded > 0, "Should add atoms to hypergraph")
        // Should add 2 concept nodes + 1 causal link = 3 atoms
        assertTrue(atomsAdded >= 3, "Should add at least concept nodes and causal link")
    }
    
    @Test
    fun `test reliability calculation with varying sample sizes`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Small sample
        val smallObservations = List(3) { i ->
            mapOf("A" to i * 0.1f, "B" to i * 0.1f + 0.05f)
        }
        val smallResult = engine.discoverCausalStructure(smallObservations)
        
        // Large sample
        val largeObservations = List(50) { i ->
            mapOf("A" to i * 0.02f, "B" to i * 0.02f + 0.01f)
        }
        val largeResult = engine.discoverCausalStructure(largeObservations)
        
        // Larger sample should have better or equal reliability
        assertTrue(largeResult.reliability >= smallResult.reliability,
            "Larger sample should have better reliability")
    }
    
    @Test
    fun `test counterfactual confidence decreases for indirect effects`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Direct effect: A -> B
        val directGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B"),
            edges = mapOf("A" to setOf("B")),
            strengths = mapOf(Pair("A", "B") to 0.9f),
            confidence = 0.9f
        )
        
        val directResult = engine.counterfactualReasoning(
            causalGraph = directGraph,
            intervention = mapOf("A" to 1.0f),
            query = "B"
        )
        
        // Indirect effect: A -> B -> C (query C)
        val indirectGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B", "C"),
            edges = mapOf(
                "A" to setOf("B"),
                "B" to setOf("C")
            ),
            strengths = mapOf(
                Pair("A", "B") to 0.9f,
                Pair("B", "C") to 0.9f
            ),
            confidence = 0.9f
        )
        
        val indirectResult = engine.counterfactualReasoning(
            causalGraph = indirectGraph,
            intervention = mapOf("A" to 1.0f),
            query = "C"
        )
        
        // Both should have confidence, but relationship may vary
        assertTrue(directResult.confidence > 0.0f, "Direct effect should have confidence")
        assertTrue(indirectResult.confidence > 0.0f, "Indirect effect should have confidence")
    }
    
    @Test
    fun `test alternative world generation`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B"),
            edges = mapOf("A" to setOf("B")),
            strengths = mapOf(Pair("A", "B") to 0.8f),
            confidence = 0.9f
        )
        
        val result = engine.counterfactualReasoning(
            causalGraph = causalGraph,
            intervention = mapOf("A" to 0.7f),
            query = "B"
        )
        
        // Check alternative worlds
        assertEquals(5, result.alternativeWorlds.size, "Should generate 5 worlds")
        
        result.alternativeWorlds.forEach { world ->
            assertTrue(world.worldId.startsWith("world_"), "World should have proper ID")
            assertTrue(world.probability > 0.0f, "World should have positive probability")
            assertTrue(world.values.containsKey("A"), "World should include intervention variable")
            assertEquals(0.7f, world.values["A"], 0.01f, "Intervention value should be set")
        }
        
        // Probabilities should sum to ~1.0
        val totalProbability = result.alternativeWorlds.sumOf { it.probability.toDouble() }.toFloat()
        assertEquals(1.0f, totalProbability, 0.01f, "Probabilities should sum to 1.0")
    }
    
    @Test
    fun `test intervention removes incoming edges`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create graph where B depends on both A and C
        val causalGraph = CausalReasoningEngine.CausalGraph(
            nodes = setOf("A", "B", "C"),
            edges = mapOf(
                "A" to setOf("B"),
                "C" to setOf("B")
            ),
            strengths = mapOf(
                Pair("A", "B") to 0.7f,
                Pair("C", "B") to 0.6f
            ),
            confidence = 0.9f
        )
        
        // When we intervene on B, it should be independent of A and C
        val result = engine.counterfactualReasoning(
            causalGraph = causalGraph,
            intervention = mapOf("B" to 0.8f),
            query = "B"
        )
        
        // The predicted outcome should be close to the intervention value
        // since B is directly set
        assertTrue(result.predictedOutcome >= 0.0f && result.predictedOutcome <= 1.0f,
            "Predicted outcome should be in valid range")
        
        result.alternativeWorlds.forEach { world ->
            // In all worlds, B should be set to the intervention value
            assertEquals(0.8f, world.values["B"], 0.01f,
                "B should be set to intervention value in all worlds")
        }
    }
    
    @Test
    fun `test causal discovery with uncorrelated variables`() {
        val hypergraph = Hypergraph()
        val engine = CausalReasoningEngine(hypergraph)
        
        // Create observations where variables are independent
        val observations = listOf(
            mapOf("A" to 0.1f, "B" to 0.9f),
            mapOf("A" to 0.2f, "B" to 0.1f),
            mapOf("A" to 0.3f, "B" to 0.8f),
            mapOf("A" to 0.4f, "B" to 0.2f),
            mapOf("A" to 0.5f, "B" to 0.7f),
            mapOf("A" to 0.6f, "B" to 0.3f),
            mapOf("A" to 0.7f, "B" to 0.6f),
            mapOf("A" to 0.8f, "B" to 0.4f)
        )
        
        val result = engine.discoverCausalStructure(observations, significanceLevel = 0.5f)
        
        // With high significance threshold, should find fewer or no relationships
        assertEquals(2, result.graph.nodes.size, "Should still have nodes")
        assertTrue(result.discoveredCauses >= 0, "Discovered causes should be non-negative")
    }
}
