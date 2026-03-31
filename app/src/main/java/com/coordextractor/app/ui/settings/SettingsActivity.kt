package com.coordextractor.app.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.coordextractor.app.R
import com.coordextractor.app.databinding.ActivitySettingsBinding
import com.coordextractor.app.util.Constants
import com.coordextractor.app.util.PreferencesManager
import com.google.android.material.slider.Slider

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
                prefsManager.captureInterval = value.toLong()
                binding.tvCaptureIntervalValue.text = getString(R.string.interval_ms_format, value.toInt())
            }
        }
        
        // ROI Width slider
        binding.sliderRoiWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.roiWidthPercent = value / 100f
                binding.tvRoiWidthValue.text = getString(R.string.percent_format, value.toInt())
            }
        }
        
        // ROI Height slider
        binding.sliderRoiHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.roiHeightPercent = value / 100f
                binding.tvRoiHeightValue.text = getString(R.string.percent_format, value.toInt())
            }
        }
        
        // Confidence threshold slider
        binding.sliderConfidenceThreshold.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefsManager.ocrConfidenceThreshold = value / 100f
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
            tvCaptureIntervalValue.text = getString(R.string.interval_ms_format, prefsManager.captureInterval.toInt())
            
            sliderRoiWidth.value = (prefsManager.roiWidthPercent * 100)
            tvRoiWidthValue.text = getString(R.string.percent_format, (prefsManager.roiWidthPercent * 100).toInt())
            
            sliderRoiHeight.value = (prefsManager.roiHeightPercent * 100)
            tvRoiHeightValue.text = getString(R.string.percent_format, (prefsManager.roiHeightPercent * 100).toInt())
            
            sliderConfidenceThreshold.value = (prefsManager.ocrConfidenceThreshold * 100)
            tvConfidenceValue.text = getString(R.string.percent_format, (prefsManager.ocrConfidenceThreshold * 100).toInt())
        }
    }
    
    private fun resetToDefaults() {
        with(Constants.Defaults) {
            prefsManager.apply {
                realtimeMode = REALTIME_MODE
                captureInterval = CAPTURE_INTERVAL_MS
                roiWidthPercent = ROI_WIDTH_PERCENT
                roiHeightPercent = ROI_HEIGHT_PERCENT
                autoCopy = AUTO_COPY
                vibrateOnSuccess = VIBRATE_ON_SUCCESS
                soundOnSuccess = SOUND_ON_SUCCESS
                ocrConfidenceThreshold = OCR_CONFIDENCE_THRESHOLD
            }
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
