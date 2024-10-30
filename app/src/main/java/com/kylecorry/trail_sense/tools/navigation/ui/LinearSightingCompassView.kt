package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.databinding.ViewLinearSightingCompassBinding
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

class LinearSightingCompassView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs), ICompassView {

    val isCameraActive: Boolean
        get() = sightingCompass.isRunning()

    private var showSightingCompass = false
    private var isStarted = false
    private var isResumed = false
    private val lock = Any()
    private val binding by lazy { ViewLinearSightingCompassBinding.bind(this) }

    private val sightingCompass by lazy {
        SightingCompassView(
            binding.viewCamera,
            binding.viewCameraLine,
            binding.linearCompass
        ){
            // Forward the click event from the sighting compass
            callOnClick()
        }
    }

    private val hooks = Hooks()

    // TODO: Make this more efficient (only update when the camera changes)
    private val updateTimer = CoroutineTimer {
        sightingCompass.update()
    }

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                resume()
            }

            Lifecycle.Event.ON_PAUSE -> {
                pause()
            }

            else -> {
                // Do nothing
            }
        }
    }

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        hooks.effect("visibility", isVisible) {
            if (isVisible) {
                start()
            } else {
                stop()
            }
        }
    }

    init {
        inflate(context, R.layout.view_linear_sighting_compass, this)

        CustomUiUtils.setButtonState(binding.sightingCompassBtn, false)
        binding.sightingCompassBtn.setOnClickListener {
            setSightingCompass(!showSightingCompass)
        }

        doOnAttach {
            val observer = findViewTreeLifecycleOwner()
            observer?.lifecycle?.addObserver(lifecycleListener)
            resume()
        }

        doOnDetach {
            val observer = findViewTreeLifecycleOwner()
            observer?.lifecycle?.removeObserver(lifecycleListener)
            pause()
        }

    }

    /**
     * Called when the view is resumed or attached
     */
    private fun resume() {
        synchronized(lock) {
            if (isResumed) {
                return
            }
            isResumed = true
            showSightingCompass = false
            viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        }
        if (isVisible) {
            start()
        }
    }

    /**
     * Called when the view is paused or detached
     */
    private fun pause() {
        synchronized(lock) {
            if (!isResumed) {
                return
            }
            isResumed = false
            viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        }
        stop()
    }

    /**
     * Called when the view becomes visible
     */
    private fun start() {
        synchronized(lock) {
            if (isStarted) {
                return
            }
            isStarted = true
            setSightingCompass(showSightingCompass)
            updateTimer.interval(20)
        }
    }

    /**
     * Called when the view becomes invisible
     */
    private fun stop() {
        synchronized(lock) {
            if (!isStarted) {
                return
            }
            isStarted = false
            sightingCompass.stop()
            CustomUiUtils.setButtonState(binding.sightingCompassBtn, false)
            updateTimer.stop()
        }
    }

    private fun setSightingCompass(shouldShow: Boolean) {
        showSightingCompass = shouldShow
        if (!shouldShow) {
            sightingCompass.stop()
            CustomUiUtils.setButtonState(binding.sightingCompassBtn, false)
        } else if (!sightingCompass.isRunning()) {
            CustomUiUtils.setButtonState(binding.sightingCompassBtn, true)
            val fragment = findFragment<AndromedaFragment>()
            fragment.requestCamera { hasPermission ->
                if (hasPermission) {
                    sightingCompass.start()
                } else {
                    fragment.alertNoCameraPermission()
                    setSightingCompass(false)
                }
            }
        }
    }

    override var compassCenter: Coordinate
        get() = binding.linearCompass.compassCenter
        set(value) {
            binding.linearCompass.compassCenter = value
        }
    override var useTrueNorth: Boolean
        get() = binding.linearCompass.useTrueNorth
        set(value) {
            binding.linearCompass.useTrueNorth = value
        }
    override var declination: Float
        get() = binding.linearCompass.declination
        set(value) {
            binding.linearCompass.declination = value
        }
    override var azimuth: Bearing
        get() = binding.linearCompass.azimuth
        set(value) {
            binding.linearCompass.azimuth = value
        }

    override fun addCompassLayer(layer: ICompassLayer) {
        binding.linearCompass.addCompassLayer(layer)
    }

    override fun removeCompassLayer(layer: ICompassLayer) {
        binding.linearCompass.removeCompassLayer(layer)
    }

    override fun setCompassLayers(layers: List<ICompassLayer>) {
        binding.linearCompass.setCompassLayers(layers)
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        binding.linearCompass.draw(reference, size)
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        binding.linearCompass.draw(bearing, stopAt)
    }

}