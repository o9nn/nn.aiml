package org.ninelym.jni

/**
 * Stable Diffusion JNI Interface
 *
 * Native bindings for Stable Diffusion image generation.
 * Provides on-device text-to-image and image-to-image capabilities.
 *
 * Native library: sd-jni
 */
object SDJni {

    private var isLoaded = false

    /**
     * Callback interface for image generation progress
     */
    interface SDGenImgCallback {
        /**
         * Called with progress updates during generation
         * @param step Current step
         * @param totalSteps Total steps
         */
        fun onProgress(step: Int, totalSteps: Int)

        /**
         * Called when generation is complete
         * @param imagePath Path to the generated image
         */
        fun onComplete(imagePath: String)

        /**
         * Called on error
         * @param error Error message
         */
        fun onError(error: String)

        /**
         * Called when generation starts
         */
        fun onStart()
    }

    /**
     * Load the native library
     */
    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("sd-jni")
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
     * Initialize Stable Diffusion with model
     * @param modelPath Path to the SD model
     * @param vaePath Path to VAE (optional)
     * @param loraPath Path to LoRA weights (optional)
     * @param useGpu Whether to use GPU acceleration
     * @return True if initialization successful
     */
    @JvmStatic
    external fun sd_init(
        modelPath: String,
        vaePath: String,
        loraPath: String,
        useGpu: Boolean
    ): Boolean

    /**
     * Generate image from text prompt (txt2img)
     * @param prompt Positive prompt
     * @param negativePrompt Negative prompt
     * @param outputPath Path to save generated image
     * @param width Image width
     * @param height Image height
     * @param steps Number of diffusion steps
     * @param guidanceScale CFG scale
     * @param seed Random seed (-1 for random)
     * @param callback Progress callback
     */
    @JvmStatic
    external fun sd_txt2img(
        prompt: String,
        negativePrompt: String,
        outputPath: String,
        width: Int,
        height: Int,
        steps: Int,
        guidanceScale: Float,
        seed: Long,
        callback: SDGenImgCallback
    )

    /**
     * Generate image from image and text (img2img)
     * @param inputImagePath Path to input image
     * @param prompt Positive prompt
     * @param negativePrompt Negative prompt
     * @param outputPath Path to save generated image
     * @param strength Denoising strength (0.0-1.0)
     * @param steps Number of diffusion steps
     * @param guidanceScale CFG scale
     * @param seed Random seed
     * @param callback Progress callback
     */
    @JvmStatic
    external fun sd_img2img(
        inputImagePath: String,
        prompt: String,
        negativePrompt: String,
        outputPath: String,
        strength: Float,
        steps: Int,
        guidanceScale: Float,
        seed: Long,
        callback: SDGenImgCallback
    )

    /**
     * Upscale image using ESRGAN or similar
     * @param inputPath Input image path
     * @param outputPath Output image path
     * @param scale Upscale factor (2 or 4)
     */
    @JvmStatic
    external fun sd_upscale(
        inputPath: String,
        outputPath: String,
        scale: Int
    )

    /**
     * Free model resources
     */
    @JvmStatic
    external fun sd_free()

    /**
     * Get available schedulers
     */
    @JvmStatic
    external fun sd_get_schedulers(): Array<String>

    /**
     * Set scheduler
     * @param scheduler Scheduler name (euler, euler_a, dpm++, etc.)
     */
    @JvmStatic
    external fun sd_set_scheduler(scheduler: String)
}

/**
 * High-level wrapper for Stable Diffusion generation
 */
class StableDiffusionEngine {

    data class ModelConfig(
        val modelPath: String,
        val vaePath: String = "",
        val loraPath: String = "",
        val useGpu: Boolean = true
    )

    data class GenerationConfig(
        val prompt: String,
        val negativePrompt: String = "",
        val width: Int = 512,
        val height: Int = 512,
        val steps: Int = 20,
        val guidanceScale: Float = 7.5f,
        val seed: Long = -1,
        val scheduler: String = "euler_a"
    )

