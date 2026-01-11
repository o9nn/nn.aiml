package org.ninelym.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Vector Database (LVDB equivalent)
 *
 * Local vector database for storing and querying embeddings.
 * Supports similarity search, metadata filtering, and persistence.
 */

/**
 * Embedding data class
 */
data class Embedding(
    val id: String,
    val vector: FloatArray,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Embedding) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Result from similarity search
 */
data class SimilarityResult(
    val embedding: Embedding,
    val similarity: Float,
    val distance: Float
)

/**
 * Vector database namespace
 */
class VectorNamespace(val name: String) {
    private val embeddings = ConcurrentHashMap<String, Embedding>()
    private val mutex = Mutex()

    /**
     * Add embedding to namespace
     */
    suspend fun addEmbedding(embedding: Embedding) = mutex.withLock {
        embeddings[embedding.id] = embedding
    }

    /**
     * Add multiple embeddings
     */
    suspend fun addEmbeddings(embeddingsList: List<Embedding>) = mutex.withLock {
        embeddingsList.forEach { embeddings[it.id] = it }
    }

    /**
     * Get embedding by ID
     */
    fun getEmbedding(id: String): Embedding? = embeddings[id]

    /**
     * Check if embedding exists
     */
    fun exists(id: String): Boolean = embeddings.containsKey(id)

    /**
     * Delete embedding
     */
    suspend fun deleteEmbedding(id: String) = mutex.withLock {
        embeddings.remove(id)
    }

    /**
     * Delete embeddings by metadata
     */
    suspend fun deleteByMetadata(key: String, value: String) = mutex.withLock {
        val toDelete = embeddings.values.filter {
            it.metadata[key] == value
        }.map { it.id }
        toDelete.forEach { embeddings.remove(it) }
    }

    /**
     * Query top N similar embeddings
     */
    fun queryTopN(
        queryVector: FloatArray,
        n: Int,
        filter: ((Embedding) -> Boolean)? = null
    ): List<SimilarityResult> {
        val candidates = if (filter != null) {
            embeddings.values.filter(filter)
        } else {
            embeddings.values.toList()
        }

        return candidates
            .map { embedding ->
                val similarity = cosineSimilarity(queryVector, embedding.vector)
                val distance = 1f - similarity
                SimilarityResult(embedding, similarity, distance)
            }
            .sortedByDescending { it.similarity }
            .take(n)
    }

    /**
     * Get all embeddings
     */
    fun getAllEmbeddings(): List<Embedding> = embeddings.values.toList()

    /**
     * Get embedding count
     */
    fun count(): Int = embeddings.size

    /**
     * Clear all embeddings
     */
    suspend fun clear() = mutex.withLock {
        embeddings.clear()
    }

    /**
     * Serialize to bytes
     */
    fun serialize(): ByteArray {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { oos ->
            oos.writeInt(embeddings.size)
            embeddings.values.forEach { embedding ->
                oos.writeUTF(embedding.id)
                oos.writeInt(embedding.vector.size)
                embedding.vector.forEach { oos.writeFloat(it) }
                oos.writeInt(embedding.metadata.size)
                embedding.metadata.forEach { (k, v) ->
                    oos.writeUTF(k)
                    oos.writeUTF(v)
                }
                oos.writeLong(embedding.timestamp)
            }
        }
        return baos.toByteArray()
    }

