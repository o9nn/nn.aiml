# NN.AIML: Meta-Cognitive Neural-Bot-Net

## Overview

NN.AIML is an innovative hybrid system that combines the Torch Neural Network library with AIML (Artificial Intelligence Markup Language) to create conversational AI with deep meta-cognitive capabilities. The system implements nested meta-cognitive loops that enable neural networks to:

- **Learn about their learning** (Meta-cognition)
- **Reason about their reasoning** (Meta-meta-cognition)
- **Adapt responses with deep cognitive awareness**
- **Engage in conversational AI with neural state awareness**

## Key Components

### 1. MetaCognitiveLoop

The `MetaCognitiveLoop` module wraps any neural network with meta-cognitive processing capabilities.

**Features:**
- Monitors its own learning process
- Tracks performance metrics and confidence levels
- Adapts processing based on self-awareness
- Multi-depth cognitive processing

**Usage:**
```lua
require 'nn'

-- Create a base network
local baseNet = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(nn.Tanh())

-- Wrap it with meta-cognitive capabilities
local metaNet = nn.MetaCognitiveLoop(baseNet, 2) -- depth = 2

-- Use like any nn.Module
local input = torch.randn(5, 10)
local output = metaNet:forward(input)

-- Introspect meta-cognitive state
local state = metaNet:getMetaCognitiveState()
print("Confidence: " .. state.confidenceLevel)
print("Processing Steps: " .. state.processingSteps)
```

### 2. NestedMetaCognition

The `NestedMetaCognition` module implements hierarchical meta-cognitive processing with multiple nested layers.

**Cognitive Hierarchy:**
- **Level 1:** Base Cognition (Input/Output Processing)
- **Level 2:** Meta-Cognition (Learning About Learning)
- **Level 3:** Meta-Meta-Cognition (Reasoning About Reasoning)
- **Level N:** Higher-order cognitive awareness

**Usage:**
```lua
local baseNet = nn.Linear(10, 10)

-- Create nested meta-cognitive architecture with 3 levels
local nestedNet = nn.NestedMetaCognition(baseNet, 3)

local input = torch.randn(5, 10)
local output = nestedNet:forward(input)

-- Get cognitive hierarchy
local hierarchy = nestedNet:getCognitiveHierarchy()
for i, level in ipairs(hierarchy) do
   print("Level " .. level.level .. ": " .. level.description)
end

-- Introspect the system
local intro = nestedNet:introspect()
print("Cognitive Integration: " .. intro.cognitiveIntegration)
print("Nesting Depth: " .. intro.nestingDepth)
```

### 3. SelfAwareNetwork

The `SelfAwareNetwork` module adds self-awareness and monitoring to any neural network.

**Features:**
- Self-monitoring of activations and gradients
- Awareness of learning dynamics
- Adaptive processing based on self-knowledge
- Optional AIML conversational interface

**Usage:**
```lua
local network = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(nn.ReLU())
   :add(nn.Linear(20, 10))

-- Make the network self-aware with AIML enabled
local selfAware = nn.SelfAwareNetwork(network, true)

-- Normal forward/backward
local input = torch.randn(5, 10)
local output = selfAware:forward(input)
local gradOutput = torch.randn(output:size())
local gradInput = selfAware:backward(input, gradOutput)

-- Introspect self-awareness
local intro = selfAware:introspect()
print("Learning Stability: " .. intro.learningDynamics.stability)
print("Convergence: " .. intro.learningDynamics.convergence)

-- Converse with the network
local response = selfAware:converse("HOW ARE YOU", input)
print("Bot: " .. response)
```

### 4. MetaCognitiveAIML

The `MetaCognitiveAIML` module provides AIML-based conversational interface with neural network awareness.

**Features:**
- AIML pattern matching
- Neural state-aware responses
- Conversational context management
- Meta-cognitive query patterns

