package org.ninelym.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * Advanced Memory Optimizer for AI-intensive Android application
 * 
 * Manages memory for 87 native libraries, large AI models, and cognitive computing
 */
class MemoryOptimizer(private val context: Context) {
    private val TAG = "MemoryOptimizer"
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    private val _memoryState = MutableStateFlow(MemoryState.OPTIMAL)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    private val _memoryStats = MutableStateFlow(MemoryStatistics())
    val memoryStats: StateFlow<MemoryStatistics> = _memoryStats.asStateFlow()
    
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Memory thresholds (in MB)
    private val CRITICAL_THRESHOLD = 100
    private val WARNING_THRESHOLD = 200
    private val OPTIMAL_THRESHOLD = 500
    
    // Cached objects with weak references
    private val objectCache = mutableMapOf<String, WeakReference<Any>>()
    
    // Memory pools for frequent allocations
    private val tensorPool = TensorMemoryPool(maxSize = 50)
    private val atomPool = AtomMemoryPool(maxSize = 1000)
    
    init {
        startMemoryMonitoring()
    }
    
    /**
     * Start continuous memory monitoring
     */
    private fun startMemoryMonitoring() {
        monitoringScope.launch {
            while (isActive) {
                updateMemoryStatistics()
                checkMemoryPressure()
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Update current memory statistics
     */
    private fun updateMemoryStatistics() {
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        val dalvikHeap = runtime.totalMemory() - runtime.freeMemory()
        
        val stats = MemoryStatistics(
            availableMemoryMB = memoryInfo.availMem / (1024 * 1024),
            totalMemoryMB = memoryInfo.totalMem / (1024 * 1024),
            usedMemoryMB = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024),
            nativeHeapMB = nativeHeap / (1024 * 1024),
            dalvikHeapMB = dalvikHeap / (1024 * 1024),
            isLowMemory = memoryInfo.lowMemory,
            threshold = memoryInfo.threshold / (1024 * 1024)
        )
        
        _memoryStats.value = stats
    }
    
    /**
     * Check for memory pressure and adjust state
     */
    private fun checkMemoryPressure() {
        val availableMB = _memoryStats.value.availableMemoryMB
        
        val newState = when {
            availableMB < CRITICAL_THRESHOLD -> {
                Log.w(TAG, "CRITICAL memory pressure: ${availableMB}MB available")
                triggerAggressiveCleanup()
                MemoryState.CRITICAL
            }
            availableMB < WARNING_THRESHOLD -> {
                Log.w(TAG, "WARNING memory pressure: ${availableMB}MB available")
                triggerModerateCleanup()
                MemoryState.WARNING
            }
            availableMB < OPTIMAL_THRESHOLD -> {
                MemoryState.MODERATE
            }
            else -> {
                MemoryState.OPTIMAL
            }
        }
        
        if (newState != _memoryState.value) {
            _memoryState.value = newState
            Log.i(TAG, "Memory state changed to: $newState")
        }
    }
    
    /**
     * Trigger aggressive memory cleanup
     */
    private fun triggerAggressiveCleanup() {
        Log.i(TAG, "Triggering aggressive memory cleanup...")
        
        // Clear all caches
        clearAllCaches()
        
        // Clear memory pools
        tensorPool.clear()
        atomPool.clear()
        
        // Request garbage collection
        System.gc()
        Runtime.getRuntime().gc()
        
        // Trim memory in native libraries
        trimNativeMemory()
    }
    
    /**
     * Trigger moderate memory cleanup
     */
    private fun triggerModerateCleanup() {
        Log.i(TAG, "Triggering moderate memory cleanup...")
        
        // Clear expired cache entries
        clearExpiredCaches()
        
        // Trim memory pools
        tensorPool.trim(0.5f)
        atomPool.trim(0.5f)
        
        // Suggest garbage collection
        System.gc()
    }
    
    /**
     * Clear all cached objects
     */
    fun clearAllCaches() {
        synchronized(objectCache) {
            val size = objectCache.size
            objectCache.clear()
            Log.d(TAG, "Cleared $size cache entries")
        }
    }
    
    /**
     * Clear expired cache entries (weak references that are null)
     */
    private fun clearExpiredCaches() {
        synchronized(objectCache) {
            val iterator = objectCache.iterator()
            var cleared = 0
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.get() == null) {
                    iterator.remove()
                    cleared++
                }
            }
            Log.d(TAG, "Cleared $cleared expired cache entries")
        }
    }
    
    /**
     * Cache an object with weak reference
     */
    fun <T : Any> cacheObject(key: String, obj: T) {
        synchronized(objectCache) {
            objectCache[key] = WeakReference(obj)
        }
    }
    
    /**
     * Retrieve cached object
     */
    fun <T : Any> getCachedObject(key: String): T? {
        synchronized(objectCache) {
            @Suppress("UNCHECKED_CAST")
            return objectCache[key]?.get() as? T
        }
    }
    
    /**
     * Trim native memory in loaded libraries
     */
    private fun trimNativeMemory() {
        try {
            // Signal native libraries to release unused memory
            // This would call native methods if implemented
            Log.d(TAG, "Trimming native memory...")
        } catch (e: Exception) {
            Log.e(TAG, "Error trimming native memory", e)
        }
    }
    
