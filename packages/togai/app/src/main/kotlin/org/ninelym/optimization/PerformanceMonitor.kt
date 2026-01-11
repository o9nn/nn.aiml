package org.ninelym.optimization

import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Advanced Performance Monitor for AI operations
 * 
 * Tracks inference times, memory usage, and system performance
 */
class PerformanceMonitor {
    private val TAG = "PerformanceMonitor"
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Operation timing tracking
    private val operationTimings = ConcurrentHashMap<String, OperationStats>()
    
    // Real-time performance tracking
    private val inferenceCount = AtomicLong(0)
    private val totalInferenceTime = AtomicLong(0)
    
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        startPerformanceMonitoring()
        logDeviceCapabilities()
    }
    
    /**
     * Log device capabilities for optimization
     */
    private fun logDeviceCapabilities() {
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i(TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.i(TAG, "ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        Log.i(TAG, "Cores: ${Runtime.getRuntime().availableProcessors()}")
    }
    
    /**
     * Start continuous performance monitoring
     */
    private fun startPerformanceMonitoring() {
        monitoringScope.launch {
            while (isActive) {
                updatePerformanceMetrics()
                delay(10000) // Update every 10 seconds
            }
        }
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        val avgInferenceTime = if (inferenceCount.get() > 0) {
            totalInferenceTime.get() / inferenceCount.get()
        } else {
            0L
        }
        
        val metrics = PerformanceMetrics(
            averageInferenceTimeMs = avgInferenceTime,
            totalInferences = inferenceCount.get(),
            operationStats = operationTimings.values.toList(),
            deviceInfo = DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                cpuCores = Runtime.getRuntime().availableProcessors(),
                supportedAbis = Build.SUPPORTED_ABIS.toList()
            )
        )
        
        _performanceMetrics.value = metrics
    }
    
    /**
     * Measure and track operation performance
     */
    suspend fun <T> measureOperation(
        operationName: String,
        operation: suspend () -> T
    ): T {
        val startTime = SystemClock.elapsedRealtime()
        val startMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        
        return try {
            val result = operation()
            
            val duration = SystemClock.elapsedRealtime() - startTime
            val endMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
            val memoryDelta = endMemory - startMemory
            
            recordOperation(operationName, duration, memoryDelta, success = true)
            
            result
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordOperation(operationName, duration, 0, success = false)
            throw e
        }
    }
    
    /**
     * Measure synchronous operation
     */
    fun <T> measureSync(
        operationName: String,
        operation: () -> T
    ): T {
        val startTime = SystemClock.elapsedRealtime()
        val startMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
        
        return try {
            val result = operation()
            
            val duration = SystemClock.elapsedRealtime() - startTime
            val endMemory = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
            val memoryDelta = endMemory - startMemory
            
            recordOperation(operationName, duration, memoryDelta, success = true)
            
            result
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            recordOperation(operationName, duration, 0, success = false)
            throw e
        }
    }
    
    /**
     * Record operation statistics
     */
    private fun recordOperation(
        name: String,
        durationMs: Long,
        memoryDeltaBytes: Long,
        success: Boolean
    ) {
        val stats = operationTimings.getOrPut(name) {
            OperationStats(operationName = name)
        }
        
        stats.recordExecution(durationMs, memoryDeltaBytes, success)
        
        // Track inference operations separately
        if (name.contains("inference", ignoreCase = true)) {
            inferenceCount.incrementAndGet()
            totalInferenceTime.addAndGet(durationMs)
        }
        
        Log.d(TAG, "$name: ${durationMs}ms, memory: ${memoryDeltaBytes / 1024}KB, success: $success")
    }
    
    /**
     * Get statistics for a specific operation
     */
    fun getOperationStats(operationName: String): OperationStats? {
        return operationTimings[operationName]
    }
    
    /**
     * Get all operation statistics
     */
    fun getAllOperationStats(): List<OperationStats> {
        return operationTimings.values.sortedByDescending { it.totalExecutions }
    }
    
    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _performanceMetrics.value
        
        // Check inference performance
        if (metrics.averageInferenceTimeMs > 5000) {
            recommendations.add("Inference time is high (${metrics.averageInferenceTimeMs}ms)")
            recommendations.add("Consider using quantized models or smaller model sizes")
            recommendations.add("Enable GPU acceleration if available")
        }
        
        // Check for slow operations
        val slowOps = operationTimings.values.filter { it.averageDurationMs > 1000 }
        if (slowOps.isNotEmpty()) {
            recommendations.add("Slow operations detected:")
            slowOps.forEach { op ->
                recommendations.add("  - ${op.operationName}: ${op.averageDurationMs}ms avg")
            }
        }
        
        // Check for memory-intensive operations
        val memoryIntensiveOps = operationTimings.values.filter { 
            it.averageMemoryDeltaMB > 50 
        }
        if (memoryIntensiveOps.isNotEmpty()) {
            recommendations.add("Memory-intensive operations:")
            memoryIntensiveOps.forEach { op ->
                recommendations.add("  - ${op.operationName}: ${op.averageMemoryDeltaMB}MB avg")
            }
        }
        
        // Check failure rates
        val failingOps = operationTimings.values.filter { it.failureRate > 0.1f }
        if (failingOps.isNotEmpty()) {
            recommendations.add("Operations with high failure rates:")
            failingOps.forEach { op ->
                recommendations.add("  - ${op.operationName}: ${(op.failureRate * 100).toInt()}% failures")
            }
        }
        
        // Device-specific recommendations
        val cores = Runtime.getRuntime().availableProcessors()
        if (cores <= 4) {
            recommendations.add("Device has limited CPU cores ($cores)")
            recommendations.add("Consider reducing parallel operations")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance is optimal")
        }
        
        return recommendations
    }
    
    /**
     * Reset all statistics
     */
    fun resetStatistics() {
        operationTimings.clear()
        inferenceCount.set(0)
        totalInferenceTime.set(0)
        Log.i(TAG, "Performance statistics reset")
    }
    
    /**
     * Export performance report
     */
    fun exportReport(): PerformanceReport {
        return PerformanceReport(
            metrics = _performanceMetrics.value,
            operationStats = getAllOperationStats(),
            recommendations = getPerformanceRecommendations(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Shutdown performance monitor
     */
    fun shutdown() {
        monitoringScope.cancel()
    }
}

/**
 * Performance metrics
 */
data class PerformanceMetrics(
    val averageInferenceTimeMs: Long = 0,
    val totalInferences: Long = 0,
    val operationStats: List<OperationStats> = emptyList(),
    val deviceInfo: DeviceInfo = DeviceInfo()
)

/**
 * Device information
 */
data class DeviceInfo(
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val cpuCores: Int = 0,
    val supportedAbis: List<String> = emptyList()
)

/**
 * Statistics for a specific operation
 */
data class OperationStats(
    val operationName: String,
    var totalExecutions: Long = 0,
    var successfulExecutions: Long = 0,
    var failedExecutions: Long = 0,
    var totalDurationMs: Long = 0,
    var minDurationMs: Long = Long.MAX_VALUE,
    var maxDurationMs: Long = 0,
    var totalMemoryDeltaBytes: Long = 0
) {
    val averageDurationMs: Long
        get() = if (totalExecutions > 0) totalDurationMs / totalExecutions else 0
    
    val averageMemoryDeltaMB: Long
        get() = if (totalExecutions > 0) (totalMemoryDeltaBytes / totalExecutions) / (1024 * 1024) else 0
    
    val successRate: Float
        get() = if (totalExecutions > 0) successfulExecutions.toFloat() / totalExecutions else 0f
    
    val failureRate: Float
        get() = 1f - successRate
    
    @Synchronized
    fun recordExecution(durationMs: Long, memoryDeltaBytes: Long, success: Boolean) {
        totalExecutions++
        if (success) {
            successfulExecutions++
        } else {
            failedExecutions++
        }
        
        totalDurationMs += durationMs
        minDurationMs = minOf(minDurationMs, durationMs)
        maxDurationMs = maxOf(maxDurationMs, durationMs)
        totalMemoryDeltaBytes += memoryDeltaBytes
    }
}

/**
 * Performance report
 */
data class PerformanceReport(
    val metrics: PerformanceMetrics,
    val operationStats: List<OperationStats>,
    val recommendations: List<String>,
    val timestamp: Long
)
