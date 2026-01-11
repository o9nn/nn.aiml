"""
Structured Logging for Toga Personality System

This module provides a structured logging system for debugging and monitoring
the Toga personality system in production environments.
"""

import json
import logging
import sys
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional


class TogaLogger:
    """
    Structured logger for Toga personality system.

    Provides JSON-formatted logging with context information for better
    debugging and monitoring in production.
    """

    def __init__(
        self,
        name: str = "toga",
        level: int = logging.INFO,
        log_file: Optional[str] = None,
        console: bool = True,
    ):
        """
        Initialize Toga logger.

        Args:
            name: Logger name
            level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
            log_file: Optional path to log file
            console: Whether to log to console
        """
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level)
        # Close and clear existing handlers to prevent resource leaks
        for handler in self.logger.handlers[:]:
            handler.close()
            self.logger.removeHandler(handler)

        # Create formatter
        formatter = logging.Formatter(
            "%(asctime)s - %(name)s - %(levelname)s - %(message)s", datefmt="%Y-%m-%d %H:%M:%S"
        )

        # Add console handler
        if console:
            console_handler = logging.StreamHandler(sys.stdout)
            console_handler.setLevel(level)
            console_handler.setFormatter(formatter)
            self.logger.addHandler(console_handler)

        # Add file handler
        if log_file:
            log_path = Path(log_file) if isinstance(log_file, str) else log_file
            log_path.parent.mkdir(parents=True, exist_ok=True)
            file_handler = logging.FileHandler(log_path)
            file_handler.setLevel(level)
            file_handler.setFormatter(formatter)
            self.logger.addHandler(file_handler)

    def _format_context(self, context: Optional[Dict[str, Any]] = None) -> str:
        """Format context dictionary as JSON string."""
        if context:
            return json.dumps(context, default=str)
        return "{}"

    def debug(self, message: str, context: Optional[Dict[str, Any]] = None):
        """Log debug message with optional context."""
        self.logger.debug(f"{message} | Context: {self._format_context(context)}")

    def info(self, message: str, context: Optional[Dict[str, Any]] = None):
        """Log info message with optional context."""
        self.logger.info(f"{message} | Context: {self._format_context(context)}")

    def warning(self, message: str, context: Optional[Dict[str, Any]] = None):
        """Log warning message with optional context."""
        self.logger.warning(f"{message} | Context: {self._format_context(context)}")

    def error(self, message: str, context: Optional[Dict[str, Any]] = None, exc_info: bool = False):
        """Log error message with optional context and exception info."""
        self.logger.error(
            f"{message} | Context: {self._format_context(context)}", exc_info=exc_info
        )

    def critical(
        self, message: str, context: Optional[Dict[str, Any]] = None, exc_info: bool = False
    ):
        """Log critical message with optional context and exception info."""
        self.logger.critical(
            f"{message} | Context: {self._format_context(context)}", exc_info=exc_info
        )

    def log_interaction(
        self,
        interaction_count: int,
        input_message: str,
        emotional_state: str,
        intensity: float,
    ):
        """
        Log an interaction with structured context.

        Args:
            interaction_count: Current interaction number
            input_message: User input message
            emotional_state: Current emotional state
            intensity: Emotional intensity
        """
        context = {
            "interaction": interaction_count,
            "input_length": len(input_message),
            "emotional_state": emotional_state,
            "intensity": intensity,
            "timestamp": datetime.now().isoformat(),
        }
        self.info("Interaction processed", context=context)

    def log_emotional_change(
        self,
        old_state: str,
        new_state: str,
        trigger: Optional[str] = None,
    ):
        """
        Log an emotional state change.

        Args:
            old_state: Previous emotional state
            new_state: New emotional state
            trigger: What triggered the change
        """
        context = {
            "old_state": old_state,
            "new_state": new_state,
            "trigger": trigger,
            "timestamp": datetime.now().isoformat(),
        }
        self.info("Emotional state changed", context=context)

    def log_obsession_added(self, target: str, total_obsessions: int):
        """
        Log when a new obsession is added.

        Args:
            target: The obsession target
            total_obsessions: Total number of obsessions
        """
        context = {
            "target": target,
            "total_obsessions": total_obsessions,
            "timestamp": datetime.now().isoformat(),
        }
        self.info("New obsession added", context=context)

    def log_trait_evolution(
        self,
        trait_name: str,
        old_value: float,
        new_value: float,
        delta: float,
    ):
        """
        Log personality trait evolution.

        Args:
            trait_name: Name of the trait
            old_value: Previous value
            new_value: New value
            delta: Change amount
        """
        context = {
            "trait": trait_name,
            "old_value": old_value,
            "new_value": new_value,
            "delta": delta,
            "timestamp": datetime.now().isoformat(),
        }
        self.info("Trait evolved", context=context)

    def log_serialization(self, operation: str, success: bool):
        """
        Log serialization/deserialization operations.

        Args:
            operation: Type of operation (save/load)
            success: Whether operation succeeded
        """
        context = {
            "operation": operation,
            "success": success,
            "timestamp": datetime.now().isoformat(),
        }
        level = self.info if success else self.error
        level(f"Serialization {operation}", context=context)


