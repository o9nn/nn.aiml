"""Tests for the Action Queue System."""

import pytest
from simulation.actions import (
    ActionQueue, Action, ActionType, ActionState,
    ActionEffect, ACTION_TEMPLATES
)
from simulation.needs import NeedType


class TestAction:
    """Tests for the Action class."""

    def test_initial_state(self):
        """Test action initialization."""
        action = Action(
            name="test_action",
            action_type=ActionType.AUTONOMOUS,
            duration=1.0
        )
        assert action.state == ActionState.QUEUED
        assert action.progress == 0.0
        assert not action.is_complete

    def test_advance_progress(self):
        """Test advancing action progress."""
        action = Action(
            name="test_action",
            action_type=ActionType.AUTONOMOUS,
            duration=2.0
        )

        action.advance(1.0)  # Half done
        assert action.progress == 0.5
        assert action.state == ActionState.IN_PROGRESS
        assert not action.is_complete

        action.advance(1.0)  # Fully done
        assert action.progress == 1.0
        assert action.state == ActionState.COMPLETED
        assert action.is_complete

    def test_time_remaining(self):
        """Test time remaining calculation."""
        action = Action(
            name="test_action",
            action_type=ActionType.AUTONOMOUS,
            duration=4.0
        )

        assert action.time_remaining == 4.0

        action.advance(1.0)
        assert action.time_remaining == 3.0

    def test_instant_action(self):
        """Test action with zero duration."""
        action = Action(
            name="instant",
            action_type=ActionType.AUTONOMOUS,
            duration=0.0
        )

        action.advance(0.1)
        assert action.is_complete


class TestActionEffect:
    """Tests for the ActionEffect class."""

    def test_default_effects(self):
        """Test default empty effects."""
        effect = ActionEffect()
        assert len(effect.need_effects) == 0
        assert len(effect.skill_effects) == 0
        assert len(effect.relationship_effects) == 0

    def test_custom_effects(self):
        """Test custom effect values."""
        effect = ActionEffect(
            need_effects={NeedType.HUNGER: 50.0, NeedType.ENERGY: -10.0},
            skill_effects={"cooking": 5.0}
        )
        assert effect.need_effects[NeedType.HUNGER] == 50.0
        assert effect.skill_effects["cooking"] == 5.0


