package com.coordextractor.app.parser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for extracting and converting GPS coordinates from OCR text
 * Handles various coordinate formats and OCR misreadings
 */
@Singleton
class TextParser @Inject constructor() {

    companion object {
        // Regex patterns for coordinate extraction
        // Pattern 1: Standard format with space - "22.1234N 71.1234E"
        private val PATTERN_STANDARD = Regex(
            """(\d{1,2}[.,]\d+)\s*([NSns])\s*(\d{1,3}[.,]\d+)\s*([EWew])"""
        )
        
        // Pattern 2: No space format - "22.1234N71.1234E"
        private val PATTERN_NO_SPACE = Regex(
            """(\d{1,2}[.,]\d+)([NSns])(\d{1,3}[.,]\d+)([EWew])"""
        )
        
        // Pattern 3: Decimal format - "22.1234, 71.1234" or "22.1234,71.1234"
        private val PATTERN_DECIMAL = Regex(
            """(-?\d{1,2}[.,]\d+)\s*[,]\s*(-?\d{1,3}[.,]\d+)"""
        )
        
        // Pattern 4: DMS format - "22°12'34"N 71°12'34"E"
        private val PATTERN_DMS = Regex(
            """(\d{1,2})[°]\s*(\d{1,2})[''′]\s*(\d{1,2}(?:[.,]\d+)?)[""″]?\s*([NSns])\s*(\d{1,3})[°]\s*(\d{1,2})[''′]\s*(\d{1,2}(?:[.,]\d+)?)[""″]?\s*([EWew])"""
        )

        // Common OCR misreadings corrections
        private val OCR_CORRECTIONS = mapOf(
            "0" to "O",  // Sometimes O is read as 0
            "l" to "1",  // Sometimes 1 is read as l
            "I" to "1",  // Sometimes 1 is read as I
            "S" to "5",  // Sometimes 5 is read as S
            "B" to "8",  // Sometimes 8 is read as B
            "O" to "0",  // Sometimes 0 is read as O
        )
    }

    /**
     * Extract coordinates from OCR text
     * Tries multiple patterns and returns the first match
     */
    fun extractCoordinates(text: String): CoordinateResult {
        // Clean and normalize text
        val cleanedText = cleanText(text)
        
        // Try each pattern
        var result = tryStandardPattern(cleanedText)
        if (result.success) return result
        
        result = tryNoSpacePattern(cleanedText)
        if (result.success) return result
        
        result = tryDecimalPattern(cleanedText)
        if (result.success) return result
        
        result = tryDMSPattern(cleanedText)
        if (result.success) return result
        
        // Try with OCR corrections
        val correctedText = applyOCRCorrections(cleanedText)
        if (correctedText != cleanedText) {
            result = tryStandardPattern(correctedText)
            if (result.success) return result
            
            result = tryNoSpacePattern(correctedText)
            if (result.success) return result
        }
        
        return CoordinateResult(
            success = false,
            rawMatch = null,
            latitude = null,
            longitude = null,
            formattedOutput = null,
            error = "No coordinates found in text"
        )
    }

