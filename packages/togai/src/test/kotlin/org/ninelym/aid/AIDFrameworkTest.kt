/**
 * AIDFrameworkTest.kt - Comprehensive tests for the AI-ID Framework
 */
package org.ninelym.aid

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.ninelym.aid.core.*
import org.ninelym.aid.driver.*
import org.ninelym.aid.hub.*
import org.ninelym.aid.kernel.*
import org.ninelym.aid.persona.*
import org.ninelym.aid.endpoint.*

class AIDFrameworkTest {

    private lateinit var framework: AIDFramework

    @Before
    fun setUp() {
        // Reset singleton for each test
        AIDCore.resetInstance()
        framework = AIDFramework.create {
            autoRegisterDefaultPersonas(false)
            autoRegisterDefaultEndpoints(false)
            debugMode(true)
        }
    }

    @After
    fun tearDown() = runBlocking {
        framework.shutdown()
    }

    // ============= Framework Initialization Tests =============

    @Test
    fun `framework initializes successfully`() = runBlocking {
        val result = framework.initialize()
        assertTrue("Framework should initialize successfully", result is AIDResult.Success)
        assertEquals(FrameworkState.READY, framework.state.value)
    }

    @Test
    fun `framework prevents double initialization`() = runBlocking {
        framework.initialize()
        val result = framework.initialize()
        assertTrue("Double initialization should fail", result is AIDResult.Error)
    }

    @Test
    fun `framework starts in CREATED state`() {
        assertEquals(FrameworkState.CREATED, framework.state.value)
    }

    // ============= Persona Tests =============

    @Test
    fun `register and retrieve persona`() = runBlocking {
        framework.initialize()

        val persona = PersonaFactory.createHelper("test-helper")
        val result = framework.registerPersona(persona)

        assertTrue("Persona registration should succeed", result is AIDResult.Success)

        val personas = framework.getPersonas()
        assertEquals(1, personas.size)
        assertEquals("test-helper", personas[0].name)
    }

    @Test
    fun `create persona with builder`() = runBlocking {
        framework.initialize()

        val result = framework.createPersona("custom-persona") {
            displayName("Custom Persona")
            description("A test persona")
            archetype(PersonaArchetype.CREATIVE)
            coreTrait("curiosity", 0.9f)
        }

        assertTrue("Persona creation should succeed", result is AIDResult.Success)
        val personas = framework.getPersonas()
        assertTrue(personas.any { it.name == "custom-persona" })
    }

    @Test
    fun `set and get active persona`() = runBlocking {
        framework.initialize()

        val persona = PersonaFactory.createHelper("active-test")
        framework.registerPersona(persona)

        val setResult = framework.setActivePersona(persona.id)
        assertTrue("Setting active persona should succeed", setResult is AIDResult.Success)

        val activePersona = framework.getActivePersona()
        assertNotNull(activePersona)
        assertEquals("active-test", activePersona?.name)
    }

    @Test
    fun `set active persona by name`() = runBlocking {
        framework.initialize()

        val persona = PersonaFactory.createCreative("aria-test")
        framework.registerPersona(persona)

        val setResult = framework.setActivePersonaByName("aria-test")
        assertTrue("Setting active persona by name should succeed", setResult is AIDResult.Success)
    }

    // ============= Endpoint Tests =============

    @Test
    fun `register custom endpoint`() = runBlocking {
        framework.initialize()

        val handler = object : EndpointHandler {
            override suspend fun initialize() = AIDResult.Success(Unit)
            override suspend fun handle(request: EndpointRequest, tensor: AIDTensor): AIDResult<Any> {
                return AIDResult.Success(mapOf("handled" to true))
            }
            override fun getHealth() = EndpointHealth(true, EndpointState.READY)
            override suspend fun shutdown() {}
        }

        val result = framework.registerEndpoint(
            name = "custom-endpoint",
            description = "A test endpoint",
            capabilities = setOf(ServiceCapability.TEXT_INPUT, ServiceCapability.TEXT_OUTPUT),
            handler = handler
        )

        assertTrue("Endpoint registration should succeed", result is AIDResult.Success)
        val endpoints = framework.getEndpoints()
        assertTrue(endpoints.any { it.name == "custom-endpoint" })
    }

