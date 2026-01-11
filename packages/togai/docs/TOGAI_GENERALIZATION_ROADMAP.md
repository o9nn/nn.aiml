# Togai Framework Generalization Roadmap

## Overview

This document outlines the features that need to be implemented in the Togai framework to fully generalize and replace all Layla-specific implementations. The goal is to make Togai a complete, platform-agnostic AI assistant framework.

---

## Feature Gap Analysis

### Legend
- ✅ = Fully Implemented in Togai
- ⚠️ = Partially Implemented
- ❌ = Missing - Needs Implementation

---

## 1. Native JNI Bindings

### Status: ❌ Critical Gap

The Layla implementation relies on several native libraries that need Togai equivalents.

| Feature | Layla Implementation | Togai Status | Priority |
|---------|---------------------|--------------|----------|
| LLaMA.cpp JNI | `com.layla.jni.LlamaCppJni` | ❌ Missing | **P0** |
| Stable Diffusion JNI | `com.layla.jni.SDJni` | ❌ Missing | **P0** |
| CTranslate2 JNI | `com.layla.jni.CTranslate2Jni` | ❌ Missing | **P1** |
| Cubism JNI | `com.layla.cubism.JniBridgeJava` | ❌ Missing | **P2** |
| LVDB JNI | `lvdb-jni` native lib | ❌ Missing | **P1** |

### Required Implementations

#### 1.1 LlamaCppJni (`org.ninelym.jni.LlamaCppJni`)
```kotlin
interface LlamaCppJni {
    fun llama_init(modelPath: String, useMMQ: Boolean)
    fun llama_infer(
        prompt: String,
        systemPrompt: String,
        chatTemplate: String,
        bos: String,
        eos: String,
        inputSuffix: String,
        temperature: Float,
        topP: Float,
        topK: Float,
        minP: Float,
        repeatPenalty: Int,
        maxTokens: Int,
        callback: LlamaCppCallback
    ): String
    fun llama_repl_start(/* 30+ params */)
    fun llama_repl_send_msg(input: ByteArray, length: Int, ...)
    fun llama_describe_img(imagePath: String, prompt: String, model: String): String
    fun isCPUDotProdSupported(): Int
    fun isCPUi8mmSupported(): Int
}
```

#### 1.2 SDJni (`org.ninelym.jni.SDJni`)
```kotlin
interface SDJni {
    interface SDGenImgCallback {
        fun onProgress(step: Int, totalSteps: Int)
        fun onComplete(imagePath: String)
        fun onError(error: String)
    }

    fun generateImage(
        prompt: String,
        negativePrompt: String,
        modelPath: String,
        outputPath: String,
        steps: Int,
        guidanceScale: Float,
        seed: Long,
        callback: SDGenImgCallback
    )
}
```

#### 1.3 CTranslate2Jni (`org.ninelym.jni.CTranslate2Jni`)
```kotlin
interface CTranslate2Jni {
    fun translate_m2m100(modelPath: String, text: String, srcLang: String, tgtLang: String): String
    fun translate_opusmt(modelPath: String, text: String): String
    fun summarise_bart(modelPath: String, text: String): String
    fun summarise_flant5(modelPath: String, text: String): String
    fun classify_xlmroberta(modelPath: String, text: String): String
    fun release_model(modelPath: String)
}
```

---

## 2. Vector Database (LVDB)

### Status: ❌ Missing

Local vector database for embeddings and semantic search.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| Embedding storage | `Lvdb.lvdb_save_embeddings()` | ❌ Missing |
| Similarity search | `Lvdb.lvdb_query_top_n_similar()` | ❌ Missing |
| Metadata queries | `Lvdb.lvdb_delete_embeddings_by_metadata()` | ❌ Missing |
| Persistence | `Lvdb.lvdb_save_to_disk()` | ❌ Missing |

### Required Implementation: `org.ninelym.db.VectorDatabase`

```kotlin
interface VectorDatabase {
    data class Embedding(
        val id: String,
        val vector: FloatArray,
        val metadata: Map<String, String>
    )

    data class SimilarResult(
        val embedding: Embedding,
        val similarity: Float
    )

    suspend fun saveEmbedding(dbPath: String, namespace: String, vector: FloatArray, metadata: String)
    suspend fun queryTopNSimilar(dbPath: String, namespace: String, queryVector: FloatArray, n: Int): List<SimilarResult>
    suspend fun deleteByMetadata(dbPath: String, namespace: String, metadata: String)
    suspend fun exists(dbPath: String, namespace: String, key: String): Boolean
    suspend fun saveToDisk(dbPath: String, namespace: String)
}
```

---

## 3. MLC Chat Integration

### Status: ❌ Missing

Machine Learning Compilation chat framework for optimized inference.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| Model loading | `MLCChat.start()` | ❌ Missing |
| Message generation | `MLCChat.sendMessage()` | ❌ Missing |
| Chat state management | `MLCChat.ChatState` | ❌ Missing |
| Model configs | `ModelConfig`, `ParamsConfig` | ❌ Missing |

