package com.coordextractor.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing extracted coordinates
 */
@Parcelize
data class Coordinate(
    val latitude: Double,
    val longitude: Double,
    val rawText: String,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val source: CoordinateSource = CoordinateSource.OCR
) : Parcelable {
    
    /**
     * Returns formatted coordinate string: "lat,lng"
     */
    val formatted: String
        get() = "$latitude,$longitude"
    
    /**
     * Returns display string with direction indicators
     */
    val displayString: String
        get() {
            val latDir = if (latitude >= 0) "N" else "S"
            val lonDir = if (longitude >= 0) "E" else "W"
            return "${kotlin.math.abs(latitude)}$latDir ${kotlin.math.abs(longitude)}$lonDir"
        }
    
    /**
     * Returns Google Maps URL
     */
    val mapsUrl: String
        get() = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    
    /**
     * Returns geo URI for map intents
     */
    val geoUri: String
        get() = "geo:$latitude,$longitude?q=$latitude,$longitude"
    
    /**
     * Check if coordinates are valid
     */
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0
    
    /**
     * Check if confidence is above threshold
     */
    fun hasHighConfidence(threshold: Float = 0.7f): Boolean = confidence >= threshold
    
    companion object {
        val EMPTY = Coordinate(0.0, 0.0, "", 0f)
        
        fun fromString(coordString: String, rawText: String = ""): Coordinate? {
            return try {
                val parts = coordString.split(",")
                if (parts.size != 2) return null
                val lat = parts[0].trim().toDouble()
                val lon = parts[1].trim().toDouble()
                Coordinate(lat, lon, rawText.ifEmpty { coordString })
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Enum for coordinate source
 */
enum class CoordinateSource {
    OCR,
    MANUAL,
    GPS,
    CLIPBOARD
}

/**
 * Data class for OCR result
 */
data class OCRResult(
    val text: String,
    val confidence: Float,
    val processingTimeMs: Long,
    val boundingBox: android.graphics.Rect? = null
)

/**
 * Data class for extraction result
 */
data class ExtractionResult(
    val coordinate: Coordinate?,
    val rawText: String,
    val allMatches: List<String>,
    val processingTimeMs: Long,
    val success: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(coordinate: Coordinate, rawText: String, matches: List<String>, timeMs: Long) =
            ExtractionResult(coordinate, rawText, matches, timeMs, true)
        
        fun failure(rawText: String, error: String, timeMs: Long) =
            ExtractionResult(null, rawText, emptyList(), timeMs, false, error)
        
        fun empty() = ExtractionResult(null, "", emptyList(), 0, false, "No text extracted")
    }
}

/**
 * UI State for coordinate display
 */
sealed class CoordinateUiState {
    data object Idle : CoordinateUiState()
    data object Loading : CoordinateUiState()
    data class Success(val result: ExtractionResult) : CoordinateUiState()
    data class Error(val message: String) : CoordinateUiState()
    
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}

/**
 * Settings state
 */
data class AppSettings(
    val realtimeMode: Boolean = false,
    val captureIntervalMs: Long = 500L,
    val roiWidthPercent: Float = 0.35f,
    val roiHeightPercent: Float = 0.25f,
    val autoCopy: Boolean = false,
    val vibrateOnSuccess: Boolean = true,
    val soundOnSuccess: Boolean = false,
    val ocrConfidenceThreshold: Float = 0.7f
)
