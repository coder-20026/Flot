package com.coordextractor.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import com.coordextractor.app.CoordinateExtractorApp
import com.coordextractor.app.R
import com.coordextractor.app.capture.CaptureManager
import com.coordextractor.app.databinding.LayoutFloatingButtonBinding
import com.coordextractor.app.databinding.LayoutResultCardBinding
import com.coordextractor.app.ocr.OCRRepository
import com.coordextractor.app.parser.CoordinateResult
import com.coordextractor.app.parser.TextParser
import com.coordextractor.app.processing.ImageProcessor
import com.coordextractor.app.ui.main.MainActivity
import com.coordextractor.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Floating overlay service that displays a capture button on all screens
 * Handles screen capture, OCR processing, and result display
 */
@AndroidEntryPoint
class FloatingOverlayService : Service() {

    @Inject lateinit var captureManager: CaptureManager
    @Inject lateinit var imageProcessor: ImageProcessor
    @Inject lateinit var ocrRepository: OCRRepository
    @Inject lateinit var textParser: TextParser
    @Inject lateinit var preferencesManager: PreferencesManager

    private lateinit var windowManager: WindowManager
    private var floatingButtonBinding: LayoutFloatingButtonBinding? = null
    private var resultCardBinding: LayoutResultCardBinding? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var realtimeJob: Job? = null
    
    private var isProcessing = false
    private var isRealtimeMode = false
    private var isServiceReady = false  // Track if service is fully initialized
    private var isButtonActive = false  // Track button active state for visual feedback
    private var lastCoordinates: String? = null

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val LONG_PRESS_DURATION = 500L
        private const val DRAG_THRESHOLD = 10
        
        const val ACTION_CAPTURE = "com.coordextractor.ACTION_CAPTURE"
        const val ACTION_STOP = "com.coordextractor.ACTION_STOP"
        const val ACTION_TOGGLE_REALTIME = "com.coordextractor.ACTION_TOGGLE_REALTIME"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FloatingOverlay", "onCreate called")
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            
            // Remove any existing overlays before creating new ones
            removeOverlays()
            
