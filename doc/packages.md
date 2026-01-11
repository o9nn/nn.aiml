# NN.AIML Integrated Packages

This directory contains integrated external packages that extend nn.aiml's capabilities across neural networks, cognitive systems, financial tools, and development utilities.

## Package Index

| Package | Description | Type |
|---------|-------------|------|
| **gneural-net** | GNU Neural Network library - C-based neural network implementation | Neural Networks |
| **dream-vortex** | Dream state visualization and cognitive processing system | Cognitive Systems |
| **ATenSpace** | ATen tensor space operations with ECAN and PLN implementations | Tensor Operations |
| **nanocyc** | Nanocognitive cycling framework with GGML tensor architecture | Cognitive Framework |
| **cogfin** | Cognitive financial analysis system (GnuCash-based) | Financial Tools |
| **virtunomicog** | Virtual economic cognition simulation platform | Economic Simulation |
| **dreamcog** | Dream cognition analysis and processing system | Cognitive Analysis |
| **llm** | Large Language Model integration utilities | LLM Integration |
| **simsfreeplay-ext** | Simulation extension framework with Lua schemas | Simulation |
| **gnucash-on-windows** | GnuCash Windows build system and dependencies | Build Tools |
| **azure-powershell** | Azure PowerShell modules and cmdlets | Cloud Integration |
| **nupkg** | NuGet packages (Microsoft Graph, AI, Gallery Modules) | .NET Packages |

## Package Details

### Neural Networks & Tensor Operations

#### gneural-net
GNU Neural Network library providing C-based neural network implementations with autotools build system.
- **Build**: `./configure && make`
- **License**: GPL

#### ATenSpace
ATen tensor space operations implementing:
- ECAN (Economic Attention Allocation)
- PLN (Probabilistic Logic Networks)
- Advanced tensor manipulations

### Cognitive Systems

#### dream-vortex
Full-stack dream state visualization system with:
- React/TypeScript client
- Drizzle ORM database layer
- Real-time cognitive processing

#### nanocyc
Nanocognitive cycling framework featuring:
- GGML tensor architecture integration
- Nanobrain cognitive processing
- Chapter-based learning system

#### dreamcog
Dream cognition analysis system with:
- Advanced feature extraction
- Schema-based processing
- Client-server architecture

#### virtunomicog
Virtual economic cognition simulation:
- Economic agent modeling
- Cognitive decision simulation
- Full-stack TypeScript implementation

### Financial & Economic Tools

#### cogfin
Cognitive financial analysis based on GnuCash:
- Extensive changelog history (2000-2024)
- CMake build system
- Financial data processing

#### gnucash-on-windows
Windows build system for GnuCash:
- MinGW64 bundling scripts
- JHBuild integration
- Inno Setup installer

### Integration & Utilities

#### llm
Large Language Model integration:
- TypeScript/Node.js implementation
- End-to-end testing framework
- Model abstraction layer

#### simsfreeplay-ext
Simulation extension framework:
- Lua schema definitions
- Native library bindings
- Python integration

#### azure-powershell
Azure PowerShell modules:
- 200+ service modules
- Docker deployment support
- Comprehensive documentation

### .NET Packages (nupkg)

| Package | Version | Description |
|---------|---------|-------------|
| microsoft.graph | 2.34.0 | Microsoft Graph API SDK |
| ai | 1.0.2 | AI integration utilities |
| download-allgallerymodules | 0.1.0 | Gallery module downloader |

## Integration with nn.aiml

These packages extend nn.aiml's core neural network capabilities:

```lua
-- Example: Using gneural-net with nn.aiml
require 'nn'
local gneural = require 'packages.gneural-net'

-- Create hybrid neural network
local net = nn.Sequential()
   :add(nn.Linear(10, 20))
   :add(gneural.GNeuralLayer(20, 15))  -- GNU Neural layer
   :add(nn.Tanh())
   :add(nn.Linear(15, 10))
```

## Building Packages

Most packages include their own build systems:

```bash
# gneural-net (autotools)
cd packages/gneural-net
./configure && make

# ATenSpace (CMake)
cd packages/ATenSpace
mkdir build && cd build
cmake .. && make

# Node.js packages
cd packages/llm
npm install

# TypeScript packages
cd packages/dream-vortex
pnpm install
```

## License

Each package maintains its original license. See individual package directories for specific licensing information.
