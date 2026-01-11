"""
Action Queue System - Queue-based action execution for agent behaviors.

Based on the Sims FreePlay action queue where characters execute actions
from a queue, supporting both autonomous (AI-driven) and directed (player-driven) actions.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, Optional, Callable, Any
from collections import deque
import uuid

from .needs import NeedType


class ActionType(Enum):
    """Categories of actions."""
    AUTONOMOUS = "autonomous"  # AI-selected actions
    DIRECTED = "directed"      # User/system-directed actions
    SOCIAL = "social"          # Interactions with other agents
    OBJECT = "object"          # Interactions with objects


class ActionState(Enum):
    """Current state of an action."""
    QUEUED = "queued"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    CANCELLED = "cancelled"
    FAILED = "failed"


@dataclass
class ActionEffect:
    """Effect an action has on needs or skills."""
    need_effects: Dict[NeedType, float] = field(default_factory=dict)
    skill_effects: Dict[str, float] = field(default_factory=dict)
    relationship_effects: Dict[str, float] = field(default_factory=dict)


@dataclass
class Action:
    """Represents a single action that can be performed by an agent."""
    name: str
    action_type: ActionType
    duration: float  # Time in game hours to complete
    effects: ActionEffect = field(default_factory=ActionEffect)
    priority: int = 5  # 1-10, higher = more urgent
    interruptible: bool = True
    target_agent_id: Optional[str] = None  # For social actions
    target_object_id: Optional[str] = None  # For object actions
    metadata: Dict[str, Any] = field(default_factory=dict)

    # Runtime state
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    state: ActionState = ActionState.QUEUED
    progress: float = 0.0  # 0-1 completion ratio

    @property
    def is_complete(self) -> bool:
        """Check if action is complete."""
        return self.progress >= 1.0 or self.state == ActionState.COMPLETED

    @property
    def time_remaining(self) -> float:
        """Get remaining time to complete action."""
        return self.duration * (1.0 - self.progress)

    def advance(self, delta_time: float) -> float:
        """
        Advance action progress.

        Args:
            delta_time: Time elapsed in game hours.

        Returns:
            Completion ratio after advancement.
        """
        if self.duration > 0:
            self.progress = min(1.0, self.progress + (delta_time / self.duration))
        else:
            self.progress = 1.0

        if self.progress >= 1.0:
            self.state = ActionState.COMPLETED
        elif self.state == ActionState.QUEUED:
            self.state = ActionState.IN_PROGRESS

        return self.progress


# Pre-defined action templates
ACTION_TEMPLATES: Dict[str, Action] = {
    "eat": Action(
        name="eat",
        action_type=ActionType.OBJECT,
        duration=0.5,
        effects=ActionEffect(need_effects={NeedType.HUNGER: 40.0, NeedType.BLADDER: -10.0}),
        priority=7,
    ),
    "sleep": Action(
        name="sleep",
        action_type=ActionType.AUTONOMOUS,
        duration=8.0,
        effects=ActionEffect(need_effects={NeedType.ENERGY: 100.0}),
        priority=8,
        interruptible=True,
    ),
    "shower": Action(
        name="shower",
        action_type=ActionType.OBJECT,
        duration=0.5,
        effects=ActionEffect(need_effects={NeedType.HYGIENE: 50.0}),
        priority=6,
    ),
    "use_bathroom": Action(
        name="use_bathroom",
        action_type=ActionType.OBJECT,
        duration=0.1,
        effects=ActionEffect(need_effects={NeedType.BLADDER: 100.0}),
        priority=9,
    ),
    "chat": Action(
        name="chat",
        action_type=ActionType.SOCIAL,
        duration=0.5,
        effects=ActionEffect(
            need_effects={NeedType.SOCIAL: 20.0, NeedType.FUN: 10.0},
            relationship_effects={"friendship": 5.0}
        ),
        priority=4,
    ),
    "play_game": Action(
        name="play_game",
        action_type=ActionType.OBJECT,
        duration=1.0,
        effects=ActionEffect(need_effects={NeedType.FUN: 30.0}),
        priority=3,
    ),
    "watch_tv": Action(
        name="watch_tv",
        action_type=ActionType.OBJECT,
        duration=2.0,
        effects=ActionEffect(need_effects={NeedType.FUN: 20.0, NeedType.ENERGY: -5.0}),
        priority=2,
    ),
    "cook": Action(
        name="cook",
        action_type=ActionType.OBJECT,
        duration=1.0,
        effects=ActionEffect(skill_effects={"cooking": 2.0}),
        priority=5,
    ),
    "exercise": Action(
        name="exercise",
        action_type=ActionType.AUTONOMOUS,
        duration=1.0,
        effects=ActionEffect(
            need_effects={NeedType.ENERGY: -20.0, NeedType.FUN: 10.0, NeedType.HYGIENE: -15.0},
            skill_effects={"fitness": 3.0}
        ),
        priority=4,
    ),
    "read": Action(
        name="read",
        action_type=ActionType.OBJECT,
        duration=1.0,
        effects=ActionEffect(
            need_effects={NeedType.FUN: 15.0},
            skill_effects={"logic": 2.0}
        ),
        priority=3,
    ),
}


class ActionQueue:
    """
    Manages a queue of actions for an agent.

    Actions are executed in order, with priority-based reordering
    and support for interruption and cancellation.
    """

    def __init__(self, max_queue_size: int = 10):
        """
        Initialize the action queue.

        Args:
            max_queue_size: Maximum number of queued actions.
        """
        self._queue: deque[Action] = deque(maxlen=max_queue_size)
        self._current_action: Optional[Action] = None
        self._action_history: list[Action] = []
        self._max_history: int = 50
        self._callbacks: Dict[str, list[Callable]] = {
            "on_action_start": [],
            "on_action_complete": [],
            "on_action_cancelled": [],
        }

    @property
    def current_action(self) -> Optional[Action]:
        """Get the currently executing action."""
        return self._current_action

    @property
    def is_busy(self) -> bool:
        """Check if agent is currently performing an action."""
        return self._current_action is not None

    @property
    def queue_length(self) -> int:
        """Get number of queued actions."""
        return len(self._queue)

    def add_action(self, action: Action, front: bool = False) -> bool:
        """
        Add an action to the queue.

        Args:
            action: The action to add.
            front: If True, add to front of queue (next after current).

        Returns:
            True if action was added, False if queue is full.
        """
        if len(self._queue) >= self._queue.maxlen:
            return False

        # Create a copy to avoid modifying templates
        action_copy = Action(
            name=action.name,
            action_type=action.action_type,
            duration=action.duration,
            effects=action.effects,
            priority=action.priority,
            interruptible=action.interruptible,
            target_agent_id=action.target_agent_id,
            target_object_id=action.target_object_id,
            metadata=action.metadata.copy(),
        )

        if front:
            self._queue.appendleft(action_copy)
        else:
            self._queue.append(action_copy)

        return True

    def add_action_by_name(self, name: str, **kwargs) -> bool:
        """
        Add an action by template name.

        Args:
            name: Name of the action template.
            **kwargs: Override parameters for the action.

        Returns:
            True if action was added.
        """
        template = ACTION_TEMPLATES.get(name)
        if not template:
            return False

        action = Action(
            name=template.name,
            action_type=template.action_type,
            duration=template.duration,
            effects=template.effects,
            priority=kwargs.get("priority", template.priority),
            interruptible=kwargs.get("interruptible", template.interruptible),
            target_agent_id=kwargs.get("target_agent_id"),
            target_object_id=kwargs.get("target_object_id"),
            metadata=kwargs.get("metadata", {}),
        )

        return self.add_action(action, front=kwargs.get("front", False))

    def update(self, delta_time: float) -> Optional[ActionEffect]:
        """
        Update the action queue, advancing current action.

        Args:
            delta_time: Time elapsed in game hours.

        Returns:
            ActionEffect if an action completed, None otherwise.
        """
        # Start new action if none in progress
        if self._current_action is None and self._queue:
            self._current_action = self._queue.popleft()
            self._current_action.state = ActionState.IN_PROGRESS
            self._trigger_callbacks("on_action_start", self._current_action)

        if self._current_action is None:
            return None

        # Advance current action
        self._current_action.advance(delta_time)

        # Check for completion
        if self._current_action.is_complete:
            completed_action = self._current_action
            self._current_action = None

            # Store in history
            self._action_history.append(completed_action)
            if len(self._action_history) > self._max_history:
                self._action_history.pop(0)

            self._trigger_callbacks("on_action_complete", completed_action)
            return completed_action.effects

        return None

    def cancel_current(self) -> Optional[Action]:
        """
        Cancel the current action if interruptible.

        Returns:
            The cancelled action, or None if not interruptible.
        """
        if self._current_action and self._current_action.interruptible:
            cancelled = self._current_action
            cancelled.state = ActionState.CANCELLED
            self._current_action = None
            self._trigger_callbacks("on_action_cancelled", cancelled)
            return cancelled
        return None

    def clear_queue(self) -> list[Action]:
        """
        Clear all queued actions (not the current one).

        Returns:
            List of cleared actions.
        """
        cleared = list(self._queue)
        self._queue.clear()
        for action in cleared:
            action.state = ActionState.CANCELLED
        return cleared

    def interrupt_for(self, action: Action) -> bool:
        """
        Interrupt current action and start a new one immediately.

        Args:
            action: The urgent action to perform.

        Returns:
            True if interruption succeeded.
        """
        if self._current_action:
            if not self._current_action.interruptible:
                return False
            # Put current action back at front of queue
            self._queue.appendleft(self._current_action)
            self._current_action.state = ActionState.QUEUED

        self._current_action = action
        self._current_action.state = ActionState.IN_PROGRESS
        self._trigger_callbacks("on_action_start", action)
        return True

    def get_queue_preview(self) -> list[str]:
        """Get names of all queued actions."""
        names = []
        if self._current_action:
            names.append(f"*{self._current_action.name}* (in progress)")
        names.extend(action.name for action in self._queue)
        return names

    def get_history(self, limit: int = 10) -> list[Action]:
        """Get recent action history."""
        return self._action_history[-limit:]

    def on_action_start(self, callback: Callable[[Action], None]) -> None:
        """Register callback for action start."""
        self._callbacks["on_action_start"].append(callback)

    def on_action_complete(self, callback: Callable[[Action], None]) -> None:
        """Register callback for action completion."""
        self._callbacks["on_action_complete"].append(callback)

    def on_action_cancelled(self, callback: Callable[[Action], None]) -> None:
        """Register callback for action cancellation."""
        self._callbacks["on_action_cancelled"].append(callback)

    def _trigger_callbacks(self, event: str, *args) -> None:
        """Trigger all callbacks for an event."""
        for callback in self._callbacks.get(event, []):
            callback(*args)
