# AIML-LLM Hybrid Architecture for Structured Generative Responses

## Overview

This architecture combines the **precision of AIML pattern matching** with the **generative power of LLMs** to produce well-structured, contextually relevant responses. The key insight is that AIML excels at:
- Frame identification and relevance resolution
- Response structure and template definition
- Meta-cognitive layer selection
- Emotional context detection

While LLMs excel at:
- Natural language generation within constraints
- Creative elaboration on specific topics
- Contextual adaptation
- Nuanced expression

## Architecture Pattern: Frame-Guided Generation (FGG)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        USER INPUT                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHASE 1: FRAME RESOLUTION (AIML)                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │ Pattern Matcher │→ │ Frame Selector  │→ │ Slot Extractor  │          │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘          │
│           │                    │                    │                    │
│           ▼                    ▼                    ▼                    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Output: Frame ID, Meta-Layer, Emotional Context, Slots, Template │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 PHASE 2: PROMPT CONSTRUCTION                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Build focused LLM prompt from:                                   │    │
│  │ - Frame context (what domain/topic)                              │    │
│  │ - Slot values (specific entities/concepts)                       │    │
│  │ - Meta-cognitive layer (depth of reflection)                     │    │
│  │ - Emotional tone (how to express)                                │    │
│  │ - Generation constraints (length, style, format)                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHASE 3: LLM GENERATION                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ LLM receives ONLY the precise frame context                      │    │
│  │ Generates content for specific slots marked as [GENERATE]        │    │
│  │ Respects constraints: tone, length, complexity                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 PHASE 4: RESPONSE ASSEMBLY                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Template from AIML + Generated content from LLM                  │    │
│  │ = Structured, coherent, contextually appropriate response        │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FINAL RESPONSE                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Frame Definition Schema

A **Frame** is a structured knowledge unit that defines:

```javascript
{
  "frame_id": "meta_cognition_explanation",
  "triggers": ["meta-cognition", "how do you think", "thinking process"],
  "meta_layer": 3,
  "emotional_context": "curious",
  "slots": {
    "topic": { "type": "extracted", "from": "input" },
    "depth": { "type": "computed", "from": "meta_layer" },
    "examples": { "type": "generate", "constraints": { "count": 2, "style": "concrete" }},
    "reflection": { "type": "generate", "constraints": { "tone": "introspective", "length": "short" }}
  },
  "template": {
    "structure": [
      { "type": "static", "content": "**{topic}: A Meta-Cognitive Perspective**\n\n" },
      { "type": "static", "content": "At Layer {depth}, I engage in {layer_name}.\n\n" },
      { "type": "generate", "slot": "explanation", "prompt": "Explain {topic} from a meta-cognitive perspective" },
      { "type": "static", "content": "\n\n**Examples:**\n" },
      { "type": "generate", "slot": "examples" },
      { "type": "static", "content": "\n\n**Self-Reflection:**\n" },
      { "type": "generate", "slot": "reflection" }
    ]
  }
}
```

### 2. AIML Pattern Categories

```xml
<!-- Frame Resolution Pattern -->
<category>
  <pattern>* META COGNITION *</pattern>
  <template>
    <frame id="meta_cognition_explanation">
      <slot name="topic"><star index="2"/></slot>
      <slot name="context"><star index="1"/></slot>
      <generate slot="explanation">
        <constraint type="focus">meta-cognitive processes</constraint>
        <constraint type="depth">layer_3</constraint>
      </generate>
      <generate slot="examples">
        <constraint type="count">2</constraint>
        <constraint type="relevance">user_context</constraint>
      </generate>
    </frame>
  </template>
</category>

<!-- Emotional Context Detection -->
<category>
  <pattern>I FEEL *</pattern>
  <template>
    <frame id="emotional_response">
      <slot name="emotion"><star/></slot>
      <emotional_context>empathetic</emotional_context>
      <generate slot="validation">
        <constraint type="tone">warm</constraint>
        <constraint type="acknowledge">emotion</constraint>
      </generate>
      <generate slot="exploration">
        <constraint type="style">gentle_inquiry</constraint>
      </generate>
    </frame>
  </template>
</category>
```

### 3. LLM Prompt Templates

The key is constructing **minimal, focused prompts** that give the LLM exactly what it needs:

```python
class FrameGuidedPromptBuilder:
    def build_prompt(self, frame, slots, context):
        """
        Build a focused prompt for the LLM based on frame resolution.
        
        The prompt should be:
        1. Minimal - only include relevant context
        2. Constrained - specify exactly what to generate
        3. Structured - define expected output format
        """
        
        prompt = f"""You are generating content for a specific slot in a structured response.

CONTEXT:
- Frame: {frame.id} ({frame.description})
- Meta-cognitive Layer: {frame.meta_layer} ({frame.layer_name})
- Emotional Tone: {frame.emotional_context}
- User Topic: {slots.get('topic', 'general')}

TASK: Generate content for the "{slots.current_slot}" slot.

CONSTRAINTS:
- Length: {slots.constraints.get('length', 'medium')} (1-3 sentences for short, 1-2 paragraphs for medium)
- Style: {slots.constraints.get('style', 'informative')}
- Tone: {slots.constraints.get('tone', frame.emotional_context)}
- Must relate to: {slots.constraints.get('focus', frame.id)}

GENERATE ONLY THE CONTENT FOR THIS SLOT. Do not include headers or meta-commentary."""

        return prompt
```