    data class Img2ImgConfig(
        val inputImagePath: String,
        val prompt: String,
        val negativePrompt: String = "",
        val strength: Float = 0.75f,
        val steps: Int = 20,
        val guidanceScale: Float = 7.5f,
        val seed: Long = -1
    )

    sealed class GenerationResult {
        data class Success(val imagePath: String) : GenerationResult()
        data class Error(val message: String) : GenerationResult()
    }

    private var isInitialized = false

    /**
     * Initialize with model
     */
    fun initialize(config: ModelConfig): Result<Unit> {
        return try {
            if (!SDJni.loadLibrary()) {
                return Result.failure(Exception("Failed to load sd-jni library"))
            }

            val success = SDJni.sd_init(
                config.modelPath,
                config.vaePath,
                config.loraPath,
                config.useGpu
            )

            if (success) {
                isInitialized = true
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to initialize SD model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate image from text
     */
    fun generateImage(
        config: GenerationConfig,
        outputPath: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): GenerationResult {
        if (!isInitialized) {
            return GenerationResult.Error("Engine not initialized")
        }

        var result: GenerationResult = GenerationResult.Error("Generation did not complete")

        val callback = object : SDJni.SDGenImgCallback {
            override fun onProgress(step: Int, totalSteps: Int) {
                onProgress?.invoke(step, totalSteps)
            }

            override fun onComplete(imagePath: String) {
                result = GenerationResult.Success(imagePath)
            }

            override fun onError(error: String) {
                result = GenerationResult.Error(error)
            }

            override fun onStart() {}
        }

        try {
            if (config.scheduler.isNotEmpty()) {
                SDJni.sd_set_scheduler(config.scheduler)
            }

            SDJni.sd_txt2img(
                config.prompt,
                config.negativePrompt,
                outputPath,
                config.width,
                config.height,
                config.steps,
                config.guidanceScale,
                config.seed,
                callback
            )
        } catch (e: Exception) {
            return GenerationResult.Error(e.message ?: "Unknown error")
        }

        return result
    }

    /**
     * Generate image from image (img2img)
     */
    fun transformImage(
        config: Img2ImgConfig,
        outputPath: String,
        onProgress: ((Int, Int) -> Unit)? = null
    ): GenerationResult {
        if (!isInitialized) {
            return GenerationResult.Error("Engine not initialized")
        }

        var result: GenerationResult = GenerationResult.Error("Generation did not complete")

        val callback = object : SDJni.SDGenImgCallback {
            override fun onProgress(step: Int, totalSteps: Int) {
                onProgress?.invoke(step, totalSteps)
            }

            override fun onComplete(imagePath: String) {
                result = GenerationResult.Success(imagePath)
            }

            override fun onError(error: String) {
                result = GenerationResult.Error(error)
            }

            override fun onStart() {}
        }

        try {
            SDJni.sd_img2img(
                config.inputImagePath,
                config.prompt,
                config.negativePrompt,
                outputPath,
                config.strength,
                config.steps,
                config.guidanceScale,
                config.seed,
                callback
            )
        } catch (e: Exception) {
            return GenerationResult.Error(e.message ?: "Unknown error")
        }

        return result
    }

    /**
     * Upscale image
     */
    fun upscaleImage(inputPath: String, outputPath: String, scale: Int = 2): Result<String> {
        if (!isInitialized) {
            return Result.failure(Exception("Engine not initialized"))
        }

        return try {
            SDJni.sd_upscale(inputPath, outputPath, scale)
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available schedulers
     */
    fun getSchedulers(): List<String> {
        return try {
            SDJni.sd_get_schedulers().toList()
        } catch (e: Exception) {
            listOf("euler", "euler_a", "dpm++")
        }
    }

    /**
     * Shutdown and free resources
     */
    fun shutdown() {
        if (isInitialized) {
            try {
                SDJni.sd_free()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            isInitialized = false
        }
    }
}
