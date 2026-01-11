"""
Configuration Management for Toga Personality System

This module provides configuration management for customizing Toga's personality
parameters, emotional state settings, and system behavior.
"""

import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Dict, Optional


@dataclass
class TogaConfig:
    """
    Configuration for Toga personality system.

    This class holds all configurable parameters for the personality system,
    including trait defaults, memory limits, and behavior settings.
    """

    # === Personality Trait Defaults ===
    default_cheerfulness: float = 0.95
    default_obsessiveness: float = 0.90
    default_playfulness: float = 0.92
    default_chaos: float = 0.95
    default_vulnerability: float = 0.70
    default_identity_fluidity: float = 0.88
    default_twisted_love: float = 0.85
    default_cuteness_sensitivity: float = 0.93

    # === Emotional State Settings ===
    default_emotional_state: str = "cheerful"
    default_emotional_intensity: float = 0.8
    emotional_decay_rate: float = 0.05

    # === Memory Management ===
    memory_max_size: int = 100
    memory_export_size: int = 10

    # === Obsession Tracking ===
    obsession_target_words: int = 3  # How many words to extract for obsession targets
    max_obsession_targets: int = 50  # Maximum number of obsessions to track

    # === Speech Pattern Settings ===
    speech_pattern_probability: float = 0.5  # Base probability for adding speech patterns
    heart_emoji_probability: float = 0.7  # Base probability for heart emojis

    # === Cute Detection ===
    cute_triggers: list = None  # Will be set in __post_init__

    # === Inheritance Settings ===
    default_inheritance_factor: float = 0.7

    def __post_init__(self):
        """Initialize default cute triggers if not provided."""
        if self.cute_triggers is None:
            self.cute_triggers = [
                "cute",
                "adorable",
                "lovely",
                "pretty",
                "sweet",
                "kawaii",
                "cuddly",
                "precious",
                "darling",
                "beautiful",
                "gorgeous",
            ]

    def to_dict(self) -> Dict[str, Any]:
        """Convert configuration to dictionary."""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "TogaConfig":
        """Create configuration from dictionary."""
        return cls(**data)

    def save(self, filepath: Path) -> None:
        """
        Save configuration to JSON file.

        Args:
            filepath: Path to save configuration file
        """
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(self.to_dict(), f, indent=2)

    @classmethod
    def load(cls, filepath: Path) -> "TogaConfig":
        """
        Load configuration from JSON file.

        Args:
            filepath: Path to configuration file

        Returns:
            Loaded TogaConfig instance
        """
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
        return cls.from_dict(data)

    @classmethod
    def load_or_default(cls, filepath: Optional[Path] = None) -> "TogaConfig":
        """
        Load configuration from file or return default if file doesn't exist.

        Args:
            filepath: Optional path to configuration file

        Returns:
            Loaded or default TogaConfig instance
        """
        if filepath and filepath.exists():
            return cls.load(filepath)
        return cls()


# Default configuration instance
DEFAULT_CONFIG = TogaConfig()


def get_default_config() -> TogaConfig:
    """
    Get the default configuration instance.

    Returns:
        Default TogaConfig instance
    """
    return DEFAULT_CONFIG


def load_config(filepath: Optional[Path] = None) -> TogaConfig:
    """
    Load configuration from file or return default.

    This is a convenience function for loading configuration.

    Args:
        filepath: Optional path to configuration file

    Returns:
        Loaded or default TogaConfig instance

    Example:
        >>> config = load_config(Path("toga_config.json"))
        >>> print(config.default_cheerfulness)
        0.95
    """
    return TogaConfig.load_or_default(filepath)


def create_default_config_file(filepath: Path) -> None:
    """
    Create a default configuration file.

    This is useful for generating a template configuration file that users
    can customize.

    Args:
        filepath: Path where to save the default configuration

    Example:
        >>> create_default_config_file(Path("toga_config.json"))
    """
    config = TogaConfig()
    config.save(filepath)


class ConfigManager:
    """
    Configuration manager for Toga personality system.

    Provides utilities for managing, validating, and merging configurations.
    """

    @staticmethod
    def get_default_config() -> TogaConfig:
        """Get default configuration."""
        return TogaConfig()

    @staticmethod
    def save_config(config: TogaConfig, filepath: Path) -> None:
        """Save configuration to file."""
        config.save(filepath)

    @staticmethod
    def load_config(filepath: Path) -> TogaConfig:
        """Load configuration from file, return default if not found."""
        return TogaConfig.load_or_default(filepath)

    @staticmethod
    def validate_config(config: TogaConfig) -> tuple[bool, list[str]]:
        """
        Validate configuration values.

        Returns:
            Tuple of (is_valid, list_of_errors)
        """
        errors = []

        # Validate trait bounds (0.0 to 1.0)
        traits = {
            "cheerfulness": config.default_cheerfulness,
            "obsessiveness": config.default_obsessiveness,
            "playfulness": config.default_playfulness,
            "chaos": config.default_chaos,
            "vulnerability": config.default_vulnerability,
            "identity_fluidity": config.default_identity_fluidity,
            "twisted_love": config.default_twisted_love,
            "cuteness_sensitivity": config.default_cuteness_sensitivity,
        }

        for name, value in traits.items():
            if not 0.0 <= value <= 1.0:
                errors.append(f"Trait '{name}' value {value} out of bounds [0.0, 1.0]")

        # Validate emotional intensity
        if not 0.0 <= config.default_emotional_intensity <= 1.0:
            errors.append(
                f"Emotional intensity {config.default_emotional_intensity} out of bounds [0.0, 1.0]"
            )

        # Validate memory settings
        if config.memory_max_size <= 0:
            errors.append(f"Memory max size {config.memory_max_size} must be positive")

        if config.memory_export_size <= 0:
            errors.append(f"Memory export size {config.memory_export_size} must be positive")

        # Validate probabilities
        if not 0.0 <= config.speech_pattern_probability <= 1.0:
            errors.append(
                f"Speech pattern probability {config.speech_pattern_probability} out of bounds [0.0, 1.0]"
            )

        if not 0.0 <= config.heart_emoji_probability <= 1.0:
            errors.append(
                f"Heart emoji probability {config.heart_emoji_probability} out of bounds [0.0, 1.0]"
            )

        return len(errors) == 0, errors

    @staticmethod
    def config_to_dict(config: TogaConfig) -> Dict[str, Any]:
        """Convert config to dictionary."""
        return config.to_dict()

    @staticmethod
    def config_from_dict(data: Dict[str, Any]) -> TogaConfig:
        """Create config from dictionary."""
        return TogaConfig.from_dict(data)

    @staticmethod
    def merge_configs(base: TogaConfig, override: TogaConfig) -> TogaConfig:
        """
        Merge two configurations, with override taking precedence.

        Args:
            base: Base configuration
            override: Override configuration

        Returns:
            Merged configuration
        """
        base_dict = base.to_dict()
        override_dict = override.to_dict()

        # Merge dictionaries
        merged_dict = {**base_dict, **override_dict}

        return TogaConfig.from_dict(merged_dict)
