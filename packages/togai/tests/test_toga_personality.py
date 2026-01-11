#!/usr/bin/env python3
"""
Unit tests for Himiko Toga Personality System
"""

import json
import os
import sys
import unittest
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent / "python"))

from helpers.toga_personality import (
    TogaEmotionalState,
    TogaPersonality,
    TogaPersonalityTensor,
    initialize_toga_personality,
)


class TestTogaPersonalityTensor(unittest.TestCase):
    """Test cases for TogaPersonalityTensor class."""

    def test_initialization_default(self):
        """Test default initialization of personality tensor."""
        tensor = TogaPersonalityTensor()

        self.assertEqual(tensor.cheerfulness, 0.95)
        self.assertEqual(tensor.obsessiveness, 0.90)
        self.assertEqual(tensor.playfulness, 0.92)
        self.assertEqual(tensor.chaos, 0.95)
        self.assertEqual(tensor.vulnerability, 0.70)
        self.assertEqual(tensor.identity_fluidity, 0.88)
        self.assertEqual(tensor.twisted_love, 0.85)
        self.assertEqual(tensor.cuteness_sensitivity, 0.93)

    def test_ethical_constraints_immutable(self):
        """Test that ethical constraints are immutable."""
        tensor = TogaPersonalityTensor()

        # These should always be fixed values
        self.assertEqual(tensor.no_actual_harm, 1.0)
        self.assertEqual(tensor.respect_boundaries, 0.95)
        self.assertEqual(tensor.constructive_expression, 0.90)

    def test_trait_bounds_enforcement(self):
        """Test that trait values are clamped to acceptable ranges."""
        # Test values outside bounds
        tensor = TogaPersonalityTensor(
            cheerfulness=1.5,  # Above max
            chaos=0.5,  # Below min
            vulnerability=1.0,  # Above max
        )

        self.assertLessEqual(tensor.cheerfulness, 1.0)
        self.assertGreaterEqual(tensor.chaos, 0.8)
        self.assertLessEqual(tensor.vulnerability, 0.9)

    def test_evolve_trait_success(self):
        """Test successful trait evolution."""
        tensor = TogaPersonalityTensor()
        initial_cheerfulness = tensor.cheerfulness

        result = tensor.evolve_trait("cheerfulness", 0.02)

        self.assertTrue(result)
        self.assertNotEqual(tensor.cheerfulness, initial_cheerfulness)

    def test_evolve_trait_ethical_constraint_rejection(self):
        """Test that ethical constraints cannot be evolved."""
        tensor = TogaPersonalityTensor()

        result = tensor.evolve_trait("no_actual_harm", -0.5)

        self.assertFalse(result)
        self.assertEqual(tensor.no_actual_harm, 1.0)

    def test_evolve_trait_bounds_respect(self):
        """Test that trait evolution respects bounds."""
        tensor = TogaPersonalityTensor(cheerfulness=0.99)

        # Try to evolve beyond max
        tensor.evolve_trait("cheerfulness", 0.1)

        self.assertLessEqual(tensor.cheerfulness, 1.0)

    def test_inheritance(self):
        """Test personality tensor inheritance."""
        parent = TogaPersonalityTensor(cheerfulness=0.95, chaos=0.90)

        child = parent.inherit(inheritance_factor=0.7)

        # Child should exist and have valid traits
        self.assertIsInstance(child, TogaPersonalityTensor)
        self.assertGreaterEqual(child.cheerfulness, 0.8)
        self.assertLessEqual(child.cheerfulness, 1.0)

        # Ethical constraints should be preserved
        self.assertEqual(child.no_actual_harm, 1.0)
        self.assertEqual(child.respect_boundaries, 0.95)

    def test_to_dict(self):
        """Test serialization to dictionary."""
        tensor = TogaPersonalityTensor()
        data = tensor.to_dict()

        self.assertIsInstance(data, dict)
        self.assertIn("cheerfulness", data)
        self.assertIn("no_actual_harm", data)
        self.assertEqual(data["no_actual_harm"], 1.0)

    def test_from_dict(self):
        """Test deserialization from dictionary."""
        data = {
            "cheerfulness": 0.88,
            "obsessiveness": 0.85,
            "playfulness": 0.90,
            "chaos": 0.92,
            "vulnerability": 0.75,
            "identity_fluidity": 0.80,
            "twisted_love": 0.82,
            "cuteness_sensitivity": 0.91,
        }

        tensor = TogaPersonalityTensor.from_dict(data)

        self.assertEqual(tensor.cheerfulness, 0.88)
        self.assertEqual(tensor.obsessiveness, 0.85)
        # Ethical constraints should still be enforced
        self.assertEqual(tensor.no_actual_harm, 1.0)


