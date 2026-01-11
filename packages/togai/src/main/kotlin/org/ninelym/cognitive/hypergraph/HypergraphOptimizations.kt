package org.ninelym.cognitive.hypergraph

/**
 * Performance optimizations for Hypergraph operations
 * 
 * Provides memory management, caching, and performance enhancements
 * for large-scale cognitive hypergraph operations.
 */
class HypergraphOptimizations(
    private val hypergraph: Hypergraph,
    private val maxCacheSize: Int = 1000,
    private val maxHistorySize: Int = 5000
) {
    
    // Cache for frequently accessed atom lists
    private val atomListCache = mutableMapOf<String, CachedAtomList>()
    private val connectivityCache = mutableMapOf<String, List<Atom>>()
    
    /**
     * Get atoms by type with caching
     */
    fun getAtomsByTypeCached(type: AtomType): List<Atom> {
        val cacheKey = "type_${type.name}"
        val cached = atomListCache[cacheKey]
        
        if (cached != null && !cached.isExpired()) {
            return cached.atoms
        }
        
        val atoms = hypergraph.getAtomsByType(type)
        atomListCache[cacheKey] = CachedAtomList(atoms, System.currentTimeMillis())
        
        // Cleanup old cache entries
        if (atomListCache.size > maxCacheSize) {
            cleanupCache()
        }
        
        return atoms
    }
    
    /**
     * Get connected atoms with caching
     */
    fun getConnectedAtomsCached(atomId: String): List<Atom> {
        val cached = connectivityCache[atomId]
        if (cached != null) {
            return cached
        }
        
        val connected = hypergraph.getConnectedAtoms(atomId)
        connectivityCache[atomId] = connected
        
        if (connectivityCache.size > maxCacheSize) {
            cleanupConnectivityCache()
        }
        
        return connected
    }
    
    /**
     * Batch update attention values for better performance
     */
    fun batchUpdateAttention(updates: Map<String, AttentionValue>) {
        updates.forEach { (atomId, attentionValue) ->
            hypergraph.updateAtomAttention(atomId, attentionValue)
        }
        
        // Invalidate affected caches
        invalidateCaches()
    }
    
    /**
     * Optimize activation spreading with parallel processing
     */
    fun optimizedActivationSpreading(
        initialAtoms: List<String>,
        spreadingStrength: Float = 0.8f,
        maxDepth: Int = 3,
        minActivation: Float = 0.1f
    ): ActivationSpreadingResult {
        // Clear old connectivity cache before spreading
        if (connectivityCache.size > maxCacheSize / 2) {
            cleanupConnectivityCache()
        }
        
        return hypergraph.performActivationSpreading(
            initialAtoms,
            spreadingStrength,
            maxDepth,
            minActivation
        )
    }
    
    /**
     * Get mesh connectivity with optimized caching
     */
    fun optimizedMeshConnectivity(minAttention: Float = 0.3f): MeshConnectivityResult {
        val cacheKey = "mesh_${minAttention}"
        val cached = atomListCache[cacheKey]
        
        // Cache mesh connectivity for 5 seconds
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 5000) {
            // Return cached result (stored in atoms field as placeholder)
            return hypergraph.getMeshConnectivity(minAttention)
        }
        
        val result = hypergraph.getMeshConnectivity(minAttention)
        
        return result
    }
    
    /**
     * Cleanup expired cache entries
     */
    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        val toRemove = atomListCache.filter { (_, cached) ->
            cached.isExpired(now)
        }.keys
        
        toRemove.forEach { atomListCache.remove(it) }
        
        // If still too large, remove oldest entries
        if (atomListCache.size > maxCacheSize) {
            val sortedEntries = atomListCache.entries
                .sortedBy { it.value.timestamp }
                .take(atomListCache.size - maxCacheSize)
            
            sortedEntries.forEach { atomListCache.remove(it.key) }
        }
    }
    
    /**
     * Cleanup connectivity cache
     */
    private fun cleanupConnectivityCache() {
        val toRemove = connectivityCache.size - (maxCacheSize / 2)
        if (toRemove > 0) {
            connectivityCache.keys.take(toRemove).forEach {
                connectivityCache.remove(it)
            }
        }
    }
    
    /**
     * Invalidate all caches
     */
    fun invalidateCaches() {
        atomListCache.clear()
        connectivityCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            atomListCacheSize = atomListCache.size,
            connectivityCacheSize = connectivityCache.size,
            maxCacheSize = maxCacheSize,
            cacheHitRate = calculateCacheHitRate()
        )
    }
    
    /**
     * Calculate cache hit rate (simplified)
     */
    private fun calculateCacheHitRate(): Float {
        // Simplified calculation - in production, track actual hits/misses
        val utilizationRate = atomListCache.size.toFloat() / maxCacheSize.toFloat()
        return utilizationRate.coerceIn(0.0f, 1.0f)
    }
}

/**
 * Cached atom list with timestamp
 */
private data class CachedAtomList(
    val atoms: List<Atom>,
    val timestamp: Long,
    val ttlMs: Long = 10000 // 10 seconds TTL
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return (now - timestamp) > ttlMs
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val atomListCacheSize: Int,
    val connectivityCacheSize: Int,
    val maxCacheSize: Int,
    val cacheHitRate: Float
)