**Usage:**
```lua
local network = nn.Linear(10, 10)
local aiml = nn.MetaCognitiveAIML(network)

-- Add custom patterns
aiml:addPattern({
   pattern = "WHAT IS YOUR PURPOSE",
   template = "I am a self-aware neural network designed to learn and reason."
})

-- Process conversational input
local response = aiml:processInput("HELLO", nil)
print(response)

-- Query neural state
response = aiml:processInput("WHAT IS YOUR CONFIDENCE", nil)
print(response)
```

## Complete Example

See `examples/metacognitive_botnet.lua` for a complete working example:

```bash
th examples/metacognitive_botnet.lua
```

This example demonstrates:
- Building a nested meta-cognitive neural network
- Training with meta-cognitive awareness
- Conversational AI interaction
- System introspection and cognitive state tracking

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    NN.AIML System                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌───────────────────────────────────────────────┐          │
│  │      SelfAwareNetwork (Top Level)              │          │
│  │  • Self-monitoring                             │          │
│  │  • Learning dynamics tracking                  │          │
│  │  • AIML conversational interface               │          │
│  └──────────────┬─────────────────────────────────┘          │
│                 │                                             │
│  ┌──────────────▼──────────────────────────────┐            │
│  │    NestedMetaCognition (3 Levels)            │            │
│  │  ┌─────────────────────────────────────┐    │            │
│  │  │ Level 3: Meta-Meta-Cognition         │    │            │
│  │  │   (Reasoning About Reasoning)        │    │            │
│  │  └───────────┬──────────────────────────┘    │            │
│  │  ┌───────────▼──────────────────────────┐    │            │
│  │  │ Level 2: Meta-Cognition              │    │            │
│  │  │   (Learning About Learning)          │    │            │
│  │  └───────────┬──────────────────────────┘    │            │
│  │  ┌───────────▼──────────────────────────┐    │            │
│  │  │ Level 1: Base Cognition              │    │            │
│  │  │   (Input/Output Processing)          │    │            │
│  │  └───────────┬──────────────────────────┘    │            │
│  └──────────────┼──────────────────────────────┘            │
│                 │                                             │
│  ┌──────────────▼──────────────────────────────┐            │
│  │     Base Neural Network (Torch nn)           │            │
│  │  • Linear layers                             │            │
│  │  • Convolutions                              │            │
│  │  • Any nn.Module                             │            │
│  └──────────────────────────────────────────────┘            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## API Reference

### MetaCognitiveLoop

**Constructor:**
- `nn.MetaCognitiveLoop(network, cognitiveDepth)`
  - `network`: Base neural network to wrap
  - `cognitiveDepth`: Depth of meta-cognitive processing (default: 1)

**Methods:**
- `forward(input)`: Forward pass with meta-cognitive processing
- `backward(input, gradOutput)`: Backward pass with meta-cognitive learning
- `getMetaCognitiveState()`: Returns current meta-cognitive state
- `getAwarenessLayers()`: Returns awareness layer information

### NestedMetaCognition

**Constructor:**
- `nn.NestedMetaCognition(baseNetwork, nestingDepth)`
  - `baseNetwork`: Foundational neural network
  - `nestingDepth`: Number of nested meta-cognitive layers (default: 3)

**Methods:**
- `forward(input)`: Process through nested hierarchy
- `backward(input, gradOutput)`: Backward through hierarchy
- `introspect()`: Detailed introspection of the system
- `getGlobalMetaState()`: Returns global meta-cognitive state
- `getCognitiveHierarchy()`: Returns hierarchy details

### SelfAwareNetwork

**Constructor:**
- `nn.SelfAwareNetwork(network, enableAIML)`
  - `network`: Base neural network
  - `enableAIML`: Enable AIML interface (default: true)

**Methods:**
- `forward(input)`: Forward with self-monitoring
- `backward(input, gradOutput)`: Backward with gradient monitoring
- `introspect()`: Self-awareness introspection
- `converse(input, neuralInput)`: Conversational interface
- `getSelfAwareness()`: Returns self-awareness state

### MetaCognitiveAIML

