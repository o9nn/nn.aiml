package org.ninelym.android

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Togai Android Components
 *
 * Platform-agnostic definitions for Android components.
 * These are interfaces/abstractions that can be implemented
 * by actual Android Activities/Services.
 */

// ============================================================================
// Activity Abstractions
// ============================================================================

/**
 * Main Activity interface
 */
interface TogaiMainActivity {
    fun getMainComponentName(): String
    fun onCreate(savedInstanceState: Map<String, Any>?)
    fun onResume()
    fun onPause()
    fun onDestroy()
    fun onNewIntent(intentData: Map<String, Any>?)
    fun isTablet(): Boolean
}

/**
 * Text Processing Activity interface (RememberActivity/ChatActivity)
 */
interface TextProcessingActivity {
    val activityType: TextActivityType

    suspend fun processIntent(
        selectedText: String,
        sourceApp: String?,
        mimeType: String
    ): TextProcessingResult

    fun getActivityLabel(): String
}

enum class TextActivityType {
    REMEMBER,  // "Layla, remember this!"
    CHAT       // "Chat with Layla"
}

sealed class TextProcessingResult {
    data class Success(val message: String, val data: Map<String, Any> = emptyMap()) : TextProcessingResult()
    data class Error(val message: String) : TextProcessingResult()
}

/**
 * Tasker Configuration Activity interface
 */
interface TaskerConfigActivity {
    val configType: TaskerConfigType

    fun setupUI()
    fun onConfigSaved(config: Map<String, Any>)
    fun onConfigCancelled()
    fun getResultBundle(): Map<String, Any>
}

enum class TaskerConfigType {
    NEW_INFER_TASK,
    INFER_IN_BACKGROUND,
    TASK_COMPLETE_EVENT,
    BACKGROUND_WORK
}

// ============================================================================
// Service Abstractions
// ============================================================================

/**
 * Service state
 */
enum class ServiceState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING
}

/**
 * Foreground service type
 */
enum class ForegroundServiceType {
    MEDIA_PLAYBACK,
    DATA_SYNC,
    LOCATION,
    SPECIAL_USE
}

/**
 * Base Service interface
 */
interface TogaiService {
    val serviceId: String
    val state: StateFlow<ServiceState>

    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    fun isRunning(): Boolean
}

/**
 * Inference Service interface
 */
interface InferenceService : TogaiService {
    val notificationChannelId: String
    val foregroundServiceType: ForegroundServiceType

    suspend fun loadModel(modelPath: String, config: Map<String, Any>): Result<Unit>
    suspend fun infer(prompt: String, params: Map<String, Any>, onToken: ((String) -> Unit)?): Result<String>
    suspend fun unloadModel(): Result<Unit>

    fun showFloatingWidget()
    fun hideFloatingWidget()
    fun updateNotification(title: String, message: String)
}

/**
 * Task Manager Service interface
 */
interface TaskManagerService : TogaiService {
    data class Task(
        val id: String,
        val type: String,
        val input: Map<String, Any>,
        val status: TaskStatus,
        val result: Any? = null
    )

    enum class TaskStatus {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    fun queueTask(type: String, input: Map<String, Any>): String
    fun cancelTask(taskId: String): Boolean
    fun getTaskStatus(taskId: String): TaskStatus?
    fun getQueuedTasks(): List<Task>

    fun showFloatingWidget()
    fun hideFloatingWidget()
}

/**
 * Headless JS Service interface (React Native background tasks)
 */
interface HeadlessJsService : TogaiService {
    fun executeTask(taskKey: String, data: Map<String, Any>)
    fun isTaskRunning(taskKey: String): Boolean
}

// ============================================================================
// Receiver Abstractions
// ============================================================================

/**
 * Broadcast Receiver interface
 */
interface TogaiBroadcastReceiver {
    val intentFilter: List<String>

    fun onReceive(action: String, extras: Map<String, Any>?)
}

/**
 * Stop Service Receiver
 */
interface StopServiceReceiver : TogaiBroadcastReceiver {
    fun stopService(serviceId: String)
}

// ============================================================================
// Implementation Stubs
// ============================================================================

/**
 * Base implementation for Inference Service
 */
abstract class BaseInferenceService : InferenceService {

    override val serviceId: String = "inference_service"
    override val notificationChannelId: String = "com.togai.channels.inference"
    override val foregroundServiceType: ForegroundServiceType = ForegroundServiceType.MEDIA_PLAYBACK

    protected val _state = MutableStateFlow(ServiceState.STOPPED)
    override val state: StateFlow<ServiceState> = _state

    protected val isModelLoaded = AtomicBoolean(false)
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun isRunning(): Boolean = _state.value == ServiceState.RUNNING

