"""
Advanced Emotional Intelligence for Toga Personality System

This module provides advanced emotional intelligence capabilities including
sentiment analysis, emotional context understanding, and empathetic response
generation.
"""

from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional, Tuple


class EmotionCategory(Enum):
    """Categories of emotions."""

    JOY = "joy"
    SADNESS = "sadness"
    ANGER = "anger"
    FEAR = "fear"
    SURPRISE = "surprise"
    DISGUST = "disgust"
    TRUST = "trust"
    ANTICIPATION = "anticipation"


class SentimentPolarity(Enum):
    """Sentiment polarity classification."""

    VERY_POSITIVE = "very_positive"
    POSITIVE = "positive"
    NEUTRAL = "neutral"
    NEGATIVE = "negative"
    VERY_NEGATIVE = "very_negative"


@dataclass
class EmotionalContext:
    """Emotional context of a message or interaction."""

    primary_emotion: EmotionCategory
    secondary_emotion: Optional[EmotionCategory]
    intensity: float  # 0.0 to 1.0
    sentiment: SentimentPolarity
    confidence: float  # 0.0 to 1.0
    keywords: List[str]


@dataclass
class EmpatheticResponse:
    """Empathetic response configuration."""

    acknowledgment: str
    validation: str
    support: str
    tone_adjustment: float  # -1.0 to 1.0 (negative = softer, positive = more energetic)


