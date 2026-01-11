local TensorLogicReasoner, parent = torch.class('nn.TensorLogicReasoner', 'nn.Module')

--[[
   TensorLogicReasoner: Neural-symbolic reasoning module.

   Combines tensor logic with neural network processing to enable
   hybrid reasoning that leverages both:
   - Neural: Learning, pattern recognition, generalization
   - Symbolic: Logical inference, explainability, compositionality

   Integrates with the meta-cognitive system to provide:
   - Reasoning trace for introspection
   - Confidence-weighted inference
   - Adaptive reasoning depth

   Usage:
      local reasoner = nn.TensorLogicReasoner(inputDim, outputDim, {
         logicDepth = 3,
         useMetaCognition = true
      })

      -- Define knowledge base
      reasoner:addFact('parent', {'alice', 'bob'})
      reasoner:addFact('parent', {'bob', 'charlie'})
      reasoner:addRule('grandparent', {'parent', 'parent'})

      -- Reason with neural input
      local output, reasoning = reasoner:forward(input)

   Reference: https://tensor-logic.org/
]]

function TensorLogicReasoner:__init(inputDim, outputDim, options)
   parent.__init(self)

   options = options or {}
   self.inputDim = inputDim
   self.outputDim = outputDim
   self.logicDepth = options.logicDepth or 3
   self.useMetaCognition = options.useMetaCognition or false
   self.hiddenDim = options.hiddenDim or 64
   self.reasoningMode = options.reasoningMode or 'hybrid'  -- 'neural', 'symbolic', 'hybrid'

   -- Initialize tensor logic engine
   self.logic = nn.TensorLogic({
      embedding_dim = self.hiddenDim,
      use_soft_logic = (self.reasoningMode == 'hybrid'),
      trace = true
   })

   -- Neural encoder: maps input to symbolic space
   self.encoder = nn.Sequential()
   self.encoder:add(nn.Linear(inputDim, self.hiddenDim))
   self.encoder:add(nn.Tanh())
   self.encoder:add(nn.Linear(self.hiddenDim, self.hiddenDim))

   -- Symbolic reasoning layer (learned rule weights)
   self.ruleWeights = torch.randn(self.hiddenDim, self.hiddenDim)

   -- Neural decoder: maps symbolic results back to output space
   self.decoder = nn.Sequential()
   self.decoder:add(nn.Linear(self.hiddenDim, self.hiddenDim))
   self.decoder:add(nn.Tanh())
   self.decoder:add(nn.Linear(self.hiddenDim, outputDim))

   -- Confidence estimator
   self.confidenceNet = nn.Sequential()
   self.confidenceNet:add(nn.Linear(self.hiddenDim, 32))
   self.confidenceNet:add(nn.Tanh())
   self.confidenceNet:add(nn.Linear(32, 1))
   self.confidenceNet:add(nn.Sigmoid())

   -- Knowledge base
   self.facts = {}
   self.rules = {}

   -- Reasoning state
   self.reasoningState = {
      lastConfidence = 1.0,
      inferenceSteps = 0,
      factsUsed = {},
      rulesApplied = {}
   }

   -- Output
   self.output = torch.Tensor()
   self.gradInput = torch.Tensor()
end

function TensorLogicReasoner:addFact(relationName, tuple)
   -- Add a fact to the knowledge base
   if not self.facts[relationName] then
      self.facts[relationName] = {}
   end
   table.insert(self.facts[relationName], tuple)

   -- Update tensor logic
   self.logic:relation(self.facts[relationName], relationName)
end

function TensorLogicReasoner:addRule(headName, bodyNames)
   -- Add a rule: head :- body1, body2, ...
   table.insert(self.rules, {
      head = headName,
      body = bodyNames
   })
end

function TensorLogicReasoner:updateOutput(input)
   -- Forward pass: hybrid neural-symbolic reasoning
   local batchSize = input:size(1)

   -- Step 1: Neural encoding
   local encoded = self.encoder:forward(input)

   -- Step 2: Symbolic reasoning
   local symbolized = self:symbolicReason(encoded)

   -- Step 3: Apply learned rule transformations
   local reasoned = torch.mm(symbolized, self.ruleWeights)

   -- Step 4: Estimate confidence
   local confidence = self.confidenceNet:forward(reasoned:mean(1))
   self.reasoningState.lastConfidence = confidence[1][1]

   -- Step 5: Decode to output
   self.output = self.decoder:forward(reasoned)

   -- Track reasoning state
   self.reasoningState.inferenceSteps = self.reasoningState.inferenceSteps + 1

   return self.output
end

