package com.kylecorry.trail_sense.tools.whitenoise.ui

import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import com.kylecorry.trailsensecore.infrastructure.system.NotificationUtils
import com.kylecorry.trailsensecore.infrastructure.time.Intervalometer

class QuickActionWhiteNoise(btn: FloatingActionButton, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val intervalometer = Intervalometer {
        CustomUiUtils.setButtonState(button, isOn())
    }

    private fun isOn(): Boolean {
        return NotificationUtils.isNotificationActive(context, WhiteNoiseService.NOTIFICATION_ID)
    }

    override fun onCreate() {
        button.setImageResource(R.drawable.ic_tool_white_noise)
        CustomUiUtils.setButtonState(button, false);

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