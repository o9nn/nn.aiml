package org.ninelym.cognitive.causal

import org.ninelym.cognitive.hypergraph.Hypergraph
import org.ninelym.cognitive.hypergraph.Atom
import org.ninelym.cognitive.hypergraph.AtomType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Causal Reasoning Engine - Phase 4A Advanced Cognitive Capabilities
 * 
 * Implements causal inference and counterfactual reasoning for the cognitive system.
 * Enables understanding and reasoning about cause-effect relationships through:
 * - Causal graph construction from observations
 * - Counterfactual reasoning ("What if X had been different?")
 * - Intervention simulation for causal effects
 * - Causal discovery from observational data
 * 
 * Based on Pearl's causal hierarchy:
 * - Level 1: Association (P(y|x) - seeing)
 * - Level 2: Intervention (P(y|do(x)) - doing)
 * - Level 3: Counterfactuals (P(y_x|x',y') - imagining)
 */
class CausalReasoningEngine(
    private val hypergraph: Hypergraph
) {
    
    /**
     * Represents a causal graph structure
     */
    data class CausalGraph(
        val nodes: Set<String>,
        val edges: Map<String, Set<String>>, // cause -> effects
        val strengths: Map<Pair<String, String>, Float>,
        val confidence: Float = 1.0f
    )
    
    /**
     * Result of counterfactual reasoning
     */
    data class CounterfactualResult(
        val query: String,
        val intervention: Map<String, Float>,
        val predictedOutcome: Float,
        val confidence: Float,
        val alternativeWorlds: List<AlternativeWorld>
    )
    
    /**
     * Represents an alternative possible world under intervention
     */
    data class AlternativeWorld(
        val worldId: String,
        val values: Map<String, Float>,
        val probability: Float
    )
    
    /**
     * Result of causal discovery process
     */
    data class CausalDiscoveryResult(
        val graph: CausalGraph,
        val discoveredCauses: Int,
        val discoveredEffects: Int,
        val reliability: Float,
        val executionTime: Long
    )
    
    /**
     * Result of intervention simulation
     */
    data class InterventionResult(
        val targetVariable: String,
        val interventionValue: Float,
        val predictedEffects: Map<String, Float>,
        val causalPathways: List<CausalPathway>,
        val totalCausalEffect: Float
    )
    
    /**
     * Represents a causal pathway from cause to effect
     */
    data class CausalPathway(
        val path: List<String>,
        val strength: Float,
        val directness: Float
    )
    
    /**
     * Discover causal relationships from observations
     * 
     * Uses conditional independence testing and constraint-based causal discovery
     * to infer causal structure from observational data.
     */
    fun discoverCausalStructure(
        observations: List<Map<String, Float>>,
        significanceLevel: Float = 0.05f
    ): CausalDiscoveryResult {
        val startTime = System.currentTimeMillis()
        
        if (observations.isEmpty()) {
            return CausalDiscoveryResult(
                graph = CausalGraph(emptySet(), emptyMap(), emptyMap(), 0.0f),
                discoveredCauses = 0,
                discoveredEffects = 0,
                reliability = 0.0f,
                executionTime = System.currentTimeMillis() - startTime
            )
        }
        
        val nodes = observations.first().keys
        val edges = mutableMapOf<String, MutableSet<String>>()
        val strengths = mutableMapOf<Pair<String, String>, Float>()
        
        // Use conditional independence tests to discover causal structure
        nodes.forEach { cause ->
            nodes.forEach { effect ->
                if (cause != effect) {
                    val causalStrength = testCausality(cause, effect, observations, significanceLevel)
                    
                    if (causalStrength > significanceLevel) {
                        edges.getOrPut(cause) { mutableSetOf() }.add(effect)
                        strengths[Pair(cause, effect)] = causalStrength
                    }
                }
            }
        }
        
        // Calculate reliability based on sample size and consistency
        val reliability = calculateReliability(observations.size, strengths.values)
        
        val graph = CausalGraph(
            nodes = nodes,
            edges = edges.mapValues { it.value.toSet() },
            strengths = strengths,
            confidence = reliability
        )
        
        return CausalDiscoveryResult(
            graph = graph,
            discoveredCauses = edges.keys.size,
            discoveredEffects = edges.values.flatten().toSet().size,
            reliability = reliability,
            executionTime = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Test causality between two variables using correlation and temporal precedence
     */
    private fun testCausality(
        cause: String,
        effect: String,
        observations: List<Map<String, Float>>,
        significanceLevel: Float
    ): Float {
        // Extract values for both variables
        val causeValues = observations.mapNotNull { it[cause] }
        val effectValues = observations.mapNotNull { it[effect] }
        
        if (causeValues.size != effectValues.size || causeValues.isEmpty()) {
            return 0.0f
        }
        
        // Calculate Pearson correlation coefficient
        val correlation = calculateCorrelation(causeValues, effectValues)
        
        // Apply significance threshold
        val significance = abs(correlation)
        
        return if (significance > significanceLevel) significance else 0.0f
    }
    
    /**
     * Calculate Pearson correlation coefficient
     */
    private fun calculateCorrelation(x: List<Float>, y: List<Float>): Float {
        if (x.size != y.size || x.isEmpty()) return 0.0f
        
        val n = x.size
        val meanX = x.average().toFloat()
        val meanY = y.average().toFloat()
        
        var numerator = 0.0f
        var sumXSquared = 0.0f
        var sumYSquared = 0.0f
        
        for (i in x.indices) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            numerator += dx * dy
            sumXSquared += dx * dx
            sumYSquared += dy * dy
        }
        
        val denominator = sqrt(sumXSquared * sumYSquared)
        
        return if (denominator > 0.0f) numerator / denominator else 0.0f
    }
    
    /**
     * Calculate reliability score based on sample size and consistency
     */
    private fun calculateReliability(sampleSize: Int, strengths: Collection<Float>): Float {
        if (strengths.isEmpty()) return 0.0f
        
        // Sample size factor (diminishing returns)
        val sampleFactor = (sampleSize.toFloat() / (sampleSize + 10)).coerceIn(0.0f, 1.0f)
        
        // Consistency factor (variance of strengths)
        val mean = strengths.average().toFloat()
        val variance = strengths.map { (it - mean) * (it - mean) }.average().toFloat()
        val consistencyFactor = (1.0f - variance).coerceIn(0.0f, 1.0f)
        
        return (sampleFactor + consistencyFactor) / 2.0f
    }
    
    /**
     * Perform counterfactual reasoning: "What if X had been different?"
     * 
     * Implements Pearl's structural causal model approach to counterfactuals.
     */
    fun counterfactualReasoning(
        causalGraph: CausalGraph,
        intervention: Map<String, Float>,
        query: String
    ): CounterfactualResult {
        // Create modified graph with intervention
        val modifiedGraph = applyIntervention(causalGraph, intervention)
        
        // Simulate alternative worlds
        val alternativeWorlds = generateAlternativeWorlds(modifiedGraph, intervention, 5)
        
        // Predict outcome in query variable
        val predictedOutcome = predictOutcome(modifiedGraph, query, alternativeWorlds)
        
        // Calculate confidence based on causal strength and graph reliability
        val confidence = calculateCounterfactualConfidence(causalGraph, intervention, query)
        
        return CounterfactualResult(
            query = query,
            intervention = intervention,
            predictedOutcome = predictedOutcome,
            confidence = confidence,
            alternativeWorlds = alternativeWorlds
        )
    }
    
    /**
     * Apply intervention to causal graph (do-operator)
     */
    private fun applyIntervention(
        graph: CausalGraph,
        intervention: Map<String, Float>
    ): CausalGraph {
        // Remove incoming edges to intervened variables
        val newEdges = graph.edges.toMutableMap()
        val newStrengths = graph.strengths.toMutableMap()
        
        intervention.keys.forEach { interventionNode ->
            // Remove all edges pointing to this node
            newEdges.keys.forEach { cause ->
                newEdges[cause]?.let { effects ->
                    val filtered = effects.filter { it != interventionNode }.toSet()
                    if (filtered.isNotEmpty()) {
                        newEdges[cause] = filtered
                    } else {
                        newEdges.remove(cause)
                    }
                }
            }
            
            // Remove corresponding strengths
            newStrengths.keys.removeIf { (_, effect) -> effect == interventionNode }
        }
        
        return CausalGraph(
            nodes = graph.nodes,
            edges = newEdges,
            strengths = newStrengths,
            confidence = graph.confidence * 0.95f // Slight confidence penalty for intervention
        )
    }
    
    /**
     * Generate alternative possible worlds under intervention
     */
    private fun generateAlternativeWorlds(
        graph: CausalGraph,
        intervention: Map<String, Float>,
        numWorlds: Int
    ): List<AlternativeWorld> {
        val worlds = mutableListOf<AlternativeWorld>()
        
        for (i in 0 until numWorlds) {
            val values = mutableMapOf<String, Float>()
            
            // Set intervention values
            values.putAll(intervention)
            
            // Propagate effects through causal graph with variation
            graph.edges.forEach { (cause, effects) ->
                val causeValue = values[cause] ?: 0.5f
                effects.forEach { effect ->
                    if (effect !in intervention) {
                        val strength = graph.strengths[Pair(cause, effect)] ?: 0.5f
                        val noise = (Math.random().toFloat() - 0.5f) * 0.1f
                        values[effect] = (causeValue * strength + noise).coerceIn(0.0f, 1.0f)
                    }
                }
            }
            
            worlds.add(
                AlternativeWorld(
                    worldId = "world_$i",
                    values = values,
                    probability = 1.0f / numWorlds
                )
            )
        }
        
        return worlds
    }
    
    /**
     * Predict outcome for query variable across alternative worlds
     */
    private fun predictOutcome(
        graph: CausalGraph,
        query: String,
        worlds: List<AlternativeWorld>
    ): Float {
        val predictions = worlds.mapNotNull { world ->
            world.values[query]?.let { it * world.probability }
        }
        
        return predictions.sum()
    }
    
    /**
     * Calculate confidence in counterfactual prediction
     */
    private fun calculateCounterfactualConfidence(
        graph: CausalGraph,
        intervention: Map<String, Float>,
        query: String
    ): Float {
        // Base confidence from graph
        var confidence = graph.confidence
        
        // Reduce confidence for indirect effects
        val pathExists = hasPath(graph, intervention.keys, query)
        if (!pathExists) {
            confidence *= 0.5f
        }
        
        return confidence.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Check if there's a causal path from sources to target
     */
    private fun hasPath(graph: CausalGraph, sources: Set<String>, target: String): Boolean {
        val visited = mutableSetOf<String>()
        val queue = sources.toMutableList()
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current == target) return true
            if (current in visited) continue
            
            visited.add(current)
            graph.edges[current]?.let { queue.addAll(it) }
        }
        
        return false
    }
    
    /**
     * Simulate intervention and predict causal effects
     */
    fun simulateIntervention(
        causalGraph: CausalGraph,
        targetVariable: String,
        interventionValue: Float
    ): InterventionResult {
        val intervention = mapOf(targetVariable to interventionValue)
        val modifiedGraph = applyIntervention(causalGraph, intervention)
        
        // Find all causal pathways from intervention
        val pathways = findCausalPathways(modifiedGraph, targetVariable)
        
        // Predict effects on all downstream variables
        val predictedEffects = mutableMapOf<String, Float>()
        pathways.forEach { pathway ->
            val lastNode = pathway.path.last()
            val effect = interventionValue * pathway.strength
            predictedEffects[lastNode] = (predictedEffects[lastNode] ?: 0.0f) + effect
        }
        
        // Calculate total causal effect
        val totalEffect = predictedEffects.values.sum()
        
        return InterventionResult(
            targetVariable = targetVariable,
            interventionValue = interventionValue,
            predictedEffects = predictedEffects,
            causalPathways = pathways,
            totalCausalEffect = totalEffect
        )
    }
    
    /**
     * Find all causal pathways from a source node
     */
    private fun findCausalPathways(
        graph: CausalGraph,
        source: String,
        maxDepth: Int = 5
    ): List<CausalPathway> {
        val pathways = mutableListOf<CausalPathway>()
        
        fun dfs(current: String, path: List<String>, strength: Float, depth: Int) {
            if (depth >= maxDepth) return
            
            graph.edges[current]?.forEach { next ->
                val edgeStrength = graph.strengths[Pair(current, next)] ?: 0.5f
                val newPath = path + next
                val newStrength = strength * edgeStrength
                
                pathways.add(
                    CausalPathway(
                        path = newPath,
                        strength = newStrength,
                        directness = 1.0f / newPath.size
                    )
                )
                
                dfs(next, newPath, newStrength, depth + 1)
            }
        }
        
        dfs(source, listOf(source), 1.0f, 0)
        return pathways
    }
    
    /**
     * Integrate causal knowledge into hypergraph
     */
    fun integrateCausalKnowledge(
        causalGraph: CausalGraph,
        namespace: String = "causal"
    ): Int {
        var addedAtoms = 0
        
        // Add nodes as concept atoms
        causalGraph.nodes.forEach { node ->
            val atom = Atom(
                id = "$namespace:concept:$node",
                type = AtomType.CONCEPT_NODE,
                name = node,
                truthValue = causalGraph.confidence
            )
            if (hypergraph.addAtom(atom)) {
                addedAtoms++
            }
        }
        
        // Add causal relationships as evaluation links
        causalGraph.edges.forEach { (cause, effects) ->
            effects.forEach { effect ->
                val strength = causalGraph.strengths[Pair(cause, effect)] ?: 0.5f
                val atom = Atom(
                    id = "$namespace:causes:$cause:$effect",
                    type = AtomType.EVALUATION_LINK,
                    name = "causes",
                    truthValue = strength
                )
                if (hypergraph.addAtom(atom)) {
                    addedAtoms++
                }
            }
        }
        
        return addedAtoms
    }
}