    /**
     * Clean and normalize input text
     */
    private fun cleanText(text: String): String {
        return text
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Apply common OCR corrections to text
     */
    private fun applyOCRCorrections(text: String): String {
        var result = text
        
        // Fix common coordinate-specific misreadings
        // N/M confusion
        result = result.replace(Regex("""(\d[.,]\d+)\s*[Mm]\s*(\d)""")) { match ->
            "${match.groupValues[1]}N ${match.groupValues[2]}"
        }
        
        // E/F confusion
        result = result.replace(Regex("""(\d[.,]\d+)\s*[Ff](\s|$)""")) { match ->
            "${match.groupValues[1]}E${match.groupValues[2]}"
        }
        
        return result
    }

    /**
     * Try standard pattern: "22.1234N 71.1234E"
     */
    private fun tryStandardPattern(text: String): CoordinateResult {
        val match = PATTERN_STANDARD.find(text) ?: return CoordinateResult.empty()
        
        return parseMatchGroups(
            rawMatch = match.value,
            latValue = match.groupValues[1],
            latDir = match.groupValues[2],
            lonValue = match.groupValues[3],
            lonDir = match.groupValues[4]
        )
    }

    /**
     * Try no-space pattern: "22.1234N71.1234E"
     */
    private fun tryNoSpacePattern(text: String): CoordinateResult {
        val match = PATTERN_NO_SPACE.find(text) ?: return CoordinateResult.empty()
        
        return parseMatchGroups(
            rawMatch = match.value,
            latValue = match.groupValues[1],
            latDir = match.groupValues[2],
            lonValue = match.groupValues[3],
            lonDir = match.groupValues[4]
        )
    }

    /**
     * Try decimal pattern: "22.1234, 71.1234"
     */
    private fun tryDecimalPattern(text: String): CoordinateResult {
        val match = PATTERN_DECIMAL.find(text) ?: return CoordinateResult.empty()
        
        val latValue = match.groupValues[1].replace(",", ".").toDoubleOrNull()
        val lonValue = match.groupValues[2].replace(",", ".").toDoubleOrNull()
        
        if (latValue == null || lonValue == null) return CoordinateResult.empty()
        
        // Validate ranges
        if (latValue !in -90.0..90.0 || lonValue !in -180.0..180.0) {
            return CoordinateResult.empty()
        }
        
        return CoordinateResult(
            success = true,
            rawMatch = match.value,
            latitude = latValue,
            longitude = lonValue,
            formattedOutput = formatCoordinates(latValue, lonValue)
        )
    }

    /**
     * Try DMS pattern: "22°12'34"N 71°12'34"E"
     */
    private fun tryDMSPattern(text: String): CoordinateResult {
        val match = PATTERN_DMS.find(text) ?: return CoordinateResult.empty()
        
        try {
            val latDeg = match.groupValues[1].toDouble()
            val latMin = match.groupValues[2].toDouble()
            val latSec = match.groupValues[3].replace(",", ".").toDouble()
            val latDir = match.groupValues[4]
            
            val lonDeg = match.groupValues[5].toDouble()
            val lonMin = match.groupValues[6].toDouble()
            val lonSec = match.groupValues[7].replace(",", ".").toDouble()
            val lonDir = match.groupValues[8]
            
            // Convert DMS to decimal
            var latitude = latDeg + latMin / 60 + latSec / 3600
            var longitude = lonDeg + lonMin / 60 + lonSec / 3600
            
            // Apply direction
            if (latDir.uppercase() == "S") latitude = -latitude
            if (lonDir.uppercase() == "W") longitude = -longitude
            
            return CoordinateResult(
                success = true,
                rawMatch = match.value,
                latitude = latitude,
                longitude = longitude,
                formattedOutput = formatCoordinates(latitude, longitude)
            )
        } catch (e: Exception) {
            return CoordinateResult.empty()
        }
    }

    /**
     * Parse match groups into CoordinateResult
     */
    private fun parseMatchGroups(
        rawMatch: String,
        latValue: String,
        latDir: String,
        lonValue: String,
        lonDir: String
    ): CoordinateResult {
        try {
            var latitude = latValue.replace(",", ".").toDouble()
            var longitude = lonValue.replace(",", ".").toDouble()
            
            // Apply direction
            if (latDir.uppercase() == "S") latitude = -latitude
            if (lonDir.uppercase() == "W") longitude = -longitude
            
            // Validate ranges
            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                return CoordinateResult.empty()
            }
            
            return CoordinateResult(
                success = true,
                rawMatch = rawMatch,
                latitude = latitude,
                longitude = longitude,
                formattedOutput = formatCoordinates(latitude, longitude)
            )
        } catch (e: Exception) {
            return CoordinateResult.empty()
        }
    }

    /**
     * Format coordinates as "lat,lon"
     */
    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "%.6f,%.6f".format(latitude, longitude)
    }

    /**
     * Format coordinates for display with direction indicators
     */
    fun formatForDisplay(latitude: Double, longitude: Double): String {
        val latDir = if (latitude >= 0) "N" else "S"
        val lonDir = if (longitude >= 0) "E" else "W"
        
        return "%.6f°%s %.6f°%s".format(
            kotlin.math.abs(latitude),
            latDir,
            kotlin.math.abs(longitude),
            lonDir
        )
    }

    /**
     * Generate Google Maps URL
     */
    fun generateMapsUrl(latitude: Double, longitude: Double): String {
        return "https://www.google.com/maps?q=$latitude,$longitude"
    }

    /**
     * Generate Google Maps intent URI
     */
    fun generateMapsIntent(latitude: Double, longitude: Double): String {
        return "geo:$latitude,$longitude?q=$latitude,$longitude"
    }
}

/**
 * Result of coordinate extraction
 */
data class CoordinateResult(
    val success: Boolean,
    val rawMatch: String?,
    val latitude: Double?,
    val longitude: Double?,
    val formattedOutput: String?,
    val error: String? = null
) {
    companion object {
        fun empty() = CoordinateResult(
            success = false,
            rawMatch = null,
            latitude = null,
            longitude = null,
            formattedOutput = null
        )
    }
}
