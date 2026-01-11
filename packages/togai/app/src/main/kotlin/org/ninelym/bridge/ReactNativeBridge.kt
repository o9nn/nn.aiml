package org.ninelym.bridge

import kotlinx.coroutines.*
import org.ninelym.jni.LlamaCppEngine
import org.ninelym.jni.StableDiffusionEngine
import org.ninelym.jni.CTranslate2Engine
import org.ninelym.db.VectorDatabase
import org.ninelym.db.VectorDatabaseFactory
import org.ninelym.mlc.MLCChatEngine
import org.ninelym.mlc.MLCModelConfig
import org.ninelym.mlc.MLCGenerationParams
import org.ninelym.animation.CubismEngine
import org.ninelym.animation.CubismModelAssets
import org.ninelym.executorch.ExecutorchRuntime
import org.ninelym.executorch.LlamaModelConfig
import org.ninelym.tokenizer.TokenizerEngine
import org.ninelym.tokenizer.TokenizerConfig
import org.ninelym.tokenizer.TokenizerType
import java.util.concurrent.ConcurrentHashMap

/**
 * React Native Bridge
 *
 * Bridge interfaces and implementations for React Native integration.
 * These modules can be exposed to JavaScript through React Native's
 * native module system.
 */

// ============================================================================
// Base Module Interface
// ============================================================================

/**
 * Base React Native Module interface
 */
interface ReactNativeModule {
    val name: String

    fun initialize()
    fun destroy()

    // Event emitter support
    fun addListener(eventName: String)
    fun removeListeners(count: Int)
}

/**
 * Promise interface for async operations
 */
interface Promise {
    fun resolve(value: Any?)
    fun reject(code: String, message: String, throwable: Throwable? = null)
}

/**
 * Simple Promise implementation
 */
class SimplePromise(
    private val onResolve: (Any?) -> Unit,
    private val onReject: (String, String, Throwable?) -> Unit
) : Promise {
    override fun resolve(value: Any?) = onResolve(value)
    override fun reject(code: String, message: String, throwable: Throwable?) = onReject(code, message, throwable)
}

// ============================================================================
// LlamaCpp Module
// ============================================================================

/**
 * LlamaCpp React Native Module
 */
class LlamaCppModule : ReactNativeModule {

    override val name = "LlamaCpp"

    private val engine = LlamaCppEngine()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var listenerCount = 0

    override fun initialize() {
        // Module initialization
    }

    override fun destroy() {
        scope.cancel()
        engine.shutdown()
    }

    override fun addListener(eventName: String) {
        listenerCount++
    }

    override fun removeListeners(count: Int) {
        listenerCount = maxOf(0, listenerCount - count)
    }

