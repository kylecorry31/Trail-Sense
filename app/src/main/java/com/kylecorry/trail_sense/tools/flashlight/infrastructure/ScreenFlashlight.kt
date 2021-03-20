package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import android.view.Window
import android.view.WindowManager
import com.kylecorry.trailsensecore.infrastructure.flashlight.IFlashlight

class ScreenFlashlight(private val window: Window): IFlashlight {
    override fun on() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = layoutParams
    }

    override fun off() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }
}