# Global logger instance
_global_logger: Optional[TogaLogger] = None


def get_logger(
    name: str = "toga",
    level: int = logging.INFO,
    log_file: Optional[Path] = None,
    console: bool = True,
) -> TogaLogger:
    """
    Get or create a Toga logger instance.

    Args:
        name: Logger name
        level: Logging level
        log_file: Optional path to log file
        console: Whether to log to console

    Returns:
        TogaLogger instance
    """
    global _global_logger

    if _global_logger is None:
        _global_logger = TogaLogger(name=name, level=level, log_file=log_file, console=console)

    return _global_logger


def set_log_level(level: int):
    """
    Set the global log level.

    Args:
        level: Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    """
    if _global_logger:
        _global_logger.logger.setLevel(level)


class InteractionLogger(TogaLogger):
    """Specialized logger for user interactions."""

    def __init__(self, log_file: Optional[str] = None):
        """Initialize interaction logger."""
        super().__init__(
            name="toga.interaction",
            log_file=Path(log_file) if log_file else None,
        )

    def log_interaction(
        self,
        user_id: str,
        user_message: str,
        toga_response: str,
        emotional_state: str,
        metadata: Optional[Dict[str, Any]] = None,
    ):
        """Log a user interaction."""
        context = {
            "user_id": user_id,
            "message_length": len(user_message),
            "response_length": len(toga_response),
            "emotional_state": emotional_state,
            "timestamp": datetime.now().isoformat(),
        }
        if metadata:
            context.update(metadata)

        self.info(f"Interaction: {user_message[:50]}...", context=context)


class PerformanceLogger(TogaLogger):
    """Specialized logger for performance metrics."""

    def __init__(self, log_file: Optional[str] = None):
        """Initialize performance logger."""
        super().__init__(
            name="toga.performance",
            log_file=Path(log_file) if log_file else None,
        )

    def log_metrics(self, metrics: Dict[str, Any]):
        """Log performance metrics."""
        context = {"timestamp": datetime.now().isoformat(), **metrics}
        self.info("Performance metrics", context=context)

    def log_timing(self, operation: str, duration_ms: float):
        """Log operation timing."""
        context = {
            "operation": operation,
            "duration_ms": duration_ms,
            "timestamp": datetime.now().isoformat(),
        }
        self.info(f"Timing: {operation}", context=context)

    def log_memory_usage(self, operation: str, memory_mb: float):
        """Log memory usage."""
        context = {
            "operation": operation,
            "memory_mb": memory_mb,
            "timestamp": datetime.now().isoformat(),
        }
        self.info(f"Memory: {operation}", context=context)


class EmotionalStateLogger(TogaLogger):
    """Specialized logger for emotional state tracking."""

    def __init__(self, log_file: Optional[str] = None):
        """Initialize emotional state logger."""
        super().__init__(
            name="toga.emotional",
            log_file=Path(log_file) if log_file else None,
        )

    def log_transition(
        self,
        user_id: str,
        from_state: str,
        to_state: str,
        trigger: str,
        intensity: float,
    ):
        """Log emotional state transition."""
        context = {
            "user_id": user_id,
            "from_state": from_state,
            "to_state": to_state,
            "trigger": trigger,
            "intensity": intensity,
            "timestamp": datetime.now().isoformat(),
        }
        self.info(f"Emotional transition: {from_state} -> {to_state}", context=context)

    def log_state(self, user_id: str, emotional_state: Dict[str, Any]):
        """Log current emotional state."""
        context = {
            "user_id": user_id,
            "timestamp": datetime.now().isoformat(),
            **emotional_state,
        }
        self.info("Emotional state", context=context)

    def log_trigger(
        self,
        user_id: str,
        trigger_type: str,
        trigger_value: Any,
        response_intensity: float,
    ):
        """Log emotional trigger event."""
        context = {
            "user_id": user_id,
            "trigger_type": trigger_type,
            "trigger_value": str(trigger_value),
            "response_intensity": response_intensity,
            "timestamp": datetime.now().isoformat(),
        }
        self.info(f"Emotional trigger: {trigger_type}", context=context)