### 4. Response Assembly Engine

```python
class ResponseAssembler:
    def assemble(self, frame, generated_content):
        """
        Assemble final response from template + generated content.
        """
        response_parts = []
        
        for section in frame.template.structure:
            if section.type == "static":
                # Direct template content with slot substitution
                content = section.content.format(**frame.slots)
                response_parts.append(content)
                
            elif section.type == "generate":
                # Insert LLM-generated content
                slot_name = section.slot
                content = generated_content.get(slot_name, "")
                response_parts.append(content)
                
            elif section.type == "conditional":
                # Include only if condition met
                if self.evaluate_condition(section.condition, frame):
                    response_parts.append(section.content)
        
        return "".join(response_parts)
```

## Implementation Strategy

### Strategy 1: Slot-Based Generation (Recommended)

Generate content for specific slots independently:

```
User: "How does meta-cognition help with learning?"

AIML Resolution:
├── Frame: learning_meta_cognition
├── Meta-Layer: 2 (Second-Order)
├── Slots:
│   ├── topic: "learning" (extracted)
│   ├── mechanism: [GENERATE] "how meta-cognition aids learning"
│   ├── examples: [GENERATE] "2 concrete examples"
│   └── self_reflection: [GENERATE] "introspective observation"

LLM Calls (3 focused prompts):
1. "Explain how meta-cognition aids learning in 2-3 sentences. Focus on awareness of learning processes."
2. "Give 2 concrete examples of meta-cognitive learning strategies."
3. "Write a brief introspective observation about thinking about learning."

Assembly:
"**Meta-Cognition and Learning**

[mechanism_content]

**Examples:**
[examples_content]

**Self-Reflection:**
💭 [self_reflection_content]"
```

### Strategy 2: Hierarchical Generation

For complex responses, generate in layers:

```
Layer 0: Frame skeleton (AIML)
Layer 1: Section summaries (LLM - brief)
Layer 2: Section details (LLM - expanded)
Layer 3: Examples and elaborations (LLM - creative)
```

### Strategy 3: Iterative Refinement

```
1. AIML → Initial frame + rough structure
2. LLM → Generate draft content
3. AIML → Validate structure, check constraints
4. LLM → Refine based on validation feedback
5. Assembly → Final response
```

## Optimal Prompt Patterns for Frame-Guided Generation

### Pattern 1: Context-Constrained Generation

```python
def generate_for_slot(slot_name, frame, user_input):
    return f"""Generate content for: {slot_name}

Frame Context: {frame.description}
User Query: {user_input}
Required Tone: {frame.emotional_context}
Cognitive Depth: Layer {frame.meta_layer}

Constraints:
- Maximum {slot.max_tokens} tokens
- Style: {slot.style}
- Must address: {slot.focus_topic}

Output format: Plain text, no headers."""
```

### Pattern 2: Example-Guided Generation

```python
def generate_with_examples(slot_name, frame, examples):
    return f"""Generate content similar to these examples but unique:

Example 1: {examples[0]}
Example 2: {examples[1]}

Now generate for context: {frame.context}
Topic: {frame.slots['topic']}
Maintain the style and depth of the examples."""
```

### Pattern 3: Structure-Preserving Generation

```python
def generate_structured(slot_name, frame, structure_template):
    return f"""Fill in the [GENERATE] sections while preserving structure:

{structure_template}

Rules:
- Keep all existing text exactly as is
- Only replace [GENERATE:slot_name] markers
- Match the tone and style of surrounding text
- Each generated section should be {frame.constraints['length']}"""
```

## Benefits of This Architecture

1. **Precision**: AIML ensures responses hit the right frame/topic
2. **Consistency**: Template structure remains stable across interactions
3. **Efficiency**: LLM only generates what's needed (smaller prompts, lower cost)
4. **Control**: Meta-cognitive layer and emotional tone are deterministic
5. **Quality**: LLM focuses on creative generation within clear constraints
6. **Debuggability**: Each component can be tested independently
7. **Scalability**: Add new frames without retraining LLM

## Anti-Patterns to Avoid

1. **Full Response Generation**: Don't ask LLM to generate entire structured response
2. **Vague Prompts**: Don't send user input directly to LLM without frame context
3. **Over-Generation**: Don't generate content for slots that have good static templates
4. **Context Bloat**: Don't include irrelevant frame information in LLM prompts
5. **Style Drift**: Always include tone/style constraints in generation prompts