### Required Implementation: `org.ninelym.mlc.MLCChatEngine`

```kotlin
interface MLCChatEngine {
    enum class ModelChatState { IDLE, GENERATING, READY, FAILED }
    enum class ModelInitState { UNINITIALIZED, LOADING, READY, FAILED }

    data class ModelConfig(
        val modelLib: String,
        val modelPath: String,
        val vocabPath: String
    )

    data class GenerationParams(
        val temperature: Float = 0.7f,
        val topP: Float = 0.9f,
        val maxTokens: Int = 512
    )

    suspend fun loadModel(config: ModelConfig): Result<Unit>
    suspend fun generate(prompt: String, params: GenerationParams, onToken: (String) -> Unit): Result<String>
    suspend fun reset()
    suspend fun unload()

    val modelState: StateFlow<ModelInitState>
    val chatState: StateFlow<ModelChatState>
}
```

---

## 4. Live2D Cubism Animation

### Status: ❌ Missing

Character animation system using Live2D Cubism SDK.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| GL rendering | `CubismGLSurfaceView`, `GLRenderer` | ❌ Missing |
| Model loading | `JniBridgeJava.LoadFile()` | ❌ Missing |
| View management | `CubismViewManager` | ❌ Missing |
| React Native bridge | `RNCubism` | ❌ Missing |

### Required Implementation: `org.ninelym.animation.CubismEngine`

```kotlin
interface CubismEngine {
    data class ModelAssets(
        val modelPath: String,
        val motionGroups: Map<String, List<String>>,
        val expressions: List<String>
    )

    suspend fun loadModel(assets: ModelAssets): Result<Unit>
    fun playMotion(group: String, index: Int)
    fun setExpression(name: String)
    fun setParameter(name: String, value: Float)
    fun startRandomMotion()
    fun update(deltaTime: Float)
    fun render()
    fun dispose()
}
```

---

## 5. Executorch Integration

### Status: ❌ Missing

Meta's PyTorch runtime for mobile inference.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| Model loading | `Executorch.loadModel()` | ❌ Missing |
| LlamaModule | `org.pytorch.executorch.LlamaModule` | ❌ Missing |
| Inference | `LlamaCallback` interface | ❌ Missing |

### Required Implementation: `org.ninelym.executorch.ExecutorchRuntime`

```kotlin
interface ExecutorchRuntime {
    interface InferenceCallback {
        fun onToken(token: String)
        fun onComplete(result: String)
        fun onError(error: String)
    }

    suspend fun loadModel(
        modelPath: String,
        tokenizerPath: String,
        temperature: Float = 0.8f
    ): Result<Unit>

    suspend fun generate(
        prompt: String,
        maxTokens: Int,
        callback: InferenceCallback
    ): Result<String>

    fun unload()
}
```

---

## 6. Tokenizers

### Status: ❌ Missing

Text tokenization for various models.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| Tokenizer module | `com.layla.tokenizers.Tokenizers` | ❌ Missing |
| React Native bridge | `TokenizersPackage` | ❌ Missing |

### Required Implementation: `org.ninelym.tokenizers.TokenizerEngine`

```kotlin
interface TokenizerEngine {
    data class TokenizerConfig(
        val vocabPath: String,
        val modelType: ModelType
    )

    enum class ModelType { BPE, WORDPIECE, SENTENCEPIECE, UNIGRAM }

    suspend fun load(config: TokenizerConfig): Result<Unit>
    fun encode(text: String): IntArray
    fun decode(tokens: IntArray): String
    fun tokenCount(text: String): Int
}
```

---

## 7. PDF Processing

### Status: ⚠️ Partial

Document processing with PDF support.

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| PDF parsing | `LaylaPdfParser` | ⚠️ Stub in DocumentProcessingService |
| Text extraction | Native implementation | ❌ Missing native |
| React Native bridge | `LaylaPdfParserPackage` | ❌ Missing |

### Required Enhancement: `org.ninelym.document.PdfProcessor`

```kotlin
interface PdfProcessor {
    data class PdfPage(
        val pageNumber: Int,
        val text: String,
        val images: List<ExtractedImage>
    )

    data class ExtractedImage(
        val data: ByteArray,
        val width: Int,
        val height: Int
    )

    suspend fun extractText(pdfPath: String): Result<String>
    suspend fun extractPages(pdfPath: String): Result<List<PdfPage>>
    suspend fun getPageCount(pdfPath: String): Result<Int>
}
```

---

## 8. Android-Specific Components

### Status: ⚠️ Partial

Android Activity/Service implementations exist as stubs but need full functionality.

| Component | Layla Implementation | Togai Status |
|-----------|---------------------|--------------|
| MainActivity | `com.layla.MainActivity` | ⚠️ Logic only |
| MainApplication | `com.layla.MainApplication` | ❌ Missing |
| HeadlessJsService | `com.layla.HeadlessJsService` | ❌ Missing |
| FloatingWidget | `TaskMgrService` widget | ❌ Missing |
| LaylaWorker | Background processing | ❌ Missing |

