package com.coordextractor.app.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coordextractor.app.service.FloatingOverlayService
import com.coordextractor.app.service.ScreenCaptureService
import com.coordextractor.app.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity
 * Manages service state and permissions
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()

    init {
        checkPermissions()
        loadSettings()
    }

    /**
     * Check all required permissions
     */
    fun checkPermissions() {
        val hasOverlayPermission = Settings.canDrawOverlays(context)
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _uiState.update {
            it.copy(
                hasOverlayPermission = hasOverlayPermission,
                hasNotificationPermission = hasNotificationPermission,
                allPermissionsGranted = hasOverlayPermission && hasNotificationPermission
            )
        }
    }

    /**
     * Load saved settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.roiWidthPercentFlow.collect { width ->
                _uiState.update { it.copy(roiWidthPercent = width) }
            }
        }
        viewModelScope.launch {
            preferencesManager.roiHeightPercentFlow.collect { height ->
                _uiState.update { it.copy(roiHeightPercent = height) }
            }
        }
        viewModelScope.launch {
            preferencesManager.captureIntervalFlow.collect { interval ->
                _uiState.update { it.copy(captureInterval = interval) }
            }
        }
        viewModelScope.launch {
            preferencesManager.realtimeModeFlow.collect { enabled ->
                _uiState.update { it.copy(realtimeModeEnabled = enabled) }
            }
        }
    }

    /**
     * Start the floating overlay service
     */
    fun startFloatingService() {
        android.util.Log.d("MainViewModel", "startFloatingService called")
        
        if (!_uiState.value.allPermissionsGranted) {
            android.util.Log.d("MainViewModel", "Permissions not granted, showing dialog")
            _uiState.update { it.copy(showPermissionDialog = true) }
            return
        }

        try {
            val serviceIntent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                android.util.Log.d("MainViewModel", "startForegroundService called")
            } else {
                context.startService(serviceIntent)
                android.util.Log.d("MainViewModel", "startService called")
            }

            _uiState.update { it.copy(isServiceRunning = true) }
            android.util.Log.d("MainViewModel", "Service started, state updated")
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to start service: ${e.message}", e)
            _uiState.update { it.copy(error = "Failed to start service: ${e.message}") }
        }
    }

    /**
     * Stop the floating overlay service
     */
    fun stopFloatingService() {
        val serviceIntent = Intent(context, FloatingOverlayService::class.java)
        context.stopService(serviceIntent)
        
        val captureIntent = Intent(context, ScreenCaptureService::class.java)
        context.stopService(captureIntent)

        _uiState.update { it.copy(isServiceRunning = false) }
    }

    /**
     * Initialize screen capture with MediaProjection result
     */
    fun initializeCapture(resultCode: Int, data: Intent) {
        android.util.Log.d("MainViewModel", "initializeCapture called with resultCode=$resultCode")
        
        try {
            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_CAPTURE
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                android.util.Log.d("MainViewModel", "ScreenCaptureService started as foreground")
            } else {
                context.startService(serviceIntent)
                android.util.Log.d("MainViewModel", "ScreenCaptureService started")
            }

            _uiState.update { it.copy(hasCapturePermission = true) }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to initialize capture: ${e.message}", e)
        }
    }

    /**
     * Get intent for overlay permission settings
     */
    fun getOverlayPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Update ROI width percentage
     */
    fun updateRoiWidth(percent: Int) {
        preferencesManager.roiWidthPercent = percent
    }

    /**
     * Update ROI height percentage
     */
    fun updateRoiHeight(percent: Int) {
        preferencesManager.roiHeightPercent = percent
    }

    /**
     * Update capture interval
     */
    fun updateCaptureInterval(intervalMs: Int) {
        preferencesManager.captureInterval = intervalMs
    }

    /**
     * Toggle realtime mode
     */
    fun toggleRealtimeMode(enabled: Boolean) {
        preferencesManager.realtimeMode = enabled
    }

    /**
     * Dismiss permission dialog
     */
    fun dismissPermissionDialog() {
        _uiState.update { it.copy(showPermissionDialog = false) }
    }

    /**
     * Mark first launch as complete
     */
    fun completeFirstLaunch() {
        preferencesManager.isFirstLaunch = false
        _uiState.update { it.copy(isFirstLaunch = false) }
    }

    /**
     * Reset settings to defaults
     */
    fun resetSettings() {
        viewModelScope.launch {
            preferencesManager.resetToDefaults()
        }
    }
}

/**
 * UI State for MainActivity
 */
data class MainUiState(
    val isServiceRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasCapturePermission: Boolean = false,
    val allPermissionsGranted: Boolean = false,
    val showPermissionDialog: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val roiWidthPercent: Int = 30,
    val roiHeightPercent: Int = 25,
    val captureInterval: Int = 500,
    val realtimeModeEnabled: Boolean = false,
    val error: String? = null
)
