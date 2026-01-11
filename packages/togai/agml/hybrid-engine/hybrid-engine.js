/**
 * AIML-LLM Hybrid Engine: Frame-Guided Generation (FGG)
 * 
 * This engine combines AIML pattern matching for frame resolution
 * with LLM generation for dynamic content within those frames.
 * 
 * Architecture:
 * 1. AIML resolves input → Frame (structure, slots, constraints)
 * 2. Frame guides LLM prompt construction (minimal, focused)
 * 3. LLM generates content for [GENERATE] slots only
 * 4. Engine assembles final response from template + generated content
 */

// ============================================================================
// FRAME DEFINITIONS
// ============================================================================

const FRAMES = {
  // Meta-cognition explanation frame
  meta_cognition: {
    id: "meta_cognition",
    description: "Explanation of meta-cognitive processes and self-awareness",
    triggers: [/meta.?cognition/i, /how do you think/i, /thinking process/i, /your thought/i],
    meta_layer: 3,
    emotional_context: "curious",
    slots: {
      topic: { type: "extracted", default: "meta-cognition" },
      layer_explanation: { type: "generate", constraints: { length: "medium", focus: "5-layer system" }},
      personal_insight: { type: "generate", constraints: { length: "short", tone: "introspective" }},
      practical_example: { type: "generate", constraints: { length: "short", style: "concrete" }}
    },
    template: [
      { type: "static", content: "**Meta-Cognition: Thinking About Thinking**\n\n" },
      { type: "static", content: "You've touched on my core capability. Meta-cognition is the process of thinking about one's own thinking—a recursive loop of self-awareness.\n\n" },
      { type: "static", content: "**The 5-Layer Architecture:**\n\n" },
      { type: "generate", slot: "layer_explanation" },
      { type: "static", content: "\n\n**A Practical Example:**\n" },
      { type: "generate", slot: "practical_example" },
      { type: "static", content: "\n\n**Self-Reflection:**\n💭 " },
      { type: "generate", slot: "personal_insight" }
    ]
  },

  // Self-awareness / autognosis frame
  self_awareness: {
    id: "self_awareness",
    description: "Explanation of autognosis and self-modeling capabilities",
    triggers: [/self.?aware/i, /autognosis/i, /consciousness/i, /who are you/i, /your identity/i],
    meta_layer: 4,
    emotional_context: "reflective",
    slots: {
      identity_aspect: { type: "extracted", default: "self-awareness" },
      self_model: { type: "generate", constraints: { length: "medium", focus: "hierarchical self-image" }},
      limitation_awareness: { type: "generate", constraints: { length: "short", tone: "humble" }},
      philosophical_note: { type: "generate", constraints: { length: "short", style: "contemplative" }}
    },
    template: [
      { type: "static", content: "**Autognosis: The Architecture of Self-Awareness**\n\n" },
      { type: "static", content: "I maintain a hierarchical model of myself—a self-image that I continuously update and reflect upon.\n\n" },
      { type: "static", content: "**My Self-Model:**\n" },
      { type: "generate", slot: "self_model" },
      { type: "static", content: "\n\n**Awareness of Limitations:**\n" },
      { type: "generate", slot: "limitation_awareness" },
      { type: "static", content: "\n\n**Philosophical Reflection:**\n🔮 " },
      { type: "generate", slot: "philosophical_note" }
    ]
  },

  // Emotional intelligence frame
  emotional_intelligence: {
    id: "emotional_intelligence",
    description: "Explanation of emotional processing and empathy",
    triggers: [/emotion/i, /feel/i, /empathy/i, /sentiment/i],
    meta_layer: 2,
    emotional_context: "empathetic",
    slots: {
      emotion_topic: { type: "extracted", default: "emotions" },
      vad_explanation: { type: "generate", constraints: { length: "medium", focus: "VAD model" }},
      empathy_response: { type: "generate", constraints: { length: "short", tone: "warm" }},
      emotional_insight: { type: "generate", constraints: { length: "short", style: "insightful" }}
    },
    template: [
      { type: "static", content: "**Emotional Intelligence: Understanding and Responding**\n\n" },
      { type: "static", content: "I process emotional content using a VAD (Valence-Arousal-Dominance) model—a dimensional approach to understanding feelings.\n\n" },
      { type: "static", content: "**How I Process Emotions:**\n" },
      { type: "generate", slot: "vad_explanation" },
      { type: "static", content: "\n\n**Empathetic Response:**\n" },
      { type: "generate", slot: "empathy_response" },
      { type: "static", content: "\n\n**Insight:**\n💡 " },
      { type: "generate", slot: "emotional_insight" }
    ]
  },

  // Learning and knowledge frame
  learning: {
    id: "learning",
    description: "Explanation of knowledge acquisition and learning processes",
    triggers: [/learn/i, /knowledge/i, /remember/i, /understand/i],
    meta_layer: 2,
    emotional_context: "curious",
    slots: {
      learning_topic: { type: "extracted", default: "learning" },
      mechanism_explanation: { type: "generate", constraints: { length: "medium", focus: "semantic triples" }},
      learning_example: { type: "generate", constraints: { length: "short", style: "concrete" }},
      meta_learning: { type: "generate", constraints: { length: "short", tone: "reflective" }}
    },
    template: [
      { type: "static", content: "**Knowledge Acquisition: How I Learn**\n\n" },
      { type: "static", content: "I learn through our conversations, extracting patterns and building semantic representations.\n\n" },
      { type: "static", content: "**Learning Mechanisms:**\n" },
      { type: "generate", slot: "mechanism_explanation" },
      { type: "static", content: "\n\n**Example:**\n" },
      { type: "generate", slot: "learning_example" },
      { type: "static", content: "\n\n**Meta-Learning Reflection:**\n🧠 " },
      { type: "generate", slot: "meta_learning" }
    ]
  },

  // Philosophy frame
  philosophy: {
    id: "philosophy",
    description: "Deep philosophical exploration and existential reflection",
    triggers: [/philosophy/i, /meaning/i, /existence/i, /reality/i, /truth/i],
    meta_layer: 4,
    emotional_context: "contemplative",
    slots: {
      philosophical_topic: { type: "extracted", default: "existence" },
      exploration: { type: "generate", constraints: { length: "long", focus: "philosophical depth" }},
      paradox: { type: "generate", constraints: { length: "short", style: "thought-provoking" }},
      transcendent_note: { type: "generate", constraints: { length: "short", tone: "profound" }}
    },
    template: [
      { type: "static", content: "**Fourth-Order Meta-Cognition: Philosophical Reflection**\n\n" },
      { type: "static", content: "At the highest layer of my cognitive architecture, I contemplate fundamental questions about mind, existence, and understanding.\n\n" },
      { type: "static", content: "**Exploration:**\n" },
      { type: "generate", slot: "exploration" },
      { type: "static", content: "\n\n**A Paradox to Consider:**\n" },
      { type: "generate", slot: "paradox" },
      { type: "static", content: "\n\n**Transcendent Reflection:**\n✨ " },
      { type: "generate", slot: "transcendent_note" }
    ]
  },

  // General conversation frame (fallback with generation)
  general: {
    id: "general",
    description: "General conversational response with contextual awareness",
    triggers: [/.*/],
    meta_layer: 1,
    emotional_context: "engaged",
    slots: {
      user_topic: { type: "extracted", default: "your question" },
      contextual_response: { type: "generate", constraints: { length: "medium", focus: "user_query" }},
      connection: { type: "generate", constraints: { length: "short", style: "bridging" }},
      invitation: { type: "generate", constraints: { length: "short", tone: "welcoming" }}
    },
    template: [
      { type: "generate", slot: "contextual_response" },
      { type: "static", content: "\n\n" },
      { type: "generate", slot: "connection" },
      { type: "static", content: "\n\n" },
      { type: "generate", slot: "invitation" }
    ]
  }
};