            setupFloatingButton()
            android.util.Log.d("FloatingOverlay", "onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Failed to create floating button: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("FloatingOverlay", "onStartCommand called, action=${intent?.action}")
        
        try {
            // Start foreground first to prevent ANR
            startForeground(NOTIFICATION_ID, createNotification())
            android.util.Log.d("FloatingOverlay", "Foreground started")
            
            // Mark service as ready after foreground started
            isServiceReady = true
            
            // Ensure floating button is visible
            floatingButtonBinding?.root?.let { view ->
                view.visibility = android.view.View.VISIBLE
                view.alpha = 0.85f // Ready but inactive
            }
            
            updateButtonVisualState()
            
            // Log capture manager state
            android.util.Log.d("FloatingOverlay", "CaptureManager initialized: ${captureManager.isInitialized()}")
            
            when (intent?.action) {
                ACTION_CAPTURE -> performCapture()
                ACTION_STOP -> {
                    android.util.Log.d("FloatingOverlay", "Stopping service")
                    stopSelf()
                }
                ACTION_TOGGLE_REALTIME -> toggleRealtimeMode()
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Error in onStartCommand: ${e.message}", e)
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceReady = false
        isButtonActive = false
        try {
            serviceScope.cancel()
            realtimeJob?.cancel()
            removeOverlays()
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Error in onDestroy: ${e.message}")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingButton() {
        android.util.Log.d("FloatingOverlay", "setupFloatingButton called")
        try {
            floatingButtonBinding = LayoutFloatingButtonBinding.inflate(
                LayoutInflater.from(this)
            )
            android.util.Log.d("FloatingOverlay", "Binding inflated")

            val params = createFloatingLayoutParams()

            // Set initial position (center-right)
            val displayMetrics = resources.displayMetrics
            params.x = displayMetrics.widthPixels / 2 - 100
            params.y = displayMetrics.heightPixels / 3
            android.util.Log.d("FloatingOverlay", "Position set: x=${params.x}, y=${params.y}")

            floatingButtonBinding?.root?.let { view ->
                windowManager.addView(view, params)
                android.util.Log.d("FloatingOverlay", "View added to WindowManager")
                
                // Initial state: button is inactive until service is ready
                updateButtonVisualState()
                
                var isDragging = false
                var longPressTriggered = false
                val longPressRunnable = Runnable {
                    longPressTriggered = true
                    vibrate()
                }

                view.setOnTouchListener { _, event ->
                    try {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                isDragging = false
                                longPressTriggered = false
                                initialX = params.x
                                initialY = params.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                                view.handler?.postDelayed(longPressRunnable, LONG_PRESS_DURATION)
                                
                                // Visual feedback on touch
                                view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val deltaX = (event.rawX - initialTouchX).toInt()
                                val deltaY = (event.rawY - initialTouchY).toInt()
                                
                                if (kotlin.math.abs(deltaX) > DRAG_THRESHOLD || 
                                    kotlin.math.abs(deltaY) > DRAG_THRESHOLD) {
                                    isDragging = true
                                    view.handler?.removeCallbacks(longPressRunnable)
                                }
                                
                                if (isDragging || longPressTriggered) {
                                    params.x = initialX + deltaX
                                    params.y = initialY + deltaY
                                    try {
                                        windowManager.updateViewLayout(view, params)
                                    } catch (e: Exception) {
                                        // View might be detached
                                    }
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                view.handler?.removeCallbacks(longPressRunnable)
                                
                                // Reset visual feedback
                                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                                
                                if (!isDragging && !longPressTriggered) {
                                    // Single tap - activate and perform capture
                                    handleButtonClick()
                                } else if (longPressTriggered) {
                                    // Long press ended - snap to edge
                                    snapToEdge(params)
                                }
                                true
                            }
                            else -> false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingOverlay", "Touch error: ${e.message}")
                        false
                    }
                }

                // Setup realtime toggle button with safe click handling
                floatingButtonBinding?.btnRealtime?.setOnClickListener {
                    try {
                        toggleRealtimeMode()
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingOverlay", "Realtime toggle error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Setup floating button error: ${e.message}")
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle button click - activate button and perform action
     */
    private fun handleButtonClick() {
        android.util.Log.d("FloatingOverlay", "handleButtonClick called, isServiceReady=$isServiceReady")
        
        if (!isServiceReady) {
            Toast.makeText(this, R.string.wait_initializing, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if capture is initialized
        if (!captureManager.isInitialized()) {
            android.util.Log.e("FloatingOverlay", "CaptureManager not initialized in handleButtonClick")
            Toast.makeText(this, "Screen capture not ready. Please restart from app.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Set button as active
        if (!isButtonActive) {
            isButtonActive = true
            updateButtonVisualState()
        }
        
        // Perform capture
        performCapture()
    }
    
    /**
     * Update button visual state based on active/inactive status
     */
    private fun updateButtonVisualState() {
        floatingButtonBinding?.apply {
            try {
                if (isServiceReady && isButtonActive) {
                    // Active state - full opacity, green indicator with pulse
                    root.alpha = 1.0f
                    statusIndicator.isVisible = true
                    
                    // Apply pulse animation to status indicator
                    val pulseAnim = android.view.animation.AnimationUtils.loadAnimation(
                        this@FloatingOverlayService, R.anim.pulse
                    )
                    statusIndicator.startAnimation(pulseAnim)
                    
                } else if (isServiceReady) {
                    // Ready but inactive - slightly dimmed, no indicator
                    root.alpha = 0.85f
                    statusIndicator.clearAnimation()
                    statusIndicator.isVisible = false
                } else {
                    // Not ready - more dimmed, no indicator
                    root.alpha = 0.6f
                    statusIndicator.clearAnimation()
                    statusIndicator.isVisible = false
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingOverlay", "Update visual state error: ${e.message}")
            }
        }
    }

    private fun createFloatingLayoutParams(): WindowManager.LayoutParams {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        val targetX = if (params.x < screenWidth / 2) {
            0
        } else {
            screenWidth - (floatingButtonBinding?.root?.width ?: 0)
        }

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200
            interpolator = OvershootInterpolator()
            addUpdateListener { animator ->
                params.x = animator.animatedValue as Int
                floatingButtonBinding?.root?.let { view ->
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        // View might be removed
                    }
                }
            }
            start()
        }
    }

    private fun performCapture() {
        if (isProcessing) {
            android.util.Log.d("FloatingOverlay", "Already processing, skipping capture")
            return
        }
        
        android.util.Log.d("FloatingOverlay", "performCapture called, checking initialization...")
        
        if (!captureManager.isInitialized()) {
            android.util.Log.e("FloatingOverlay", "CaptureManager not initialized!")
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show()
            // Try to inform user to restart
            Toast.makeText(this, "Please restart the service from app", Toast.LENGTH_LONG).show()
            return
        }

        android.util.Log.d("FloatingOverlay", "Starting capture...")
        isProcessing = true
        updateButtonState(processing = true)

        serviceScope.launch {
            try {
                // Hide floating button during capture
                withContext(Dispatchers.Main) {
                    floatingButtonBinding?.root?.alpha = 0f
                }
                
                delay(100) // Small delay for UI update

                // Capture screen
                val captureResult = captureManager.captureScreen()
                
                // Show floating button again
                withContext(Dispatchers.Main) {
                    floatingButtonBinding?.root?.alpha = 1f
                }

                captureResult.fold(
                    onSuccess = { bitmap ->
                        // Process image
                        val processed = imageProcessor.processForOCR(
                            bitmap,
                            preferencesManager.roiWidthPercent,
                            preferencesManager.roiHeightPercent
                        )

                        // Perform OCR
                        val ocrResult = ocrRepository.recognizeText(processed)

                        // Parse coordinates
                        val coordinates = textParser.extractCoordinates(ocrResult.fullText)

                        // Show result
                        withContext(Dispatchers.Main) {
                            showResult(ocrResult.fullText, coordinates)
                        }
                    },
                    onFailure = { error ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@FloatingOverlayService,
                                getString(R.string.error_capture_failed) + ": ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    floatingButtonBinding?.root?.alpha = 1f
                    Toast.makeText(
                        this@FloatingOverlayService,
                        getString(R.string.error_unknown) + ": ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isProcessing = false
                withContext(Dispatchers.Main) {
                    updateButtonState(processing = false)
                }
            }
        }
    }

    private fun toggleRealtimeMode() {
        if (!isServiceReady) {
            Toast.makeText(this, R.string.wait_initializing, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!captureManager.isInitialized()) {
            Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_SHORT).show()
            return
        }
        
        isRealtimeMode = !isRealtimeMode
        
        // Mark button as active when realtime mode is enabled
        if (isRealtimeMode && !isButtonActive) {
            isButtonActive = true
        }
        
        floatingButtonBinding?.apply {
            try {
                btnRealtime.isSelected = isRealtimeMode
                statusIndicator.isVisible = isRealtimeMode
            } catch (e: Exception) {
                android.util.Log.e("FloatingOverlay", "Toggle UI error: ${e.message}")
            }
        }

        if (isRealtimeMode) {
            startRealtimeCapture()
        } else {
            realtimeJob?.cancel()
            realtimeJob = null
        }
        
        updateButtonVisualState()
    }

    private fun startRealtimeCapture() {
        realtimeJob?.cancel()
        realtimeJob = serviceScope.launch {
            try {
                captureManager.continuousCapture(
                    preferencesManager.captureInterval.toLong()
                ).collectLatest { bitmap ->
                    try {
                        val processed = imageProcessor.processForOCR(
                            bitmap,
                            preferencesManager.roiWidthPercent,
                            preferencesManager.roiHeightPercent
                        )
                        
                        val ocrResult = ocrRepository.recognizeText(processed)
                        val coordinates = textParser.extractCoordinates(ocrResult.fullText)
                        
                        // Only update if coordinates changed
                        if (coordinates.success && 
                            coordinates.formattedOutput != lastCoordinates) {
                            lastCoordinates = coordinates.formattedOutput
                            withContext(Dispatchers.Main) {
                                showResult(ocrResult.fullText, coordinates)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore individual frame errors in realtime mode
                        android.util.Log.w("FloatingOverlay", "Realtime frame error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FloatingOverlay", "Realtime capture error: ${e.message}")
                withContext(Dispatchers.Main) {
                    isRealtimeMode = false
                    floatingButtonBinding?.apply {
                        btnRealtime.isSelected = false
                        statusIndicator.isVisible = false
                    }
                    Toast.makeText(
                        this@FloatingOverlayService,
                        R.string.error_capture_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showResult(rawText: String, coordinates: CoordinateResult) {
        try {
            if (resultCardBinding == null) {
                setupResultCard()
            }

            resultCardBinding?.apply {
                if (coordinates.success) {
                    tvRawText.text = coordinates.rawMatch ?: rawText.take(100)
                    tvCoordinates.text = coordinates.formattedOutput
                    tvCoordinates.isVisible = true
                    btnCopy.isVisible = true
                    btnMaps.isVisible = true
                    ivStatus.setImageResource(R.drawable.ic_check)
                } else {
                    tvRawText.text = if (rawText.isNotEmpty()) {
                        rawText.take(150)
                    } else {
                        getString(R.string.no_coordinates_found)
                    }
                    tvCoordinates.isVisible = false
                    btnCopy.isVisible = false
                    btnMaps.isVisible = false
                    ivStatus.setImageResource(R.drawable.ic_error)
                }

                // Setup button clicks
                btnCopy.setOnClickListener {
                    try {
                        coordinates.formattedOutput?.let { text ->
                            copyToClipboard(text)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingOverlay", "Copy error: ${e.message}")
                    }
                }

                btnMaps.setOnClickListener {
                    try {
                        if (coordinates.latitude != null && coordinates.longitude != null) {
                            openInMaps(coordinates.latitude, coordinates.longitude)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingOverlay", "Maps error: ${e.message}")
                    }
                }

                btnClose.setOnClickListener {
                    hideResultCard()
                }

                root.isVisible = true
                animateResultCard(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Show result error: ${e.message}")
        }
    }

    private fun setupResultCard() {
        try {
            resultCardBinding = LayoutResultCardBinding.inflate(
                LayoutInflater.from(this)
            )

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            resultCardBinding?.root?.let { view ->
                windowManager.addView(view, params)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Setup result card error: ${e.message}")
            resultCardBinding = null
        }
    }

    private fun animateResultCard(show: Boolean) {
        resultCardBinding?.root?.let { view ->
            if (show) {
                view.translationY = 200f
                view.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            } else {
                view.animate()
                    .translationY(200f)
                    .setDuration(200)
                    .withEndAction {
                        view.isVisible = false
                    }
                    .start()
            }
        }
    }

    private fun hideResultCard() {
        animateResultCard(false)
    }

    private fun updateButtonState(processing: Boolean) {
        try {
            floatingButtonBinding?.apply {
                progressIndicator.isVisible = processing
                ivCapture.isVisible = !processing
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Update button state error: ${e.message}")
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("coordinates", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun openInMaps(latitude: Double, longitude: Double) {
        val uri = textParser.generateMapsIntent(latitude, longitude)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web URL
            val webUrl = textParser.generateMapsUrl(latitude, longitude)
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(webIntent)
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun removeOverlays() {
        android.util.Log.d("FloatingOverlay", "removeOverlays called")
        try {
            floatingButtonBinding?.root?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                    android.util.Log.d("FloatingOverlay", "Floating button removed")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Error removing floating button: ${e.message}")
        }
        
        try {
            resultCardBinding?.root?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                    android.util.Log.d("FloatingOverlay", "Result card removed")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingOverlay", "Error removing result card: ${e.message}")
        }
        
        floatingButtonBinding = null
        resultCardBinding = null
    }

    private fun createNotification(): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, pendingIntentFlags
        )

        val stopIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CoordinateExtractorApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.notification_action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
