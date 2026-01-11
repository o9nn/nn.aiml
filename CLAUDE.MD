# CLAUDE.MD - NN.AIML Project Context

## Project Overview

NN.AIML is a meta-cognitive neural network system built on top of the Torch7 `nn` library. It extends traditional neural networks with AIML (Artificial Intelligence Markup Language) conversational capabilities and nested meta-cognitive loops, enabling networks that can "learn about their learning" and "reason about their reasoning."

## Repository Structure

```
nn.aiml/
├── *.lua                    # Core nn modules (150+ files)
├── init.lua                 # Package entry point
├── test.lua                 # Main test suite
├── THNN.lua                 # THNN library bindings
├── lib/THNN/                # C backend library
├── doc/                     # Documentation
│   ├── architecture.md      # System architecture
│   ├── metacognitive_aiml.md # Meta-cognitive API reference
│   └── *.md                 # Other documentation
├── examples/                # Usage examples
│   ├── metacognitive_botnet.lua
│   └── README.md
├── test/                    # Additional test files
└── rocks/                   # LuaRocks package definitions
```

## Key Components

### Meta-Cognitive Modules (Core Innovation)
- **MetaCognitiveLoop.lua**: Wraps networks with meta-cognitive processing (~204 lines)
- **NestedMetaCognition.lua**: Hierarchical meta-cognition (3+ levels) (~229 lines)
- **SelfAwareNetwork.lua**: Self-monitoring with AIML interface (~261 lines)
- **MetaCognitiveAIML.lua**: Pattern-based conversational AI (~292 lines)
- **AdvancedAIMLPatterns.lua**: Extended pattern library with 25+ cognitive patterns (~320 lines)
- **MetaCognitiveBenchmark.lua**: Benchmarking utilities for performance measurement (~350 lines)
- **CognitiveVisualizer.lua**: ASCII visualization and data export for cognitive states (~420 lines)
- **TensorLogic.lua**: Tensor-based logical reasoning (relations, inference, embeddings) (~580 lines)
- **TensorLogicReasoner.lua**: Neural-symbolic hybrid reasoning module (~320 lines)

### Standard NN Modules
The library includes all standard Torch `nn` modules:
- **Containers**: Sequential, Parallel, Concat, ConcatTable
- **Linear Layers**: Linear, Bilinear, SparseLinear
- **Convolutions**: Spatial, Temporal, Volumetric variants
- **Activations**: ReLU, Tanh, Sigmoid, ELU, etc.
- **Normalization**: BatchNormalization, LayerNormalization
- **Criterions**: MSE, CrossEntropy, ClassNLL, etc.

## Development Guidelines

### Language & Framework
- **Primary Language**: Lua 5.1/5.2
- **Framework**: Torch7
- **C Backend**: THNN library (lib/THNN/)

### Code Style
- Follow existing Torch nn module patterns
- Use `torch.class()` for class definitions
- Implement standard methods: `__init`, `updateOutput`, `updateGradInput`, `accGradParameters`
- Proper tensor handling with cloning when necessary
- No TODO/FIXME comments in production code

### Module Structure Pattern
```lua
local ModuleName, Parent = torch.class('nn.ModuleName', 'nn.Module')

function ModuleName:__init(...)
   Parent.__init(self)
   -- initialization
end

function ModuleName:updateOutput(input)
   -- forward pass
   return self.output
end

function ModuleName:updateGradInput(input, gradOutput)
   -- backward pass
   return self.gradInput
end
```

### Testing
```bash
# Run all tests
th -lnn -e "nn.test()"

# Run specific test
th -lnn -e "nn.test{'ModuleName'}"

# Run meta-cognitive tests
th -lnn -e "nn.test{'MetaCognitiveLoop', 'NestedMetaCognition', 'SelfAwareNetwork', 'MetaCognitiveAIML'}"

# Run tensor logic tests
th -lnn -e "nn.test{'TensorLogic', 'TensorLogicReasoner'}"

# Run new module tests
th -lnn -e "nn.test{'AdvancedAIMLPatterns', 'MetaCognitiveBenchmark', 'CognitiveVisualizer'}"

# Run integration tests
th -lnn -e "nn.test{'IntegrationTensorLogicWithMetaCognition', 'IntegrationFullStack'}"
```

