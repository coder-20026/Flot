package com.coordextractor.app.util

/**
 * Application-wide constants
 */
object Constants {
    
    // Notification Constants
    const val NOTIFICATION_CHANNEL_ID = "coordinate_extractor_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Coordinate Extractor"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1001
    const val CAPTURE_SERVICE_NOTIFICATION_ID = 1002
    
    // Intent Actions
    const val ACTION_START_CAPTURE = "com.coordextractor.ACTION_START_CAPTURE"
    const val ACTION_STOP_CAPTURE = "com.coordextractor.ACTION_STOP_CAPTURE"
    const val ACTION_TOGGLE_REALTIME = "com.coordextractor.ACTION_TOGGLE_REALTIME"
    const val ACTION_SHOW_RESULT = "com.coordextractor.ACTION_SHOW_RESULT"
    const val ACTION_HIDE_OVERLAY = "com.coordextractor.ACTION_HIDE_OVERLAY"
    
    // Intent Extras
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_COORDINATES = "extra_coordinates"
    const val EXTRA_RAW_TEXT = "extra_raw_text"
    
    // Request Codes
    const val REQUEST_MEDIA_PROJECTION = 100
    const val REQUEST_OVERLAY_PERMISSION = 101
    const val REQUEST_NOTIFICATION_PERMISSION = 102
    
    // Broadcast Actions
    const val BROADCAST_COORDINATES_EXTRACTED = "com.coordextractor.COORDINATES_EXTRACTED"
    const val BROADCAST_CAPTURE_STARTED = "com.coordextractor.CAPTURE_STARTED"
    const val BROADCAST_CAPTURE_STOPPED = "com.coordextractor.CAPTURE_STOPPED"
    const val BROADCAST_ERROR = "com.coordextractor.ERROR"
    
    // SharedPreferences Keys
    object Prefs {
        const val PREF_NAME = "coordinate_extractor_prefs"
        const val KEY_REALTIME_MODE = "realtime_mode"
        const val KEY_CAPTURE_INTERVAL = "capture_interval"
        const val KEY_ROI_WIDTH_PERCENT = "roi_width_percent"
        const val KEY_ROI_HEIGHT_PERCENT = "roi_height_percent"
        const val KEY_AUTO_COPY = "auto_copy"
        const val KEY_VIBRATE_ON_SUCCESS = "vibrate_on_success"
        const val KEY_SOUND_ON_SUCCESS = "sound_on_success"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_FLOATING_BUTTON_X = "floating_button_x"
        const val KEY_FLOATING_BUTTON_Y = "floating_button_y"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_LAST_COORDINATES = "last_coordinates"
        const val KEY_OCR_CONFIDENCE_THRESHOLD = "ocr_confidence_threshold"
    }
    
    // Default Values
    object Defaults {
        const val CAPTURE_INTERVAL_MS = 500L
        const val ROI_WIDTH_PERCENT = 0.35f
        const val ROI_HEIGHT_PERCENT = 0.25f
        const val OCR_CONFIDENCE_THRESHOLD = 0.7f
        const val REALTIME_MODE = false
        const val AUTO_COPY = false
        const val VIBRATE_ON_SUCCESS = true
        const val SOUND_ON_SUCCESS = false
    }
    
    // OCR Related
    object OCR {
        const val MIN_TEXT_SIZE = 8
        const val MAX_PROCESSING_TIME_MS = 5000L
        const val BITMAP_MAX_DIMENSION = 1920
        const val DOWNSCALE_FACTOR = 0.75f
    }
    
    // Animation Durations
    object Animation {
        const val FAST = 150L
        const val NORMAL = 300L
        const val SLOW = 500L
    }
    
    // Coordinate Patterns
    object Patterns {
        // Standard pattern: 22.1234N 71.1234E
        const val COORD_PATTERN_STANDARD = """(\d{1,2}[.,]\d+)\s*([NS])\s*(\d{1,3}[.,]\d+)\s*([EW])"""
        
        // No space pattern: 22.1234N71.1234E
        const val COORD_PATTERN_NO_SPACE = """(\d{1,2}[.,]\d+)([NS])(\d{1,3}[.,]\d+)([EW])"""
        
        // Decimal degrees: 22.1234, 71.1234
        const val COORD_PATTERN_DECIMAL = """(-?\d{1,2}[.,]\d+)\s*,\s*(-?\d{1,3}[.,]\d+)"""
        
        // DMS pattern: 22°07'24.2"N 71°07'24.2"E
        const val COORD_PATTERN_DMS = """(\d{1,2})°(\d{1,2})'(\d{1,2}(?:[.,]\d+)?)"([NS])\s*(\d{1,3})°(\d{1,2})'(\d{1,2}(?:[.,]\d+)?)"([EW])"""
    }
    
    // Error Messages
    object Errors {
        const val PERMISSION_DENIED = "Permission denied"
        const val CAPTURE_FAILED = "Screen capture failed"
        const val OCR_FAILED = "Text recognition failed"
        const val NO_COORDINATES = "No coordinates found"
        const val INVALID_FORMAT = "Invalid coordinate format"
        const val SERVICE_ERROR = "Service error occurred"
    }
}
