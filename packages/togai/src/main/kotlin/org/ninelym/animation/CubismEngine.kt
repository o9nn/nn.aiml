package org.ninelym.animation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Cubism Engine for Live2D Character Animation
 *
 * Provides Live2D Cubism SDK integration for character animations.
 * Supports model loading, motion playback, expressions, and parameter control.
 */

/**
 * Model state
 */
enum class CubismModelState {
    UNLOADED,
    LOADING,
    READY,
    ERROR
}

/**
 * Motion priority
 */
enum class MotionPriority(val value: Int) {
    NONE(0),
    IDLE(1),
    NORMAL(2),
    FORCE(3)
}

/**
 * Model assets configuration
 */
data class CubismModelAssets(
    val modelPath: String,           // Path to .model3.json
    val motionGroups: Map<String, List<String>> = emptyMap(),  // Group -> motion files
    val expressions: List<String> = emptyList(),   // Expression files
    val textures: List<String> = emptyList()       // Texture files
)

/**
 * Motion data
 */
data class CubismMotion(
    val name: String,
    val group: String,
    val file: String,
    val duration: Float = 0f,
    val loop: Boolean = false
)

/**
 * Expression data
 */
data class CubismExpression(
    val name: String,
    val file: String
)

/**
 * Parameter for live control
 */
data class CubismParameter(
    val id: String,
    val value: Float,
    val defaultValue: Float = 0f,
    val minValue: Float = -1f,
    val maxValue: Float = 1f
)

/**
 * Eye blink settings
 */
data class EyeBlinkSettings(
    val enabled: Boolean = true,
    val intervalSeconds: Float = 3.5f,
    val closingDuration: Float = 0.1f,
    val closedDuration: Float = 0.05f,
    val openingDuration: Float = 0.15f
)

/**
 * Lip sync settings
 */
data class LipSyncSettings(
    val enabled: Boolean = true,
    val gain: Float = 1.0f,
    val parameterIds: List<String> = listOf("ParamMouthOpenY")
)

/**
 * Cubism JNI interface
 */
object CubismJni {

    private var isLoaded = false

