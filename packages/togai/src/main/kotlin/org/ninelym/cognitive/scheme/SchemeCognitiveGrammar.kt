package org.ninelym.cognitive.scheme

import org.ninelym.cognitive.hypergraph.Atom
import org.ninelym.cognitive.hypergraph.AtomType
import org.ninelym.cognitive.hypergraph.TruthValue

/**
 * Scheme-based cognitive grammar for symbolic reasoning
 * 
 * Provides a simplified Scheme-like syntax for expressing cognitive operations
 * and translating between symbolic expressions and hypergraph patterns
 */
class SchemeCognitiveGrammar {
    
    /**
     * Parse a simple S-expression into cognitive atoms
     */
    fun parseExpression(expression: String): List<Atom> {
        val tokens = tokenize(expression)
        return parseTokens(tokens)
    }
    
    /**
     * Convert atoms back to Scheme expression
     */
    fun atomsToExpression(atoms: List<Atom>): String {
        if (atoms.isEmpty()) return "()"
        
        return atoms.joinToString(" ", "(", ")") { atom ->
            when (atom.type) {
                AtomType.CONCEPT -> atom.name
                AtomType.PREDICATE -> "(${atom.name})"
                AtomType.EVALUATION -> "(eval ${atom.name})"
                AtomType.INHERITANCE -> "(inherit ${atom.name})"
                AtomType.SIMILARITY -> "(similar ${atom.name})"
                AtomType.IMPLICATION -> "(implies ${atom.name})"
                else -> atom.name
            }
        }
    }
    
    /**
     * Basic cognitive operations in Scheme syntax
     */
    fun evaluateOperation(operation: String, args: List<String>): Atom? {
        return when (operation.lowercase()) {
            "concept" -> {
                if (args.isNotEmpty()) {
                    Atom(
                        id = generateId(),
                        type = AtomType.CONCEPT,
                        name = args[0],
                        truthValue = TruthValue.DEFAULT
                    )
                } else null
            }
            "inherit" -> {
                if (args.size >= 2) {
                    Atom(
                        id = generateId(),
                        type = AtomType.INHERITANCE,
                        name = "${args[0]}->${args[1]}",
                        truthValue = TruthValue.TRUE
                    )
                } else null
            }
            "similar" -> {
                if (args.size >= 2) {
                    Atom(
                        id = generateId(),
                        type = AtomType.SIMILARITY,
                        name = "${args[0]}<->${args[1]}",
                        truthValue = TruthValue.TRUE
                    )
                } else null
            }
            "implies" -> {
                if (args.size >= 2) {
                    Atom(
                        id = generateId(),
                        type = AtomType.IMPLICATION,
                        name = "${args[0]}=>${args[1]}",
                        truthValue = TruthValue.TRUE
                    )
                } else null
            }
            else -> null
        }
    }
    
    private fun tokenize(expression: String): List<String> {
        return expression
            .replace("(", " ( ")
            .replace(")", " ) ")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }
    
    private fun parseTokens(tokens: List<String>): List<Atom> {
        val atoms = mutableListOf<Atom>()
        var i = 0
        
        while (i < tokens.size) {
            val token = tokens[i]
            
            if (token == "(") {
                // Find matching closing parenthesis
                var depth = 1
                var j = i + 1
                while (j < tokens.size && depth > 0) {
                    if (tokens[j] == "(") depth++
                    else if (tokens[j] == ")") depth--
                    j++
                }
                
                if (depth == 0) {
                    // Extract expression between parentheses
                    val subTokens = tokens.subList(i + 1, j - 1)
                    if (subTokens.isNotEmpty()) {
                        val operation = subTokens[0]
                        val args = subTokens.drop(1)
                        evaluateOperation(operation, args)?.let { atoms.add(it) }
                    }
                    i = j
                } else {
                    i++
                }
            } else if (token != ")") {
                // Simple atom
                atoms.add(Atom(
                    id = generateId(),
                    type = AtomType.CONCEPT,
                    name = token,
                    truthValue = TruthValue.DEFAULT
                ))
                i++
            } else {
                i++
            }
        }
        
        return atoms
    }
    
    private fun generateId(): String = "atom-${System.currentTimeMillis()}-${(Math.random() * 1000).toInt()}"
}

/**
 * Microservice interface for cognitive grammar operations
 */
interface CognitiveGrammarService {
    fun parseToAtoms(expression: String): List<Atom>
    fun atomsToScheme(atoms: List<Atom>): String
    fun evaluateExpression(expression: String): Atom?
    fun validateSyntax(expression: String): Boolean
}

/**
 * ML Primitive representation for bidirectional translation
 * 
 * Represents machine learning model primitives (embeddings, activations, etc.)
 * that can be translated to/from AtomSpace hypergraph patterns
 */
