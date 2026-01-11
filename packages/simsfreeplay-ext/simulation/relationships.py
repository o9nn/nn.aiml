"""
Relationship System - Tracking friendship, romance, and family bonds.

Based on the Sims FreePlay relationship system where characters have
friendship (-100 to 100), romance (0 to 100), and family relationships.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, Optional, Callable, List
import uuid


class RelationshipType(Enum):
    """Types of relationships between agents."""
    STRANGER = "stranger"
    ACQUAINTANCE = "acquaintance"
    FRIEND = "friend"
    GOOD_FRIEND = "good_friend"
    BEST_FRIEND = "best_friend"
    ROMANTIC_INTEREST = "romantic_interest"
    PARTNER = "partner"
    SPOUSE = "spouse"
    FAMILY = "family"
    RIVAL = "rival"
    ENEMY = "enemy"


@dataclass
class InteractionRecord:
    """Record of a single interaction between agents."""
    interaction_type: str
    timestamp: float  # Game time
    friendship_change: float
    romance_change: float
    metadata: Dict = field(default_factory=dict)


@dataclass
class Relationship:
    """
    Represents a relationship between two agents.

    Friendship ranges from -100 (enemies) to +100 (best friends).
    Romance ranges from 0 (none) to 100 (deeply in love).
    """
    target_agent_id: str
    friendship: float = 0.0  # -100 to 100
    romance: float = 0.0     # 0 to 100
    is_family: bool = False
    family_relation: Optional[str] = None  # parent, child, sibling, etc.
    interaction_history: List[InteractionRecord] = field(default_factory=list)
    first_met: Optional[float] = None  # Game time when first met

    # Thresholds for relationship states
    ENEMY_THRESHOLD = -50
    RIVAL_THRESHOLD = -20
    ACQUAINTANCE_THRESHOLD = 10
    FRIEND_THRESHOLD = 30
    GOOD_FRIEND_THRESHOLD = 60
    BEST_FRIEND_THRESHOLD = 85
    ROMANTIC_THRESHOLD = 30
    PARTNER_THRESHOLD = 60
    SPOUSE_THRESHOLD = 90

    @property
    def relationship_type(self) -> RelationshipType:
        """Determine the current relationship type based on values."""
        if self.is_family:
            return RelationshipType.FAMILY

        # Check romance first (overrides friendship for romantic types)
        if self.romance >= self.SPOUSE_THRESHOLD:
            return RelationshipType.SPOUSE
        if self.romance >= self.PARTNER_THRESHOLD:
            return RelationshipType.PARTNER
        if self.romance >= self.ROMANTIC_THRESHOLD:
            return RelationshipType.ROMANTIC_INTEREST

        # Then check friendship
        if self.friendship >= self.BEST_FRIEND_THRESHOLD:
            return RelationshipType.BEST_FRIEND
        if self.friendship >= self.GOOD_FRIEND_THRESHOLD:
            return RelationshipType.GOOD_FRIEND
        if self.friendship >= self.FRIEND_THRESHOLD:
            return RelationshipType.FRIEND
        if self.friendship >= self.ACQUAINTANCE_THRESHOLD:
            return RelationshipType.ACQUAINTANCE
        if self.friendship <= self.ENEMY_THRESHOLD:
            return RelationshipType.ENEMY
        if self.friendship <= self.RIVAL_THRESHOLD:
            return RelationshipType.RIVAL

        return RelationshipType.STRANGER

    @property
    def is_positive(self) -> bool:
        """Check if relationship is overall positive."""
        return self.friendship > 0 or self.romance > 0

    @property
    def is_negative(self) -> bool:
        """Check if relationship is overall negative."""
        return self.friendship < 0

    def modify_friendship(self, amount: float) -> float:
        """
        Modify friendship value.

        Args:
            amount: Change in friendship (-100 to 100 total range).

        Returns:
            The new friendship value.
        """
        self.friendship = max(-100.0, min(100.0, self.friendship + amount))
        return self.friendship

    def modify_romance(self, amount: float) -> float:
        """
        Modify romance value.

        Args:
            amount: Change in romance (0 to 100 total range).

        Returns:
            The new romance value.
        """
        self.romance = max(0.0, min(100.0, self.romance + amount))
        return self.romance

    def record_interaction(
        self,
        interaction_type: str,
        timestamp: float,
        friendship_change: float = 0.0,
        romance_change: float = 0.0,
        metadata: Optional[Dict] = None
    ) -> None:
        """Record an interaction and apply its effects."""
        record = InteractionRecord(
            interaction_type=interaction_type,
            timestamp=timestamp,
            friendship_change=friendship_change,
            romance_change=romance_change,
            metadata=metadata or {}
        )
        self.interaction_history.append(record)

        # Keep history manageable
        if len(self.interaction_history) > 100:
            self.interaction_history = self.interaction_history[-100:]

        # Apply effects
        self.modify_friendship(friendship_change)
        self.modify_romance(romance_change)

    def decay(self, delta_time: float, friendship_decay: float = 0.1, romance_decay: float = 0.05) -> None:
        """
        Apply time-based decay to relationship.

        Relationships slowly return toward neutral over time without interaction.
        """
        # Friendship decays toward 0
        if self.friendship > 0:
            self.friendship = max(0, self.friendship - friendship_decay * delta_time)
        elif self.friendship < 0:
            self.friendship = min(0, self.friendship + friendship_decay * delta_time)

        # Romance decays toward 0
        if self.romance > 0:
            self.romance = max(0, self.romance - romance_decay * delta_time)


# Pre-defined interaction effects
INTERACTION_EFFECTS: Dict[str, Dict[str, float]] = {
    "greet": {"friendship": 2.0, "romance": 0.0},
    "chat": {"friendship": 5.0, "romance": 0.0},
    "deep_conversation": {"friendship": 10.0, "romance": 2.0},
    "joke": {"friendship": 3.0, "romance": 1.0},
    "compliment": {"friendship": 5.0, "romance": 3.0},
    "flirt": {"friendship": 2.0, "romance": 8.0},
    "hug": {"friendship": 8.0, "romance": 5.0},
    "kiss": {"friendship": 5.0, "romance": 15.0},
    "gift": {"friendship": 10.0, "romance": 5.0},
    "help": {"friendship": 8.0, "romance": 2.0},
    "argue": {"friendship": -15.0, "romance": -5.0},
    "insult": {"friendship": -20.0, "romance": -10.0},
    "betray": {"friendship": -50.0, "romance": -30.0},
    "apologize": {"friendship": 10.0, "romance": 3.0},
    "work_together": {"friendship": 7.0, "romance": 0.0},
    "play_together": {"friendship": 8.0, "romance": 2.0},
}


class RelationshipSystem:
    """
    Manages all relationships for an agent.

    Tracks relationships with other agents, handles interaction effects,
    and provides methods for relationship queries.
    """

    def __init__(self, agent_id: str):
        """
        Initialize the relationship system.

        Args:
            agent_id: The ID of the owning agent.
        """
        self._agent_id = agent_id
        self._relationships: Dict[str, Relationship] = {}
        self._callbacks: Dict[str, list[Callable]] = {
            "on_relationship_change": [],
            "on_new_relationship": [],
            "on_type_change": [],
        }
        self._current_time: float = 0.0

    def get_relationship(self, target_id: str) -> Optional[Relationship]:
        """Get relationship with a specific agent."""
        return self._relationships.get(target_id)

    def get_or_create_relationship(self, target_id: str) -> Relationship:
        """Get or create a relationship with an agent."""
        if target_id not in self._relationships:
            relationship = Relationship(
                target_agent_id=target_id,
                first_met=self._current_time
            )
            self._relationships[target_id] = relationship
            self._trigger_callbacks("on_new_relationship", relationship)
        return self._relationships[target_id]

    def interact(
        self,
        target_id: str,
        interaction_type: str,
        custom_effects: Optional[Dict[str, float]] = None
    ) -> Relationship:
        """
        Record an interaction with another agent.

        Args:
            target_id: ID of the agent being interacted with.
            interaction_type: Type of interaction (e.g., "chat", "flirt").
            custom_effects: Optional custom effects override.

        Returns:
            The updated relationship.
        """
        relationship = self.get_or_create_relationship(target_id)
        old_type = relationship.relationship_type

        # Get effects
        effects = custom_effects or INTERACTION_EFFECTS.get(interaction_type, {})
        friendship_change = effects.get("friendship", 0.0)
        romance_change = effects.get("romance", 0.0)

        # Record interaction
        relationship.record_interaction(
            interaction_type=interaction_type,
            timestamp=self._current_time,
            friendship_change=friendship_change,
            romance_change=romance_change
        )

        self._trigger_callbacks("on_relationship_change", relationship, interaction_type)

        # Check for type change
        if relationship.relationship_type != old_type:
            self._trigger_callbacks(
                "on_type_change",
                relationship,
                old_type,
                relationship.relationship_type
            )

        return relationship

    def update(self, delta_time: float) -> None:
        """
        Update all relationships with time-based decay.

        Args:
            delta_time: Time elapsed in game hours.
        """
        self._current_time += delta_time
        for relationship in self._relationships.values():
            old_type = relationship.relationship_type
            relationship.decay(delta_time)

            if relationship.relationship_type != old_type:
                self._trigger_callbacks(
                    "on_type_change",
                    relationship,
                    old_type,
                    relationship.relationship_type
                )

    def get_friends(self) -> List[Relationship]:
        """Get all relationships at friend level or above."""
        return [
            r for r in self._relationships.values()
            if r.friendship >= Relationship.FRIEND_THRESHOLD
        ]

    def get_enemies(self) -> List[Relationship]:
        """Get all relationships at enemy level."""
        return [
            r for r in self._relationships.values()
            if r.friendship <= Relationship.ENEMY_THRESHOLD
        ]

    def get_romantic_interests(self) -> List[Relationship]:
        """Get all relationships with romantic interest."""
        return [
            r for r in self._relationships.values()
            if r.romance >= Relationship.ROMANTIC_THRESHOLD
        ]

    def get_family(self) -> List[Relationship]:
        """Get all family relationships."""
        return [r for r in self._relationships.values() if r.is_family]

    def get_all_relationships(self) -> Dict[str, Relationship]:
        """Get all relationships."""
        return self._relationships.copy()

    def get_relationship_summary(self) -> Dict[RelationshipType, int]:
        """Get count of relationships by type."""
        summary: Dict[RelationshipType, int] = {rt: 0 for rt in RelationshipType}
        for rel in self._relationships.values():
            summary[rel.relationship_type] += 1
        return summary

    def set_family(
        self,
        target_id: str,
        family_relation: str
    ) -> Relationship:
        """
        Mark a relationship as family.

        Args:
            target_id: ID of the family member.
            family_relation: Type of family relation (parent, child, sibling, etc.).
        """
        relationship = self.get_or_create_relationship(target_id)
        relationship.is_family = True
        relationship.family_relation = family_relation
        relationship.friendship = max(relationship.friendship, 50.0)
        return relationship

    def on_relationship_change(self, callback: Callable) -> None:
        """Register callback for relationship changes."""
        self._callbacks["on_relationship_change"].append(callback)

    def on_new_relationship(self, callback: Callable) -> None:
        """Register callback for new relationships."""
        self._callbacks["on_new_relationship"].append(callback)

    def on_type_change(self, callback: Callable) -> None:
        """Register callback for relationship type changes."""
        self._callbacks["on_type_change"].append(callback)

    def _trigger_callbacks(self, event: str, *args) -> None:
        """Trigger all callbacks for an event."""
        for callback in self._callbacks.get(event, []):
            callback(*args)

    def to_dict(self) -> Dict:
        """Serialize relationships to dictionary."""
        return {
            target_id: {
                "friendship": rel.friendship,
                "romance": rel.romance,
                "is_family": rel.is_family,
                "family_relation": rel.family_relation,
                "first_met": rel.first_met,
            }
            for target_id, rel in self._relationships.items()
        }

    def from_dict(self, data: Dict) -> None:
        """Load relationships from dictionary."""
        for target_id, rel_data in data.items():
            relationship = self.get_or_create_relationship(target_id)
            relationship.friendship = rel_data.get("friendship", 0.0)
            relationship.romance = rel_data.get("romance", 0.0)
            relationship.is_family = rel_data.get("is_family", False)
            relationship.family_relation = rel_data.get("family_relation")
            relationship.first_met = rel_data.get("first_met")
