package org.ninelym.togai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ninelym.evolution.MultimodalRequest
import org.ninelym.optimization.MemoryState

/**
 * Main Activity for Togai Application
 * 
 * Displays system status, memory usage, and AI capabilities
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var app: TogaiApplication
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        app = TogaiApplication.getInstance(application)
        
        setContent {
            TogaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(app)
                }
            }
        }
    }
}

@Composable
fun TogaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MaterialTheme.colorScheme.primary,
            secondary = MaterialTheme.colorScheme.secondary,
            background = MaterialTheme.colorScheme.background
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: TogaiApplication) {
    var memoryState by remember { mutableStateOf(MemoryState.OPTIMAL) }
    var memoryStats by remember { mutableStateOf("Loading...") }
    var libraryStats by remember { mutableStateOf("Loading...") }
    var aiCapabilities by remember { mutableStateOf("Initializing...") }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Collect memory state
    LaunchedEffect(Unit) {
        app.memoryOptimizer.memoryState.collectLatest { state ->
            memoryState = state
        }
    }
    
    // Collect memory statistics
    LaunchedEffect(Unit) {
        app.memoryOptimizer.memoryStats.collectLatest { stats ->
            memoryStats = buildString {
                appendLine("Available: ${stats.availableMemoryMB} MB")
                appendLine("Used: ${stats.usedMemoryMB} MB")
                appendLine("Total: ${stats.totalMemoryMB} MB")
                appendLine("Native Heap: ${stats.nativeHeapMB} MB")
                appendLine("Dalvik Heap: ${stats.dalvikHeapMB} MB")
                appendLine("Usage: ${stats.usagePercentage.toInt()}%")
            }
        }
    }
    
    // Get library statistics
    LaunchedEffect(Unit) {
        val stats = org.ninelym.native.NativeLibraryLoader.getStatistics()
        libraryStats = buildString {
            appendLine("Total Loaded: ${stats.totalLoaded}")
            appendLine("Core: ${stats.coreLoaded}")
            appendLine("AI/ML: ${stats.aiLoaded}")
            appendLine("Speech: ${stats.speechLoaded}")
            appendLine("React Native: ${stats.reactNativeLoaded}")
        }
    }
    
    // Get AI capabilities
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000) // Wait for initialization
        try {
            val capabilities = app.multimodalOrchestrator.getCapabilities()
            aiCapabilities = buildString {
                appendLine("Language: ${if (capabilities.languageProcessing) "✓" else "✗"}")
                appendLine("Image Gen: ${if (capabilities.imageGeneration) "✓" else "✗"}")
                appendLine("Speech: ${if (capabilities.speechProcessing) "✓" else "✗"}")
                appendLine("Quantum: ${if (capabilities.quantumProcessing) "✓" else "✗"}")
                appendLine("Multimodal: ${if (capabilities.multimodalFusion) "✓" else "✗"}")
            }
        } catch (e: Exception) {
            aiCapabilities = "Error: ${e.message}"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Togai - 9mly AI Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Status Card
            StatusCard(
                title = "Memory Status",
                status = memoryState.name,
                statusColor = when (memoryState) {
                    MemoryState.OPTIMAL -> MaterialTheme.colorScheme.primary
                    MemoryState.MODERATE -> MaterialTheme.colorScheme.secondary
                    MemoryState.WARNING -> MaterialTheme.colorScheme.tertiary
                    MemoryState.CRITICAL -> MaterialTheme.colorScheme.error
                },
                content = memoryStats
            )
            
            // Native Libraries Card
            StatusCard(
                title = "Native Libraries",
                status = "Loaded",
                statusColor = MaterialTheme.colorScheme.primary,
                content = libraryStats
            )
            
            // AI Capabilities Card
            StatusCard(
                title = "AI Capabilities",
                status = "Active",
                statusColor = MaterialTheme.colorScheme.primary,
                content = aiCapabilities
            )
            
            // Test Button
            Button(
                onClick = {
                    isProcessing = true
                    // Launch test inference
                    kotlinx.coroutines.GlobalScope.launch {
                        try {
                            val request = MultimodalRequest(
                                textInput = "Hello, Togai!",
                                quantumProcessing = true
                            )
                            val response = app.multimodalOrchestrator.processMultimodal(request)
                            android.util.Log.i("MainActivity", "Test result: ${response.success}")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Test failed", e)
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text(if (isProcessing) "Processing..." else "Test AI System")
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    status: String,
    statusColor: androidx.compose.ui.graphics.Color,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }
            }
            
            Divider()
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
