"""
Tests for Toga Logging System
"""

import logging
import tempfile
import unittest
from pathlib import Path

from python.helpers.toga_logging import (
    EmotionalStateLogger,
    InteractionLogger,
    PerformanceLogger,
    TogaLogger,
)


class TestTogaLogger(unittest.TestCase):
    """Test TogaLogger basic functionality"""

    def test_initialization(self):
        """Test logger initialization"""
        logger = TogaLogger("test_logger")

        self.assertTrue(logger.logger is not None)
        self.assertEqual(logger.logger.name, "test_logger")

    def test_log_levels(self):
        """Test different log levels"""
        with tempfile.TemporaryDirectory() as tmpdir:
            log_file = Path(tmpdir) / "test.log"
            logger = TogaLogger("test_logger", log_file=str(log_file))

            logger.debug("Debug message")
            logger.info("Info message")
            logger.warning("Warning message")
            logger.error("Error message")

            # Verify log file was created
            self.assertTrue(log_file.exists())

    def test_log_with_context(self):
        """Test logging with context data"""
        logger = TogaLogger("test_logger")

        context = {"user_id": "test_user", "session_id": "session_123"}

        # Should not raise exception
        logger.info("Test message", context=context)


class TestInteractionLogger(unittest.TestCase):
    """Test InteractionLogger functionality"""

    def test_initialization(self):
        """Test interaction logger initialization"""
        logger = InteractionLogger()

        self.assertTrue(logger.logger is not None)

    def test_log_interaction(self):
        """Test logging user interaction"""
        with tempfile.TemporaryDirectory() as tmpdir:
            log_file = Path(tmpdir) / "interactions.log"
            logger = InteractionLogger(log_file=str(log_file))

            logger.log_interaction(
                user_id="user_123",
                user_message="Hello Toga!",
                toga_response="Ehehe~ Hi there! ♡",
                emotional_state="cheerful",
            )

            # Verify log file exists and has content
            self.assertTrue(log_file.exists())
            self.assertTrue(log_file.stat().st_size > 0)

    def test_log_interaction_with_metadata(self):
        """Test logging interaction with additional metadata"""
        logger = InteractionLogger()

        metadata = {"context": "casual_chat", "sentiment": "positive", "intensity": 0.8}

        # Should not raise exception
        logger.log_interaction(
            user_id="user_123",
            user_message="Test message",
            toga_response="Test response",
            emotional_state="playful",
            metadata=metadata,
        )


class TestPerformanceLogger(unittest.TestCase):
    """Test PerformanceLogger functionality"""

    def test_initialization(self):
        """Test performance logger initialization"""
        logger = PerformanceLogger()

        self.assertTrue(logger.logger is not None)

    def test_log_performance_metrics(self):
        """Test logging performance metrics"""
        with tempfile.TemporaryDirectory() as tmpdir:
            log_file = Path(tmpdir) / "performance.log"
            logger = PerformanceLogger(log_file=str(log_file))

            metrics = {
                "operation": "process_message",
                "duration_ms": 150.5,
                "memory_mb": 45.2,
                "cpu_percent": 12.3,
            }

            logger.log_metrics(metrics)

            # Verify log file exists
            self.assertTrue(log_file.exists())

    def test_log_timing(self):
        """Test logging operation timing"""
        logger = PerformanceLogger()

        # Should not raise exception
        logger.log_timing(operation="test_operation", duration_ms=100.5)

    def test_log_memory_usage(self):
        """Test logging memory usage"""
        logger = PerformanceLogger()

        # Should not raise exception
        logger.log_memory_usage(operation="test_operation", memory_mb=50.0)


class TestEmotionalStateLogger(unittest.TestCase):
    """Test EmotionalStateLogger functionality"""

    def test_initialization(self):
        """Test emotional state logger initialization"""
        logger = EmotionalStateLogger()

        self.assertTrue(logger.logger is not None)

    def test_log_emotional_transition(self):
        """Test logging emotional state transition"""
        with tempfile.TemporaryDirectory() as tmpdir:
            log_file = Path(tmpdir) / "emotional.log"
            logger = EmotionalStateLogger(log_file=str(log_file))

            logger.log_transition(
                user_id="user_123",
                from_state="neutral",
                to_state="cheerful",
                trigger="positive_interaction",
                intensity=0.85,
            )

            # Verify log file exists
            self.assertTrue(log_file.exists())

    def test_log_emotional_state(self):
        """Test logging current emotional state"""
        logger = EmotionalStateLogger()

        emotional_state = {
            "primary": "cheerful",
            "secondary": "playful",
            "intensity": 0.9,
            "stability": 0.7,
        }

        # Should not raise exception
        logger.log_state(user_id="user_123", emotional_state=emotional_state)

    def test_log_emotional_trigger(self):
        """Test logging emotional trigger event"""
        logger = EmotionalStateLogger()

        # Should not raise exception
        logger.log_trigger(
            user_id="user_123",
            trigger_type="cute_detection",
            trigger_value="adorable puppy",
            response_intensity=0.95,
        )


class TestLoggerIntegration(unittest.TestCase):
    """Test integration between different loggers"""

    def test_multiple_loggers_same_file(self):
        """Test multiple loggers writing to same file"""
        with tempfile.TemporaryDirectory() as tmpdir:
            log_file = Path(tmpdir) / "combined.log"

            interaction_logger = InteractionLogger(log_file=str(log_file))
            performance_logger = PerformanceLogger(log_file=str(log_file))

            interaction_logger.log_interaction(
                user_id="user_123",
                user_message="Test",
                toga_response="Response",
                emotional_state="cheerful",
            )

            performance_logger.log_timing(operation="test", duration_ms=50.0)

            # Both should write successfully
            self.assertTrue(log_file.exists())
            self.assertTrue(log_file.stat().st_size > 0)

    def test_logger_hierarchy(self):
        """Test logger hierarchy and inheritance"""
        parent_logger = TogaLogger("parent")
        child_logger = TogaLogger("parent.child")

        # Child logger should inherit from parent
        self.assertEqual(child_logger.logger.parent.name, "parent")


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
