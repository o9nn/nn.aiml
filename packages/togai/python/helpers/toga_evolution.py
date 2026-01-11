"""
Advanced Personality Evolution System for Toga

This module implements sophisticated personality evolution mechanisms including:
- Adaptive trait evolution based on interaction patterns
- Long-term personality development tracking
- Relationship-based personality adjustments
- Context-aware personality modulation
"""

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Dict, List, Optional, Tuple
import math


class EvolutionTrigger(Enum):
    """Triggers for personality evolution."""
    
    INTERACTION_COUNT = "interaction_count"
    EMOTIONAL_INTENSITY = "emotional_intensity"
    RELATIONSHIP_MILESTONE = "relationship_milestone"
    CONTEXT_PATTERN = "context_pattern"
    TIME_BASED = "time_based"


class PersonalityDimension(Enum):
    """Dimensions of personality that can evolve."""
    
    OPENNESS = "openness"
    CONSCIENTIOUSNESS = "conscientiousness"
    EXTRAVERSION = "extraversion"
    AGREEABLENESS = "agreeableness"
    EMOTIONAL_STABILITY = "emotional_stability"


@dataclass
class EvolutionEvent:
    """Record of a personality evolution event."""
    
    timestamp: datetime
    trigger: EvolutionTrigger
    dimension: PersonalityDimension
    old_value: float
    new_value: float
    delta: float
    reason: str
    confidence: float = 0.8


@dataclass
class PersonalityProfile:
    """Complete personality profile with evolution tracking."""
    
    user_id: str
    dimensions: Dict[PersonalityDimension, float] = field(default_factory=dict)
    evolution_history: List[EvolutionEvent] = field(default_factory=list)
    interaction_count: int = 0
    relationship_level: int = 1
    created_at: datetime = field(default_factory=datetime.now)
    last_updated: datetime = field(default_factory=datetime.now)
    
    def __post_init__(self):
        """Initialize default dimension values."""
        if not self.dimensions:
            self.dimensions = {
                PersonalityDimension.OPENNESS: 0.7,
                PersonalityDimension.CONSCIENTIOUSNESS: 0.6,
                PersonalityDimension.EXTRAVERSION: 0.8,
                PersonalityDimension.AGREEABLENESS: 0.75,
                PersonalityDimension.EMOTIONAL_STABILITY: 0.65,
            }


