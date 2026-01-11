local CognitiveVisualizer = torch.class('nn.CognitiveVisualizer')

--[[
   CognitiveVisualizer: Visualization utilities for meta-cognitive neural networks.

   Provides tools to visualize:
   - Cognitive state evolution over time
   - Confidence level trends
   - Learning dynamics (stability, convergence)
   - Cognitive hierarchy heatmaps
   - ASCII-based terminal visualizations
   - Data export for external visualization tools

   Usage:
      local visualizer = nn.CognitiveVisualizer()
      visualizer:trackNetwork(selfAwareNetwork)
      -- run training iterations...
      visualizer:plotConfidence()
      visualizer:exportTimeline('cognitive_data.json')
]]

function CognitiveVisualizer:__init(options)
   options = options or {}
   self.width = options.width or 60
   self.height = options.height or 15
   self.maxHistory = options.maxHistory or 200

   -- Timeline data storage
   self.timeline = {
      confidence = {},
      cognitiveSignal = {},
      stability = {},
      convergence = {},
      activationMagnitude = {},
      gradientMagnitude = {},
      timestamps = {}
   }

   self.trackedNetwork = nil
   self.iteration = 0
end

function CognitiveVisualizer:trackNetwork(network)
   -- Set the network to track
   self.trackedNetwork = network
   self:resetTimeline()
   return self
end

function CognitiveVisualizer:resetTimeline()
   -- Reset all timeline data
   for key, _ in pairs(self.timeline) do
      self.timeline[key] = {}
   end
   self.iteration = 0
end

function CognitiveVisualizer:captureState()
   -- Capture current cognitive state from tracked network
   if not self.trackedNetwork then
      error("No network tracked. Call trackNetwork() first.")
   end

   self.iteration = self.iteration + 1
   local state = self:extractState(self.trackedNetwork)

   -- Store in timeline
   table.insert(self.timeline.confidence, state.confidence)
   table.insert(self.timeline.cognitiveSignal, state.cognitiveSignal)
   table.insert(self.timeline.stability, state.stability)
   table.insert(self.timeline.convergence, state.convergence)
   table.insert(self.timeline.activationMagnitude, state.activationMagnitude)
   table.insert(self.timeline.gradientMagnitude, state.gradientMagnitude)
   table.insert(self.timeline.timestamps, self.iteration)

   -- Trim history if needed
   self:trimHistory()

   return state
end

function CognitiveVisualizer:extractState(network)
   -- Extract cognitive state from various network types
   local state = {
      confidence = 1.0,
      cognitiveSignal = 0,
      stability = 1.0,
      convergence = 0,
      activationMagnitude = 0,
      gradientMagnitude = 0
   }

   if torch.isTypeOf(network, 'nn.SelfAwareNetwork') then
      local awareness = network:getSelfAwareness()
      state.stability = awareness.learningDynamics.stability
      state.convergence = awareness.learningDynamics.convergence
      state.activationMagnitude = awareness.performanceMetrics.averageActivation
      state.gradientMagnitude = awareness.performanceMetrics.averageGradient
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

function CognitiveVisualizer:trimHistory()
   -- Trim timeline to maxHistory entries
   for key, data in pairs(self.timeline) do
      while #data > self.maxHistory do
         table.remove(data, 1)
      end
   end
end