    fun loadLibrary(): Boolean {
        return try {
            if (!isLoaded) {
                System.loadLibrary("cubism-jni")
                isLoaded = true
            }
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun isLibraryLoaded(): Boolean = isLoaded

    // Native methods
    @JvmStatic
    external fun cubism_init(): Boolean

    @JvmStatic
    external fun cubism_load_model(modelPath: String): Long  // Returns model handle

    @JvmStatic
    external fun cubism_release_model(handle: Long)

    @JvmStatic
    external fun cubism_update(handle: Long, deltaTime: Float)

    @JvmStatic
    external fun cubism_render(handle: Long)

    @JvmStatic
    external fun cubism_start_motion(handle: Long, group: String, index: Int, priority: Int): Boolean

    @JvmStatic
    external fun cubism_set_expression(handle: Long, name: String)

    @JvmStatic
    external fun cubism_set_parameter(handle: Long, paramId: String, value: Float)

    @JvmStatic
    external fun cubism_get_parameter(handle: Long, paramId: String): Float

    @JvmStatic
    external fun cubism_set_auto_blink(handle: Long, enabled: Boolean)

    @JvmStatic
    external fun cubism_set_lip_sync(handle: Long, value: Float)

    @JvmStatic
    external fun cubism_hit_test(handle: Long, hitAreaName: String, x: Float, y: Float): Boolean

    @JvmStatic
    external fun cubism_get_canvas_width(handle: Long): Float

    @JvmStatic
    external fun cubism_get_canvas_height(handle: Long): Float
}

/**
 * Cubism Model class
 */
class CubismModel(
    val assets: CubismModelAssets
) {
    private var nativeHandle: Long = 0
    private val _state = MutableStateFlow(CubismModelState.UNLOADED)
    val state: StateFlow<CubismModelState> = _state

    private val motions = mutableMapOf<String, MutableList<CubismMotion>>()
    private val expressions = mutableListOf<CubismExpression>()
    private val parameters = ConcurrentHashMap<String, CubismParameter>()

    private var currentExpression: String? = null
    private var currentMotionGroup: String? = null
    private var currentMotionIndex: Int = -1

    /**
     * Load model from assets
     */
    fun load(): Result<Unit> {
        return try {
            _state.value = CubismModelState.LOADING

            // Load motion groups
            assets.motionGroups.forEach { (group, files) ->
                motions[group] = files.mapIndexed { index, file ->
                    CubismMotion(
                        name = "$group$index",
                        group = group,
                        file = file
                    )
                }.toMutableList()
            }

            // Load expressions
            assets.expressions.forEach { file ->
                val name = File(file).nameWithoutExtension
                expressions.add(CubismExpression(name, file))
            }

            // Try native loading if available
            if (CubismJni.isLibraryLoaded()) {
                nativeHandle = CubismJni.cubism_load_model(assets.modelPath)
                if (nativeHandle == 0L) {
                    _state.value = CubismModelState.ERROR
                    return Result.failure(Exception("Failed to load native model"))
                }
            }

            _state.value = CubismModelState.READY
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = CubismModelState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Update model (call every frame)
     */
    fun update(deltaTime: Float) {
        if (_state.value != CubismModelState.READY) return

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_update(nativeHandle, deltaTime)
        }

        // Update auto behaviors
        updateEyeBlink(deltaTime)
    }

    private var blinkTimer = 0f
    private fun updateEyeBlink(deltaTime: Float) {
        blinkTimer += deltaTime
        if (blinkTimer > 3.5f) {
            // Trigger blink
            setParameter("ParamEyeLOpen", 0f)
            setParameter("ParamEyeROpen", 0f)
            blinkTimer = 0f
        }
    }

    /**
     * Render model
     */
    fun render() {
        if (_state.value != CubismModelState.READY) return

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_render(nativeHandle)
        }
    }

    /**
     * Start motion
     */
    fun startMotion(
        group: String,
        index: Int = 0,
        priority: MotionPriority = MotionPriority.NORMAL
    ): Boolean {
        if (_state.value != CubismModelState.READY) return false

        val groupMotions = motions[group] ?: return false
        if (index < 0 || index >= groupMotions.size) return false

        currentMotionGroup = group
        currentMotionIndex = index

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            return CubismJni.cubism_start_motion(nativeHandle, group, index, priority.value)
        }

        return true
    }

    /**
     * Start random motion from group
     */
    fun startRandomMotion(group: String, priority: MotionPriority = MotionPriority.NORMAL): Boolean {
        val groupMotions = motions[group] ?: return false
        if (groupMotions.isEmpty()) return false

        val randomIndex = (Math.random() * groupMotions.size).toInt()
        return startMotion(group, randomIndex, priority)
    }

    /**
     * Set expression
     */
    fun setExpression(name: String): Boolean {
        if (_state.value != CubismModelState.READY) return false

        val expression = expressions.find { it.name == name } ?: return false
        currentExpression = name

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_set_expression(nativeHandle, name)
        }

        return true
    }

    /**
     * Set random expression
     */
    fun setRandomExpression(): Boolean {
        if (expressions.isEmpty()) return false
        val randomIndex = (Math.random() * expressions.size).toInt()
        return setExpression(expressions[randomIndex].name)
    }

    /**
     * Set parameter value
     */
    fun setParameter(paramId: String, value: Float) {
        val clamped = value.coerceIn(-30f, 30f)
        parameters[paramId] = CubismParameter(paramId, clamped)

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_set_parameter(nativeHandle, paramId, clamped)
        }
    }

    /**
     * Get parameter value
     */
    fun getParameter(paramId: String): Float {
        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            return CubismJni.cubism_get_parameter(nativeHandle, paramId)
        }
        return parameters[paramId]?.value ?: 0f
    }

    /**
     * Set lip sync value (0.0 - 1.0)
     */
    fun setLipSync(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        setParameter("ParamMouthOpenY", clamped)

        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_set_lip_sync(nativeHandle, clamped)
        }
    }

    /**
     * Set auto eye blink
     */
    fun setAutoBlink(enabled: Boolean) {
        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_set_auto_blink(nativeHandle, enabled)
        }
    }

    /**
     * Hit test
     */
    fun hitTest(hitAreaName: String, x: Float, y: Float): Boolean {
        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            return CubismJni.cubism_hit_test(nativeHandle, hitAreaName, x, y)
        }
        return false
    }

    /**
     * Get canvas dimensions
     */
    fun getCanvasSize(): Pair<Float, Float> {
        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            val width = CubismJni.cubism_get_canvas_width(nativeHandle)
            val height = CubismJni.cubism_get_canvas_height(nativeHandle)
            return width to height
        }
        return 1024f to 1024f // Default
    }

    /**
     * Get available motion groups
     */
    fun getMotionGroups(): Set<String> = motions.keys

    /**
     * Get motions in group
     */
    fun getMotionsInGroup(group: String): List<CubismMotion> = motions[group] ?: emptyList()

    /**
     * Get available expressions
     */
    fun getExpressions(): List<CubismExpression> = expressions.toList()

    /**
     * Release model resources
     */
    fun release() {
        if (CubismJni.isLibraryLoaded() && nativeHandle != 0L) {
            CubismJni.cubism_release_model(nativeHandle)
            nativeHandle = 0
        }
        motions.clear()
        expressions.clear()
        parameters.clear()
        _state.value = CubismModelState.UNLOADED
    }
}

