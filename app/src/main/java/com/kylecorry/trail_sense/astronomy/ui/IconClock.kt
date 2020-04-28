package com.kylecorry.trail_sense.astronomy.ui

import android.view.View
import com.kylecorry.trail_sense.shared.getCenterX
import com.kylecorry.trail_sense.shared.getCenterY
import com.kylecorry.trail_sense.shared.math.cosDegrees
import com.kylecorry.trail_sense.shared.math.sinDegrees

class IconClock(private val clock: View, private val icon: View) {

    fun display(angle: Float, percentRadius: Float = 1f){
        val radius = clock.width / 2f
        val centerX = clock.getCenterX() - icon.width / 2f
        val centerY = clock.getCenterY() - icon.height / 2f

        val newCenter = getPointOnCircumference(Pair(centerX, centerY), radius * percentRadius, angle)

        icon.x = newCenter.first
        icon.y = newCenter.second
    }

    private fun getPointOnCircumference(center: Pair<Float, Float>, radius: Float, angle: Float): Pair<Float, Float> {
        val x = center.first - cosDegrees(angle.toDouble()) * radius
        val y = center.second - sinDegrees(angle.toDouble()) * radius
        return Pair(x.toFloat(), y.toFloat())
    }

}