"""
Advanced Context Awareness System for Toga

This module implements sophisticated context understanding including:
- Multi-level context tracking (conversation, session, long-term)
- Context-based response adaptation
- Semantic context analysis
- Context transition detection
"""

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Dict, List, Optional, Set, Tuple
from collections import deque


class ContextLevel(Enum):
    """Levels of context awareness."""
    
    IMMEDIATE = "immediate"  # Current message
    CONVERSATION = "conversation"  # Current conversation thread
    SESSION = "session"  # Current interaction session
    LONG_TERM = "long_term"  # Historical context across sessions


class ContextType(Enum):
    """Types of context."""
    
    TECHNICAL = "technical"
    EMOTIONAL = "emotional"
    CREATIVE = "creative"
    CASUAL = "casual"
    SUPPORT = "support"
    LEARNING = "learning"
    PLANNING = "planning"


@dataclass
class ContextFrame:
    """Single frame of context information."""
    
    timestamp: datetime
    context_type: ContextType
    content: str
    keywords: Set[str]
    emotional_valence: float  # -1.0 (negative) to 1.0 (positive)
    importance: float  # 0.0 to 1.0
    user_id: str
    
    def age_seconds(self) -> float:
        """Get age of context frame in seconds."""
        return (datetime.now() - self.timestamp).total_seconds()
    
    def is_recent(self, threshold_seconds: float = 300) -> bool:
        """Check if context frame is recent."""
        return self.age_seconds() < threshold_seconds


@dataclass
class ConversationContext:
    """Context for a single conversation thread."""
    
    conversation_id: str
    user_id: str
    started_at: datetime
    last_updated: datetime
    frames: deque = field(default_factory=lambda: deque(maxlen=50))
    primary_context_type: ContextType = ContextType.CASUAL
    topic_keywords: Set[str] = field(default_factory=set)
    emotional_trajectory: List[float] = field(default_factory=list)
    
    def add_frame(self, frame: ContextFrame) -> None:
        """Add context frame to conversation."""
        self.frames.append(frame)
        self.last_updated = datetime.now()
        self.topic_keywords.update(frame.keywords)
        self.emotional_trajectory.append(frame.emotional_valence)
        
        # Update primary context type based on recent frames
        if len(self.frames) >= 3:
            recent_types = [f.context_type for f in list(self.frames)[-3:]]
            most_common = max(set(recent_types), key=recent_types.count)
            self.primary_context_type = most_common
    
    def get_recent_frames(self, count: int = 5) -> List[ContextFrame]:
        """Get most recent context frames."""
        return list(self.frames)[-count:]
    
    def get_emotional_trend(self) -> str:
        """Analyze emotional trend in conversation."""
        if len(self.emotional_trajectory) < 3:
            return "neutral"
        
        recent = self.emotional_trajectory[-5:]
        avg_recent = sum(recent) / len(recent)
        
        if len(self.emotional_trajectory) >= 10:
            earlier = self.emotional_trajectory[-10:-5]
            avg_earlier = sum(earlier) / len(earlier)
            
            if avg_recent > avg_earlier + 0.2:
                return "improving"
            elif avg_recent < avg_earlier - 0.2:
                return "declining"
        
        if avg_recent > 0.3:
            return "positive"
        elif avg_recent < -0.3:
            return "negative"
        else:
            return "neutral"
    
    def duration_minutes(self) -> float:
        """Get conversation duration in minutes."""
        return (self.last_updated - self.started_at).total_seconds() / 60