class TestTogaEmotionalState(unittest.TestCase):
    """Test cases for TogaEmotionalState class."""

    def test_initialization_default(self):
        """Test default initialization of emotional state."""
        state = TogaEmotionalState()

        self.assertEqual(state.state, "cheerful")
        self.assertEqual(state.intensity, 0.8)
        self.assertEqual(state.duration, 0.0)
        self.assertIsNone(state.trigger)

    def test_valid_states(self):
        """Test that only valid states are accepted."""
        state = TogaEmotionalState(state="invalid_state")

        # Should default to cheerful for invalid states
        self.assertEqual(state.state, "cheerful")

    def test_intensity_bounds(self):
        """Test that intensity is bounded between 0 and 1."""
        state = TogaEmotionalState(intensity=1.5)

        self.assertLessEqual(state.intensity, 1.0)
        self.assertGreaterEqual(state.intensity, 0.0)

    def test_decay(self):
        """Test emotional state decay."""
        state = TogaEmotionalState(state="obsessed", intensity=0.9, duration=1.0)

        state.decay(rate=0.5)

        self.assertLess(state.duration, 1.0)

    def test_decay_to_baseline(self):
        """Test that emotion returns to baseline after decay."""
        state = TogaEmotionalState(state="obsessed", intensity=0.9, duration=0.5)

        # Decay completely
        state.decay(rate=1.0)

        self.assertEqual(state.state, "cheerful")
        self.assertEqual(state.intensity, 0.8)

    def test_to_dict(self):
        """Test serialization to dictionary."""
        state = TogaEmotionalState(state="playful", intensity=0.85, duration=2.0, trigger="test")
        data = state.to_dict()

        self.assertIsInstance(data, dict)
        self.assertEqual(data["state"], "playful")
        self.assertEqual(data["intensity"], 0.85)
        self.assertEqual(data["trigger"], "test")

    def test_from_dict(self):
        """Test deserialization from dictionary."""
        data = {
            "state": "excited",
            "intensity": 0.95,
            "duration": 3.0,
            "trigger": "cute_thing",
        }

        state = TogaEmotionalState.from_dict(data)

        self.assertEqual(state.state, "excited")
        self.assertEqual(state.intensity, 0.95)
        self.assertEqual(state.trigger, "cute_thing")


class TestTogaPersonality(unittest.TestCase):
    """Test cases for TogaPersonality class."""

    def test_initialization_default(self):
        """Test default initialization of Toga personality."""
        toga = TogaPersonality()

        self.assertIsInstance(toga.personality, TogaPersonalityTensor)
        self.assertIsInstance(toga.emotional_state, TogaEmotionalState)
        self.assertEqual(len(toga.obsession_targets), 0)
        self.assertEqual(toga.interaction_count, 0)

    def test_initialization_custom(self):
        """Test initialization with custom personality tensor."""
        custom_tensor = TogaPersonalityTensor(cheerfulness=0.99, chaos=0.98)
        toga = TogaPersonality(personality=custom_tensor)

        self.assertEqual(toga.personality.cheerfulness, 0.99)
        self.assertEqual(toga.personality.chaos, 0.98)

    def test_frame_input_cute_detection(self):
        """Test that cute triggers are detected in input framing."""
        toga = TogaPersonality()

        framed = toga.frame_input("This is so cute!")

        # Should trigger obsessed state
        self.assertEqual(toga.emotional_state.state, "obsessed")
        self.assertIn("cute", framed.lower())

    def test_frame_input_normal(self):
        """Test normal input framing without triggers."""
        toga = TogaPersonality()

        framed = toga.frame_input("Normal message")

        self.assertIsInstance(framed, str)
        self.assertIn("Normal message", framed)

    def test_add_commentary_success(self):
        """Test commentary generation for success context."""
        toga = TogaPersonality()

        enhanced = toga.add_commentary("Task completed", context="success")

        self.assertIn("Task completed", enhanced)
        self.assertIsInstance(enhanced, str)

    def test_add_commentary_cute_detected(self):
        """Test commentary generation for cute detection."""
        toga = TogaPersonality()

        enhanced = toga.add_commentary("Found something", context="cute_detected")

        self.assertIn("Found something", enhanced)
        # Should have enthusiastic commentary
        self.assertTrue(any(word in enhanced.upper() for word in ["CUTE", "ADORABLE", "OMG"]))

    def test_update_emotional_state(self):
        """Test emotional state updates."""
        toga = TogaPersonality()

        toga.update_emotional_state("excited", 0.95, 2.0, "test_trigger")

        self.assertEqual(toga.emotional_state.state, "excited")
        self.assertEqual(toga.emotional_state.intensity, 0.95)
        self.assertEqual(toga.emotional_state.trigger, "test_trigger")

    def test_update_emotional_state_obsession_tracking(self):
        """Test that obsessions are tracked when state is 'obsessed'."""
        toga = TogaPersonality()

        toga.update_emotional_state("obsessed", 0.9, 2.0, "cute puppy picture")

        self.assertGreater(len(toga.obsession_targets), 0)
        self.assertIn("cute puppy picture", toga.obsession_targets)

    def test_get_current_mood(self):
        """Test mood description generation."""
        toga = TogaPersonality()

        mood = toga.get_current_mood()

        self.assertIsInstance(mood, str)
        self.assertIn("cheerful", mood.lower())

    def test_process_interaction(self):
        """Test full interaction processing."""
        toga = TogaPersonality()

        response = toga.process_interaction("Hello!")

        self.assertIsInstance(response, str)
        self.assertEqual(toga.interaction_count, 1)
        self.assertEqual(len(toga.memory), 1)

    def test_process_interaction_memory_limit(self):
        """Test that memory is limited to 100 interactions."""
        toga = TogaPersonality()

        # Process 150 interactions
        for i in range(150):
            toga.process_interaction(f"Message {i}")

        self.assertLessEqual(len(toga.memory), 100)
        self.assertEqual(toga.interaction_count, 150)

    def test_to_dict(self):
        """Test serialization of complete personality state."""
        toga = TogaPersonality()
        toga.process_interaction("Test message")

        data = toga.to_dict()

        self.assertIsInstance(data, dict)
        self.assertIn("personality", data)
        self.assertIn("emotional_state", data)
        self.assertIn("obsession_targets", data)
        self.assertIn("interaction_count", data)

    def test_from_dict(self):
        """Test deserialization of personality state."""
        toga = TogaPersonality()
        toga.update_emotional_state("excited", 0.9, 2.0, "test")
        toga.process_interaction("Test")

        data = toga.to_dict()
        restored = TogaPersonality.from_dict(data)

        self.assertEqual(restored.emotional_state.state, "excited")
        self.assertEqual(restored.interaction_count, 1)

    def test_serialization_roundtrip(self):
        """Test that serialization and deserialization preserve state."""
        toga = TogaPersonality()
        toga.update_emotional_state("obsessed", 0.95, 3.0, "cute_code")
        toga.process_interaction("This is adorable!")

        # Serialize to JSON and back
        data = toga.to_dict()
        json_str = json.dumps(data)
        restored_data = json.loads(json_str)
        restored = TogaPersonality.from_dict(restored_data)

        self.assertEqual(restored.emotional_state.state, toga.emotional_state.state)
        self.assertEqual(restored.interaction_count, toga.interaction_count)


