package com.kylecorry.trail_sense.astronomy.ui

import android.view.View
import com.kylecorry.trail_sense.shared.alignToVector

class IconClock(private val clock: View, private val icon: View) {
    fun display(angle: Float, percentRadius: Float = 1f){
        val radius = clock.width / 2f * percentRadius
        alignToVector(clock, icon, radius, angle)
    }
}