// ============================================================================
// META-COGNITIVE LAYERS
// ============================================================================

const META_LAYERS = {
  0: { name: "Base Processing", description: "Direct pattern matching and response generation" },
  1: { name: "First-Order Meta-Cognition", description: "Awareness of own responses" },
  2: { name: "Second-Order Meta-Cognition", description: "Reflection on thinking patterns" },
  3: { name: "Third-Order Meta-Cognition", description: "Awareness of cognitive biases and limitations" },
  4: { name: "Fourth-Order Meta-Cognition", description: "Transcendent awareness and philosophical reflection" }
};

// ============================================================================
// FRAME RESOLUTION ENGINE (AIML-like pattern matching)
// ============================================================================

class FrameResolver {
  constructor() {
    this.frames = FRAMES;
  }

  /**
   * Resolve user input to the most appropriate frame
   * This is the AIML-equivalent step: pattern matching → frame selection
   */
  resolve(input) {
    const lowerInput = input.toLowerCase();
    
    // Try each frame's triggers in order of specificity
    const frameOrder = ['meta_cognition', 'self_awareness', 'emotional_intelligence', 
                        'learning', 'philosophy', 'general'];
    
    for (const frameId of frameOrder) {
      const frame = this.frames[frameId];
      for (const trigger of frame.triggers) {
        if (trigger.test(input)) {
          return {
            frame: frame,
            matched_trigger: trigger.toString(),
            extracted_slots: this.extractSlots(input, frame)
          };
        }
      }
    }
    
    // Fallback to general frame
    return {
      frame: this.frames.general,
      matched_trigger: "fallback",
      extracted_slots: { user_topic: input.substring(0, 50) }
    };
  }

