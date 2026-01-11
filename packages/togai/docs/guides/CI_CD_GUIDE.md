# CI/CD Guide for Togai

This document provides comprehensive information about the continuous integration and deployment pipeline for the Togai personality system.

## Overview

The Togai project uses GitHub Actions for automated testing, building, and deployment. The CI/CD pipeline ensures code quality, runs comprehensive tests, and automates the release process.

## Workflows

### 1. Continuous Integration (ci.yml)

**Trigger**: Push to main/develop, Pull Requests  
**Purpose**: Validate code quality and functionality

**Jobs:**
- **Lint**: Code quality checks with Black, Flake8, and Pylint
- **Test**: Unit tests across multiple OS (Ubuntu, macOS, Windows) and Python versions (3.8-3.12)
- **Test Coverage**: Generate coverage reports
- **Integration Tests**: Run example scripts and benchmarks
- **Security Scan**: Bandit and Safety security scanning
- **Build Package**: Build Python distributions
- **CI Summary**: Aggregate results

**Usage:**
```bash
# Automatically runs on push/PR
git push origin main
```

### 2. Automated Testing (tests.yml)

**Trigger**: Push to python/tests, Daily schedule (2 AM UTC), Manual  
**Purpose**: Comprehensive testing suite

**Jobs:**
- **Unit Tests**: Full test suite across platforms
- **Ethical Constraints**: Verify immutable constraints
- **Performance Tests**: Benchmark performance thresholds
- **Memory Leak Test**: Detect memory issues
- **Compatibility Test**: Verify backward compatibility
- **Test Summary**: Aggregate test results

**Performance Thresholds:**
- Initialization: < 1.0 ms
- Interaction: < 2.0 ms
- Memory: Bounded to 100 items

**Usage:**
```bash
# Manual trigger
gh workflow run tests.yml

# Automatically runs daily
```

### 3. Release & Deployment (release.yml)

**Trigger**: Tag push (v*.*.*), Manual workflow dispatch  
**Purpose**: Build and publish releases

**Jobs:**
- **Validate Release**: Run tests and verify version consistency
- **Build Packages**: Create source and wheel distributions
- **Test Installation**: Verify installation across platforms
- **Create GitHub Release**: Generate release notes and artifacts
- **Publish PyPI**: Upload to Python Package Index
- **Publish Test PyPI**: Upload to test repository
- **Docker Build**: Create Docker images
- **Release Summary**: Aggregate release status

**Usage:**
```bash
# Create and push tag
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# Manual release
gh workflow run release.yml -f version=1.2.0 -f prerelease=false
```

### 4. Code Quality (code-quality.yml)

**Trigger**: Push to main/develop, Pull Requests, Manual  
**Purpose**: Enforce code quality standards

**Jobs:**
- **Formatting**: Black and isort checks
- **Linting**: Flake8, Pylint, pycodestyle
- **Type Checking**: mypy and pyright
- **Complexity Analysis**: Radon complexity metrics
- **Documentation Check**: Docstring coverage and style
- **Dependency Audit**: Security vulnerability scanning
- **Code Duplication**: Detect duplicate code
- **Quality Summary**: Aggregate quality metrics

**Usage:**
```bash
# Fix formatting issues
make format

# Run linting
make lint
```

## GitHub Secrets Required

For full CI/CD functionality, configure these secrets in GitHub repository settings:

### PyPI Publishing
- `PYPI_API_TOKEN`: API token for PyPI (production)
- `TEST_PYPI_API_TOKEN`: API token for Test PyPI (testing)

### Docker Registry (Optional)
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password/token

## Local Development

### Setup Development Environment

```bash
# Clone repository
git clone https://github.com/o9nn/togai.git
cd togai

# Install with dev dependencies
make install-dev
```

### Run Tests Locally

```bash
# Run all tests
make test

# Run with verbose output
make test-verbose

# Generate coverage report
make test-coverage
```

