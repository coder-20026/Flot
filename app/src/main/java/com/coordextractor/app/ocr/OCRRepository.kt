package com.coordextractor.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for OCR operations using ML Kit
 * Handles text recognition with confidence filtering
 */
@Singleton
class OCRRepository @Inject constructor() {

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    companion object {
        // Minimum confidence threshold for text blocks
        const val MIN_CONFIDENCE_THRESHOLD = 0.7f
    }

    /**
     * Perform text recognition on a bitmap
     * @param bitmap Input image
     * @return OCRResult containing recognized text and metadata
     */
    suspend fun recognizeText(bitmap: Bitmap): OCRResult = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val blocks = visionText.textBlocks.map { block ->
                    TextBlock(
                        text = block.text,
                        confidence = block.lines.firstOrNull()?.confidence ?: 0f,
                        boundingBox = block.boundingBox
                    )
                }
                
                // Filter low confidence blocks
                val filteredBlocks = blocks.filter { 
                    it.confidence >= MIN_CONFIDENCE_THRESHOLD || it.confidence == 0f 
                }
                
                val result = OCRResult(
                    fullText = fullText,
                    textBlocks = filteredBlocks,
                    success = true
                )
                
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }

    /**
     * Perform text recognition and return only the raw text
     */
    suspend fun recognizeTextSimple(bitmap: Bitmap): String {
        return recognizeText(bitmap).fullText
    }

    /**
     * Close the recognizer when done
     */
    fun close() {
        textRecognizer.close()
    }
}

/**
 * Result of OCR processing
 */
data class OCRResult(
    val fullText: String,
    val textBlocks: List<TextBlock>,
    val success: Boolean,
    val error: String? = null
)

/**
 * Individual text block from OCR
 */
data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?
)
