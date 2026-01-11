"""
Needs System - Agent needs that decay over time and require fulfillment.

Based on the Sims FreePlay need-based simulation where characters have
six core needs that must be maintained through various activities.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, Optional, Callable


class NeedType(Enum):
    """Core need types for agents."""
    HUNGER = "hunger"
    ENERGY = "energy"
    HYGIENE = "hygiene"
    SOCIAL = "social"
    FUN = "fun"
    BLADDER = "bladder"


@dataclass
class NeedConfig:
    """Configuration for a specific need type."""
    decay_rate: float  # Points lost per time unit
    min_value: float = 0.0
    max_value: float = 100.0
    critical_threshold: float = 20.0  # Below this triggers urgent action
    warning_threshold: float = 40.0   # Below this triggers normal priority


# Default decay rates (points per hour of game time)
DEFAULT_NEED_CONFIGS: Dict[NeedType, NeedConfig] = {
    NeedType.HUNGER: NeedConfig(decay_rate=8.0),
    NeedType.ENERGY: NeedConfig(decay_rate=6.0),
    NeedType.HYGIENE: NeedConfig(decay_rate=4.0),
    NeedType.SOCIAL: NeedConfig(decay_rate=5.0),
    NeedType.FUN: NeedConfig(decay_rate=7.0),
    NeedType.BLADDER: NeedConfig(decay_rate=10.0),
}


@dataclass
class Need:
    """Represents a single need with its current value and configuration."""
    need_type: NeedType
    value: float = 100.0
    config: NeedConfig = field(default_factory=lambda: NeedConfig(decay_rate=5.0))

    @property
    def is_critical(self) -> bool:
        """Check if need is at critical level."""
        return self.value <= self.config.critical_threshold

    @property
    def is_warning(self) -> bool:
        """Check if need is at warning level."""
        return self.value <= self.config.warning_threshold

    @property
    def satisfaction_ratio(self) -> float:
        """Get satisfaction as 0-1 ratio."""
        range_size = self.config.max_value - self.config.min_value
        return (self.value - self.config.min_value) / range_size

    def decay(self, delta_time: float) -> None:
        """Apply time-based decay to the need."""
        decay_amount = self.config.decay_rate * delta_time
        self.value = max(self.config.min_value, self.value - decay_amount)

    def fulfill(self, amount: float) -> float:
        """
        Fulfill the need by the given amount.

        Returns:
            The actual amount fulfilled (may be less if at max).
        """
        old_value = self.value
        self.value = min(self.config.max_value, self.value + amount)
        return self.value - old_value

    def set_value(self, value: float) -> None:
        """Set the need value, clamping to valid range."""
        self.value = max(self.config.min_value, min(self.config.max_value, value))


class NeedsSystem:
    """
    Manages all needs for an agent.

    The needs system tracks multiple needs, handles decay over time,
    and provides methods for fulfilling needs through actions.
    """

    def __init__(self, configs: Optional[Dict[NeedType, NeedConfig]] = None):
        """
        Initialize the needs system.

        Args:
            configs: Optional custom configurations for each need type.
                    Defaults to DEFAULT_NEED_CONFIGS.
        """
        self._configs = configs or DEFAULT_NEED_CONFIGS
        self._needs: Dict[NeedType, Need] = {}
        self._callbacks: Dict[str, list[Callable]] = {
            "on_critical": [],
            "on_warning": [],
            "on_fulfilled": [],
        }
        self._initialize_needs()

    def _initialize_needs(self) -> None:
        """Initialize all needs with their configurations."""
        for need_type, config in self._configs.items():
            self._needs[need_type] = Need(
                need_type=need_type,
                value=config.max_value,
                config=config
            )

    def get_need(self, need_type: NeedType) -> Need:
        """Get a specific need."""
        return self._needs[need_type]

    def get_value(self, need_type: NeedType) -> float:
        """Get the current value of a specific need."""
        return self._needs[need_type].value

    def get_all_values(self) -> Dict[NeedType, float]:
        """Get current values for all needs."""
        return {nt: need.value for nt, need in self._needs.items()}

    def update(self, delta_time: float) -> None:
        """
        Update all needs with time-based decay.

        Args:
            delta_time: Time elapsed in game hours.
        """
        for need in self._needs.values():
            was_critical = need.is_critical
            was_warning = need.is_warning

            need.decay(delta_time)

            # Trigger callbacks for state changes
            if need.is_critical and not was_critical:
                self._trigger_callbacks("on_critical", need)
            elif need.is_warning and not was_warning:
                self._trigger_callbacks("on_warning", need)

    def fulfill(self, need_type: NeedType, amount: float) -> float:
        """
        Fulfill a specific need.

        Args:
            need_type: The type of need to fulfill.
            amount: The amount to add to the need.

        Returns:
            The actual amount fulfilled.
        """
        need = self._needs[need_type]
        fulfilled = need.fulfill(amount)

        if fulfilled > 0:
            self._trigger_callbacks("on_fulfilled", need, fulfilled)

        return fulfilled

    def get_critical_needs(self) -> list[NeedType]:
        """Get list of needs at critical level."""
        return [nt for nt, need in self._needs.items() if need.is_critical]

    def get_warning_needs(self) -> list[NeedType]:
        """Get list of needs at warning level."""
        return [nt for nt, need in self._needs.items() if need.is_warning]

    def get_lowest_need(self) -> NeedType:
        """Get the need type with the lowest current value."""
        return min(self._needs.keys(), key=lambda nt: self._needs[nt].value)

    def get_overall_satisfaction(self) -> float:
        """
        Calculate overall satisfaction as average of all needs.

        Returns:
            Average satisfaction ratio (0-1).
        """
        if not self._needs:
            return 1.0
        return sum(n.satisfaction_ratio for n in self._needs.values()) / len(self._needs)

    def on_critical(self, callback: Callable[[Need], None]) -> None:
        """Register callback for when a need becomes critical."""
        self._callbacks["on_critical"].append(callback)

    def on_warning(self, callback: Callable[[Need], None]) -> None:
        """Register callback for when a need enters warning state."""
        self._callbacks["on_warning"].append(callback)

    def on_fulfilled(self, callback: Callable[[Need, float], None]) -> None:
        """Register callback for when a need is fulfilled."""
        self._callbacks["on_fulfilled"].append(callback)

    def _trigger_callbacks(self, event: str, *args) -> None:
        """Trigger all callbacks for an event."""
        for callback in self._callbacks.get(event, []):
            callback(*args)

    def to_dict(self) -> Dict[str, float]:
        """Serialize needs to dictionary."""
        return {need_type.value: need.value for need_type, need in self._needs.items()}

    def from_dict(self, data: Dict[str, float]) -> None:
        """Load needs from dictionary."""
        for need_name, value in data.items():
            need_type = NeedType(need_name)
            if need_type in self._needs:
                self._needs[need_type].set_value(value)
