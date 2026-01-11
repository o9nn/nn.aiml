"""Tests for the Relationship System."""

import pytest
from simulation.relationships import (
    RelationshipSystem, Relationship, RelationshipType,
    InteractionRecord, INTERACTION_EFFECTS
)


class TestRelationship:
    """Tests for the Relationship class."""

    def test_initial_values(self):
        """Test relationship initialization."""
        rel = Relationship(target_agent_id="agent_123")
        assert rel.friendship == 0.0
        assert rel.romance == 0.0
        assert rel.relationship_type == RelationshipType.STRANGER

    def test_modify_friendship(self):
        """Test modifying friendship value."""
        rel = Relationship(target_agent_id="agent_123")

        rel.modify_friendship(50.0)
        assert rel.friendship == 50.0

        rel.modify_friendship(100.0)
        assert rel.friendship == 100.0  # Clamped to max

        rel.modify_friendship(-250.0)
        assert rel.friendship == -100.0  # Clamped to min

    def test_modify_romance(self):
        """Test modifying romance value."""
        rel = Relationship(target_agent_id="agent_123")

        rel.modify_romance(50.0)
        assert rel.romance == 50.0

        rel.modify_romance(100.0)
        assert rel.romance == 100.0  # Clamped to max

        rel.modify_romance(-200.0)
        assert rel.romance == 0.0  # Clamped to min (no negative romance)

    def test_relationship_type_stranger(self):
        """Test stranger relationship type."""
        rel = Relationship(target_agent_id="agent_123", friendship=5.0)
        assert rel.relationship_type == RelationshipType.STRANGER

    def test_relationship_type_friend(self):
        """Test friend relationship type."""
        rel = Relationship(target_agent_id="agent_123", friendship=35.0)
        assert rel.relationship_type == RelationshipType.FRIEND

    def test_relationship_type_best_friend(self):
        """Test best friend relationship type."""
        rel = Relationship(target_agent_id="agent_123", friendship=90.0)
        assert rel.relationship_type == RelationshipType.BEST_FRIEND

    def test_relationship_type_enemy(self):
        """Test enemy relationship type."""
        rel = Relationship(target_agent_id="agent_123", friendship=-60.0)
        assert rel.relationship_type == RelationshipType.ENEMY

    def test_relationship_type_romantic(self):
        """Test romantic relationship types override friendship."""
        rel = Relationship(target_agent_id="agent_123", friendship=90.0, romance=65.0)
        assert rel.relationship_type == RelationshipType.PARTNER

    def test_relationship_type_family(self):
        """Test family relationship type overrides all."""
        rel = Relationship(
            target_agent_id="agent_123",
            friendship=-50.0,
            is_family=True
        )
        assert rel.relationship_type == RelationshipType.FAMILY

    def test_record_interaction(self):
        """Test recording an interaction."""
        rel = Relationship(target_agent_id="agent_123")

        rel.record_interaction(
            interaction_type="chat",
            timestamp=1.0,
            friendship_change=10.0,
            romance_change=2.0
        )

        assert len(rel.interaction_history) == 1
        assert rel.friendship == 10.0
        assert rel.romance == 2.0

    def test_decay(self):
        """Test relationship decay over time."""
        rel = Relationship(target_agent_id="agent_123", friendship=50.0, romance=30.0)

        rel.decay(10.0, friendship_decay=0.5, romance_decay=0.2)

        assert rel.friendship == 45.0
        assert rel.romance == 28.0

    def test_decay_toward_zero(self):
        """Test decay moves values toward zero."""
        rel_pos = Relationship(target_agent_id="a", friendship=5.0)
        rel_neg = Relationship(target_agent_id="b", friendship=-5.0)

        rel_pos.decay(100.0, friendship_decay=0.1)
        rel_neg.decay(100.0, friendship_decay=0.1)

        assert rel_pos.friendship == 0.0
        assert rel_neg.friendship == 0.0

    def test_is_positive(self):
        """Test positive relationship detection."""
        rel = Relationship(target_agent_id="agent_123", friendship=10.0)
        assert rel.is_positive

        rel.friendship = -10.0
        rel.romance = 5.0
        assert rel.is_positive

    def test_is_negative(self):
        """Test negative relationship detection."""
        rel = Relationship(target_agent_id="agent_123", friendship=-10.0)
        assert rel.is_negative


