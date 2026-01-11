package org.ninelym.ai.wonderful

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for WonderfulToga
 */
class WonderfulTogaTest {
    
    @Test
    fun testWonderfulTogaCreation() {
        val toga = WonderfulToga(
            userId = "test_user_123",
            userName = "TestUser"
        )
        
        assertNotNull(toga)
    }
    
    @Test
    fun testWonderfulTogaBuilder() {
        val toga = WonderfulTogaBuilder()
            .setUserId("test_user_456")
            .setUserName("BuilderUser")
            .build()
        
        assertNotNull(toga)
    }
    
    @Test
    fun testProcessMessage() {
        val toga = WonderfulToga(
            userId = "test_user_789",
            userName = "MessageUser"
        )
        
        val response = toga.processMessage(
            message = "Hello Toga!",
            context = TaskContext.CASUAL_CHAT
        )
        
        assertNotNull(response)
        assertNotNull(response.message)
        assertTrue(response.message.isNotEmpty())
        assertEquals(TaskContext.CASUAL_CHAT, response.context)
    }
    
    @Test
    fun testGenerateGreeting() {
        val toga = WonderfulToga(
            userId = "test_user_greeting",
            userName = "GreetingUser"
        )
        
        val greeting = toga.generateGreeting()
        
        assertNotNull(greeting)
        assertTrue(greeting.isNotEmpty())
    }
    
    @Test
    fun testEmotionalResponse() {
        val toga = WonderfulToga(
            userId = "test_user_emotion",
            userName = "EmotionUser"
        )
        
        // Test with a sad message
        val response = toga.processMessage(
            message = "I'm feeling really sad today",
            context = TaskContext.EMOTIONAL_SUPPORT
        )
        
        assertNotNull(response)
        assertEquals(UserEmotionalState.SAD, response.userEmotion)
        assertEquals(TaskContext.EMOTIONAL_SUPPORT, response.context)
    }
    
    @Test
    fun testCreativeExpression() {
        val toga = WonderfulToga(
            userId = "test_user_creative",
            userName = "CreativeUser"
        )
        
        val haiku = toga.generateCreativeExpression(CreativeExpressionType.HAIKU)
        assertNotNull(haiku)
        assertTrue(haiku.isNotEmpty())
        
        val art = toga.generateCreativeExpression(CreativeExpressionType.ASCII_ART)
        assertNotNull(art)
        assertTrue(art.isNotEmpty())
    }
    
    @Test
    fun testGetCurrentEmotion() {
        val toga = WonderfulToga(
            userId = "test_user_current",
            userName = "CurrentUser"
        )
        
        val emotion = toga.getCurrentEmotion()
        
        assertNotNull(emotion)
        assertNotNull(emotion.primaryEmotion)
        assertTrue(emotion.primaryIntensity >= 0.0f)
        assertTrue(emotion.primaryIntensity <= 1.0f)
    }
    
    @Test
    fun testGetRelationshipStatus() {
        val toga = WonderfulToga(
            userId = "test_user_relationship",
            userName = "RelationshipUser"
        )
        
        // Generate some interactions
        toga.processMessage("Hello!", TaskContext.CASUAL_CHAT)
        toga.processMessage("How are you?", TaskContext.CASUAL_CHAT)
        
        val status = toga.getRelationshipStatus()
        
        assertNotNull(status)
        assertTrue(status.totalInteractions >= 2)
        assertEquals(RelationshipLevel.FIRST_MEETING, status.level)
    }
    
    @Test
    fun testChatExtensionFunction() {
        val toga = WonderfulToga(
            userId = "test_user_ext",
            userName = "ExtUser"
        )
        
        val message = toga.chat("Hi there!")
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }
    
    @Test
    fun testAskForHelpExtensionFunction() {
        val toga = WonderfulToga(
            userId = "test_user_help",
            userName = "HelpUser"
        )
        
        val message = toga.askForHelp("I need help with this code")
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }
    
    @Test
    fun testCollaborateExtensionFunction() {
        val toga = WonderfulToga(
            userId = "test_user_collab",
            userName = "CollabUser"
        )
        
        val message = toga.collaborate("Let's work on this project together")
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }
    
    @Test
    fun testSeekSupportExtensionFunction() {
        val toga = WonderfulToga(
            userId = "test_user_support",
            userName = "SupportUser"
        )
        
        val message = toga.seekSupport("I'm feeling overwhelmed")
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }
}
