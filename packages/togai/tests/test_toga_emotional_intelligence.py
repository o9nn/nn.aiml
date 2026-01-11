"""
Tests for Toga Emotional Intelligence Module
"""

import unittest
from python.helpers.toga_emotional_intelligence import (
    EmotionCategory,
    SentimentPolarity,
    EmotionalContext,
    EmpatheticResponse,
    EmotionalIntelligenceEngine,
)


class TestEmotionCategory(unittest.TestCase):
    """Test EmotionCategory enum."""

    def test_all_categories_exist(self):
        """Test that all expected emotion categories exist."""
        expected = ["JOY", "SADNESS", "ANGER", "FEAR", "SURPRISE", "DISGUST", "TRUST", "ANTICIPATION"]
        actual = [e.name for e in EmotionCategory]
        self.assertEqual(set(actual), set(expected))


class TestSentimentPolarity(unittest.TestCase):
    """Test SentimentPolarity enum."""

    def test_all_polarities_exist(self):
        """Test that all sentiment polarities exist."""
        expected = ["VERY_POSITIVE", "POSITIVE", "NEUTRAL", "NEGATIVE", "VERY_NEGATIVE"]
        actual = [s.name for s in SentimentPolarity]
        self.assertEqual(set(actual), set(expected))


class TestEmotionalContext(unittest.TestCase):
    """Test EmotionalContext dataclass."""

    def test_initialization_full(self):
        """Test full initialization of emotional context."""
        context = EmotionalContext(
            primary_emotion=EmotionCategory.JOY,
            secondary_emotion=EmotionCategory.SURPRISE,
            intensity=0.8,
            sentiment=SentimentPolarity.POSITIVE,
            confidence=0.9,
            keywords=["happy", "excited"],
        )
        self.assertEqual(context.primary_emotion, EmotionCategory.JOY)
        self.assertEqual(context.secondary_emotion, EmotionCategory.SURPRISE)
        self.assertEqual(context.intensity, 0.8)


class TestEmpatheticResponse(unittest.TestCase):
    """Test EmpatheticResponse dataclass."""

    def test_initialization(self):
        """Test empathetic response initialization."""
        response = EmpatheticResponse(
            acknowledgment="I hear you",
            validation="That makes sense",
            support="I'm here for you",
            tone_adjustment=-0.3,
        )
        self.assertEqual(response.acknowledgment, "I hear you")
        self.assertEqual(response.tone_adjustment, -0.3)


class TestEmotionalIntelligenceEngine(unittest.TestCase):
    """Test EmotionalIntelligenceEngine class."""

    def test_initialization(self):
        """Test engine initialization."""
        engine = EmotionalIntelligenceEngine()
        self.assertIsNotNone(engine.emotion_keywords)
        self.assertIsNotNone(engine.sentiment_keywords)
        self.assertGreater(len(engine.emotion_keywords), 0)
        self.assertGreater(len(engine.sentiment_keywords), 0)

    def test_emotion_keywords_structure(self):
        """Test emotion keywords are properly structured."""
        engine = EmotionalIntelligenceEngine()
        self.assertIn(EmotionCategory.JOY, engine.emotion_keywords)
        self.assertIn(EmotionCategory.SADNESS, engine.emotion_keywords)
        self.assertIsInstance(engine.emotion_keywords[EmotionCategory.JOY], list)

    def test_sentiment_keywords_structure(self):
        """Test sentiment keywords are properly structured."""
        engine = EmotionalIntelligenceEngine()
        self.assertIn("positive", engine.sentiment_keywords)
        self.assertIn("negative", engine.sentiment_keywords)
        self.assertIsInstance(engine.sentiment_keywords["positive"], list)

    def test_analyze_emotional_context(self):
        """Test emotional context analysis."""
        engine = EmotionalIntelligenceEngine()
        context = engine.analyze_emotional_context("I am so happy and excited!")
        self.assertIsInstance(context, EmotionalContext)
        self.assertIsNotNone(context.primary_emotion)
        self.assertGreaterEqual(context.intensity, 0.0)
        self.assertGreaterEqual(context.confidence, 0.0)


if __name__ == "__main__":
    unittest.main()