    @Test
    fun `invoke endpoint`() = runBlocking {
        framework.initialize()

        // Register a test endpoint
        val handler = object : EndpointHandler {
            override suspend fun initialize() = AIDResult.Success(Unit)
            override suspend fun handle(request: EndpointRequest, tensor: AIDTensor): AIDResult<Any> {
                return AIDResult.Success(mapOf(
                    "action" to request.action,
                    "handled" to true
                ))
            }
            override fun getHealth() = EndpointHealth(true, EndpointState.READY)
            override suspend fun shutdown() {}
        }

        framework.registerEndpoint(
            name = "invoke-test",
            description = "Test endpoint",
            capabilities = setOf(ServiceCapability.INFERENCE),
            handler = handler
        )

        val result = framework.invokeEndpoint(
            endpointName = "invoke-test",
            action = "test-action",
            parameters = mapOf("param1" to "value1")
        )

        assertTrue("Endpoint invocation should succeed", result is AIDResult.Success)
        val data = (result as AIDResult.Success).data as Map<*, *>
        assertEquals("test-action", data["action"])
        assertEquals(true, data["handled"])
    }

    // ============= Processing Tests =============

    @Test
    fun `process input through framework`() = runBlocking {
        framework.initialize()

        // Register and activate a persona
        val persona = PersonaFactory.createHelper("processor")
        framework.registerPersona(persona)
        framework.setActivePersona(persona.id)

        val result = framework.process("Hello, how can you help me?")

        assertTrue("Processing should succeed", result is AIDResult.Success)
        val output = (result as AIDResult.Success).data
        assertEquals("Hello, how can you help me?", output.input)
        assertNotNull(output.tensor)
    }

    @Test
    fun `process input with context`() = runBlocking {
        framework.initialize()

        val persona = PersonaFactory.createAnalyst("analyst")
        framework.registerPersona(persona)
        framework.setActivePersona(persona.id)

        val result = framework.process(
            input = "Analyze this data",
            context = mapOf("dataType" to "numerical", "size" to 1000)
        )

        assertTrue("Processing with context should succeed", result is AIDResult.Success)
    }

    // ============= Introspection Tests =============

    @Test
    fun `introspect framework`() = runBlocking {
        framework.initialize()

        val result = framework.introspect()

        assertTrue("Introspection should succeed", result is AIDResult.Success)
        val report = (result as AIDResult.Success).data
        assertNotNull(report.identityState)
        assertNotNull(report.selfAssessment)
    }

    @Test
    fun `get identity state`() = runBlocking {
        framework.initialize()

        val state = framework.getIdentityState()

        assertNotNull(state)
        assertTrue(state!!.coherenceScore > 0)
    }

    @Test
    fun `get current tensor`() = runBlocking {
        framework.initialize()

        val tensor = framework.getCurrentTensor()

        assertNotNull(tensor)
        assertTrue(tensor!!.ethicalConstraint >= 0.9f)
    }

    // ============= Statistics Tests =============

    @Test
    fun `get framework statistics`() = runBlocking {
        framework.initialize()

        val persona = PersonaFactory.createHelper("stats-test")
        framework.registerPersona(persona)

        val stats = framework.getStatistics()

        assertEquals(FrameworkState.READY, stats.frameworkState)
        assertEquals(1, stats.personaCount)
    }

    // ============= Quick Setup Tests =============

    @Test
    fun `quick setup with default persona`() = runBlocking {
        val quickFramework = AIDFramework.create {
            autoRegisterDefaultPersonas(true)
            autoRegisterDefaultEndpoints(false)
        }

        val result = quickFramework.quickSetup("layla")

        assertTrue("Quick setup should succeed", result is AIDResult.Success)
        assertEquals("layla", quickFramework.getActivePersona()?.name)

        quickFramework.shutdown()
    }
}

// ============= AIDCore Tests =============

class AIDCoreTest {

    @Before
    fun setUp() {
        AIDCore.resetInstance()
    }

    @Test
    fun `AIDCore singleton pattern`() {
        val core1 = AIDCore.getInstance()
        val core2 = AIDCore.getInstance()
        assertSame(core1, core2)
    }

    @Test
    fun `AIDCore state transitions`() = runBlocking {
        val core = AIDCore.getInstance()
        assertEquals(AIDState.UNINITIALIZED, core.state.value)
    }
}

// ============= AIDTensor Tests =============

class AIDTensorTest {

    @Test
    fun `tensor creation with defaults`() {
        val tensor = AIDTensor.neutral()

        assertEquals(0.5f, tensor.modality, 0.01f)
        assertEquals(0.5f, tensor.depth, 0.01f)
        assertEquals(0.5f, tensor.context, 0.01f)
        assertEquals(0.5f, tensor.salience, 0.01f)
        assertEquals(0.5f, tensor.autonomyIndex, 0.01f)
        assertEquals(0.5f, tensor.identity, 0.01f)
        assertEquals(0.5f, tensor.emotionalValence, 0.01f)
        assertEquals(0.5f, tensor.creativityFactor, 0.01f)
        assertEquals(1.0f, tensor.ethicalConstraint, 0.01f)
    }

