# Multi-stage Dockerfile for Togai Personality System
# Optimized for production deployment

# Stage 1: Builder
FROM python:3.11-slim as builder

LABEL maintainer="o9nn"
LABEL description="Himiko Toga Personality System for Layla & Neuro-Sama"

# Set working directory
WORKDIR /build

# Install build dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    make \
    && rm -rf /var/lib/apt/lists/*

# Copy package files
COPY setup.py pyproject.toml requirements.txt README.md ./
COPY python/ ./python/

# Build wheel
RUN pip install --no-cache-dir --upgrade pip setuptools wheel && \
    pip wheel --no-cache-dir --wheel-dir /wheels .

# Stage 2: Runtime
FROM python:3.11-slim

LABEL maintainer="o9nn"
LABEL description="Himiko Toga Personality System - Runtime"
LABEL version="1.1.0"

# Create non-root user
RUN useradd -m -u 1000 -s /bin/bash togai

# Set working directory
WORKDIR /app

# Copy wheels from builder
COPY --from=builder /wheels /wheels

# Install package from wheel
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir /wheels/*.whl && \
    rm -rf /wheels

# Copy application files
COPY --chown=togai:togai python/ ./python/
COPY --chown=togai:togai tests/ ./tests/
COPY --chown=togai:togai README.md CHANGELOG.md ./

# Switch to non-root user
USER togai

# Set Python path
ENV PYTHONPATH=/app/python:$PYTHONPATH
ENV PYTHONUNBUFFERED=1

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "from helpers.toga_personality import initialize_toga_personality; toga = initialize_toga_personality(); print('OK')" || exit 1

# Default command: run demo
CMD ["python", "python/examples/demo_toga.py"]

# Expose port for potential web interface
EXPOSE 8000

# Volume for persistent data
VOLUME ["/app/data"]
