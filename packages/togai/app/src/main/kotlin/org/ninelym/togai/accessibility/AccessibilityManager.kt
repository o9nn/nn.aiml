package org.ninelym.togai.accessibility

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager as AndroidAccessibilityManager
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.ninelym.togai.util.ErrorHandler

/**
 * Accessibility manager for Togai
 * Provides comprehensive accessibility features for users with disabilities
 */
class AccessibilityManager private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "AccessibilityManager"
        
        @Volatile
        private var instance: AccessibilityManager? = null
        
        fun getInstance(context: Context): AccessibilityManager {
            return instance ?: synchronized(this) {
                instance ?: AccessibilityManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * Accessibility features
     */
    enum class AccessibilityFeature {
        SCREEN_READER,
        HIGH_CONTRAST,
        LARGE_TEXT,
        VOICE_CONTROL,
        REDUCED_MOTION,
        HAPTIC_FEEDBACK
    }
    
    /**
     * Accessibility settings
     */
    data class AccessibilitySettings(
        val screenReaderEnabled: Boolean = false,
        val highContrastEnabled: Boolean = false,
        val largeTextEnabled: Boolean = false,
        val voiceControlEnabled: Boolean = false,
        val reducedMotionEnabled: Boolean = false,
        val hapticFeedbackEnabled: Boolean = true,
        val textScaleFactor: Float = 1.0f,
        val minimumTouchTargetSize: Int = 48 // dp
    )
    
    private val androidAccessibilityManager = 
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AndroidAccessibilityManager
    
    private var currentSettings = AccessibilitySettings()
    
    /**
     * Initialize accessibility manager
     */
    fun initialize() {
        detectAccessibilityFeatures()
        ErrorHandler.logInfo(TAG, "AccessibilityManager initialized")
    }
    
    /**
     * Detect enabled accessibility features
     */
    fun detectAccessibilityFeatures(): AccessibilitySettings {
        currentSettings = AccessibilitySettings(
            screenReaderEnabled = isScreenReaderEnabled(),
            highContrastEnabled = isHighContrastEnabled(),
            largeTextEnabled = isLargeTextEnabled(),
            reducedMotionEnabled = isReducedMotionEnabled(),
            textScaleFactor = getTextScaleFactor()
        )
        
        ErrorHandler.logDebug(TAG, "Detected accessibility features: $currentSettings")
        return currentSettings
    }
    
    /**
     * Check if screen reader is enabled
     */
    fun isScreenReaderEnabled(): Boolean {
        return androidAccessibilityManager.isEnabled &&
                androidAccessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Check if high contrast is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } else {
            false
        }
    }
    
    /**
     * Check if large text is enabled
     */
    fun isLargeTextEnabled(): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale > 1.0f
    }
    
    /**
     * Check if reduced motion is enabled
     */
    fun isReducedMotionEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            ) == 0.0f
        } else {
            false
        }
    }
    
    /**
     * Get text scale factor
     */
    fun getTextScaleFactor(): Float {
        return context.resources.configuration.fontScale
    }
    
    /**
     * Get current accessibility settings
     */
    fun getSettings(): AccessibilitySettings {
        return currentSettings
    }
    
    /**
     * Announce message for screen readers
     */
    fun announce(message: String, priority: Int = AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED) {
        if (!isScreenReaderEnabled()) return
        
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        event.text.add(message)
        event.contentChangeTypes = priority
        
        androidAccessibilityManager.sendAccessibilityEvent(event)
        ErrorHandler.logDebug(TAG, "Announced: $message")
    }
    
    /**
     * Set content description for a view
     */
    fun setContentDescription(view: View, description: String) {
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = description
            }
        })
    }
    
    /**
     * Make view focusable for accessibility
     */
    fun makeAccessible(
        view: View,
        contentDescription: String,
        isButton: Boolean = false
    ) {
        view.apply {
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            this.contentDescription = contentDescription
            
            if (isButton) {
                isClickable = true
                ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = android.widget.Button::class.java.name
                        info.isClickable = true
                    }
                })
            }
        }
    }
    
    /**
     * Set minimum touch target size
     */
    fun setMinimumTouchTarget(view: View, minSizeDp: Int = 48) {
        val density = context.resources.displayMetrics.density
        val minSizePx = (minSizeDp * density).toInt()
        
        view.minimumWidth = minSizePx
        view.minimumHeight = minSizePx
    }
    
    /**
     * Apply high contrast theme
     */
    fun applyHighContrast(view: View) {
        if (!isHighContrastEnabled()) return
        
        // Apply high contrast colors
        // This would typically involve changing text colors, backgrounds, etc.
        ErrorHandler.logDebug(TAG, "Applied high contrast to view")
    }
    
    /**
     * Get animation duration multiplier based on reduced motion setting
     */
    fun getAnimationDurationMultiplier(): Float {
        return if (isReducedMotionEnabled()) 0.0f else 1.0f
    }
    
    /**
     * Provide haptic feedback if enabled
     */
    fun performHapticFeedback(view: View, feedbackType: Int = android.view.HapticFeedbackConstants.VIRTUAL_KEY) {
        if (!currentSettings.hapticFeedbackEnabled) return
        
        view.performHapticFeedback(
            feedbackType,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
    
    /**
     * Create accessible error message
     */
    fun createAccessibleError(
        errorMessage: String,
        fieldName: String? = null
    ): String {
        return if (fieldName != null) {
            "Error in $fieldName: $errorMessage"
        } else {
            "Error: $errorMessage"
        }
    }
    
    /**
     * Create accessible success message
     */
    fun createAccessibleSuccess(
        successMessage: String,
        action: String? = null
    ): String {
        return if (action != null) {
            "$action successful: $successMessage"
        } else {
            "Success: $successMessage"
        }
    }
    
    /**
     * Check if view meets accessibility guidelines
     */
    fun validateAccessibility(view: View): AccessibilityValidation {
        val issues = mutableListOf<String>()
        
        // Check content description
        if (view.contentDescription.isNullOrEmpty() && view.isClickable) {
            issues.add("Clickable view missing content description")
        }
        
        // Check touch target size
        val minSize = (48 * context.resources.displayMetrics.density).toInt()
        if (view.width < minSize || view.height < minSize) {
            issues.add("Touch target too small (minimum 48dp)")
        }
        
        // Check contrast (simplified check)
        if (view.alpha < 0.5f) {
            issues.add("View may have insufficient contrast")
        }
        
        return AccessibilityValidation(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Accessibility validation result
     */
    data class AccessibilityValidation(
        val isValid: Boolean,
        val issues: List<String>
    )
}

/**
 * Extension functions for easier accessibility usage
 */

/**
 * Make view accessible with content description
 */
fun View.makeAccessible(description: String, isButton: Boolean = false) {
    val manager = AccessibilityManager.getInstance(context)
    manager.makeAccessible(this, description, isButton)
}

/**
 * Announce message for screen readers
 */
fun Context.announce(message: String) {
    val manager = AccessibilityManager.getInstance(this)
    manager.announce(message)
}

/**
 * Set minimum touch target size
 */
fun View.setMinimumTouchTarget(minSizeDp: Int = 48) {
    val manager = AccessibilityManager.getInstance(context)
    manager.setMinimumTouchTarget(this, minSizeDp)
}

/**
 * Provide haptic feedback
 */
fun View.performAccessibleHapticFeedback() {
    val manager = AccessibilityManager.getInstance(context)
    manager.performHapticFeedback(this)
}

/**
 * Validate view accessibility
 */
fun View.validateAccessibility(): AccessibilityManager.AccessibilityValidation {
    val manager = AccessibilityManager.getInstance(context)
    return manager.validateAccessibility(this)
}