    @Test
    fun `tensor blending`() {
        val tensor1 = AIDTensor(modality = 0.0f, salience = 0.0f)
        val tensor2 = AIDTensor(modality = 1.0f, salience = 1.0f)

        val blended = tensor1.blend(tensor2, 0.5f)

        assertEquals(0.5f, blended.modality, 0.01f)
        assertEquals(0.5f, blended.salience, 0.01f)
    }

    @Test
    fun `tensor ethical constraint preserved in blend`() {
        val tensor1 = AIDTensor(ethicalConstraint = 0.9f)
        val tensor2 = AIDTensor(ethicalConstraint = 1.0f)

        val blended = tensor1.blend(tensor2, 0.5f)

        assertEquals(1.0f, blended.ethicalConstraint, 0.01f)
    }

    @Test
    fun `tensor to and from float array`() {
        val original = AIDTensor(
            modality = 0.1f,
            depth = 0.2f,
            context = 0.3f,
            salience = 0.4f,
            autonomyIndex = 0.5f,
            identity = 0.6f,
            emotionalValence = 0.7f,
            creativityFactor = 0.8f,
            ethicalConstraint = 0.9f
        )

        val array = original.toFloatArray()
        val restored = AIDTensor.fromFloatArray(array)

        assertEquals(original.modality, restored.modality, 0.01f)
        assertEquals(original.depth, restored.depth, 0.01f)
        assertEquals(original.context, restored.context, 0.01f)
        assertEquals(original.salience, restored.salience, 0.01f)
        assertEquals(original.ethicalConstraint, restored.ethicalConstraint, 0.01f)
    }

    @Test
    fun `high salience tensor creation`() {
        val tensor = AIDTensor.highSalience()

        assertEquals(0.9f, tensor.salience, 0.01f)
        assertEquals(0.8f, tensor.context, 0.01f)
    }
}

// ============= PersonaFactory Tests =============

class PersonaFactoryTest {

    @Test
    fun `create helper persona`() {
        val persona = PersonaFactory.createHelper("helper-test")

        assertEquals("helper-test", persona.name)
        assertNotNull(persona.getTraitValue("helpfulness"))
        assertTrue(persona.getTraitValue("helpfulness")!! > 0.8f)
    }

    @Test
    fun `create creative persona`() {
        val persona = PersonaFactory.createCreative("creative-test")

        assertNotNull(persona.getTraitValue("creativity"))
        assertTrue(persona.getTraitValue("creativity")!! > 0.9f)
    }

    @Test
    fun `create analyst persona`() {
        val persona = PersonaFactory.createAnalyst("analyst-test")

        assertNotNull(persona.getTraitValue("logic"))
        assertTrue(persona.getTraitValue("logic")!! > 0.9f)
    }

    @Test
    fun `create chaotic persona`() {
        val persona = PersonaFactory.createChaotic("chaotic-test", chaosLevel = 0.9f)

        assertNotNull(persona.getTraitValue("unpredictability"))
        assertTrue(persona.getTraitValue("unpredictability")!! > 0.8f)

        // Ethical constraints should still be maximum
        val ethics = persona.getTraitsByCategory(TraitCategory.ETHICAL)
        assertTrue(ethics.all { it.value >= 0.95f })
    }

    @Test
    fun `persona builder pattern`() {
        val persona = PersonaFactory.create("builder-test") {
            displayName("Builder Test")
            description("Testing the builder")
            coreTrait("testTrait", 0.75f)
            emotionalTrait("happiness", 0.9f)
            speechPattern(
                name = "test-pattern",
                prefixes = listOf("Hello!", "Hi there!"),
                probability = 0.6f
            )
        }

        assertEquals("builder-test", persona.name)
        assertEquals("Builder Test", persona.displayName)
        assertEquals(0.75f, persona.getTraitValue("testTrait")!!, 0.01f)
        assertEquals(0.9f, persona.getTraitValue("happiness")!!, 0.01f)
        assertEquals(1, persona.speechPatterns.size)
    }

    @Test
    fun `ethical baseline always present`() {
        val persona = PersonaFactory.create("ethics-test") {
            coreTrait("helpfulness", 0.5f)
        }

        // Ethical traits should be automatically added
        assertNotNull(persona.getTraitValue("noHarm"))
        assertEquals(1.0f, persona.getTraitValue("noHarm")!!, 0.01f)
    }

