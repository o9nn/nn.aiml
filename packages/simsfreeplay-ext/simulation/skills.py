"""
Skill Progression System - Skills that improve through practice.

Based on the Sims FreePlay skill system where characters gain skills
through practice across various categories.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, Optional, Callable, List


class SkillType(Enum):
    """Categories of skills."""
    # Life skills
    COOKING = "cooking"
    GARDENING = "gardening"
    FISHING = "fishing"
    HANDINESS = "handiness"

    # Social skills
    CHARISMA = "charisma"
    COMEDY = "comedy"

    # Creative skills
    WRITING = "writing"
    PAINTING = "painting"
    MUSIC = "music"
    PHOTOGRAPHY = "photography"

    # Mental skills
    LOGIC = "logic"
    PROGRAMMING = "programming"

    # Physical skills
    FITNESS = "fitness"
    DANCING = "dancing"

    # Misc skills
    GAMING = "gaming"


@dataclass
class SkillConfig:
    """Configuration for a skill type."""
    max_level: int = 10
    base_xp_per_level: float = 100.0  # XP needed for level 1
    xp_scaling: float = 1.5  # Each level requires xp_scaling * previous level XP
    decay_rate: float = 0.0  # XP decay per hour (0 = no decay)


DEFAULT_SKILL_CONFIGS: Dict[SkillType, SkillConfig] = {
    skill_type: SkillConfig() for skill_type in SkillType
}


@dataclass
class Skill:
    """Represents a single skill with level and experience."""
    skill_type: SkillType
    level: int = 0
    experience: float = 0.0
    config: SkillConfig = field(default_factory=SkillConfig)
    total_practice_time: float = 0.0  # Total hours practiced

    @property
    def xp_for_current_level(self) -> float:
        """Get XP required for current level."""
        if self.level == 0:
            return self.config.base_xp_per_level
        return self.config.base_xp_per_level * (self.config.xp_scaling ** self.level)

    @property
    def xp_progress(self) -> float:
        """Get progress toward next level (0-1)."""
        if self.level >= self.config.max_level:
            return 1.0
        return self.experience / self.xp_for_current_level

    @property
    def is_maxed(self) -> bool:
        """Check if skill is at maximum level."""
        return self.level >= self.config.max_level

    @property
    def effectiveness(self) -> float:
        """
        Get effectiveness multiplier based on skill level.

        Returns a value from 0.5 (level 0) to 2.0 (level 10).
        """
        return 0.5 + (self.level / self.config.max_level) * 1.5

    def add_experience(self, amount: float) -> int:
        """
        Add experience to the skill.

        Args:
            amount: XP to add.

        Returns:
            Number of levels gained.
        """
        if self.is_maxed:
            return 0

        self.experience += amount
        levels_gained = 0

        # Check for level up(s)
        while (
            self.level < self.config.max_level
            and self.experience >= self.xp_for_current_level
        ):
            self.experience -= self.xp_for_current_level
            self.level += 1
            levels_gained += 1

        # Cap experience if maxed
        if self.is_maxed:
            self.experience = 0

        return levels_gained

    def practice(self, hours: float, base_xp_per_hour: float = 10.0) -> float:
        """
        Practice the skill for a duration.

        Args:
            hours: Practice duration in game hours.
            base_xp_per_hour: Base XP gain rate.

        Returns:
            Actual XP gained (affected by level).
        """
        self.total_practice_time += hours

        # Higher levels gain XP slightly slower (diminishing returns)
        level_modifier = 1.0 / (1.0 + self.level * 0.1)
        xp_gained = hours * base_xp_per_hour * level_modifier

        self.add_experience(xp_gained)
        return xp_gained

    def decay(self, delta_time: float) -> None:
        """Apply time-based decay to experience."""
        if self.config.decay_rate > 0 and self.experience > 0:
            decay_amount = self.config.decay_rate * delta_time
            self.experience = max(0, self.experience - decay_amount)


class SkillSystem:
    """
    Manages all skills for an agent.

    Tracks skill levels and experience, handles skill gain through
    activities, and provides methods for skill queries.
    """

    def __init__(self, configs: Optional[Dict[SkillType, SkillConfig]] = None):
        """
        Initialize the skill system.

        Args:
            configs: Optional custom configurations for skill types.
        """
        self._configs = configs or DEFAULT_SKILL_CONFIGS
        self._skills: Dict[SkillType, Skill] = {}
        self._callbacks: Dict[str, list[Callable]] = {
            "on_level_up": [],
            "on_skill_gain": [],
        }
        self._initialize_skills()

    def _initialize_skills(self) -> None:
        """Initialize all skills at level 0."""
        for skill_type, config in self._configs.items():
            self._skills[skill_type] = Skill(
                skill_type=skill_type,
                config=config
            )

    def get_skill(self, skill_type: SkillType) -> Skill:
        """Get a specific skill."""
        return self._skills[skill_type]

    def get_level(self, skill_type: SkillType) -> int:
        """Get the level of a specific skill."""
        return self._skills[skill_type].level

    def get_all_levels(self) -> Dict[SkillType, int]:
        """Get levels for all skills."""
        return {st: skill.level for st, skill in self._skills.items()}

    def get_effectiveness(self, skill_type: SkillType) -> float:
        """Get effectiveness multiplier for a skill."""
        return self._skills[skill_type].effectiveness

    def add_experience(self, skill_type: SkillType, amount: float) -> int:
        """
        Add experience to a skill.

        Args:
            skill_type: The skill to add XP to.
            amount: Amount of XP to add.

        Returns:
            Number of levels gained.
        """
        skill = self._skills[skill_type]
        old_level = skill.level

        levels_gained = skill.add_experience(amount)

        self._trigger_callbacks("on_skill_gain", skill, amount)

        if levels_gained > 0:
            self._trigger_callbacks("on_level_up", skill, old_level, skill.level)

        return levels_gained

    def practice(
        self,
        skill_type: SkillType,
        hours: float,
        base_xp_per_hour: float = 10.0
    ) -> float:
        """
        Practice a skill for a duration.

        Args:
            skill_type: The skill to practice.
            hours: Practice duration in game hours.
            base_xp_per_hour: Base XP gain rate.

        Returns:
            Actual XP gained.
        """
        skill = self._skills[skill_type]
        old_level = skill.level

        xp_gained = skill.practice(hours, base_xp_per_hour)

        self._trigger_callbacks("on_skill_gain", skill, xp_gained)

        if skill.level > old_level:
            self._trigger_callbacks("on_level_up", skill, old_level, skill.level)

        return xp_gained

    def update(self, delta_time: float) -> None:
        """
        Update all skills with time-based decay.

        Args:
            delta_time: Time elapsed in game hours.
        """
        for skill in self._skills.values():
            skill.decay(delta_time)

    def get_strongest_skills(self, count: int = 3) -> List[Skill]:
        """Get the highest-level skills."""
        sorted_skills = sorted(
            self._skills.values(),
            key=lambda s: (s.level, s.experience),
            reverse=True
        )
        return sorted_skills[:count]

    def get_weakest_skills(self, count: int = 3) -> List[Skill]:
        """Get the lowest-level skills."""
        sorted_skills = sorted(
            self._skills.values(),
            key=lambda s: (s.level, s.experience)
        )
        return sorted_skills[:count]

    def get_total_skill_points(self) -> int:
        """Get total skill levels across all skills."""
        return sum(skill.level for skill in self._skills.values())

    def get_average_skill_level(self) -> float:
        """Get average skill level."""
        if not self._skills:
            return 0.0
        return self.get_total_skill_points() / len(self._skills)

    def on_level_up(self, callback: Callable[[Skill, int, int], None]) -> None:
        """Register callback for skill level ups."""
        self._callbacks["on_level_up"].append(callback)

    def on_skill_gain(self, callback: Callable[[Skill, float], None]) -> None:
        """Register callback for skill XP gains."""
        self._callbacks["on_skill_gain"].append(callback)

    def _trigger_callbacks(self, event: str, *args) -> None:
        """Trigger all callbacks for an event."""
        for callback in self._callbacks.get(event, []):
            callback(*args)

    def to_dict(self) -> Dict[str, Dict]:
        """Serialize skills to dictionary."""
        return {
            skill_type.value: {
                "level": skill.level,
                "experience": skill.experience,
                "total_practice_time": skill.total_practice_time,
            }
            for skill_type, skill in self._skills.items()
        }

    def from_dict(self, data: Dict[str, Dict]) -> None:
        """Load skills from dictionary."""
        for skill_name, skill_data in data.items():
            skill_type = SkillType(skill_name)
            if skill_type in self._skills:
                skill = self._skills[skill_type]
                skill.level = skill_data.get("level", 0)
                skill.experience = skill_data.get("experience", 0.0)
                skill.total_practice_time = skill_data.get("total_practice_time", 0.0)