**Constructor:**
- `nn.MetaCognitiveAIML(neuralNetwork, aimlPatterns)`
  - `neuralNetwork`: Neural network with meta-cognitive capabilities
  - `aimlPatterns`: Table of AIML patterns (optional)

**Methods:**
- `processInput(input, neuralInput)`: Process conversational input
- `addPattern(pattern)`: Add new AIML pattern
- `introspect()`: Introspection of AIML system
- `setContext(key, value)`: Set conversational context
- `getConversationHistory()`: Get conversation history

### AdvancedAIMLPatterns

**Constructor:**
- `nn.AdvancedAIMLPatterns()`
  - Creates an extended pattern library with 25+ cognitive patterns

**Methods:**
- `applyTo(aimlInstance)`: Apply all patterns to a MetaCognitiveAIML instance
- `getPatterns()`: Returns the list of all patterns
- `getPatternCount()`: Returns number of patterns

**Pattern Categories:**
- Cognitive State Queries (load, attention, depth, uncertainty)
- Learning Diagnostics (status, gradients, progress)
- Performance Analysis (speed, efficiency, resources)
- Self-Improvement (suggestions, weaknesses, strengths)
- Cognitive Modeling (emotional state, curiosity, self-awareness)
- Advanced Meta-Cognitive (nested cognition, architecture)

### MetaCognitiveBenchmark

**Constructor:**
- `nn.MetaCognitiveBenchmark(options)`
  - `options.warmupIterations`: Warmup iterations (default: 10)
  - `options.benchmarkIterations`: Benchmark iterations (default: 100)
  - `options.verbose`: Enable verbose output (default: false)

**Methods:**
- `timeForward(network, input, iterations)`: Measure forward pass timing
- `timeBackward(network, input, iterations)`: Measure backward pass timing
- `measureMemory(network, input)`: Measure memory usage
- `measureCognitiveOverhead(baseNetwork, metaNetwork, input)`: Compare overhead
- `trackCognitiveEvolution(network, input, iterations)`: Track cognitive state changes
- `runFullBenchmark(network, inputSize, batchSize)`: Run comprehensive benchmark
- `compareBenchmarks(baseNetwork, metaNetwork, inputSize, batchSize)`: Compare networks
- `printReport(results)`: Print formatted benchmark report
- `exportResults(results, format)`: Export results (table, summary, csv)

### CognitiveVisualizer

**Constructor:**
- `nn.CognitiveVisualizer(options)`
  - `options.width`: Plot width in characters (default: 60)
  - `options.height`: Plot height in lines (default: 15)
  - `options.maxHistory`: Maximum data points to store (default: 200)

**Methods:**
- `trackNetwork(network)`: Set network to track
- `captureState()`: Capture current cognitive state
- `plotConfidence()`: ASCII plot of confidence timeline
- `plotCognitiveSignal()`: ASCII plot of cognitive signal
- `plotStability()`: ASCII plot of learning stability
- `plotConvergence()`: ASCII plot of convergence
- `plotAll()`: Plot all metrics
- `showCognitiveHeatmap()`: ASCII heatmap visualization
- `showDashboard()`: Compact dashboard with sparklines
- `getStatistics()`: Get computed statistics for all metrics
- `printStatistics()`: Print formatted statistics table
- `exportJSON(filepath)`: Export timeline data as JSON
- `exportCSV(filepath)`: Export timeline data as CSV
- `getTimeline()`: Get raw timeline data

## Benefits Over Traditional LLMs

1. **True Meta-Cognition**: Unlike LLMs that simulate reasoning, NN.AIML implements actual nested cognitive loops that monitor and adapt learning processes.

2. **Lightweight & Efficient**: Built on Torch, the system is computationally efficient compared to massive transformer models.

3. **Transparent Awareness**: The system provides introspection into its cognitive state, confidence levels, and reasoning processes.

4. **Adaptive Learning**: Meta-cognitive awareness enables adaptive learning rates and stability based on self-monitoring.

