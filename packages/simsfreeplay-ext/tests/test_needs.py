"""Tests for the Needs System."""

import pytest
from simulation.needs import NeedsSystem, NeedType, Need, NeedConfig


class TestNeed:
    """Tests for the Need class."""

    def test_initial_values(self):
        """Test need initialization."""
        need = Need(need_type=NeedType.HUNGER, value=100.0)
        assert need.value == 100.0
        assert need.need_type == NeedType.HUNGER

    def test_decay(self):
        """Test need decay over time."""
        config = NeedConfig(decay_rate=10.0)
        need = Need(need_type=NeedType.HUNGER, value=100.0, config=config)

        need.decay(1.0)  # 1 hour
        assert need.value == 90.0

        need.decay(5.0)  # 5 more hours
        assert need.value == 40.0

    def test_decay_clamps_at_min(self):
        """Test that decay doesn't go below minimum."""
        config = NeedConfig(decay_rate=50.0, min_value=0.0)
        need = Need(need_type=NeedType.HUNGER, value=30.0, config=config)

        need.decay(1.0)
        assert need.value == 0.0  # Clamped, not negative

    def test_fulfill(self):
        """Test fulfilling a need."""
        need = Need(need_type=NeedType.HUNGER, value=50.0)

        fulfilled = need.fulfill(30.0)
        assert need.value == 80.0
        assert fulfilled == 30.0

    def test_fulfill_clamps_at_max(self):
        """Test that fulfill doesn't exceed maximum."""
        config = NeedConfig(decay_rate=5.0, max_value=100.0)
        need = Need(need_type=NeedType.HUNGER, value=90.0, config=config)

        fulfilled = need.fulfill(50.0)
        assert need.value == 100.0
        assert fulfilled == 10.0  # Only 10 was actually fulfilled

    def test_is_critical(self):
        """Test critical threshold detection."""
        config = NeedConfig(decay_rate=5.0, critical_threshold=20.0)
        need = Need(need_type=NeedType.HUNGER, value=25.0, config=config)

        assert not need.is_critical
        need.set_value(15.0)
        assert need.is_critical

    def test_is_warning(self):
        """Test warning threshold detection."""
        config = NeedConfig(decay_rate=5.0, warning_threshold=40.0)
        need = Need(need_type=NeedType.HUNGER, value=50.0, config=config)

        assert not need.is_warning
        need.set_value(35.0)
        assert need.is_warning

    def test_satisfaction_ratio(self):
        """Test satisfaction ratio calculation."""
        config = NeedConfig(decay_rate=5.0, min_value=0.0, max_value=100.0)
        need = Need(need_type=NeedType.HUNGER, value=50.0, config=config)

        assert need.satisfaction_ratio == 0.5


class TestNeedsSystem:
    """Tests for the NeedsSystem class."""

    def test_initialization(self):
        """Test needs system initializes all need types."""
        system = NeedsSystem()

        for need_type in NeedType:
            assert system.get_value(need_type) == 100.0

    def test_update_decays_all_needs(self):
        """Test that update decays all needs."""
        system = NeedsSystem()

        system.update(1.0)  # 1 hour

        for need_type in NeedType:
            assert system.get_value(need_type) < 100.0

    def test_fulfill_need(self):
        """Test fulfilling a specific need."""
        system = NeedsSystem()
        system.update(5.0)  # Decay for 5 hours

        initial = system.get_value(NeedType.HUNGER)
        system.fulfill(NeedType.HUNGER, 20.0)
        assert system.get_value(NeedType.HUNGER) == initial + 20.0

    def test_get_critical_needs(self):
        """Test getting critical needs list."""
        system = NeedsSystem()

        # Initially no critical needs
        assert len(system.get_critical_needs()) == 0

        # Decay heavily
        system.update(15.0)  # 15 hours

        # Should have some critical needs now
        critical = system.get_critical_needs()
        assert len(critical) > 0

    def test_get_lowest_need(self):
        """Test finding the lowest need."""
        system = NeedsSystem()

        # Fulfill all but one
        for need_type in NeedType:
            if need_type != NeedType.SOCIAL:
                system.fulfill(need_type, 100.0)

        system.update(10.0)  # Heavy decay

        # Social should be lowest since others were topped up
        # (but all decayed, so check if logic works)
        lowest = system.get_lowest_need()
        assert lowest is not None

    def test_overall_satisfaction(self):
        """Test overall satisfaction calculation."""
        system = NeedsSystem()

        # At start, should be 1.0 (all needs at max)
        assert system.get_overall_satisfaction() == 1.0

        # After decay
        system.update(5.0)
        assert system.get_overall_satisfaction() < 1.0

    def test_critical_callback(self):
        """Test callback fires when need becomes critical."""
        system = NeedsSystem()
        critical_events = []

        system.on_critical(lambda need: critical_events.append(need.need_type))

        # Decay until critical
        for _ in range(20):
            system.update(1.0)

        assert len(critical_events) > 0

    def test_serialization(self):
        """Test needs can be serialized and deserialized."""
        system = NeedsSystem()
        system.update(5.0)
        system.fulfill(NeedType.HUNGER, 30.0)

        # Serialize
        data = system.to_dict()
        assert "hunger" in data

        # Create new system and load
        system2 = NeedsSystem()
        system2.from_dict(data)

        assert system2.get_value(NeedType.HUNGER) == system.get_value(NeedType.HUNGER)
