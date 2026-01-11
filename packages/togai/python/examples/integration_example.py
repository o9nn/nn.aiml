#!/usr/bin/env python3
"""
Advanced Integration Example for Layla & Neuro-Sama

This example demonstrates how to integrate the Toga personality system
with the Layla character system and Neuro-Sama cognitive framework.
"""

import os
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

import logging

from helpers.toga_config import create_default_config_file, load_config
from helpers.toga_logging import get_logger
from helpers.toga_personality import TogaPersonality, initialize_toga_personality


class LaylaTogaIntegration:
    """
    Integration layer between Toga personality and Layla character system.

    This class demonstrates how to integrate Toga's personality with:
    - Layla's character animation system
    - Voice synthesis (TTS)
    - Cognitive processing
    - Memory management
    """

    def __init__(self, config_path: Path = None, log_level: int = logging.INFO):
        """
        Initialize the integration.

        Args:
            config_path: Optional path to configuration file
            log_level: Logging level
        """
        # Load configuration
        self.config = load_config(config_path)

        # Initialize logger
        self.logger = get_logger(level=log_level)
        self.logger.info("Initializing Layla-Toga integration")

        # Initialize Toga personality with config defaults
        custom_traits = {
            "cheerfulness": self.config.default_cheerfulness,
            "obsessiveness": self.config.default_obsessiveness,
            "playfulness": self.config.default_playfulness,
            "chaos": self.config.default_chaos,
            "vulnerability": self.config.default_vulnerability,
            "identity_fluidity": self.config.default_identity_fluidity,
            "twisted_love": self.config.default_twisted_love,
            "cuteness_sensitivity": self.config.default_cuteness_sensitivity,
        }

        self.toga = initialize_toga_personality(custom_traits)
        self.logger.info(
            "Toga personality initialized",
            context={
                "cheerfulness": self.toga.personality.cheerfulness,
                "chaos": self.toga.personality.chaos,
            },
        )

    def process_user_input(self, user_input: str) -> dict:
        """
        Process user input through the Toga personality system.

        This method demonstrates the full pipeline:
        1. Frame input through Toga's perspective
        2. Log the interaction
        3. Generate response with personality
        4. Update emotional state

        Args:
            user_input: User's input message

        Returns:
            Dictionary with response data for Layla system
        """
        # Log interaction
        self.logger.log_interaction(
            interaction_count=self.toga.interaction_count + 1,
            input_message=user_input,
            emotional_state=self.toga.emotional_state.state,
            intensity=self.toga.emotional_state.intensity,
        )

        # Store old emotional state for logging
        old_state = self.toga.emotional_state.state

        # Process through Toga
        framed_input = self.toga.process_interaction(user_input)

        # Check if emotional state changed
        new_state = self.toga.emotional_state.state
        if old_state != new_state:
            self.logger.log_emotional_change(
                old_state=old_state,
                new_state=new_state,
                trigger=user_input[:50],  # First 50 chars
            )

        # Generate response data for Layla
        response_data = {
            "framed_input": framed_input,
            "emotional_state": self.toga.emotional_state.state,
            "intensity": self.toga.emotional_state.intensity,
            "mood_description": self.toga.get_current_mood(),
            "obsessions": self.toga.obsession_targets,
            "interaction_count": self.toga.interaction_count,
            # Layla-specific fields
            "animation_state": self._get_animation_state(),
            "voice_parameters": self._get_voice_parameters(),
            "expression": self._get_expression(),
        }

        return response_data

    def _get_animation_state(self) -> str:
        """
        Get the appropriate animation state for Layla based on Toga's emotion.

        Returns:
            Animation state name for Layla's Live2D system
        """
        state_map = {
            "cheerful": "idle_happy",
            "obsessed": "excited_sparkle",
            "playful": "bounce_playful",
            "vulnerable": "sad_thoughtful",
            "chaotic": "spin_chaotic",
            "excited": "jump_excited",
            "pouty": "pout_arms_crossed",
        }
        return state_map.get(self.toga.emotional_state.state, "idle_neutral")

    def _get_voice_parameters(self) -> dict:
        """
        Get voice synthesis parameters based on Toga's emotional state.

        Returns:
            Dictionary of TTS parameters
        """
        # Base parameters
        params = {
            "pitch": 1.0,
            "speed": 1.0,
            "emotion": "neutral",
        }

        # Adjust based on emotional state
        state = self.toga.emotional_state.state
        intensity = self.toga.emotional_state.intensity

        if state == "cheerful":
            params["pitch"] = 1.0 + (intensity * 0.1)
            params["speed"] = 1.0 + (intensity * 0.1)
            params["emotion"] = "happy"
        elif state == "obsessed":
            params["pitch"] = 1.1 + (intensity * 0.15)
            params["speed"] = 1.1 + (intensity * 0.2)
            params["emotion"] = "excited"
        elif state == "playful":
            params["pitch"] = 1.05 + (intensity * 0.1)
            params["speed"] = 1.15 + (intensity * 0.15)
            params["emotion"] = "playful"
        elif state == "vulnerable":
            params["pitch"] = 0.95 - (intensity * 0.05)
            params["speed"] = 0.9 - (intensity * 0.1)
            params["emotion"] = "sad"
        elif state == "chaotic":
            params["pitch"] = 1.1 + (intensity * 0.2)
            params["speed"] = 1.2 + (intensity * 0.3)
            params["emotion"] = "chaotic"
        elif state == "excited":
            params["pitch"] = 1.15 + (intensity * 0.15)
            params["speed"] = 1.2 + (intensity * 0.2)
            params["emotion"] = "very_excited"
        elif state == "pouty":
            params["pitch"] = 0.98
            params["speed"] = 0.95
            params["emotion"] = "pouty"

        return params

    def _get_expression(self) -> str:
        """
        Get the facial expression for Layla based on Toga's emotion.

        Returns:
            Expression name for Layla's expression system
        """
        expression_map = {
            "cheerful": "smile_closed_eyes",
            "obsessed": "sparkle_eyes_blush",
            "playful": "mischievous_grin",
            "vulnerable": "sad_downcast",
            "chaotic": "wide_grin_crazy",
            "excited": "open_mouth_excited",
            "pouty": "pout_cheeks_puffed",
        }
        return expression_map.get(self.toga.emotional_state.state, "neutral")

    def add_commentary_to_output(self, content: str, context: str = "general") -> str:
        """
        Add Toga's personality-driven commentary to output.

        Args:
            content: Content to add commentary to
            context: Context type (success, failure, cute_detected, general)

        Returns:
            Content with added commentary
        """
        return self.toga.add_commentary(content, context=context)

    def save_state(self, filepath: Path):
        """
        Save the current Toga personality state.

        Args:
            filepath: Path to save state file
        """
        import json

        try:
            state = self.toga.to_dict()
            with open(filepath, "w", encoding="utf-8") as f:
                json.dump(state, f, indent=2)

            self.logger.log_serialization(operation="save", success=True)
            self.logger.info(f"State saved to {filepath}")
        except Exception as e:
            self.logger.log_serialization(operation="save", success=False)
            self.logger.error(f"Failed to save state: {e}", exc_info=True)

    def load_state(self, filepath: Path):
        """
        Load a previously saved Toga personality state.

        Args:
            filepath: Path to state file
        """
        import json

        try:
            with open(filepath, "r", encoding="utf-8") as f:
                state = json.load(f)

            from helpers.toga_personality import TogaPersonality

            self.toga = TogaPersonality.from_dict(state)

            self.logger.log_serialization(operation="load", success=True)
            self.logger.info(f"State loaded from {filepath}")
        except Exception as e:
            self.logger.log_serialization(operation="load", success=False)
            self.logger.error(f"Failed to load state: {e}", exc_info=True)


