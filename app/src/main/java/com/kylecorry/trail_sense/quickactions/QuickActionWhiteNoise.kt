package com.kylecorry.trail_sense.quickactions

import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService

class QuickActionWhiteNoise(btn: ImageButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val intervalometer = CoroutineTimer {
        CustomUiUtils.setButtonState(button, isOn())
    }

    private fun isOn(): Boolean {
        return WhiteNoiseService.isRunning
    }

    override fun onCreate() {
        super.onCreate()
        button.setImageResource(R.drawable.ic_tool_white_noise)
        CustomUiUtils.setButtonState(button, false)

        button.setOnClickListener {
            if (isOn()) {
                WhiteNoiseService.stop(context)
            } else {
                WhiteNoiseService.clearSleepTimer(context)
                WhiteNoiseService.start(context)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!intervalometer.isRunning()) {
            intervalometer.interval(20)
        }
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

}