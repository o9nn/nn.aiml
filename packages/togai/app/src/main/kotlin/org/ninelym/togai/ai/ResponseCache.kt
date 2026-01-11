package org.ninelym.togai.ai

import android.content.Context
import android.util.LruCache
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ninelym.togai.util.ErrorHandler
import java.security.MessageDigest

/**
 * Advanced AI response caching system for Togai
 * Implements multi-level caching with memory and persistent storage
 * Reduces API calls and improves response time
 */
class ResponseCache private constructor(context: Context) {
    
    companion object {
        private const val TAG = "ResponseCache"
        private const val CACHE_NAME = "ai_response_cache"
        private const val MAX_MEMORY_CACHE_SIZE = 50 // Number of responses
        private const val MAX_CACHE_AGE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        
        @Volatile
        private var instance: ResponseCache? = null
        
        fun getInstance(context: Context): ResponseCache {
            return instance ?: synchronized(this) {
                instance ?: ResponseCache(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * Cached response data structure
     */
    @Serializable
    data class CachedResponse(
        val prompt: String,
        val response: String,
        val timestamp: Long,
        val modelName: String,
        val parameters: Map<String, String> = emptyMap(),
        val tokenCount: Int = 0,
        val responseTimeMs: Long = 0
    )
    
    /**
     * Cache statistics
     */
    data class CacheStats(
        val hits: Int = 0,
        val misses: Int = 0,
        val totalRequests: Int = 0,
        val hitRate: Double = 0.0,
        val averageResponseTime: Long = 0,
        val totalSavedTokens: Int = 0
    )
    
    // Memory cache (LRU)
    private val memoryCache = LruCache<String, CachedResponse>(MAX_MEMORY_CACHE_SIZE)
    
    // Persistent cache (MMKV)
    private val persistentCache: MMKV = MMKV.mmkvWithID(CACHE_NAME)
    
    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Cache statistics
    private var stats = CacheStats()
    
    /**
     * Get cached response for a prompt
     */
    suspend fun get(
        prompt: String,
        modelName: String,
        parameters: Map<String, String> = emptyMap()
    ): CachedResponse? = withContext(Dispatchers.IO) {
        val key = generateKey(prompt, modelName, parameters)
        
        // Try memory cache first
        memoryCache.get(key)?.let { cached ->
            if (isValid(cached)) {
                updateStats(hit = true, cached)
                ErrorHandler.logDebug(TAG, "Memory cache hit for key: $key")
                return@withContext cached
            } else {
                memoryCache.remove(key)
            }
        }
        
        // Try persistent cache
        persistentCache.decodeString(key)?.let { jsonString ->
            try {
                val cached = json.decodeFromString<CachedResponse>(jsonString)
                if (isValid(cached)) {
                    // Promote to memory cache
                    memoryCache.put(key, cached)
                    updateStats(hit = true, cached)
                    ErrorHandler.logDebug(TAG, "Persistent cache hit for key: $key")
                    return@withContext cached
                } else {
                    persistentCache.remove(key)
                }
            } catch (e: Exception) {
                ErrorHandler.logError(TAG, "Failed to deserialize cached response", e)
                persistentCache.remove(key)
            }
        }
        
        // Cache miss
        updateStats(hit = false)
        ErrorHandler.logDebug(TAG, "Cache miss for key: $key")
        null
    }
    
    /**
     * Store response in cache
     */
    suspend fun put(
        prompt: String,
        response: String,
        modelName: String,
        parameters: Map<String, String> = emptyMap(),
        tokenCount: Int = 0,
        responseTimeMs: Long = 0
    ) = withContext(Dispatchers.IO) {
        val key = generateKey(prompt, modelName, parameters)
        val cached = CachedResponse(
            prompt = prompt,
            response = response,
            timestamp = System.currentTimeMillis(),
            modelName = modelName,
            parameters = parameters,
            tokenCount = tokenCount,
            responseTimeMs = responseTimeMs
        )
        
        // Store in memory cache
        memoryCache.put(key, cached)
        
        // Store in persistent cache
        try {
            val jsonString = json.encodeToString(cached)
            persistentCache.encode(key, jsonString)
            ErrorHandler.logDebug(TAG, "Cached response for key: $key")
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Failed to cache response", e)
        }
    }
    
    /**
     * Clear all caches
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        persistentCache.clearAll()
        stats = CacheStats()
        ErrorHandler.logInfo(TAG, "Cache cleared")
    }
    
    /**
     * Clear expired entries
     */
    suspend fun clearExpired() = withContext(Dispatchers.IO) {
        val keys = persistentCache.allKeys() ?: return@withContext
        var clearedCount = 0
        
        keys.forEach { key ->
            persistentCache.decodeString(key)?.let { jsonString ->
                try {
                    val cached = json.decodeFromString<CachedResponse>(jsonString)
                    if (!isValid(cached)) {
                        persistentCache.remove(key)
                        memoryCache.remove(key)
                        clearedCount++
                    }
                } catch (e: Exception) {
                    persistentCache.remove(key)
                    clearedCount++
                }
            }
        }
        
        ErrorHandler.logInfo(TAG, "Cleared $clearedCount expired entries")
    }
    
    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        return stats.copy(
            hitRate = if (stats.totalRequests > 0) {
                stats.hits.toDouble() / stats.totalRequests.toDouble()
            } else 0.0
        )
    }
    
    /**
     * Get cache size
     */
    fun getCacheSize(): Pair<Int, Int> {
        return Pair(memoryCache.size(), persistentCache.count().toInt())
    }
    
    /**
     * Check if a cached response is still valid
     */
    private fun isValid(cached: CachedResponse): Boolean {
        val age = System.currentTimeMillis() - cached.timestamp
        return age < MAX_CACHE_AGE_MS
    }
    
    /**
     * Generate cache key from prompt and parameters
     */
    private fun generateKey(
        prompt: String,
        modelName: String,
        parameters: Map<String, String>
    ): String {
        val combined = buildString {
            append(prompt)
            append("|")
            append(modelName)
            append("|")
            parameters.entries.sortedBy { it.key }.forEach { (key, value) ->
                append("$key=$value|")
            }
        }
        
        return MessageDigest.getInstance("SHA-256")
            .digest(combined.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Update cache statistics
     */
    private fun updateStats(hit: Boolean, cached: CachedResponse? = null) {
        stats = stats.copy(
            hits = if (hit) stats.hits + 1 else stats.hits,
            misses = if (!hit) stats.misses + 1 else stats.misses,
            totalRequests = stats.totalRequests + 1,
            totalSavedTokens = if (hit && cached != null) {
                stats.totalSavedTokens + cached.tokenCount
            } else stats.totalSavedTokens
        )
    }
}

/**
 * Extension functions for easier cache usage
 */

/**
 * Get or compute response with caching
 */
suspend fun ResponseCache.getOrPut(
    prompt: String,
    modelName: String,
    parameters: Map<String, String> = emptyMap(),
    compute: suspend () -> Pair<String, Int> // Returns (response, tokenCount)
): String {
    // Try to get from cache
    get(prompt, modelName, parameters)?.let { cached ->
        return cached.response
    }
    
    // Compute new response
    val startTime = System.currentTimeMillis()
    val (response, tokenCount) = compute()
    val responseTime = System.currentTimeMillis() - startTime
    
    // Cache the result
    put(
        prompt = prompt,
        response = response,
        modelName = modelName,
        parameters = parameters,
        tokenCount = tokenCount,
        responseTimeMs = responseTime
    )
    
    return response
}
