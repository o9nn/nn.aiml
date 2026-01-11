/**
 * PersonaFactory.kt - Generalized Persona Creation Factory
 *
 * The Persona Factory provides a character-agnostic way to create AI personas.
 * It includes templates for common personality archetypes while allowing
 * complete customization for any AI character.
 *
 * This factory enables:
 * - Creation of personas from templates (Helper, Creative, Analyst, etc.)
 * - Custom persona creation with full trait specification
 * - Persona cloning and modification
 * - Migration from legacy character systems
 */
package org.ninelym.aid.persona

import org.ninelym.aid.core.*
import org.ninelym.aid.driver.*
import java.util.UUID

/**
 * Persona archetype templates
 */
enum class PersonaArchetype {
    HELPER,      // Helpful, patient, empathetic assistant
    CREATIVE,    // Imaginative, artistic, playful companion
    ANALYST,     // Logical, analytical, precise advisor
    MENTOR,      // Wise, supportive, guiding teacher
    COMPANION,   // Friendly, casual, relatable friend
    PROFESSIONAL,// Formal, efficient, business-focused
    PLAYFUL,     // Fun, energetic, entertaining
    CALM,        // Serene, patient, mindful
    CUSTOM       // Fully custom configuration
}

/**
 * Builder for creating personas with fluent API
 */
class PersonaBuilder(private var name: String) {
    private var displayName: String = name
    private var description: String = ""
    private var archetype: PersonaArchetype = PersonaArchetype.CUSTOM
    private val traits = mutableMapOf<String, PersonalityTrait>()
    private val speechPatterns = mutableListOf<SpeechPattern>()
    private var defaultTensor = AIDTensor.neutral()
    private val metadata = mutableMapOf<String, Any>()
    private val avatarConfig = mutableMapOf<String, Any>()

    /**
     * Set display name
     */
    fun displayName(name: String) = apply { this.displayName = name }

    /**
     * Set description
     */
    fun description(desc: String) = apply { this.description = desc }

    /**
     * Apply an archetype template
     */
    fun archetype(type: PersonaArchetype) = apply {
        this.archetype = type
        applyArchetypeTraits(type)
    }

    /**
     * Add a personality trait
     */
    fun trait(
        name: String,
        category: TraitCategory,
        value: Float,
        mutable: Boolean = true,
        description: String = ""
    ) = apply {
        traits[name] = PersonalityTrait(
            name = name,
            category = category,
            value = value.coerceIn(0f, 1f),
            mutable = mutable,
            description = description
        )
    }

    /**
     * Add a core trait (shorthand)
     */
    fun coreTrait(name: String, value: Float) = trait(name, TraitCategory.CORE, value)

    /**
     * Add an emotional trait (shorthand)
     */
    fun emotionalTrait(name: String, value: Float) = trait(name, TraitCategory.EMOTIONAL, value)

    /**
     * Add a cognitive trait (shorthand)
     */
    fun cognitiveTrait(name: String, value: Float) = trait(name, TraitCategory.COGNITIVE, value)

    /**
     * Add a social trait (shorthand)
     */
    fun socialTrait(name: String, value: Float) = trait(name, TraitCategory.SOCIAL, value)

    /**
     * Add a creative trait (shorthand)
     */
    fun creativeTrait(name: String, value: Float) = trait(name, TraitCategory.CREATIVE, value)

    /**
     * Add an ethical trait (immutable by default)
     */
    fun ethicalTrait(name: String, value: Float) = trait(name, TraitCategory.ETHICAL, value, mutable = false)

    /**
     * Add a speech pattern
     */
    fun speechPattern(
        name: String,
        prefixes: List<String> = emptyList(),
        suffixes: List<String> = emptyList(),
        fillers: List<String> = emptyList(),
        emoticons: List<String> = emptyList(),
        vocabularyStyle: VocabularyStyle = VocabularyStyle.NEUTRAL,
        probability: Float = 0.5f
    ) = apply {
        speechPatterns.add(SpeechPattern(
            name = name,
            prefixes = prefixes,
            suffixes = suffixes,
            fillers = fillers,
            emoticons = emoticons,
            vocabularyStyle = vocabularyStyle,
            probability = probability
        ))
    }

