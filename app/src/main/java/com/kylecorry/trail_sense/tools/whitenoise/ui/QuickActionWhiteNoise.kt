package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.andromeda.core.time.Timer

class QuickActionWhiteNoise(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val intervalometer = Timer {
        CustomUiUtils.setButtonState(button, isOn())
    }

    private val notify by lazy { Notify(context) }

    private fun isOn(): Boolean {
        return notify.isActive(WhiteNoiseService.NOTIFICATION_ID)
    }

    override fun onCreate() {
        button.setImageResource(R.drawable.ic_tool_white_noise)
        CustomUiUtils.setButtonState(button, false)

        button.setOnClickListener {
            if (isOn()) {
                WhiteNoiseService.stop(context)
            } else {
                WhiteNoiseService.start(context)
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