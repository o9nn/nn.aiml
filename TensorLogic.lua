local TensorLogic = torch.class('nn.TensorLogic')

--[[
   TensorLogic: Tensor-based logical reasoning module.

   Implements concepts from Tensor Logic (Domingos, 2025) which unifies
   neural and symbolic AI through the equivalence of logical rules and
   Einstein summation.

   Core Operations:
   - Tensor Join: Generalizes database joins via tensor product
   - Tensor Projection: Marginalization through summation
   - Elementwise Nonlinearity: Step functions for discrete logic

   Logical Operations:
   - Conjunction (AND): Tensor product
   - Disjunction (OR): Element-wise maximum or addition + step
   - Negation (NOT): 1 - tensor
   - Implication: Derived from other operations

   Usage:
      local logic = nn.TensorLogic()

      -- Define relations as tensors
      local parent = logic:relation({{'alice', 'bob'}, {'bob', 'charlie'}})
      local grandparent = logic:forwardChain(parent, parent) -- transitive

      -- Query
      local result = logic:query(grandparent, {'alice', 'charlie'})

   Reference: https://tensor-logic.org/
]]

function TensorLogic:__init(options)
   options = options or {}
   self.embedding_dim = options.embedding_dim or 64
   self.use_soft_logic = options.use_soft_logic or false
   self.threshold = options.threshold or 0.5
   self.device = options.device or 'cpu'

   -- Symbol table for entity encoding
   self.symbolTable = {}
   self.reverseSymbolTable = {}
   self.nextSymbolId = 1

   -- Relation storage
   self.relations = {}

   -- Inference trace for explanation
   self.inferenceTrace = {}
   self.traceEnabled = options.trace or false
end

-- Symbol Management

function TensorLogic:encodeSymbol(symbol)
   -- Encode a symbol to an integer ID
   if type(symbol) == 'number' then
      return symbol
   end

   local key = tostring(symbol)
   if not self.symbolTable[key] then
      self.symbolTable[key] = self.nextSymbolId
      self.reverseSymbolTable[self.nextSymbolId] = key
      self.nextSymbolId = self.nextSymbolId + 1
   end
   return self.symbolTable[key]
end

function TensorLogic:decodeSymbol(id)
   -- Decode an integer ID back to symbol
   return self.reverseSymbolTable[id] or id
end

function TensorLogic:encodeSymbols(symbols)
   -- Encode multiple symbols
   local encoded = {}
   for i, s in ipairs(symbols) do
      encoded[i] = self:encodeSymbol(s)
   end
   return encoded
end

-- Relation Creation