class TestRelationshipSystem:
    """Tests for the RelationshipSystem class."""

    def test_initialization(self):
        """Test system initialization."""
        system = RelationshipSystem(agent_id="agent_001")
        assert len(system.get_all_relationships()) == 0

    def test_get_or_create_relationship(self):
        """Test getting or creating a relationship."""
        system = RelationshipSystem(agent_id="agent_001")

        rel1 = system.get_or_create_relationship("agent_002")
        rel2 = system.get_or_create_relationship("agent_002")

        assert rel1 is rel2
        assert len(system.get_all_relationships()) == 1

    def test_interact(self):
        """Test interaction recording."""
        system = RelationshipSystem(agent_id="agent_001")

        rel = system.interact("agent_002", "chat")

        assert rel.friendship == 5.0  # From INTERACTION_EFFECTS
        assert rel.romance == 0.0

    def test_interact_flirt(self):
        """Test flirt interaction."""
        system = RelationshipSystem(agent_id="agent_001")

        rel = system.interact("agent_002", "flirt")

        assert rel.friendship == 2.0
        assert rel.romance == 8.0

    def test_interact_custom_effects(self):
        """Test interaction with custom effects."""
        system = RelationshipSystem(agent_id="agent_001")

        rel = system.interact(
            "agent_002",
            "custom",
            custom_effects={"friendship": 25.0, "romance": 15.0}
        )

        assert rel.friendship == 25.0
        assert rel.romance == 15.0

    def test_update_decay(self):
        """Test system update applies decay."""
        system = RelationshipSystem(agent_id="agent_001")
        rel = system.get_or_create_relationship("agent_002")
        rel.modify_friendship(50.0)

        system.update(100.0)  # Long time

        assert rel.friendship < 50.0

    def test_get_friends(self):
        """Test getting friend relationships."""
        system = RelationshipSystem(agent_id="agent_001")

        # Create some relationships
        system.interact("agent_002", "chat")  # Low friendship
        for _ in range(10):  # Build up friendship
            system.interact("agent_003", "deep_conversation")

        friends = system.get_friends()
        assert len(friends) >= 1

    def test_get_enemies(self):
        """Test getting enemy relationships."""
        system = RelationshipSystem(agent_id="agent_001")

        # Create enemy relationship
        for _ in range(5):
            system.interact("agent_002", "insult")

        enemies = system.get_enemies()
        assert len(enemies) == 1

    def test_get_romantic_interests(self):
        """Test getting romantic relationships."""
        system = RelationshipSystem(agent_id="agent_001")

        # Build romance
        for _ in range(5):
            system.interact("agent_002", "flirt")

        romantic = system.get_romantic_interests()
        assert len(romantic) == 1

    def test_set_family(self):
        """Test setting family relationship."""
        system = RelationshipSystem(agent_id="agent_001")

        rel = system.set_family("agent_002", "sibling")

        assert rel.is_family
        assert rel.family_relation == "sibling"
        assert rel.friendship >= 50.0

    def test_get_family(self):
        """Test getting family relationships."""
        system = RelationshipSystem(agent_id="agent_001")
        system.set_family("agent_002", "parent")
        system.set_family("agent_003", "sibling")
        system.interact("agent_004", "chat")  # Non-family

        family = system.get_family()
        assert len(family) == 2

    def test_relationship_summary(self):
        """Test relationship summary by type."""
        system = RelationshipSystem(agent_id="agent_001")
        system.interact("agent_002", "chat")

        summary = system.get_relationship_summary()
        assert isinstance(summary, dict)
        assert RelationshipType.STRANGER in summary

    def test_new_relationship_callback(self):
        """Test callback for new relationships."""
        system = RelationshipSystem(agent_id="agent_001")
        new_rels = []

        system.on_new_relationship(lambda r: new_rels.append(r.target_agent_id))

        system.interact("agent_002", "chat")
        system.interact("agent_003", "chat")

        assert "agent_002" in new_rels
        assert "agent_003" in new_rels

    def test_type_change_callback(self):
        """Test callback for relationship type changes."""
        system = RelationshipSystem(agent_id="agent_001")
        type_changes = []

        system.on_type_change(
            lambda r, old, new: type_changes.append((old, new))
        )

        # Build friendship to change type
        for _ in range(10):
            system.interact("agent_002", "deep_conversation")

        assert len(type_changes) > 0

    def test_serialization(self):
        """Test relationship serialization."""
        system = RelationshipSystem(agent_id="agent_001")
        system.interact("agent_002", "chat")
        system.set_family("agent_003", "parent")

        data = system.to_dict()

        assert "agent_002" in data
        assert "agent_003" in data
        assert data["agent_003"]["is_family"] is True

    def test_deserialization(self):
        """Test relationship deserialization."""
        system = RelationshipSystem(agent_id="agent_001")

        data = {
            "agent_002": {
                "friendship": 50.0,
                "romance": 25.0,
                "is_family": False,
                "family_relation": None,
                "first_met": 0.0
            }
        }

        system.from_dict(data)

        rel = system.get_relationship("agent_002")
        assert rel.friendship == 50.0
        assert rel.romance == 25.0


class TestInteractionEffects:
    """Tests for pre-defined interaction effects."""

    def test_all_effects_have_friendship(self):
        """Test all interactions have friendship effect."""
        for name, effects in INTERACTION_EFFECTS.items():
            assert "friendship" in effects

    def test_positive_interactions(self):
        """Test positive interactions increase friendship."""
        positive = ["greet", "chat", "compliment", "help", "gift"]
        for name in positive:
            assert INTERACTION_EFFECTS[name]["friendship"] > 0

    def test_negative_interactions(self):
        """Test negative interactions decrease friendship."""
        negative = ["argue", "insult", "betray"]
        for name in negative:
            assert INTERACTION_EFFECTS[name]["friendship"] < 0
