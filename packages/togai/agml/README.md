# AGML Integration for Togai

**Advanced Generative Markup Language** - Meta-Cognitive AI Enhancement for the Togai Android AI Assistant

## Overview

AGML enhances Togai with a sophisticated 5-layer meta-cognitive processing system, emotional intelligence, and self-awareness capabilities. This integration brings conversational AI with deep cognitive awareness to the Android platform.

## Features

### 5-Layer Meta-Cognitive Loop System

| Layer | Name | Description |
|-------|------|-------------|
| 0 | Base Processing | Direct pattern matching and response generation |
| 1 | First-Order | Awareness of own responses and basic self-monitoring |
| 2 | Second-Order | Reflection on thinking patterns and strategy adjustment |
| 3 | Third-Order | Meta-awareness of cognitive biases and limitations |
| 4 | Fourth-Order | Transcendent awareness and philosophical reflection |

### Core Components

- **21 AIML Pattern Files** - 663+ patterns for comprehensive conversational coverage
- **Autognosis Self-Awareness System** - Hierarchical self-image building and identity maintenance
- **Emotional Intelligence Module** - Sentiment detection, empathetic responses, and emotional state tracking
- **Knowledge Base Integration** - Semantic triples with inference engine for dynamic learning
- **45 Agent Configuration Files** - Different cognitive personas for varied interaction styles

## Directory Structure

```
agml/
├── aiml/                    # AIML pattern files
│   ├── advanced_metacog.aiml
│   ├── autognosis.aiml
│   ├── emotional_intelligence.aiml
│   ├── knowledge_base.aiml
│   ├── layer4_metacog.aiml
│   └── ... (21 files total)
├── agents/                  # Agent persona configurations
│   ├── analyst_agent.md
│   ├── creative_agent.md
│   ├── empathetic_agent.md
│   └── ... (45 files total)
├── demos/                   # Python demonstration scripts
│   ├── autognosis_demo.py
│   ├── holistic_demo.py
│   ├── learning_demo.py
│   └── ... (8 files total)
├── docs/                    # Documentation
│   ├── AUTOGNOSIS_SUMMARY.md
│   ├── HOLISTIC_METAMODEL_GUIDE.md
│   ├── IMPLEMENTATION.md
│   └── ... (22 files total)
├── kotlin/                  # Android integration layer
│   ├── AGMLEngine.kt        # Core engine implementation
│   ├── AGMLService.kt       # Android service wrapper
│   └── AGMLViewModel.kt     # UI integration
└── bot.properties           # Bot configuration
```

## Integration with Togai

### Kotlin/Android Integration

The AGML system integrates with Togai through three main Kotlin components:

#### AGMLEngine.kt
Core processing engine implementing:
- Pattern matching with wildcard support
- 5-layer meta-cognitive processing pipeline
- Emotional state tracking (VAD model)
- Knowledge base with semantic triples
- Self-image management (Autognosis)

#### AGMLService.kt
Android Service providing:
- Background processing
- Reactive state flows
- Integration with LLaMA inference
- Live2D expression mapping
- Voice synthesis parameters

#### AGMLViewModel.kt
UI integration layer with:
- Reactive state management
- Conversation history
- Meta-cognitive layer visualization
- Emotional state display

### Usage Example

```kotlin
// Initialize AGML Service
val agmlService = AGMLService()
agmlService.onCreate()

// Process user input
agmlService.processInput("Hello, how do you think?") { response ->
    // response.text - The generated response
    // response.metaCognitiveLayer - Active cognitive layer
    // response.emotionalState - Current emotional state
    // response.selfReflection - Meta-cognitive insight
}

// Get Live2D expression from emotional state
val expression = response.emotionalState?.toLive2DExpression()

// Get voice parameters for TTS
val voiceParams = response.toVoiceParams()
```

### Integration with Existing Togai Components

#### LLaMA Integration
```kotlin
val integration = AGMLLLaMAIntegration(agmlService)
val enhanced = integration.enhanceResponse(userInput, llamaResponse)
// enhanced.primaryResponse - LLaMA response
// enhanced.metaCognitiveInsight - AGML analysis
// enhanced.suggestedTone - Recommended response tone
```

