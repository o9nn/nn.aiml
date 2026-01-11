package org.ninelym.cognitive.quantum

import kotlin.math.*
import kotlin.random.Random

/**
 * Quantum-Inspired Optimization for Cognitive Systems
 * 
 * Implements quantum computing concepts (superposition, entanglement, interference)
 * for classical optimization problems in cognitive attention allocation.
 * 
 * Based on Quantum-Inspired Evolutionary Algorithm (QIEA) principles.
 */
class QuantumInspiredOptimizer(
    private val populationSize: Int = 50,
    private val maxGenerations: Int = 100,
    private val rotationAngle: Float = 0.05f * PI.toFloat()
) {
    
    /**
     * Quantum state representation using probability amplitudes
     */
    data class QuantumState(
        val position: FloatArray,
        val amplitude: FloatArray,
        val phase: FloatArray
    ) {
        val dimensions: Int get() = position.size
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QuantumState) return false
            return position.contentEquals(other.position)
        }
        
        override fun hashCode(): Int = position.contentHashCode()
    }
    
    /**
     * Optimization result with solution and fitness
     */
    data class OptimizationResult(
        val solution: FloatArray,
        val fitness: Float,
        val generations: Int,
        val convergenceHistory: List<Float>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is OptimizationResult) return false
            return solution.contentEquals(other.solution) && fitness == other.fitness
        }
        
        override fun hashCode(): Int = solution.contentHashCode() + fitness.hashCode()
    }
    
    /**
     * Optimize a function using quantum-inspired algorithm
     */
    fun optimize(
        objectiveFunction: (FloatArray) -> Float,
        dimensions: Int,
        bounds: Pair<Float, Float> = Pair(0.0f, 1.0f)
    ): OptimizationResult {
        // Initialize quantum population
        var population = initializeQuantumPopulation(dimensions)
        
        var bestSolution = FloatArray(dimensions) { Random.nextFloat() }
        var bestFitness = objectiveFunction(bestSolution)
        
        val convergenceHistory = mutableListOf<Float>()
        var generation = 0
        
        while (generation < maxGenerations) {
            // Measure quantum states to get classical positions
            val measurements = population.map { measureState(it, bounds) }
            
            // Evaluate fitness for each measured position
            measurements.forEachIndexed { index, position ->
                val fitness = objectiveFunction(position)
                
                if (fitness > bestFitness) {
                    bestFitness = fitness
                    bestSolution = position.copyOf()
                }
            }
            
            convergenceHistory.add(bestFitness)
            
            // Apply quantum rotation gate to evolve population
            population = population.mapIndexed { index, state ->
                applyQuantumRotation(state, bestSolution, measurements[index])
            }
            
            // Apply quantum interference for diversity
            if (generation % 10 == 0) {
                population = applyQuantumInterference(population)
            }
            
            generation++
            
            // Early stopping if converged
            if (convergenceHistory.size >= 10) {
                val recentImprovement = convergenceHistory.takeLast(10)
                val variance = recentImprovement.variance()
                if (variance < 0.001f) break
            }
        }
        
        return OptimizationResult(
            solution = bestSolution,
            fitness = bestFitness,
            generations = generation,
            convergenceHistory = convergenceHistory
        )
    }
    
    /**
     * Initialize quantum population with random states
     */
    private fun initializeQuantumPopulation(dimensions: Int): List<QuantumState> {
        return List(populationSize) {
            QuantumState(
                position = FloatArray(dimensions) { Random.nextFloat() },
                amplitude = FloatArray(dimensions) { 1.0f / sqrt(2.0f) },
                phase = FloatArray(dimensions) { 0.0f }
            )
        }
    }
    
    /**
     * Measure quantum state to get classical position
     * Simulates quantum measurement collapse
     */
    private fun measureState(
        state: QuantumState,
        bounds: Pair<Float, Float>
    ): FloatArray {
        val (min, max) = bounds
        val range = max - min
        
        return FloatArray(state.dimensions) { i ->
            // Probability-based measurement
            val probability = state.amplitude[i] * state.amplitude[i]
            val measured = if (Random.nextFloat() < probability) {
                state.position[i]
            } else {
                Random.nextFloat()
            }
            
            // Scale to bounds
            min + measured * range
        }
    }
    
    /**
     * Apply quantum rotation gate to evolve state
     * Rotates probability amplitudes toward better solution
     */
    private fun applyQuantumRotation(
        state: QuantumState,
        bestSolution: FloatArray,
        currentPosition: FloatArray
    ): QuantumState {
        val newAmplitude = FloatArray(state.dimensions)
        val newPhase = FloatArray(state.dimensions)
        
        for (i in 0 until state.dimensions) {
            val delta = bestSolution[i] - currentPosition[i]
            val rotationDirection = sign(delta)
            
            // Quantum rotation gate
            val theta = rotationDirection * rotationAngle
            val cosTheta = cos(theta)
            val sinTheta = sin(theta)
            
            // Rotate amplitude
            val alpha = state.amplitude[i]
            val beta = sqrt(1.0f - alpha * alpha)
            
            newAmplitude[i] = (alpha * cosTheta - beta * sinTheta).coerceIn(-1.0f, 1.0f)
            newPhase[i] = (state.phase[i] + theta) % (2 * PI.toFloat())
        }
        
        return QuantumState(
            position = currentPosition.copyOf(),
            amplitude = newAmplitude,
            phase = newPhase
        )
    }
    
    /**
     * Apply quantum interference to maintain diversity
     * Prevents premature convergence
     */
    private fun applyQuantumInterference(
        population: List<QuantumState>
    ): List<QuantumState> {
        return population.map { state ->
            val interferedAmplitude = FloatArray(state.dimensions) { i ->
                // Add small random phase shift for interference
                val phaseShift = Random.nextFloat() * 0.1f - 0.05f
                val newPhase = state.phase[i] + phaseShift
                
                // Recalculate amplitude with interference
                (state.amplitude[i] * cos(newPhase)).coerceIn(-1.0f, 1.0f)
            }
            
            QuantumState(
                position = state.position.copyOf(),
                amplitude = interferedAmplitude,
                phase = state.phase.copyOf()
            )
        }
    }
    
    /**
     * Optimize attention allocation using quantum approach
     */
    fun optimizeAttentionAllocation(
        atoms: List<String>,
        importanceScores: Map<String, Float>,
        resourceBudget: Float
    ): AttentionAllocationResult {
        val dimensions = atoms.size
        
        // Objective: maximize total importance while respecting budget
        val objectiveFunction = { allocation: FloatArray ->
            val totalAllocation = allocation.sum()
            val totalImportance = allocation.mapIndexed { i, value ->
                value * (importanceScores[atoms[i]] ?: 0.0f)
            }.sum()
            
            // Penalty for exceeding budget
            val penalty = if (totalAllocation > resourceBudget) {
                (totalAllocation - resourceBudget) * 10.0f
            } else 0.0f
            
            totalImportance - penalty
        }
        
        val result = optimize(objectiveFunction, dimensions, Pair(0.0f, 1.0f))
        
        // Normalize allocation to budget
        val normalizedAllocation = normalizeAllocation(result.solution, resourceBudget)
        
        return AttentionAllocationResult(
            atomAllocations = atoms.zip(normalizedAllocation).toMap(),
            totalImportance = result.fitness,
            convergenceGenerations = result.generations
        )
    }
    
    /**
     * Normalize allocation to fit within budget
     */
    private fun normalizeAllocation(
        allocation: FloatArray,
        budget: Float
    ): FloatArray {
        val total = allocation.sum()
        return if (total > 0) {
            FloatArray(allocation.size) { i ->
                (allocation[i] / total) * budget
            }
        } else {
            FloatArray(allocation.size) { budget / allocation.size }
        }
    }
}

/**
 * Result of quantum-optimized attention allocation
 */
data class AttentionAllocationResult(
    val atomAllocations: Map<String, Float>,
    val totalImportance: Float,
    val convergenceGenerations: Int
)

/**
 * Extension function to calculate variance
 */
private fun List<Float>.variance(): Float {
    if (isEmpty()) return 0.0f
    val mean = average().toFloat()
    return map { (it - mean).pow(2) }.average().toFloat()
}
