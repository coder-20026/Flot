package com.coordextractor.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image processing utilities for OCR optimization
 * Handles ROI cropping, downscaling, and preprocessing
 */
@Singleton
class ImageProcessor @Inject constructor() {

    companion object {
        // Default ROI settings (bottom-right area)
        const val DEFAULT_ROI_WIDTH_PERCENT = 30
        const val DEFAULT_ROI_HEIGHT_PERCENT = 25
        
        // Maximum dimension for OCR processing
        const val MAX_OCR_DIMENSION = 1024
        
        // Contrast enhancement factor
        const val CONTRAST_FACTOR = 1.2f
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    /**
     * Crop the bottom-right region of interest from the bitmap
     * @param bitmap Source bitmap
     * @param widthPercent Width percentage of ROI (0-100)
     * @param heightPercent Height percentage of ROI (0-100)
     * @return Cropped bitmap
     */
    fun cropBottomRightROI(
        bitmap: Bitmap,
        widthPercent: Int = DEFAULT_ROI_WIDTH_PERCENT,
        heightPercent: Int = DEFAULT_ROI_HEIGHT_PERCENT
    ): Bitmap {
        val roiWidth = (bitmap.width * widthPercent / 100).coerceAtLeast(100)
        val roiHeight = (bitmap.height * heightPercent / 100).coerceAtLeast(50)
        
        val x = (bitmap.width - roiWidth).coerceAtLeast(0)
        val y = (bitmap.height - roiHeight).coerceAtLeast(0)
        
        return Bitmap.createBitmap(
            bitmap,
            x,
            y,
            roiWidth.coerceAtMost(bitmap.width - x),
            roiHeight.coerceAtMost(bitmap.height - y)
        )
    }

    /**
     * Crop custom ROI based on coordinates
     */
    fun cropCustomROI(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Bitmap {
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceIn(1, bitmap.width - safeX)
        val safeHeight = height.coerceIn(1, bitmap.height - safeY)
        
        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }

    /**
     * Downscale bitmap for faster OCR processing
     */
    fun downscaleForOCR(bitmap: Bitmap, maxDimension: Int = MAX_OCR_DIMENSION): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Preprocess image for better OCR accuracy
     * - Convert to grayscale
     * - Increase contrast
     */
    fun preprocessForOCR(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Create grayscale + contrast matrix
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Grayscale
            
            // Increase contrast
            val contrast = CONTRAST_FACTOR
            val translate = (1 - contrast) / 2 * 255
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(contrastMatrix)
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.colorFilter = null
        
        return result
    }

    /**
     * Full processing pipeline for OCR
     * 1. Crop bottom-right ROI
     * 2. Downscale if needed
     * 3. Preprocess for better accuracy
     */
    fun processForOCR(
        bitmap: Bitmap,
        roiWidthPercent: Int = DEFAULT_ROI_WIDTH_PERCENT,
        roiHeightPercent: Int = DEFAULT_ROI_HEIGHT_PERCENT,
        preprocess: Boolean = true
    ): Bitmap {
        // Step 1: Crop ROI
        var processed = cropBottomRightROI(bitmap, roiWidthPercent, roiHeightPercent)
        
        // Step 2: Downscale
        processed = downscaleForOCR(processed)
        
        // Step 3: Preprocess (optional)
        if (preprocess) {
            processed = preprocessForOCR(processed)
        }
        
        return processed
    }

    /**
     * Calculate ROI coordinates for display overlay
     */
    fun calculateROICoordinates(
        screenWidth: Int,
        screenHeight: Int,
        widthPercent: Int = DEFAULT_ROI_WIDTH_PERCENT,
        heightPercent: Int = DEFAULT_ROI_HEIGHT_PERCENT
    ): ROICoordinates {
        val roiWidth = screenWidth * widthPercent / 100
        val roiHeight = screenHeight * heightPercent / 100
        val x = screenWidth - roiWidth
        val y = screenHeight - roiHeight
        
        return ROICoordinates(x, y, roiWidth, roiHeight)
    }

    data class ROICoordinates(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}
