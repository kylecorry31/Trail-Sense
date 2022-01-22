package com.kylecorry.trail_sense.navigation.ui

import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.Dial

class CompassDial(
    private val center: PixelCoordinate,
    private val radius: Float,
    @ColorInt private val backgroundColor: Int,
    @ColorInt private val tickColor: Int,
    @ColorInt private val cardinalTickColor: Int = tickColor
) {

    private val tickThicknessDp = 1f
    private val tickLengthPercent = 0.03f
    private val tickRadiusPercent = 0.9f
    private val ticks = Dial.ticks(
        center,
        tickRadiusPercent * radius,
        tickLengthPercent * radius,
        15
    )
    private val cardinalTicks = Dial.ticks(
        center,
        tickRadiusPercent * radius,
        tickLengthPercent * radius,
        45
    )

    fun draw(drawer: ICanvasDrawer) {
        drawer.opacity(255)
        drawer.noStroke()
        drawer.fill(backgroundColor)
        drawer.circle(center.x, center.y, radius * 2)

        drawer.strokeWeight(drawer.dp(tickThicknessDp))
        drawer.noFill()
        drawer.stroke(tickColor)
        drawer.path(ticks)

        drawer.stroke(cardinalTickColor)
        drawer.path(cardinalTicks)
    }

}