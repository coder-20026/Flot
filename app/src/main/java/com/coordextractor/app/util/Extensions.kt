package com.coordextractor.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.AnimRes
import com.coordextractor.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Extension functions for commonly used operations
 */

// Context Extensions
fun Context.copyToClipboard(text: String, label: String = "Coordinates") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    showToast(getString(R.string.copied_to_clipboard))
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.openInMaps(latitude: Double, longitude: Double) {
    try {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to browser if Google Maps is not installed
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    } catch (e: Exception) {
        showToast(getString(R.string.error_opening_maps))
    }
}

fun Context.shareCoordinates(coordinates: String, rawText: String? = null) {
    val shareText = buildString {
        append(getString(R.string.share_coordinates_prefix))
        append("\n")
        append(coordinates)
        rawText?.let {
            append("\n\n")
            append(getString(R.string.share_raw_text_prefix))
            append("\n")
            append(it)
        }
    }
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
}

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showWithAnimation(@AnimRes animRes: Int = R.anim.scale_up) {
    if (visibility == View.VISIBLE) return
    visibility = View.VISIBLE
    startAnimation(AnimationUtils.loadAnimation(context, animRes))
}

fun View.hideWithAnimation(@AnimRes animRes: Int = R.anim.scale_down) {
    if (visibility != View.VISIBLE) return
    val anim = AnimationUtils.loadAnimation(context, animRes)
    anim.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            visibility = View.GONE
        }
        override fun onAnimationRepeat(animation: Animation?) {}
    })
    startAnimation(anim)
}

fun View.setOnSingleClickListener(debounceTime: Long = 500L, action: () -> Unit) {
    setOnClickListener {
        isEnabled = false
        action()
        postDelayed({ isEnabled = true }, debounceTime)
    }
}

// Coroutine Extensions
fun <T> debounce(
    delayMs: Long = 300L,
    scope: CoroutineScope,
    action: (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            action(param)
        }
    }
}

// String Extensions
fun String.isValidCoordinate(): Boolean {
    return try {
        val parts = this.split(",")
        if (parts.size != 2) return false
        val lat = parts[0].trim().toDouble()
        val lon = parts[1].trim().toDouble()
        lat in -90.0..90.0 && lon in -180.0..180.0
    } catch (e: Exception) {
        false
    }
}

fun String.parseLatLng(): Pair<Double, Double>? {
    return try {
        val parts = this.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDouble()
        val lon = parts[1].trim().toDouble()
        Pair(lat, lon)
    } catch (e: Exception) {
        null
    }
}

// Number Extensions
fun Double.formatCoordinate(decimals: Int = 6): String {
    return String.format("%.${decimals}f", this)
}

fun Float.toDp(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Int.toPx(context: Context): Float {
    return this / context.resources.displayMetrics.density
}
