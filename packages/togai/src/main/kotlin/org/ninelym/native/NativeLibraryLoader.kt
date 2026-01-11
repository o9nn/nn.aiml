package org.ninelym.native

import android.util.Log
import java.io.File

/**
 * Native Library Loader for ARM64-v8a libraries
 * 
 * Manages loading of 87 native libraries for AI, ML, and cognitive computing
 */
object NativeLibraryLoader {
    private const val TAG = "NativeLibraryLoader"
    private val loadedLibraries = mutableSetOf<String>()
    
    /**
     * Core libraries that must be loaded first
     */
    private val coreLibraries = listOf(
        "c++_shared",
        "folly_runtime",
        "fbjni",
        "glog"
    )
    
    /**
     * AI/ML core libraries
     */
    private val aiLibraries = listOf(
        "ggml",
        "ggml-base",
        "ggml-cpu",
        "ggml-blas",
        "llama",
        "llama-jni",
        "onnxruntime",
        "onnxruntime4j_jni",
        "ncnn",
        "tvm4j_runtime_packed"
    )
    
    /**
     * GPU acceleration libraries
     */
    private val gpuLibraries = listOf(
        "ggml-opencl",
        "ggml-vulkan",
        "OpenCL"
    )
    
    /**
     * Image generation libraries
     */
    private val imageLibraries = listOf(
        "sd-jni",
        "imaegenerator_gpu",
        "mediapipe_tasks_vision_image_generator_jni"
    )
    
    /**
     * Speech processing libraries
     */
    private val speechLibraries = listOf(
        "kaldi-decoder-core",
        "kaldi-native-fbank-core",
        "sherpa-onnx-jni",
        "piper_phonemize",
        "sentencepiece",
        "sentencepiece_train",
        "ssentencepiece_core",
        "tokenizers-jni",
        "espeak-ng"
    )
    
    /**
     * React Native libraries
     */
    private val reactNativeLibraries = listOf(
        "hermes",
        "hermes_executor",
        "hermesinstancejni",
        "jsi",
        "jsinspector",
        "reactnativejni",
        "react_nativemodule_core",
        "react_render_core",
        "reanimated",
        "rninstance"
    )
    
    /**
     * Storage and data libraries
     */
    private val storageLibraries = listOf(
        "lvdb-jni",
        "mmkv"
    )
    
    /**
     * Animation libraries
     */
    private val animationLibraries = listOf(
        "rive-android",
        "cubism"
    )
    
    /**
     * Cognitive computing libraries
     */
    private val cognitiveLibraries = listOf(
        "laylaQNN",
        "ucd"
    )
    
    /**
     * Performance optimization libraries
     */
    private val performanceLibraries = listOf(
        "openblas",
        "omp",
        "spdlog"
    )
    
    /**
     * Load all core libraries required for basic functionality
     */
    fun loadCoreLibraries() {
        Log.i(TAG, "Loading core libraries...")
        loadLibraries(coreLibraries)
    }
    
    /**
     * Load AI/ML libraries for language models and inference
     */
    fun loadAILibraries(enableGPU: Boolean = true) {
        Log.i(TAG, "Loading AI/ML libraries...")
        loadLibraries(performanceLibraries)
        loadLibraries(aiLibraries)
        
        if (enableGPU) {
            loadLibraries(gpuLibraries, optional = true)
        }
    }
    
    /**
     * Load image generation libraries
     */
    fun loadImageGenerationLibraries() {
        Log.i(TAG, "Loading image generation libraries...")
        loadLibraries(imageLibraries, optional = true)
    }
    
    /**
     * Load speech processing libraries
     */
    fun loadSpeechLibraries() {
        Log.i(TAG, "Loading speech processing libraries...")
        loadLibraries(speechLibraries, optional = true)
    }
    
    /**
     * Load React Native framework libraries
     */
    fun loadReactNativeLibraries() {
        Log.i(TAG, "Loading React Native libraries...")
        loadLibraries(reactNativeLibraries)
    }
    
    /**
     * Load storage and database libraries
     */
    fun loadStorageLibraries() {
        Log.i(TAG, "Loading storage libraries...")
        loadLibraries(storageLibraries)
    }
    
    /**
     * Load animation libraries for character system
     */
    fun loadAnimationLibraries() {
        Log.i(TAG, "Loading animation libraries...")
        loadLibraries(animationLibraries, optional = true)
    }
    
    /**
     * Load cognitive computing libraries
     */
    fun loadCognitiveLibraries() {
        Log.i(TAG, "Loading cognitive computing libraries...")
        loadLibraries(cognitiveLibraries, optional = true)
    }
    
    /**
     * Load all libraries (full initialization)
     */
    fun loadAllLibraries(enableGPU: Boolean = true) {
        Log.i(TAG, "Loading all native libraries...")
        
        loadCoreLibraries()
        loadStorageLibraries()
        loadAILibraries(enableGPU)
        loadImageGenerationLibraries()
        loadSpeechLibraries()
        loadReactNativeLibraries()
        loadAnimationLibraries()
        loadCognitiveLibraries()
        
        Log.i(TAG, "Loaded ${loadedLibraries.size} native libraries")
    }
    
    /**
     * Load a list of libraries
     */
    private fun loadLibraries(libraries: List<String>, optional: Boolean = false) {
        for (lib in libraries) {
            loadLibrary(lib, optional)
        }
    }
    
    /**
     * Load a single library
     */
    private fun loadLibrary(name: String, optional: Boolean = false) {
        if (loadedLibraries.contains(name)) {
            Log.d(TAG, "Library already loaded: $name")
            return
        }
        
        try {
            System.loadLibrary(name)
            loadedLibraries.add(name)
            Log.d(TAG, "✓ Loaded library: lib$name.so")
        } catch (e: UnsatisfiedLinkError) {
            if (optional) {
                Log.w(TAG, "✗ Optional library not available: lib$name.so")
            } else {
                Log.e(TAG, "✗ Failed to load required library: lib$name.so", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error loading library: lib$name.so", e)
            if (!optional) throw e
        }
    }
    
    /**
     * Check if a library is loaded
     */
    fun isLibraryLoaded(name: String): Boolean {
        return loadedLibraries.contains(name)
    }
    
    /**
     * Get list of loaded libraries
     */
    fun getLoadedLibraries(): Set<String> {
        return loadedLibraries.toSet()
    }
    
    /**
     * Get library loading statistics
     */
    fun getStatistics(): LibraryStatistics {
        return LibraryStatistics(
            totalLoaded = loadedLibraries.size,
            coreLoaded = coreLibraries.count { loadedLibraries.contains(it) },
            aiLoaded = aiLibraries.count { loadedLibraries.contains(it) },
            speechLoaded = speechLibraries.count { loadedLibraries.contains(it) },
            reactNativeLoaded = reactNativeLibraries.count { loadedLibraries.contains(it) }
        )
    }
}

/**
 * Statistics about loaded native libraries
 */
data class LibraryStatistics(
    val totalLoaded: Int,
    val coreLoaded: Int,
    val aiLoaded: Int,
    val speechLoaded: Int,
    val reactNativeLoaded: Int
)
