"""
Tests for Toga Performance Module
"""

import time
import unittest
from python.helpers.toga_performance import (
    PerformanceMonitor,
    get_performance_monitor,
    timed,
    TraitCache,
    cached_trait_calculation,
    get_memory_usage,
    BatchProcessor,
    optimize_string_operations,
    LazyEvaluator,
    get_or_compile_pattern,
)


class TestPerformanceMonitor(unittest.TestCase):
    """Test PerformanceMonitor class."""

    def test_initialization(self):
        """Test monitor initialization."""
        monitor = PerformanceMonitor()
        self.assertEqual(monitor.metrics, {})

    def test_record_timing_single(self):
        """Test recording a single timing."""
        monitor = PerformanceMonitor()
        monitor.record_timing("test_op", 100.0)

        metrics = monitor.get_metrics("test_op")
        self.assertEqual(metrics["count"], 1)
        self.assertEqual(metrics["total_ms"], 100.0)
        self.assertEqual(metrics["min_ms"], 100.0)
        self.assertEqual(metrics["max_ms"], 100.0)
        self.assertEqual(metrics["avg_ms"], 100.0)

    def test_record_timing_multiple(self):
        """Test recording multiple timings for same operation."""
        monitor = PerformanceMonitor()
        monitor.record_timing("test_op", 100.0)
        monitor.record_timing("test_op", 200.0)
        monitor.record_timing("test_op", 150.0)

        metrics = monitor.get_metrics("test_op")
        self.assertEqual(metrics["count"], 3)
        self.assertEqual(metrics["total_ms"], 450.0)
        self.assertEqual(metrics["min_ms"], 100.0)
        self.assertEqual(metrics["max_ms"], 200.0)
        self.assertEqual(metrics["avg_ms"], 150.0)

    def test_get_metrics_all(self):
        """Test getting all metrics."""
        monitor = PerformanceMonitor()
        monitor.record_timing("op1", 100.0)
        monitor.record_timing("op2", 200.0)

        all_metrics = monitor.get_metrics()
        self.assertEqual(len(all_metrics), 2)
        self.assertIn("op1", all_metrics)
        self.assertIn("op2", all_metrics)

    def test_reset(self):
        """Test resetting metrics."""
        monitor = PerformanceMonitor()
        monitor.record_timing("test_op", 100.0)
        self.assertEqual(len(monitor.metrics), 1)

        monitor.reset()
        self.assertEqual(len(monitor.metrics), 0)

    def test_get_summary(self):
        """Test summary generation."""
        monitor = PerformanceMonitor()
        monitor.record_timing("test_op", 100.0)

        summary = monitor.get_summary()
        self.assertIn("Performance Metrics Summary", summary)
        self.assertIn("test_op", summary)


class TestGlobalMonitor(unittest.TestCase):
    """Test global monitor functionality."""

    def test_get_global_monitor(self):
        """Test getting global monitor instance."""
        monitor1 = get_performance_monitor()
        monitor2 = get_performance_monitor()
        self.assertIs(monitor1, monitor2)


class TestTimedDecorator(unittest.TestCase):
    """Test timed decorator."""

    def test_timed_decorator_basic(self):
        """Test basic timed decorator functionality."""
        monitor = PerformanceMonitor()

        @timed("test_function")
        def test_func():
            time.sleep(0.01)
            return "result"

        import python.helpers.toga_performance as perf_module
        original_monitor = perf_module._global_monitor
        perf_module._global_monitor = monitor

        try:
            result = test_func()
            self.assertEqual(result, "result")
            metrics = monitor.get_metrics("test_function")
            self.assertEqual(metrics["count"], 1)
            self.assertGreaterEqual(metrics["avg_ms"], 10.0)
        finally:
            perf_module._global_monitor = original_monitor


class TestTraitCache(unittest.TestCase):
    """Test TraitCache class."""

    def test_initialization(self):
        """Test cache initialization."""
        cache = TraitCache(max_size=10)
        self.assertEqual(cache.max_size, 10)
        self.assertEqual(cache.size(), 0)

    def test_set_and_get(self):
        """Test setting and getting values."""
        cache = TraitCache()
        cache.set("key1", "value1")

        value = cache.get("key1")
        self.assertEqual(value, "value1")

    def test_get_nonexistent(self):
        """Test getting non-existent key."""
        cache = TraitCache()
        value = cache.get("nonexistent")
        self.assertIsNone(value)

    def test_clear(self):
        """Test clearing cache."""
        cache = TraitCache()
        cache.set("key1", "value1")
        self.assertEqual(cache.size(), 1)

        cache.clear()
        self.assertEqual(cache.size(), 0)

    def test_size(self):
        """Test getting cache size."""
        cache = TraitCache()
        self.assertEqual(cache.size(), 0)

        cache.set("key1", "value1")
        self.assertEqual(cache.size(), 1)

    def test_max_size_eviction(self):
        """Test LFU eviction when max size is reached."""
        cache = TraitCache(max_size=3)

        cache.set("key1", "value1")
        cache.set("key2", "value2")
        cache.set("key3", "value3")
        self.assertEqual(cache.size(), 3)

        cache.get("key2")
        cache.get("key3")

        cache.set("key4", "value4")
        self.assertEqual(cache.size(), 3)