function CognitiveVisualizer:plotASCII(data, title, minVal, maxVal)
   -- Create ASCII line plot
   title = title or "Plot"
   minVal = minVal or 0
   maxVal = maxVal or 1

   if #data == 0 then
      return title .. ": No data"
   end

   local lines = {}
   table.insert(lines, string.rep("-", self.width + 10))
   table.insert(lines, " " .. title)
   table.insert(lines, string.rep("-", self.width + 10))

   -- Create the plot grid
   local grid = {}
   for y = 1, self.height do
      grid[y] = {}
      for x = 1, self.width do
         grid[y][x] = " "
      end
   end

   -- Sample data to fit width
   local step = math.max(1, math.floor(#data / self.width))
   local sampledData = {}
   for i = 1, #data, step do
      table.insert(sampledData, data[i])
      if #sampledData >= self.width then break end
   end

   -- Plot points
   for x, value in ipairs(sampledData) do
      local normalized = (value - minVal) / (maxVal - minVal + 1e-8)
      normalized = math.max(0, math.min(1, normalized))
      local y = self.height - math.floor(normalized * (self.height - 1))
      y = math.max(1, math.min(self.height, y))
      if x <= self.width then
         grid[y][x] = "*"
      end
   end

   -- Add y-axis labels and grid
   for y = 1, self.height do
      local label
      if y == 1 then
         label = string.format("%5.2f|", maxVal)
      elseif y == self.height then
         label = string.format("%5.2f|", minVal)
      elseif y == math.floor(self.height / 2) then
         label = string.format("%5.2f|", (maxVal + minVal) / 2)
      else
         label = "     |"
      end
      table.insert(lines, label .. table.concat(grid[y]))
   end

   -- X-axis
   table.insert(lines, "     +" .. string.rep("-", self.width))
   local xLabel = string.format("      0%s%d", string.rep(" ", self.width - 6), #data)
   table.insert(lines, xLabel)

   return table.concat(lines, "\n")
end

function CognitiveVisualizer:plotConfidence()
   -- Plot confidence timeline
   return self:plotASCII(self.timeline.confidence, "Confidence Level", 0, 1)
end

function CognitiveVisualizer:plotCognitiveSignal()
   -- Plot cognitive signal timeline
   local maxSig = 1
   for _, v in ipairs(self.timeline.cognitiveSignal) do
      if v > maxSig then maxSig = v end
   end
   return self:plotASCII(self.timeline.cognitiveSignal, "Cognitive Signal", 0, maxSig)
end

function CognitiveVisualizer:plotStability()
   -- Plot stability timeline
   return self:plotASCII(self.timeline.stability, "Learning Stability", 0, 2)
end

function CognitiveVisualizer:plotConvergence()
   -- Plot convergence timeline
   return self:plotASCII(self.timeline.convergence, "Convergence", 0, 1)
end

function CognitiveVisualizer:plotAll()
   -- Plot all metrics
   local plots = {}
   table.insert(plots, self:plotConfidence())
   table.insert(plots, "")
   table.insert(plots, self:plotCognitiveSignal())
   table.insert(plots, "")
   table.insert(plots, self:plotStability())
   table.insert(plots, "")
   table.insert(plots, self:plotConvergence())
   return table.concat(plots, "\n")
end

function CognitiveVisualizer:createHeatmap(data, rows, cols, title)
   -- Create ASCII heatmap visualization
   title = title or "Heatmap"
   rows = rows or 5
   cols = cols or 20

   if #data == 0 then
      return title .. ": No data"
   end

   local lines = {}
   table.insert(lines, title)
   table.insert(lines, string.rep("-", cols + 2))

   -- Intensity characters (low to high)
   local intensity = {" ", ".", ":", "+", "*", "#", "@"}

   -- Reshape data into rows x cols
   local step = math.max(1, math.floor(#data / (rows * cols)))
   local idx = 1

   for r = 1, rows do
      local row = "|"
      for c = 1, cols do
         local value = data[idx] or 0
         local normalized = math.max(0, math.min(1, value))
         local level = math.floor(normalized * (#intensity - 1)) + 1
         row = row .. intensity[level]
         idx = idx + step
      end
      row = row .. "|"
      table.insert(lines, row)
   end

   table.insert(lines, string.rep("-", cols + 2))
   table.insert(lines, "Legend: ' '=0.0  '.'=0.2  ':'=0.4  '+'=0.6  '*'=0.8  '#'=0.9  '@'=1.0")

   return table.concat(lines, "\n")
end

function CognitiveVisualizer:showCognitiveHeatmap()
   -- Create heatmap showing cognitive state over time
   return self:createHeatmap(self.timeline.confidence, 5, 40, "Confidence Heatmap (time ->)")
end

function CognitiveVisualizer:createSparkline(data, width)
   -- Create a compact sparkline visualization
   width = width or 40

   if #data == 0 then
      return "[no data]"
   end

   local sparks = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"}
   local step = math.max(1, math.floor(#data / width))
   local result = {}

   -- Find min/max for normalization
   local minVal, maxVal = data[1], data[1]
   for _, v in ipairs(data) do
      if v < minVal then minVal = v end
      if v > maxVal then maxVal = v end
   end

   for i = 1, #data, step do
      local normalized = (data[i] - minVal) / (maxVal - minVal + 1e-8)
      normalized = math.max(0, math.min(1, normalized))
      local level = math.floor(normalized * (#sparks - 1)) + 1
      table.insert(result, sparks[level])
      if #result >= width then break end
   end

   return table.concat(result)
end

function CognitiveVisualizer:showDashboard()
   -- Create a compact dashboard view
   local lines = {}

   table.insert(lines, "╔══════════════════════════════════════════════════════════════╗")
   table.insert(lines, "║           COGNITIVE STATE DASHBOARD                          ║")
   table.insert(lines, "╠══════════════════════════════════════════════════════════════╣")

   -- Current values
   local n = #self.timeline.confidence
   if n > 0 then
      local current = {
         confidence = self.timeline.confidence[n],
         signal = self.timeline.cognitiveSignal[n],
         stability = self.timeline.stability[n],
         convergence = self.timeline.convergence[n]
      }

      table.insert(lines, string.format("║ Iteration: %-6d                                            ║", self.iteration))
      table.insert(lines, string.format("║ Confidence:  %5.3f  %s ║", current.confidence, self:createSparkline(self.timeline.confidence, 40)))
      table.insert(lines, string.format("║ Cog.Signal:  %5.3f  %s ║", current.signal, self:createSparkline(self.timeline.cognitiveSignal, 40)))
      table.insert(lines, string.format("║ Stability:   %5.3f  %s ║", current.stability, self:createSparkline(self.timeline.stability, 40)))
      table.insert(lines, string.format("║ Convergence: %5.3f  %s ║", current.convergence, self:createSparkline(self.timeline.convergence, 40)))
   else
      table.insert(lines, "║ No data captured yet. Call captureState() during training.  ║")
   end

   table.insert(lines, "╚══════════════════════════════════════════════════════════════╝")

   return table.concat(lines, "\n")
end

function CognitiveVisualizer:getStatistics()
   -- Calculate statistics for all metrics
   local stats = {}

   for metric, data in pairs(self.timeline) do
      if metric ~= "timestamps" and #data > 0 then
         local sum, min, max = 0, data[1], data[1]
         for _, v in ipairs(data) do
            sum = sum + v
            if v < min then min = v end
            if v > max then max = v end
         end
         local mean = sum / #data

         local variance = 0
         for _, v in ipairs(data) do
            variance = variance + (v - mean)^2
         end
         variance = variance / #data

         stats[metric] = {
            count = #data,
            mean = mean,
            std = math.sqrt(variance),
            min = min,
            max = max,
            latest = data[#data],
            trend = #data > 1 and (data[#data] - data[1]) or 0
         }
      end
   end

   return stats
end

function CognitiveVisualizer:printStatistics()
   -- Print formatted statistics
   local stats = self:getStatistics()
   local lines = {}

   table.insert(lines, "\nCognitive State Statistics")
   table.insert(lines, string.rep("=", 70))
   table.insert(lines, string.format("%-20s %8s %8s %8s %8s %8s",
      "Metric", "Mean", "Std", "Min", "Max", "Trend"))
   table.insert(lines, string.rep("-", 70))

   local metrics = {"confidence", "cognitiveSignal", "stability", "convergence"}
   for _, metric in ipairs(metrics) do
      if stats[metric] then
         local s = stats[metric]
         local trendSymbol = s.trend > 0.01 and "↑" or (s.trend < -0.01 and "↓" or "→")
         table.insert(lines, string.format("%-20s %8.4f %8.4f %8.4f %8.4f %7.3f%s",
            metric, s.mean, s.std, s.min, s.max, s.trend, trendSymbol))
      end
   end

   table.insert(lines, string.rep("=", 70))

   return table.concat(lines, "\n")
end

function CognitiveVisualizer:exportJSON(filepath)
   -- Export timeline data as JSON
   filepath = filepath or "cognitive_timeline.json"

   local json = "{\n"
   json = json .. '  "iterations": ' .. self.iteration .. ',\n'
   json = json .. '  "dataPoints": ' .. #self.timeline.confidence .. ',\n'
   json = json .. '  "timeline": {\n'

   local first = true
   for metric, data in pairs(self.timeline) do
      if not first then json = json .. ",\n" end
      first = false
      json = json .. '    "' .. metric .. '": ['
      for i, v in ipairs(data) do
         if i > 1 then json = json .. ", " end
         json = json .. string.format("%.6f", v)
      end
      json = json .. ']'
   end

   json = json .. '\n  },\n'

   -- Add statistics
   local stats = self:getStatistics()
   json = json .. '  "statistics": {\n'
   first = true
   for metric, s in pairs(stats) do
      if not first then json = json .. ",\n" end
      first = false
      json = json .. '    "' .. metric .. '": {'
      json = json .. '"mean": ' .. string.format("%.6f", s.mean) .. ', '
      json = json .. '"std": ' .. string.format("%.6f", s.std) .. ', '
      json = json .. '"min": ' .. string.format("%.6f", s.min) .. ', '
      json = json .. '"max": ' .. string.format("%.6f", s.max) .. ', '
      json = json .. '"trend": ' .. string.format("%.6f", s.trend) .. '}'
   end
   json = json .. '\n  }\n'
   json = json .. '}\n'

   local file = io.open(filepath, "w")
   if file then
      file:write(json)
      file:close()
      return true, filepath
   else
      return false, "Could not open file for writing"
   end
end

function CognitiveVisualizer:exportCSV(filepath)
   -- Export timeline data as CSV
   filepath = filepath or "cognitive_timeline.csv"

   local csv = "iteration,confidence,cognitiveSignal,stability,convergence,activationMagnitude,gradientMagnitude\n"

   local n = #self.timeline.confidence
   for i = 1, n do
      csv = csv .. string.format("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f\n",
         self.timeline.timestamps[i] or i,
         self.timeline.confidence[i] or 0,
         self.timeline.cognitiveSignal[i] or 0,
         self.timeline.stability[i] or 0,
         self.timeline.convergence[i] or 0,
         self.timeline.activationMagnitude[i] or 0,
         self.timeline.gradientMagnitude[i] or 0
      )
   end

   local file = io.open(filepath, "w")
   if file then
      file:write(csv)
      file:close()
      return true, filepath
   else
      return false, "Could not open file for writing"
   end
end

function CognitiveVisualizer:getTimeline()
   return self.timeline
end

function CognitiveVisualizer:getIteration()
   return self.iteration
end

function CognitiveVisualizer:__tostring__()
   return string.format("%s{iterations=%d, dataPoints=%d, width=%d, height=%d}",
      torch.type(self),
      self.iteration,
      #self.timeline.confidence,
      self.width,
      self.height
   )
end

return nn.CognitiveVisualizer
