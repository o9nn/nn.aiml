"""
Agent - The central class integrating all simulation systems.

An Agent represents a simulated entity with needs, actions, relationships,
and skills - similar to a Sim in The Sims FreePlay.
"""

from dataclasses import dataclass, field
from typing import Dict, Optional, Any, List
import uuid

from .needs import NeedsSystem, NeedType
from .actions import ActionQueue, Action, ActionEffect, ACTION_TEMPLATES
from .relationships import RelationshipSystem, RelationshipType
from .skills import SkillSystem, SkillType


@dataclass
class AgentState:
    """Snapshot of agent state for serialization."""
    id: str
    name: str
    needs: Dict[str, float]
    skills: Dict[str, Dict]
    relationships: Dict
    current_action: Optional[str]
    action_queue: List[str]
    total_time: float
    metadata: Dict[str, Any]


class Agent:
    """
    A simulated agent with needs, actions, relationships, and skills.

    The Agent class integrates all subsystems and provides a unified
    interface for simulation updates and agent interactions.

    Example:
        agent = Agent(name="Alice")

        # Perform actions
        agent.queue_action("eat")
        agent.queue_action("chat", target_agent_id="bob_123")

        # Update simulation (1 game hour)
        agent.update(1.0)

        # Check state
        print(agent.needs.get_value(NeedType.HUNGER))
        print(agent.relationships.get_friends())
    """

    def __init__(
        self,
        name: str,
        agent_id: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ):
        """
        Initialize an agent.

        Args:
            name: Display name for the agent.
            agent_id: Unique identifier (auto-generated if not provided).
            metadata: Optional additional data for the agent.
        """
        self.id = agent_id or str(uuid.uuid4())
        self.name = name
        self.metadata = metadata or {}

        # Initialize subsystems
        self.needs = NeedsSystem()
        self.actions = ActionQueue()
        self.relationships = RelationshipSystem(self.id)
        self.skills = SkillSystem()

        # Track simulation time
        self._total_time: float = 0.0

        # Wire up internal callbacks
        self._setup_callbacks()

    def _setup_callbacks(self) -> None:
        """Set up internal callbacks between subsystems."""
        # When an action completes, apply its effects
        self.actions.on_action_complete(self._on_action_complete)

        # When needs become critical, consider autonomous actions
        self.needs.on_critical(self._on_need_critical)

    def _on_action_complete(self, action: Action) -> None:
        """Handle action completion by applying effects."""
        effects = action.effects

        # Apply need effects
        for need_type, amount in effects.need_effects.items():
            self.needs.fulfill(need_type, amount)

        # Apply skill effects
        for skill_name, amount in effects.skill_effects.items():
            try:
                skill_type = SkillType(skill_name)
                self.skills.add_experience(skill_type, amount)
            except ValueError:
                pass  # Unknown skill type

        # Apply relationship effects
        if action.target_agent_id and effects.relationship_effects:
            for effect_type, amount in effects.relationship_effects.items():
                if effect_type == "friendship":
                    rel = self.relationships.get_or_create_relationship(action.target_agent_id)
                    rel.modify_friendship(amount)
                elif effect_type == "romance":
                    rel = self.relationships.get_or_create_relationship(action.target_agent_id)
                    rel.modify_romance(amount)

    def _on_need_critical(self, need) -> None:
        """Handle critical need by potentially queuing autonomous action."""
        # Map critical needs to appropriate actions
        need_to_action = {
            NeedType.HUNGER: "eat",
            NeedType.ENERGY: "sleep",
            NeedType.HYGIENE: "shower",
            NeedType.BLADDER: "use_bathroom",
            NeedType.FUN: "play_game",
            NeedType.SOCIAL: "chat",
        }

        action_name = need_to_action.get(need.need_type)
        if action_name and not self.actions.is_busy:
            self.queue_action(action_name, front=True)

    def update(self, delta_time: float) -> Optional[ActionEffect]:
        """
        Update the agent simulation.

        Args:
            delta_time: Time elapsed in game hours.

        Returns:
            ActionEffect if an action completed during this update.
        """
        self._total_time += delta_time

        # Update all subsystems
        self.needs.update(delta_time)
        self.relationships.update(delta_time)
        self.skills.update(delta_time)

        # Process action queue
        effects = self.actions.update(delta_time)

        return effects

    def queue_action(
        self,
        action_name: str,
        front: bool = False,
        target_agent_id: Optional[str] = None,
        target_object_id: Optional[str] = None,
        **kwargs
    ) -> bool:
        """
        Queue an action by name.

        Args:
            action_name: Name of the action template.
            front: If True, add to front of queue.
            target_agent_id: Target agent for social actions.
            target_object_id: Target object for object actions.
            **kwargs: Additional action parameters.

        Returns:
            True if action was queued successfully.
        """
        return self.actions.add_action_by_name(
            action_name,
            front=front,
            target_agent_id=target_agent_id,
            target_object_id=target_object_id,
            **kwargs
        )

    def queue_custom_action(self, action: Action, front: bool = False) -> bool:
        """
        Queue a custom action.

        Args:
            action: The action to queue.
            front: If True, add to front of queue.

        Returns:
            True if action was queued successfully.
        """
        return self.actions.add_action(action, front=front)

    def interact_with(
        self,
        other_agent: 'Agent',
        interaction_type: str = "chat"
    ) -> None:
        """
        Interact with another agent.

        Args:
            other_agent: The agent to interact with.
            interaction_type: Type of interaction.
        """
        # Record interaction for both agents
        self.relationships.interact(other_agent.id, interaction_type)
        other_agent.relationships.interact(self.id, interaction_type)

    def practice_skill(
        self,
        skill_type: SkillType,
        hours: float = 1.0,
        base_xp: float = 10.0
    ) -> float:
        """
        Practice a skill directly.

        Args:
            skill_type: The skill to practice.
            hours: Duration of practice.
            base_xp: Base XP per hour.

        Returns:
            Actual XP gained.
        """
        return self.skills.practice(skill_type, hours, base_xp)

    def get_autonomous_action(self) -> Optional[str]:
        """
        Determine the best autonomous action based on current needs.

        Returns:
            Name of the recommended action, or None.
        """
        # Check for critical needs first
        critical = self.needs.get_critical_needs()
        if critical:
            need = critical[0]
            need_to_action = {
                NeedType.BLADDER: "use_bathroom",
                NeedType.HUNGER: "eat",
                NeedType.ENERGY: "sleep",
                NeedType.HYGIENE: "shower",
                NeedType.FUN: "play_game",
                NeedType.SOCIAL: "chat",
            }
            return need_to_action.get(need)

        # Check for warning needs
        warning = self.needs.get_warning_needs()
        if warning:
            lowest = self.needs.get_lowest_need()
            need_to_action = {
                NeedType.BLADDER: "use_bathroom",
                NeedType.HUNGER: "eat",
                NeedType.ENERGY: "sleep",
                NeedType.HYGIENE: "shower",
                NeedType.FUN: "watch_tv",
                NeedType.SOCIAL: "chat",
            }
            return need_to_action.get(lowest)

        return None

    def auto_act(self) -> bool:
        """
        Queue an autonomous action based on current state.

        Returns:
            True if an action was queued.
        """
        action_name = self.get_autonomous_action()
        if action_name:
            return self.queue_action(action_name)
        return False

    @property
    def is_busy(self) -> bool:
        """Check if agent is currently performing an action."""
        return self.actions.is_busy

    @property
    def current_action_name(self) -> Optional[str]:
        """Get name of current action, if any."""
        action = self.actions.current_action
        return action.name if action else None

    @property
    def overall_wellbeing(self) -> float:
        """
        Calculate overall wellbeing score (0-1).

        Combines needs satisfaction, relationships, and skills.
        """
        needs_score = self.needs.get_overall_satisfaction()
        skill_score = min(1.0, self.skills.get_average_skill_level() / 5.0)

        # Weight needs more heavily
        return needs_score * 0.7 + skill_score * 0.3

    @property
    def total_time(self) -> float:
        """Get total simulation time elapsed."""
        return self._total_time

    def get_status_summary(self) -> Dict[str, Any]:
        """Get a summary of agent status."""
        return {
            "id": self.id,
            "name": self.name,
            "wellbeing": round(self.overall_wellbeing, 2),
            "current_action": self.current_action_name,
            "queued_actions": self.actions.queue_length,
            "critical_needs": [n.value for n in self.needs.get_critical_needs()],
            "warning_needs": [n.value for n in self.needs.get_warning_needs()],
            "friend_count": len(self.relationships.get_friends()),
            "total_skill_points": self.skills.get_total_skill_points(),
            "total_time": round(self._total_time, 2),
        }

    def to_state(self) -> AgentState:
        """Serialize agent to state object."""
        return AgentState(
            id=self.id,
            name=self.name,
            needs=self.needs.to_dict(),
            skills=self.skills.to_dict(),
            relationships=self.relationships.to_dict(),
            current_action=self.current_action_name,
            action_queue=self.actions.get_queue_preview(),
            total_time=self._total_time,
            metadata=self.metadata.copy(),
        )

    def from_state(self, state: AgentState) -> None:
        """Load agent from state object."""
        self.id = state.id
        self.name = state.name
        self._total_time = state.total_time
        self.metadata = state.metadata.copy()

        self.needs.from_dict(state.needs)
        self.skills.from_dict(state.skills)
        self.relationships.from_dict(state.relationships)

    def __repr__(self) -> str:
        return f"Agent(id={self.id!r}, name={self.name!r})"