    @Test
    fun `clone persona with modifications`() {
        val original = PersonaFactory.createHelper("original")
        val cloned = PersonaFactory.clone(original, "cloned") {
            coreTrait("helpfulness", 0.5f) // Override
        }

        assertEquals("cloned", cloned.name)
        assertEquals(0.5f, cloned.getTraitValue("helpfulness")!!, 0.01f)
    }

    @Test
    fun `persona to tensor conversion`() {
        val persona = PersonaFactory.createCreative("tensor-test")
        val tensor = persona.toTensor()

        // Creative personas should have high creativity factor
        assertTrue(tensor.creativityFactor > 0.7f)
        assertEquals(1.0f, tensor.identity, 0.01f)
    }
}

// ============= PersonaDriver Tests =============

class PersonaDriverTest {

    @Test
    fun `base driver initialization`() = runBlocking {
        val spec = PersonaFactory.createHelper("driver-test")
        val driver = BasePersonaDriver(spec)

        val result = driver.initialize()
        assertTrue(result is AIDResult.Success)
    }

    @Test
    fun `driver modulates tensor`() {
        val spec = PersonaFactory.createCreative("modulate-test")
        val driver = BasePersonaDriver(spec)

        val inputTensor = AIDTensor.neutral()
        val modulated = driver.modulate(inputTensor)

        // Should blend with persona characteristics
        assertNotEquals(inputTensor.creativityFactor, modulated.creativityFactor)
        assertEquals(1.0f, modulated.ethicalConstraint, 0.01f)
    }

    @Test
    fun `driver processes input`() = runBlocking {
        val spec = PersonaFactory.createHelper("process-test")
        val driver = BasePersonaDriver(spec)
        driver.initialize()

        val result = driver.process(
            input = "Hello!",
            inputTensor = AIDTensor.neutral()
        )

        assertTrue(result is AIDResult.Success)
        val output = (result as AIDResult.Success).data
        assertNotNull(output.content)
        assertNotNull(output.tensor)
    }

    @Test
    fun `driver applies emotional stimulus`() = runBlocking {
        val spec = PersonaFactory.createHelper("emotion-test")
        val driver = BasePersonaDriver(spec)
        driver.initialize()

        val initialState = driver.state.emotionalState
        val newState = driver.applyStimulus("happy and excited", 0.8f)

        assertTrue(newState.valence > initialState.valence)
    }

    @Test
    fun `driver capabilities`() {
        val spec = PersonaFactory.createHelper("caps-test")
        val driver = BasePersonaDriver(spec)

        val caps = driver.getCapabilities()

        assertTrue(caps.contains(DriverCapability.TEXT_PROCESSING))
        assertTrue(caps.contains(DriverCapability.EMOTIONAL_MODULATION))
    }
}

// ============= IntegrationHub Tests =============

class IntegrationHubTest {

    @Test
    fun `hub registers endpoint`() = runBlocking {
        val hub = IntegrationHub()
        val core = AIDCore.getInstance()

        hub.initialize(core)

        val handler = object : EndpointHandler {
            override suspend fun initialize() = AIDResult.Success(Unit)
            override suspend fun handle(request: EndpointRequest, tensor: AIDTensor) =
                AIDResult.Success(mapOf("test" to true))
            override fun getHealth() = EndpointHealth(true, EndpointState.READY)
            override suspend fun shutdown() {}
        }

        val result = hub.registerEndpoint(
            name = "test-endpoint",
            description = "Test",
            capabilities = setOf(ServiceCapability.INFERENCE),
            handler = handler
        )

        assertTrue(result is AIDResult.Success)
        assertEquals(1, hub.getEndpoints().size)
    }

    @Test
    fun `hub finds endpoints by capability`() = runBlocking {
        val hub = IntegrationHub()
        val core = AIDCore.getInstance()
        hub.initialize(core)

        val handler = object : EndpointHandler {
            override suspend fun initialize() = AIDResult.Success(Unit)
            override suspend fun handle(request: EndpointRequest, tensor: AIDTensor) =
                AIDResult.Success(Unit)
            override fun getHealth() = EndpointHealth(true, EndpointState.READY)
            override suspend fun shutdown() {}
        }

        hub.registerEndpoint(
            name = "inference-endpoint",
            description = "Inference",
            capabilities = setOf(ServiceCapability.INFERENCE, ServiceCapability.TEXT_OUTPUT),
            handler = handler
        )

        hub.registerEndpoint(
            name = "voice-endpoint",
            description = "Voice",
            capabilities = setOf(ServiceCapability.VOICE_INPUT, ServiceCapability.VOICE_OUTPUT),
            handler = handler
        )

        val inferenceEndpoints = hub.getEndpointsByCapability(ServiceCapability.INFERENCE)
        val voiceEndpoints = hub.getEndpointsByCapability(ServiceCapability.VOICE_INPUT)

        assertEquals(1, inferenceEndpoints.size)
        assertEquals("inference-endpoint", inferenceEndpoints[0].name)
        assertEquals(1, voiceEndpoints.size)
    }