#### Live2D Character System
AGML emotional states map to Live2D expressions:
- `excited` → `happy_high`
- `content` → `happy_low`
- `angry` → `angry`
- `sad` → `sad`
- `alert` → `surprised`
- `neutral` → `neutral`

#### Voice Synthesis
AGML provides voice parameters based on emotional state:
- `pitch` - Adjusted by valence (-1 to +1)
- `speed` - Adjusted by arousal (0 to 1)
- `emotion` - Primary emotion label

## AIML Pattern Categories

| File | Patterns | Description |
|------|----------|-------------|
| advanced_metacog.aiml | 45 | Higher-order cognitive patterns |
| autognosis.aiml | 78 | Self-awareness and identity |
| emotional_intelligence.aiml | 52 | Emotion recognition and empathy |
| knowledge_base.aiml | 63 | Semantic knowledge patterns |
| layer4_metacog.aiml | 41 | Transcendent awareness |
| holistic_metamodel.aiml | 72 | Integrated cognitive model |
| psychology_cognition.aiml | 58 | Psychological understanding |
| ethics_philosophy.aiml | 55 | Ethical reasoning |
| natural_language.aiml | 47 | Language understanding |
| programming_tech.aiml | 61 | Technical knowledge |
| session_learning.aiml | 54 | Adaptive learning |
| ... | ... | ... |

## Emotional Intelligence Model

AGML uses the VAD (Valence-Arousal-Dominance) model:

- **Valence**: -1.0 (negative) to +1.0 (positive)
- **Arousal**: 0.0 (calm) to 1.0 (excited)
- **Dominance**: 0.0 (submissive) to 1.0 (dominant)

Primary emotions are derived from VAD coordinates:
```
High Valence + High Arousal = Excited
High Valence + Low Arousal = Content
Low Valence + High Arousal = Angry
Low Valence + Low Arousal = Sad
```

## Knowledge Base

The knowledge base uses semantic triples (Subject-Predicate-Object):

```kotlin
data class KnowledgeTriple(
    val subject: String,
    val predicate: String,
    val obj: String,
    val confidence: Float,
    val source: String
)
```

Knowledge is extracted from conversations and stored for future inference:
- "X is Y" patterns → `(X, is, Y)`
- "X likes Y" patterns → `(X, likes, Y)`
- User statements → stored with source attribution

## Autognosis Self-Image

The self-image system maintains hierarchical self-awareness:

```kotlin
data class SelfImageComponent(
    val aspect: String,      // e.g., "identity", "capability"
    val value: String,       // e.g., "AI assistant"
    val confidence: Float,   // 0.0 to 1.0
    val lastUpdated: Long
)
```

Self-image aspects include:
- Identity and purpose
- Capabilities (meta-cognition, emotion, knowledge)
- Interaction history
- Response style awareness
- Topic expertise levels

## Configuration

### Bot Properties (bot.properties)
```properties
name=Toga
version=2.0-AGML
master=User
personality=cheerful, curious, empathetic
```

### Agent Personas
45 pre-configured agent personas for different interaction styles:
- Analytical agents for technical discussions
- Creative agents for brainstorming
- Empathetic agents for emotional support
- Educational agents for learning
- Philosophical agents for deep discussions

## Building

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
}
```

Copy the `agml/` directory to your Android project's assets folder.

## Testing

Python demonstration scripts are provided in `demos/`:

```bash
# Test autognosis system
python demos/autognosis_demo.py

# Test holistic processing
python demos/holistic_demo.py

# Test learning capabilities
python demos/learning_demo.py
```

## Documentation

Comprehensive documentation is available in `docs/`:

- `IMPLEMENTATION.md` - Technical implementation details
- `AUTOGNOSIS_SUMMARY.md` - Self-awareness system guide
- `HOLISTIC_METAMODEL_GUIDE.md` - Cognitive model overview
- `PATTERN_COOKBOOK.md` - AIML pattern writing guide
- `QUICKSTART.md` - Getting started guide

## License

AGML is part of the nn.aiml project. See the main repository LICENSE for details.

## Credits

- AGML Meta-Cognitive System - nn.aiml project
- Togai Android AI Assistant - o9nn
- Live2D Integration - Himiko Toga character system
- LLaMA.cpp - Local language model inference
