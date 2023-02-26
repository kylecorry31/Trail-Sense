package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class SightingCompassView(
    private val camera: CameraView,
    private val reticle: View
) {

    private val prefs by lazy {
        PreferencesSubsystem.getInstance(camera.context).preferences
    }

    private var _isRunning = false

    init {
        camera.setShowTorch(false)
    }

    fun start() {
        if (!Camera.isAvailable(camera.context)) {
            return
        }
        try {
            camera.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Alerts.toast(camera.context, camera.context.getString(R.string.no_camera_access))
            stop()
            return
        }

        _isRunning = true

        camera.setOnZoomChangeListener {
            prefs.putFloat(NavigatorFragment.CACHE_CAMERA_ZOOM, it)
        }

        camera.setZoom(prefs.getFloat(NavigatorFragment.CACHE_CAMERA_ZOOM) ?: 0.5f)

        camera.isVisible = true
        reticle.isVisible = true


        val flashlight = FlashlightSubsystem.getInstance(camera.context)
        flashlight.set(FlashlightMode.Off)
    }

    fun stop() {
        _isRunning = false
        try {
            camera.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        camera.isVisible = false
        reticle.isVisible = false
    }

    fun isRunning(): Boolean {
        return _isRunning
    }
}