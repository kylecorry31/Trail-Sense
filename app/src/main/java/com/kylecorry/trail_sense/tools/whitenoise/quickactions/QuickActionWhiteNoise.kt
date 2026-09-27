package com.kylecorry.trail_sense.tools.whitenoise.quickactions

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.whitenoise.WhiteNoiseToolRegistration
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService

class QuickActionWhiteNoise(btn: QuickActionButtonView, fragment: Fragment) :
    QuickActionButton(btn, fragment) {

    private val stateListener: suspend (Bundle) -> Unit = {
        onMain {
            setState(isOn())
        }
    }

    private fun isOn(): Boolean {
        return WhiteNoiseService.isRunning
    }

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_white_noise)
    }

    override fun onClick() {
        super.onClick()
        if (isOn()) {
            WhiteNoiseService.stop(context)
        } else {
            WhiteNoiseService.play(context)
        }
    }

    override fun onResume() {
        super.onResume()
        setState(isOn())
        Tools.subscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onPause() {
        super.onPause()
        Tools.unsubscribe(WhiteNoiseToolRegistration.BROADCAST_PLAYBACK_STATE_CHANGED, stateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        onPause()
    }

}