    override suspend fun start(): Result<Unit> {
        return try {
            _state.value = ServiceState.STARTING
            onServiceStart()
            _state.value = ServiceState.RUNNING
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = ServiceState.STOPPED
            Result.failure(e)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return try {
            _state.value = ServiceState.STOPPING
            onServiceStop()
            scope.cancel()
            _state.value = ServiceState.STOPPED
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    protected abstract suspend fun onServiceStart()
    protected abstract suspend fun onServiceStop()
}

/**
 * Base implementation for Task Manager Service
 */
abstract class BaseTaskManagerService : TaskManagerService {

    override val serviceId: String = "task_manager_service"

    protected val _state = MutableStateFlow(ServiceState.STOPPED)
    override val state: StateFlow<ServiceState> = _state

    protected val tasks = ConcurrentHashMap<String, TaskManagerService.Task>()
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var taskCounter = 0L

    override fun isRunning(): Boolean = _state.value == ServiceState.RUNNING

    override fun queueTask(type: String, input: Map<String, Any>): String {
        val taskId = "task_${++taskCounter}_${System.currentTimeMillis()}"
        val task = TaskManagerService.Task(
            id = taskId,
            type = type,
            input = input,
            status = TaskManagerService.TaskStatus.QUEUED
        )
        tasks[taskId] = task

        scope.launch {
            processTask(taskId)
        }

        return taskId
    }

    override fun cancelTask(taskId: String): Boolean {
        val task = tasks[taskId] ?: return false
        if (task.status == TaskManagerService.TaskStatus.RUNNING ||
            task.status == TaskManagerService.TaskStatus.QUEUED) {
            tasks[taskId] = task.copy(status = TaskManagerService.TaskStatus.CANCELLED)
            return true
        }
        return false
    }

    override fun getTaskStatus(taskId: String): TaskManagerService.TaskStatus? {
        return tasks[taskId]?.status
    }

    override fun getQueuedTasks(): List<TaskManagerService.Task> {
        return tasks.values.filter { it.status == TaskManagerService.TaskStatus.QUEUED }
    }

    protected abstract suspend fun processTask(taskId: String)
}

// ============================================================================
// Floating Widget
// ============================================================================

/**
 * Floating Widget interface
 */
interface FloatingWidget {
    data class Position(val x: Int, val y: Int)
    data class Size(val width: Int, val height: Int)

    fun show()
    fun hide()
    fun updatePosition(position: Position)
    fun updateContent(content: WidgetContent)
    fun setOnClickListener(listener: () -> Unit)
    fun setOnDragListener(listener: (Position) -> Unit)
}

/**
 * Widget content
 */
sealed class WidgetContent {
    data class Text(val text: String) : WidgetContent()
    data class Progress(val progress: Float, val message: String) : WidgetContent()
    data class Badge(val count: Int) : WidgetContent()
    object Loading : WidgetContent()
}

/**
 * Base Floating Widget implementation
 */
abstract class BaseFloatingWidget : FloatingWidget {

    protected var isVisible = false
    protected var currentPosition = FloatingWidget.Position(0, 0)
    protected var currentContent: WidgetContent = WidgetContent.Loading

    protected var onClick: (() -> Unit)? = null
    protected var onDrag: ((FloatingWidget.Position) -> Unit)? = null

    override fun show() {
        isVisible = true
        onShow()
    }

    override fun hide() {
        isVisible = false
        onHide()
    }

    override fun updatePosition(position: FloatingWidget.Position) {
        currentPosition = position
        onPositionChanged(position)
    }

    override fun updateContent(content: WidgetContent) {
        currentContent = content
        onContentChanged(content)
    }

    override fun setOnClickListener(listener: () -> Unit) {
        onClick = listener
    }

    override fun setOnDragListener(listener: (FloatingWidget.Position) -> Unit) {
        onDrag = listener
    }

    protected abstract fun onShow()
    protected abstract fun onHide()
    protected abstract fun onPositionChanged(position: FloatingWidget.Position)
    protected abstract fun onContentChanged(content: WidgetContent)
}

// ============================================================================
// Application
// ============================================================================

/**
 * Main Application interface
 */
interface TogaiApplication {
    fun onCreate()
    fun onTerminate()

    fun getInferenceService(): InferenceService?
    fun getTaskManagerService(): TaskManagerService?

    fun registerService(serviceId: String, service: TogaiService)
    fun unregisterService(serviceId: String)
}

/**
 * Application lifecycle listener
 */
interface AppLifecycleListener {
    fun onAppForegrounded()
    fun onAppBackgrounded()
}
