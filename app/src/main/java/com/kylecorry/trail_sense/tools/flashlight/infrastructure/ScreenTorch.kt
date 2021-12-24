package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.view.Window
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.torch.ITorch

class ScreenTorch(private val window: Window) : ITorch {

    var brightness = 1f

    override fun on() {
        Screen.setBrightness(window, brightness)
    }

    override fun off() {
        Screen.resetBrightness(window)
    }

    override fun isAvailable(): Boolean {
        return true
    }
}