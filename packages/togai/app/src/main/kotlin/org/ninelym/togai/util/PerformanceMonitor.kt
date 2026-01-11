package org.ninelym.togai.util

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ninelym.togai.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utility for Togai
 * Tracks execution times, memory usage, and performance metrics
 */
object PerformanceMonitor {
    
    private const val TAG = "TogaiPerformance"
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    private var isEnabled = BuildConfig.DEBUG
    
    /**
     * Performance metric data class
     */
    data class PerformanceMetric(
        val name: String,
        val startTime: Long,
        var endTime: Long = 0,
        var duration: Long = 0,
        var memoryBefore: Long = 0,
        var memoryAfter: Long = 0,
        var memoryDelta: Long = 0
    )
    
    /**
     * Enable or disable performance monitoring
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Start tracking a performance metric
     */
    fun startTracking(name: String): String {
        if (!isEnabled) return name
        
        val metric = PerformanceMetric(
            name = name,
            startTime = SystemClock.elapsedRealtime(),
            memoryBefore = getUsedMemory()
        )
        
        metrics[name] = metric
        Log.d(TAG, "Started tracking: $name")
        
        return name
    }
    
    /**
     * Stop tracking a performance metric
     */
    fun stopTracking(name: String) {
        if (!isEnabled) return
        
        val metric = metrics[name] ?: run {
            Log.w(TAG, "Metric not found: $name")
            return
        }
        
        metric.endTime = SystemClock.elapsedRealtime()
        metric.duration = metric.endTime - metric.startTime
        metric.memoryAfter = getUsedMemory()
        metric.memoryDelta = metric.memoryAfter - metric.memoryBefore
        
        Log.d(TAG, buildString {
            append("Completed: ${metric.name}\n")
            append("  Duration: ${metric.duration}ms\n")
            append("  Memory delta: ${formatBytes(metric.memoryDelta)}")
        })
    }
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        if (!isEnabled) return block()
        
        var result: T
        val duration = measureTimeMillis {
            result = block()
        }
        
        Log.d(TAG, "$name completed in ${duration}ms")
        return result
    }
    
    /**
     * Measure execution time of a suspend block
     */
    suspend inline fun <T> measureSuspend(name: String, crossinline block: suspend () -> T): T {
        if (!isEnabled) return block()
        
        var result: T
        val duration = measureTimeMillis {
            result = block()
        }
        
        Log.d(TAG, "$name completed in ${duration}ms")
        return result
    }
    
    /**
     * Get all tracked metrics
     */
    fun getMetrics(): Map<String, PerformanceMetric> {
        return metrics.toMap()
    }
    
    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
        Log.d(TAG, "Cleared all metrics")
    }
    
    /**
     * Get metric by name
     */
    fun getMetric(name: String): PerformanceMetric? {
        return metrics[name]
    }
    
    /**
     * Log all metrics
     */
    fun logAllMetrics() {
        if (!isEnabled || metrics.isEmpty()) return
        
        Log.d(TAG, buildString {
            append("\n=== Performance Metrics ===\n")
            metrics.values.sortedByDescending { it.duration }.forEach { metric ->
                append("${metric.name}:\n")
                append("  Duration: ${metric.duration}ms\n")
                append("  Memory: ${formatBytes(metric.memoryDelta)}\n")
            }
            append("========================\n")
        })
    }
    
    /**
     * Get current memory usage
     */
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * Format bytes to human-readable format
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    /**
     * Track memory usage
     */
    fun logMemoryUsage(tag: String = TAG) {
        if (!isEnabled) return
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        Log.d(tag, buildString {
            append("\n=== Memory Usage ===\n")
            append("Used: ${formatBytes(usedMemory)}\n")
            append("Available: ${formatBytes(availableMemory)}\n")
            append("Max: ${formatBytes(maxMemory)}\n")
            append("Usage: ${(usedMemory * 100 / maxMemory)}%\n")
            append("===================\n")
        })
    }
    
    /**
     * Monitor memory usage periodically
     */
    fun startMemoryMonitoring(intervalMs: Long = 5000) {
        if (!isEnabled) return
        
        CoroutineScope(Dispatchers.Default).launch {
            while (isEnabled) {
                logMemoryUsage()
                kotlinx.coroutines.delay(intervalMs)
            }
        }
    }
}

/**
 * Extension function to measure execution time
 */
inline fun <T> T.measurePerformance(name: String, block: T.() -> Unit): T {
    PerformanceMonitor.measure(name) {
        block()
    }
    return this
}

/**
 * Annotation for methods that should be performance monitored
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PerformanceTracked(val name: String = "")
