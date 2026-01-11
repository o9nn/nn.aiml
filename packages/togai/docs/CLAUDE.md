# CLAUDE.md - Togai (9mly) Project Guide

## Project Overview

**Togai (9mly)** is a sophisticated Android AI assistant application featuring advanced artificial intelligence, character animation, task automation, and comprehensive document processing capabilities. Built on a foundation of 188,678+ source files with 459 native library integrations.

## Quick Start Commands

```bash
# Run Python tests/demos
python python/examples/demo_toga.py

# Check Python syntax
python3 -m py_compile python/helpers/toga_personality.py

# Build Kotlin code (requires network access to Maven/Gradle repositories)
gradle compileKotlin

# Run tests
gradle test

# Run shell demos
./toga_demo.sh
./layla_phase1_demo.sh
```

## Build Requirements

- **JDK 11+** (JDK 21 recommended)
- **Gradle 8.x** (8.14+ for Kotlin 2.x support)
- **Kotlin 1.9.25** (configured in build.gradle.kts)
- **Network access** to Gradle Plugin Portal and Maven Central for dependency resolution

**Note**: If Gradle cannot resolve dependencies, verify:
1. Network connectivity to `plugins.gradle.org` and `repo.maven.apache.org`
2. No corporate proxy/firewall blocking Maven repositories
3. Valid TLS/SSL certificates are installed

## Project Structure

```
togai/
├── src/
│   ├── main/kotlin/org/ninelym/    # Kotlin source code
│   │   ├── ai/                     # AI system (characters, inference, privacy)
│   │   │   └── wonderful/          # Advanced AI features (Toga, emotions, memory)
│   │   ├── cognitive/              # Cognitive computing engine
│   │   │   ├── ecan/               # ECAN attention system
│   │   │   ├── hypergraph/         # Hypergraph data structures
│   │   │   ├── metacognition/      # Meta-cognitive systems
│   │   │   ├── planetary/          # Planetary neural network
│   │   │   └── unification/        # Cognitive unification
│   │   └── layla/                  # Layla assistant services
│   │       ├── auth/               # Authentication
│   │       ├── billing/            # Billing system
│   │       ├── calendar/           # Calendar integration
│   │       ├── chat/               # Chat service
│   │       ├── character/          # Character system
│   │       ├── document/           # Document processing
│   │       ├── inference/          # AI inference
│   │       ├── privacy/            # Privacy features
│   │       ├── sd/                 # Stable Diffusion
│   │       ├── tasker/             # Tasker integration
│   │       └── voice/              # Voice I/O
│   ├── test/kotlin/                # Kotlin tests
│   └── smali/                      # Decompiled Android bytecode
├── python/
│   ├── helpers/
│   │   └── toga_personality.py     # Himiko Toga personality system
│   └── examples/
│       └── demo_toga.py            # Toga demo script
├── docs/                           # Documentation
├── ref/                            # Reference materials
├── build.gradle.kts                # Gradle build configuration
└── settings.gradle.kts             # Gradle settings
```

## Key Technologies

- **Language**: Kotlin (JVM 11)
- **Build System**: Gradle with Kotlin DSL
- **AI/ML**: LLaMA.cpp, ONNX Runtime, Stable Diffusion
- **Architecture**: Clean Architecture with MVVM pattern
- **Coroutines**: kotlinx-coroutines-core 1.8.0
- **Serialization**: kotlinx-serialization-json 1.6.2
- **Testing**: JUnit 4/5, kotlinx-coroutines-test

## Character System

The project includes multiple AI character personalities:

| Character | Personality | Traits |
|-----------|-------------|--------|
| Layla | Helpful assistant | Knowledgeable, friendly, empathetic |
| Aria | Creative companion | Imaginative, artistic, playful |
| Marcus | Analyst advisor | Logical, analytical, problem-solving |
| Himiko Toga | Chaotic persona | Cheerful, obsessive, unpredictable |

### Toga Personality System (Python)

```python
from python.helpers.toga_personality import initialize_toga_personality

toga = initialize_toga_personality()
response = toga.frame_input("This is so cute!")
# Returns: "Ehehe~ This is so cute! (So cuuute! I just want to become one with it~)"
```

**Ethical Constraints (Immutable)**:
- `no_actual_harm`: Always 1.0
- `respect_boundaries`: Always >= 0.95
- `constructive_expression`: Always >= 0.90

## Code Style & Conventions

### Kotlin
- Use `kotlinOptions.jvmTarget = "11"`
- Use `@OptIn` for experimental APIs
- Follow Kotlin coding conventions
- Use data classes for DTOs
- Prefer sealed classes for result types

### Python
- Use type hints (PEP 484)
- Use dataclasses for data structures
- Follow PEP 8 style guide
- Use descriptive variable names

## Testing

### Run All Tests
```bash
gradle test
```

### Run Specific Test Class
```bash
gradle test --tests "org.ninelym.cognitive.CognitivePrimitivesTest"
```

### Python Tests
```bash
python python/examples/demo_toga.py
```

## Common Tasks

### Adding a New Character
1. Create character definition in `CharacterSystem.kt`
2. Define personality traits and constraints
3. Add speech patterns and emotional states
4. Write tests in `src/test/kotlin/`

### Adding Cognitive Features
1. Implement in `src/main/kotlin/org/ninelym/cognitive/`
2. Create appropriate data types and interfaces
3. Integrate with existing cognitive engine
4. Add tests in corresponding test directory

## Debugging

### Enable Verbose Logging
```kotlin
// In CognitiveEngine.kt
private val DEBUG = true
```

### Check Compilation Errors
```bash
gradle compileKotlin --info
```

### Validate Python Syntax
```bash
python3 -m py_compile <file.py>
```

## API Patterns

### AI Response Pattern
```kotlin
sealed class AIResponse {
    data class Success(
        val text: String,
        val confidenceScore: Float,
        val processingTime: Long
    ) : AIResponse()

    data class Error(val message: String) : AIResponse()
}
```

### Cognitive Processing Pattern
```kotlin
sealed class ProcessingResult {
    data class Success(val message: String, val atoms: Any?) : ProcessingResult()
    data class Failure(val error: String) : ProcessingResult()
}
```

## Dependencies

Key dependencies (from build.gradle.kts):
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2`
- `junit:junit:4.13.2`

## Important Notes

1. **Privacy First**: All AI processing happens locally on-device
2. **No Internet Required**: Core AI features work offline
3. **Ethical AI**: Character personas have immutable ethical constraints
4. **Modular Design**: Features are isolated in separate services

## Phase Implementation Status

- Phase 1: Foundation (Complete) - 6 services, 21 tests
- Phase 2: Enhancement (Complete) - 6 services, 62 tests
- Phase 3: Integration (Complete) - 5 services, 35 tests
- Phase 4: Polish & Scale (Complete) - 4 services
- Phase 5: Meta-Cognition (Complete) - 3 services, 2,265 lines
- Phase 6: Testing & Unification (Complete) - 3 services, 1,336 lines

**Total**: 27 services, 207+ tests, 12,000+ lines of production code

## Related Documentation

- [Technical Architecture](./docs/TECHNICAL_ARCHITECTURE.md)
- [System Components](./docs/SYSTEM_COMPONENTS.md)
- [API Integration](./docs/API_INTEGRATION.md)
- [Toga Character Agent](./.github/agents/toga.md)
- [Cognitive Computing](./docs/COGNITIVE_COMPUTING.md)
