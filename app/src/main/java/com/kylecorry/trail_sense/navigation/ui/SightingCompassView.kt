package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.SeekBar
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.setOnProgressChangeListener
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import kotlin.math.roundToInt

class SightingCompassView(
    private val lifecycleOwner: LifecycleOwner,
    private val preview: PreviewView,
    private val reticle: View,
    private val zoom: SeekBar
) {

    private val camera by lazy {
        Camera(
            preview.context,
            lifecycleOwner,
            previewView = preview,
            analyze = false
        )
    }

    private val prefs by lazy {
        Preferences(preview.context)
    }

    private var _isRunning = false

    fun start() {
        if (!Camera.isAvailable(preview.context)) {
            return
        }
        try {
            camera.start(this::onCameraUpdate)
        } catch (e: Exception) {
            e.printStackTrace()
            Alerts.toast(preview.context, preview.context.getString(R.string.no_camera_access))
            stop()
            return
        }

        _isRunning = true

        zoom.setOnProgressChangeListener { progress, _ ->
            val zoom = (progress / 100f).coerceIn(0f, 1f)
            camera.setZoom(zoom)
            prefs.putFloat(NavigatorFragment.CACHE_CAMERA_ZOOM, zoom)
        }

        preview.isVisible = true
        reticle.isVisible = true
        zoom.isVisible = true
        zoom.progress =
            (100 * (prefs.getFloat(NavigatorFragment.CACHE_CAMERA_ZOOM) ?: 0.5f)).roundToInt()
        val handler = FlashlightHandler.getInstance(preview.context)
        handler.off()
    }

    fun stop() {
        _isRunning = false
        try {
            camera.stop(this::onCameraUpdate)
        } catch (e: Exception){
            e.printStackTrace()
        }
        zoom.setOnSeekBarChangeListener(null)
        preview.isVisible = false
        reticle.isVisible = false
        zoom.isVisible = false
    }

    fun isRunning(): Boolean {
        return _isRunning
    }

    private fun onCameraUpdate(): Boolean {
        val zoom = prefs.getFloat(NavigatorFragment.CACHE_CAMERA_ZOOM) ?: 0.5f
        camera.setZoom(zoom)
        return true
    }

}