class AgentManager:
    """
    Manages multiple agents in a simulation.

    Provides methods for batch updates, inter-agent interactions,
    and population-level queries.
    """

    def __init__(self):
        """Initialize the agent manager."""
        self._agents: Dict[str, Agent] = {}

    def create_agent(
        self,
        name: str,
        agent_id: Optional[str] = None,
        **kwargs
    ) -> Agent:
        """
        Create and register a new agent.

        Args:
            name: Display name for the agent.
            agent_id: Optional specific ID.
            **kwargs: Additional agent parameters.

        Returns:
            The created agent.
        """
        agent = Agent(name=name, agent_id=agent_id, **kwargs)
        self._agents[agent.id] = agent
        return agent

    def get_agent(self, agent_id: str) -> Optional[Agent]:
        """Get an agent by ID."""
        return self._agents.get(agent_id)

    def get_all_agents(self) -> List[Agent]:
        """Get all agents."""
        return list(self._agents.values())

    def remove_agent(self, agent_id: str) -> Optional[Agent]:
        """Remove and return an agent."""
        return self._agents.pop(agent_id, None)

    def update_all(self, delta_time: float) -> None:
        """Update all agents."""
        for agent in self._agents.values():
            agent.update(delta_time)

    def auto_act_all(self) -> int:
        """
        Have all idle agents perform autonomous actions.

        Returns:
            Number of agents that performed actions.
        """
        count = 0
        for agent in self._agents.values():
            if not agent.is_busy and agent.auto_act():
                count += 1
        return count

    def get_agents_by_wellbeing(self, ascending: bool = True) -> List[Agent]:
        """Get agents sorted by wellbeing."""
        return sorted(
            self._agents.values(),
            key=lambda a: a.overall_wellbeing,
            reverse=not ascending
        )

    def get_population_stats(self) -> Dict[str, Any]:
        """Get statistics about the agent population."""
        if not self._agents:
            return {"count": 0}

        agents = list(self._agents.values())
        wellbeings = [a.overall_wellbeing for a in agents]

        return {
            "count": len(agents),
            "avg_wellbeing": sum(wellbeings) / len(wellbeings),
            "min_wellbeing": min(wellbeings),
            "max_wellbeing": max(wellbeings),
            "busy_count": sum(1 for a in agents if a.is_busy),
            "idle_count": sum(1 for a in agents if not a.is_busy),
        }

    def __len__(self) -> int:
        return len(self._agents)

    def __iter__(self):
        return iter(self._agents.values())