### Code Quality Checks

```bash
# Format code
make format

# Run linters
make lint

# Run all checks
make check
```

### Build Package

```bash
# Clean and build
make build

# Check build artifacts
ls -lh dist/
```

## Docker Support

### Build Docker Image

```bash
# Using Makefile
make docker-build

# Using docker directly
docker build -t togai:latest .
```

### Run Docker Container

```bash
# Using Makefile
make docker-run

# Using docker directly
docker run --rm -it togai:latest
```

### Docker Compose

```bash
# Start services
make docker-compose-up

# Stop services
make docker-compose-down

# Development mode
docker-compose --profile dev up
```

## Release Process

### Automated Release (Recommended)

1. **Update Version**
   ```bash
   # Update version in setup.py and python/__init__.py
   vim setup.py python/__init__.py
   ```

2. **Update CHANGELOG.md**
   ```bash
   # Document changes
   vim CHANGELOG.md
   ```

3. **Commit Changes**
   ```bash
   git add .
   git commit -m "chore: Prepare release v1.2.0"
   git push origin main
   ```

4. **Create and Push Tag**
   ```bash
   git tag -a v1.2.0 -m "Release version 1.2.0"
   git push origin v1.2.0
   ```

5. **Workflow Automatically**:
   - Runs all tests
   - Builds distributions
   - Creates GitHub release
   - Publishes to PyPI
   - Builds Docker image

### Manual Release

```bash
# Build locally
make release

# Upload to PyPI
twine upload dist/*
```

## Continuous Monitoring

### Test Results

View test results in GitHub Actions:
- Navigate to Actions tab
- Select workflow run
- Review job outputs and artifacts

### Coverage Reports

Coverage reports are uploaded as artifacts:
- Download from workflow run
- View `coverage.xml` or `htmlcov/` directory

### Performance Benchmarks

Benchmark results are available:
- Check `benchmark-results` artifact
- Review performance thresholds in test logs

## Troubleshooting

### Tests Failing

1. **Check test logs**: Review detailed output in GitHub Actions
2. **Run locally**: `make test-verbose`
3. **Check dependencies**: Ensure all dependencies installed
4. **Python version**: Verify Python version compatibility

### Build Failing

1. **Clean build**: `make clean && make build`
2. **Check MANIFEST.in**: Ensure all files included
3. **Verify setup.py**: Check package configuration
4. **Test installation**: `pip install -e .`

### Release Failing

1. **Version consistency**: Ensure versions match in setup.py and __init__.py
2. **Tag format**: Use `v*.*.*` format (e.g., v1.2.0)
3. **Secrets configured**: Verify PyPI tokens in GitHub secrets
4. **Tests passing**: Ensure all tests pass before release

## Best Practices

### Before Committing

```bash
# Format code
make format

# Run tests
make test

# Run linting
make lint

# Full check
make check
```

### Before Release

```bash
# Update version numbers
# Update CHANGELOG.md
# Run full cycle
make all

# Verify build
make release
```

### Code Review

- Ensure all CI checks pass
- Review test coverage
- Check code quality metrics
- Verify documentation updates

## Workflow Status Badges

Add these badges to README.md:

```markdown
![CI](https://github.com/o9nn/togai/workflows/Continuous%20Integration/badge.svg)
![Tests](https://github.com/o9nn/togai/workflows/Automated%20Testing/badge.svg)
![Code Quality](https://github.com/o9nn/togai/workflows/Code%20Quality/badge.svg)
```

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Python Packaging Guide](https://packaging.python.org/)
- [Docker Documentation](https://docs.docker.com/)
- [PyPI Publishing Guide](https://packaging.python.org/tutorials/packaging-projects/)

## Support

For issues with CI/CD:
1. Check workflow logs in GitHub Actions
2. Review this guide
3. Open an issue with workflow run link
4. Contact maintainers

---

**Last Updated**: December 25, 2025  
**Version**: 1.1.0
