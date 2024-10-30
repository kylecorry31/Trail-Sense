package com.kylecorry.trail_sense.tools.navigation.ui

import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SightingCompassView(
    private val camera: CameraView,
    private val reticle: View,
    private val compass: LinearCompassView,
    private val onClick: () -> Unit = {}
) {

    private val prefs by lazy {
        PreferencesSubsystem.getInstance(camera.context).preferences
    }

    private var _isRunning = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private val zoomRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.IO)
    private val fovRunner = CoroutineQueueRunner(1, dispatcher = Dispatchers.Default)

    init {
        camera.setShowTorch(false)
        camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
    }

    fun start() {
        if (!Camera.isAvailable(camera.context)) {
            return
        }
        try {
            camera.start(
                readFrames = false,
                shouldStabilizePreview = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Alerts.toast(camera.context, camera.context.getString(R.string.no_camera_access))
            stop()
            return
        }

        _isRunning = true

        camera.setOnZoomChangeListener {
            scope.launch {
                zoomRunner.enqueue {
                    prefs.putFloat(NavigatorFragment.CACHE_CAMERA_ZOOM, it)
                }
            }
        }

        camera.setOnClickListener {
            onClick()
        }

        camera.setZoom(prefs.getFloat(NavigatorFragment.CACHE_CAMERA_ZOOM) ?: 0.5f)

        camera.isVisible = true
        reticle.isVisible = true


        val flashlight = FlashlightSubsystem.getInstance(camera.context)
        flashlight.set(FlashlightMode.Off)
    }

    fun update(){
        if (!isRunning()){
            compass.range = 180f
            return
        }
        scope.launch {
            fovRunner.enqueue {
                val compassWidth = compass.width
                val cameraWidth = camera.width

                val ratio = compassWidth / cameraWidth.toFloat()

                val minimumFOV = 5f
                compass.range =
                    (camera.fov.first.coerceAtLeast(minimumFOV) * ratio).coerceAtMost(180f)
            }
        }
    }

    fun stop() {
        _isRunning = false
        try {
            camera.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fovRunner.cancel()
        zoomRunner.cancel()
        camera.isVisible = false
        reticle.isVisible = false
    }

    fun isRunning(): Boolean {
        return _isRunning
    }
}