### Required: Android Module (`togai-android`)

```kotlin
// Activity implementations that delegate to Togai core
class TogaiMainActivity : ReactActivity()
class TogaiMainApplication : ReactApplication()
class TogaiInferenceService : Service()
class TogaiFloatingWidgetService : Service()
```

---

## 9. React Native Bridge

### Status: ❌ Missing

React Native module integration.

| Package | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| LlamaCpp | `LlamaCppPackage` | ❌ Missing |
| StableDiffusion | `StableDiffusionPackage` | ❌ Missing |
| MLCChat | `MLCChatPackage` | ❌ Missing |
| Cubism | `CubismPackage` | ❌ Missing |
| TaskMgr | `TaskMgrPackage` | ❌ Missing |
| LVDB | `LvdbPackage` | ❌ Missing |
| CTranslate2 | `CTranslate2Package` | ❌ Missing |
| Executorch | `ExecutorchPackage` | ❌ Missing |
| Tokenizers | `TokenizersPackage` | ❌ Missing |
| PDF | `LaylaPdfParserPackage` | ❌ Missing |

---

## 10. Utilities

### Status: ⚠️ Partial

| Feature | Layla Implementation | Togai Status |
|---------|---------------------|--------------|
| License checking | `LaylaNativeUtils` | ❌ Missing |
| Native library loading | Various | ⚠️ Manual |
| CPU feature detection | `LlamaCppJni.isCPU*` | ❌ Missing |

---

## Implementation Roadmap

### Phase 1: Core Inference (P0) - Critical Path
1. **LlamaCppJni** - Native LLM inference binding
2. **SDJni** - Stable Diffusion native binding
3. **VectorDatabase** - Local embedding storage
4. **TokenizerEngine** - Text tokenization

### Phase 2: Extended ML (P1)
1. **MLCChatEngine** - MLC optimization framework
2. **ExecutorchRuntime** - Meta's mobile runtime
3. **CTranslate2Jni** - Translation/summarization

### Phase 3: Animation & UI (P2)
1. **CubismEngine** - Live2D character animation
2. **FloatingWidgetService** - Android overlay UI
3. **React Native Packages** - JS bridge modules

### Phase 4: Platform Integration (P3)
1. **Android Module** - Full Android app support
2. **iOS Module** - iOS platform support (future)
3. **Desktop Module** - JVM desktop support

---

## Architecture Recommendation

```
togai/
├── togai-core/                    # Platform-agnostic Kotlin
│   ├── inference/                 # LLM inference abstractions
│   ├── image/                     # Image generation abstractions
│   ├── vector/                    # Vector DB abstractions
│   ├── tokenizer/                 # Tokenizer abstractions
│   ├── animation/                 # Animation engine abstractions
│   └── cognitive/                 # Cognitive computing (exists)
│
├── togai-native/                  # JNI implementations
│   ├── llama-jni/                 # LLaMA.cpp bindings
│   ├── sd-jni/                    # Stable Diffusion bindings
│   ├── lvdb-jni/                  # Vector DB bindings
│   ├── cubism-jni/                # Live2D bindings
│   └── ctranslate2-jni/           # CTranslate2 bindings
│
├── togai-android/                 # Android-specific
│   ├── activities/
│   ├── services/
│   ├── widgets/
│   └── react-native/              # RN bridge packages
│
└── togai-desktop/                 # Desktop JVM (future)
```

---

## Summary Statistics

| Category | Total Features | Implemented | Partial | Missing |
|----------|---------------|-------------|---------|---------|
| JNI Bindings | 5 | 0 | 0 | 5 |
| Vector Database | 5 | 0 | 0 | 5 |
| MLC Chat | 4 | 0 | 0 | 4 |
| Cubism Animation | 4 | 0 | 0 | 4 |
| Executorch | 3 | 0 | 0 | 3 |
| Tokenizers | 2 | 0 | 0 | 2 |
| PDF Processing | 3 | 0 | 1 | 2 |
| Android Components | 5 | 0 | 2 | 3 |
| React Native | 10 | 0 | 0 | 10 |
| Utilities | 3 | 0 | 1 | 2 |
| **Total** | **44** | **0** | **4** | **40** |

**Generalization Progress: ~9% (4/44 partial)**

---

## Existing Togai Strengths

The current Togai implementation provides strong foundations in:

1. **Cognitive Computing** - Comprehensive hypergraph, ECAN, metacognition
2. **Character System** - AI personas with ethical constraints
3. **Service Abstractions** - Clean service interfaces for Android features
4. **Tasker Integration** - Workflow automation logic
5. **Privacy Management** - On-device processing guarantees

These can be leveraged as the business logic layer while implementing the missing native bindings and platform integrations.