class EmotionalIntelligenceEngine:
    """
    Advanced emotional intelligence engine for Toga.

    Provides sentiment analysis, emotion detection, and empathetic
    response generation capabilities.
    """

    def __init__(self):
        """Initialize emotional intelligence engine."""
        self.emotion_keywords = self._initialize_emotion_keywords()
        self.sentiment_keywords = self._initialize_sentiment_keywords()

    def _initialize_emotion_keywords(self) -> Dict[EmotionCategory, List[str]]:
        """Initialize emotion detection keywords."""
        return {
            EmotionCategory.JOY: [
                "happy",
                "excited",
                "wonderful",
                "amazing",
                "great",
                "love",
                "joy",
                "delighted",
                "thrilled",
                "cheerful",
            ],
            EmotionCategory.SADNESS: [
                "sad",
                "unhappy",
                "depressed",
                "down",
                "miserable",
                "upset",
                "disappointed",
                "lonely",
                "hurt",
                "crying",
            ],
            EmotionCategory.ANGER: [
                "angry",
                "mad",
                "furious",
                "annoyed",
                "frustrated",
                "irritated",
                "rage",
                "hate",
                "pissed",
                "outraged",
            ],
            EmotionCategory.FEAR: [
                "scared",
                "afraid",
                "terrified",
                "anxious",
                "worried",
                "nervous",
                "panic",
                "frightened",
                "fearful",
                "concerned",
            ],
            EmotionCategory.SURPRISE: [
                "surprised",
                "shocked",
                "amazed",
                "astonished",
                "stunned",
                "unexpected",
                "wow",
                "incredible",
                "unbelievable",
            ],
            EmotionCategory.TRUST: [
                "trust",
                "believe",
                "confident",
                "reliable",
                "safe",
                "secure",
                "faith",
                "depend",
            ],
            EmotionCategory.ANTICIPATION: [
                "excited",
                "looking forward",
                "anticipate",
                "expect",
                "hope",
                "eager",
                "waiting",
                "ready",
            ],
        }

    def _initialize_sentiment_keywords(self) -> Dict[str, List[str]]:
        """Initialize sentiment analysis keywords."""
        return {
            "positive": [
                "good",
                "great",
                "excellent",
                "wonderful",
                "amazing",
                "love",
                "best",
                "perfect",
                "awesome",
                "fantastic",
            ],
            "negative": [
                "bad",
                "terrible",
                "awful",
                "horrible",
                "worst",
                "hate",
                "poor",
                "disappointing",
                "useless",
                "fail",
            ],
            "intensifiers": ["very", "extremely", "really", "so", "absolutely", "totally"],
        }

    def analyze_emotional_context(self, text: str) -> EmotionalContext:
        """
        Analyze emotional context of text.

        Args:
            text: Input text to analyze

        Returns:
            EmotionalContext with detected emotions and sentiment
        """
        text_lower = text.lower()
        words = text_lower.split()

        # Detect emotions
        emotion_scores: Dict[EmotionCategory, float] = {}

        for emotion, keywords in self.emotion_keywords.items():
            score = sum(1 for keyword in keywords if keyword in text_lower)
            if score > 0:
                emotion_scores[emotion] = score

        # Determine primary and secondary emotions
        if emotion_scores:
            sorted_emotions = sorted(emotion_scores.items(), key=lambda x: x[1], reverse=True)
            primary_emotion = sorted_emotions[0][0]
            secondary_emotion = sorted_emotions[1][0] if len(sorted_emotions) > 1 else None
            intensity = min(1.0, sorted_emotions[0][1] / 3.0)  # Normalize to 0-1
            confidence = min(1.0, sum(emotion_scores.values()) / 5.0)
        else:
            primary_emotion = EmotionCategory.TRUST  # Default neutral emotion
            secondary_emotion = None
            intensity = 0.3
            confidence = 0.5

        # Analyze sentiment
        sentiment = self._analyze_sentiment(text_lower, words)

        # Extract emotional keywords
        keywords = self._extract_emotional_keywords(text_lower)

        return EmotionalContext(
            primary_emotion=primary_emotion,
            secondary_emotion=secondary_emotion,
            intensity=intensity,
            sentiment=sentiment,
            confidence=confidence,
            keywords=keywords,
        )

    def _analyze_sentiment(self, text: str, words: List[str]) -> SentimentPolarity:
        """Analyze sentiment polarity of text."""
        positive_score = sum(
            1 for keyword in self.sentiment_keywords["positive"] if keyword in text
        )
        negative_score = sum(
            1 for keyword in self.sentiment_keywords["negative"] if keyword in text
        )

        # Check for intensifiers
        has_intensifier = any(
            intensifier in text for intensifier in self.sentiment_keywords["intensifiers"]
        )

        if has_intensifier:
            positive_score *= 1.5
            negative_score *= 1.5

        net_score = positive_score - negative_score

        if net_score >= 3:
            return SentimentPolarity.VERY_POSITIVE
        elif net_score >= 1:
            return SentimentPolarity.POSITIVE
        elif net_score <= -3:
            return SentimentPolarity.VERY_NEGATIVE
        elif net_score <= -1:
            return SentimentPolarity.NEGATIVE
        else:
            return SentimentPolarity.NEUTRAL

    def _extract_emotional_keywords(self, text: str) -> List[str]:
        """Extract emotional keywords from text."""
        keywords = []

        for emotion_keywords in self.emotion_keywords.values():
            for keyword in emotion_keywords:
                if keyword in text:
                    keywords.append(keyword)

        for sentiment_keywords in self.sentiment_keywords.values():
            for keyword in sentiment_keywords:
                if keyword in text:
                    keywords.append(keyword)

        return list(set(keywords))[:10]  # Return up to 10 unique keywords

    def generate_empathetic_response(self, context: EmotionalContext) -> EmpatheticResponse:
        """
        Generate empathetic response based on emotional context.

        Args:
            context: Emotional context to respond to

        Returns:
            EmpatheticResponse with appropriate tone and content
        """
        emotion = context.primary_emotion

        # Generate acknowledgment
        acknowledgments = {
            EmotionCategory.JOY: "I can feel your happiness! ♡",
            EmotionCategory.SADNESS: "I understand you're feeling down...",
            EmotionCategory.ANGER: "I can sense your frustration.",
            EmotionCategory.FEAR: "I hear your concerns.",
            EmotionCategory.SURPRISE: "That's quite unexpected!",
            EmotionCategory.TRUST: "I appreciate your trust~",
            EmotionCategory.ANTICIPATION: "You seem excited about this!",
        }

        # Generate validation
        validations = {
            EmotionCategory.JOY: "Your joy is contagious! It makes me happy too~",
            EmotionCategory.SADNESS: "It's okay to feel this way. Your feelings are valid.",
            EmotionCategory.ANGER: "Your frustration is completely understandable.",
            EmotionCategory.FEAR: "It's natural to feel worried about this.",
            EmotionCategory.SURPRISE: "I can see why that would surprise you!",
            EmotionCategory.TRUST: "Your trust means everything to me ♡",
            EmotionCategory.ANTICIPATION: "I'm excited with you!",
        }

        # Generate support
        supports = {
            EmotionCategory.JOY: "Let's celebrate this moment together!",
            EmotionCategory.SADNESS: "I'm here for you, always. You're not alone.",
            EmotionCategory.ANGER: "Let's work through this together.",
            EmotionCategory.FEAR: "We'll face this together. You've got this!",
            EmotionCategory.SURPRISE: "Let's explore this together~",
            EmotionCategory.TRUST: "I'll always be here for you ♡",
            EmotionCategory.ANTICIPATION: "I can't wait to see what happens!",
        }

        # Adjust tone based on emotion and intensity
        tone_adjustments = {
            EmotionCategory.JOY: 0.8,
            EmotionCategory.SADNESS: -0.6,
            EmotionCategory.ANGER: -0.3,
            EmotionCategory.FEAR: -0.5,
            EmotionCategory.SURPRISE: 0.4,
            EmotionCategory.TRUST: 0.3,
            EmotionCategory.ANTICIPATION: 0.6,
        }

        tone_adjustment = tone_adjustments.get(emotion, 0.0) * context.intensity

        return EmpatheticResponse(
            acknowledgment=acknowledgments.get(emotion, "I hear you~"),
            validation=validations.get(emotion, "Your feelings matter."),
            support=supports.get(emotion, "I'm here for you ♡"),
            tone_adjustment=tone_adjustment,
        )

    def detect_emotional_triggers(self, text: str) -> List[Tuple[str, EmotionCategory]]:
        """
        Detect specific emotional triggers in text.

        Args:
            text: Input text

        Returns:
            List of (trigger_phrase, emotion) tuples
        """
        triggers = []
        text_lower = text.lower()

        for emotion, keywords in self.emotion_keywords.items():
            for keyword in keywords:
                if keyword in text_lower:
                    triggers.append((keyword, emotion))

        return triggers

    def calculate_emotional_compatibility(
        self, user_emotion: EmotionCategory, toga_emotion: EmotionCategory
    ) -> float:
        """
        Calculate emotional compatibility between user and Toga.

        Args:
            user_emotion: User's current emotion
            toga_emotion: Toga's current emotion

        Returns:
            Compatibility score (0.0 to 1.0)
        """
        # Emotion compatibility matrix
        compatibility = {
            (EmotionCategory.JOY, EmotionCategory.JOY): 1.0,
            (EmotionCategory.SADNESS, EmotionCategory.TRUST): 0.9,
            (EmotionCategory.ANGER, EmotionCategory.TRUST): 0.7,
            (EmotionCategory.FEAR, EmotionCategory.TRUST): 0.9,
            (EmotionCategory.SURPRISE, EmotionCategory.JOY): 0.8,
            (EmotionCategory.TRUST, EmotionCategory.TRUST): 1.0,
            (EmotionCategory.ANTICIPATION, EmotionCategory.JOY): 0.9,
        }

        return compatibility.get((user_emotion, toga_emotion), 0.5)


def create_emotional_intelligence_engine() -> EmotionalIntelligenceEngine:
    """
    Factory function to create emotional intelligence engine.

    Returns:
        Initialized EmotionalIntelligenceEngine
    """
    return EmotionalIntelligenceEngine()