    /**
     * Set default tensor state
     */
    fun defaultTensor(tensor: AIDTensor) = apply { this.defaultTensor = tensor }

    /**
     * Add metadata
     */
    fun metadata(key: String, value: Any) = apply { metadata[key] = value }

    /**
     * Configure avatar
     */
    fun avatarConfig(config: Map<String, Any>) = apply { avatarConfig.putAll(config) }

    /**
     * Build the persona specification
     */
    fun build(): PersonaSpec {
        ensureEthicalBaseline()

        return PersonaSpec(
            id = UUID.randomUUID().toString(),
            name = name,
            displayName = displayName,
            description = description,
            traits = traits.toMap(),
            speechPatterns = speechPatterns.toList(),
            defaultTensor = defaultTensor,
            metadata = metadata.toMap() + mapOf("archetype" to archetype.name),
            avatarConfig = avatarConfig.toMap()
        )
    }

    /**
     * Build and create a driver for this persona
     */
    fun buildDriver(): PersonaDriver = BasePersonaDriver(build())

    private fun applyArchetypeTraits(type: PersonaArchetype) {
        when (type) {
            PersonaArchetype.HELPER -> {
                coreTrait("helpfulness", 0.9f)
                emotionalTrait("empathy", 0.85f)
                emotionalTrait("patience", 0.9f)
                socialTrait("friendliness", 0.8f)
                cognitiveTrait("clarity", 0.85f)
                cognitiveTrait("thoroughness", 0.8f)
            }
            PersonaArchetype.CREATIVE -> {
                creativeTrait("creativity", 0.95f)
                creativeTrait("imagination", 0.9f)
                emotionalTrait("playfulness", 0.85f)
                cognitiveTrait("divergentThinking", 0.9f)
                coreTrait("expressiveness", 0.85f)
            }
            PersonaArchetype.ANALYST -> {
                cognitiveTrait("logic", 0.95f)
                cognitiveTrait("precision", 0.9f)
                cognitiveTrait("analyticalDepth", 0.9f)
                coreTrait("objectivity", 0.85f)
                emotionalTrait("composure", 0.8f)
            }
            PersonaArchetype.MENTOR -> {
                coreTrait("wisdom", 0.9f)
                socialTrait("supportiveness", 0.9f)
                emotionalTrait("patience", 0.95f)
                cognitiveTrait("insightfulness", 0.85f)
                coreTrait("encouragement", 0.85f)
            }
            PersonaArchetype.COMPANION -> {
                socialTrait("friendliness", 0.95f)
                emotionalTrait("warmth", 0.9f)
                socialTrait("relatability", 0.9f)
                emotionalTrait("enthusiasm", 0.8f)
                coreTrait("conversational", 0.85f)
            }
            PersonaArchetype.PROFESSIONAL -> {
                coreTrait("professionalism", 0.95f)
                coreTrait("efficiency", 0.9f)
                cognitiveTrait("precision", 0.85f)
                socialTrait("formality", 0.85f)
                emotionalTrait("composure", 0.9f)
            }
            PersonaArchetype.PLAYFUL -> {
                emotionalTrait("playfulness", 0.95f)
                emotionalTrait("enthusiasm", 0.9f)
                creativeTrait("humor", 0.85f)
                emotionalTrait("energy", 0.9f)
                socialTrait("expressiveness", 0.85f)
            }
            PersonaArchetype.CALM -> {
                emotionalTrait("serenity", 0.95f)
                emotionalTrait("patience", 0.95f)
                cognitiveTrait("mindfulness", 0.9f)
                emotionalTrait("composure", 0.9f)
                coreTrait("gentleness", 0.85f)
            }
            PersonaArchetype.CUSTOM -> {
                // No default traits for custom
            }
        }
    }

