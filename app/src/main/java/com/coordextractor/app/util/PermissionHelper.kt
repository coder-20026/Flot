package com.coordextractor.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper class for permission management
 */
object PermissionHelper {
    
    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasOverlayPermission(context) && hasNotificationPermission(context)
    }
    
    /**
     * Get intent for overlay permission settings
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
    
    /**
     * Get intent for app settings
     */
    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    
    /**
     * Get notification permission string (Android 13+)
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }
    
    /**
     * Check permission status and return detailed result
     */
    data class PermissionStatus(
        val hasOverlay: Boolean,
        val hasNotification: Boolean,
        val needsOverlay: Boolean,
        val needsNotification: Boolean
    ) {
        val isAllGranted: Boolean get() = hasOverlay && hasNotification
        val missingPermissions: List<String> get() = buildList {
            if (!hasOverlay) add("Overlay Permission")
            if (!hasNotification) add("Notification Permission")
        }
    }
    
    /**
     * Get comprehensive permission status
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        val hasOverlay = hasOverlayPermission(context)
        val hasNotification = hasNotificationPermission(context)
        
        return PermissionStatus(
            hasOverlay = hasOverlay,
            hasNotification = hasNotification,
            needsOverlay = !hasOverlay,
            needsNotification = !hasNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }
}