function TensorLogic:relation(tuples, name)
   --[[
      Create a relation (sparse boolean tensor) from tuples.

      Args:
         tuples: List of tuples, e.g., {{'a', 'b'}, {'b', 'c'}}
         name: Optional name for the relation

      Returns:
         Sparse tensor representing the relation
   ]]
   name = name or ('relation_' .. #self.relations + 1)

   if #tuples == 0 then
      local rel = {
         name = name,
         arity = 0,
         tensor = torch.zeros(1),
         tuples = {},
         size = {}
      }
      self.relations[name] = rel
      return rel
   end

   -- Determine arity from first tuple
   local arity = #tuples[1]

   -- Encode all symbols and find dimensions
   local maxIndices = {}
   for i = 1, arity do
      maxIndices[i] = 0
   end

   local encodedTuples = {}
   for _, tuple in ipairs(tuples) do
      local encoded = self:encodeSymbols(tuple)
      table.insert(encodedTuples, encoded)
      for i, idx in ipairs(encoded) do
         if idx > maxIndices[i] then
            maxIndices[i] = idx
         end
      end
   end

   -- Create tensor (dense for simplicity, sparse in production)
   local tensorSize = {}
   for i = 1, arity do
      tensorSize[i] = maxIndices[i]
   end

   local tensor
   if arity == 1 then
      tensor = torch.zeros(tensorSize[1])
   elseif arity == 2 then
      tensor = torch.zeros(tensorSize[1], tensorSize[2])
   elseif arity == 3 then
      tensor = torch.zeros(tensorSize[1], tensorSize[2], tensorSize[3])
   else
      -- For higher arity, use table-based storage
      tensor = torch.zeros(tensorSize[1], tensorSize[2] or 1)
   end

   -- Fill tensor with 1s for true tuples
   for _, encoded in ipairs(encodedTuples) do
      if arity == 1 then
         tensor[encoded[1]] = 1
      elseif arity == 2 then
         tensor[encoded[1]][encoded[2]] = 1
      elseif arity == 3 then
         tensor[encoded[1]][encoded[2]][encoded[3]] = 1
      end
   end

   local rel = {
      name = name,
      arity = arity,
      tensor = tensor,
      tuples = encodedTuples,
      size = tensorSize
   }

   self.relations[name] = rel
   return rel
end

function TensorLogic:fromTensor(tensor, name)
   -- Create relation directly from tensor
   name = name or ('tensor_rel_' .. #self.relations + 1)
   local arity = tensor:dim()
   local size = {}
   for i = 1, arity do
      size[i] = tensor:size(i)
   end

   local rel = {
      name = name,
      arity = arity,
      tensor = tensor:clone(),
      tuples = {},  -- Not tracked for direct tensor input
      size = size
   }

   self.relations[name] = rel
   return rel
end

-- Core Tensor Operations (Einstein Summation Equivalents)

function TensorLogic:tensorJoin(rel1, rel2, joinIndices)
   --[[
      Tensor Join: Generalized database join via tensor product.

      Equivalent to Einstein summation where shared indices are contracted.

      Args:
         rel1, rel2: Relations to join
         joinIndices: Pair of indices to join on, e.g., {2, 1} means
                      rel1's 2nd index joins with rel2's 1st index

      Returns:
         New relation representing the join
   ]]
   joinIndices = joinIndices or {2, 1}

   local t1 = rel1.tensor
   local t2 = rel2.tensor

   -- For binary relations: R(x,y) join S(y,z) = Result(x,z)
   -- This is matrix multiplication for 2D tensors
   if rel1.arity == 2 and rel2.arity == 2 then
      if joinIndices[1] == 2 and joinIndices[2] == 1 then
         -- Standard: R(x,y) * S(y,z) -> Result(x,z)
         local result = torch.mm(t1, t2)
         return self:fromTensor(result, rel1.name .. '_join_' .. rel2.name)
      elseif joinIndices[1] == 1 and joinIndices[2] == 1 then
         -- R(y,x) * S(y,z) -> need transpose
         local result = torch.mm(t1:t(), t2)
         return self:fromTensor(result, rel1.name .. '_join_' .. rel2.name)
      elseif joinIndices[1] == 2 and joinIndices[2] == 2 then
         -- R(x,y) * S(z,y) -> need transpose of t2
         local result = torch.mm(t1, t2:t())
         return self:fromTensor(result, rel1.name .. '_join_' .. rel2.name)
      end
   end

   -- For 1D: element-wise product
   if rel1.arity == 1 and rel2.arity == 1 then
      local minSize = math.min(t1:size(1), t2:size(1))
      local result = torch.cmul(t1:narrow(1, 1, minSize), t2:narrow(1, 1, minSize))
      return self:fromTensor(result, rel1.name .. '_join_' .. rel2.name)
   end

   -- General case: outer product then contract
   error("Join indices " .. tostring(joinIndices) .. " not yet implemented for arity " ..
         rel1.arity .. " x " .. rel2.arity)
end

function TensorLogic:tensorProject(rel, projectIndices)
   --[[
      Tensor Projection: Marginalization through summation.

      Sums over specified indices, reducing dimensionality.

      Args:
         rel: Relation to project
         projectIndices: List of indices to sum over

      Returns:
         Projected relation
   ]]
   local tensor = rel.tensor:clone()

   -- Sort indices in descending order to avoid dimension shift issues
   local sorted = {}
   for _, idx in ipairs(projectIndices) do
      table.insert(sorted, idx)
   end
   table.sort(sorted, function(a, b) return a > b end)

   for _, idx in ipairs(sorted) do
      tensor = tensor:sum(idx)
      -- Remove singleton dimension
      if tensor:dim() > 1 then
         tensor = tensor:squeeze(idx)
      end
   end

   return self:fromTensor(tensor, rel.name .. '_proj')
end

function TensorLogic:step(tensor, threshold)
   --[[
      Step function: Converts continuous values to discrete logic.

      Returns 1 where tensor >= threshold, 0 otherwise.
   ]]
   threshold = threshold or self.threshold
   local result = tensor:ge(threshold):double()
   return result
end

function TensorLogic:sigmoid(tensor)
   -- Soft logic version using sigmoid
   return torch.sigmoid(tensor)
end

function TensorLogic:softmax(tensor, dim)
   -- Softmax for probabilistic interpretation
   dim = dim or tensor:dim()
   local exp_tensor = torch.exp(tensor - tensor:max())
   return exp_tensor / exp_tensor:sum(dim)
end

-- Logical Operations

function TensorLogic:conjunction(rel1, rel2)
   --[[
      Logical AND: Element-wise minimum or product.

      R AND S holds where both R and S hold.
   ]]
   local t1 = rel1.tensor
   local t2 = rel2.tensor

   -- Ensure same size
   if not t1:isSameSizeAs(t2) then
      error("Conjunction requires tensors of same size")
   end

   local result = torch.cmin(t1, t2)  -- Element-wise minimum
   return self:fromTensor(result, '(' .. rel1.name .. ' AND ' .. rel2.name .. ')')
end

function TensorLogic:disjunction(rel1, rel2)
   --[[
      Logical OR: Element-wise maximum.

      R OR S holds where either R or S holds.
   ]]
   local t1 = rel1.tensor
   local t2 = rel2.tensor

   if not t1:isSameSizeAs(t2) then
      error("Disjunction requires tensors of same size")
   end

   local result = torch.cmax(t1, t2)  -- Element-wise maximum
   return self:fromTensor(result, '(' .. rel1.name .. ' OR ' .. rel2.name .. ')')
end

function TensorLogic:negation(rel)
   --[[
      Logical NOT: 1 - tensor.

      NOT R holds where R does not hold.
   ]]
   local result = (-rel.tensor + 1):clamp(0, 1)
   return self:fromTensor(result, 'NOT(' .. rel.name .. ')')
end

function TensorLogic:implication(rel1, rel2)
   --[[
      Logical Implication: NOT(R) OR S

      R -> S is equivalent to (NOT R) OR S
   ]]
   local notRel1 = self:negation(rel1)
   return self:disjunction(notRel1, rel2)
end

-- Inference Operations

function TensorLogic:forwardChain(rel, maxIterations)
   --[[
      Forward Chaining: Compute transitive closure.

      Repeatedly applies relation to derive new facts until fixpoint.

      For relation R(x,y), computes R+(x,y) = R ∪ R∘R ∪ R∘R∘R ∪ ...
   ]]
   maxIterations = maxIterations or 10

   if rel.arity ~= 2 then
      error("Forward chaining requires binary relation")
   end

   local current = rel.tensor:clone()
   local accumulated = current:clone()

   if self.traceEnabled then
      self.inferenceTrace = {{step = 0, tuples = self:extractTuples(rel)}}
   end

   for i = 1, maxIterations do
      -- Compute R ∘ current (composition)
      local next = torch.mm(rel.tensor, current)

      -- Apply step function to get boolean
      next = self:step(next)

      -- Union with accumulated
      local newAccumulated = torch.cmax(accumulated, next)

      -- Check for fixpoint
      if newAccumulated:equal(accumulated) then
         if self.traceEnabled then
            table.insert(self.inferenceTrace, {
               step = i,
               status = 'fixpoint',
               newFacts = 0
            })
         end
         break
      end

      if self.traceEnabled then
         local newFacts = (newAccumulated - accumulated):sum()
         table.insert(self.inferenceTrace, {
            step = i,
            newFacts = newFacts
         })
      end

      accumulated = newAccumulated
      current = next
   end

   return self:fromTensor(accumulated, rel.name .. '_closure')
end

function TensorLogic:backwardChain(rel, queryTuple, maxDepth)
   --[[
      Backward Chaining: Query-driven inference.

      Given a query, works backward to find supporting facts.

      Args:
         rel: Base relation
         queryTuple: Query tuple to prove, e.g., {'alice', 'charlie'}
         maxDepth: Maximum inference depth

      Returns:
         {proved: boolean, trace: table}
   ]]
   maxDepth = maxDepth or 5

   local encoded = self:encodeSymbols(queryTuple)
   local trace = {}

   local function prove(target, depth)
      if depth > maxDepth then
         return false, "max depth exceeded"
      end

      -- Direct lookup
      local directResult = self:lookup(rel, target)
      if directResult then
         table.insert(trace, {
            depth = depth,
            target = target,
            found = 'direct'
         })
         return true, 'direct'
      end

      -- Try to find intermediate
      if rel.arity == 2 then
         local t = rel.tensor
         local targetX, targetZ = target[1], target[2]

         -- Find all y such that R(x,y) and R(y,z)
         for y = 1, t:size(2) do
            if t[targetX][y] > 0 then
               -- R(x, y) holds, check R(y, z)
               if y <= t:size(1) and t[y][targetZ] > 0 then
                  table.insert(trace, {
                     depth = depth,
                     target = target,
                     found = 'chain',
                     via = y
                  })
                  return true, 'chain via ' .. self:decodeSymbol(y)
               end
            end
         end
      end

      return false, 'not found'
   end

   local proved, reason = prove(encoded, 1)

   return {
      proved = proved,
      reason = reason,
      trace = trace,
      query = queryTuple
   }
end

function TensorLogic:lookup(rel, encodedTuple)
   -- Direct lookup in relation tensor
   local t = rel.tensor
   if rel.arity == 1 then
      return t[encodedTuple[1]] > 0
   elseif rel.arity == 2 then
      if encodedTuple[1] <= t:size(1) and encodedTuple[2] <= t:size(2) then
         return t[encodedTuple[1]][encodedTuple[2]] > 0
      end
   elseif rel.arity == 3 then
      if encodedTuple[1] <= t:size(1) and
         encodedTuple[2] <= t:size(2) and
         encodedTuple[3] <= t:size(3) then
         return t[encodedTuple[1]][encodedTuple[2]][encodedTuple[3]] > 0
      end
   end
   return false
end

function TensorLogic:query(rel, tuple)
   -- Query if a tuple is in the relation
   local encoded = self:encodeSymbols(tuple)
   return self:lookup(rel, encoded)
end

function TensorLogic:extractTuples(rel)
   -- Extract all true tuples from relation
   local tuples = {}
   local t = rel.tensor

   if rel.arity == 1 then
      for i = 1, t:size(1) do
         if t[i] > 0 then
            table.insert(tuples, {self:decodeSymbol(i)})
         end
      end
   elseif rel.arity == 2 then
      for i = 1, t:size(1) do
         for j = 1, t:size(2) do
            if t[i][j] > 0 then
               table.insert(tuples, {self:decodeSymbol(i), self:decodeSymbol(j)})
            end
         end
      end
   elseif rel.arity == 3 then
      for i = 1, t:size(1) do
         for j = 1, t:size(2) do
            for k = 1, t:size(3) do
               if t[i][j][k] > 0 then
                  table.insert(tuples, {
                     self:decodeSymbol(i),
                     self:decodeSymbol(j),
                     self:decodeSymbol(k)
                  })
               end
            end
         end
      end
   end

   return tuples
end

-- Embedding Space Operations

function TensorLogic:createEmbedding(symbols)
   --[[
      Create learnable embeddings for symbols.

      This enables "sound reasoning in embedding space" by representing
      symbols as continuous vectors that can be learned.
   ]]
   local numSymbols = #symbols
   local embeddings = torch.randn(numSymbols, self.embedding_dim)

   -- Normalize
   for i = 1, numSymbols do
      embeddings[i]:div(embeddings[i]:norm() + 1e-8)
   end

   return {
      embeddings = embeddings,
      symbols = symbols,
      lookup = function(symbol)
         for i, s in ipairs(symbols) do
            if s == symbol then
               return embeddings[i]
            end
         end
         return nil
      end
   }
end

function TensorLogic:similarityMatrix(embeddings)
   -- Compute pairwise cosine similarity
   local e = embeddings.embeddings
   local similarity = torch.mm(e, e:t())
   return similarity
end

-- Rule Representation

function TensorLogic:rule(head, body)
   --[[
      Create a logical rule in tensor form.

      Rule: head :- body1, body2, ...

      Args:
         head: Head relation name
         body: List of body relation names

      Returns:
         Rule object that can be applied for inference
   ]]
   return {
      head = head,
      body = body,
      apply = function(self, facts)
         -- Join all body relations
         local result = facts[body[1]]
         for i = 2, #body do
            result = self:tensorJoin(result, facts[body[i]])
         end
         -- Apply step function
         result.tensor = self:step(result.tensor)
         result.name = head
         return result
      end
   }
end

-- Integration with Neural Networks

function TensorLogic:neuralRule(inputDim, outputDim, hiddenDim)
   --[[
      Create a neural network that can learn logical rules.

      The network learns to map from input relation to output relation,
      essentially learning the rule's transformation.
   ]]
   hiddenDim = hiddenDim or 64

   local network = nn.Sequential()
   network:add(nn.Linear(inputDim, hiddenDim))
   network:add(nn.ReLU())
   network:add(nn.Linear(hiddenDim, outputDim))
   network:add(nn.Sigmoid())

   return {
      network = network,
      forward = function(self, inputRel)
         local input = inputRel.tensor:view(-1)
         local output = network:forward(input)
         return output:view(outputDim, 1)
      end,
      learn = function(self, inputRel, targetRel, criterion, learningRate)
         criterion = criterion or nn.MSECriterion()
         learningRate = learningRate or 0.01

         local input = inputRel.tensor:view(-1)
         local target = targetRel.tensor:view(-1)

         local output = network:forward(input)
         local loss = criterion:forward(output, target)
         local gradOutput = criterion:backward(output, target)
         network:backward(input, gradOutput)
         network:updateParameters(learningRate)

         return loss
      end
   }
end

-- Utility Functions

function TensorLogic:getRelation(name)
   return self.relations[name]
end

function TensorLogic:listRelations()
   local names = {}
   for name, _ in pairs(self.relations) do
      table.insert(names, name)
   end
   return names
end

function TensorLogic:getInferenceTrace()
   return self.inferenceTrace
end

function TensorLogic:clearTrace()
   self.inferenceTrace = {}
end

function TensorLogic:stats()
   -- Return statistics about the logic system
   return {
      numSymbols = self.nextSymbolId - 1,
      numRelations = #self:listRelations(),
      embeddingDim = self.embedding_dim,
      useSoftLogic = self.use_soft_logic
   }
end

function TensorLogic:__tostring__()
   local stats = self:stats()
   return string.format("%s{symbols=%d, relations=%d, embedding_dim=%d}",
      torch.type(self),
      stats.numSymbols,
      stats.numRelations,
      stats.embeddingDim
   )
end

return nn.TensorLogic
