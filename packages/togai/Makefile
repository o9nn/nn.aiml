.PHONY: help install install-dev test test-verbose test-coverage lint format clean build docker-build docker-run release

# Default target
.DEFAULT_GOAL := help

# Colors
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)Togai - Himiko Toga Personality System$(NC)"
	@echo ""
	@echo "$(GREEN)Available targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'

install: ## Install package in development mode
	@echo "$(BLUE)Installing Togai...$(NC)"
	pip install -e .
	@echo "$(GREEN)✓ Installation complete$(NC)"

install-dev: ## Install package with development dependencies
	@echo "$(BLUE)Installing Togai with dev dependencies...$(NC)"
	pip install -e ".[dev]"
	@echo "$(GREEN)✓ Development installation complete$(NC)"

test: ## Run unit tests
	@echo "$(BLUE)Running unit tests...$(NC)"
	python -m unittest discover -s tests -p "test_*.py"
	@echo "$(GREEN)✓ Tests complete$(NC)"

test-verbose: ## Run unit tests with verbose output
	@echo "$(BLUE)Running unit tests (verbose)...$(NC)"
	python -m unittest discover -s tests -p "test_*.py" -v

test-coverage: ## Run tests with coverage report
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	pip install coverage
	coverage run -m unittest discover -s tests -p "test_*.py"
	coverage report -m
	coverage html
	@echo "$(GREEN)✓ Coverage report generated in htmlcov/$(NC)"

lint: ## Run code linting
	@echo "$(BLUE)Running linters...$(NC)"
	pip install flake8 pylint
	flake8 python/ --max-line-length=100 || true
	pylint python/helpers/ --max-line-length=100 --disable=C0111,C0103,R0913,R0914 || true
	@echo "$(GREEN)✓ Linting complete$(NC)"

format: ## Format code with black and isort
	@echo "$(BLUE)Formatting code...$(NC)"
	pip install black isort
	black python/
	isort python/
	@echo "$(GREEN)✓ Code formatted$(NC)"

clean: ## Clean build artifacts and cache
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	rm -rf build/
	rm -rf dist/
	rm -rf *.egg-info/
	rm -rf .eggs/
	find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete
	find . -type f -name "*.pyo" -delete
	find . -type f -name "*.so" -delete
	rm -rf .pytest_cache/
	rm -rf .mypy_cache/
	rm -rf htmlcov/
	rm -rf .coverage
	@echo "$(GREEN)✓ Cleanup complete$(NC)"

build: clean ## Build distribution packages
	@echo "$(BLUE)Building distribution packages...$(NC)"
	pip install build twine
	python -m build
	twine check dist/*
	@echo "$(GREEN)✓ Build complete - packages in dist/$(NC)"

docker-build: ## Build Docker image
	@echo "$(BLUE)Building Docker image...$(NC)"
	docker build -t togai:latest .
	@echo "$(GREEN)✓ Docker image built$(NC)"

docker-run: ## Run Docker container
	@echo "$(BLUE)Running Docker container...$(NC)"
	docker run --rm -it togai:latest

docker-compose-up: ## Start services with docker-compose
	@echo "$(BLUE)Starting services...$(NC)"
	docker-compose up -d
	@echo "$(GREEN)✓ Services started$(NC)"

docker-compose-down: ## Stop services
	@echo "$(BLUE)Stopping services...$(NC)"
	docker-compose down
	@echo "$(GREEN)✓ Services stopped$(NC)"

demo: ## Run demo script
	@echo "$(BLUE)Running demo...$(NC)"
	echo "n" | python python/examples/demo_toga.py

integration: ## Run integration example
	@echo "$(BLUE)Running integration example...$(NC)"
	python python/examples/integration_example.py

benchmark: ## Run performance benchmarks
	@echo "$(BLUE)Running benchmarks...$(NC)"
	python python/examples/benchmark_toga.py

release: build ## Prepare release (build and check)
	@echo "$(BLUE)Preparing release...$(NC)"
	@echo "$(YELLOW)Version:$(NC)"
	@python setup.py --version
	@echo ""
	@echo "$(YELLOW)Distribution files:$(NC)"
	@ls -lh dist/
	@echo ""
	@echo "$(GREEN)✓ Release ready$(NC)"
	@echo ""
	@echo "$(YELLOW)To publish to PyPI:$(NC)"
	@echo "  twine upload dist/*"

version: ## Show current version
	@python setup.py --version

check: lint test ## Run linting and tests
	@echo "$(GREEN)✓ All checks passed$(NC)"

all: clean install-dev test lint build ## Run full development cycle
	@echo "$(GREEN)✓ Full cycle complete$(NC)"
