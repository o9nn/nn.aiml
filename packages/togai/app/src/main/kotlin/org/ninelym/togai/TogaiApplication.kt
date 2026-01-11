package org.ninelym.togai

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.ninelym.native.NativeLibraryLoader
import org.ninelym.optimization.MemoryOptimizer
import org.ninelym.optimization.PerformanceMonitor
import org.ninelym.quantum.QuantumNeuralProcessor
import org.ninelym.evolution.MultimodalOrchestrator

/**
 * Main Application class for Togai (9mly)
 * 
 * Initializes core systems:
 * - Native library loading (87 ARM64 libraries)
 * - Memory optimization
 * - Performance monitoring
 * - Quantum neural processing
 * - Multimodal AI orchestration
 */
class TogaiApplication : Application() {
    
    private val TAG = "TogaiApplication"
    
    // Application scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Core systems
    lateinit var memoryOptimizer: MemoryOptimizer
        private set
    
    lateinit var performanceMonitor: PerformanceMonitor
        private set
    
    lateinit var quantumProcessor: QuantumNeuralProcessor
        private set
    
    lateinit var multimodalOrchestrator: MultimodalOrchestrator
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        Log.i(TAG, "Togai Application starting...")
        Log.i(TAG, "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        
        // Initialize core systems
        initializeCoreSystem()
        
        // Load native libraries
        loadNativeLibraries()
        
        // Initialize AI systems
        initializeAISystems()
        
        Log.i(TAG, "Togai Application initialized successfully")
    }
    
    /**
     * Initialize core optimization systems
     */
    private fun initializeCoreSystem() {
        try {
            // Initialize memory optimizer
            memoryOptimizer = MemoryOptimizer(this)
            Log.i(TAG, "✓ Memory optimizer initialized")
            
            // Initialize performance monitor
            performanceMonitor = PerformanceMonitor()
            Log.i(TAG, "✓ Performance monitor initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize core systems", e)
            throw e
        }
    }
    
    /**
     * Load native libraries
     */
    private fun loadNativeLibraries() {
        try {
            performanceMonitor.measureSync("load_native_libraries") {
                // Load core libraries first
                NativeLibraryLoader.loadCoreLibraries()
                
                // Check available memory for additional libraries
                val availableMemory = memoryOptimizer.memoryStats.value.availableMemoryMB
                
                when {
                    availableMemory > 2000 -> {
                        // High memory mode - load everything
                        Log.i(TAG, "High memory mode: Loading all libraries")
                        NativeLibraryLoader.loadAllLibraries(enableGPU = true)
                    }
                    availableMemory > 1000 -> {
                        // Medium memory mode - load essentials
                        Log.i(TAG, "Medium memory mode: Loading essential libraries")
                        NativeLibraryLoader.loadAILibraries(enableGPU = true)
                        NativeLibraryLoader.loadSpeechLibraries()
                        NativeLibraryLoader.loadStorageLibraries()
                    }
                    else -> {
                        // Low memory mode - minimal libraries
                        Log.i(TAG, "Low memory mode: Loading minimal libraries")
                        NativeLibraryLoader.loadAILibraries(enableGPU = false)
                        NativeLibraryLoader.loadStorageLibraries()
                    }
                }
                
                val stats = NativeLibraryLoader.getStatistics()
                Log.i(TAG, "✓ Loaded ${stats.totalLoaded} native libraries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native libraries", e)
            // Continue with reduced functionality
        }
    }
    
    /**
     * Initialize AI systems
     */
    private fun initializeAISystems() {
        applicationScope.launch {
            try {
                // Initialize quantum processor
                quantumProcessor = QuantumNeuralProcessor()
                quantumProcessor.initialize(numQubits = 64)
                Log.i(TAG, "✓ Quantum processor initialized")
                
                // Initialize multimodal orchestrator
                multimodalOrchestrator = MultimodalOrchestrator(
                    memoryOptimizer = memoryOptimizer,
                    performanceMonitor = performanceMonitor,
                    quantumProcessor = quantumProcessor
                )
                
                multimodalOrchestrator.initialize()
                Log.i(TAG, "✓ Multimodal orchestrator initialized")
                
                // Log capabilities
                val capabilities = multimodalOrchestrator.getCapabilities()
                Log.i(TAG, "AI Capabilities:")
                Log.i(TAG, "  - Language Processing: ${capabilities.languageProcessing}")
                Log.i(TAG, "  - Image Generation: ${capabilities.imageGeneration}")
                Log.i(TAG, "  - Speech Processing: ${capabilities.speechProcessing}")
                Log.i(TAG, "  - Quantum Processing: ${capabilities.quantumProcessing}")
                Log.i(TAG, "  - Multimodal Fusion: ${capabilities.multimodalFusion}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AI systems", e)
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
        
        // Trigger aggressive cleanup
        memoryOptimizer.clearAllCaches()
        System.gc()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "Trim memory: level=$level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                // Critical memory pressure
                memoryOptimizer.clearAllCaches()
                System.gc()
            }
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_MODERATE -> {
                // Moderate memory pressure
                memoryOptimizer.clearAllCaches()
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "Togai Application terminating...")
        
        // Shutdown systems
        try {
            memoryOptimizer.shutdown()
            performanceMonitor.shutdown()
            quantumProcessor.shutdown()
            multimodalOrchestrator.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
    
    companion object {
        /**
         * Get application instance
         */
        fun getInstance(application: Application): TogaiApplication {
            return application as TogaiApplication
        }
    }
}
