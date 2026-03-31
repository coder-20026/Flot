package com.coordextractor.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.coordextractor.app.model.Coordinate

/**
 * Helper class for managing broadcasts
 */
object BroadcastHelper {
    
    /**
     * Send coordinate extracted broadcast
     */
    fun sendCoordinateExtracted(context: Context, coordinate: Coordinate) {
        val intent = Intent(Constants.BROADCAST_COORDINATES_EXTRACTED).apply {
            putExtra(Constants.EXTRA_COORDINATES, coordinate.formatted)
            putExtra(Constants.EXTRA_RAW_TEXT, coordinate.rawText)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
    
    /**
     * Send capture started broadcast
     */
    fun sendCaptureStarted(context: Context) {
        val intent = Intent(Constants.BROADCAST_CAPTURE_STARTED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
    
    /**
     * Send capture stopped broadcast
     */
    fun sendCaptureStopped(context: Context) {
        val intent = Intent(Constants.BROADCAST_CAPTURE_STOPPED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
    
    /**
     * Send error broadcast
     */
    fun sendError(context: Context, errorMessage: String) {
        val intent = Intent(Constants.BROADCAST_ERROR).apply {
            putExtra("error_message", errorMessage)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
    
    /**
     * Register receiver for coordinate updates
     */
    fun registerCoordinateReceiver(
        context: Context,
        onCoordinateExtracted: (String, String?) -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    val coordinates = it.getStringExtra(Constants.EXTRA_COORDINATES)
                    val rawText = it.getStringExtra(Constants.EXTRA_RAW_TEXT)
                    if (!coordinates.isNullOrEmpty()) {
                        onCoordinateExtracted(coordinates, rawText)
                    }
                }
            }
        }
        
        val filter = IntentFilter(Constants.BROADCAST_COORDINATES_EXTRACTED)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        return receiver
    }
    
    /**
     * Register receiver for capture state changes
     */
    fun registerCaptureStateReceiver(
        context: Context,
        onStarted: () -> Unit,
        onStopped: () -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Constants.BROADCAST_CAPTURE_STARTED -> onStarted()
                    Constants.BROADCAST_CAPTURE_STOPPED -> onStopped()
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Constants.BROADCAST_CAPTURE_STARTED)
            addAction(Constants.BROADCAST_CAPTURE_STOPPED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        return receiver
    }
    
    /**
     * Register receiver for errors
     */
    fun registerErrorReceiver(
        context: Context,
        onError: (String) -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.getStringExtra("error_message")?.let { error ->
                    onError(error)
                }
            }
        }
        
        val filter = IntentFilter(Constants.BROADCAST_ERROR)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        return receiver
    }
}