function TensorLogicReasoner:symbolicReason(encoded)
   -- Apply symbolic reasoning based on mode
   if self.reasoningMode == 'neural' then
      return encoded  -- Pass-through
   end

   local result = encoded:clone()

   -- Apply forward chaining on encoded representation
   for _, rule in ipairs(self.rules) do
      local bodyRel = self.logic:getRelation(rule.body[1])
      if bodyRel then
         -- Apply rule transformation
         for i = 2, #rule.body do
            local nextRel = self.logic:getRelation(rule.body[i])
            if nextRel then
               bodyRel = self.logic:tensorJoin(bodyRel, nextRel)
            end
         end

         -- Modulate encoded representation with logical result
         if bodyRel.tensor:nElement() <= result:size(2) then
            local logicMask = bodyRel.tensor:view(1, -1):expand(result:size(1), -1)
            local maskSize = math.min(logicMask:size(2), result:size(2))
            result:narrow(2, 1, maskSize):cmul(
               logicMask:narrow(2, 1, maskSize):expand(result:size(1), maskSize)
            )
         end

         table.insert(self.reasoningState.rulesApplied, rule.head)
      end
   end

   return result
end

function TensorLogicReasoner:updateGradInput(input, gradOutput)
   -- Backward pass
   local gradDecoded = self.decoder:backward(
      self.encoder.output,
      gradOutput
   )

   -- Gradient through rule weights
   -- (simplified: treat as fixed for now)

   self.gradInput = self.encoder:backward(input, gradDecoded)

   return self.gradInput
end

function TensorLogicReasoner:query(relationName, tuple)
   -- Query the knowledge base
   local rel = self.logic:getRelation(relationName)
   if rel then
      return self.logic:query(rel, tuple)
   end
   return false
end

function TensorLogicReasoner:infer(queryRelation, queryTuple)
   -- Perform inference to answer a query
   local rel = self.logic:getRelation(queryRelation)
   if not rel then
      return {proved = false, reason = "relation not found"}
   end

   return self.logic:backwardChain(rel, queryTuple, self.logicDepth)
end

function TensorLogicReasoner:explain()
   -- Generate explanation of reasoning
   local trace = self.logic:getInferenceTrace()
   local explanation = {
      confidence = self.reasoningState.lastConfidence,
      steps = self.reasoningState.inferenceSteps,
      rulesApplied = self.reasoningState.rulesApplied,
      inferenceTrace = trace
   }
   return explanation
end

function TensorLogicReasoner:getReasoningState()
   return self.reasoningState
end

function TensorLogicReasoner:getConfidence()
   return self.reasoningState.lastConfidence
end

function TensorLogicReasoner:setReasoningMode(mode)
   -- Set reasoning mode: 'neural', 'symbolic', 'hybrid'
   assert(mode == 'neural' or mode == 'symbolic' or mode == 'hybrid',
          "Invalid mode. Use 'neural', 'symbolic', or 'hybrid'")
   self.reasoningMode = mode
end

function TensorLogicReasoner:resetState()
   self.reasoningState = {
      lastConfidence = 1.0,
      inferenceSteps = 0,
      factsUsed = {},
      rulesApplied = {}
   }
   self.logic:clearTrace()
end

function TensorLogicReasoner:getKnowledgeBase()
   return {
      facts = self.facts,
      rules = self.rules,
      relations = self.logic:listRelations()
   }
end

function TensorLogicReasoner:introspect()
   -- Provide introspection for meta-cognitive integration
   return {
      reasoningMode = self.reasoningMode,
      logicDepth = self.logicDepth,
      confidence = self.reasoningState.lastConfidence,
      inferenceSteps = self.reasoningState.inferenceSteps,
      numFacts = self:countFacts(),
      numRules = #self.rules,
      lastRulesApplied = self.reasoningState.rulesApplied,
      logicStats = self.logic:stats()
   }
end

function TensorLogicReasoner:countFacts()
   local count = 0
   for _, facts in pairs(self.facts) do
      count = count + #facts
   end
   return count
end

function TensorLogicReasoner:__tostring__()
   local str = torch.type(self) .. ' {\n'
   str = str .. '  inputDim: ' .. self.inputDim .. '\n'
   str = str .. '  outputDim: ' .. self.outputDim .. '\n'
   str = str .. '  reasoningMode: ' .. self.reasoningMode .. '\n'
   str = str .. '  logicDepth: ' .. self.logicDepth .. '\n'
   str = str .. '  numFacts: ' .. self:countFacts() .. '\n'
   str = str .. '  numRules: ' .. #self.rules .. '\n'
   str = str .. '  confidence: ' .. string.format('%.4f', self.reasoningState.lastConfidence) .. '\n'
   str = str .. '}'
   return str
end

return nn.TensorLogicReasoner
