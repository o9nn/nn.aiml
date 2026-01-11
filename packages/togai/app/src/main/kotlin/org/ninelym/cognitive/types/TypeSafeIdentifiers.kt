package org.ninelym.cognitive.types

/**
 * Type-safe identifiers for cognitive system components
 * 
 * Using inline value classes to provide type safety without runtime overhead.
 * Prevents mixing different types of IDs and catches errors at compile time.
 */

/**
 * Type-safe identifier for atoms in the hypergraph
 */
@JvmInline
value class AtomId(val value: String) {
    init {
        require(value.isNotBlank()) { "AtomId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(prefix: String = "atom"): AtomId {
            return AtomId("${prefix}_${System.currentTimeMillis()}_${(0..9999).random()}")
        }
    }
}

/**
 * Type-safe identifier for tasks in the ECAN scheduler
 */
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(): TaskId {
            return TaskId("task_${System.currentTimeMillis()}_${(0..9999).random()}")
        }
    }
}

/**
 * Type-safe identifier for hyperlinks
 */
@JvmInline
value class LinkId(val value: String) {
    init {
        require(value.isNotBlank()) { "LinkId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(linkType: String = "link"): LinkId {
            return LinkId("${linkType}_${System.currentTimeMillis()}_${(0..9999).random()}")
        }
    }
}

/**
 * Type-safe identifier for cognitive agents
 */
@JvmInline
value class AgentId(val value: String) {
    init {
        require(value.isNotBlank()) { "AgentId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(agentName: String): AgentId {
            return AgentId("agent_${agentName}_${System.currentTimeMillis()}")
        }
    }
}

/**
 * Type-safe identifier for conversation sessions
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "SessionId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(): SessionId {
            return SessionId("session_${System.currentTimeMillis()}_${(0..9999).random()}")
        }
    }
}

/**
 * Type-safe identifier for cognitive clusters
 */
@JvmInline
value class ClusterId(val value: String) {
    init {
        require(value.isNotBlank()) { "ClusterId cannot be blank" }
    }
    
    override fun toString(): String = value
    
    companion object {
        fun generate(baseAtomId: String): ClusterId {
            return ClusterId("cluster_${baseAtomId}_${System.currentTimeMillis()}")
        }
    }
}

/**
 * Result type for operations that can succeed or fail
 */
sealed interface OperationResult<out T> {
    data class Success<T>(val value: T) : OperationResult<T>
    data class Failure(val error: OperationError) : OperationResult<Nothing>
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw OperationException(error)
    }
    
    inline fun <R> map(transform: (T) -> R): OperationResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): OperationResult<T> {
        if (this is Success) action(value)
        return this
    }
    
    inline fun onFailure(action: (OperationError) -> Unit): OperationResult<T> {
        if (this is Failure) action(error)
        return this
    }
}

/**
 * Structured error information for operations
 */
data class OperationError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap()
) {
    override fun toString(): String {
        return "OperationError(code=$code, message='$message', context=$context)"
    }
}

/**
 * Error codes for cognitive operations
 */
enum class ErrorCode {
    // Hypergraph errors
    ATOM_NOT_FOUND,
    ATOM_ALREADY_EXISTS,
    INVALID_ATOM_TYPE,
    LINK_CREATION_FAILED,
    
    // ECAN errors
    INSUFFICIENT_RESOURCES,
    TASK_EXECUTION_FAILED,
    INVALID_PRIORITY,
    SCHEDULER_OVERLOAD,
    
    // Tensor errors
    INVALID_TENSOR_DIMENSIONS,
    TENSOR_COMPUTATION_FAILED,
    
    // Integration errors
    VERIFICATION_FAILED,
    CONSISTENCY_CHECK_FAILED,
    STATE_CAPTURE_FAILED,
    
    // General errors
    INVALID_INPUT,
    INTERNAL_ERROR,
    TIMEOUT,
    UNKNOWN_ERROR
}

/**
 * Exception for operation failures
 */
class OperationException(val error: OperationError) : Exception(error.message, error.cause) {
    override fun toString(): String = "OperationException: $error"
}

/**
 * Extension functions for working with results
 */
fun <T> T.asSuccess(): OperationResult<T> = OperationResult.Success(this)

fun OperationError.asFailure(): OperationResult<Nothing> = OperationResult.Failure(this)

fun <T> Result<T>.toOperationResult(): OperationResult<T> = fold(
    onSuccess = { it.asSuccess() },
    onFailure = { 
        OperationError(
            code = ErrorCode.INTERNAL_ERROR,
            message = it.message ?: "Unknown error",
            cause = it
        ).asFailure()
    }
)
