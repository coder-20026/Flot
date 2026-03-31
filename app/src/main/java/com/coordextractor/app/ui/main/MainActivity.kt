package com.coordextractor.app.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.coordextractor.app.R
import com.coordextractor.app.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main Activity - Entry point of the application
 * Handles permissions and service management
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // Permission launchers
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkPermissions()
        if (Settings.canDrawOverlays(this)) {
            checkAndRequestCapturePermission()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkPermissions()
        if (isGranted) {
            checkNextPermission()
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.initializeCapture(result.resultCode, result.data!!)
            viewModel.startFloatingService()
        } else {
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    private fun setupUI() {
        binding.apply {
            // Start/Stop button
            btnStartService.setOnClickListener {
                val state = viewModel.uiState.value
                if (state.isServiceRunning) {
                    viewModel.stopFloatingService()
                } else {
                    startServiceFlow()
                }
            }

            // Settings button
            btnSettings.setOnClickListener {
                toggleSettingsPanel()
            }

            // ROI Width slider
            sliderRoiWidth.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateRoiWidth(value.toInt())
                    tvRoiWidthValue.text = "${value.toInt()}%"
                }
            }

            // ROI Height slider
            sliderRoiHeight.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateRoiHeight(value.toInt())
                    tvRoiHeightValue.text = "${value.toInt()}%"
                }
            }

            // Capture interval slider
            sliderCaptureInterval.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateCaptureInterval(value.toInt())
                    tvCaptureIntervalValue.text = "${value.toInt()}ms"
                }
            }

            // Realtime mode switch
            switchRealtimeMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleRealtimeMode(isChecked)
            }

            // Permission buttons
            btnGrantOverlay.setOnClickListener {
                requestOverlayPermission()
            }

            btnGrantNotification.setOnClickListener {
                requestNotificationPermission()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: MainUiState) {
        binding.apply {
            // Service button state
            btnStartService.apply {
                text = if (state.isServiceRunning) {
                    getString(R.string.btn_stop_service)
                } else {
                    getString(R.string.btn_start_service)
                }
                icon = ContextCompat.getDrawable(
                    this@MainActivity,
                    if (state.isServiceRunning) R.drawable.ic_stop else R.drawable.ic_play
                )
            }

            // Service status
            tvServiceStatus.apply {
                text = if (state.isServiceRunning) {
                    getString(R.string.realtime_active)
                } else {
                    getString(R.string.welcome_subtitle)
                }
            }

            statusIndicator.isVisible = state.isServiceRunning

            // Permission states
            cardPermissions.isVisible = !state.allPermissionsGranted
            
            permissionOverlay.apply {
                ivPermissionStatus.setImageResource(
                    if (state.hasOverlayPermission) R.drawable.ic_check else R.drawable.ic_error
                )
                btnGrantOverlay.isVisible = !state.hasOverlayPermission
            }

            permissionNotification.apply {
                ivNotificationStatus.setImageResource(
                    if (state.hasNotificationPermission) R.drawable.ic_check else R.drawable.ic_error
                )
                btnGrantNotification.isVisible = !state.hasNotificationPermission
            }

            // Settings values
            sliderRoiWidth.value = state.roiWidthPercent.toFloat()
            tvRoiWidthValue.text = "${state.roiWidthPercent}%"

            sliderRoiHeight.value = state.roiHeightPercent.toFloat()
            tvRoiHeightValue.text = "${state.roiHeightPercent}%"

            sliderCaptureInterval.value = state.captureInterval.toFloat()
            tvCaptureIntervalValue.text = "${state.captureInterval}ms"

            switchRealtimeMode.isChecked = state.realtimeModeEnabled

            // Permission dialog
            if (state.showPermissionDialog) {
                showPermissionRequiredDialog()
                viewModel.dismissPermissionDialog()
            }
        }
    }

    private fun startServiceFlow() {
        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
            return
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Request MediaProjection permission
        checkAndRequestCapturePermission()
    }

    private fun checkNextPermission() {
        if (Settings.canDrawOverlays(this)) {
            checkAndRequestCapturePermission()
        }
    }

    private fun requestOverlayPermission() {
        showOverlayPermissionDialog()
    }

    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_overlay_title)
            .setMessage(R.string.permission_overlay_desc)
            .setPositiveButton(R.string.btn_grant_permission) { _, _ ->
                overlayPermissionLauncher.launch(viewModel.getOverlayPermissionIntent())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkAndRequestCapturePermission() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(captureIntent)
    }

    private fun showPermissionRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_overlay_title)
            .setMessage(R.string.permission_overlay_desc)
            .setPositiveButton(R.string.btn_grant_permission) { _, _ ->
                startServiceFlow()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toggleSettingsPanel() {
        binding.apply {
            val isVisible = cardSettings.isVisible
            cardSettings.isVisible = !isVisible
            
            btnSettings.icon = ContextCompat.getDrawable(
                this@MainActivity,
                if (!isVisible) R.drawable.ic_close else R.drawable.ic_settings
            )
        }
    }
}
