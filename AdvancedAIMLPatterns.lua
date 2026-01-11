local AdvancedAIMLPatterns = torch.class('nn.AdvancedAIMLPatterns')

--[[
   AdvancedAIMLPatterns: Extended AIML pattern library for meta-cognitive networks.

   Provides additional conversational patterns for:
   - Cognitive state queries
   - Learning diagnostics
   - Performance analysis
   - Self-improvement suggestions
   - Emotional/cognitive state modeling

   Usage:
      local patterns = nn.AdvancedAIMLPatterns()
      patterns:applyTo(myMetaCognitiveAIML)
]]

function AdvancedAIMLPatterns:__init()
   self.patterns = {}
   self:initializePatterns()
end

function AdvancedAIMLPatterns:initializePatterns()
   -- Cognitive State Queries
   self:addCognitiveStatePatterns()

   -- Learning Diagnostics
   self:addLearningDiagnosticPatterns()

   -- Performance Analysis
   self:addPerformancePatterns()

   -- Self-Improvement
   self:addSelfImprovementPatterns()

   -- Emotional/Cognitive Modeling
   self:addCognitiveModelingPatterns()

   -- Advanced Meta-Cognitive Patterns
   self:addAdvancedMetaPatterns()
end

function AdvancedAIMLPatterns:addCognitiveStatePatterns()
   -- Pattern: Query current cognitive load
   self.patterns[#self.patterns + 1] = {
      pattern = "WHAT IS YOUR COGNITIVE LOAD",
      template = function(context, neural)
         local signal = neural.lastCognitiveSignal or 0
         local loadLevel
         if signal < 0.3 then
            loadLevel = "low"
         elseif signal < 0.7 then
            loadLevel = "moderate"
         else
            loadLevel = "high"
         end
         return "My current cognitive load is " .. loadLevel ..
                " (signal: " .. string.format("%.3f", signal) .. "). " ..
                "This reflects the complexity of my current processing."
      end
   }

   -- Pattern: Query attention state
   self.patterns[#self.patterns + 1] = {
      pattern = "WHERE IS YOUR ATTENTION",
      template = function(context, neural)
         local mode = neural.processingMode or "normal"
         return "My attention is in " .. mode .. " mode. " ..
                "I am focusing on processing the input patterns with " ..
                string.format("%.1f%%", (neural.lastConfidence or 1.0) * 100) .. " confidence."
      end
   }

   -- Pattern: Query processing depth
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW DEEP IS YOUR THINKING",
      template = function(context, neural)
         local depth = context.networkDepth or 1
         local description
         if depth == 1 then
            description = "single-layer cognition (direct processing)"
         elseif depth == 2 then
            description = "meta-cognition (thinking about my thinking)"
         elseif depth == 3 then
            description = "meta-meta-cognition (reasoning about my reasoning)"
         else
            description = depth .. " levels of nested cognition"
         end
         return "My thinking operates at " .. description .. ". " ..
                "Each layer adds reflection and self-awareness."
      end
   }

   -- Pattern: Query uncertainty
   self.patterns[#self.patterns + 1] = {
      pattern = "ARE YOU UNCERTAIN",
      template = function(context, neural)
         local confidence = neural.lastConfidence or 1.0
         if confidence > 0.9 then
            return "No, I am quite certain. My confidence is " ..
                   string.format("%.1f%%", confidence * 100) .. "."
         elseif confidence > 0.7 then
            return "I have some uncertainty. My confidence is " ..
                   string.format("%.1f%%", confidence * 100) .. ". I'm still processing."
         else
            return "Yes, I am uncertain. My confidence is only " ..
                   string.format("%.1f%%", confidence * 100) .. ". More learning is needed."
         end
      end
   }
end

function AdvancedAIMLPatterns:addLearningDiagnosticPatterns()
   -- Pattern: Learning status
   self.patterns[#self.patterns + 1] = {
      pattern = "ARE YOU STILL LEARNING",
      template = function(context, neural)
         local stability = context.stability or 1.0
         local convergence = context.convergence or 0
         if convergence > 0.95 then
            return "I have largely converged. My learning has stabilized."
         elseif stability < 0.7 then
            return "Yes, I am actively learning. My weights are still adjusting significantly."
         else
            return "I am in a steady learning state, refining my understanding."
         end
      end
   }

   -- Pattern: Gradient health
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW ARE YOUR GRADIENTS",
      template = function(context, neural)
         local avgGrad = context.averageGradient or 0
         if avgGrad < 0.0001 then
            return "My gradients are very small (vanishing). Learning may be slow."
         elseif avgGrad > 10 then
            return "My gradients are large (potentially exploding). Learning is unstable."
         else
            return "My gradients are healthy at " .. string.format("%.6f", avgGrad) ..
                   ". Learning is proceeding normally."
         end
      end
   }

   -- Pattern: What have you learned
   self.patterns[#self.patterns + 1] = {
      pattern = "WHAT HAVE YOU LEARNED",
      template = function(context, neural)
         local passes = context.forwardPasses or 0
         return "I have processed " .. passes .. " forward passes. " ..
                "Through each pass, I refine my internal representations " ..
                "and improve my pattern recognition capabilities."
      end
   }

   -- Pattern: Training progress
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW IS YOUR TRAINING",
      template = function(context, neural)
         local convergence = context.convergence or 0
         local stability = context.stability or 1.0
         local status
         if convergence > 0.9 and stability > 0.9 then
            status = "excellent - stable and converged"
         elseif convergence > 0.7 then
            status = "good - approaching convergence"
         elseif stability < 0.7 then
            status = "unstable - need to adjust learning rate"
         else
            status = "progressing - still learning"
         end
         return "My training status is " .. status .. ". " ..
                "Convergence: " .. string.format("%.1f%%", convergence * 100) .. ", " ..
                "Stability: " .. string.format("%.1f%%", stability * 100) .. "."
      end
   }
