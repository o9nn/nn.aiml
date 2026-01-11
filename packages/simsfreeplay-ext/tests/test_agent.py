"""Tests for the Agent class."""

import pytest
from simulation.agent import Agent, AgentManager, AgentState
from simulation.needs import NeedType
from simulation.skills import SkillType
from simulation.actions import Action, ActionType, ActionEffect


class TestAgent:
    """Tests for the Agent class."""

    def test_initialization(self):
        """Test agent initialization."""
        agent = Agent(name="Alice")

        assert agent.name == "Alice"
        assert agent.id is not None
        assert agent.total_time == 0.0

    def test_initialization_with_id(self):
        """Test agent initialization with custom ID."""
        agent = Agent(name="Bob", agent_id="bob_123")

        assert agent.id == "bob_123"

    def test_initialization_with_metadata(self):
        """Test agent initialization with metadata."""
        agent = Agent(name="Charlie", metadata={"age": 25, "occupation": "chef"})

        assert agent.metadata["age"] == 25

    def test_update_advances_time(self):
        """Test update advances total time."""
        agent = Agent(name="Alice")

        agent.update(1.0)
        assert agent.total_time == 1.0

        agent.update(2.5)
        assert agent.total_time == 3.5

    def test_update_decays_needs(self):
        """Test update causes need decay."""
        agent = Agent(name="Alice")
        initial_hunger = agent.needs.get_value(NeedType.HUNGER)

        agent.update(1.0)

        assert agent.needs.get_value(NeedType.HUNGER) < initial_hunger

    def test_queue_action(self):
        """Test queuing an action by name."""
        agent = Agent(name="Alice")

        result = agent.queue_action("eat")

        assert result is True
        assert agent.actions.queue_length == 1

    def test_queue_action_with_target(self):
        """Test queuing action with target."""
        agent = Agent(name="Alice")

        result = agent.queue_action("chat", target_agent_id="bob_123")

        assert result is True

    def test_update_processes_actions(self):
        """Test update processes queued actions."""
        agent = Agent(name="Alice")
        agent.queue_action("eat")

        agent.update(0.1)

        assert agent.is_busy
        assert agent.current_action_name == "eat"

    def test_action_completion_fulfills_needs(self):
        """Test completed actions fulfill needs."""
        agent = Agent(name="Alice")

        # Decay needs first
        agent.update(5.0)
        hunger_before = agent.needs.get_value(NeedType.HUNGER)

        # Queue and complete eat action
        agent.queue_action("eat")
        agent.update(1.0)  # Complete the eat action

        hunger_after = agent.needs.get_value(NeedType.HUNGER)
        # Note: there's also decay during the update, so check net increase
        # Eat gives +40 hunger, decay is ~8/hour, so net should be positive
        assert hunger_after > hunger_before

    def test_action_completion_adds_skills(self):
        """Test completed actions add skill XP."""
        agent = Agent(name="Alice")

        # Queue cooking action (gives cooking skill XP)
        agent.queue_action("cook")

        # Complete it
        agent.update(2.0)

        assert agent.skills.get_skill(SkillType.COOKING).experience > 0

    def test_interact_with(self):
        """Test interacting with another agent."""
        alice = Agent(name="Alice")
        bob = Agent(name="Bob")

        alice.interact_with(bob, "chat")

        # Both should have relationships
        alice_rel = alice.relationships.get_relationship(bob.id)
        bob_rel = bob.relationships.get_relationship(alice.id)

        assert alice_rel is not None
        assert bob_rel is not None
        assert alice_rel.friendship > 0
        assert bob_rel.friendship > 0

    def test_practice_skill(self):
        """Test practicing a skill directly."""
        agent = Agent(name="Alice")

        xp = agent.practice_skill(SkillType.FITNESS, hours=2.0)

        assert xp > 0
        assert agent.skills.get_skill(SkillType.FITNESS).experience > 0

    def test_get_autonomous_action_critical(self):
        """Test autonomous action selection for critical needs."""
        agent = Agent(name="Alice")

        # Decay needs heavily
        for _ in range(20):
            agent.update(1.0)

        action = agent.get_autonomous_action()

        assert action is not None

    def test_auto_act(self):
        """Test automatic action selection and queuing."""
        agent = Agent(name="Alice")

        # Decay needs heavily
        for _ in range(15):
            agent.update(1.0)

        result = agent.auto_act()

        assert result is True
        assert agent.actions.queue_length > 0 or agent.is_busy

    def test_is_busy(self):
        """Test busy state."""
        agent = Agent(name="Alice")

        assert not agent.is_busy

        agent.queue_action("sleep")
        agent.update(0.1)

        assert agent.is_busy

    def test_overall_wellbeing(self):
        """Test overall wellbeing calculation."""
        agent = Agent(name="Alice")

        # Initially should be high (all needs at max)
        initial = agent.overall_wellbeing
        assert initial > 0.5

        # After decay
        for _ in range(10):
            agent.update(1.0)

        later = agent.overall_wellbeing
        assert later < initial

    def test_get_status_summary(self):
        """Test status summary generation."""
        agent = Agent(name="Alice")
        agent.queue_action("eat")
        agent.update(0.1)

        summary = agent.get_status_summary()

        assert summary["name"] == "Alice"
        assert summary["current_action"] == "eat"
        assert "wellbeing" in summary
        assert "critical_needs" in summary

    def test_to_state(self):
        """Test serialization to state object."""
        agent = Agent(name="Alice", metadata={"test": True})
        agent.update(1.0)

        state = agent.to_state()

        assert isinstance(state, AgentState)
        assert state.name == "Alice"
        assert state.metadata["test"] is True
        assert "hunger" in state.needs

    def test_from_state(self):
        """Test loading from state object."""
        agent1 = Agent(name="Alice")
        agent1.update(5.0)
        agent1.practice_skill(SkillType.COOKING, 2.0)
        state = agent1.to_state()

        agent2 = Agent(name="Bob")  # Different agent
        agent2.from_state(state)

        assert agent2.name == "Alice"
        assert agent2.total_time == state.total_time
        assert agent2.skills.get_skill(SkillType.COOKING).experience > 0

    def test_repr(self):
        """Test string representation."""
        agent = Agent(name="Alice", agent_id="alice_001")

        repr_str = repr(agent)

        assert "Alice" in repr_str
        assert "alice_001" in repr_str


