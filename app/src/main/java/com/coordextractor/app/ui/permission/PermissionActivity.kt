package com.coordextractor.app.ui.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.coordextractor.app.service.ScreenCaptureService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Transparent activity for requesting MediaProjection permission
 * Used when starting capture from service
 */
@AndroidEntryPoint
class PermissionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_TYPE = "request_type"
        const val REQUEST_MEDIA_PROJECTION = 1

        fun createIntent(context: Context, requestType: Int = REQUEST_MEDIA_PROJECTION): Intent {
            return Intent(context, PermissionActivity::class.java).apply {
                putExtra(EXTRA_REQUEST_TYPE, requestType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Start capture service with permission result
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START_CAPTURE
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val requestType = intent.getIntExtra(EXTRA_REQUEST_TYPE, REQUEST_MEDIA_PROJECTION)
        
        when (requestType) {
            REQUEST_MEDIA_PROJECTION -> {
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                mediaProjectionLauncher.launch(captureIntent)
            }
            else -> finish()
        }
    }
}