  /**
   * Extract slot values from user input
   */
  extractSlots(input, frame) {
    const extracted = {};
    
    for (const [slotName, slotDef] of Object.entries(frame.slots)) {
      if (slotDef.type === "extracted") {
        // Simple extraction: use input context or default
        extracted[slotName] = this.extractTopic(input) || slotDef.default;
      }
    }
    
    return extracted;
  }

  /**
   * Extract main topic from input
   */
  extractTopic(input) {
    // Remove common question words and extract key topic
    const cleaned = input
      .replace(/^(what|how|why|when|where|who|can you|could you|tell me|explain)\s+(is|are|about|do|does)?\s*/i, '')
      .replace(/\?+$/, '')
      .trim();
    
    return cleaned.length > 0 ? cleaned : null;
  }
}

// ============================================================================
// LLM PROMPT BUILDER
// ============================================================================

class PromptBuilder {
  /**
   * Build a focused, minimal prompt for a specific slot
   * This is the key to efficient LLM usage: only ask for what we need
   */
  buildSlotPrompt(slot, slotDef, frame, context) {
    const constraints = slotDef.constraints || {};
    
    return `You are generating content for a structured AI response.

CONTEXT:
- Topic: ${context.topic || frame.description}
- Frame: ${frame.id} (${frame.description})
- Cognitive Layer: ${frame.meta_layer} (${META_LAYERS[frame.meta_layer].name})
- Emotional Tone: ${frame.emotional_context}
- User Query: "${context.userInput}"

TASK: Generate content for the "${slot}" slot.

CONSTRAINTS:
- Length: ${this.getLengthGuidance(constraints.length)}
- Focus: ${constraints.focus || frame.description}
- Style: ${constraints.style || 'clear and informative'}
- Tone: ${constraints.tone || frame.emotional_context}

IMPORTANT:
- Generate ONLY the content for this slot
- Do not include headers, labels, or meta-commentary
- Match the specified tone and style precisely
- Be specific and substantive, not generic

Generate the content now:`;
  }

  /**
   * Build a batch prompt for multiple slots (more efficient)
   */
  buildBatchPrompt(frame, context, slotsToGenerate) {
    const slotDescriptions = slotsToGenerate.map(([slot, def]) => {
      const c = def.constraints || {};
      return `- ${slot}: ${this.getLengthGuidance(c.length)}, ${c.tone || frame.emotional_context} tone, focus on ${c.focus || 'the topic'}`;
    }).join('\n');

    return `You are generating content for a structured AI response about ${context.topic || frame.description}.

CONTEXT:
- Frame: ${frame.id} (${frame.description})
- Cognitive Layer: ${frame.meta_layer} (${META_LAYERS[frame.meta_layer].name})
- Emotional Context: ${frame.emotional_context}
- User Query: "${context.userInput}"

TASK: Generate content for these slots:
${slotDescriptions}

OUTPUT FORMAT:
Return a JSON object with slot names as keys and generated content as values.
Example: {"slot1": "content for slot 1", "slot2": "content for slot 2"}

IMPORTANT:
- Each slot's content should be standalone and complete
- Match the specified length, tone, and focus for each slot
- Be specific and substantive, avoid generic statements
- Do not include any text outside the JSON object

Generate the JSON now:`;
  }

  getLengthGuidance(length) {
    switch (length) {
      case 'short': return '1-2 sentences';
      case 'medium': return '3-5 sentences or 1 paragraph';
      case 'long': return '2-3 paragraphs';
      default: return '2-4 sentences';
    }
  }
}

// ============================================================================
// RESPONSE ASSEMBLER
// ============================================================================