class TestActionQueue:
    """Tests for the ActionQueue class."""

    def test_empty_queue(self):
        """Test empty queue state."""
        queue = ActionQueue()
        assert not queue.is_busy
        assert queue.current_action is None
        assert queue.queue_length == 0

    def test_add_action(self):
        """Test adding actions to queue."""
        queue = ActionQueue()
        action = Action(name="test", action_type=ActionType.AUTONOMOUS, duration=1.0)

        result = queue.add_action(action)
        assert result is True
        assert queue.queue_length == 1

    def test_add_action_by_name(self):
        """Test adding action by template name."""
        queue = ActionQueue()

        result = queue.add_action_by_name("eat")
        assert result is True
        assert queue.queue_length == 1

    def test_add_unknown_action(self):
        """Test adding unknown action name fails."""
        queue = ActionQueue()

        result = queue.add_action_by_name("nonexistent_action")
        assert result is False

    def test_update_starts_action(self):
        """Test update starts first queued action."""
        queue = ActionQueue()
        queue.add_action_by_name("eat")

        queue.update(0.1)

        assert queue.is_busy
        assert queue.current_action is not None
        assert queue.current_action.name == "eat"

    def test_update_completes_action(self):
        """Test update completes action when duration elapsed."""
        queue = ActionQueue()
        queue.add_action_by_name("eat")  # duration is 0.5

        # Advance past duration
        effect = queue.update(1.0)

        assert not queue.is_busy
        assert queue.current_action is None
        assert effect is not None  # Returns effects on completion

    def test_action_effects_returned(self):
        """Test that completed actions return their effects."""
        queue = ActionQueue()
        queue.add_action_by_name("eat")

        effect = queue.update(1.0)

        assert NeedType.HUNGER in effect.need_effects
        assert effect.need_effects[NeedType.HUNGER] == 40.0

    def test_cancel_current_action(self):
        """Test cancelling the current action."""
        queue = ActionQueue()
        queue.add_action_by_name("sleep")
        queue.update(0.1)  # Start the action

        assert queue.is_busy
        cancelled = queue.cancel_current()

        assert cancelled is not None
        assert cancelled.state == ActionState.CANCELLED
        assert not queue.is_busy

    def test_non_interruptible_action(self):
        """Test that non-interruptible actions can't be cancelled."""
        queue = ActionQueue()
        action = Action(
            name="important",
            action_type=ActionType.DIRECTED,
            duration=1.0,
            interruptible=False
        )
        queue.add_action(action)
        queue.update(0.1)

        cancelled = queue.cancel_current()
        assert cancelled is None
        assert queue.is_busy

    def test_clear_queue(self):
        """Test clearing the action queue."""
        queue = ActionQueue()
        queue.add_action_by_name("eat")
        queue.add_action_by_name("sleep")
        queue.add_action_by_name("shower")

        cleared = queue.clear_queue()

        assert len(cleared) == 3
        assert queue.queue_length == 0

    def test_add_to_front(self):
        """Test adding action to front of queue."""
        queue = ActionQueue()
        queue.add_action_by_name("sleep")
        queue.add_action_by_name("eat", front=True)

        queue.update(0.1)
        assert queue.current_action.name == "eat"

    def test_interrupt_for(self):
        """Test interrupting current action for urgent action."""
        queue = ActionQueue()
        queue.add_action_by_name("sleep")
        queue.update(0.1)  # Start sleep

        urgent = Action(
            name="urgent",
            action_type=ActionType.DIRECTED,
            duration=0.1
        )

        result = queue.interrupt_for(urgent)

        assert result is True
        assert queue.current_action.name == "urgent"
        assert queue.queue_length == 1  # sleep was re-queued

    def test_queue_preview(self):
        """Test getting queue preview."""
        queue = ActionQueue()
        queue.add_action_by_name("eat")
        queue.add_action_by_name("sleep")
        queue.update(0.1)

        preview = queue.get_queue_preview()

        assert "*eat* (in progress)" in preview[0]
        assert "sleep" in preview[1]

    def test_action_history(self):
        """Test action history tracking."""
        queue = ActionQueue()
        queue.add_action_by_name("use_bathroom")  # Short duration
        queue.update(1.0)  # Complete it

        history = queue.get_history()

        assert len(history) == 1
        assert history[0].name == "use_bathroom"
        assert history[0].state == ActionState.COMPLETED

    def test_callbacks(self):
        """Test action callbacks."""
        queue = ActionQueue()
        started = []
        completed = []

        queue.on_action_start(lambda a: started.append(a.name))
        queue.on_action_complete(lambda a: completed.append(a.name))

        queue.add_action_by_name("use_bathroom")
        queue.update(0.01)  # Start
        queue.update(1.0)   # Complete

        assert "use_bathroom" in started
        assert "use_bathroom" in completed


class TestActionTemplates:
    """Tests for pre-defined action templates."""

    def test_all_templates_valid(self):
        """Test all templates have required fields."""
        for name, template in ACTION_TEMPLATES.items():
            assert template.name == name
            assert template.action_type is not None
            assert template.duration >= 0

    def test_eat_template(self):
        """Test eat action template."""
        eat = ACTION_TEMPLATES["eat"]
        assert eat.duration == 0.5
        assert NeedType.HUNGER in eat.effects.need_effects

    def test_sleep_template(self):
        """Test sleep action template."""
        sleep = ACTION_TEMPLATES["sleep"]
        assert sleep.duration == 8.0
        assert NeedType.ENERGY in sleep.effects.need_effects
