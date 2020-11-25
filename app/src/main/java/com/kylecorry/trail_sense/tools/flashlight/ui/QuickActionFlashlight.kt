package com.kylecorry.trail_sense.tools.flashlight.ui

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightState
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightHandler
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class QuickActionFlashlight(btn: FloatingActionButton, fragment: Fragment): QuickActionButton(btn, fragment) {

    private var flashlightState = FlashlightState.Off
    private val flashlight by lazy { FlashlightHandler(context) }
    private val intervalometer = Intervalometer {
        flashlightState = flashlight.getState()
        updateFlashlightUI()
    }

    private fun getNextFlashlightState(currentState: FlashlightState): FlashlightState {
        return flashlight.getNextState(currentState)
    }

    private fun updateFlashlightUI() {
        when (flashlightState) {
            FlashlightState.On -> {
                button.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(
                    button,
                    true,
                    UiUtils.color(context, R.color.colorPrimary),
                    UiUtils.color(context, R.color.colorSecondary)
                )
            }
            FlashlightState.SOS -> {
                button.setImageResource(R.drawable.flashlight_sos)
                UiUtils.setButtonState(
                    button,
                    true,
                    UiUtils.color(context, R.color.colorPrimary),
                    UiUtils.color(context, R.color.colorSecondary)
                )
            }
            else -> {
                button.setImageResource(R.drawable.flashlight)
                UiUtils.setButtonState(
                    button,
                    false,
                    UiUtils.color(context, R.color.colorPrimary),
                    UiUtils.color(context, R.color.colorSecondary)
                )
            }
        }
    }

    override fun onCreate() {
        if (!flashlight.isAvailable()) {
            button.visibility = View.GONE
        } else {
            button.setOnClickListener {
                flashlightState = getNextFlashlightState(flashlightState)
                flashlight.set(flashlightState)
            }
        }
    }

    override fun onResume(){
        if (!intervalometer.isRunning()) {
            intervalometer.interval(20)
        }
    }

    override fun onPause(){
        intervalometer.stop()
    }

    override fun onDestroy() {
        onPause()
    }

}