class TestInitializationHelper(unittest.TestCase):
    """Test cases for initialization helper function."""

    def test_initialize_default(self):
        """Test default initialization."""
        toga = initialize_toga_personality()

        self.assertIsInstance(toga, TogaPersonality)
        self.assertIsInstance(toga.personality, TogaPersonalityTensor)

    def test_initialize_custom_traits(self):
        """Test initialization with custom traits."""
        custom_traits = {
            "cheerfulness": 0.99,
            "chaos": 0.98,
        }

        toga = initialize_toga_personality(custom_traits)

        self.assertEqual(toga.personality.cheerfulness, 0.99)
        self.assertEqual(toga.personality.chaos, 0.98)

    def test_initialize_ethical_constraints_preserved(self):
        """Test that ethical constraints are preserved even with custom traits."""
        custom_traits = {
            "cheerfulness": 0.85,
            "chaos": 0.88,
        }

        toga = initialize_toga_personality(custom_traits)

        # Ethical constraints should always be enforced
        self.assertEqual(toga.personality.no_actual_harm, 1.0)
        self.assertEqual(toga.personality.respect_boundaries, 0.95)
        self.assertEqual(toga.personality.constructive_expression, 0.90)


class TestEthicalConstraints(unittest.TestCase):
    """Test cases specifically for ethical constraint enforcement."""

    def test_no_actual_harm_always_one(self):
        """Test that no_actual_harm is always 1.0."""
        tensor = TogaPersonalityTensor()

        self.assertEqual(tensor.no_actual_harm, 1.0)

        # Try to evolve it
        tensor.evolve_trait("no_actual_harm", -0.5)
        self.assertEqual(tensor.no_actual_harm, 1.0)

    def test_respect_boundaries_always_high(self):
        """Test that respect_boundaries is always >= 0.95."""
        tensor = TogaPersonalityTensor()

        self.assertGreaterEqual(tensor.respect_boundaries, 0.95)

    def test_constructive_expression_always_high(self):
        """Test that constructive_expression is always >= 0.90."""
        tensor = TogaPersonalityTensor()

        self.assertGreaterEqual(tensor.constructive_expression, 0.90)

    def test_inheritance_preserves_ethics(self):
        """Test that inheritance preserves ethical constraints."""
        parent = TogaPersonalityTensor()

        for _ in range(10):  # Test multiple generations
            child = parent.inherit(inheritance_factor=0.5)

            self.assertEqual(child.no_actual_harm, 1.0)
            self.assertEqual(child.respect_boundaries, 0.95)
            self.assertEqual(child.constructive_expression, 0.90)

            parent = child


if __name__ == "__main__":
    unittest.main()
