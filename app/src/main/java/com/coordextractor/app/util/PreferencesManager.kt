package com.coordextractor.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coordinate_extractor_preferences"
)

/**
 * Manages app preferences using DataStore
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Keys
        private val KEY_REALTIME_MODE = booleanPreferencesKey("realtime_mode")
        private val KEY_ROI_WIDTH_PERCENT = intPreferencesKey("roi_width_percent")
        private val KEY_ROI_HEIGHT_PERCENT = intPreferencesKey("roi_height_percent")
        private val KEY_CAPTURE_INTERVAL = intPreferencesKey("capture_interval")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_FLOATING_BUTTON_X = intPreferencesKey("floating_button_x")
        private val KEY_FLOATING_BUTTON_Y = intPreferencesKey("floating_button_y")

        // Defaults
        const val DEFAULT_ROI_WIDTH_PERCENT = 30
        const val DEFAULT_ROI_HEIGHT_PERCENT = 25
        const val DEFAULT_CAPTURE_INTERVAL = 500
        const val DEFAULT_THEME_MODE = "system"
    }

    // Realtime Mode
    val realtimeModeFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_REALTIME_MODE] ?: false
    }

    var realtimeMode: Boolean
        get() = runBlocking { realtimeModeFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_REALTIME_MODE] = value }
        }

    // ROI Width Percent
    val roiWidthPercentFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_ROI_WIDTH_PERCENT] ?: DEFAULT_ROI_WIDTH_PERCENT
    }

    var roiWidthPercent: Int
        get() = runBlocking { roiWidthPercentFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_ROI_WIDTH_PERCENT] = value.coerceIn(10, 100) }
        }

    // ROI Height Percent
    val roiHeightPercentFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_ROI_HEIGHT_PERCENT] ?: DEFAULT_ROI_HEIGHT_PERCENT
    }

    var roiHeightPercent: Int
        get() = runBlocking { roiHeightPercentFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_ROI_HEIGHT_PERCENT] = value.coerceIn(10, 100) }
        }

    // Capture Interval
    val captureIntervalFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_CAPTURE_INTERVAL] ?: DEFAULT_CAPTURE_INTERVAL
    }

    var captureInterval: Int
        get() = runBlocking { captureIntervalFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_CAPTURE_INTERVAL] = value.coerceIn(100, 5000) }
        }

    // Theme Mode
    val themeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: DEFAULT_THEME_MODE
    }

    var themeMode: String
        get() = runBlocking { themeModeFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_THEME_MODE] = value }
        }

    // First Launch
    val isFirstLaunchFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH] ?: true
    }

    var isFirstLaunch: Boolean
        get() = runBlocking { isFirstLaunchFlow.first() }
        set(value) = runBlocking {
            dataStore.edit { it[KEY_FIRST_LAUNCH] = value }
        }

    // Floating Button Position
    val floatingButtonPositionFlow: Flow<Pair<Int, Int>> = dataStore.data.map { preferences ->
        val x = preferences[KEY_FLOATING_BUTTON_X] ?: -1
        val y = preferences[KEY_FLOATING_BUTTON_Y] ?: -1
        Pair(x, y)
    }

    fun saveFloatingButtonPosition(x: Int, y: Int) = runBlocking {
        dataStore.edit {
            it[KEY_FLOATING_BUTTON_X] = x
            it[KEY_FLOATING_BUTTON_Y] = y
        }
    }

    // Reset all settings
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[KEY_REALTIME_MODE] = false
            preferences[KEY_ROI_WIDTH_PERCENT] = DEFAULT_ROI_WIDTH_PERCENT
            preferences[KEY_ROI_HEIGHT_PERCENT] = DEFAULT_ROI_HEIGHT_PERCENT
            preferences[KEY_CAPTURE_INTERVAL] = DEFAULT_CAPTURE_INTERVAL
            preferences[KEY_THEME_MODE] = DEFAULT_THEME_MODE
        }
    }
}