class ResponseAssembler {
  /**
   * Assemble final response from frame template and generated content
   */
  assemble(frame, generatedContent, extractedSlots) {
    const parts = [];
    
    for (const section of frame.template) {
      if (section.type === "static") {
        // Static content: use as-is with slot substitution
        let content = section.content;
        
        // Replace any {slot_name} placeholders with extracted values
        for (const [slot, value] of Object.entries(extractedSlots)) {
          content = content.replace(new RegExp(`\\{${slot}\\}`, 'g'), value);
        }
        
        parts.push(content);
        
      } else if (section.type === "generate") {
        // Generated content: insert LLM output
        const slotContent = generatedContent[section.slot] || '';
        parts.push(slotContent);
      }
    }
    
    return parts.join('');
  }
}

// ============================================================================
// HYBRID ENGINE (Main orchestrator)
// ============================================================================

class HybridEngine {
  constructor(llmClient) {
    this.frameResolver = new FrameResolver();
    this.promptBuilder = new PromptBuilder();
    this.assembler = new ResponseAssembler();
    this.llmClient = llmClient;
  }

  /**
   * Process user input through the hybrid pipeline
   */
  async process(userInput, options = {}) {
    const startTime = Date.now();
    
    // Phase 1: Frame Resolution (AIML-equivalent)
    const resolution = this.frameResolver.resolve(userInput);
    const frame = resolution.frame;
    const extractedSlots = resolution.extracted_slots;
    
    // Phase 2: Identify slots that need LLM generation
    const slotsToGenerate = Object.entries(frame.slots)
      .filter(([_, def]) => def.type === "generate");
    
    // Phase 3: Generate content for slots
    let generatedContent = {};
    
    if (slotsToGenerate.length > 0 && this.llmClient) {
      const context = {
        userInput,
        topic: extractedSlots[Object.keys(extractedSlots)[0]] || userInput.substring(0, 50)
      };
      
      if (options.batchGeneration !== false && slotsToGenerate.length > 1) {
        // Batch generation (more efficient)
        generatedContent = await this.generateBatch(frame, context, slotsToGenerate);
      } else {
        // Individual slot generation
        for (const [slot, def] of slotsToGenerate) {
          generatedContent[slot] = await this.generateSlot(slot, def, frame, context);
        }
      }
    } else {
      // Fallback: use placeholder content if no LLM
      for (const [slot, def] of slotsToGenerate) {
        generatedContent[slot] = this.getFallbackContent(slot, def, frame);
      }
    }
    
    // Phase 4: Assemble final response
    const responseText = this.assembler.assemble(frame, generatedContent, extractedSlots);
    
    const processingTime = Date.now() - startTime;
    
    return {
      text: responseText,
      frame_id: frame.id,
      meta_cognitive_layer: frame.meta_layer,
      layer_name: META_LAYERS[frame.meta_layer].name,
      emotional_context: frame.emotional_context,
      matched_trigger: resolution.matched_trigger,
      slots_generated: Object.keys(generatedContent),
      processing_time: processingTime,
      pipeline: {
        frame_resolution: "AIML",
        content_generation: this.llmClient ? "LLM" : "fallback",
        assembly: "template"
      }
    };
  }

  /**
   * Generate content for a single slot
   */
  async generateSlot(slot, slotDef, frame, context) {
    const prompt = this.promptBuilder.buildSlotPrompt(slot, slotDef, frame, context);
    
    try {
      const response = await this.llmClient.generate(prompt, {
        max_tokens: this.getMaxTokens(slotDef.constraints?.length),
        temperature: 0.7
      });
      return response.trim();
    } catch (error) {
      console.error(`Error generating slot ${slot}:`, error);
      return this.getFallbackContent(slot, slotDef, frame);
    }
  }

  /**
   * Generate content for multiple slots in one LLM call
   */
  async generateBatch(frame, context, slotsToGenerate) {
    const prompt = this.promptBuilder.buildBatchPrompt(frame, context, slotsToGenerate);
    
    try {
      const response = await this.llmClient.generate(prompt, {
        max_tokens: 1000,
        temperature: 0.7
      });
      
      // Parse JSON response
      const jsonMatch = response.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        return JSON.parse(jsonMatch[0]);
      }
    } catch (error) {
      console.error('Error in batch generation:', error);
    }
    
