package com.coordextractor.app.util

import android.content.Context
import android.content.SharedPreferences
import com.coordextractor.app.model.Coordinate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class for coordinate history
 */
class HistoryManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _historyFlow = MutableStateFlow<List<Coordinate>>(emptyList())
    val historyFlow: StateFlow<List<Coordinate>> = _historyFlow.asStateFlow()
    
    init {
        loadHistory()
    }
    
    /**
     * Add a coordinate to history
     */
    fun addToHistory(coordinate: Coordinate) {
        val currentList = _historyFlow.value.toMutableList()
        
        // Remove duplicate if exists
        currentList.removeAll { it.formatted == coordinate.formatted }
        
        // Add to beginning
        currentList.add(0, coordinate)
        
        // Keep only last MAX_HISTORY_SIZE items
        if (currentList.size > MAX_HISTORY_SIZE) {
            currentList.removeAt(currentList.lastIndex)
        }
        
        _historyFlow.value = currentList
        saveHistory()
    }
    
    /**
     * Remove a coordinate from history
     */
    fun removeFromHistory(coordinate: Coordinate) {
        val currentList = _historyFlow.value.toMutableList()
        currentList.removeAll { it.timestamp == coordinate.timestamp }
        _historyFlow.value = currentList
        saveHistory()
    }
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    /**
     * Get latest coordinate
     */
    fun getLatest(): Coordinate? = _historyFlow.value.firstOrNull()
    
    /**
     * Get all history
     */
    fun getAll(): List<Coordinate> = _historyFlow.value
    
    private fun loadHistory() {
        val json = prefs.getString(KEY_HISTORY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<CoordinateData>>() {}.type
                val dataList: List<CoordinateData> = gson.fromJson(json, type)
                _historyFlow.value = dataList.map { it.toCoordinate() }
            } catch (e: Exception) {
                _historyFlow.value = emptyList()
            }
        }
    }
    
    private fun saveHistory() {
        val dataList = _historyFlow.value.map { CoordinateData.fromCoordinate(it) }
        val json = gson.toJson(dataList)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
    
    /**
     * Serializable data class for Coordinate
     */
    private data class CoordinateData(
        val latitude: Double,
        val longitude: Double,
        val rawText: String,
        val confidence: Float,
        val timestamp: Long
    ) {
        fun toCoordinate(): Coordinate = Coordinate(
            latitude = latitude,
            longitude = longitude,
            rawText = rawText,
            confidence = confidence,
            timestamp = timestamp
        )
        
        companion object {
            fun fromCoordinate(coord: Coordinate): CoordinateData = CoordinateData(
                latitude = coord.latitude,
                longitude = coord.longitude,
                rawText = coord.rawText,
                confidence = coord.confidence,
                timestamp = coord.timestamp
            )
        }
    }
    
    companion object {
        private const val PREFS_NAME = "coordinate_history_prefs"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY_SIZE = 100
        
        @Volatile
        private var instance: HistoryManager? = null
        
        fun getInstance(context: Context): HistoryManager {
            return instance ?: synchronized(this) {
                instance ?: HistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