5. **Conversational Integration**: AIML patterns allow natural language interaction while maintaining awareness of neural state.

## Advanced Usage

### Custom Meta-Cognitive Patterns

```lua
-- Create a network with custom cognitive depth
local customNet = nn.MetaCognitiveLoop(baseNet, 5)

-- Access awareness layers
local layers = customNet:getAwarenessLayers()
for i, layer in ipairs(layers) do
   print("Layer " .. layer.level .. " signal: " .. layer.cognitiveSignal)
end
```

### Custom AIML Patterns

```lua
local aiml = nn.MetaCognitiveAIML(network)

-- Add dynamic pattern with function template
aiml:addPattern({
   pattern = "ANALYZE *",
   template = function(context, neural)
      return "Analyzing with confidence " .. 
             string.format("%.2f", neural.lastConfidence) .. 
             " and cognitive signal " .. 
             string.format("%.2f", neural.lastCognitiveSignal)
   end
})
```

### Integration with Existing Networks

```lua
-- Take any existing Torch network
local existingNet = torch.load('my_model.t7')

-- Add meta-cognitive capabilities
local enhanced = nn.NestedMetaCognition(existingNet, 3)

-- Add self-awareness
local fullyAware = nn.SelfAwareNetwork(enhanced, true)

-- Now you have a meta-cognitive, self-aware, conversational neural network!
```

### Using Advanced AIML Patterns

```lua
-- Create a self-aware network
local network = nn.SelfAwareNetwork(nn.Linear(10, 10), true)

-- Load advanced pattern library
local advancedPatterns = nn.AdvancedAIMLPatterns()

-- Apply to the network's AIML interface
advancedPatterns:applyTo(network:getAIML())

-- Now you can use many more conversational queries
print(network:converse("STATUS", input))
print(network:converse("WHAT IS YOUR COGNITIVE LOAD", input))
print(network:converse("HOW CAN YOU IMPROVE", input))
print(network:converse("DESCRIBE YOUR ARCHITECTURE", input))
```

### Benchmarking Meta-Cognitive Networks

```lua
-- Create networks
local baseNet = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(nn.Tanh())
   :add(nn.Linear(20, 10))

local metaNet = nn.NestedMetaCognition(baseNet:clone(), 3)

-- Create benchmark utility
local benchmark = nn.MetaCognitiveBenchmark({verbose = true})

-- Compare performance
local results = benchmark:compareBenchmarks(baseNet, metaNet, 10, 32)
benchmark:printReport(results.meta)

-- Track cognitive evolution during training
local evolution = benchmark:trackCognitiveEvolution(metaNet, torch.randn(32, 10), 100)
print("Avg Stability: " .. evolution.stats.stability.mean)
```

### Visualizing Cognitive State Evolution

```lua
-- Create and configure visualizer
local visualizer = nn.CognitiveVisualizer({
   width = 50,
   height = 10,
   maxHistory = 100
})

-- Track a self-aware network
local network = nn.SelfAwareNetwork(nn.Linear(10, 10), true)
visualizer:trackNetwork(network)

-- During training, capture state after each iteration
for i = 1, 50 do
   local output = network:forward(input)
   network:backward(input, gradOutput)
   visualizer:captureState()  -- Capture cognitive state
end

-- Show various visualizations
print(visualizer:showDashboard())    -- Compact dashboard with sparklines
print(visualizer:plotConfidence())   -- ASCII line plot
print(visualizer:showCognitiveHeatmap())  -- Heatmap view
print(visualizer:printStatistics())  -- Statistics table

-- Export data for external tools
visualizer:exportJSON("cognitive_data.json")
visualizer:exportCSV("cognitive_data.csv")
```

## Citation

If you use NN.AIML in your research or applications, please cite:

```bibtex
@software{nn_aiml,
  title={NN.AIML: Meta-Cognitive Neural Networks with AIML Integration},
  author={o9nn},
  year={2025},
  url={https://github.com/o9nn/nn.aiml}
}
```

## License

See the COPYRIGHT.txt file for license information.