    @Test
    fun `hub statistics`() = runBlocking {
        val hub = IntegrationHub()
        val core = AIDCore.getInstance()
        hub.initialize(core)

        val stats = hub.getStatistics()

        assertNotNull(stats)
        assertEquals(0, stats.totalEndpoints)
    }
}

// ============= SelfKernel Tests =============

class SelfKernelTest {

    @Test
    fun `kernel identity state`() = runBlocking {
        val kernel = SelfKernel()
        val core = AIDCore.getInstance()
        kernel.initialize(core)

        val state = kernel.getIdentityState()

        assertNotNull(state)
        assertEquals(1.0f, state.coherenceScore, 0.01f)
        assertTrue(state.ethicalBaseline.noActualHarm >= 0.95f)
    }

    @Test
    fun `kernel memory stats`() = runBlocking {
        val kernel = SelfKernel()
        val core = AIDCore.getInstance()
        kernel.initialize(core)

        val stats = kernel.getMemoryStats()

        assertNotNull(stats)
        assertEquals(0, stats.totalEntries)
    }

    @Test
    fun `kernel modulates tensor`() = runBlocking {
        val kernel = SelfKernel()
        val core = AIDCore.getInstance()
        kernel.initialize(core)

        val inputTensor = AIDTensor(ethicalConstraint = 0.5f)
        val modulated = kernel.modulate(inputTensor)

        // Ethical constraint should be enforced to baseline minimum
        assertTrue(modulated.ethicalConstraint >= 0.9f)
    }

    @Test
    fun `ethical baseline validation`() {
        val baseline = EthicalBaseline()

        assertTrue(baseline.validate("harmless_action"))
        assertTrue(baseline.minConstraint() >= 0.95f)
    }
}

// ============= EmotionalState Tests =============

class EmotionalStateTest {

    @Test
    fun `emotional state defaults`() {
        val state = EmotionalState()

        assertEquals(0.5f, state.valence, 0.01f)
        assertEquals(0.5f, state.arousal, 0.01f)
        assertEquals("neutral", state.mood)
    }

    @Test
    fun `emotional state blending`() {
        val calm = EmotionalState(valence = 0.3f, arousal = 0.2f)
        val excited = EmotionalState(valence = 0.9f, arousal = 0.9f)

        val blended = calm.blend(excited, 0.5f)

        assertTrue(blended.valence > calm.valence)
        assertTrue(blended.valence < excited.valence)
    }

    @Test
    fun `emotional state decay`() {
        val excited = EmotionalState(valence = 0.9f, arousal = 0.9f, intensity = 0.8f)

        val decayed = excited.decay(0.2f)

        assertTrue(decayed.valence < excited.valence)
        assertTrue(decayed.intensity < excited.intensity)
    }
}

// ============= Endpoint Adapter Tests =============

class ServiceEndpointTest {

    @Test
    fun `inference endpoint handles generate`() = runBlocking {
        val endpoint = InferenceEndpoint()
        endpoint.initialize()

        val request = EndpointRequest(
            action = "generate",
            payload = null,
            parameters = mapOf(
                "prompt" to "Hello world",
                "maxTokens" to 100
            )
        )

        val result = endpoint.handle(request, AIDTensor.neutral())

        assertTrue(result is AIDResult.Success)
    }

    @Test
    fun `voice endpoint handles synthesize`() = runBlocking {
        val endpoint = VoiceEndpoint()
        endpoint.initialize()

        val request = EndpointRequest(
            action = "synthesize",
            payload = null,
            parameters = mapOf("text" to "Hello")
        )

        val result = endpoint.handle(request, AIDTensor.neutral())

        assertTrue(result is AIDResult.Success)
    }

    @Test
    fun `endpoint factory creates all endpoints`() {
        val endpoints = EndpointFactory.createAllEndpoints()

        assertTrue(endpoints.size >= 9)
        assertTrue(endpoints.any { it.endpointName == "inference" })
        assertTrue(endpoints.any { it.endpointName == "voice" })
        assertTrue(endpoints.any { it.endpointName == "image" })
    }

    @Test
    fun `endpoint health check`() = runBlocking {
        val endpoint = InferenceEndpoint()
        endpoint.initialize()

        val health = endpoint.getHealth()

        assertTrue(health.healthy)
        assertEquals(EndpointState.READY, health.state)
    }
}
