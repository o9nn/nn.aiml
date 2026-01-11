"""Tests for the Skill Progression System."""

import pytest
from simulation.skills import SkillSystem, Skill, SkillType, SkillConfig


class TestSkill:
    """Tests for the Skill class."""

    def test_initial_values(self):
        """Test skill initialization."""
        skill = Skill(skill_type=SkillType.COOKING)
        assert skill.level == 0
        assert skill.experience == 0.0
        assert not skill.is_maxed

    def test_add_experience(self):
        """Test adding experience."""
        skill = Skill(skill_type=SkillType.COOKING)

        skill.add_experience(50.0)
        assert skill.experience == 50.0
        assert skill.level == 0

    def test_level_up(self):
        """Test leveling up from experience."""
        config = SkillConfig(base_xp_per_level=100.0, xp_scaling=1.0)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        levels = skill.add_experience(150.0)

        assert levels == 1
        assert skill.level == 1
        assert skill.experience == 50.0  # Overflow

    def test_multiple_level_ups(self):
        """Test multiple level ups from large XP gain."""
        config = SkillConfig(base_xp_per_level=100.0, xp_scaling=1.0, max_level=10)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        levels = skill.add_experience(350.0)

        assert levels == 3
        assert skill.level == 3
        assert skill.experience == 50.0

    def test_max_level_cap(self):
        """Test skill can't exceed max level."""
        config = SkillConfig(base_xp_per_level=10.0, xp_scaling=1.0, max_level=5)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        skill.add_experience(10000.0)

        assert skill.level == 5
        assert skill.is_maxed
        assert skill.experience == 0  # Capped XP

    def test_xp_progress(self):
        """Test XP progress toward next level."""
        config = SkillConfig(base_xp_per_level=100.0, xp_scaling=1.0)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        skill.add_experience(50.0)
        assert skill.xp_progress == 0.5

    def test_xp_scaling(self):
        """Test XP requirements scale with level."""
        config = SkillConfig(base_xp_per_level=100.0, xp_scaling=2.0)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        # Level 0->1 requires 100 XP
        assert skill.xp_for_current_level == 100.0

        skill.level = 1
        # Level 1->2 requires 200 XP (100 * 2^1)
        assert skill.xp_for_current_level == 200.0

        skill.level = 2
        # Level 2->3 requires 400 XP (100 * 2^2)
        assert skill.xp_for_current_level == 400.0

    def test_effectiveness(self):
        """Test effectiveness multiplier."""
        config = SkillConfig(max_level=10)
        skill = Skill(skill_type=SkillType.COOKING, config=config)

        # Level 0: 0.5
        assert skill.effectiveness == 0.5

        skill.level = 5
        # Level 5: 1.25
        assert skill.effectiveness == 1.25

        skill.level = 10
        # Level 10: 2.0
        assert skill.effectiveness == 2.0

    def test_practice(self):
        """Test practicing a skill."""
        skill = Skill(skill_type=SkillType.COOKING)

        xp = skill.practice(1.0, base_xp_per_hour=20.0)

        assert xp > 0
        assert skill.experience > 0
        assert skill.total_practice_time == 1.0

    def test_practice_diminishing_returns(self):
        """Test practice has diminishing returns at higher levels."""
        skill1 = Skill(skill_type=SkillType.COOKING)
        skill2 = Skill(skill_type=SkillType.COOKING)
        skill2.level = 5

        xp1 = skill1.practice(1.0, base_xp_per_hour=20.0)
        xp2 = skill2.practice(1.0, base_xp_per_hour=20.0)

        assert xp2 < xp1  # Higher level gets less XP

    def test_decay(self):
        """Test skill decay over time."""
        config = SkillConfig(decay_rate=1.0)
        skill = Skill(skill_type=SkillType.COOKING, config=config)
        skill.experience = 50.0

        skill.decay(10.0)

        assert skill.experience == 40.0

    def test_decay_clamp(self):
        """Test decay doesn't go below zero."""
        config = SkillConfig(decay_rate=10.0)
        skill = Skill(skill_type=SkillType.COOKING, config=config)
        skill.experience = 5.0

        skill.decay(1.0)

        assert skill.experience == 0.0