class ContextAwarenessEngine:
    """
    Advanced context awareness engine.
    
    Manages multi-level context tracking and provides context-aware
    response adaptation capabilities.
    """
    
    def __init__(self, max_conversations: int = 100):
        """
        Initialize context awareness engine.
        
        Args:
            max_conversations: Maximum number of conversations to track
        """
        self.max_conversations = max_conversations
        self.conversations: Dict[str, ConversationContext] = {}
        self.long_term_context: Dict[str, List[ContextFrame]] = {}
        
        # Context type keywords for classification
        self.context_keywords = {
            ContextType.TECHNICAL: {
                "code", "function", "error", "debug", "algorithm", "api",
                "database", "server", "bug", "compile", "syntax"
            },
            ContextType.EMOTIONAL: {
                "feel", "emotion", "sad", "happy", "angry", "scared",
                "love", "hate", "worry", "stress", "anxious"
            },
            ContextType.CREATIVE: {
                "create", "design", "art", "imagine", "idea", "story",
                "draw", "write", "compose", "invent"
            },
            ContextType.CASUAL: {
                "hey", "hi", "hello", "chat", "talk", "how", "what",
                "weather", "day", "weekend"
            },
            ContextType.SUPPORT: {
                "help", "problem", "issue", "stuck", "confused", "need",
                "assist", "guide", "advice", "support"
            },
            ContextType.LEARNING: {
                "learn", "teach", "explain", "understand", "study",
                "lesson", "tutorial", "practice", "knowledge"
            },
            ContextType.PLANNING: {
                "plan", "schedule", "organize", "goal", "future",
                "prepare", "strategy", "roadmap", "timeline"
            }
        }
    
    def create_context_frame(
        self,
        user_id: str,
        content: str,
        emotional_valence: float = 0.0,
        importance: float = 0.5
    ) -> ContextFrame:
        """
        Create a context frame from user input.
        
        Args:
            user_id: User identifier
            content: Message content
            emotional_valence: Emotional tone (-1.0 to 1.0)
            importance: Importance score (0.0 to 1.0)
            
        Returns:
            ContextFrame object
        """
        # Extract keywords
        words = content.lower().split()
        keywords = set(w for w in words if len(w) > 3)
        
        # Classify context type
        context_type = self._classify_context_type(content, keywords)
        
        return ContextFrame(
            timestamp=datetime.now(),
            context_type=context_type,
            content=content,
            keywords=keywords,
            emotional_valence=emotional_valence,
            importance=importance,
            user_id=user_id
        )
    
    def _classify_context_type(self, content: str, keywords: Set[str]) -> ContextType:
        """Classify context type based on content and keywords."""
        content_lower = content.lower()
        
        # Score each context type
        scores = {}
        for context_type, type_keywords in self.context_keywords.items():
            score = sum(1 for kw in type_keywords if kw in content_lower)
            scores[context_type] = score
        
        # Return type with highest score, default to CASUAL
        if max(scores.values()) == 0:
            return ContextType.CASUAL
        
        return max(scores, key=scores.get)
    
    def add_to_conversation(
        self,
        conversation_id: str,
        user_id: str,
        frame: ContextFrame
    ) -> ConversationContext:
        """
        Add context frame to conversation.
        
        Args:
            conversation_id: Conversation identifier
            user_id: User identifier
            frame: Context frame to add
            
        Returns:
            Updated conversation context
        """
        if conversation_id not in self.conversations:
            self.conversations[conversation_id] = ConversationContext(
                conversation_id=conversation_id,
                user_id=user_id,
                started_at=datetime.now(),
                last_updated=datetime.now()
            )
        
        conversation = self.conversations[conversation_id]
        conversation.add_frame(frame)
        
        # Also add to long-term context
        if user_id not in self.long_term_context:
            self.long_term_context[user_id] = []
        self.long_term_context[user_id].append(frame)
        
        # Limit long-term context size
        if len(self.long_term_context[user_id]) > 1000:
            self.long_term_context[user_id] = self.long_term_context[user_id][-1000:]
        
        return conversation
    
    def get_context_summary(
        self,
        conversation_id: str,
        levels: List[ContextLevel] = None
    ) -> Dict[str, any]:
        """
        Get comprehensive context summary.
        
        Args:
            conversation_id: Conversation identifier
            levels: Context levels to include (default: all)
            
        Returns:
            Dictionary with context summary
        """
        if conversation_id not in self.conversations:
            return {}
        
        conversation = self.conversations[conversation_id]
        
        if levels is None:
            levels = list(ContextLevel)
        
        summary = {
            "conversation_id": conversation_id,
            "user_id": conversation.user_id,
            "primary_context_type": conversation.primary_context_type.value,
            "duration_minutes": conversation.duration_minutes(),
            "message_count": len(conversation.frames),
            "emotional_trend": conversation.get_emotional_trend(),
        }
        
        if ContextLevel.IMMEDIATE in levels:
            recent = conversation.get_recent_frames(1)
            if recent:
                summary["immediate_context"] = {
                    "type": recent[0].context_type.value,
                    "keywords": list(recent[0].keywords)[:5],
                    "emotional_valence": recent[0].emotional_valence
                }
        
        if ContextLevel.CONVERSATION in levels:
            recent_frames = conversation.get_recent_frames(5)
            summary["conversation_context"] = {
                "recent_types": [f.context_type.value for f in recent_frames],
                "topic_keywords": list(conversation.topic_keywords)[:10],
                "avg_emotional_valence": (
                    sum(f.emotional_valence for f in recent_frames) / len(recent_frames)
                    if recent_frames else 0.0
                )
            }
        
        if ContextLevel.LONG_TERM in levels:
            user_id = conversation.user_id
            if user_id in self.long_term_context:
                long_term_frames = self.long_term_context[user_id][-50:]
                context_type_counts = {}
                for frame in long_term_frames:
                    context_type_counts[frame.context_type.value] = (
                        context_type_counts.get(frame.context_type.value, 0) + 1
                    )
                
                summary["long_term_context"] = {
                    "total_interactions": len(self.long_term_context[user_id]),
                    "context_type_distribution": context_type_counts,
                    "preferred_context": max(context_type_counts, key=context_type_counts.get)
                }
        
        return summary
    
    def detect_context_shift(self, conversation_id: str) -> Optional[Tuple[ContextType, ContextType]]:
        """
        Detect if context has shifted significantly.
        
        Args:
            conversation_id: Conversation identifier
            
        Returns:
            Tuple of (old_context, new_context) if shift detected, None otherwise
        """
        if conversation_id not in self.conversations:
            return None
        
        conversation = self.conversations[conversation_id]
        frames = list(conversation.frames)
        
        if len(frames) < 4:
            return None
        
        # Compare recent context to previous context
        recent_type = frames[-1].context_type
        previous_types = [f.context_type for f in frames[-4:-1]]
        
        if all(t == previous_types[0] for t in previous_types):
            if recent_type != previous_types[0]:
                return (previous_types[0], recent_type)
        
        return None
    
    def get_context_recommendations(self, conversation_id: str) -> List[str]:
        """
        Get recommendations for context-appropriate responses.
        
        Args:
            conversation_id: Conversation identifier
            
        Returns:
            List of recommendation strings
        """
        if conversation_id not in self.conversations:
            return ["Engage in friendly conversation"]
        
        conversation = self.conversations[conversation_id]
        recommendations = []
        
        # Based on primary context type
        context_recommendations = {
            ContextType.TECHNICAL: [
                "Provide precise technical information",
                "Use code examples when relevant",
                "Break down complex concepts"
            ],
            ContextType.EMOTIONAL: [
                "Show empathy and understanding",
                "Validate feelings",
                "Offer emotional support"
            ],
            ContextType.CREATIVE: [
                "Encourage imagination",
                "Suggest creative alternatives",
                "Be playful and expressive"
            ],
            ContextType.CASUAL: [
                "Keep tone light and friendly",
                "Use casual language",
                "Show personality"
            ],
            ContextType.SUPPORT: [
                "Focus on problem-solving",
                "Provide step-by-step guidance",
                "Be patient and encouraging"
            ],
            ContextType.LEARNING: [
                "Explain concepts clearly",
                "Provide examples",
                "Check for understanding"
            ],
            ContextType.PLANNING: [
                "Help organize thoughts",
                "Suggest structured approaches",
                "Focus on actionable steps"
            ]
        }
        
        recommendations.extend(
            context_recommendations.get(conversation.primary_context_type, [])
        )
        
        # Based on emotional trend
        emotional_trend = conversation.get_emotional_trend()
        if emotional_trend == "declining":
            recommendations.append("Be extra supportive and encouraging")
        elif emotional_trend == "improving":
            recommendations.append("Maintain positive momentum")
        
        return recommendations


def create_context_engine() -> ContextAwarenessEngine:
    """Factory function to create context awareness engine."""
    return ContextAwarenessEngine()