    /**
     * Get tensor from memory pool or create new
     */
    fun <T> acquireTensor(creator: () -> T): T {
        return tensorPool.acquire(creator)
    }
    
    /**
     * Release tensor back to memory pool
     */
    fun <T> releaseTensor(tensor: T) {
        tensorPool.release(tensor)
    }
    
    /**
     * Get atom from memory pool or create new
     */
    fun <T> acquireAtom(creator: () -> T): T {
        return atomPool.acquire(creator)
    }
    
    /**
     * Release atom back to memory pool
     */
    fun <T> releaseAtom(atom: T) {
        atomPool.release(atom)
    }
    
    /**
     * Check if memory is available for operation
     */
    fun isMemoryAvailable(requiredMB: Long): Boolean {
        val available = _memoryStats.value.availableMemoryMB
        return available >= requiredMB
    }
    
    /**
     * Wait for memory to become available
     */
    suspend fun waitForMemory(requiredMB: Long, timeoutMs: Long = 30000): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (!isMemoryAvailable(requiredMB)) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                Log.w(TAG, "Timeout waiting for ${requiredMB}MB memory")
                return false
            }
            
            triggerModerateCleanup()
            delay(1000)
        }
        
        return true
    }
    
    /**
     * Optimize memory for AI model loading
     */
    suspend fun optimizeForModelLoading(modelSizeMB: Long): Boolean {
        Log.i(TAG, "Optimizing memory for ${modelSizeMB}MB model...")
        
        // Require 1.5x the model size to be safe
        val requiredMB = (modelSizeMB * 1.5).toLong()
        
        if (!isMemoryAvailable(requiredMB)) {
            triggerAggressiveCleanup()
            return waitForMemory(requiredMB)
        }
        
        return true
    }
    
    /**
     * Get memory recommendations
     */
    fun getMemoryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val stats = _memoryStats.value
        
        when (_memoryState.value) {
            MemoryState.CRITICAL -> {
                recommendations.add("Critical memory pressure - close other apps")
                recommendations.add("Disable optional features (image generation, animations)")
                recommendations.add("Reduce model size or use quantized models")
            }
            MemoryState.WARNING -> {
                recommendations.add("Low memory - consider closing background apps")
                recommendations.add("Reduce cache sizes")
                recommendations.add("Use CPU instead of GPU for inference")
            }
            MemoryState.MODERATE -> {
                recommendations.add("Memory usage is moderate")
                recommendations.add("GPU acceleration available")
            }
            MemoryState.OPTIMAL -> {
                recommendations.add("Memory usage is optimal")
                recommendations.add("All features available")
            }
        }
        
        if (stats.nativeHeapMB > 500) {
            recommendations.add("High native heap usage (${stats.nativeHeapMB}MB)")
        }
        
        return recommendations
    }
    
    /**
     * Shutdown memory optimizer
     */
    fun shutdown() {
        monitoringScope.cancel()
        clearAllCaches()
        tensorPool.clear()
        atomPool.clear()
    }
}

/**
 * Memory state levels
 */
enum class MemoryState {
    OPTIMAL,    // > 500MB available
    MODERATE,   // 200-500MB available
    WARNING,    // 100-200MB available
    CRITICAL    // < 100MB available
}

/**
 * Memory statistics
 */
data class MemoryStatistics(
    val availableMemoryMB: Long = 0,
    val totalMemoryMB: Long = 0,
    val usedMemoryMB: Long = 0,
    val nativeHeapMB: Long = 0,
    val dalvikHeapMB: Long = 0,
    val isLowMemory: Boolean = false,
    val threshold: Long = 0
) {
    val usagePercentage: Float
        get() = if (totalMemoryMB > 0) (usedMemoryMB.toFloat() / totalMemoryMB) * 100 else 0f
}

/**
 * Generic memory pool for object reuse
 */
class TensorMemoryPool<T>(private val maxSize: Int) {
    private val pool = mutableListOf<T>()
    
    @Synchronized
    fun acquire(creator: () -> T): T {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            creator()
        }
    }
    
    @Synchronized
    fun release(obj: T) {
        if (pool.size < maxSize) {
            pool.add(obj)
        }
    }
    
    @Synchronized
    fun trim(factor: Float) {
        val targetSize = (pool.size * factor).toInt()
        while (pool.size > targetSize) {
            pool.removeAt(pool.size - 1)
        }
    }
    
    @Synchronized
    fun clear() {
        pool.clear()
    }
}

/**
 * Memory pool for cognitive atoms
 */
class AtomMemoryPool<T>(private val maxSize: Int) {
    private val pool = mutableListOf<T>()
    
    @Synchronized
    fun acquire(creator: () -> T): T {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            creator()
        }
    }
    
    @Synchronized
    fun release(obj: T) {
        if (pool.size < maxSize) {
            pool.add(obj)
        }
    }
    
    @Synchronized
    fun trim(factor: Float) {
        val targetSize = max(1, (pool.size * factor).toInt())
        while (pool.size > targetSize) {
            pool.removeAt(pool.size - 1)
        }
    }
    
    @Synchronized
    fun clear() {
        pool.clear()
    }
}
