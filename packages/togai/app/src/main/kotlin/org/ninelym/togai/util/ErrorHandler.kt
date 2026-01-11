package org.ninelym.togai.util

import android.util.Log
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utility for Togai
 * Provides consistent error handling, logging, and user-friendly error messages
 */
object ErrorHandler {
    
    private const val TAG = "TogaiErrorHandler"
    
    /**
     * Error types for categorization
     */
    enum class ErrorType {
        NETWORK,
        AUTHENTICATION,
        PERMISSION,
        DATA,
        UNKNOWN
    }
    
    /**
     * Error result wrapper
     */
    sealed class ErrorResult {
        data class Success<T>(val data: T) : ErrorResult()
        data class Error(
            val type: ErrorType,
            val message: String,
            val exception: Throwable? = null
        ) : ErrorResult()
    }
    
    /**
     * Handle exceptions and convert to user-friendly messages
     */
    fun handleException(exception: Throwable): ErrorResult.Error {
        // Don't handle cancellation exceptions
        if (exception is CancellationException) {
            throw exception
        }
        
        Log.e(TAG, "Exception occurred", exception)
        
        return when (exception) {
            is UnknownHostException -> ErrorResult.Error(
                type = ErrorType.NETWORK,
                message = "No internet connection. Please check your network settings.",
                exception = exception
            )
            is SocketTimeoutException -> ErrorResult.Error(
                type = ErrorType.NETWORK,
                message = "Connection timed out. Please try again.",
                exception = exception
            )
            is IOException -> ErrorResult.Error(
                type = ErrorType.NETWORK,
                message = "Network error occurred. Please try again.",
                exception = exception
            )
            is SecurityException -> ErrorResult.Error(
                type = ErrorType.PERMISSION,
                message = "Permission denied. Please grant necessary permissions.",
                exception = exception
            )
            is IllegalArgumentException -> ErrorResult.Error(
                type = ErrorType.DATA,
                message = "Invalid data provided. Please check your input.",
                exception = exception
            )
            is IllegalStateException -> ErrorResult.Error(
                type = ErrorType.DATA,
                message = "Invalid state. Please try again.",
                exception = exception
            )
            else -> ErrorResult.Error(
                type = ErrorType.UNKNOWN,
                message = "An unexpected error occurred. Please try again.",
                exception = exception
            )
        }
    }
    
    /**
     * Safe execution wrapper for suspend functions
     */
    suspend fun <T> safeExecute(
        block: suspend () -> T
    ): ErrorResult {
        return try {
            val result = block()
            ErrorResult.Success(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    /**
     * Safe execution wrapper for regular functions
     */
    fun <T> safeExecuteSync(
        block: () -> T
    ): ErrorResult {
        return try {
            val result = block()
            ErrorResult.Success(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    /**
     * Log error with context
     */
    fun logError(
        tag: String,
        message: String,
        exception: Throwable? = null
    ) {
        if (exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log warning with context
     */
    fun logWarning(
        tag: String,
        message: String,
        exception: Throwable? = null
    ) {
        if (exception != null) {
            Log.w(tag, message, exception)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log info with context
     */
    fun logInfo(
        tag: String,
        message: String
    ) {
        Log.i(tag, message)
    }
    
    /**
     * Log debug with context
     */
    fun logDebug(
        tag: String,
        message: String
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
}

/**
 * Extension functions for easier error handling
 */

/**
 * Execute a suspend block with error handling
 */
suspend fun <T> withErrorHandling(
    block: suspend () -> T
): ErrorHandler.ErrorResult {
    return ErrorHandler.safeExecute(block)
}

/**
 * Execute a regular block with error handling
 */
fun <T> withErrorHandlingSync(
    block: () -> T
): ErrorHandler.ErrorResult {
    return ErrorHandler.safeExecuteSync(block)
}

/**
 * Extension to unwrap ErrorResult or throw
 */
fun <T> ErrorHandler.ErrorResult.getOrThrow(): T {
    return when (this) {
        is ErrorHandler.ErrorResult.Success<*> -> this.data as T
        is ErrorHandler.ErrorResult.Error -> throw this.exception 
            ?: RuntimeException(this.message)
    }
}

/**
 * Extension to unwrap ErrorResult or return null
 */
fun <T> ErrorHandler.ErrorResult.getOrNull(): T? {
    return when (this) {
        is ErrorHandler.ErrorResult.Success<*> -> this.data as T
        is ErrorHandler.ErrorResult.Error -> null
    }
}

/**
 * Extension to unwrap ErrorResult or return default value
 */
fun <T> ErrorHandler.ErrorResult.getOrDefault(default: T): T {
    return when (this) {
        is ErrorHandler.ErrorResult.Success<*> -> this.data as T
        is ErrorHandler.ErrorResult.Error -> default
    }
}
