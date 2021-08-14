package com.kylecorry.trail_sense.tools.flashlight.ui

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.andromeda.core.time.Timer

class QuickActionFlashlight(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private var flashlightState = FlashlightState.Off
    private val flashlight by lazy { FlashlightHandler.getInstance(context) }
    private val intervalometer = Timer {
        flashlightState = flashlight.getState()
        updateFlashlightUI()
    }

    private fun updateFlashlightUI() {
        CustomUiUtils.setButtonState(button, flashlightState == FlashlightState.On)
    }

    override fun onCreate() {
        button.setImageResource(R.drawable.flashlight)
        CustomUiUtils.setButtonState(button, false)
        if (!flashlight.isAvailable()) {
            button.visibility = View.GONE
        } else {
            button.setOnClickListener {
                if (flashlight.getState() == FlashlightState.On) {
                    flashlight.set(FlashlightState.Off)
                } else {
                    flashlight.set(FlashlightState.On)
                }
            }
        }
    }

    override fun onResume() {
        if (!intervalometer.isRunning()) {
            intervalometer.interval(20)
        }
    }

    override fun onPause() {
        intervalometer.stop()
    }

    override fun onDestroy() {
        onPause()
    }

}