class TestSkillSystem:
    """Tests for the SkillSystem class."""

    def test_initialization(self):
        """Test all skills initialized at level 0."""
        system = SkillSystem()

        for skill_type in SkillType:
            assert system.get_level(skill_type) == 0

    def test_get_skill(self):
        """Test getting a specific skill."""
        system = SkillSystem()

        skill = system.get_skill(SkillType.COOKING)
        assert skill.skill_type == SkillType.COOKING

    def test_add_experience(self):
        """Test adding experience to a skill."""
        system = SkillSystem()

        levels = system.add_experience(SkillType.COOKING, 50.0)

        assert system.get_skill(SkillType.COOKING).experience == 50.0

    def test_practice(self):
        """Test practicing a skill through the system."""
        system = SkillSystem()

        xp = system.practice(SkillType.COOKING, 2.0, base_xp_per_hour=15.0)

        assert xp > 0
        assert system.get_skill(SkillType.COOKING).experience > 0

    def test_update_decay(self):
        """Test system update applies decay to all skills."""
        # Create system with decay
        configs = {
            skill_type: SkillConfig(decay_rate=1.0)
            for skill_type in SkillType
        }
        system = SkillSystem(configs=configs)

        # Add some XP
        system.add_experience(SkillType.COOKING, 50.0)

        # Update
        system.update(10.0)

        assert system.get_skill(SkillType.COOKING).experience == 40.0

    def test_get_all_levels(self):
        """Test getting all skill levels."""
        system = SkillSystem()
        system.add_experience(SkillType.COOKING, 200.0)

        levels = system.get_all_levels()

        assert isinstance(levels, dict)
        assert SkillType.COOKING in levels

    def test_get_effectiveness(self):
        """Test getting skill effectiveness."""
        system = SkillSystem()

        eff = system.get_effectiveness(SkillType.COOKING)
        assert eff == 0.5  # Level 0

    def test_get_strongest_skills(self):
        """Test getting highest level skills."""
        system = SkillSystem()
        system.add_experience(SkillType.COOKING, 500.0)
        system.add_experience(SkillType.FITNESS, 300.0)
        system.add_experience(SkillType.LOGIC, 200.0)

        strongest = system.get_strongest_skills(2)

        assert len(strongest) == 2
        assert strongest[0].skill_type == SkillType.COOKING

    def test_get_weakest_skills(self):
        """Test getting lowest level skills."""
        system = SkillSystem()
        system.add_experience(SkillType.COOKING, 500.0)

        weakest = system.get_weakest_skills(3)

        assert len(weakest) == 3
        assert all(s.level == 0 for s in weakest[:2])  # Most are level 0

    def test_total_skill_points(self):
        """Test total skill points calculation."""
        system = SkillSystem()
        system.add_experience(SkillType.COOKING, 500.0)  # ~3-4 levels
        system.add_experience(SkillType.FITNESS, 200.0)  # ~1-2 levels

        total = system.get_total_skill_points()
        assert total > 0

    def test_average_skill_level(self):
        """Test average skill level calculation."""
        system = SkillSystem()

        avg = system.get_average_skill_level()
        assert avg == 0.0  # All at level 0

    def test_level_up_callback(self):
        """Test callback for level ups."""
        system = SkillSystem()
        level_ups = []

        system.on_level_up(
            lambda skill, old, new: level_ups.append((skill.skill_type, old, new))
        )

        system.add_experience(SkillType.COOKING, 150.0)

        assert len(level_ups) >= 1
        assert level_ups[0][0] == SkillType.COOKING

    def test_skill_gain_callback(self):
        """Test callback for skill gains."""
        system = SkillSystem()
        gains = []

        system.on_skill_gain(lambda skill, xp: gains.append((skill.skill_type, xp)))

        system.add_experience(SkillType.COOKING, 50.0)

        assert len(gains) == 1
        assert gains[0] == (SkillType.COOKING, 50.0)

    def test_serialization(self):
        """Test skill serialization."""
        system = SkillSystem()
        system.add_experience(SkillType.COOKING, 150.0)
        system.practice(SkillType.FITNESS, 2.0)

        data = system.to_dict()

        assert "cooking" in data
        assert "fitness" in data
        assert data["cooking"]["level"] >= 1

    def test_deserialization(self):
        """Test skill deserialization."""
        system = SkillSystem()

        data = {
            "cooking": {
                "level": 5,
                "experience": 50.0,
                "total_practice_time": 10.0
            }
        }

        system.from_dict(data)

        skill = system.get_skill(SkillType.COOKING)
        assert skill.level == 5
        assert skill.experience == 50.0
        assert skill.total_practice_time == 10.0