### Adding New Modules
1. Create `ModuleName.lua` in root directory
2. Add `require('nn.ModuleName')` to `init.lua` in appropriate section
3. Add corresponding tests to `test.lua`
4. Update documentation in `doc/`

## Build & Dependencies

### Requirements
- Torch7 (https://github.com/torch/torch7)
- THNN library (included in lib/THNN/)

### Installation
```bash
luarocks make rocks/nn-scm-1.rockspec
```

### Development Workflow
```bash
# Symlink for quick iteration
ln -sf $(pwd) ~/torch/install/share/lua/5.1/nn

# Rebuild after C changes
luarocks make rocks/*
```

## Current Status

**Phase**: Full Neural-Symbolic Integration with Tensor Logic

### Completed Features
- Meta-cognitive loop processing
- Nested meta-cognition (configurable depth)
- Self-aware network monitoring
- AIML conversational interface
- Comprehensive testing and documentation
- **Advanced AIML patterns** (25+ cognitive query patterns)
- **Benchmarking utilities** (timing, memory, cognitive evolution tracking)
- **Cognitive visualization** (ASCII plots, dashboards, heatmaps, data export)
- **Tensor Logic** (symbolic reasoning via tensor operations)
- **Neural-Symbolic Reasoner** (hybrid neural/symbolic inference)
- **Exhaustive unit tests** (260+ test functions)

### Suggested Next Steps
1. **PyTorch Port**: Port to PyTorch for wider adoption
2. **GPU Optimization**: Optimize tensor logic operations for CUDA
3. **Interactive Dashboard**: Web-based cognitive state monitoring
4. **Extended Knowledge Base**: Pre-built ontologies and rule sets

## Key Files for Context

| File | Purpose |
|------|---------|
| `init.lua` | Package initialization, all requires |
| `test.lua` | Main test suite (~10000 lines, 260+ tests) |
| `Module.lua` | Base module class |
| `Container.lua` | Base container class |
| `IMPLEMENTATION_SUMMARY.md` | Implementation overview |
| `COMPLETION_REPORT.md` | Project completion status |
| `VALIDATION_SUMMARY.md` | Quality validation report |

## Quick Reference

### Meta-Cognitive Usage
```lua
require 'nn'

-- Create base network
local net = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(nn.Tanh())
   :add(nn.Linear(20, 10))

-- Add meta-cognition (3 levels)
local metaNet = nn.NestedMetaCognition(net, 3)

-- Make self-aware with AIML
local selfAware = nn.SelfAwareNetwork(metaNet, true)

-- Use and converse
local output = selfAware:forward(torch.randn(5, 10))
local response = selfAware:converse("HOW ARE YOU", input)
```

### AIML Built-in Patterns
- `HELLO` - Greeting response
- `HOW ARE YOU` - Status with confidence
- `WHAT ARE YOU LEARNING` - Learning explanation
- `HOW DO YOU THINK` - Architecture description
- `WHAT IS YOUR CONFIDENCE` - Confidence level
- `EXPLAIN YOUR REASONING` - Cognitive introspection

### Tensor Logic Usage
```lua
-- Create tensor logic engine
local logic = nn.TensorLogic()

-- Define relations as tensors
local parent = logic:relation({
   {'alice', 'bob'},
   {'bob', 'charlie'}
}, 'parent')

-- Compute transitive closure (grandparent)
local ancestor = logic:forwardChain(parent, 5)

-- Query
local result = logic:query(parent, {'alice', 'bob'})  -- true

-- Logical operations
local conj = logic:conjunction(rel1, rel2)  -- AND
local disj = logic:disjunction(rel1, rel2)  -- OR
local neg = logic:negation(rel)             -- NOT

-- Neural-Symbolic Reasoner
local reasoner = nn.TensorLogicReasoner(10, 10, {
   reasoningMode = 'hybrid',  -- 'neural', 'symbolic', or 'hybrid'
   logicDepth = 3
})
reasoner:addFact('parent', {'alice', 'bob'})
reasoner:addRule('grandparent', {'parent', 'parent'})

local output = reasoner:forward(input)
local explanation = reasoner:explain()
```

## Important Notes

- This is a research-oriented implementation
- Overhead is ~10-20% per meta-cognitive level
- History buffers default to 50-100 entries
- All operations maintain backward compatibility with standard nn
