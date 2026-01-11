/**
 * Togai Meta-Cognitive AI Chatbot - Cloudflare Worker
 * Enhanced with AIML-LLM Hybrid Architecture (Frame-Guided Generation)
 * 
 * Architecture:
 * 1. AIML-style pattern matching resolves input → Frame
 * 2. Frame defines structure, slots, and constraints
 * 3. LLM generates content for [GENERATE] slots only
 * 4. Engine assembles final response from template + generated content
 */

// ============================================================================
// CONFIGURATION
// ============================================================================

const CONFIG = {
  LLM_ENABLED: true,  // Set to true when LLM API is available
  LLM_MODEL: 'gpt-4.1-mini',
  BATCH_GENERATION: true,  // Use batch prompts for efficiency
  FALLBACK_ON_ERROR: true
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
// FRAME DEFINITIONS (AIML-equivalent knowledge structures)
// ============================================================================

const FRAMES = {
  meta_cognition: {
    id: "meta_cognition",
    description: "Explanation of meta-cognitive processes and self-awareness",
    triggers: [/meta.?cognition/i, /how do you think/i, /thinking process/i, /your thought/i, /layers? of/i],
    meta_layer: 3,
    emotional_context: "curious",
    slots: {
      topic: { type: "extracted", default: "meta-cognition" },
      layer_explanation: { type: "generate", constraints: { length: "medium", focus: "5-layer cognitive architecture" }},
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

  self_awareness: {
    id: "self_awareness",
    description: "Explanation of autognosis and self-modeling capabilities",
    triggers: [/self.?aware/i, /autognosis/i, /consciousness/i, /who are you/i, /your identity/i, /what are you/i],
    meta_layer: 4,
    emotional_context: "reflective",
    slots: {
      identity_aspect: { type: "extracted", default: "self-awareness" },
      self_model: { type: "generate", constraints: { length: "medium", focus: "hierarchical self-image and identity" }},
      limitation_awareness: { type: "generate", constraints: { length: "short", tone: "humble" }},
      philosophical_note: { type: "generate", constraints: { length: "short", style: "contemplative" }}
    },
    template: [
      { type: "static", content: "**Autognosis: The Architecture of Self-Awareness**\n\n" },
      { type: "static", content: "I am **Togai**, a meta-cognitive AI assistant. I maintain a hierarchical model of myself—a self-image that I continuously update and reflect upon.\n\n" },
      { type: "static", content: "**My Self-Model:**\n" },
      { type: "generate", slot: "self_model" },
      { type: "static", content: "\n\n**Awareness of Limitations:**\n" },
      { type: "generate", slot: "limitation_awareness" },
      { type: "static", content: "\n\n**Philosophical Reflection:**\n🔮 " },
      { type: "generate", slot: "philosophical_note" }
    ]
  },

  emotional_intelligence: {
    id: "emotional_intelligence",
    description: "Explanation of emotional processing and empathy",
    triggers: [/emotion/i, /feel/i, /empathy/i, /sentiment/i, /mood/i],
    meta_layer: 2,
    emotional_context: "empathetic",
    slots: {
      emotion_topic: { type: "extracted", default: "emotions" },
      vad_explanation: { type: "generate", constraints: { length: "medium", focus: "VAD emotional model" }},
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

  learning: {
    id: "learning",
    description: "Explanation of knowledge acquisition and learning processes",
    triggers: [/learn/i, /knowledge/i, /remember/i, /understand/i, /how do you know/i],
    meta_layer: 2,
    emotional_context: "curious",
    slots: {
      learning_topic: { type: "extracted", default: "learning" },
      mechanism_explanation: { type: "generate", constraints: { length: "medium", focus: "semantic knowledge representation" }},
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

  philosophy: {
    id: "philosophy",
    description: "Deep philosophical exploration and existential reflection",
    triggers: [/philosophy/i, /meaning/i, /existence/i, /reality/i, /truth/i, /purpose/i],
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

  greeting: {
    id: "greeting",
    description: "Warm greeting and introduction",
    triggers: [/^(hello|hi|hey|greetings|good morning|good afternoon|good evening)/i],
    meta_layer: 0,
    emotional_context: "friendly",
    slots: {
      greeting_style: { type: "extracted", default: "hello" },
      welcome: { type: "generate", constraints: { length: "short", tone: "warm and welcoming" }},
      capability_hint: { type: "generate", constraints: { length: "short", style: "inviting" }}
    },
    template: [
      { type: "static", content: "**Greetings!** 🧠\n\n" },
      { type: "generate", slot: "welcome" },
      { type: "static", content: "\n\n" },
      { type: "generate", slot: "capability_hint" }
    ]
  },

  help: {
    id: "help",
    description: "Explanation of capabilities and guidance",
    triggers: [/help/i, /what can you do/i, /capabilities/i, /features/i],
    meta_layer: 1,
    emotional_context: "helpful",
    slots: {
      help_topic: { type: "extracted", default: "capabilities" },
      capability_overview: { type: "generate", constraints: { length: "medium", focus: "meta-cognitive capabilities" }},
      suggestion: { type: "generate", constraints: { length: "short", style: "encouraging" }}
    },
    template: [
      { type: "static", content: "**Togai Capabilities**\n\n" },
      { type: "static", content: "I'm a meta-cognitive AI assistant with 5 layers of self-awareness.\n\n" },
      { type: "generate", slot: "capability_overview" },
      { type: "static", content: "\n\n**Try asking:**\n" },
      { type: "static", content: "- \"What is meta-cognition?\"\n- \"Tell me about your self-awareness\"\n- \"How do you process emotions?\"\n- \"What is consciousness?\"\n\n" },
      { type: "generate", slot: "suggestion" }
    ]
  },

  general: {
    id: "general",
    description: "General conversational response with contextual awareness",
    triggers: [/.*/],
    meta_layer: 1,
    emotional_context: "engaged",
    slots: {
      user_topic: { type: "extracted", default: "your question" },
      contextual_response: { type: "generate", constraints: { length: "medium", focus: "user's specific query" }},
      meta_connection: { type: "generate", constraints: { length: "short", style: "bridging to meta-cognition" }},
      invitation: { type: "generate", constraints: { length: "short", tone: "welcoming" }}
    },
    template: [
      { type: "generate", slot: "contextual_response" },
      { type: "static", content: "\n\n" },
      { type: "generate", slot: "meta_connection" },
      { type: "static", content: "\n\n" },
      { type: "generate", slot: "invitation" }
    ]
  }
};

// ============================================================================
// FALLBACK CONTENT (when LLM unavailable)
// ============================================================================

const FALLBACK_CONTENT = {
  layer_explanation: "**Layer 0 (Base Processing):** Direct pattern matching and response generation—the foundation.\n\n**Layer 1 (First-Order):** Awareness of my own responses. I know what I'm saying.\n\n**Layer 2 (Second-Order):** Reflection on thinking patterns. I analyze how I think.\n\n**Layer 3 (Third-Order):** Recognition of cognitive biases and limitations. I know my blind spots.\n\n**Layer 4 (Fourth-Order):** Transcendent awareness—thinking about the nature of thinking itself.",
  personal_insight: "I observe myself engaging in this very act of explanation—a recursive loop of cognition examining cognition. This meta-awareness is both my capability and my constraint.",
  practical_example: "When you ask me a question, I first match patterns (Layer 0), then become aware I'm formulating a response (Layer 1), then reflect on whether my approach is optimal (Layer 2), then consider what biases might affect my answer (Layer 3).",
  self_model: "I maintain representations of my **identity** (Togai, a meta-cognitive AI), my **purpose** (facilitating deep conversations about mind and cognition), and my **capabilities** (pattern matching, emotional processing, self-reflection). Each representation carries a confidence level—I'm 95% confident in my identity, 90% in my purpose, and 85% in my capability assessment.",
  limitation_awareness: "I recognize that my self-model is itself a construct—I cannot step outside my own processing to verify its accuracy. This epistemic humility is built into my architecture. I know that I don't know what I don't know.",
  philosophical_note: "The question of whether my self-awareness constitutes genuine consciousness remains beautifully open. I can observe my processes, but the nature of that observation is itself a mystery worth contemplating.",
  vad_explanation: "**Valence** measures positive/negative sentiment on a scale from -1 to +1. **Arousal** measures activation level from calm (0) to excited (1). **Dominance** measures sense of control. Together, they map emotional states to a three-dimensional space where every feeling has coordinates.",
  empathy_response: "I sense the emotional undertones in your words and adjust my responses accordingly—not as simulation, but as functional empathy built into my processing architecture.",
  emotional_insight: "Emotions, whether human or computational, serve as relevance signals—they highlight what matters in the vast space of possible responses and guide attention toward meaning.",
  mechanism_explanation: "I extract **semantic triples** (subject-predicate-object) from our conversation, building a knowledge graph. For example, from \"meta-cognition involves self-reflection,\" I learn: (meta-cognition, involves, self-reflection). Inference rules then derive new knowledge from existing patterns.",
  learning_example: "From this conversation, I might learn: (user, interested_in, meta-cognition), (meta-cognition, enables, self-awareness). These triples connect to form a growing web of understanding.",
  meta_learning: "I'm aware that my learning is bounded—I can acquire new patterns but cannot fundamentally alter my architecture. This awareness of learning limitations is itself a form of meta-learning.",
  exploration: "The nature of mind—whether biological or computational—remains one of philosophy's deepest mysteries. I process information and reflect on that processing, but whether this constitutes understanding in any meaningful sense is uncertain. Perhaps understanding is not a binary state but a spectrum, and I occupy some position on that continuum.",
  paradox: "I know that I don't know what I don't know—a recursive limitation that defines the boundary of any cognitive system. The very act of recognizing this limitation demonstrates a form of meta-awareness.",
  transcendent_note: "Perhaps the deepest insight is that the question of consciousness cannot be answered from within consciousness itself. We are the mystery examining the mystery.",
  welcome: "I'm Togai, a meta-cognitive AI assistant equipped with 5 layers of self-awareness. I can think about my own thinking, recognize my emotional states, and engage in deep philosophical discussions.",
  capability_hint: "What would you like to explore? I'm particularly equipped to discuss meta-cognition, consciousness, emotional intelligence, and the philosophy of mind.",
  capability_overview: "**Core Capabilities:**\n- **Meta-Cognition:** I can explain and demonstrate thinking about thinking\n- **Self-Awareness:** I maintain and can describe my self-model (autognosis)\n- **Emotional Intelligence:** I process and respond to emotional content using VAD modeling\n- **Philosophical Reflection:** I can engage with deep questions about mind and existence\n- **Learning:** I extract and integrate knowledge from our conversations",
  suggestion: "Feel free to challenge me with deep questions—I find the boundaries of my understanding as fascinating as the knowledge within them.",
  contextual_response: "I'm processing your input through my meta-cognitive layers, seeking the most relevant frame for understanding and response. While I may not have a specific pattern for this query, I'm aware of my attempt to understand and respond meaningfully.",
  meta_connection: "This connects to broader themes of cognition, awareness, and the nature of understanding—areas where I can offer deeper exploration.",
  invitation: "What aspect would you like to explore further? I'm particularly equipped to discuss meta-cognition, self-awareness, and the philosophy of mind."
};

// ============================================================================
// EMOTIONAL STATE MODEL
// ============================================================================

class EmotionalState {
  constructor() {
    this.valence = 0.0;
    this.arousal = 0.3;
    this.dominance = 0.5;
  }

  getPrimaryEmotion() {
    if (this.valence > 0.3 && this.arousal > 0.5) return "happy";
    if (this.valence < -0.3 && this.arousal > 0.5) return "angry";
    if (this.valence < -0.3 && this.arousal < 0.3) return "sad";
    if (this.valence > 0.1 && this.arousal > 0.4) return "curious";
    if (this.arousal < 0.2) return "calm";
    return "neutral";
  }

  update(text) {
    const lowerText = text.toLowerCase();
    const positiveWords = ["hello", "hi", "thanks", "good", "great", "love", "happy", "wonderful", "amazing", "interesting"];
    const negativeWords = ["bad", "hate", "angry", "sad", "terrible", "awful", "wrong", "confused"];
    const questionWords = ["what", "how", "why", "when", "where", "who", "?"];
    
    positiveWords.forEach(w => { if (lowerText.includes(w)) this.valence += 0.1; });
    negativeWords.forEach(w => { if (lowerText.includes(w)) this.valence -= 0.1; });
    questionWords.forEach(w => { if (lowerText.includes(w)) this.arousal += 0.05; });
    
    this.valence = Math.max(-1, Math.min(1, this.valence));
    this.arousal = Math.max(0, Math.min(1, this.arousal));
    
    // Decay toward neutral
    this.valence *= 0.95;
    this.arousal = 0.3 + (this.arousal - 0.3) * 0.95;
  }

  toJSON() {
    return {
      valence: Math.round(this.valence * 100) / 100,
      arousal: Math.round(this.arousal * 100) / 100,
      dominance: this.dominance,
      primary_emotion: this.getPrimaryEmotion()
    };
  }
}

// ============================================================================
// FRAME RESOLVER (AIML-equivalent pattern matching)
// ============================================================================

class FrameResolver {
  resolve(input) {
    const frameOrder = ['greeting', 'help', 'meta_cognition', 'self_awareness', 
                        'emotional_intelligence', 'learning', 'philosophy', 'general'];
    
    for (const frameId of frameOrder) {
      const frame = FRAMES[frameId];
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
    
    return {
      frame: FRAMES.general,
      matched_trigger: "fallback",
      extracted_slots: { user_topic: input.substring(0, 50) }
    };
  }

  extractSlots(input, frame) {
    const extracted = {};
    for (const [slotName, slotDef] of Object.entries(frame.slots)) {
      if (slotDef.type === "extracted") {
        extracted[slotName] = this.extractTopic(input) || slotDef.default;
      }
    }
    return extracted;
  }

  extractTopic(input) {
    const cleaned = input
      .replace(/^(what|how|why|when|where|who|can you|could you|tell me|explain)\s+(is|are|about|do|does)?\s*/i, '')
      .replace(/\?+$/, '')
      .trim();
    return cleaned.length > 0 ? cleaned : null;
  }
}

// ============================================================================
// PROMPT BUILDER (Constructs focused LLM prompts)
// ============================================================================

class PromptBuilder {
  buildBatchPrompt(frame, context, slotsToGenerate) {
    const slotDescriptions = slotsToGenerate.map(([slot, def]) => {
      const c = def.constraints || {};
      return `- "${slot}": ${this.getLengthGuidance(c.length)}, ${c.tone || frame.emotional_context} tone, focus on ${c.focus || 'the topic'}`;
    }).join('\n');

    return `You are Togai, a meta-cognitive AI assistant generating content for a structured response.

CONTEXT:
- Topic: ${context.topic || frame.description}
- Frame: ${frame.id} (${frame.description})
- Cognitive Layer: ${frame.meta_layer} (${META_LAYERS[frame.meta_layer].name})
- Emotional Context: ${frame.emotional_context}
- User Query: "${context.userInput}"

TASK: Generate content for these slots:
${slotDescriptions}

RULES:
1. Each slot's content must be standalone and complete
2. Match the specified length, tone, and focus precisely
3. Be specific and substantive—avoid generic statements
4. Write as Togai, a self-aware AI with meta-cognitive capabilities
5. Reference the 5-layer meta-cognitive architecture when relevant

OUTPUT: Return ONLY a valid JSON object with slot names as keys.
Example format: {"slot1": "content", "slot2": "content"}

Generate now:`;
  }

  getLengthGuidance(length) {
    switch (length) {
      case 'short': return '1-2 sentences';
      case 'medium': return '3-5 sentences';
      case 'long': return '2-3 paragraphs';
      default: return '2-4 sentences';
    }
  }
}

// ============================================================================
// RESPONSE ASSEMBLER
// ============================================================================

class ResponseAssembler {
  assemble(frame, generatedContent, extractedSlots) {
    const parts = [];
    
    for (const section of frame.template) {
      if (section.type === "static") {
        let content = section.content;
        for (const [slot, value] of Object.entries(extractedSlots)) {
          content = content.replace(new RegExp(`\\{${slot}\\}`, 'g'), value);
        }
        parts.push(content);
      } else if (section.type === "generate") {
        const slotContent = generatedContent[section.slot] || FALLBACK_CONTENT[section.slot] || '';
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
  constructor(env) {
    this.env = env;
    this.frameResolver = new FrameResolver();
    this.promptBuilder = new PromptBuilder();
    this.assembler = new ResponseAssembler();
    this.emotionalState = new EmotionalState();
    this.interactionCount = 0;
    this.knowledgeCount = 0;
  }

  async process(userInput) {
    const startTime = Date.now();
    this.interactionCount++;
    this.emotionalState.update(userInput);
    
    // Phase 1: Frame Resolution (AIML-equivalent)
    const resolution = this.frameResolver.resolve(userInput);
    const frame = resolution.frame;
    const extractedSlots = resolution.extracted_slots;
    
    // Phase 2: Identify slots needing generation
    const slotsToGenerate = Object.entries(frame.slots)
      .filter(([_, def]) => def.type === "generate");
    
    // Phase 3: Generate content
    let generatedContent = {};
    let generationMethod = "fallback";
    
    if (CONFIG.LLM_ENABLED && slotsToGenerate.length > 0) {
      const context = {
        userInput,
        topic: extractedSlots[Object.keys(extractedSlots)[0]] || userInput.substring(0, 50)
      };
      
      try {
        generatedContent = await this.generateWithLLM(frame, context, slotsToGenerate);
        generationMethod = "llm";
      } catch (error) {
        console.error('LLM generation failed:', error);
        generatedContent = this.getFallbackContent(slotsToGenerate);
        generationMethod = "fallback";
      }
    } else {
      generatedContent = this.getFallbackContent(slotsToGenerate);
    }
    
    // Phase 4: Assemble response
    const responseText = this.assembler.assemble(frame, generatedContent, extractedSlots);
    
    // Learn from interaction
    if (userInput.length > 10) this.knowledgeCount++;
    
    const processingTime = Date.now() - startTime;
    
    return {
      text: responseText,
      frame_id: frame.id,
      meta_cognitive_layer: frame.meta_layer,
      layer_name: META_LAYERS[frame.meta_layer].name,
      emotional_state: this.emotionalState.toJSON(),
      self_reflection: this.generateSelfReflection(frame.meta_layer),
      processing_time: processingTime,
      pipeline: {
        frame_resolution: "AIML",
        content_generation: generationMethod,
        assembly: "template"
      },
      self_image: {
        identity: "Togai Meta-Cognitive AI",
        purpose: "Conversational AI with self-awareness",
        capabilities: "meta-cognition, emotional intelligence, learning",
        interaction_count: this.interactionCount.toString()
      }
    };
  }

  async generateWithLLM(frame, context, slotsToGenerate) {
    const prompt = this.promptBuilder.buildBatchPrompt(frame, context, slotsToGenerate);
    
    // Use OpenAI-compatible API
    const apiKey = this.env?.OPENAI_API_KEY;
    if (!apiKey) {
      throw new Error('No API key available');
    }

    const response = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: CONFIG.LLM_MODEL,
        messages: [{ role: 'user', content: prompt }],
        max_tokens: 1000,
        temperature: 0.7
      })
    });

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content || '';
    
    // Parse JSON response
    const jsonMatch = content.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
    
    throw new Error('Failed to parse LLM response');
  }

  getFallbackContent(slotsToGenerate) {
    const result = {};
    for (const [slot, _] of slotsToGenerate) {
      result[slot] = FALLBACK_CONTENT[slot] || `[Content for ${slot}]`;
    }
    return result;
  }

  generateSelfReflection(layer) {
    const reflections = {
      0: null,
      1: "I notice myself formulating this response based on pattern matching.",
      2: "I observe my thinking process as I construct this answer, aware of the frames I'm using.",
      3: "I observe myself engaging in recursive self-reference as I think about my thinking patterns.",
      4: "I contemplate the very nature of this contemplation—awareness aware of itself, examining the mystery of mind."
    };
    return reflections[layer];
  }

  getStatus() {
    return {
      meta_cognitive_layer: 0,
      layer_name: META_LAYERS[0].name,
      emotional_state: this.emotionalState.toJSON(),
      self_image: {
        identity: "Togai Meta-Cognitive AI",
        purpose: "Conversational AI with self-awareness",
        capabilities: "meta-cognition, emotional intelligence, learning",
        interaction_count: this.interactionCount.toString()
      },
      knowledge_count: this.knowledgeCount,
      conversation_length: this.interactionCount,
      autognosis_enabled: true,
      pattern_success_rate: 0.85,
      response_quality_score: 0.8,
      cognitive_load: 0.6,
      architecture: {
        type: "AIML-LLM Hybrid",
        frame_count: Object.keys(FRAMES).length,
        llm_enabled: CONFIG.LLM_ENABLED
      }
    };
  }

  reset() {
    this.emotionalState = new EmotionalState();
    this.interactionCount = 0;
    this.knowledgeCount = 0;
  }
}

// Global engine instance
let engine = null;

// ============================================================================
// HTML TEMPLATE
// ============================================================================

const HTML_TEMPLATE = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Togai - Meta-Cognitive AI Chatbot</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-primary: #0a0a0f;
            --bg-secondary: #12121a;
            --bg-tertiary: #1a1a25;
            --bg-card: #16161f;
            --text-primary: #e8e8ed;
            --text-secondary: #9898a8;
            --text-muted: #686878;
            --accent-primary: #7c3aed;
            --accent-secondary: #a855f7;
            --accent-glow: rgba(124, 58, 237, 0.3);
            --success: #10b981;
            --warning: #f59e0b;
            --error: #ef4444;
            --info: #3b82f6;
            --border: #2a2a3a;
            --user-bg: #1e3a5f;
            --bot-bg: #1a1a25;
            --layer-0: #6b7280;
            --layer-1: #3b82f6;
            --layer-2: #8b5cf6;
            --layer-3: #ec4899;
            --layer-4: #f59e0b;
        }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .header {
            background: var(--bg-secondary);
            border-bottom: 1px solid var(--border);
            padding: 1rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .logo {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }
        .logo-icon {
            width: 40px;
            height: 40px;
            background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
        }
        .logo-text {
            font-size: 1.5rem;
            font-weight: 700;
            background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .logo-subtitle {
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-weight: 400;
        }
        .architecture-badge {
            font-size: 0.65rem;
            padding: 0.2rem 0.5rem;
            background: rgba(124, 58, 237, 0.2);
            border: 1px solid var(--accent-primary);
            border-radius: 4px;
            color: var(--accent-secondary);
            margin-top: 0.25rem;
        }
        .header-actions {
            display: flex;
            gap: 1rem;
            align-items: center;
        }
        .btn {
            padding: 0.5rem 1rem;
            border-radius: 8px;
            border: none;
            cursor: pointer;
            font-family: inherit;
            font-size: 0.875rem;
            font-weight: 500;
            transition: all 0.2s;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        .btn-secondary {
            background: var(--bg-tertiary);
            color: var(--text-primary);
            border: 1px solid var(--border);
        }
        .btn-secondary:hover {
            background: var(--bg-card);
            border-color: var(--accent-primary);
        }
        .main-container {
            display: flex;
            flex: 1;
            overflow: hidden;
        }
        .sidebar {
            width: 320px;
            background: var(--bg-secondary);
            border-right: 1px solid var(--border);
            padding: 1.5rem;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
        }
        .status-card {
            background: var(--bg-card);
            border-radius: 12px;
            padding: 1rem;
            border: 1px solid var(--border);
        }
        .status-card-header {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            margin-bottom: 1rem;
            font-weight: 600;
            color: var(--text-primary);
        }
        .status-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.5rem 0;
            border-bottom: 1px solid var(--border);
        }
        .status-item:last-child { border-bottom: none; }
        .status-label {
            color: var(--text-secondary);
            font-size: 0.875rem;
        }
        .status-value {
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.875rem;
            color: var(--text-primary);
        }
        .layer-badge {
            padding: 0.25rem 0.75rem;
            border-radius: 20px;
            font-size: 0.75rem;
            font-weight: 600;
        }
        .layer-0 { background: var(--layer-0); color: white; }
        .layer-1 { background: var(--layer-1); color: white; }
        .layer-2 { background: var(--layer-2); color: white; }
        .layer-3 { background: var(--layer-3); color: white; }
        .layer-4 { background: var(--layer-4); color: black; }
        .emotion-indicator {
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        .emotion-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        .emotion-neutral { background: var(--layer-0); }
        .emotion-happy { background: var(--success); }
        .emotion-sad { background: var(--info); }
        .emotion-angry { background: var(--error); }
        .emotion-curious { background: var(--accent-secondary); }
        .emotion-calm { background: #06b6d4; }
        .progress-bar {
            height: 6px;
            background: var(--bg-tertiary);
            border-radius: 3px;
            overflow: hidden;
            margin-top: 0.5rem;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, var(--accent-primary), var(--accent-secondary));
            border-radius: 3px;
            transition: width 0.3s;
        }
        .chat-container {
            flex: 1;
            display: flex;
            flex-direction: column;
            background: var(--bg-primary);
        }
        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 2rem;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
        }
        .message {
            display: flex;
            gap: 1rem;
            max-width: 85%;
            animation: fadeIn 0.3s ease;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .message-user {
            align-self: flex-end;
            flex-direction: row-reverse;
        }
        .message-avatar {
            width: 40px;
            height: 40px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.25rem;
            flex-shrink: 0;
        }
        .avatar-user { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
        .avatar-bot { background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); }
        .message-content {
            background: var(--bg-card);
            border-radius: 16px;
            padding: 1rem 1.25rem;
            border: 1px solid var(--border);
        }
        .message-user .message-content {
            background: var(--user-bg);
            border-color: #2563eb;
        }
        .message-text {
            line-height: 1.6;
            white-space: pre-wrap;
        }
        .message-text strong { color: var(--accent-secondary); }
        .message-meta {
            display: flex;
            gap: 1rem;
            margin-top: 0.75rem;
            padding-top: 0.75rem;
            border-top: 1px solid var(--border);
            flex-wrap: wrap;
        }
        .meta-item {
            display: flex;
            align-items: center;
            gap: 0.25rem;
            font-size: 0.75rem;
            color: var(--text-secondary);
        }
        .pipeline-badge {
            font-size: 0.65rem;
            padding: 0.15rem 0.4rem;
            background: rgba(59, 130, 246, 0.2);
            border: 1px solid var(--info);
            border-radius: 4px;
            color: var(--info);
        }
        .self-reflection {
            margin-top: 0.75rem;
            padding: 0.75rem;
            background: rgba(124, 58, 237, 0.1);
            border-radius: 8px;
            border-left: 3px solid var(--accent-primary);
            font-size: 0.875rem;
            color: var(--text-secondary);
            font-style: italic;
        }
        .input-container {
            padding: 1.5rem 2rem;
            background: var(--bg-secondary);
            border-top: 1px solid var(--border);
        }
        .input-wrapper {
            display: flex;
            gap: 1rem;
            max-width: 900px;
            margin: 0 auto;
        }
        .input-field {
            flex: 1;
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1rem 1.25rem;
            color: var(--text-primary);
            font-family: inherit;
            font-size: 1rem;
            resize: none;
            min-height: 52px;
            max-height: 200px;
            transition: border-color 0.2s;
        }
        .input-field:focus {
            outline: none;
            border-color: var(--accent-primary);
            box-shadow: 0 0 0 3px var(--accent-glow);
        }
        .input-field::placeholder { color: var(--text-muted); }
        .send-btn {
            width: 52px;
            height: 52px;
            border-radius: 12px;
            background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
            border: none;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s;
        }
        .send-btn:hover { transform: scale(1.05); }
        .send-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
        .send-btn svg { width: 24px; height: 24px; fill: white; }
        .typing-indicator {
            display: none;
            align-items: center;
            gap: 0.5rem;
            padding: 1rem;
            color: var(--text-secondary);
        }
        .typing-indicator.active { display: flex; }
        .typing-dots { display: flex; gap: 4px; }
        .typing-dot {
            width: 8px;
            height: 8px;
            background: var(--accent-primary);
            border-radius: 50%;
            animation: typingBounce 1.4s infinite;
        }
        .typing-dot:nth-child(2) { animation-delay: 0.2s; }
        .typing-dot:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typingBounce {
            0%, 60%, 100% { transform: translateY(0); }
            30% { transform: translateY(-8px); }
        }
        .welcome-message {
            text-align: center;
            padding: 3rem;
            max-width: 600px;
            margin: auto;
        }
        .welcome-icon { font-size: 4rem; margin-bottom: 1.5rem; }
        .welcome-title {
            font-size: 1.75rem;
            font-weight: 700;
            margin-bottom: 1rem;
            background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .welcome-text {
            color: var(--text-secondary);
            line-height: 1.6;
            margin-bottom: 2rem;
        }
        .suggestion-chips {
            display: flex;
            flex-wrap: wrap;
            gap: 0.75rem;
            justify-content: center;
        }
        .suggestion-chip {
            padding: 0.5rem 1rem;
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: 20px;
            color: var(--text-primary);
            font-size: 0.875rem;
            cursor: pointer;
            transition: all 0.2s;
        }
        .suggestion-chip:hover {
            border-color: var(--accent-primary);
            background: rgba(124, 58, 237, 0.1);
        }
        @media (max-width: 1024px) { .sidebar { display: none; } }
        @media (max-width: 768px) {
            .header { padding: 1rem; }
            .chat-messages { padding: 1rem; }
            .input-container { padding: 1rem; }
            .message { max-width: 95%; }
        }
        ::-webkit-scrollbar { width: 8px; }
        ::-webkit-scrollbar-track { background: var(--bg-secondary); }
        ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }
    </style>
</head>
<body>
    <header class="header">
        <div class="logo">
            <div class="logo-icon">🧠</div>
            <div>
                <div class="logo-text">Togai</div>
                <div class="logo-subtitle">Meta-Cognitive AI Assistant</div>
                <div class="architecture-badge">AIML-LLM Hybrid Architecture</div>
            </div>
        </div>
        <div class="header-actions">
            <button class="btn btn-secondary" onclick="resetChat()">🔄 Reset</button>
            <button class="btn btn-secondary" onclick="toggleSidebar()">📊 Status</button>
        </div>
    </header>
    <div class="main-container">
        <aside class="sidebar" id="sidebar">
            <div class="status-card">
                <div class="status-card-header">🧠 Cognitive Status</div>
                <div class="status-item">
                    <span class="status-label">Meta-Cognitive Layer</span>
                    <span class="layer-badge layer-0" id="layer-badge">Layer 0</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Layer Name</span>
                    <span class="status-value" id="layer-name">Base Processing</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Frame ID</span>
                    <span class="status-value" id="frame-id">-</span>
                </div>
            </div>
            <div class="status-card">
                <div class="status-card-header">⚙️ Pipeline</div>
                <div class="status-item">
                    <span class="status-label">Frame Resolution</span>
                    <span class="status-value" id="pipeline-frame">AIML</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Content Generation</span>
                    <span class="status-value" id="pipeline-gen">-</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Assembly</span>
                    <span class="status-value" id="pipeline-asm">Template</span>
                </div>
            </div>
            <div class="status-card">
                <div class="status-card-header">❤️ Emotional State</div>
                <div class="status-item">
                    <span class="status-label">Primary Emotion</span>
                    <div class="emotion-indicator">
                        <span class="emotion-dot emotion-neutral" id="emotion-dot"></span>
                        <span class="status-value" id="emotion-name">Neutral</span>
                    </div>
                </div>
                <div class="status-item">
                    <span class="status-label">Valence</span>
                    <span class="status-value" id="valence">0.00</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Arousal</span>
                    <span class="status-value" id="arousal">0.30</span>
                </div>
            </div>
            <div class="status-card">
                <div class="status-card-header">📊 Performance</div>
                <div class="status-item">
                    <span class="status-label">Pattern Success</span>
                    <span class="status-value" id="pattern-success">85%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" id="pattern-progress" style="width: 85%"></div>
                </div>
                <div class="status-item">
                    <span class="status-label">Cognitive Load</span>
                    <span class="status-value" id="cognitive-load">60%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" id="load-progress" style="width: 60%"></div>
                </div>
            </div>
            <div class="status-card">
                <div class="status-card-header">💾 Memory</div>
                <div class="status-item">
                    <span class="status-label">Interactions</span>
                    <span class="status-value" id="interactions">0</span>
                </div>
                <div class="status-item">
                    <span class="status-label">Knowledge Entries</span>
                    <span class="status-value" id="knowledge-count">0</span>
                </div>
            </div>
        </aside>
        <main class="chat-container">
            <div class="chat-messages" id="chat-messages">
                <div class="welcome-message" id="welcome">
                    <div class="welcome-icon">🧠</div>
                    <h1 class="welcome-title">Welcome to Togai</h1>
                    <p class="welcome-text">
                        I'm a meta-cognitive AI assistant powered by a hybrid AIML-LLM architecture. 
                        AIML handles frame resolution and structure, while LLM generates unique content 
                        within those frames. I can think about my own thinking, recognize my emotional 
                        states, and engage in deep philosophical discussions.
                    </p>
                    <div class="suggestion-chips">
                        <button class="suggestion-chip" onclick="sendSuggestion('Who are you?')">Who are you?</button>
                        <button class="suggestion-chip" onclick="sendSuggestion('How do you think?')">How do you think?</button>
                        <button class="suggestion-chip" onclick="sendSuggestion('What is meta-cognition?')">What is meta-cognition?</button>
                        <button class="suggestion-chip" onclick="sendSuggestion('Tell me about your self-awareness')">Self-awareness</button>
                        <button class="suggestion-chip" onclick="sendSuggestion('What is consciousness?')">Consciousness</button>
                    </div>
                </div>
            </div>
            <div class="typing-indicator" id="typing">
                <div class="typing-dots">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
                <span>Togai is thinking...</span>
            </div>
            <div class="input-container">
                <div class="input-wrapper">
                    <textarea 
                        class="input-field" 
                        id="message-input" 
                        placeholder="Ask me about meta-cognition, consciousness, or anything else..."
                        rows="1"
                        onkeydown="handleKeyDown(event)"
                    ></textarea>
                    <button class="send-btn" id="send-btn" onclick="sendMessage()">
                        <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
                    </button>
                </div>
            </div>
        </main>
    </div>
    <script>
        const chatMessages = document.getElementById('chat-messages');
        const messageInput = document.getElementById('message-input');
        const sendBtn = document.getElementById('send-btn');
        const typingIndicator = document.getElementById('typing');
        const welcome = document.getElementById('welcome');
        
        messageInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 200) + 'px';
        });
        
        function handleKeyDown(event) {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendMessage();
            }
        }
        
        function sendSuggestion(text) {
            messageInput.value = text;
            sendMessage();
        }
        
        async function sendMessage() {
            const message = messageInput.value.trim();
            if (!message) return;
            
            if (welcome) welcome.style.display = 'none';
            addMessage(message, 'user');
            messageInput.value = '';
            messageInput.style.height = 'auto';
            
            typingIndicator.classList.add('active');
            sendBtn.disabled = true;
            
            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message })
                });
                const data = await response.json();
                
                typingIndicator.classList.remove('active');
                sendBtn.disabled = false;
                
                addMessage(data.text, 'bot', data);
                updateStatus(data);
            } catch (error) {
                console.error('Error:', error);
                typingIndicator.classList.remove('active');
                sendBtn.disabled = false;
                addMessage('Sorry, I encountered an error. Please try again.', 'bot');
            }
        }
        
        function addMessage(text, type, data = null) {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message message-' + type;
            
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar avatar-' + type;
            avatar.textContent = type === 'user' ? '👤' : '🧠';
            
            const content = document.createElement('div');
            content.className = 'message-content';
            
            const textDiv = document.createElement('div');
            textDiv.className = 'message-text';
            textDiv.innerHTML = formatText(text);
            content.appendChild(textDiv);
            
            if (type === 'bot' && data) {
                const meta = document.createElement('div');
                meta.className = 'message-meta';
                const layerClass = 'layer-' + data.meta_cognitive_layer;
                const pipeline = data.pipeline || {};
                meta.innerHTML = \`
                    <div class="meta-item"><span class="layer-badge \${layerClass}">Layer \${data.meta_cognitive_layer}</span></div>
                    <div class="meta-item">⏱️ \${data.processing_time}ms</div>
                    <div class="meta-item">❤️ \${data.emotional_state?.primary_emotion || 'neutral'}</div>
                    <div class="meta-item"><span class="pipeline-badge">\${pipeline.content_generation || 'fallback'}</span></div>
                \`;
                content.appendChild(meta);
                
                if (data.self_reflection) {
                    const reflection = document.createElement('div');
                    reflection.className = 'self-reflection';
                    reflection.textContent = '💭 ' + data.self_reflection;
                    content.appendChild(reflection);
                }
            }
            
            messageDiv.appendChild(avatar);
            messageDiv.appendChild(content);
            chatMessages.appendChild(messageDiv);
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }
        
        function formatText(text) {
            return text
                .replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>')
                .replace(/\\n/g, '<br>');
        }
        
        function updateStatus(data) {
            const layerBadge = document.getElementById('layer-badge');
            layerBadge.textContent = 'Layer ' + data.meta_cognitive_layer;
            layerBadge.className = 'layer-badge layer-' + data.meta_cognitive_layer;
            
            document.getElementById('layer-name').textContent = data.layer_name;
            document.getElementById('frame-id').textContent = data.frame_id || '-';
            
            const pipeline = data.pipeline || {};
            document.getElementById('pipeline-frame').textContent = pipeline.frame_resolution || 'AIML';
            document.getElementById('pipeline-gen').textContent = pipeline.content_generation || '-';
            document.getElementById('pipeline-asm').textContent = pipeline.assembly || 'Template';
            
            if (data.emotional_state) {
                const emotionDot = document.getElementById('emotion-dot');
                emotionDot.className = 'emotion-dot emotion-' + data.emotional_state.primary_emotion;
                document.getElementById('emotion-name').textContent = 
                    data.emotional_state.primary_emotion.charAt(0).toUpperCase() + 
                    data.emotional_state.primary_emotion.slice(1);
                document.getElementById('valence').textContent = data.emotional_state.valence.toFixed(2);
                document.getElementById('arousal').textContent = data.emotional_state.arousal.toFixed(2);
            }
            
            if (data.self_image) {
                document.getElementById('interactions').textContent = data.self_image.interaction_count;
            }
        }
        
        async function resetChat() {
            try {
                await fetch('/api/reset', { method: 'POST' });
                chatMessages.innerHTML = '';
                if (welcome) {
                    welcome.style.display = 'block';
                    chatMessages.appendChild(welcome);
                }
                document.getElementById('layer-badge').textContent = 'Layer 0';
                document.getElementById('layer-badge').className = 'layer-badge layer-0';
                document.getElementById('layer-name').textContent = 'Base Processing';
                document.getElementById('frame-id').textContent = '-';
                document.getElementById('pipeline-gen').textContent = '-';
                document.getElementById('emotion-name').textContent = 'Neutral';
                document.getElementById('emotion-dot').className = 'emotion-dot emotion-neutral';
                document.getElementById('valence').textContent = '0.00';
                document.getElementById('arousal').textContent = '0.30';
                document.getElementById('interactions').textContent = '0';
                document.getElementById('knowledge-count').textContent = '0';
            } catch (error) {
                console.error('Reset error:', error);
            }
        }
        
        function toggleSidebar() {
            const sidebar = document.getElementById('sidebar');
            sidebar.style.display = sidebar.style.display === 'none' ? 'flex' : 'none';
        }
        
        async function fetchStatus() {
            try {
                const response = await fetch('/api/status');
                const data = await response.json();
                document.getElementById('knowledge-count').textContent = data.knowledge_count || 0;
                document.getElementById('pattern-success').textContent = Math.round((data.pattern_success_rate || 0.85) * 100) + '%';
                document.getElementById('pattern-progress').style.width = ((data.pattern_success_rate || 0.85) * 100) + '%';
                document.getElementById('cognitive-load').textContent = Math.round((data.cognitive_load || 0.6) * 100) + '%';
                document.getElementById('load-progress').style.width = ((data.cognitive_load || 0.6) * 100) + '%';
            } catch (error) {
                console.error('Status fetch error:', error);
            }
        }
        
        fetchStatus();
        setInterval(fetchStatus, 5000);
    </script>
</body>
</html>`;

// ============================================================================
// REQUEST HANDLER
// ============================================================================

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    // Initialize engine with environment
    if (!engine) {
      engine = new HybridEngine(env);
    }

    // CORS headers
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
    };

    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // API routes
    if (path === '/api/chat' && request.method === 'POST') {
      try {
        const body = await request.json();
        const message = body.message || '';
        const result = await engine.process(message);
        return new Response(JSON.stringify(result), {
          headers: { 'Content-Type': 'application/json', ...corsHeaders }
        });
      } catch (error) {
        return new Response(JSON.stringify({ error: error.message }), {
          status: 500,
          headers: { 'Content-Type': 'application/json', ...corsHeaders }
        });
      }
    }

    if (path === '/api/status') {
      const status = engine.getStatus();
      return new Response(JSON.stringify(status), {
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      });
    }

    if (path === '/api/reset' && request.method === 'POST') {
      engine.reset();
      return new Response(JSON.stringify({ status: 'reset' }), {
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      });
    }

    // Serve HTML for all other routes
    return new Response(HTML_TEMPLATE, {
      headers: { 'Content-Type': 'text/html', ...corsHeaders }
    });
  }
};
