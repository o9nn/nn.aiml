"""
Performance Optimization Utilities for Toga Personality System

This module provides performance optimization utilities including caching,
memoization, and performance monitoring for the Toga personality system.
"""

import sys
from functools import lru_cache, wraps
from time import time
from typing import Any, Callable, Dict, Optional, TypeVar, cast

T = TypeVar("T")


class PerformanceMonitor:
    """Monitor and track performance metrics for Toga operations."""

    def __init__(self):
        """Initialize performance monitor."""
        self.metrics: Dict[str, Dict[str, Any]] = {}

    def record_timing(self, operation: str, duration_ms: float) -> None:
        """
        Record timing for an operation.

        Args:
            operation: Name of the operation
            duration_ms: Duration in milliseconds
        """
        if operation not in self.metrics:
            self.metrics[operation] = {
                "count": 0,
                "total_ms": 0.0,
                "min_ms": float("inf"),
                "max_ms": 0.0,
                "avg_ms": 0.0,
            }

        metrics = self.metrics[operation]
        metrics["count"] += 1
        metrics["total_ms"] += duration_ms
        metrics["min_ms"] = min(metrics["min_ms"], duration_ms)
        metrics["max_ms"] = max(metrics["max_ms"], duration_ms)
        metrics["avg_ms"] = metrics["total_ms"] / metrics["count"]

    def get_metrics(self, operation: Optional[str] = None) -> Dict[str, Any]:
        """
        Get performance metrics.

        Args:
            operation: Optional specific operation to get metrics for

        Returns:
            Dictionary of metrics
        """
        if operation:
            return self.metrics.get(operation, {})
        return self.metrics

    def reset(self) -> None:
        """Reset all metrics."""
        self.metrics.clear()

    def get_summary(self) -> str:
        """
        Get a formatted summary of all metrics.

        Returns:
            Formatted string summary
        """
        if not self.metrics:
            return "No performance metrics recorded."

        lines = ["Performance Metrics Summary:", "=" * 60]

        for operation, metrics in self.metrics.items():
            lines.append(f"\nOperation: {operation}")
            lines.append(f"  Count: {metrics['count']}")
            lines.append(f"  Average: {metrics['avg_ms']:.2f}ms")
            lines.append(f"  Min: {metrics['min_ms']:.2f}ms")
            lines.append(f"  Max: {metrics['max_ms']:.2f}ms")
            lines.append(f"  Total: {metrics['total_ms']:.2f}ms")

        return "\n".join(lines)


# Global performance monitor instance
_global_monitor = PerformanceMonitor()


def get_performance_monitor() -> PerformanceMonitor:
    """Get the global performance monitor instance."""
    return _global_monitor


def timed(operation_name: Optional[str] = None) -> Callable[[Callable[..., T]], Callable[..., T]]:
    """
    Decorator to time function execution and record metrics.

    Args:
        operation_name: Optional custom name for the operation

    Returns:
        Decorated function

    Example:
        @timed("process_message")
        def process_message(msg: str) -> str:
            return msg.upper()
    """

    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        op_name = operation_name or func.__name__

        @wraps(func)
        def wrapper(*args: Any, **kwargs: Any) -> T:
            start_time = time()
            try:
                result = func(*args, **kwargs)
                return result
            finally:
                duration_ms = (time() - start_time) * 1000
                _global_monitor.record_timing(op_name, duration_ms)

        return wrapper

    return decorator


class TraitCache:
    """
    Cache for personality trait calculations.

    Provides efficient caching for frequently accessed trait values
    and calculations to improve performance.
    """

    def __init__(self, max_size: int = 128):
        """
        Initialize trait cache.

        Args:
            max_size: Maximum number of cached items
        """
        self.max_size = max_size
        self._cache: Dict[str, Any] = {}
        self._access_count: Dict[str, int] = {}

    def get(self, key: str) -> Optional[Any]:
        """
        Get value from cache.

        Args:
            key: Cache key

        Returns:
            Cached value or None if not found
        """
        if key in self._cache:
            self._access_count[key] = self._access_count.get(key, 0) + 1
            return self._cache[key]
        return None

    def set(self, key: str, value: Any) -> None:
        """
        Set value in cache.

        Args:
            key: Cache key
            value: Value to cache
        """
        if len(self._cache) >= self.max_size:
            # Remove least frequently accessed item
            lfu_key = min(self._access_count, key=self._access_count.get)  # type: ignore
            del self._cache[lfu_key]
            del self._access_count[lfu_key]

        self._cache[key] = value
        self._access_count[key] = 0

    def clear(self) -> None:
        """Clear the cache."""
        self._cache.clear()
        self._access_count.clear()

    def size(self) -> int:
        """Get current cache size."""
        return len(self._cache)


