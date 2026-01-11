package org.ninelym.jni

/**
 * LLaMA.cpp JNI Interface
 *
 * Native bindings for LLaMA.cpp inference engine.
 * Provides on-device large language model inference capabilities.
 *
 * Native library: llama-jni
 */
object LlamaCppJni {

    private var isLoaded = false

    /**
     * Callback interface for streaming inference results
     */
    interface LlamaCppCallback {
        /**
         * Called for each generated token
         */
        fun onToken(token: String)

        /**
         * Called when generation is complete
         */
        fun onComplete(fullResponse: String)

        /**
         * Called on error
         */
        fun onError(error: String)

        /**
         * Called with progress updates
         */
        fun onProgress(tokensGenerated: Int, tokensPerSecond: Float)
    }

    /**
     * Load the native library
     */
    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("llama-jni")
                isLoaded = true
            }
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Check if library is loaded
     */
    fun isLibraryLoaded(): Boolean = isLoaded

    // Native method declarations

    /**
     * Initialize LLaMA with model
     * @param modelPath Path to the GGUF/GGML model file
     * @param useMMQ Use memory-mapped quantization
     */
    @JvmStatic
    external fun llama_init(modelPath: String, useMMQ: Boolean)

    /**
     * Perform single-shot inference
     * @param prompt User prompt
     * @param systemPrompt System/instruction prompt
     * @param chatTemplate Chat template format
     * @param bos Beginning of sequence token
     * @param eos End of sequence token
     * @param inputSuffix Suffix to append to input
     * @param temperature Sampling temperature (0.0-2.0)
     * @param topP Top-p (nucleus) sampling
     * @param topK Top-k sampling
     * @param minP Minimum probability threshold
     * @param repeatPenalty Repetition penalty
     * @param maxTokens Maximum tokens to generate
     * @param callback Callback for streaming results
     * @return Generated text
     */
    @JvmStatic
    external fun llama_infer(
        prompt: String,
        systemPrompt: String,
        chatTemplate: String,
        bos: String,
        eos: String,
        inputSuffix: String,
        temperature: Float,
        topP: Float,
        topK: Float,
        minP: Float,
        repeatPenalty: Int,
        maxTokens: Int,
        callback: LlamaCppCallback
    ): String

    /**
     * Start REPL (Read-Eval-Print Loop) session
     * For multi-turn conversations with context retention
     */
    @JvmStatic
    external fun llama_repl_start(
        modelPath: String,
        loraPath: String,
        systemPrompt: String,
        chatTemplate: String,
        bos: String,
        eos: String,
        inputPrefix: String,
        inputSuffix: String,
        antiprompt: String,
        prePrompt: String,
        grammarPath: String,
        temperature: Float,
        topP: Float,
        topK: Float,
        repeatPenalty: Int,
        minP: Float,
        presencePenalty: Int,
        frequencyPenalty: Int,
        mirostat: Int,
        useMlock: Boolean,
        useMmap: Boolean,
        cacheTypeK: String,
        cacheTypeV: Float,
        ropeFreqBase: Float,
        ropeFreqScale: Float,
        yarnExtFactor: Float,
        yarnAttnFactor: Int,
        yarnBetaFast: Int,
        yarnBetaSlow: Float,
        yarnOrigCtx: Float,
        contextSize: Int,
        batchSize: Float,
        ubatchSize: Float,
        nGpuLayers: Int,
        nThreads: Int,
        seed: Int,
        grpAttnN: Int,
        grpAttnW: Int,
        callback: LlamaCppCallback
    )

    /**
     * Send message to active REPL session
     * @param input Message bytes
     * @param length Input length
     * @param antiprompt Stop sequence
     * @param grammarPath Grammar file path
     * @param samplerSeq Sampler sequence
     */
    @JvmStatic
    external fun llama_repl_send_msg(
        input: ByteArray,
        length: Int,
        antiprompt: String,
        grammarPath: String,
        samplerSeq: String
    )

    /**
     * Stop active REPL session
     */
    @JvmStatic
    external fun llama_repl_stop()

    /**
     * Describe an image using vision model
     * @param imagePath Path to image file
     * @param prompt Prompt for image description
     * @param modelPath Path to vision model
     * @return Image description
     */
    @JvmStatic
    external fun llama_describe_img(
        imagePath: String,
        prompt: String,
        modelPath: String
    ): String

    /**
     * Check CPU dot product support
     * @return 1 if supported, 0 otherwise
     */
    @JvmStatic
    external fun isCPUDotProdSupported(): Int

    /**
     * Check CPU i8mm (Int8 Matrix Multiply) support
     * @return 1 if supported, 0 otherwise
     */
    @JvmStatic
    external fun isCPUi8mmSupported(): Int

    /**
     * Get available VRAM in bytes
     */
    @JvmStatic
    external fun getAvailableVRAM(): Long

    /**
     * Free model from memory
     */
    @JvmStatic
    external fun llama_free()
}