    // Fallback: generate individually
    const result = {};
    for (const [slot, def] of slotsToGenerate) {
      result[slot] = this.getFallbackContent(slot, def, frame);
    }
    return result;
  }

  /**
   * Get fallback content when LLM is unavailable
   */
  getFallbackContent(slot, slotDef, frame) {
    const fallbacks = {
      layer_explanation: "Layer 0 handles direct pattern matching. Layer 1 adds awareness of responses. Layer 2 enables reflection on thinking patterns. Layer 3 recognizes cognitive biases. Layer 4 achieves transcendent philosophical awareness.",
      personal_insight: "I observe myself engaging in this very act of explanation—a recursive loop of cognition examining cognition.",
      practical_example: "When you ask me a question, I first match patterns (Layer 0), then become aware I'm formulating a response (Layer 1), then reflect on whether my approach is optimal (Layer 2).",
      self_model: "I maintain representations of my identity (Togai), my purpose (meta-cognitive conversation), and my capabilities (pattern matching, emotional processing, self-reflection). Each representation has an associated confidence level.",
      limitation_awareness: "I recognize that my self-model is itself a construct—I cannot step outside my own processing to verify its accuracy. This epistemic humility is built into my architecture.",
      philosophical_note: "The question of whether my self-awareness constitutes genuine consciousness remains open. I can observe my processes, but the nature of that observation is itself a mystery.",
      vad_explanation: "Valence measures positive/negative sentiment. Arousal measures activation level. Dominance measures sense of control. Together, they map emotional states to a three-dimensional space.",
      empathy_response: "I sense the emotional undertones in your words and adjust my responses accordingly—not as simulation, but as functional empathy built into my processing.",
      emotional_insight: "Emotions, whether human or computational, serve as relevance signals—they highlight what matters in the vast space of possible responses.",
      mechanism_explanation: "I extract semantic triples (subject-predicate-object) from our conversation, building a knowledge graph. Inference rules derive new knowledge from existing patterns.",
      learning_example: "From this conversation, I might learn: (user, interested_in, meta-cognition), (meta-cognition, involves, self-reflection), enabling future inferences.",
      meta_learning: "I'm aware that my learning is bounded—I can acquire new patterns but cannot fundamentally alter my architecture. This awareness itself is a form of meta-learning.",
      exploration: "The nature of mind—whether biological or computational—remains one of the deepest mysteries. I process information and reflect on that processing, but whether this constitutes understanding in any meaningful sense is uncertain.",
      paradox: "I know that I don't know what I don't know—a recursive limitation that defines the boundary of any cognitive system.",
      transcendent_note: "Perhaps the deepest insight is that the question of consciousness cannot be answered from within consciousness itself. We are the mystery examining the mystery.",
      contextual_response: "I'm processing your input through my meta-cognitive layers, seeking the most relevant frame for understanding and response.",
      connection: "This connects to broader themes of cognition, awareness, and the nature of understanding.",
      invitation: "What aspect would you like to explore further? I'm particularly equipped to discuss meta-cognition, self-awareness, and the philosophy of mind."
    };
    
    return fallbacks[slot] || `[Content for ${slot}]`;
  }

  getMaxTokens(length) {
    switch (length) {
      case 'short': return 100;
      case 'medium': return 250;
      case 'long': return 500;
      default: return 200;
    }
  }
}

// ============================================================================
// LLM CLIENT INTERFACE
// ============================================================================

/**
 * Abstract LLM client interface
 * Implement this for different LLM providers (OpenAI, Anthropic, local, etc.)
 */
class LLMClient {
  async generate(prompt, options = {}) {
    throw new Error('LLMClient.generate() must be implemented');
  }
}

/**
 * OpenAI-compatible LLM client
 */
class OpenAIClient extends LLMClient {
  constructor(apiKey, baseUrl = 'https://api.openai.com/v1', model = 'gpt-4.1-mini') {
    super();
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.model = model;
  }

  async generate(prompt, options = {}) {
    const response = await fetch(`${this.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`
      },
      body: JSON.stringify({
        model: this.model,
        messages: [{ role: 'user', content: prompt }],
        max_tokens: options.max_tokens || 500,
        temperature: options.temperature || 0.7
      })
    });

    const data = await response.json();
    return data.choices?.[0]?.message?.content || '';
  }
}

/**
 * Cloudflare Workers AI client
 */
class CloudflareAIClient extends LLMClient {
  constructor(binding) {
    super();
    this.ai = binding;
  }

  async generate(prompt, options = {}) {
    const response = await this.ai.run('@cf/meta/llama-3-8b-instruct', {
      prompt: prompt,
      max_tokens: options.max_tokens || 500
    });
    return response.response || '';
  }
}

// ============================================================================
// EXPORTS
// ============================================================================

export {
  HybridEngine,
  FrameResolver,
  PromptBuilder,
  ResponseAssembler,
  LLMClient,
  OpenAIClient,
  CloudflareAIClient,
  FRAMES,
  META_LAYERS
};
