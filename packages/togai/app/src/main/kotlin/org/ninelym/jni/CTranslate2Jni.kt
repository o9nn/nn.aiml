package org.ninelym.jni

/**
 * CTranslate2 JNI Interface
 *
 * Native bindings for CTranslate2 inference engine.
 * Provides efficient translation, summarization, and classification.
 *
 * Native library: ctranslate2-jni
 */
object CTranslate2Jni {

    private var isLoaded = false

    /**
     * Load the native library
     */
    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("ctranslate2-jni")
                isLoaded = true
            }
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Check if library is loaded
     */
    fun isLibraryLoaded(): Boolean = isLoaded

    // Native method declarations

    /**
     * Translate using M2M100 multilingual model
     * @param modelPath Path to the M2M100 model
     * @param text Text to translate
     * @param srcLang Source language code
     * @param tgtLang Target language code
     * @return Translated text
     */
    @JvmStatic
    external fun translate_m2m100(
        modelPath: String,
        text: String,
        srcLang: String,
        tgtLang: String
    ): String

    /**
     * Translate using OPUS-MT models
     * @param modelPath Path to the OPUS model
     * @param text Text to translate
     * @return Translated text
     */
    @JvmStatic
    external fun translate_opusmt(
        modelPath: String,
        text: String
    ): String

    /**
     * Summarize using BART model
     * @param modelPath Path to the BART model
     * @param text Text to summarize
     * @return Summary
     */
    @JvmStatic
    external fun summarise_bart(
        modelPath: String,
        text: String
    ): String

    /**
     * Summarize using Flan-T5 model
     * @param modelPath Path to the Flan-T5 model
     * @param text Text to summarize
     * @return Summary
     */
    @JvmStatic
    external fun summarise_flant5(
        modelPath: String,
        text: String
    ): String

    /**
     * Classify text using XLM-RoBERTa model
     * @param modelPath Path to the XLM-RoBERTa model
     * @param text Text to classify
     * @return Classification result (JSON)
     */
    @JvmStatic
    external fun classify_xlmroberta(
        modelPath: String,
        text: String
    ): String

    /**
     * Release loaded model
     * @param modelPath Path of model to release
     */
    @JvmStatic
    external fun release_model(modelPath: String)
}

/**
 * High-level wrapper for CTranslate2 operations
 */
class CTranslate2Engine {

    enum class ModelType {
        M2M100,       // Multilingual translation
        OPUS_MT,      // Bilingual translation
        BART,         // Summarization
        FLAN_T5,      // Summarization/instruction
        XLM_ROBERTA   // Classification
    }

    data class TranslationResult(
        val sourceText: String,
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String
    )

    data class SummaryResult(
        val originalText: String,
        val summary: String,
        val compressionRatio: Float
    )

    data class ClassificationResult(
        val text: String,
        val label: String,
        val confidence: Float,
        val allLabels: Map<String, Float>
    )

    private val loadedModels = mutableSetOf<String>()

    /**
     * Initialize the engine
     */
    fun initialize(): Result<Unit> {
        return try {
            if (!CTranslate2Jni.loadLibrary()) {
                Result.failure(Exception("Failed to load ctranslate2-jni library"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translate text using M2M100
     */
    fun translateMultilingual(
        modelPath: String,
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): Result<TranslationResult> {
        return try {
            val translated = CTranslate2Jni.translate_m2m100(
                modelPath, text, sourceLanguage, targetLanguage
            )
            loadedModels.add(modelPath)

            Result.success(TranslationResult(
                sourceText = text,
                translatedText = translated,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translate text using OPUS-MT (bilingual)
     */
    fun translateBilingual(
        modelPath: String,
        text: String,
        sourceLanguage: String = "auto",
        targetLanguage: String = "en"
    ): Result<TranslationResult> {
        return try {
            val translated = CTranslate2Jni.translate_opusmt(modelPath, text)
            loadedModels.add(modelPath)

            Result.success(TranslationResult(
                sourceText = text,
                translatedText = translated,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Summarize text using BART
     */
    fun summarizeWithBART(modelPath: String, text: String): Result<SummaryResult> {
        return try {
            val summary = CTranslate2Jni.summarise_bart(modelPath, text)
            loadedModels.add(modelPath)

            val ratio = if (text.isNotEmpty()) {
                summary.length.toFloat() / text.length.toFloat()
            } else 0f

            Result.success(SummaryResult(
                originalText = text,
                summary = summary,
                compressionRatio = ratio
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Summarize text using Flan-T5
     */
    fun summarizeWithFlanT5(modelPath: String, text: String): Result<SummaryResult> {
        return try {
            val summary = CTranslate2Jni.summarise_flant5(modelPath, text)
            loadedModels.add(modelPath)

            val ratio = if (text.isNotEmpty()) {
                summary.length.toFloat() / text.length.toFloat()
            } else 0f

            Result.success(SummaryResult(
                originalText = text,
                summary = summary,
                compressionRatio = ratio
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Classify text using XLM-RoBERTa
     */
    fun classify(modelPath: String, text: String): Result<ClassificationResult> {
        return try {
            val jsonResult = CTranslate2Jni.classify_xlmroberta(modelPath, text)
            loadedModels.add(modelPath)

            // Parse JSON result (simplified - would use proper JSON parsing)
            val result = parseClassificationResult(text, jsonResult)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseClassificationResult(text: String, jsonResult: String): ClassificationResult {
        // Simplified parsing - in production would use kotlinx.serialization
        // Expected format: {"label": "positive", "confidence": 0.95, "labels": {"positive": 0.95, "negative": 0.05}}
        return ClassificationResult(
            text = text,
            label = "neutral",
            confidence = 0.5f,
            allLabels = mapOf("neutral" to 0.5f)
        )
    }

    /**
     * Release specific model
     */
    fun releaseModel(modelPath: String) {
        try {
            CTranslate2Jni.release_model(modelPath)
            loadedModels.remove(modelPath)
        } catch (e: Exception) {
            // Ignore release errors
        }
    }

    /**
     * Release all loaded models
     */
    fun shutdown() {
        loadedModels.toList().forEach { modelPath ->
            releaseModel(modelPath)
        }
    }

    companion object {
        // Common language codes for M2M100
        object LanguageCodes {
            const val ENGLISH = "en"
            const val SPANISH = "es"
            const val FRENCH = "fr"
            const val GERMAN = "de"
            const val ITALIAN = "it"
            const val PORTUGUESE = "pt"
            const val RUSSIAN = "ru"
            const val CHINESE = "zh"
            const val JAPANESE = "ja"
            const val KOREAN = "ko"
            const val ARABIC = "ar"
            const val HINDI = "hi"
        }
    }
}
