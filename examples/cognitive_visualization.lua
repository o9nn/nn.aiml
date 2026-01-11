#!/usr/bin/env th
--[[
   Cognitive Visualization Example

   This example demonstrates the visualization capabilities
   for meta-cognitive neural networks, including:
   - Real-time cognitive state tracking
   - ASCII-based visualizations
   - Dashboard views
   - Data export for external tools

   Usage: th examples/cognitive_visualization.lua
]]

require 'nn'

print("="..string.rep("=", 60))
print("NN.AIML Cognitive Visualization Demo")
print("="..string.rep("=", 60))

-- Step 1: Create a base neural network
print("\n[1] Creating base neural network...")
local baseNetwork = nn.Sequential()
   :add(nn.Linear(20, 40))
   :add(nn.Tanh())
   :add(nn.Linear(40, 20))
   :add(nn.Tanh())
   :add(nn.Linear(20, 10))

print("    Base network: 20 -> 40 -> 20 -> 10")

-- Step 2: Wrap with nested meta-cognition
print("\n[2] Adding nested meta-cognition (3 levels)...")
local metaNetwork = nn.NestedMetaCognition(baseNetwork, 3)

-- Step 3: Make it self-aware with AIML
print("[3] Adding self-awareness and AIML interface...")
local selfAwareNet = nn.SelfAwareNetwork(metaNetwork, true)

-- Step 4: Apply advanced AIML patterns
print("[4] Loading advanced AIML patterns...")
local advancedPatterns = nn.AdvancedAIMLPatterns()
advancedPatterns:applyTo(selfAwareNet:getAIML())
print("    Loaded " .. advancedPatterns:getPatternCount() .. " cognitive patterns")

-- Step 5: Create visualizer and attach to network
print("\n[5] Creating cognitive visualizer...")
local visualizer = nn.CognitiveVisualizer({
   width = 50,
   height = 10,
   maxHistory = 100
})
visualizer:trackNetwork(selfAwareNet)

-- Step 6: Simulate training with cognitive tracking
print("\n[6] Simulating training (50 iterations)...")
print("    Capturing cognitive state at each step...")

local criterion = nn.MSECriterion()
local learningRate = 0.01

for i = 1, 50 do
   -- Create random input/target
   local input = torch.randn(8, 20)
   local target = torch.randn(8, 10)

   -- Forward pass
   local output = selfAwareNet:forward(input)
   local loss = criterion:forward(output, target)

   -- Backward pass
   local gradOutput = criterion:backward(output, target)
   selfAwareNet:backward(input, gradOutput)

   -- Update parameters (simplified)
   selfAwareNet:updateParameters(learningRate)
   selfAwareNet:zeroGradParameters()

   -- Capture cognitive state for visualization
   visualizer:captureState()

   -- Progress indicator
   if i % 10 == 0 then
      io.write(string.format("    Iteration %d/50 - Loss: %.4f\n", i, loss))
   end
end

-- Step 7: Display visualizations
print("\n" .. string.rep("=", 62))
print("VISUALIZATION RESULTS")
print(string.rep("=", 62))

-- Dashboard view
print("\n--- Cognitive Dashboard ---")
print(visualizer:showDashboard())

-- Individual metric plots
print("\n--- Confidence Level Plot ---")
print(visualizer:plotConfidence())

print("\n--- Stability Plot ---")
print(visualizer:plotStability())

-- Heatmap view
print("\n--- Confidence Heatmap ---")
print(visualizer:showCognitiveHeatmap())

-- Statistics
print(visualizer:printStatistics())

-- Step 8: Demonstrate conversational queries about visualization
print("\n" .. string.rep("=", 62))
print("CONVERSATIONAL INTROSPECTION")
print(string.rep("=", 62))

local queries = {
   "STATUS",
   "HOW IS YOUR TRAINING",
   "WHAT IS YOUR COGNITIVE LOAD",
   "HOW CAN YOU IMPROVE"
}

local input = torch.randn(1, 20)
for _, query in ipairs(queries) do
   print("\nUser: " .. query)
   local response = selfAwareNet:converse(query, input)
   print("Bot:  " .. response)
end

-- Step 9: Export data for external visualization tools
print("\n" .. string.rep("=", 62))
print("DATA EXPORT")
print(string.rep("=", 62))

local success, filepath = visualizer:exportJSON("cognitive_data.json")
if success then
   print("\n[+] Exported JSON data to: " .. filepath)
else
   print("\n[-] JSON export failed: " .. filepath)
end

success, filepath = visualizer:exportCSV("cognitive_data.csv")
if success then
   print("[+] Exported CSV data to: " .. filepath)
else
   print("[-] CSV export failed: " .. filepath)
end

-- Step 10: Show final summary
print("\n" .. string.rep("=", 62))
print("SUMMARY")
print(string.rep("=", 62))
print(string.format("\nTotal iterations tracked: %d", visualizer:getIteration()))
print(string.format("Visualizer state: %s", tostring(visualizer)))

local stats = visualizer:getStatistics()
print("\nFinal Cognitive State:")
print(string.format("  - Confidence: %.4f (trend: %+.4f)",
   stats.confidence.latest, stats.confidence.trend))
print(string.format("  - Stability:  %.4f (trend: %+.4f)",
   stats.stability.latest, stats.stability.trend))
print(string.format("  - Convergence: %.4f (trend: %+.4f)",
   stats.convergence.latest, stats.convergence.trend))

print("\n" .. string.rep("=", 62))
print("Demo complete!")
print(string.rep("=", 62))
