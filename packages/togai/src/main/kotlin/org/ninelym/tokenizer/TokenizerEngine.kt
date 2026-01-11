package org.ninelym.tokenizer

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Tokenizer Engine
 *
 * Text tokenization for various model types.
 * Supports BPE, WordPiece, SentencePiece, and Unigram tokenizers.
 */

/**
 * Tokenizer model types
 */
enum class TokenizerType {
    BPE,           // Byte-Pair Encoding (GPT-2, GPT-3, LLaMA)
    WORDPIECE,     // WordPiece (BERT)
    SENTENCEPIECE, // SentencePiece (T5, mBART)
    UNIGRAM,       // Unigram (XLNet)
    TIKTOKEN       // OpenAI's tiktoken (GPT-4)
}

/**
 * Token data class
 */
data class Token(
    val id: Int,
    val text: String,
    val start: Int = -1,
    val end: Int = -1
)

/**
 * Tokenizer configuration
 */
data class TokenizerConfig(
    val vocabPath: String,
    val mergesPath: String? = null,
    val type: TokenizerType = TokenizerType.BPE,
    val addBosToken: Boolean = true,
    val addEosToken: Boolean = true,
    val bosToken: String = "<s>",
    val eosToken: String = "</s>",
    val padToken: String = "<pad>",
    val unkToken: String = "<unk>",
    val maxLength: Int = 4096
)

/**
 * Native JNI interface for tokenizers
 */
object TokenizersJni {

    private var isLoaded = false

    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("tokenizers-jni")
                isLoaded = true
            }
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun isLibraryLoaded(): Boolean = isLoaded

    @JvmStatic
    external fun tokenizer_load(vocabPath: String, type: Int): Long  // Returns handle

    @JvmStatic
    external fun tokenizer_encode(handle: Long, text: String): IntArray

    @JvmStatic
    external fun tokenizer_decode(handle: Long, tokens: IntArray): String

    @JvmStatic
    external fun tokenizer_vocab_size(handle: Long): Int

    @JvmStatic
    external fun tokenizer_free(handle: Long)
}

/**
 * Base tokenizer interface
 */
interface Tokenizer {
    val config: TokenizerConfig
    val vocabSize: Int

    fun encode(text: String): IntArray
    fun decode(tokens: IntArray): String
    fun tokenize(text: String): List<Token>
    fun countTokens(text: String): Int
    fun truncate(tokens: IntArray, maxLength: Int): IntArray
    fun pad(tokens: IntArray, length: Int, padId: Int = 0): IntArray
}

/**
 * BPE Tokenizer implementation
 */
class BPETokenizer(
    override val config: TokenizerConfig
) : Tokenizer {

    private val vocab = mutableMapOf<String, Int>()
    private val reverseVocab = mutableMapOf<Int, String>()
    private val merges = mutableListOf<Pair<String, String>>()

    override val vocabSize: Int get() = vocab.size

    init {
        loadVocab()
        loadMerges()
    }

    private fun loadVocab() {
        val file = File(config.vocabPath)
        if (file.exists()) {
            file.readLines().forEachIndexed { index, line ->
                val token = line.trim()
                if (token.isNotEmpty()) {
                    vocab[token] = index
                    reverseVocab[index] = token
                }
            }
        }
    }

    private fun loadMerges() {
        config.mergesPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.trim().split(" ")
                    if (parts.size == 2) {
                        merges.add(parts[0] to parts[1])
                    }
                }
            }
        }
    }

    override fun encode(text: String): IntArray {
        val tokens = tokenize(text)
        val ids = tokens.map { it.id }.toMutableList()

        if (config.addBosToken) {
            vocab[config.bosToken]?.let { ids.add(0, it) }
        }
        if (config.addEosToken) {
            vocab[config.eosToken]?.let { ids.add(it) }
        }

        return ids.toIntArray()
    }

    override fun decode(tokens: IntArray): String {
        return tokens
            .mapNotNull { reverseVocab[it] }
            .joinToString("")
            .replace("Ġ", " ")
            .replace("▁", " ")
            .trim()
    }

    override fun tokenize(text: String): List<Token> {
        val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
        val tokens = mutableListOf<Token>()
        var position = 0

        for (word in words) {
            if (word.isEmpty()) continue

            val wordTokens = tokenizeWord(word)
            wordTokens.forEach { tokenText ->
                val id = vocab[tokenText] ?: vocab[config.unkToken] ?: 0
                tokens.add(Token(id, tokenText, position, position + tokenText.length))
                position += tokenText.length
            }
        }

        return tokens
    }

    private fun tokenizeWord(word: String): List<String> {
        // Simplified BPE - in production would use proper merge algorithm
        var tokens = word.map { it.toString() }.toMutableList()

        for ((first, second) in merges) {
            var i = 0
            while (i < tokens.size - 1) {
                if (tokens[i] == first && tokens[i + 1] == second) {
                    tokens[i] = first + second
                    tokens.removeAt(i + 1)
                } else {
                    i++
                }
            }
        }

        return tokens
    }

    override fun countTokens(text: String): Int = encode(text).size

    override fun truncate(tokens: IntArray, maxLength: Int): IntArray {
        return if (tokens.size > maxLength) {
            tokens.copyOf(maxLength)
        } else {
            tokens
        }
    }

    override fun pad(tokens: IntArray, length: Int, padId: Int): IntArray {
        return if (tokens.size < length) {
            tokens + IntArray(length - tokens.size) { padId }
        } else {
            tokens
        }
    }
}