class TestCachedTraitCalculation(unittest.TestCase):
    """Test cached_trait_calculation function."""

    def test_basic_calculation(self):
        """Test basic trait calculation."""
        result = cached_trait_calculation("test", 0.5, 0.2, 0.0, 1.0)
        self.assertEqual(result, 0.7)

    def test_min_bound(self):
        """Test minimum bound enforcement."""
        result = cached_trait_calculation("test", 0.2, -0.5, 0.0, 1.0)
        self.assertEqual(result, 0.0)

    def test_max_bound(self):
        """Test maximum bound enforcement."""
        result = cached_trait_calculation("test", 0.8, 0.5, 0.0, 1.0)
        self.assertEqual(result, 1.0)

    def test_caching(self):
        """Test that results are cached."""
        result1 = cached_trait_calculation("cached", 0.5, 0.2, 0.0, 1.0)
        result2 = cached_trait_calculation("cached", 0.5, 0.2, 0.0, 1.0)
        self.assertEqual(result1, result2)


class TestGetMemoryUsage(unittest.TestCase):
    """Test get_memory_usage function."""

    def test_memory_usage_returns_float(self):
        """Test that memory usage returns a float."""
        memory = get_memory_usage()
        self.assertIsInstance(memory, float)
        self.assertGreaterEqual(memory, 0.0)


class TestBatchProcessor(unittest.TestCase):
    """Test BatchProcessor class."""

    def test_initialization(self):
        """Test batch processor initialization."""
        processor = BatchProcessor(batch_size=5)
        self.assertEqual(processor.batch_size, 5)

    def test_process_batch(self):
        """Test batch processing."""
        processor = BatchProcessor(batch_size=3)
        items = [1, 2, 3, 4, 5, 6, 7]

        def double(x):
            return x * 2

        results = processor.process_batch(items, double)
        self.assertEqual(results, [2, 4, 6, 8, 10, 12, 14])

    def test_process_empty_batch(self):
        """Test processing empty batch."""
        processor = BatchProcessor()
        results = processor.process_batch([], lambda x: x)
        self.assertEqual(results, [])


class TestOptimizeStringOperations(unittest.TestCase):
    """Test optimize_string_operations function."""

    def test_whitespace_normalization(self):
        """Test whitespace normalization."""
        text = "hello    world  \n  test"
        result = optimize_string_operations(text)
        self.assertEqual(result, "hello world test")

    def test_empty_string(self):
        """Test empty string handling."""
        result = optimize_string_operations("")
        self.assertEqual(result, "")


class TestLazyEvaluator(unittest.TestCase):
    """Test LazyEvaluator class."""

    def test_lazy_computation(self):
        """Test that computation is deferred."""
        call_count = 0

        def compute():
            nonlocal call_count
            call_count += 1
            return "computed"

        evaluator = LazyEvaluator(compute)
        self.assertEqual(call_count, 0)

        value = evaluator.value
        self.assertEqual(value, "computed")
        self.assertEqual(call_count, 1)

    def test_cached_value(self):
        """Test that value is cached after first computation."""
        call_count = 0

        def compute():
            nonlocal call_count
            call_count += 1
            return "computed"

        evaluator = LazyEvaluator(compute)

        value1 = evaluator.value
        value2 = evaluator.value

        self.assertEqual(value1, value2)
        self.assertEqual(call_count, 1)

    def test_reset(self):
        """Test resetting lazy evaluator."""
        call_count = 0

        def compute():
            nonlocal call_count
            call_count += 1
            return call_count

        evaluator = LazyEvaluator(compute)

        value1 = evaluator.value
        self.assertEqual(value1, 1)

        evaluator.reset()

        value2 = evaluator.value
        self.assertEqual(value2, 2)
        self.assertEqual(call_count, 2)


class TestGetOrCompilePattern(unittest.TestCase):
    """Test get_or_compile_pattern function."""

    def test_pattern_compilation(self):
        """Test pattern compilation."""
        pattern = get_or_compile_pattern(r"\d+")
        self.assertIsNotNone(pattern)
        match = pattern.search("test123")
        self.assertIsNotNone(match)
        self.assertEqual(match.group(), "123")

    def test_pattern_caching(self):
        """Test that patterns are cached."""
        pattern1 = get_or_compile_pattern(r"\d+")
        pattern2 = get_or_compile_pattern(r"\d+")
        self.assertIs(pattern1, pattern2)

    def test_case_insensitive(self):
        """Test case insensitive matching."""
        pattern = get_or_compile_pattern(r"test")
        self.assertIsNotNone(pattern.search("TEST"))
        self.assertIsNotNone(pattern.search("Test"))


if __name__ == "__main__":
    unittest.main()
