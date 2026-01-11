"""
Sims FreePlay-inspired simulation systems for Dream Vortex integration.

This package provides agent simulation systems based on patterns extracted
from the Sims FreePlay architecture:

- Needs System: Agent needs that decay over time and require fulfillment
- Action System: Queue-based action execution for agent behaviors
- Relationship System: Tracking friendship, romance, and family bonds
- Skill System: Progressive skill development through activities

Example:
    from simulation import Agent

    agent = Agent(name="Alice")
    agent.perform_action("eat")
    agent.update(delta_time=1.0)
"""

from .agent import Agent
from .needs import NeedsSystem, NeedType
from .actions import ActionQueue, Action, ActionType
from .relationships import RelationshipSystem, RelationshipType
from .skills import SkillSystem, SkillType

__version__ = "0.1.0"
__all__ = [
    "Agent",
    "NeedsSystem",
    "NeedType",
    "ActionQueue",
    "Action",
    "ActionType",
    "RelationshipSystem",
    "RelationshipType",
    "SkillSystem",
    "SkillType",
]