data class MLPrimitive(
    val id: String,
    val type: MLPrimitiveType,
    val embedding: FloatArray,
    val metadata: Map<String, Any> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MLPrimitive
        return id == other.id && type == other.type && embedding.contentEquals(other.embedding)
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

/**
 * Types of ML primitives that can be encoded in hypergraph
 */
enum class MLPrimitiveType {
    EMBEDDING,      // Dense vector embeddings
    ACTIVATION,     // Neural network activations
    ATTENTION,      // Attention weights
    HIDDEN_STATE,   // RNN/LSTM hidden states
    FEATURE_MAP,    // CNN feature maps
    TOKEN,          // Tokenized text/data
    LOGIT           // Model output logits
}

/**
 * Bidirectional translation service between ML primitives and AtomSpace
 */
class MLAtomSpaceTranslator {
    
    /**
     * Translate ML primitive to AtomSpace hypergraph atoms
     */
    fun mlToAtomSpace(primitive: MLPrimitive): List<Atom> {
        val atoms = mutableListOf<Atom>()
        
        // Create concept atom for the primitive
        val conceptAtom = Atom(
            id = primitive.id,
            type = AtomType.CONCEPT,
            name = "${primitive.type.name}_${primitive.id}",
            truthValue = TruthValue.DEFAULT,
            attentionValue = computeAttentionFromEmbedding(primitive.embedding)
        )
        atoms.add(conceptAtom)
        
        // Create evaluation atoms for embedding dimensions
        val dimensionAtoms = primitive.embedding.take(5).mapIndexed { index, value ->
            Atom(
                id = "${primitive.id}_dim_$index",
                type = AtomType.EVALUATION,
                name = "dimension_$index",
                truthValue = TruthValue(value.coerceIn(0f, 1f), 1.0f)
            )
        }
        atoms.addAll(dimensionAtoms)
        
        // Create link atoms for relationships
        if (primitive.metadata.containsKey("parent")) {
            val inheritAtom = Atom(
                id = "${primitive.id}_inherit",
                type = AtomType.INHERITANCE,
                name = "${primitive.id}->parent",
                truthValue = TruthValue.TRUE
            )
            atoms.add(inheritAtom)
        }
        
        return atoms
    }
    
    /**
     * Translate AtomSpace atoms back to ML primitive
     */
    fun atomSpaceToML(atoms: List<Atom>): MLPrimitive? {
        if (atoms.isEmpty()) return null
        
        val primaryAtom = atoms.firstOrNull { it.type == AtomType.CONCEPT } ?: return null
        
        // Extract embedding from evaluation atoms
        val embedding = atoms
            .filter { it.type == AtomType.EVALUATION }
            .sortedBy { it.name }
            .map { it.truthValue.strength }
            .toFloatArray()
        
        // Determine ML primitive type from atom name
        val type = MLPrimitiveType.values().firstOrNull { 
            primaryAtom.name.startsWith(it.name)
        } ?: MLPrimitiveType.EMBEDDING
        
        // Extract metadata from atom properties
        val metadata = mutableMapOf<String, Any>()
        if (atoms.any { it.type == AtomType.INHERITANCE }) {
            metadata["hasInheritance"] = true
        }
        
        return MLPrimitive(
            id = primaryAtom.id,
            type = type,
            embedding = embedding,
            metadata = metadata
        )
    }
    
    /**
     * Compute attention value from embedding statistics
     */
    private fun computeAttentionFromEmbedding(embedding: FloatArray): org.ninelym.cognitive.hypergraph.AttentionValue {
        val magnitude = kotlin.math.sqrt(embedding.map { it * it }.sum())
        val mean = embedding.average().toFloat()
        
        return org.ninelym.cognitive.hypergraph.AttentionValue(
            sti = magnitude.coerceIn(0f, 1f),
            lti = mean.coerceIn(0f, 1f)
        )
    }
    
    /**
     * Batch translation for efficiency
     */
    fun mlBatchToAtomSpace(primitives: List<MLPrimitive>): List<Atom> {
        return primitives.flatMap { mlToAtomSpace(it) }
    }
    
    /**
     * Batch translation from atoms to ML primitives
     */
    fun atomSpaceBatchToML(atomGroups: List<List<Atom>>): List<MLPrimitive> {
        return atomGroups.mapNotNull { atomSpaceToML(it) }
    }
}

/**
 * Enhanced cognitive grammar service implementation
 */
class CognitiveGrammarServiceImpl(
    private val grammar: SchemeCognitiveGrammar = SchemeCognitiveGrammar(),
    private val translator: MLAtomSpaceTranslator = MLAtomSpaceTranslator()
) : CognitiveGrammarService {
    
    override fun parseToAtoms(expression: String): List<Atom> {
        return grammar.parseExpression(expression)
    }
    
    override fun atomsToScheme(atoms: List<Atom>): String {
        return grammar.atomsToExpression(atoms)
    }
    
    override fun evaluateExpression(expression: String): Atom? {
        val atoms = parseToAtoms(expression)
        return atoms.firstOrNull()
    }
    
    override fun validateSyntax(expression: String): Boolean {
        return try {
            val atoms = parseToAtoms(expression)
            atoms.all { it.truthValue.isValid() && it.attentionValue.isValid() }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Translate ML primitive to Scheme expression through atoms
     */
    fun mlToScheme(primitive: MLPrimitive): String {
        val atoms = translator.mlToAtomSpace(primitive)
        return atomsToScheme(atoms)
    }
    
    /**
     * Parse Scheme expression and convert to ML primitive
     */
    fun schemeToML(expression: String): MLPrimitive? {
        val atoms = parseToAtoms(expression)
        return translator.atomSpaceToML(atoms)
    }
}