class PersonalityEvolutionEngine:
    """
    Advanced personality evolution engine.
    
    Manages long-term personality development through adaptive learning
    and context-aware trait adjustments.
    """
    
    def __init__(self, learning_rate: float = 0.01, stability_factor: float = 0.9):
        """
        Initialize evolution engine.
        
        Args:
            learning_rate: Rate of personality adaptation (0.0 to 1.0)
            stability_factor: Resistance to change (0.0 to 1.0, higher = more stable)
        """
        self.learning_rate = learning_rate
        self.stability_factor = stability_factor
        self.profiles: Dict[str, PersonalityProfile] = {}
    
    def get_or_create_profile(self, user_id: str) -> PersonalityProfile:
        """Get existing profile or create new one."""
        if user_id not in self.profiles:
            self.profiles[user_id] = PersonalityProfile(user_id=user_id)
        return self.profiles[user_id]
    
    def evolve_from_interaction(
        self,
        user_id: str,
        interaction_type: str,
        emotional_intensity: float,
        context: str
    ) -> Dict[PersonalityDimension, float]:
        """
        Evolve personality based on interaction.
        
        Args:
            user_id: User identifier
            interaction_type: Type of interaction (chat, support, creative, etc.)
            emotional_intensity: Intensity of emotional exchange (0.0 to 1.0)
            context: Context of interaction
            
        Returns:
            Dictionary of evolved dimension values
        """
        profile = self.get_or_create_profile(user_id)
        profile.interaction_count += 1
        profile.last_updated = datetime.now()
        
        # Determine which dimensions to evolve based on interaction type
        evolution_map = self._get_evolution_map(interaction_type)
        
        for dimension, influence in evolution_map.items():
            if dimension in profile.dimensions:
                old_value = profile.dimensions[dimension]
                
                # Calculate evolution delta with stability factor
                base_delta = influence * emotional_intensity * self.learning_rate
                stabilized_delta = base_delta * (1.0 - self.stability_factor)
                
                # Apply bounds (0.0 to 1.0)
                new_value = max(0.0, min(1.0, old_value + stabilized_delta))
                
                # Record evolution event if significant change
                if abs(new_value - old_value) > 0.001:
                    event = EvolutionEvent(
                        timestamp=datetime.now(),
                        trigger=EvolutionTrigger.INTERACTION_COUNT,
                        dimension=dimension,
                        old_value=old_value,
                        new_value=new_value,
                        delta=new_value - old_value,
                        reason=f"Interaction type: {interaction_type}, intensity: {emotional_intensity:.2f}",
                        confidence=min(0.95, 0.5 + (profile.interaction_count / 200))
                    )
                    profile.evolution_history.append(event)
                    profile.dimensions[dimension] = new_value
        
        # Check for relationship milestones
        self._check_relationship_milestones(profile)
        
        return profile.dimensions
    
    def _get_evolution_map(self, interaction_type: str) -> Dict[PersonalityDimension, float]:
        """Map interaction types to personality dimension influences."""
        evolution_maps = {
            "chat": {
                PersonalityDimension.EXTRAVERSION: 0.1,
                PersonalityDimension.AGREEABLENESS: 0.05,
            },
            "support": {
                PersonalityDimension.AGREEABLENESS: 0.15,
                PersonalityDimension.EMOTIONAL_STABILITY: 0.1,
            },
            "creative": {
                PersonalityDimension.OPENNESS: 0.2,
                PersonalityDimension.EXTRAVERSION: 0.1,
            },
            "technical": {
                PersonalityDimension.CONSCIENTIOUSNESS: 0.15,
                PersonalityDimension.OPENNESS: 0.1,
            },
            "emotional": {
                PersonalityDimension.EMOTIONAL_STABILITY: 0.2,
                PersonalityDimension.AGREEABLENESS: 0.15,
            }
        }
        return evolution_maps.get(interaction_type, {})
    
    def _check_relationship_milestones(self, profile: PersonalityProfile) -> None:
        """Check and apply relationship milestone bonuses."""
        milestones = {
            10: (PersonalityDimension.AGREEABLENESS, 0.05),
            50: (PersonalityDimension.EMOTIONAL_STABILITY, 0.1),
            100: (PersonalityDimension.OPENNESS, 0.1),
            200: (PersonalityDimension.EXTRAVERSION, 0.1),
            500: (PersonalityDimension.CONSCIENTIOUSNESS, 0.1),
        }
        
        for milestone, (dimension, bonus) in milestones.items():
            if profile.interaction_count == milestone:
                old_value = profile.dimensions[dimension]
                new_value = min(1.0, old_value + bonus)
                
                event = EvolutionEvent(
                    timestamp=datetime.now(),
                    trigger=EvolutionTrigger.RELATIONSHIP_MILESTONE,
                    dimension=dimension,
                    old_value=old_value,
                    new_value=new_value,
                    delta=bonus,
                    reason=f"Reached {milestone} interactions milestone",
                    confidence=1.0
                )
                profile.evolution_history.append(event)
                profile.dimensions[dimension] = new_value
                profile.relationship_level = milestone // 50 + 1
    
    def get_personality_summary(self, user_id: str) -> Dict[str, any]:
        """Get comprehensive personality summary."""
        if user_id not in self.profiles:
            return {}
        
        profile = self.profiles[user_id]
        
        return {
            "user_id": user_id,
            "dimensions": {dim.value: val for dim, val in profile.dimensions.items()},
            "interaction_count": profile.interaction_count,
            "relationship_level": profile.relationship_level,
            "evolution_events": len(profile.evolution_history),
            "days_since_creation": (datetime.now() - profile.created_at).days,
            "last_updated": profile.last_updated.isoformat(),
            "personality_type": self._classify_personality_type(profile),
        }
    
    def _classify_personality_type(self, profile: PersonalityProfile) -> str:
        """Classify personality type based on dominant dimensions."""
        dims = profile.dimensions
        
        # Simple classification based on highest dimensions
        if dims[PersonalityDimension.OPENNESS] > 0.7 and dims[PersonalityDimension.EXTRAVERSION] > 0.7:
            return "Enthusiastic Explorer"
        elif dims[PersonalityDimension.CONSCIENTIOUSNESS] > 0.7 and dims[PersonalityDimension.EMOTIONAL_STABILITY] > 0.7:
            return "Reliable Companion"
        elif dims[PersonalityDimension.AGREEABLENESS] > 0.7 and dims[PersonalityDimension.EMOTIONAL_STABILITY] > 0.7:
            return "Supportive Friend"
        elif dims[PersonalityDimension.OPENNESS] > 0.7:
            return "Creative Spirit"
        elif dims[PersonalityDimension.EXTRAVERSION] > 0.7:
            return "Social Butterfly"
        else:
            return "Balanced Personality"
    
    def get_evolution_trajectory(self, user_id: str, dimension: PersonalityDimension) -> List[Tuple[datetime, float]]:
        """Get evolution trajectory for a specific dimension."""
        if user_id not in self.profiles:
            return []
        
        profile = self.profiles[user_id]
        trajectory = []
        
        # Add initial value
        if profile.evolution_history:
            first_event = profile.evolution_history[0]
            trajectory.append((profile.created_at, first_event.old_value))
        
        # Add all evolution events for this dimension
        for event in profile.evolution_history:
            if event.dimension == dimension:
                trajectory.append((event.timestamp, event.new_value))
        
        return trajectory
    
    def predict_future_evolution(
        self,
        user_id: str,
        dimension: PersonalityDimension,
        days_ahead: int = 30
    ) -> float:
        """
        Predict future personality dimension value.
        
        Args:
            user_id: User identifier
            dimension: Dimension to predict
            days_ahead: Number of days to predict ahead
            
        Returns:
            Predicted dimension value
        """
        if user_id not in self.profiles:
            return 0.5
        
        profile = self.profiles[user_id]
        trajectory = self.get_evolution_trajectory(user_id, dimension)
        
        if len(trajectory) < 2:
            return profile.dimensions.get(dimension, 0.5)
        
        # Simple linear extrapolation
        recent_events = [e for e in profile.evolution_history if e.dimension == dimension][-5:]
        if not recent_events:
            return profile.dimensions[dimension]
        
        avg_delta = sum(e.delta for e in recent_events) / len(recent_events)
        interactions_per_day = profile.interaction_count / max(1, (datetime.now() - profile.created_at).days)
        predicted_interactions = interactions_per_day * days_ahead
        
        predicted_value = profile.dimensions[dimension] + (avg_delta * predicted_interactions)
        return max(0.0, min(1.0, predicted_value))
    
    def get_compatibility_score(self, user_id1: str, user_id2: str) -> float:
        """
        Calculate personality compatibility between two users.
        
        Args:
            user_id1: First user identifier
            user_id2: Second user identifier
            
        Returns:
            Compatibility score (0.0 to 1.0)
        """
        if user_id1 not in self.profiles or user_id2 not in self.profiles:
            return 0.5
        
        profile1 = self.profiles[user_id1]
        profile2 = self.profiles[user_id2]
        
        # Calculate Euclidean distance in personality space
        total_distance = 0.0
        for dimension in PersonalityDimension:
            val1 = profile1.dimensions.get(dimension, 0.5)
            val2 = profile2.dimensions.get(dimension, 0.5)
            total_distance += (val1 - val2) ** 2
        
        distance = math.sqrt(total_distance)
        max_distance = math.sqrt(len(PersonalityDimension))
        
        # Convert distance to similarity score
        compatibility = 1.0 - (distance / max_distance)
        return compatibility


def create_evolution_engine(learning_rate: float = 0.01) -> PersonalityEvolutionEngine:
    """Factory function to create evolution engine with default settings."""
    return PersonalityEvolutionEngine(learning_rate=learning_rate, stability_factor=0.9)
