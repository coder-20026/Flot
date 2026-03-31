package com.coordextractor.app.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.coordextractor.app.R
import com.coordextractor.app.databinding.ActivitySettingsBinding
import com.coordextractor.app.util.PreferencesManager

/**
 * Settings Activity for app configuration
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager.getInstance(this)
        
        setupToolbar()
        setupUI()
        loadSettings()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings)
        }
    }
    
    private fun setupUI() {
        // Realtime mode switch
        binding.switchRealtimeMode.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.realtimeMode = isChecked
        }
        
        // Auto copy switch
        binding.switchAutoCopy.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.autoCopy = isChecked
        }
        
        // Vibrate on success switch
        binding.switchVibrateOnSuccess.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.vibrateOnSuccess = isChecked
        }
        
        // Sound on success switch
        binding.switchSoundOnSuccess.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.soundOnSuccess = isChecked
        }
        
        // Capture interval slider
        binding.sliderCaptureInterval.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.captureInterval = value.toInt()
                binding.tvCaptureIntervalValue.text = getString(R.string.interval_ms_format, value.toInt())
            }
        }
        
        // ROI Width slider
        binding.sliderRoiWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.roiWidthPercent = value.toInt()
                binding.tvRoiWidthValue.text = getString(R.string.percent_format, value.toInt())
            }
        }
        
        // ROI Height slider
        binding.sliderRoiHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.roiHeightPercent = value.toInt()
                binding.tvRoiHeightValue.text = getString(R.string.percent_format, value.toInt())
            }
        }
        
        // Confidence threshold slider
        binding.sliderConfidenceThreshold.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.ocrConfidenceThreshold = value.toInt()
                binding.tvConfidenceValue.text = getString(R.string.percent_format, value.toInt())
            }
        }
        
        // Reset button
        binding.btnResetDefaults.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun loadSettings() {
        binding.apply {
            // Load switches
            switchRealtimeMode.isChecked = prefsManager.realtimeMode
            switchAutoCopy.isChecked = prefsManager.autoCopy
            switchVibrateOnSuccess.isChecked = prefsManager.vibrateOnSuccess
            switchSoundOnSuccess.isChecked = prefsManager.soundOnSuccess
            
            // Load sliders
            sliderCaptureInterval.value = prefsManager.captureInterval.toFloat()
            tvCaptureIntervalValue.text = getString(R.string.interval_ms_format, prefsManager.captureInterval)
            
            sliderRoiWidth.value = prefsManager.roiWidthPercent.toFloat()
            tvRoiWidthValue.text = getString(R.string.percent_format, prefsManager.roiWidthPercent)
            
            sliderRoiHeight.value = prefsManager.roiHeightPercent.toFloat()
            tvRoiHeightValue.text = getString(R.string.percent_format, prefsManager.roiHeightPercent)
            
            sliderConfidenceThreshold.value = prefsManager.ocrConfidenceThreshold.toFloat()
            tvConfidenceValue.text = getString(R.string.percent_format, prefsManager.ocrConfidenceThreshold)
        }
    }
    
    private fun resetToDefaults() {
        prefsManager.apply {
            realtimeMode = false
            captureInterval = PreferencesManager.DEFAULT_CAPTURE_INTERVAL
            roiWidthPercent = PreferencesManager.DEFAULT_ROI_WIDTH_PERCENT
            roiHeightPercent = PreferencesManager.DEFAULT_ROI_HEIGHT_PERCENT
            autoCopy = false
            vibrateOnSuccess = true
            soundOnSuccess = false
            ocrConfidenceThreshold = PreferencesManager.DEFAULT_OCR_CONFIDENCE_THRESHOLD
        }
        loadSettings()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
