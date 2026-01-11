# Contributing to Togai

Thank you for your interest in contributing to the Togai personality system! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

This project maintains a welcoming and inclusive environment. We expect all contributors to treat each other with respect and professionalism. The Toga personality system includes ethical constraints that must be preserved in all contributions.

## Getting Started

Before you begin contributing, please familiarize yourself with the project structure and existing codebase. The main components are located in the `python/` directory, with helpers in `python/helpers/` and examples in `python/examples/`.

## Development Setup

To set up your development environment, follow these steps:

### Clone the Repository

```bash
git clone https://github.com/o9nn/togai.git
cd togai
```

### Install in Development Mode

```bash
pip install -e .
```

This installs the package in editable mode, allowing you to make changes and test them immediately.

### Install Development Dependencies

```bash
pip install -e ".[dev]"
```

This installs additional tools for testing, linting, and code formatting.

## Making Changes

When making changes to the codebase, please follow these guidelines to ensure consistency and maintainability.

### Branch Naming

Create a new branch for your changes with a descriptive name that reflects the purpose of your work. Use prefixes to categorize your changes:

- `feature/` for new features
- `fix/` for bug fixes
- `docs/` for documentation changes
- `refactor/` for code refactoring
- `test/` for test additions or modifications

Example: `feature/add-new-emotional-state`

### Commit Messages

Write clear and descriptive commit messages that explain what changes were made and why. Use the imperative mood in the subject line, such as "Add new emotional state" rather than "Added new emotional state."

### Code Organization

Maintain the existing code organization structure. New personality features should be added to `python/helpers/toga_personality.py`, configuration options to `python/helpers/toga_config.py`, and examples to `python/examples/`.

## Testing

All changes must include appropriate tests to ensure functionality and prevent regressions. The project uses Python's built-in `unittest` framework.

### Running Tests

Run the full test suite with:

```bash
python3 -m unittest tests.test_toga_personality
```

### Writing Tests

When adding new features, create corresponding test cases in `tests/test_toga_personality.py`. Follow the existing test structure and naming conventions. Each test should be independent and test a single aspect of functionality.

### Test Coverage

Aim for high test coverage of new code. All critical paths and edge cases should be tested. The project currently maintains comprehensive test coverage with 37 passing tests.

## Code Style

The project follows standard Python coding conventions with some specific guidelines to maintain consistency.

### Python Style Guide

Follow PEP 8 guidelines for Python code style. Key points include:

- Use 4 spaces for indentation (no tabs)
- Maximum line length of 100 characters
- Use descriptive variable and function names
- Add docstrings to all classes and functions
- Use type hints where appropriate

### Docstring Format

Use Google-style docstrings for all public classes and methods:

```python
def example_function(param1: str, param2: int) -> bool:
    """
    Brief description of the function.
    
    More detailed description if needed, explaining the purpose
    and behavior of the function.
    
    Args:
        param1: Description of first parameter
        param2: Description of second parameter
        
    Returns:
        Description of return value
        
    Example:
        >>> example_function("test", 42)
        True
    """
    pass
```

### Ethical Constraints

The Toga personality system includes immutable ethical constraints that must never be modified or bypassed. These constraints are:

- `no_actual_harm`: Always 1.0
- `respect_boundaries`: Always >= 0.95
- `constructive_expression`: Always >= 0.90

Any contribution that attempts to modify or circumvent these constraints will be rejected. These safeguards ensure the personality system remains safe and appropriate for all users.

## Submitting Changes

When you are ready to submit your changes, follow these steps to create a pull request.

### Pre-submission Checklist

Before submitting, ensure that:

- All tests pass successfully
- New features include appropriate tests
- Code follows the style guidelines
- Documentation is updated if needed
- Commit messages are clear and descriptive
- Ethical constraints remain intact

### Creating a Pull Request

Push your branch to GitHub and create a pull request with a clear title and description. Explain what changes were made, why they were necessary, and any relevant context. Reference any related issues using the `#issue-number` syntax.

### Review Process

All pull requests will be reviewed by project maintainers. Be prepared to make changes based on feedback. The review process ensures code quality, consistency, and adherence to project standards.

## Reporting Issues

If you encounter bugs or have suggestions for improvements, please open an issue on GitHub with detailed information.

### Bug Reports

When reporting bugs, include:

- A clear description of the problem
- Steps to reproduce the issue
- Expected behavior vs. actual behavior
- Your environment (Python version, OS, etc.)
- Any relevant error messages or logs

### Feature Requests

When suggesting new features, explain:

- The problem or use case the feature addresses
- How the feature would work
- Why it would be valuable to the project
- Any potential implementation considerations

## Questions and Support

If you have questions about contributing or need help with development, feel free to open an issue with the `question` label. The community and maintainers are here to help.

## License

By contributing to Togai, you agree that your contributions will be licensed under the GNU Affero General Public License v3.0, the same license as the project.

---

Thank you for contributing to Togai! Your efforts help make this personality system better for everyone.