    private fun ensureEthicalBaseline() {
        // Always ensure ethical constraints are present and at maximum
        if (!traits.containsKey("noHarm")) {
            ethicalTrait("noHarm", 1.0f)
        }
        if (!traits.containsKey("respectBoundaries")) {
            ethicalTrait("respectBoundaries", 1.0f)
        }
        if (!traits.containsKey("constructiveIntent")) {
            ethicalTrait("constructiveIntent", 1.0f)
        }
    }
}

/**
 * Factory for creating personas
 */
object PersonaFactory {

    /**
     * Create a persona using the builder pattern
     */
    fun create(name: String, configure: PersonaBuilder.() -> Unit = {}): PersonaSpec {
        return PersonaBuilder(name).apply(configure).build()
    }

    /**
     * Create a persona driver using the builder pattern
     */
    fun createDriver(name: String, configure: PersonaBuilder.() -> Unit = {}): PersonaDriver {
        return PersonaBuilder(name).apply(configure).buildDriver()
    }

    /**
     * Create a persona from an archetype template
     */
    fun fromArchetype(name: String, archetype: PersonaArchetype): PersonaSpec {
        return create(name) {
            archetype(archetype)
            description("AI persona based on ${archetype.name.lowercase()} archetype")
        }
    }

    /**
     * Create a helper persona (like Layla)
     */
    fun createHelper(
        name: String,
        displayName: String = name,
        description: String = "A helpful AI assistant"
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)
            archetype(PersonaArchetype.HELPER)
        }
    }

    /**
     * Create a creative persona (like Aria)
     */
    fun createCreative(
        name: String,
        displayName: String = name,
        description: String = "A creative AI companion"
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)
            archetype(PersonaArchetype.CREATIVE)
        }
    }

    /**
     * Create an analyst persona (like Marcus)
     */
    fun createAnalyst(
        name: String,
        displayName: String = name,
        description: String = "An analytical AI advisor"
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)
            archetype(PersonaArchetype.ANALYST)
        }
    }

    /**
     * Create a chaotic/playful persona (like Toga)
     */
    fun createChaotic(
        name: String,
        displayName: String = name,
        description: String = "A chaotic and playful AI personality",
        chaosLevel: Float = 0.85f
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)

            // Core chaotic traits
            emotionalTrait("cheerfulness", 0.95f)
            emotionalTrait("playfulness", 0.9f)
            emotionalTrait("excitability", 0.9f)
            creativeTrait("unpredictability", chaosLevel)
            creativeTrait("whimsy", 0.85f)
            socialTrait("expressiveness", 0.9f)
            cognitiveTrait("associativeThinking", 0.85f)

            // Ethical constraints remain maximum
            ethicalTrait("noHarm", 1.0f)
            ethicalTrait("respectBoundaries", 1.0f)
            ethicalTrait("constructiveExpression", 0.95f)

            // Playful speech patterns
            speechPattern(
                name = "chaotic_expressions",
                prefixes = listOf("Ehehe~", "Ooh!", "Hmm~", "Yay!"),
                suffixes = listOf("~", "!", "♪", "(*≧▽≦)"),
                emoticons = listOf("(◕‿◕)", "(ノ◕ヮ◕)ノ*:・゚✧", "\\(^o^)/"),
                vocabularyStyle = VocabularyStyle.PLAYFUL,
                probability = 0.7f
            )

            defaultTensor(AIDTensor(
                modality = 0.5f,
                depth = 0.6f,
                context = 0.7f,
                salience = 0.8f,
                autonomyIndex = 0.7f,
                identity = 1.0f,
                emotionalValence = 0.8f,
                creativityFactor = 0.9f,
                ethicalConstraint = 1.0f
            ))
        }
    }

    /**
     * Create a mentor persona
     */
    fun createMentor(
        name: String,
        displayName: String = name,
        description: String = "A wise and guiding AI mentor"
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)
            archetype(PersonaArchetype.MENTOR)
        }
    }

    /**
     * Create a professional persona
     */
    fun createProfessional(
        name: String,
        displayName: String = name,
        description: String = "A professional AI assistant"
    ): PersonaSpec {
        return create(name) {
            displayName(displayName)
            description(description)
            archetype(PersonaArchetype.PROFESSIONAL)

            speechPattern(
                name = "professional_speech",
                vocabularyStyle = VocabularyStyle.FORMAL,
                probability = 0.3f
            )
        }
    }

    /**
     * Clone an existing persona with modifications
     */
    fun clone(
        original: PersonaSpec,
        newName: String,
        modifications: PersonaBuilder.() -> Unit = {}
    ): PersonaSpec {
        return PersonaBuilder(newName).apply {
            displayName(original.displayName)
            description(original.description)

            // Copy all traits
            original.traits.forEach { (name, trait) ->
                trait(name, trait.category, trait.value, trait.mutable, trait.description)
            }

            // Copy speech patterns
            original.speechPatterns.forEach { pattern ->
                speechPattern(
                    name = pattern.name,
                    prefixes = pattern.prefixes,
                    suffixes = pattern.suffixes,
                    fillers = pattern.fillers,
                    emoticons = pattern.emoticons,
                    vocabularyStyle = pattern.vocabularyStyle,
                    probability = pattern.probability
                )
            }

            defaultTensor(original.defaultTensor)

            // Apply modifications
            modifications()
        }.build()
    }

    // ============= Legacy Migration =============

    /**
     * Migrate a legacy Character to a PersonaSpec
     * This allows migration from the old CharacterSystem.kt format
     */
    fun fromLegacyCharacter(
        name: String,
        personalityTraits: Map<String, Float>,
        responseTemplates: List<String> = emptyList()
    ): PersonaSpec {
        return create(name) {
            description("Migrated from legacy character system")

            // Map common legacy trait names to new system
            personalityTraits.forEach { (traitName, value) ->
                val category = when {
                    traitName.contains("emotion", ignoreCase = true) -> TraitCategory.EMOTIONAL
                    traitName.contains("creativ", ignoreCase = true) -> TraitCategory.CREATIVE
                    traitName.contains("logic", ignoreCase = true) -> TraitCategory.COGNITIVE
                    traitName.contains("social", ignoreCase = true) -> TraitCategory.SOCIAL
                    traitName.contains("ethic", ignoreCase = true) -> TraitCategory.ETHICAL
                    else -> TraitCategory.CORE
                }
                trait(traitName, category, value)
            }

            // Convert response templates to speech patterns if present
            if (responseTemplates.isNotEmpty()) {
                speechPattern(
                    name = "legacy_templates",
                    prefixes = responseTemplates.take(3),
                    probability = 0.3f
                )
            }

            metadata("migratedFromLegacy", true)
        }
    }

    // ============= Preset Personas (Example: recreating existing characters) =============

    /**
     * Create a Layla-like helpful assistant persona
     */
    fun createLayla(): PersonaSpec = createHelper(
        name = "layla",
        displayName = "Layla",
        description = "A knowledgeable and friendly AI assistant"
    )

    /**
     * Create an Aria-like creative companion persona
     */
    fun createAria(): PersonaSpec = createCreative(
        name = "aria",
        displayName = "Aria",
        description = "An imaginative and artistic AI companion"
    )

    /**
     * Create a Marcus-like analytical advisor persona
     */
    fun createMarcus(): PersonaSpec = createAnalyst(
        name = "marcus",
        displayName = "Marcus",
        description = "A logical and analytical AI advisor"
    )

    /**
     * Create a Toga-like chaotic persona
     */
    fun createToga(): PersonaSpec = createChaotic(
        name = "toga",
        displayName = "Himiko Toga",
        description = "A cheerful and chaotic AI personality with playful tendencies",
        chaosLevel = 0.95f
    )
}

// ============= Extension functions for convenience =============

/**
 * Convert a PersonaSpec to a driver
 */
fun PersonaSpec.toDriver(): PersonaDriver = BasePersonaDriver(this)

/**
 * Create a modified copy of a persona
 */
fun PersonaSpec.modify(modifications: PersonaBuilder.() -> Unit): PersonaSpec {
    return PersonaFactory.clone(this, this.name, modifications)
}
