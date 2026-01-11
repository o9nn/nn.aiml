package org.ninelym.togai

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Test utilities for Togai project
 * Provides common testing helpers and extensions
 */

/**
 * Coroutine test rule that sets the main dispatcher to a test dispatcher
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Extension function to get LiveData value for testing
 * Usage: val value = liveData.getOrAwaitValue()
 */
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            data = value
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)
    
    try {
        afterObserve.invoke()
        
        if (!latch.await(time, timeUnit)) {
            this.removeObserver(observer)
            throw TimeoutException("LiveData value was never set.")
        }
    } finally {
        this.removeObserver(observer)
    }
    
    @Suppress("UNCHECKED_CAST")
    return data as T
}

/**
 * Test data builders for common entities
 */
object TestDataBuilder {
    
    fun createTestUser(
        id: String = "test_user_123",
        name: String = "Test User",
        email: String = "test@example.com"
    ) = mapOf(
        "id" to id,
        "name" to name,
        "email" to email
    )
    
    fun createTestMessage(
        id: String = "msg_123",
        content: String = "Test message",
        timestamp: Long = System.currentTimeMillis()
    ) = mapOf(
        "id" to id,
        "content" to content,
        "timestamp" to timestamp
    )
    
    fun createTestDocument(
        id: String = "doc_123",
        title: String = "Test Document",
        content: String = "Test content"
    ) = mapOf(
        "id" to id,
        "title" to title,
        "content" to content
    )
}

/**
 * Mock response builders for API testing
 */
object MockResponseBuilder {
    
    fun successResponse(data: Any? = null) = mapOf(
        "status" to "success",
        "data" to data
    )
    
    fun errorResponse(
        message: String = "Test error",
        code: Int = 400
    ) = mapOf(
        "status" to "error",
        "message" to message,
        "code" to code
    )
}

/**
 * Coroutine test helpers
 */
@ExperimentalCoroutinesApi
object CoroutineTestHelper {
    
    /**
     * Run a test with a test dispatcher
     */
    fun runTest(block: suspend TestScope.() -> Unit) {
        kotlinx.coroutines.test.runTest {
            block()
        }
    }
    
    /**
     * Advance time by specified milliseconds
     */
    suspend fun TestScope.advanceTimeBy(delayTimeMillis: Long) {
        testScheduler.advanceTimeBy(delayTimeMillis)
    }
    
    /**
     * Run all pending coroutines
     */
    suspend fun TestScope.runCurrent() {
        testScheduler.runCurrent()
    }
}

/**
 * Assertion helpers
 */
object AssertionHelper {
    
    /**
     * Assert that a block throws a specific exception
     */
    inline fun <reified T : Throwable> assertThrows(
        message: String? = null,
        block: () -> Unit
    ): T {
        try {
            block()
            throw AssertionError(
                message ?: "Expected ${T::class.simpleName} to be thrown"
            )
        } catch (e: Throwable) {
            if (e is T) {
                return e
            }
            throw AssertionError(
                message ?: "Expected ${T::class.simpleName} but got ${e::class.simpleName}",
                e
            )
        }
    }
    
    /**
     * Assert that a value is within a range
     */
    fun <T : Comparable<T>> assertInRange(
        value: T,
        min: T,
        max: T,
        message: String? = null
    ) {
        if (value < min || value > max) {
            throw AssertionError(
                message ?: "Expected $value to be between $min and $max"
            )
        }
    }
}

/**
 * File test helpers
 */
object FileTestHelper {
    
    /**
     * Create a temporary test file with content
     */
    fun createTempFile(
        prefix: String = "test",
        suffix: String = ".txt",
        content: String = ""
    ): java.io.File {
        return java.io.File.createTempFile(prefix, suffix).apply {
            writeText(content)
            deleteOnExit()
        }
    }
    
    /**
     * Create a temporary test directory
     */
    fun createTempDirectory(prefix: String = "test"): java.io.File {
        return java.nio.file.Files.createTempDirectory(prefix).toFile().apply {
            deleteOnExit()
        }
    }
}

/**
 * Extension functions for testing
 */

/**
 * Extension to check if a string contains all specified substrings
 */
fun String.containsAll(vararg substrings: String): Boolean {
    return substrings.all { this.contains(it) }
}

/**
 * Extension to check if a collection contains all specified elements
 */
fun <T> Collection<T>.containsAll(vararg elements: T): Boolean {
    return elements.all { this.contains(it) }
}

/**
 * Extension to safely get a value from a map with a default
 */
fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: V): V {
    return this[key] ?: defaultValue
}
