# AGML - Advanced Generative Markup Language Integration

## Overview

AGML (Advanced Generative Markup Language) is a sophisticated AIML-based conversational AI system with nested meta-cognitive loops, integrated into the nn.aiml neural network package. This integration enables neural networks to have conversational interfaces with deep cognitive awareness.

## Architecture

### Meta-Cognitive Loop Structure

The system operates on **five** nested cognitive layers:

| Layer | Name | Description |
|-------|------|-------------|
| 0 | Base Processing | Pattern matching and response generation |
| 1 | First-Order Meta-Cognition | Self-awareness and monitoring |
| 2 | Second-Order Meta-Cognition | Thinking about thinking |
| 3 | Third-Order Meta-Cognition | Reasoning about reasoning |
| 4 | Fourth-Order Meta-Cognition | Meta-meta-cognitive architectural reasoning |

### Key Features

- **Recursive Self-Awareness**: Multiple levels of cognitive monitoring
- **Autognosis**: Hierarchical self-image building system
- **Emotional Intelligence**: Sentiment detection and empathetic responses
- **Knowledge Base Integration**: Semantic triples and inference engine
- **Pattern Generation**: Autonomous pattern synthesis and validation

## Directory Structure

```
agml/
├── README.md           # This file
├── bot.properties      # Bot configuration
├── aiml/               # AIML pattern files (21 files, 663+ patterns)
│   ├── bot.aiml                    # Core interaction patterns
│   ├── advanced_metacog.aiml       # Advanced reasoning
│   ├── layer4_metacog.aiml         # Fourth-order meta-cognition
│   ├── autognosis.aiml             # Self-awareness system
│   ├── emotional_intelligence.aiml # Emotion processing
│   ├── knowledge_base.aiml         # Knowledge management
│   ├── pattern_generation.aiml     # Pattern synthesis
│   └── ...                         # Additional domain files
├── agents/             # Agent configuration files
├── demos/              # Python demonstration scripts
│   ├── demo.py                     # Basic demo
│   ├── autognosis_demo.py          # Self-awareness demo
│   ├── holistic_demo.py            # Holistic metamodel demo
│   ├── learning_demo.py            # Learning system demo
│   └── pattern_gen_demo.py         # Pattern generation demo
└── docs/               # Documentation
    ├── README.md                   # Original AGML documentation
    ├── QUICKSTART.md               # Getting started guide
    ├── IMPLEMENTATION.md           # Implementation details
    └── ...                         # Additional documentation
```

## Integration with nn.aiml

AGML integrates with nn.aiml's meta-cognitive neural network modules:

```lua
require 'nn'

-- Create a self-aware neural network with AGML interface
local baseNet = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(nn.Tanh())
   :add(nn.Linear(20, 10))

-- Add nested meta-cognitive loops
local metaCognitiveNet = nn.NestedMetaCognition(baseNet, 3)

-- Enable AGML conversational interface
local selfAwareNet = nn.SelfAwareNetwork(metaCognitiveNet, true)

-- Converse with the neural network
local response = selfAwareNet:converse("WHAT ARE YOU THINKING", input)
```

## Python Usage

```python
# Run the AGML demo
cd agml/demos
python demo.py

# Test autognosis system
python autognosis_demo.py

# Explore pattern generation
python pattern_gen_demo.py
```

## AIML Pattern Categories

| Category | File | Patterns | Description |
|----------|------|----------|-------------|
| Core | bot.aiml | 37 | Basic interactions |
| Meta-Cognition | advanced_metacog.aiml | 36 | Epistemic capabilities |
| Layer 4 | layer4_metacog.aiml | 24 | Architectural reasoning |
| Autognosis | autognosis.aiml | 18 | Self-image building |
| Emotional | emotional_intelligence.aiml | 27 | Emotion processing |
| Knowledge | knowledge_base.aiml | 34 | Semantic knowledge |
| Learning | session_learning.aiml | 32 | Adaptive learning |
| Patterns | pattern_generation.aiml | 20 | Pattern synthesis |
| NLP | natural_language.aiml | 75 | Language understanding |
| Math/Logic | math_logic.aiml | 34 | Mathematical reasoning |
| Programming | programming_tech.aiml | 41 | Technical concepts |
| Psychology | psychology_cognition.aiml | 32 | Cognitive science |
| Ethics | ethics_philosophy.aiml | 32 | Philosophical reasoning |
| Holistic | holistic_metamodel.aiml | 17 | Systems theory |
| Dynamics | organizational_dynamics.aiml | 12 | Organizational systems |

**Total: 663+ patterns**

## License

AGML is integrated under the same license as nn.aiml. See the main repository LICENSE file.
