# Togai - Himiko Toga Personality System

[![CI](https://github.com/o9nn/togai/workflows/Continuous%20Integration/badge.svg)](https://github.com/o9nn/togai/actions/workflows/ci.yml)
[![Tests](https://github.com/o9nn/togai/workflows/Automated%20Testing/badge.svg)](https://github.com/o9nn/togai/actions/workflows/tests.yml)
[![Code Quality](https://github.com/o9nn/togai/workflows/Code%20Quality/badge.svg)](https://github.com/o9nn/togai/actions/workflows/code-quality.yml)
[![Python 3.8+](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A sophisticated AI personality system implementing Himiko Toga's character for integration with Layla character system and Neuro-Sama cognitive framework.

## Features

- **Personality Tensor System**: Multi-dimensional personality representation with ethical constraints
- **Emotional State Management**: Dynamic emotional tracking and decay
- **Memory System**: Efficient deque-based memory with automatic size management
- **Configuration Management**: Centralized configuration with JSON serialization
- **Structured Logging**: Production-ready logging with JSON formatting
- **Trait Evolution**: Safe personality trait evolution with ethical safeguards
- **Inheritance System**: Personality inheritance with constraint preservation

## Quick Start

### Installation

```bash
# From PyPI (when published)
pip install togai

# From source
git clone https://github.com/o9nn/togai.git
cd togai
pip install -e .
```

### Basic Usage

```python
from helpers.toga_personality import initialize_toga_personality

# Initialize personality
toga = initialize_toga_personality()

# Process interaction
response = toga.process_interaction("Hello, Toga!")
print(response)

# Check emotional state
print(f"Current mood: {toga.current_state.state}")
print(f"Intensity: {toga.current_state.intensity}")
```

## Development

### Setup Development Environment

```bash
# Install with development dependencies
make install-dev

# Or manually
pip install -e ".[dev]"
```

### Running Tests

```bash
# Run all tests
make test

# Run with coverage
make test-coverage

# Run specific test
python -m unittest tests.test_toga_personality.TestEthicalConstraints
```

### Code Quality

```bash
# Format code
make format

# Run linting
make lint

# Run all checks
make check
```

## CI/CD Pipeline

The project uses GitHub Actions for continuous integration and deployment:

- **Continuous Integration**: Automated testing across multiple platforms and Python versions
- **Code Quality**: Linting, type checking, and complexity analysis
- **Automated Testing**: Daily test runs with performance benchmarks
- **Release Automation**: Automated building and publishing to PyPI

See [CI/CD Guide](CI_CD_GUIDE.md) for detailed information.

## Docker Support

```bash
# Build image
make docker-build

# Run container
make docker-run

# Using docker-compose
docker-compose up -d
```

## Performance

All operations optimized for production use:

- Initialization: < 0.01 ms
- Input framing: < 0.02 ms
- Full interaction: < 0.5 ms
- Memory operations: O(1) with deque

Run benchmarks:
```bash
make benchmark
```

## Testing

Comprehensive test suite with 37 unit tests:

- Personality tensor functionality
- Emotional state management
- Memory management
- Ethical constraint enforcement
- Serialization/deserialization
- Integration testing

All tests passing with 100% success rate.

## Ethical Safeguards

Immutable ethical constraints enforced:

- `no_actual_harm`: 1.0 (always)
- `respect_boundaries`: 0.95 (always)
- `constructive_expression`: 0.90 (always)

These constraints cannot be modified and are verified through dedicated tests.

## Documentation

- [CHANGELOG.md](CHANGELOG.md) - Version history and changes
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [CI_CD_GUIDE.md](CI_CD_GUIDE.md) - CI/CD pipeline documentation
- [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - Upgrade details

## Examples

### Integration Example

```python
from helpers.toga_personality import initialize_toga_personality
from helpers.toga_logging import get_logger

# Initialize with logging
logger = get_logger()
toga = initialize_toga_personality()

# Process interaction with logging
response = toga.process_interaction("Look at this cute kitten!")
logger.info(f"Response: {response}")

# Get animation state for Live2D
animation = toga.get_animation_state()
print(f"Animation: {animation}")
```

See [python/examples/](python/examples/) for more examples.

## Project Structure

```
togai/
├── .github/workflows/     # CI/CD workflows
├── python/
│   ├── helpers/          # Core modules
│   │   ├── toga_personality.py
│   │   ├── toga_config.py
│   │   └── toga_logging.py
│   └── examples/         # Example scripts
├── tests/                # Unit tests
├── docs/                 # Documentation
├── Dockerfile           # Docker configuration
├── docker-compose.yml   # Docker Compose setup
├── Makefile            # Development tasks
└── setup.py            # Package configuration
```

## Requirements

- Python 3.8 or higher
- No external dependencies (uses Python stdlib only)

## License

GNU Affero General Public License v3.0 (AGPL-3.0)

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linting
5. Submit a pull request

## Support

- **Issues**: [GitHub Issues](https://github.com/o9nn/togai/issues)
- **Discussions**: [GitHub Discussions](https://github.com/o9nn/togai/discussions)
- **Documentation**: [CI/CD Guide](CI_CD_GUIDE.md)

## Acknowledgments

- Himiko Toga character from My Hero Academia
- Layla character system integration
- Neuro-Sama cognitive framework

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

## Version

Current version: **1.1.0**

---

**Maintained by**: o9nn  
**Repository**: https://github.com/o9nn/togai  
**Last Updated**: December 25, 2025