@lru_cache(maxsize=256)
def cached_trait_calculation(
    trait_name: str,
    base_value: float,
    modifier: float,
    min_val: float = 0.0,
    max_val: float = 1.0,
) -> float:
    """
    Cached trait value calculation.

    Args:
        trait_name: Name of the trait
        base_value: Base trait value
        modifier: Modification amount
        min_val: Minimum allowed value
        max_val: Maximum allowed value

    Returns:
        Calculated trait value
    """
    result = base_value + modifier
    return max(min_val, min(max_val, result))


def get_memory_usage() -> float:
    """
    Get current memory usage in MB.

    Returns:
        Memory usage in megabytes
    """
    if sys.platform == "linux":
        try:
            with open("/proc/self/status") as f:
                for line in f:
                    if line.startswith("VmRSS:"):
                        # Extract memory in kB and convert to MB
                        mem_kb = int(line.split()[1])
                        return mem_kb / 1024.0
        except (IOError, ValueError):
            pass

    # Fallback: use sys.getsizeof for rough estimate
    return sys.getsizeof({}) / (1024 * 1024)


class BatchProcessor:
    """
    Batch processor for efficient bulk operations.

    Processes items in batches to optimize performance and memory usage.
    """

    def __init__(self, batch_size: int = 10):
        """
        Initialize batch processor.

        Args:
            batch_size: Number of items to process per batch
        """
        self.batch_size = batch_size

    @timed("batch_process")
    def process_batch(self, items: list[Any], processor: Callable[[Any], Any]) -> list[Any]:
        """
        Process items in batches.

        Args:
            items: List of items to process
            processor: Function to process each item

        Returns:
            List of processed items
        """
        results = []

        for i in range(0, len(items), self.batch_size):
            batch = items[i : i + self.batch_size]
            batch_results = [processor(item) for item in batch]
            results.extend(batch_results)

        return results


def optimize_string_operations(text: str) -> str:
    """
    Optimize common string operations.

    Args:
        text: Input text

    Returns:
        Optimized text
    """
    # Use join instead of repeated concatenation
    # Use list comprehension for filtering
    # Pre-compile regex patterns if needed

    # Example: efficient whitespace normalization
    return " ".join(text.split())


class LazyEvaluator:
    """
    Lazy evaluator for deferred computation.

    Delays expensive computations until they're actually needed.
    """

    def __init__(self, compute_func: Callable[[], T]):
        """
        Initialize lazy evaluator.

        Args:
            compute_func: Function to compute the value
        """
        self._compute_func = compute_func
        self._value: Optional[T] = None
        self._computed = False

    @property
    def value(self) -> T:
        """Get the computed value (computing if necessary)."""
        if not self._computed:
            self._value = self._compute_func()
            self._computed = True
        return cast(T, self._value)

    def reset(self) -> None:
        """Reset the lazy evaluator."""
        self._value = None
        self._computed = False


# Pre-compiled regex patterns for common operations
CUTE_PATTERN_CACHE: Dict[str, Any] = {}


def get_or_compile_pattern(pattern: str) -> Any:
    """
    Get or compile regex pattern with caching.

    Args:
        pattern: Regex pattern string

    Returns:
        Compiled regex pattern
    """
    import re

    if pattern not in CUTE_PATTERN_CACHE:
        CUTE_PATTERN_CACHE[pattern] = re.compile(pattern, re.IGNORECASE)
    return CUTE_PATTERN_CACHE[pattern]