    /**
     * Load model
     */
    fun loadModel(modelPath: String, options: Map<String, Any>, promise: Promise) {
        scope.launch {
            try {
                val config = LlamaCppEngine.ModelConfig(
                    modelPath = modelPath,
                    useMMQ = options["useMMQ"] as? Boolean ?: true,
                    contextSize = (options["contextSize"] as? Number)?.toInt() ?: 4096,
                    nGpuLayers = (options["nGpuLayers"] as? Number)?.toInt() ?: 0,
                    nThreads = (options["nThreads"] as? Number)?.toInt() ?: 4
                )

                val result = engine.initialize(config)
                if (result.isSuccess) {
                    promise.resolve(mapOf("success" to true))
                } else {
                    promise.reject("LOAD_ERROR", result.exceptionOrNull()?.message ?: "Load failed")
                }
            } catch (e: Exception) {
                promise.reject("LOAD_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * Generate text
     */
    fun generate(prompt: String, options: Map<String, Any>, promise: Promise) {
        scope.launch {
            try {
                val config = LlamaCppEngine.InferenceConfig(
                    temperature = (options["temperature"] as? Number)?.toFloat() ?: 0.7f,
                    topP = (options["topP"] as? Number)?.toFloat() ?: 0.9f,
                    topK = (options["topK"] as? Number)?.toFloat() ?: 40f,
                    maxTokens = (options["maxTokens"] as? Number)?.toInt() ?: 512,
                    systemPrompt = options["systemPrompt"] as? String ?: ""
                )

                val result = engine.infer(prompt, config) { token ->
                    // Emit token event
                    emitEvent("onToken", mapOf("token" to token))
                }

                if (result.isSuccess) {
                    promise.resolve(mapOf("text" to result.getOrNull()))
                } else {
                    promise.reject("GENERATE_ERROR", result.exceptionOrNull()?.message ?: "Generation failed")
                }
            } catch (e: Exception) {
                promise.reject("GENERATE_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * Unload model
     */
    fun unload(promise: Promise) {
        engine.shutdown()
        promise.resolve(null)
    }

    private fun emitEvent(eventName: String, params: Map<String, Any>) {
        // In actual RN implementation, would call sendEvent
    }
}

// ============================================================================
// Stable Diffusion Module
// ============================================================================

/**
 * Stable Diffusion React Native Module
 */
class StableDiffusionModule : ReactNativeModule {

    override val name = "StableDiffusion"

    private val engine = StableDiffusionEngine()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var listenerCount = 0

    override fun initialize() {}

    override fun destroy() {
        scope.cancel()
        engine.shutdown()
    }

    override fun addListener(eventName: String) { listenerCount++ }
    override fun removeListeners(count: Int) { listenerCount = maxOf(0, listenerCount - count) }

    /**
     * Load model
     */
    fun loadModel(modelPath: String, options: Map<String, Any>, promise: Promise) {
        scope.launch {
            try {
                val config = StableDiffusionEngine.ModelConfig(
                    modelPath = modelPath,
                    vaePath = options["vaePath"] as? String ?: "",
                    loraPath = options["loraPath"] as? String ?: "",
                    useGpu = options["useGpu"] as? Boolean ?: true
                )

                val result = engine.initialize(config)
                if (result.isSuccess) {
                    promise.resolve(mapOf("success" to true))
                } else {
                    promise.reject("LOAD_ERROR", result.exceptionOrNull()?.message ?: "Load failed")
                }
            } catch (e: Exception) {
                promise.reject("LOAD_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * Generate image
     */
    fun generateImage(prompt: String, outputPath: String, options: Map<String, Any>, promise: Promise) {
        scope.launch {
            try {
                val config = StableDiffusionEngine.GenerationConfig(
                    prompt = prompt,
                    negativePrompt = options["negativePrompt"] as? String ?: "",
                    width = (options["width"] as? Number)?.toInt() ?: 512,
                    height = (options["height"] as? Number)?.toInt() ?: 512,
                    steps = (options["steps"] as? Number)?.toInt() ?: 20,
                    guidanceScale = (options["guidanceScale"] as? Number)?.toFloat() ?: 7.5f,
                    seed = (options["seed"] as? Number)?.toLong() ?: -1
                )

                val result = engine.generateImage(config, outputPath) { step, total ->
                    emitEvent("onProgress", mapOf("step" to step, "total" to total))
                }

                when (result) {
                    is StableDiffusionEngine.GenerationResult.Success -> {
                        promise.resolve(mapOf("imagePath" to result.imagePath))
                    }
                    is StableDiffusionEngine.GenerationResult.Error -> {
                        promise.reject("GENERATE_ERROR", result.message)
                    }
                }
            } catch (e: Exception) {
                promise.reject("GENERATE_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    private fun emitEvent(eventName: String, params: Map<String, Any>) {}
}

// ============================================================================
// MLC Chat Module
// ============================================================================

/**
 * MLC Chat React Native Module
 */
class MLCChatModule : ReactNativeModule {

    override val name = "MLCChat"

    private val engine = MLCChatEngine()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var listenerCount = 0

    override fun initialize() {}

    override fun destroy() {
        scope.cancel()
        engine.shutdown()
    }

    override fun addListener(eventName: String) { listenerCount++ }
    override fun removeListeners(count: Int) { listenerCount = maxOf(0, listenerCount - count) }

    /**
     * Start chat with model
     */
    fun start(modelDir: String, modelLib: String, promise: Promise) {
        scope.launch {
            try {
                val config = MLCModelConfig(
                    modelPath = modelDir,
                    modelLib = modelLib
                )

                val result = engine.loadModel(config)
                if (result.isSuccess) {
                    promise.resolve(null)
                } else {
                    promise.reject("START_ERROR", result.exceptionOrNull()?.message ?: "Start failed")
                }
            } catch (e: Exception) {
                promise.reject("START_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * Send message
     */
    fun sendMessage(message: String, promise: Promise) {
        scope.launch {
            try {
                val result = engine.generate(message, onToken = { token ->
                    emitEvent("onToken", mapOf("token" to token))
                })

                if (result.isSuccess) {
                    promise.resolve(mapOf("response" to result.getOrNull()))
                } else {
                    promise.reject("SEND_ERROR", result.exceptionOrNull()?.message ?: "Send failed")
                }
            } catch (e: Exception) {
                promise.reject("SEND_ERROR", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * Stop chat
     */
    fun stop(promise: Promise) {
        scope.launch {
            engine.unloadModel()
            promise.resolve(null)
        }
    }

    private fun emitEvent(eventName: String, params: Map<String, Any>) {}
}

// ============================================================================
// LVDB Module
// ============================================================================

/**
 * LVDB (Vector Database) React Native Module
 */
class LvdbModule : ReactNativeModule {

    override val name = "Lvdb"

    private var database: VectorDatabase? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var listenerCount = 0

    override fun initialize() {}

    override fun destroy() {
        scope.cancel()
    }

    override fun addListener(eventName: String) { listenerCount++ }
    override fun removeListeners(count: Int) { listenerCount = maxOf(0, listenerCount - count) }

    /**
     * Initialize database
     */
    fun init(dbPath: String, promise: Promise) {
        database = VectorDatabaseFactory.create(dbPath)
        promise.resolve(null)
    }

    /**
     * Save embeddings
     */
    fun saveEmbeddings(
        namespace: String,
        id: String,
        vector: List<Double>,
        metadata: String,
        promise: Promise
    ) {
        scope.launch {
            try {
                val db = database ?: throw Exception("Database not initialized")
                val floatVector = vector.map { it.toFloat() }.toFloatArray()
                val metadataMap = parseMetadata(metadata)

                db.saveEmbedding(namespace, id, floatVector, metadataMap)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("SAVE_ERROR", e.message ?: "Save failed", e)
            }
        }
    }

    /**
     * Query similar embeddings
     */
    fun queryTopNSimilar(
        namespace: String,
        queryVector: List<Double>,
        n: Int,
        promise: Promise
    ) {
        scope.launch {
            try {
                val db = database ?: throw Exception("Database not initialized")
                val floatVector = queryVector.map { it.toFloat() }.toFloatArray()

                val results = db.queryTopNSimilar(namespace, floatVector, n)

                val resultList = results.map { result ->
                    mapOf(
                        "id" to result.embedding.id,
                        "similarity" to result.similarity,
                        "metadata" to result.embedding.metadata
                    )
                }

                promise.resolve(resultList)
            } catch (e: Exception) {
                promise.reject("QUERY_ERROR", e.message ?: "Query failed", e)
            }
        }
    }

    /**
     * Delete by metadata
     */
    fun deleteEmbeddingsByMetadata(namespace: String, key: String, value: String, promise: Promise) {
        scope.launch {
            try {
                val db = database ?: throw Exception("Database not initialized")
                db.deleteByMetadata(namespace, key, value)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("DELETE_ERROR", e.message ?: "Delete failed", e)
            }
        }
    }

    /**
     * Save to disk
     */
    fun saveToDisk(namespace: String, promise: Promise) {
        scope.launch {
            try {
                val db = database ?: throw Exception("Database not initialized")
                db.saveToDisk(namespace)
                promise.resolve(null)
            } catch (e: Exception) {
                promise.reject("SAVE_ERROR", e.message ?: "Save failed", e)
            }
        }
    }

    private fun parseMetadata(json: String): Map<String, String> {
        // Simplified - would use proper JSON parsing
        return emptyMap()
    }
}

// ============================================================================
// Cubism Module
// ============================================================================

/**
 * Cubism React Native Module
 */
class CubismModule : ReactNativeModule {

    override val name = "RNCubism"

    private val engine = CubismEngine()
    private var listenerCount = 0

    override fun initialize() {
        engine.initialize()
    }

    override fun destroy() {
        engine.shutdown()
    }

    override fun addListener(eventName: String) { listenerCount++ }
    override fun removeListeners(count: Int) { listenerCount = maxOf(0, listenerCount - count) }

    /**
     * Load model
     */
    fun loadModel(modelId: String, modelPath: String, promise: Promise) {
        val assets = CubismModelAssets(modelPath = modelPath)
        val result = engine.loadModel(modelId, assets)

        if (result.isSuccess) {
            promise.resolve(mapOf("modelId" to modelId))
        } else {
            promise.reject("LOAD_ERROR", result.exceptionOrNull()?.message ?: "Load failed")
        }
    }

    /**
     * Start motion
     */
    fun startMotion(modelId: String, group: String, index: Int, promise: Promise) {
        val model = engine.getModel(modelId)
        if (model != null) {
            val success = model.startMotion(group, index)
            promise.resolve(mapOf("success" to success))
        } else {
            promise.reject("MODEL_ERROR", "Model not found")
        }
    }

    /**
     * Set expression
     */
    fun setExpression(modelId: String, expression: String, promise: Promise) {
        val model = engine.getModel(modelId)
        if (model != null) {
            val success = model.setExpression(expression)
            promise.resolve(mapOf("success" to success))
        } else {
            promise.reject("MODEL_ERROR", "Model not found")
        }
    }

    /**
     * Set parameter
     */
    fun setParameter(modelId: String, paramId: String, value: Double, promise: Promise) {
        val model = engine.getModel(modelId)
        if (model != null) {
            model.setParameter(paramId, value.toFloat())
            promise.resolve(null)
        } else {
            promise.reject("MODEL_ERROR", "Model not found")
        }
    }
}

// ============================================================================
// Module Registry
// ============================================================================

/**
 * React Native Package registry
 */
object ReactNativePackageRegistry {

    private val modules = ConcurrentHashMap<String, ReactNativeModule>()

    /**
     * Register all Togai modules
     */
    fun registerAllModules() {
        registerModule(LlamaCppModule())
        registerModule(StableDiffusionModule())
        registerModule(MLCChatModule())
        registerModule(LvdbModule())
        registerModule(CubismModule())
    }

    /**
     * Register a module
     */
    fun registerModule(module: ReactNativeModule) {
        modules[module.name] = module
        module.initialize()
    }

    /**
     * Get module by name
     */
    fun getModule(name: String): ReactNativeModule? = modules[name]

    /**
     * Get all modules
     */
    fun getAllModules(): List<ReactNativeModule> = modules.values.toList()

    /**
     * Shutdown all modules
     */
    fun shutdown() {
        modules.values.forEach { it.destroy() }
        modules.clear()
    }
}