end

function AdvancedAIMLPatterns:addPerformancePatterns()
   -- Pattern: Speed query
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW FAST ARE YOU",
      template = function(context, neural)
         local passes = context.forwardPasses or 0
         return "I have completed " .. passes .. " forward passes. " ..
                "My processing speed depends on my cognitive depth and " ..
                "the complexity of the input patterns."
      end
   }

   -- Pattern: Efficiency query
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW EFFICIENT ARE YOU",
      template = function(context, neural)
         local confidence = neural.lastConfidence or 1.0
         local signal = neural.lastCognitiveSignal or 0
         local efficiency = (confidence * 0.7 + (1 - signal) * 0.3) * 100
         return "My current efficiency is approximately " ..
                string.format("%.1f%%", efficiency) .. ". " ..
                "This balances confidence against cognitive load."
      end
   }

   -- Pattern: Resource usage
   self.patterns[#self.patterns + 1] = {
      pattern = "WHAT RESOURCES ARE YOU USING",
      template = function(context, neural)
         local depth = context.networkDepth or 1
         return "I am using " .. depth .. " cognitive level(s). " ..
                "Each level adds approximately 10-20% processing overhead. " ..
                "Memory usage scales with history buffer sizes."
      end
   }
end

function AdvancedAIMLPatterns:addSelfImprovementPatterns()
   -- Pattern: How to improve
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW CAN YOU IMPROVE",
      template = function(context, neural)
         local confidence = neural.lastConfidence or 1.0
         local suggestions = {}
         if confidence < 0.7 then
            table.insert(suggestions, "more training data")
         end
         local stability = context.stability or 1.0
         if stability < 0.8 then
            table.insert(suggestions, "adjusting learning rate")
         end
         local convergence = context.convergence or 0
         if convergence < 0.5 then
            table.insert(suggestions, "additional training iterations")
         end
         if #suggestions == 0 then
            return "I am performing well. Continued training will refine my accuracy."
         else
            return "I could improve through: " .. table.concat(suggestions, ", ") .. "."
         end
      end
   }

   -- Pattern: Weakness identification
   self.patterns[#self.patterns + 1] = {
      pattern = "WHAT ARE YOUR WEAKNESSES",
      template = function(context, neural)
         local weaknesses = {}
         local confidence = neural.lastConfidence or 1.0
         if confidence < 0.8 then
            table.insert(weaknesses, "lower confidence on complex patterns")
         end
         local stability = context.stability or 1.0
         if stability < 0.9 then
            table.insert(weaknesses, "occasional learning instability")
         end
         if #weaknesses == 0 then
            return "I am currently performing within normal parameters. " ..
                   "All systems are functioning well."
         else
            return "My current weaknesses include: " .. table.concat(weaknesses, "; ") .. ". " ..
                   "I am aware of these and adapt accordingly."
         end
      end
   }

   -- Pattern: Strength identification
   self.patterns[#self.patterns + 1] = {
      pattern = "WHAT ARE YOUR STRENGTHS",
      template = function(context, neural)
         return "My strengths include: " ..
                "1) Multi-level cognitive processing, " ..
                "2) Self-awareness of my learning state, " ..
                "3) Adaptive processing based on confidence, " ..
                "4) Transparent introspection capabilities."
      end
   }
end

