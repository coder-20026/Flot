package com.coordextractor.app.capture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages screen capture using MediaProjection API
 * Handles virtual display creation and bitmap extraction
 */
@Singleton
class CaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val displayMetrics: DisplayMetrics
        get() {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return DisplayMetrics().also { 
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(it) 
            }
        }

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0

    // Bitmap pool for reuse
    private var reusableBitmap: Bitmap? = null

    init {
        updateScreenMetrics()
    }

    private fun updateScreenMetrics() {
        displayMetrics.let {
            screenWidth = it.widthPixels
            screenHeight = it.heightPixels
            screenDensity = it.densityDpi
        }
    }

    /**
     * Initialize MediaProjection with the result from permission request
     */
    fun initializeProjection(resultCode: Int, data: Intent) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                cleanup()
            }
        }, handler)
    }

    /**
     * Check if MediaProjection is initialized and ready
     */
    fun isInitialized(): Boolean = mediaProjection != null

    /**
     * Capture a single frame from the screen
     */
    @SuppressLint("WrongConstant")
    suspend fun captureScreen(): Result<Bitmap> = suspendCancellableCoroutine { continuation ->
        if (mediaProjection == null) {
            continuation.resumeWithException(
                IllegalStateException("MediaProjection not initialized")
            )
            return@suspendCancellableCoroutine
        }

        updateScreenMetrics()
        
        try {
            // Create ImageReader for screen capture
            imageReader?.close()
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            // Create virtual display
            virtualDisplay?.release()
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )

            // Capture image with a small delay for rendering
            handler.postDelayed({
                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image)
                        image.close()
                        
                        // Release virtual display after capture
                        virtualDisplay?.release()
                        virtualDisplay = null
                        
                        if (continuation.isActive) {
                            continuation.resume(Result.success(bitmap))
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                RuntimeException("Failed to acquire image")
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }, 100)

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }

        continuation.invokeOnCancellation {
            virtualDisplay?.release()
            virtualDisplay = null
        }
    }

    /**
     * Continuous capture flow for real-time mode
     */
    @SuppressLint("WrongConstant")
    fun continuousCapture(intervalMs: Long = 500L): Flow<Bitmap> = callbackFlow {
        if (mediaProjection == null) {
            throw IllegalStateException("MediaProjection not initialized")
        }

        updateScreenMetrics()

        imageReader?.close()
        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay?.release()
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        val captureRunnable = object : Runnable {
            override fun run() {
                try {
                    val image = imageReader?.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image)
                        image.close()
                        trySend(bitmap)
                    }
                    handler.postDelayed(this, intervalMs)
                } catch (e: Exception) {
                    // Ignore and continue
                }
            }
        }

        handler.postDelayed(captureRunnable, intervalMs)

        awaitClose {
            handler.removeCallbacks(captureRunnable)
            virtualDisplay?.release()
            virtualDisplay = null
        }
    }

    /**
     * Convert Image to Bitmap efficiently with bitmap reuse
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val width = image.width + rowPadding / pixelStride
        val height = image.height

        // Reuse bitmap if possible
        val bitmap = if (reusableBitmap != null && 
            reusableBitmap!!.width == width && 
            reusableBitmap!!.height == height &&
            !reusableBitmap!!.isRecycled
        ) {
            reusableBitmap!!
        } else {
            reusableBitmap?.recycle()
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                reusableBitmap = it
            }
        }

        bitmap.copyPixelsFromBuffer(buffer)

        // Crop to actual screen size
        return if (width != image.width) {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    /**
     * Stop the MediaProjection
     */
    fun stopProjection() {
        mediaProjection?.stop()
        cleanup()
    }

    /**
     * Clean up resources
     */
    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        reusableBitmap?.recycle()
        reusableBitmap = null
        
        mediaProjection = null
    }

    /**
     * Get current screen dimensions
     */
    fun getScreenDimensions(): Pair<Int, Int> {
        updateScreenMetrics()
        return Pair(screenWidth, screenHeight)
    }
}