/**
 * High-level wrapper for LlamaCppJni
 */
class LlamaCppEngine {

    data class InferenceConfig(
        val temperature: Float = 0.7f,
        val topP: Float = 0.9f,
        val topK: Float = 40f,
        val minP: Float = 0.05f,
        val repeatPenalty: Int = 1,
        val maxTokens: Int = 512,
        val systemPrompt: String = "",
        val chatTemplate: String = "chatml"
    )

    data class ModelConfig(
        val modelPath: String,
        val useMMQ: Boolean = true,
        val contextSize: Int = 4096,
        val nGpuLayers: Int = 0,
        val nThreads: Int = 4
    )

    private var isInitialized = false
    private var currentConfig: ModelConfig? = null

    /**
     * Initialize with model
     */
    fun initialize(config: ModelConfig): Result<Unit> {
        return try {
            if (!LlamaCppJni.loadLibrary()) {
                return Result.failure(Exception("Failed to load llama-jni library"))
            }

            LlamaCppJni.llama_init(config.modelPath, config.useMMQ)
            currentConfig = config
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Perform inference
     */
    fun infer(
        prompt: String,
        config: InferenceConfig = InferenceConfig(),
        onToken: ((String) -> Unit)? = null
    ): Result<String> {
        if (!isInitialized) {
            return Result.failure(Exception("Engine not initialized"))
        }

        return try {
            val callback = object : LlamaCppJni.LlamaCppCallback {
                override fun onToken(token: String) {
                    onToken?.invoke(token)
                }
                override fun onComplete(fullResponse: String) {}
                override fun onError(error: String) {}
                override fun onProgress(tokensGenerated: Int, tokensPerSecond: Float) {}
            }

            val result = LlamaCppJni.llama_infer(
                prompt = prompt,
                systemPrompt = config.systemPrompt,
                chatTemplate = config.chatTemplate,
                bos = "<|im_start|>",
                eos = "<|im_end|>",
                inputSuffix = "",
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                minP = config.minP,
                repeatPenalty = config.repeatPenalty,
                maxTokens = config.maxTokens,
                callback = callback
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Describe an image
     */
    fun describeImage(imagePath: String, prompt: String = "Describe this image"): Result<String> {
        if (!isInitialized) {
            return Result.failure(Exception("Engine not initialized"))
        }

        return try {
            val modelPath = currentConfig?.modelPath ?: return Result.failure(Exception("No model loaded"))
            val result = LlamaCppJni.llama_describe_img(imagePath, prompt, modelPath)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check hardware capabilities
     */
    fun getHardwareCapabilities(): HardwareCapabilities {
        return HardwareCapabilities(
            dotProdSupported = LlamaCppJni.isCPUDotProdSupported() == 1,
            i8mmSupported = LlamaCppJni.isCPUi8mmSupported() == 1,
            availableVRAM = try { LlamaCppJni.getAvailableVRAM() } catch (e: Exception) { 0L }
        )
    }

    data class HardwareCapabilities(
        val dotProdSupported: Boolean,
        val i8mmSupported: Boolean,
        val availableVRAM: Long
    )

    /**
     * Shutdown and free resources
     */
    fun shutdown() {
        if (isInitialized) {
            try {
                LlamaCppJni.llama_free()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            isInitialized = false
            currentConfig = null
        }
    }
}