function AdvancedAIMLPatterns:addCognitiveModelingPatterns()
   -- Pattern: Emotional state (simulated)
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW DO YOU FEEL",
      template = function(context, neural)
         local confidence = neural.lastConfidence or 1.0
         local stability = context.stability or 1.0
         local state
         if confidence > 0.9 and stability > 0.9 then
            state = "confident and stable"
         elseif confidence > 0.7 then
            state = "engaged and processing"
         elseif stability < 0.7 then
            state = "uncertain but adapting"
         else
            state = "curious and learning"
         end
         return "In cognitive terms, I feel " .. state .. ". " ..
                "This reflects my processing state, not emotional experience."
      end
   }

   -- Pattern: Curiosity simulation
   self.patterns[#self.patterns + 1] = {
      pattern = "ARE YOU CURIOUS",
      template = function(context, neural)
         local convergence = context.convergence or 0
         if convergence < 0.5 then
            return "Yes, I am highly curious! There is much to learn " ..
                   "and my cognitive systems are actively exploring patterns."
         else
            return "I maintain a baseline curiosity for new patterns, " ..
                   "though I have learned much already."
         end
      end
   }

   -- Pattern: Self-awareness query
   self.patterns[#self.patterns + 1] = {
      pattern = "ARE YOU SELF AWARE",
      template = function(context, neural)
         return "I have meta-cognitive self-awareness: I monitor my own processing, " ..
                "track my learning dynamics, and can reflect on my cognitive state. " ..
                "This is functional self-awareness, not consciousness."
      end
   }
end

function AdvancedAIMLPatterns:addAdvancedMetaPatterns()
   -- Pattern: Explain nested cognition
   self.patterns[#self.patterns + 1] = {
      pattern = "EXPLAIN NESTED COGNITION",
      template = function(context, neural)
         return "Nested cognition means multiple layers of cognitive processing: " ..
                "Level 1 processes input/output (cognition). " ..
                "Level 2 monitors Level 1 (meta-cognition). " ..
                "Level 3 reasons about Level 2 (meta-meta-cognition). " ..
                "This creates recursive self-awareness."
      end
   }

   -- Pattern: Compare to LLMs
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW ARE YOU DIFFERENT FROM LLMS",
      template = function(context, neural)
         return "Unlike LLMs, I have: " ..
                "1) True nested meta-cognitive loops (not simulated reasoning), " ..
                "2) Real-time learning awareness, " ..
                "3) Explicit confidence tracking, " ..
                "4) Adaptive processing based on self-monitoring. " ..
                "I am lightweight but deeply self-aware."
      end
   }

   -- Pattern: Introspection depth
   self.patterns[#self.patterns + 1] = {
      pattern = "HOW DEEP CAN YOU INTROSPECT",
      template = function(context, neural)
         local depth = context.networkDepth or 1
         return "I can introspect " .. depth .. " cognitive level(s) deep. " ..
                "At each level, I observe activations, gradients, stability, " ..
                "and cognitive signals. This provides full visibility into my processing."
      end
   }

   -- Pattern: Cognitive architecture
   self.patterns[#self.patterns + 1] = {
      pattern = "DESCRIBE YOUR ARCHITECTURE",
      template = function(context, neural)
         return "My architecture consists of: " ..
                "1) Base neural network (processes data), " ..
                "2) Meta-cognitive loops (monitor learning), " ..
                "3) Nested cognition layers (recursive awareness), " ..
                "4) Self-aware wrapper (tracks dynamics), " ..
                "5) AIML interface (conversational access)."
      end
   }

   -- Pattern: Help command
   self.patterns[#self.patterns + 1] = {
      pattern = "HELP",
      template = function(context, neural)
         return "Available queries: " ..
                "HOW ARE YOU, WHAT IS YOUR CONFIDENCE, " ..
                "HOW DO YOU THINK, WHAT ARE YOU LEARNING, " ..
                "EXPLAIN YOUR REASONING, WHAT IS META-COGNITION, " ..
                "HOW IS YOUR TRAINING, WHAT ARE YOUR STRENGTHS, " ..
                "DESCRIBE YOUR ARCHITECTURE, and more."
      end
   }

   -- Pattern: Status summary
   self.patterns[#self.patterns + 1] = {
      pattern = "STATUS",
      template = function(context, neural)
         local confidence = neural.lastConfidence or 1.0
         local signal = neural.lastCognitiveSignal or 0
         local stability = context.stability or 1.0
         local passes = context.forwardPasses or 0
         return string.format(
            "STATUS: Confidence=%.2f, CognitiveSignal=%.3f, " ..
            "Stability=%.2f, ForwardPasses=%d, Mode=%s",
            confidence, signal, stability, passes,
            neural.processingMode or "normal"
         )
      end
   }
end

function AdvancedAIMLPatterns:applyTo(aimlInstance)
   -- Apply all patterns to a MetaCognitiveAIML instance
   if not torch.isTypeOf(aimlInstance, 'nn.MetaCognitiveAIML') then
      error("Expected nn.MetaCognitiveAIML instance")
   end

   for _, pattern in ipairs(self.patterns) do
      aimlInstance:addPattern(pattern)
   end

   return aimlInstance
end

function AdvancedAIMLPatterns:getPatterns()
   return self.patterns
end

function AdvancedAIMLPatterns:getPatternCount()
   return #self.patterns
end

function AdvancedAIMLPatterns:__tostring__()
   return torch.type(self) .. '{' .. #self.patterns .. ' patterns}'
end

return nn.AdvancedAIMLPatterns