/**
 * SentencePiece Tokenizer implementation
 */
class SentencePieceTokenizer(
    override val config: TokenizerConfig
) : Tokenizer {

    private val vocab = mutableMapOf<String, Int>()
    private val reverseVocab = mutableMapOf<Int, String>()
    private val scores = mutableMapOf<String, Float>()

    override val vocabSize: Int get() = vocab.size

    init {
        loadVocab()
    }

    private fun loadVocab() {
        val file = File(config.vocabPath)
        if (file.exists()) {
            file.readLines().forEachIndexed { index, line ->
                val parts = line.trim().split("\t")
                if (parts.isNotEmpty()) {
                    val token = parts[0]
                    val score = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    vocab[token] = index
                    reverseVocab[index] = token
                    scores[token] = score
                }
            }
        }
    }

    override fun encode(text: String): IntArray {
        val tokens = tokenize(text)
        val ids = tokens.map { it.id }.toMutableList()

        if (config.addBosToken) {
            vocab[config.bosToken]?.let { ids.add(0, it) }
        }
        if (config.addEosToken) {
            vocab[config.eosToken]?.let { ids.add(it) }
        }

        return ids.toIntArray()
    }

    override fun decode(tokens: IntArray): String {
        return tokens
            .mapNotNull { reverseVocab[it] }
            .joinToString("")
            .replace("▁", " ")
            .trim()
    }

    override fun tokenize(text: String): List<Token> {
        // Simplified unigram tokenization
        val normalizedText = "▁" + text.replace(" ", "▁")
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < normalizedText.length) {
            var bestToken: String? = null
            var bestLength = 0

            // Find longest matching token
            for (length in minOf(normalizedText.length - i, 20) downTo 1) {
                val candidate = normalizedText.substring(i, i + length)
                if (vocab.containsKey(candidate)) {
                    bestToken = candidate
                    bestLength = length
                    break
                }
            }

            if (bestToken != null) {
                val id = vocab[bestToken]!!
                tokens.add(Token(id, bestToken, i, i + bestLength))
                i += bestLength
            } else {
                // Unknown character
                val char = normalizedText[i].toString()
                val id = vocab[config.unkToken] ?: 0
                tokens.add(Token(id, char, i, i + 1))
                i++
            }
        }

        return tokens
    }

    override fun countTokens(text: String): Int = encode(text).size

    override fun truncate(tokens: IntArray, maxLength: Int): IntArray {
        return if (tokens.size > maxLength) tokens.copyOf(maxLength) else tokens
    }

    override fun pad(tokens: IntArray, length: Int, padId: Int): IntArray {
        return if (tokens.size < length) {
            tokens + IntArray(length - tokens.size) { padId }
        } else {
            tokens
        }
    }
}

/**
 * Tokenizer Engine - manages multiple tokenizers
 */
class TokenizerEngine {

    private val tokenizers = ConcurrentHashMap<String, Tokenizer>()
    private var useNative = false

    /**
     * Initialize engine
     */
    fun initialize(preferNative: Boolean = true): Result<Unit> {
        return try {
            if (preferNative) {
                useNative = TokenizersJni.loadLibrary()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load tokenizer with config
     */
    fun loadTokenizer(name: String, config: TokenizerConfig): Result<Tokenizer> {
        return try {
            val tokenizer = when (config.type) {
                TokenizerType.BPE -> BPETokenizer(config)
                TokenizerType.SENTENCEPIECE, TokenizerType.UNIGRAM -> SentencePieceTokenizer(config)
                else -> BPETokenizer(config) // Default to BPE
            }
            tokenizers[name] = tokenizer
            Result.success(tokenizer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get loaded tokenizer
     */
    fun getTokenizer(name: String): Tokenizer? = tokenizers[name]

    /**
     * Encode text with named tokenizer
     */
    fun encode(name: String, text: String): IntArray? {
        return tokenizers[name]?.encode(text)
    }

    /**
     * Decode tokens with named tokenizer
     */
    fun decode(name: String, tokens: IntArray): String? {
        return tokenizers[name]?.decode(tokens)
    }

    /**
     * Count tokens
     */
    fun countTokens(name: String, text: String): Int {
        return tokenizers[name]?.countTokens(text) ?: 0
    }

    /**
     * Unload tokenizer
     */
    fun unloadTokenizer(name: String) {
        tokenizers.remove(name)
    }

    /**
     * Shutdown and clear all tokenizers
     */
    fun shutdown() {
        tokenizers.clear()
    }

    companion object {
        /**
         * Quick token count estimation (without loading full tokenizer)
         * Uses approximate 4 chars per token ratio
         */
        fun estimateTokenCount(text: String): Int {
            return (text.length / 4.0).toInt() + 1
        }
    }
}