    /**
     * Deserialize from bytes
     */
    suspend fun deserialize(data: ByteArray) = mutex.withLock {
        embeddings.clear()
        ByteArrayInputStream(data).use { bais ->
            ObjectInputStream(bais).use { ois ->
                val count = ois.readInt()
                repeat(count) {
                    val id = ois.readUTF()
                    val vectorSize = ois.readInt()
                    val vector = FloatArray(vectorSize) { ois.readFloat() }
                    val metadataSize = ois.readInt()
                    val metadata = mutableMapOf<String, String>()
                    repeat(metadataSize) {
                        val k = ois.readUTF()
                        val v = ois.readUTF()
                        metadata[k] = v
                    }
                    val timestamp = ois.readLong()
                    embeddings[id] = Embedding(id, vector, metadata, timestamp)
                }
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }
}

/**
 * Main Vector Database class
 */
class VectorDatabase(private val basePath: String) {

    private val namespaces = ConcurrentHashMap<String, VectorNamespace>()
    private val mutex = Mutex()

    /**
     * Get or create namespace
     */
    fun getNamespace(name: String): VectorNamespace {
        return namespaces.getOrPut(name) { VectorNamespace(name) }
    }

    /**
     * List all namespaces
     */
    fun listNamespaces(): List<String> = namespaces.keys().toList()

    /**
     * Delete namespace
     */
    suspend fun deleteNamespace(name: String) = mutex.withLock {
        namespaces.remove(name)
        val file = File(basePath, "$name.lvdb")
        if (file.exists()) file.delete()
    }

    // Convenience methods that delegate to namespaces

    /**
     * Save embedding
     */
    suspend fun saveEmbedding(
        namespace: String,
        id: String,
        vector: FloatArray,
        metadata: Map<String, String> = emptyMap()
    ) {
        val embedding = Embedding(id, vector, metadata)
        getNamespace(namespace).addEmbedding(embedding)
    }

    /**
     * Query top N similar
     */
    fun queryTopNSimilar(
        namespace: String,
        queryVector: FloatArray,
        n: Int,
        filter: ((Embedding) -> Boolean)? = null
    ): List<SimilarityResult> {
        return getNamespace(namespace).queryTopN(queryVector, n, filter)
    }

    /**
     * Check if exists
     */
    fun exists(namespace: String, id: String): Boolean {
        return getNamespace(namespace).exists(id)
    }

    /**
     * Delete embeddings by metadata
     */
    suspend fun deleteByMetadata(namespace: String, key: String, value: String) {
        getNamespace(namespace).deleteByMetadata(key, value)
    }

    /**
     * Save namespace to disk
     */
    suspend fun saveToDisk(namespace: String) = withContext(Dispatchers.IO) {
        val ns = namespaces[namespace] ?: return@withContext
        val dir = File(basePath)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$namespace.lvdb")
        file.writeBytes(ns.serialize())
    }

    /**
     * Load namespace from disk
     */
    suspend fun loadFromDisk(namespace: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(basePath, "$namespace.lvdb")
        if (!file.exists()) return@withContext false

        try {
            val ns = getNamespace(namespace)
            ns.deserialize(file.readBytes())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Save all namespaces to disk
     */
    suspend fun saveAll() {
        namespaces.keys.forEach { saveToDisk(it) }
    }

    /**
     * Get total embedding count across all namespaces
     */
    fun totalCount(): Int = namespaces.values.sumOf { it.count() }

    /**
     * Clear all data
     */
    suspend fun clearAll() = mutex.withLock {
        namespaces.values.forEach { it.clear() }
        namespaces.clear()
    }
}

/**
 * JNI interface for native LVDB (optional optimization)
 */
object LvdbJni {

    private var isLoaded = false

    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("lvdb-jni")
                isLoaded = true
            }
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun isLibraryLoaded(): Boolean = isLoaded

    // Native methods (optional - for when native lib is available)
    @JvmStatic
    external fun lvdb_save_embeddings(
        dbPath: String,
        namespace: String,
        vector: FloatArray,
        metadata: String
    )

    @JvmStatic
    external fun lvdb_query_top_n_similar(
        dbPath: String,
        namespace: String,
        queryVector: FloatArray,
        n: Int
    ): Array<Any> // Returns array of [id, similarity, metadata]

    @JvmStatic
    external fun lvdb_delete_embeddings_by_metadata(
        dbPath: String,
        namespace: String,
        metadata: String
    )

    @JvmStatic
    external fun lvdb_exists(
        dbPath: String,
        namespace: String,
        key: String
    ): Boolean

    @JvmStatic
    external fun lvdb_save_to_disk(
        dbPath: String,
        namespace: String
    )
}

/**
 * Factory for creating VectorDatabase with optional native acceleration
 */
object VectorDatabaseFactory {

    /**
     * Create a VectorDatabase instance
     * @param basePath Base path for persistence
     * @param useNative Attempt to use native implementation
     */
    fun create(basePath: String, useNative: Boolean = true): VectorDatabase {
        if (useNative) {
            LvdbJni.loadLibrary() // Try to load, ignore failure
        }
        return VectorDatabase(basePath)
    }
}