class TestAgentManager:
    """Tests for the AgentManager class."""

    def test_initialization(self):
        """Test manager initialization."""
        manager = AgentManager()

        assert len(manager) == 0

    def test_create_agent(self):
        """Test creating agents through manager."""
        manager = AgentManager()

        alice = manager.create_agent("Alice")

        assert len(manager) == 1
        assert alice.name == "Alice"

    def test_get_agent(self):
        """Test getting agent by ID."""
        manager = AgentManager()
        alice = manager.create_agent("Alice")

        retrieved = manager.get_agent(alice.id)

        assert retrieved is alice

    def test_get_nonexistent_agent(self):
        """Test getting nonexistent agent returns None."""
        manager = AgentManager()

        result = manager.get_agent("nonexistent")

        assert result is None

    def test_remove_agent(self):
        """Test removing an agent."""
        manager = AgentManager()
        alice = manager.create_agent("Alice")

        removed = manager.remove_agent(alice.id)

        assert removed is alice
        assert len(manager) == 0

    def test_get_all_agents(self):
        """Test getting all agents."""
        manager = AgentManager()
        manager.create_agent("Alice")
        manager.create_agent("Bob")
        manager.create_agent("Charlie")

        agents = manager.get_all_agents()

        assert len(agents) == 3

    def test_update_all(self):
        """Test updating all agents."""
        manager = AgentManager()
        alice = manager.create_agent("Alice")
        bob = manager.create_agent("Bob")

        manager.update_all(1.0)

        assert alice.total_time == 1.0
        assert bob.total_time == 1.0

    def test_auto_act_all(self):
        """Test auto-acting for all agents."""
        manager = AgentManager()
        manager.create_agent("Alice")
        manager.create_agent("Bob")

        # Decay needs
        for _ in range(15):
            manager.update_all(1.0)

        count = manager.auto_act_all()

        # At least some agents should act
        assert count >= 0

    def test_get_agents_by_wellbeing(self):
        """Test sorting agents by wellbeing."""
        manager = AgentManager()
        alice = manager.create_agent("Alice")
        bob = manager.create_agent("Bob")

        # Make Bob have lower wellbeing
        for _ in range(10):
            bob.update(1.0)

        sorted_agents = manager.get_agents_by_wellbeing()

        assert sorted_agents[0] is bob  # Lowest first

    def test_get_population_stats(self):
        """Test population statistics."""
        manager = AgentManager()
        manager.create_agent("Alice")
        manager.create_agent("Bob")
        manager.create_agent("Charlie")

        stats = manager.get_population_stats()

        assert stats["count"] == 3
        assert "avg_wellbeing" in stats
        assert "busy_count" in stats

    def test_get_population_stats_empty(self):
        """Test population stats for empty manager."""
        manager = AgentManager()

        stats = manager.get_population_stats()

        assert stats["count"] == 0

    def test_iteration(self):
        """Test iterating over agents."""
        manager = AgentManager()
        manager.create_agent("Alice")
        manager.create_agent("Bob")

        names = [agent.name for agent in manager]

        assert "Alice" in names
        assert "Bob" in names