def demo_integration():
    """Demonstrate the integration with example interactions."""
    print("=" * 70)
    print("LAYLA-TOGA INTEGRATION DEMO")
    print("=" * 70)
    print()

    # Initialize integration
    integration = LaylaTogaIntegration(log_level=logging.INFO)

    # Example interactions
    test_inputs = [
        "Hello Toga!",
        "Look at this cute kitten picture!",
        "Can you help me with this task?",
        "This code is so adorable!",
        "Something went wrong...",
    ]

    for i, user_input in enumerate(test_inputs, 1):
        print(f"\n--- Interaction {i} ---")
        print(f"User: {user_input}")

        response_data = integration.process_user_input(user_input)

        print(f"Toga: {response_data['framed_input']}")
        print(f"Mood: {response_data['mood_description']}")
        print(f"Animation: {response_data['animation_state']}")
        print(f"Expression: {response_data['expression']}")
        print(
            f"Voice: pitch={response_data['voice_parameters']['pitch']:.2f}, "
            f"speed={response_data['voice_parameters']['speed']:.2f}"
        )

    # Demonstrate commentary
    print("\n" + "=" * 70)
    print("COMMENTARY EXAMPLES")
    print("=" * 70)

    contexts = [
        ("Task completed successfully!", "success"),
        ("Found an adorable solution!", "cute_detected"),
    ]

    for content, context in contexts:
        print(f"\n{context.upper()}:")
        enhanced = integration.add_commentary_to_output(content, context=context)
        print(enhanced)

    # Demonstrate state persistence
    print("\n" + "=" * 70)
    print("STATE PERSISTENCE")
    print("=" * 70)

    state_file = Path("toga_state.json")
    integration.save_state(state_file)
    print(f"\n✓ State saved to {state_file}")
    print(f"  Interactions: {integration.toga.interaction_count}")
    print(f"  Obsessions: {len(integration.toga.obsession_targets)}")

    if state_file.exists():
        state_file.unlink()
        print(f"✓ Cleaned up demo state file")


if __name__ == "__main__":
    demo_integration()
