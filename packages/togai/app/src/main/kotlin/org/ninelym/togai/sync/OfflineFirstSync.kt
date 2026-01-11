package org.ninelym.togai.sync

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ninelym.togai.util.ErrorHandler
import java.util.concurrent.TimeUnit

/**
 * Offline-first data synchronization system for Togai
 * Ensures data availability offline and syncs when online
 */
class OfflineFirstSync private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "OfflineFirstSync"
        private const val SYNC_WORK_NAME = "togai_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L
        
        @Volatile
        private var instance: OfflineFirstSync? = null
        
        fun getInstance(context: Context): OfflineFirstSync {
            return instance ?: synchronized(this) {
                instance ?: OfflineFirstSync(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * Sync operation types
     */
    enum class SyncOperation {
        CREATE,
        UPDATE,
        DELETE
    }
    
    /**
     * Sync status
     */
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        ERROR
    }
    
    /**
     * Pending sync item
     */
    @Serializable
    data class PendingSyncItem(
        val id: String,
        val entityType: String,
        val operation: String, // SyncOperation as string
        val data: String, // JSON data
        val timestamp: Long,
        val retryCount: Int = 0
    )
    
    /**
     * Sync state
     */
    data class SyncState(
        val status: SyncStatus = SyncStatus.IDLE,
        val pendingCount: Int = 0,
        val lastSyncTime: Long = 0,
        val error: String? = null
    )
    
    // Sync state flow
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Pending operations queue
    private val pendingOperations = mutableListOf<PendingSyncItem>()
    
    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // WorkManager instance
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Initialize sync system
     */
    fun initialize() {
        // Load pending operations
        loadPendingOperations()
        
        // Schedule periodic sync
        schedulePeriodicSync()
        
        ErrorHandler.logInfo(TAG, "OfflineFirstSync initialized with ${pendingOperations.size} pending operations")
    }
    
    /**
     * Queue a sync operation
     */
    suspend fun queueOperation(
        entityType: String,
        operation: SyncOperation,
        data: Any,
        id: String = generateId()
    ) = withContext(Dispatchers.IO) {
        val item = PendingSyncItem(
            id = id,
            entityType = entityType,
            operation = operation.name,
            data = json.encodeToString(data),
            timestamp = System.currentTimeMillis()
        )
        
        pendingOperations.add(item)
        savePendingOperations()
        
        updateSyncState(pendingCount = pendingOperations.size)
        
        ErrorHandler.logDebug(TAG, "Queued $operation operation for $entityType")
        
        // Trigger immediate sync if online
        if (isOnline()) {
            triggerSync()
        }
    }
    
    /**
     * Perform sync operation
     */
    suspend fun sync() = withContext(Dispatchers.IO) {
        if (_syncState.value.status == SyncStatus.SYNCING) {
            ErrorHandler.logDebug(TAG, "Sync already in progress")
            return@withContext
        }
        
        if (!isOnline()) {
            ErrorHandler.logDebug(TAG, "Device is offline, skipping sync")
            return@withContext
        }
        
        updateSyncState(status = SyncStatus.SYNCING)
        
        try {
            val operations = pendingOperations.toList()
            var successCount = 0
            var failureCount = 0
            
            operations.forEach { item ->
                try {
                    // Perform sync operation
                    performSyncOperation(item)
                    
                    // Remove from pending queue
                    pendingOperations.remove(item)
                    successCount++
                    
                } catch (e: Exception) {
                    ErrorHandler.logError(TAG, "Failed to sync item ${item.id}", e)
                    
                    // Increment retry count
                    val updatedItem = item.copy(retryCount = item.retryCount + 1)
                    val index = pendingOperations.indexOf(item)
                    if (index >= 0) {
                        pendingOperations[index] = updatedItem
                    }
                    
                    // Remove if max retries reached
                    if (updatedItem.retryCount >= 3) {
                        pendingOperations.remove(item)
                        ErrorHandler.logError(TAG, "Max retries reached for item ${item.id}")
                    }
                    
                    failureCount++
                }
            }
            
            savePendingOperations()
            
            updateSyncState(
                status = SyncStatus.SUCCESS,
                pendingCount = pendingOperations.size,
                lastSyncTime = System.currentTimeMillis()
            )
            
            ErrorHandler.logInfo(TAG, "Sync completed: $successCount succeeded, $failureCount failed")
            
        } catch (e: Exception) {
            ErrorHandler.logError(TAG, "Sync failed", e)
            updateSyncState(
                status = SyncStatus.ERROR,
                error = e.message
            )
        }
    }
    
    /**
     * Trigger immediate sync
     */
    fun triggerSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * Schedule periodic sync
     */
    private fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        
        ErrorHandler.logInfo(TAG, "Scheduled periodic sync every $SYNC_INTERVAL_MINUTES minutes")
    }
    
    /**
     * Perform individual sync operation
     */
    private suspend fun performSyncOperation(item: PendingSyncItem) {
        // TODO: Implement actual sync logic with backend API
        // This is a placeholder for the actual implementation
        
        ErrorHandler.logDebug(TAG, "Performing ${item.operation} for ${item.entityType}")
        
        // Simulate network delay
        kotlinx.coroutines.delay(100)
        
        // In production, this would call the appropriate API endpoint
        // based on entityType and operation
    }
    
    /**
     * Check if device is online
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Load pending operations from storage
     */
    private fun loadPendingOperations() {
        // TODO: Load from persistent storage (Room database or MMKV)
        // For now, operations are kept in memory
    }
    
    /**
     * Save pending operations to storage
     */
    private fun savePendingOperations() {
        // TODO: Save to persistent storage (Room database or MMKV)
        // For now, operations are kept in memory
    }
    
    /**
     * Update sync state
     */
    private fun updateSyncState(
        status: SyncStatus? = null,
        pendingCount: Int? = null,
        lastSyncTime: Long? = null,
        error: String? = null
    ) {
        _syncState.value = _syncState.value.copy(
            status = status ?: _syncState.value.status,
            pendingCount = pendingCount ?: _syncState.value.pendingCount,
            lastSyncTime = lastSyncTime ?: _syncState.value.lastSyncTime,
            error = error
        )
    }
    
    /**
     * Generate unique ID
     */
    private fun generateId(): String {
        return "${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

/**
 * Background sync worker
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val syncManager = OfflineFirstSync.getInstance(applicationContext)
            syncManager.sync()
            Result.success()
        } catch (e: Exception) {
            ErrorHandler.logError("SyncWorker", "Sync work failed", e)
            Result.retry()
        }
    }
}

/**
 * Extension functions for easier sync usage
 */

/**
 * Queue create operation
 */
suspend fun OfflineFirstSync.create(entityType: String, data: Any, id: String? = null) {
    queueOperation(
        entityType = entityType,
        operation = OfflineFirstSync.SyncOperation.CREATE,
        data = data,
        id = id ?: "${System.currentTimeMillis()}_${(0..9999).random()}"
    )
}

/**
 * Queue update operation
 */
suspend fun OfflineFirstSync.update(entityType: String, id: String, data: Any) {
    queueOperation(
        entityType = entityType,
        operation = OfflineFirstSync.SyncOperation.UPDATE,
        data = data,
        id = id
    )
}

/**
 * Queue delete operation
 */
suspend fun OfflineFirstSync.delete(entityType: String, id: String) {
    queueOperation(
        entityType = entityType,
        operation = OfflineFirstSync.SyncOperation.DELETE,
        data = mapOf("id" to id),
        id = id
    )
}
