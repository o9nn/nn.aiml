local MetaCognitiveBenchmark = torch.class('nn.MetaCognitiveBenchmark')

--[[
   MetaCognitiveBenchmark: Benchmarking utilities for meta-cognitive neural networks.

   Provides tools to measure:
   - Forward/backward pass timing
   - Memory overhead per cognitive level
   - Cognitive state evolution
   - Comparison between standard and meta-cognitive networks
   - Throughput measurements

   Usage:
      local benchmark = nn.MetaCognitiveBenchmark()
      local results = benchmark:runFullBenchmark(network, inputSize, batchSize)
      benchmark:printReport(results)
]]

function MetaCognitiveBenchmark:__init(options)
   options = options or {}
   self.warmupIterations = options.warmupIterations or 10
   self.benchmarkIterations = options.benchmarkIterations or 100
   self.verbose = options.verbose or false
end

function MetaCognitiveBenchmark:timeForward(network, input, iterations)
   -- Measure forward pass time
   iterations = iterations or self.benchmarkIterations

   -- Warmup
   for _ = 1, self.warmupIterations do
      network:forward(input)
   end

   -- Benchmark
   local startTime = os.clock()
   for _ = 1, iterations do
      network:forward(input)
   end
   local endTime = os.clock()

   local totalTime = endTime - startTime
   return {
      totalTime = totalTime,
      iterations = iterations,
      avgTime = totalTime / iterations,
      throughput = iterations / totalTime
   }
end

function MetaCognitiveBenchmark:timeBackward(network, input, iterations)
   -- Measure backward pass time
   iterations = iterations or self.benchmarkIterations

   -- Get output for gradient shape
   local output = network:forward(input)
   local gradOutput = output:clone():fill(1)

   -- Warmup
   for _ = 1, self.warmupIterations do
      network:forward(input)
      network:backward(input, gradOutput)
   end

   -- Benchmark
   local startTime = os.clock()
   for _ = 1, iterations do
      network:forward(input)
      network:backward(input, gradOutput)
   end
   local endTime = os.clock()

   local totalTime = endTime - startTime
   return {
      totalTime = totalTime,
      iterations = iterations,
      avgTime = totalTime / iterations,
      throughput = iterations / totalTime
   }
end

function MetaCognitiveBenchmark:measureMemory(network, input)
   -- Measure memory usage (approximate via collectgarbage)
   collectgarbage('collect')
   local memBefore = collectgarbage('count')

   -- Run some iterations to populate buffers
   for _ = 1, 10 do
      local output = network:forward(input)
      local gradOutput = output:clone():fill(1)
      network:backward(input, gradOutput)
   end

   collectgarbage('collect')
   local memAfter = collectgarbage('count')

   return {
      memoryBefore = memBefore,
      memoryAfter = memAfter,
      memoryDelta = memAfter - memBefore,
      unit = "KB"
   }
end

function MetaCognitiveBenchmark:measureCognitiveOverhead(baseNetwork, metaNetwork, input)
   -- Compare timing between base network and meta-cognitive version
   local baseForward = self:timeForward(baseNetwork, input)
   local metaForward = self:timeForward(metaNetwork, input)

   local baseBackward = self:timeBackward(baseNetwork, input)
   local metaBackward = self:timeBackward(metaNetwork, input)

   local forwardOverhead = (metaForward.avgTime - baseForward.avgTime) / baseForward.avgTime * 100
   local backwardOverhead = (metaBackward.avgTime - baseBackward.avgTime) / baseBackward.avgTime * 100

   return {
      baseForward = baseForward,
      metaForward = metaForward,
      baseBackward = baseBackward,
      metaBackward = metaBackward,
      forwardOverheadPercent = forwardOverhead,
      backwardOverheadPercent = backwardOverhead,
      totalOverheadPercent = (forwardOverhead + backwardOverhead) / 2
   }
end

function MetaCognitiveBenchmark:trackCognitiveEvolution(network, input, iterations)
   -- Track how cognitive state evolves over iterations
   iterations = iterations or 50
   local evolution = {
      confidence = {},
      cognitiveSignal = {},
      stability = {},
      convergence = {}
   }

   local output = network:forward(input)
   local gradOutput = output:clone():fill(1)

   for i = 1, iterations do
      output = network:forward(input)
      network:backward(input, gradOutput)

      -- Capture cognitive state
      local state = self:getCognitiveState(network)
      table.insert(evolution.confidence, state.confidence)
      table.insert(evolution.cognitiveSignal, state.cognitiveSignal)
      table.insert(evolution.stability, state.stability)
      table.insert(evolution.convergence, state.convergence)
   end

   -- Calculate statistics
   evolution.stats = {
      confidence = self:computeStats(evolution.confidence),
      cognitiveSignal = self:computeStats(evolution.cognitiveSignal),
      stability = self:computeStats(evolution.stability),
      convergence = self:computeStats(evolution.convergence)
   }

   return evolution