/**
 * Cubism Engine - manages multiple models
 */
class CubismEngine {

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized

    private val models = ConcurrentHashMap<String, CubismModel>()
    private var activeModelId: String? = null

    /**
     * Initialize engine
     */
    fun initialize(): Result<Unit> {
        return try {
            CubismJni.loadLibrary() // Try to load, ignore failure

            if (CubismJni.isLibraryLoaded()) {
                CubismJni.cubism_init()
            }

            _initialized.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load model
     */
    fun loadModel(id: String, assets: CubismModelAssets): Result<CubismModel> {
        return try {
            val model = CubismModel(assets)
            val result = model.load()

            if (result.isSuccess) {
                models[id] = model
                if (activeModelId == null) {
                    activeModelId = id
                }
                Result.success(model)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Load failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get model by ID
     */
    fun getModel(id: String): CubismModel? = models[id]

    /**
     * Get active model
     */
    fun getActiveModel(): CubismModel? = activeModelId?.let { models[it] }

    /**
     * Set active model
     */
    fun setActiveModel(id: String): Boolean {
        return if (models.containsKey(id)) {
            activeModelId = id
            true
        } else {
            false
        }
    }

    /**
     * Update all models
     */
    fun update(deltaTime: Float) {
        models.values.forEach { it.update(deltaTime) }
    }

    /**
     * Render active model
     */
    fun render() {
        getActiveModel()?.render()
    }

    /**
     * Unload model
     */
    fun unloadModel(id: String) {
        models.remove(id)?.release()
        if (activeModelId == id) {
            activeModelId = models.keys.firstOrNull()
        }
    }

    /**
     * Shutdown engine
     */
    fun shutdown() {
        models.values.forEach { it.release() }
        models.clear()
        activeModelId = null
        _initialized.value = false
    }

    /**
     * Get loaded model IDs
     */
    fun getLoadedModelIds(): Set<String> = models.keys
}

/**
 * Common Cubism parameter IDs
 */
object CubismParameters {
    // Head/Face
    const val PARAM_ANGLE_X = "ParamAngleX"
    const val PARAM_ANGLE_Y = "ParamAngleY"
    const val PARAM_ANGLE_Z = "ParamAngleZ"

    // Body
    const val PARAM_BODY_ANGLE_X = "ParamBodyAngleX"
    const val PARAM_BODY_ANGLE_Y = "ParamBodyAngleY"
    const val PARAM_BODY_ANGLE_Z = "ParamBodyAngleZ"

    // Eyes
    const val PARAM_EYE_L_OPEN = "ParamEyeLOpen"
    const val PARAM_EYE_R_OPEN = "ParamEyeROpen"
    const val PARAM_EYE_BALL_X = "ParamEyeBallX"
    const val PARAM_EYE_BALL_Y = "ParamEyeBallY"

    // Brows
    const val PARAM_BROW_L_Y = "ParamBrowLY"
    const val PARAM_BROW_R_Y = "ParamBrowRY"

    // Mouth
    const val PARAM_MOUTH_OPEN_Y = "ParamMouthOpenY"
    const val PARAM_MOUTH_FORM = "ParamMouthForm"

    // Breath
    const val PARAM_BREATH = "ParamBreath"
}