end

function MetaCognitiveBenchmark:getCognitiveState(network)
   -- Extract cognitive state from various network types
   local state = {
      confidence = 1.0,
      cognitiveSignal = 0,
      stability = 1.0,
      convergence = 0
   }

   if torch.isTypeOf(network, 'nn.SelfAwareNetwork') then
      local awareness = network:getSelfAwareness()
      state.stability = awareness.learningDynamics.stability
      state.convergence = awareness.learningDynamics.convergence
      if network.aiml then
         state.confidence = network.aiml.neuralAwareness.lastConfidence
         state.cognitiveSignal = network.aiml.neuralAwareness.lastCognitiveSignal
      end
   elseif torch.isTypeOf(network, 'nn.NestedMetaCognition') then
      local globalState = network:getGlobalMetaState()
      state.cognitiveSignal = globalState.cognitiveIntegration
      local hierarchy = network:getCognitiveHierarchy()
      if #hierarchy > 0 then
         local topLoop = hierarchy[#hierarchy].network
         if torch.isTypeOf(topLoop, 'nn.MetaCognitiveLoop') then
            local loopState = topLoop:getMetaCognitiveState()
            state.confidence = loopState.confidenceLevel
         end
      end
   elseif torch.isTypeOf(network, 'nn.MetaCognitiveLoop') then
      local loopState = network:getMetaCognitiveState()
      state.confidence = loopState.confidenceLevel
      local layers = network:getAwarenessLayers()
      if #layers > 0 then
         state.cognitiveSignal = layers[1].cognitiveSignal
      end
   end

   return state
end

function MetaCognitiveBenchmark:computeStats(values)
   -- Compute basic statistics for a list of values
   if #values == 0 then
      return {mean = 0, std = 0, min = 0, max = 0}
   end

   local sum = 0
   local min = values[1]
   local max = values[1]

   for _, v in ipairs(values) do
      sum = sum + v
      if v < min then min = v end
      if v > max then max = v end
   end

   local mean = sum / #values

   local variance = 0
   for _, v in ipairs(values) do
      variance = variance + (v - mean) ^ 2
   end
   variance = variance / #values
   local std = math.sqrt(variance)

   return {
      mean = mean,
      std = std,
      min = min,
      max = max
   }
end

function MetaCognitiveBenchmark:runFullBenchmark(network, inputSize, batchSize, options)
   -- Run comprehensive benchmark
   options = options or {}
   inputSize = inputSize or 10
   batchSize = batchSize or 32

   local input = torch.randn(batchSize, inputSize)

   local results = {
      inputSize = inputSize,
      batchSize = batchSize,
      networkType = torch.type(network),
      timestamp = os.date("%Y-%m-%d %H:%M:%S")
   }

   -- Timing benchmarks
   if self.verbose then print("Running forward pass benchmark...") end
   results.forward = self:timeForward(network, input)

   if self.verbose then print("Running backward pass benchmark...") end
   results.backward = self:timeBackward(network, input)

   -- Memory benchmark
   if self.verbose then print("Measuring memory usage...") end
   results.memory = self:measureMemory(network, input)

   -- Cognitive evolution (if meta-cognitive)
   if torch.isTypeOf(network, 'nn.MetaCognitiveLoop') or
      torch.isTypeOf(network, 'nn.NestedMetaCognition') or
      torch.isTypeOf(network, 'nn.SelfAwareNetwork') then
      if self.verbose then print("Tracking cognitive evolution...") end
      results.cognitiveEvolution = self:trackCognitiveEvolution(network, input, options.evolutionIterations or 50)
   end

   return results
end

function MetaCognitiveBenchmark:compareBenchmarks(baseNetwork, metaNetwork, inputSize, batchSize)
   -- Compare base network with meta-cognitive version
   inputSize = inputSize or 10
   batchSize = batchSize or 32

   local input = torch.randn(batchSize, inputSize)

   local results = {
      inputSize = inputSize,
      batchSize = batchSize,
      baseNetworkType = torch.type(baseNetwork),
      metaNetworkType = torch.type(metaNetwork),
      timestamp = os.date("%Y-%m-%d %H:%M:%S")
   }

   -- Run benchmarks on both
   if self.verbose then print("Benchmarking base network...") end
   results.base = self:runFullBenchmark(baseNetwork, inputSize, batchSize)

   if self.verbose then print("Benchmarking meta-cognitive network...") end
   results.meta = self:runFullBenchmark(metaNetwork, inputSize, batchSize)

   -- Calculate overhead
   results.overhead = self:measureCognitiveOverhead(baseNetwork, metaNetwork, input)

   return results
end

function MetaCognitiveBenchmark:printReport(results)
   -- Print formatted benchmark report
   print("\n" .. string.rep("=", 60))
   print("META-COGNITIVE BENCHMARK REPORT")
   print(string.rep("=", 60))

   print("\nConfiguration:")
   print(string.format("  Network Type: %s", results.networkType or "N/A"))
   print(string.format("  Input Size: %d", results.inputSize or 0))
   print(string.format("  Batch Size: %d", results.batchSize or 0))
   print(string.format("  Timestamp: %s", results.timestamp or "N/A"))

   if results.forward then
      print("\nForward Pass:")
      print(string.format("  Total Time: %.4f s", results.forward.totalTime))
      print(string.format("  Iterations: %d", results.forward.iterations))
      print(string.format("  Avg Time: %.6f s", results.forward.avgTime))
      print(string.format("  Throughput: %.2f iter/s", results.forward.throughput))
   end

   if results.backward then
      print("\nBackward Pass:")
      print(string.format("  Total Time: %.4f s", results.backward.totalTime))
      print(string.format("  Iterations: %d", results.backward.iterations))
      print(string.format("  Avg Time: %.6f s", results.backward.avgTime))
      print(string.format("  Throughput: %.2f iter/s", results.backward.throughput))
   end

   if results.memory then
      print("\nMemory Usage:")
      print(string.format("  Before: %.2f %s", results.memory.memoryBefore, results.memory.unit))
      print(string.format("  After: %.2f %s", results.memory.memoryAfter, results.memory.unit))
      print(string.format("  Delta: %.2f %s", results.memory.memoryDelta, results.memory.unit))
   end

   if results.cognitiveEvolution then
      print("\nCognitive Evolution (over iterations):")
      local stats = results.cognitiveEvolution.stats
      print(string.format("  Confidence: mean=%.4f, std=%.4f", stats.confidence.mean, stats.confidence.std))
      print(string.format("  Cognitive Signal: mean=%.4f, std=%.4f", stats.cognitiveSignal.mean, stats.cognitiveSignal.std))
      print(string.format("  Stability: mean=%.4f, std=%.4f", stats.stability.mean, stats.stability.std))
      print(string.format("  Convergence: mean=%.4f, std=%.4f", stats.convergence.mean, stats.convergence.std))
   end

   if results.overhead then
      print("\nOverhead Analysis:")
      print(string.format("  Forward Overhead: %.2f%%", results.overhead.forwardOverheadPercent))
      print(string.format("  Backward Overhead: %.2f%%", results.overhead.backwardOverheadPercent))
      print(string.format("  Total Overhead: %.2f%%", results.overhead.totalOverheadPercent))
   end

   print(string.rep("=", 60) .. "\n")
end

function MetaCognitiveBenchmark:exportResults(results, format)
   -- Export results in specified format
   format = format or "table"

   if format == "table" then
      return results
   elseif format == "summary" then
      local summary = {
         networkType = results.networkType,
         forwardAvgMs = results.forward and results.forward.avgTime * 1000 or 0,
         backwardAvgMs = results.backward and results.backward.avgTime * 1000 or 0,
         memoryDeltaKB = results.memory and results.memory.memoryDelta or 0,
         throughput = results.forward and results.forward.throughput or 0
      }
      if results.cognitiveEvolution then
         summary.avgConfidence = results.cognitiveEvolution.stats.confidence.mean
         summary.avgStability = results.cognitiveEvolution.stats.stability.mean
      end
      return summary
   elseif format == "csv" then
      local header = "networkType,forwardAvgMs,backwardAvgMs,memoryDeltaKB,throughput"
      local values = string.format("%s,%.6f,%.6f,%.2f,%.2f",
         results.networkType or "unknown",
         results.forward and results.forward.avgTime * 1000 or 0,
         results.backward and results.backward.avgTime * 1000 or 0,
         results.memory and results.memory.memoryDelta or 0,
         results.forward and results.forward.throughput or 0
      )
      return header .. "\n" .. values
   end

   return results
end

function MetaCognitiveBenchmark:__tostring__()
   return string.format("%s{warmup=%d, iterations=%d}",
      torch.type(self),
      self.warmupIterations,
      self.benchmarkIterations
   )
end

return nn.MetaCognitiveBenchmark
